package com.revolgenx.lemillion.core.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Binder
import android.os.Build
import android.os.Handler
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.arialyy.aria.core.Aria
import com.revolgenx.lemillion.R
import com.revolgenx.lemillion.activity.MainActivity
import com.revolgenx.lemillion.core.book.Book
import com.revolgenx.lemillion.core.db.book.BookRepository
import com.revolgenx.lemillion.core.db.torrent.TorrentRepository
import com.revolgenx.lemillion.core.receiver.NotificationReceiver
import com.revolgenx.lemillion.core.torrent.Torrent
import com.revolgenx.lemillion.core.torrent.TorrentActiveState
import com.revolgenx.lemillion.core.torrent.TorrentEngine
import com.revolgenx.lemillion.core.util.*
import com.revolgenx.lemillion.event.*
import kotlinx.coroutines.*
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.koin.android.ext.android.inject
import timber.log.Timber

class MainService : Service() {

    private val binder = LocalBinder()
    private var notifyManager: NotificationManager? = null

    private val serviceStartedNotifId: Int = 1000
    private val foregroundChanId = "com.revolgenx.lemillion.FOREGROUND_DEFAULT_CHAN_ID"
    private val defChanId = "com.revolgenx.lemillion.DEFAULT_CHAN_ID"
    private val channelName = "lemillion_channel_1"
    private val channelName1 = "lemillion_channel_2"
    private var foregroundNotification: NotificationCompat.Builder? = null
    private var startupPendingIntent:PendingIntent? = null
    private val handler = Handler()


    val torrentHashMap = mutableMapOf<String, Torrent>()
    val bookHashMap = mutableMapOf<Long, Book>()
    private val torrentEngine by inject<TorrentEngine>()
    private val torrentRepository by inject<TorrentRepository>()
    private val bookRepository by inject<BookRepository>()

    private val torrentActiveState by inject<TorrentActiveState>()


    //for any error
    private val runnable = object : Runnable {
        override fun run() {
            checkIfServiceIsEmpty()
            handler.postDelayed(this, 2000)
        }
    }

    //for updating notification
    private val notifRunnable = object : Runnable {
        override fun run() {
            if (isTorrentEmpty() && isBookEmpty()) return

            updateNotification()
            handler.postDelayed(this, 1000L)
        }
    }


    inner class LocalBinder : Binder() {
        val service: MainService
            get() = this@MainService
    }


    override fun onBind(intent: Intent?): IBinder? = binder

    override fun onCreate() {
        super.onCreate()
        Timber.d("oncreate")
        notifyManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        makeNotifyChans()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Timber.d("onstartcommand")
        registerClass(this)
        makeForegroundNotify()
        return START_NOT_STICKY
    }


    //TODO://CHECK FOR DIFFERENT EVENTS
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun torrentEvent(event: TorrentEvent) {
        handler.removeCallbacksAndMessages(null)
        handler.postDelayed(runnable, 2000)
        handler.postDelayed(notifRunnable, 1000L)

        torrentActiveState.serviceActive = true
        when (event.type) {
            TorrentEventType.TORRENT_RESUMED -> {
                synchronized(torrentHashMap) {
                    event.torrents.forEach { torrent ->
                        torrentHashMap[torrent.hash] = torrent
                    }

                    event.torrents.filter { !it.checkValidity() }.forEach { torrent ->
                        torrentHashMap.remove(torrent.hash)
                    }
                }
            }

            TorrentEventType.TORRENT_PAUSED -> {
                synchronized(torrentHashMap) {
                    CoroutineScope(Dispatchers.IO).launch {
                        torrentRepository.updateAll(event.torrents)
                    }

                    event.torrents.forEach { torrent ->
                        torrentHashMap.remove(torrent.hash)
                    }

                    checkIfServiceIsEmpty()
                }
            }
            TorrentEventType.TORRENT_FINISHED -> {
                synchronized(torrentHashMap) {
                    makeCompletedNotification(event.torrents)
                    CoroutineScope(Dispatchers.IO).launch {
                        torrentRepository.updateAll(event.torrents)
                    }
                }
            }
            TorrentEventType.TORRENT_ERROR -> {
                synchronized(torrentHashMap) {
                    makeErrorNotification(event.torrents)
                    CoroutineScope(Dispatchers.IO).launch {
                        torrentRepository.updateAll(event.torrents)
                    }
                    event.torrents.forEach { torrent ->
                        torrentHashMap.remove(torrent.hash)
                    }
                    checkIfServiceIsEmpty()
                }
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onTorrentRemovedEvent(event: TorrentRemovedEvent) {
        event.hashes.forEach { torrentHashMap.remove(it) }
        checkIfServiceIsEmpty()
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onBookEvent(event: BookEvent) {
        handler.removeCallbacksAndMessages(null)
        handler.postDelayed(runnable, 2000)
        handler.postDelayed(notifRunnable, 1000L)
        when (event.bookEventType) {
            BookEventType.BOOK_RESUMED -> {
                synchronized(bookHashMap) {
                    event.books.filter { it.checkValidity() }.forEach { book ->
                        bookHashMap[book.entity!!.id] = book
                    }
                    event.books.filter { !it.checkValidity() }.forEach { book ->
                        bookHashMap.remove(book.entity!!.id)
                    }
                }
            }
            BookEventType.BOOK_PAUSED -> {
                synchronized(bookHashMap) {
                    event.books.forEach { book ->
                        bookHashMap.remove(book.entity!!.id)
                    }
                    checkIfServiceIsEmpty()
                }
            }

            BookEventType.BOOK_COMPLETED -> {
                synchronized(bookHashMap) {
                    makeCompletedNotification(event.books)
                    event.books.forEach { book ->
                        bookHashMap.remove(book.entity!!.id)
                    }
                    checkIfServiceIsEmpty()
                }
            }
            BookEventType.BOOK_RESTART -> {
                synchronized(bookHashMap) {
                    event.books.forEach { book ->
                        book.reStart()
                        bookHashMap[book.entity!!.id] = book
                    }
                }
            }
            BookEventType.BOOK_FAILED -> {
                synchronized(bookHashMap) {
                    makeErrorNotification(event.books)
                    event.books.forEach {
                        removeBook(it.id)
                    }
                }
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onBookRemoveEvent(event: BookRemovedEvent) {
        event.ids.forEach { bookHashMap.remove(it) }
        checkIfServiceIsEmpty()
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onUpdateDatabase(event: UpdateDataBase) {
        CoroutineScope(Dispatchers.IO).launch {
            val data = event.data
            if (data is Torrent) {
                torrentRepository.update(data)
            } else if (data is Book) {
                bookRepository.update(data)
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onShutdownEvent(event: ShutdownEvent) {
        stopService()
    }


    private fun removeBook(id: Long) {
        bookHashMap.remove(id)
        checkIfServiceIsEmpty()
    }


    private fun checkIfServiceIsEmpty() {
        val torrentIsEmpty = isTorrentEmpty()
        val bookIsEmpty = isBookEmpty()

        if (torrentIsEmpty) {
            torrentActiveState.serviceActive = false
        }

        if (torrentIsEmpty && bookIsEmpty) {
            stopService()
        }
    }

    private fun isTorrentEmpty() = torrentHashMap.isEmpty()
    private fun isBookEmpty() = bookHashMap.isEmpty()


    private fun stopService() {
        CoroutineScope(Dispatchers.IO).launch {
            handler.removeCallbacksAndMessages(null)
            unregisterClass(this)
            torrentHashMap.values.pmap {
                val torrent = it
                torrent.pause()
            }
            delay(100)
            torrentRepository.updateAll(torrentHashMap.values.toList())
            torrentHashMap.clear()
            Aria.download(this).stopAllTask()
            bookHashMap.clear()
            if (!torrentActiveState.fragmentActive) {
                torrentEngine.stop()
            }
            stopSelf()
        }
    }

    override fun onLowMemory() {
        super.onLowMemory()
        stopService()
    }

    //notification
    private fun makeNotifyChans() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O)
            return

        val chans = mutableListOf<NotificationChannel>()
        val defaultChan =
            NotificationChannel(defChanId, channelName, NotificationManager.IMPORTANCE_DEFAULT)
        defaultChan.enableVibration(true)
        defaultChan.vibrationPattern = longArrayOf(1000) /* ms */
        defaultChan.enableLights(true)
        defaultChan.lightColor = Color.WHITE

        chans.add(defaultChan)
        chans.add(
            NotificationChannel(
                foregroundChanId,
                channelName1,
                NotificationManager.IMPORTANCE_DEFAULT
            )
        )
        notifyManager!!.createNotificationChannels(chans)
    }

    private fun makeForegroundNotify() {
        /* For starting main activity */
        val startupIntent = Intent(applicationContext, MainActivity::class.java)
        startupIntent.action = Intent.ACTION_MAIN
        startupIntent.addCategory(Intent.CATEGORY_LAUNCHER)
//        startupIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)

        startupPendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            startupIntent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        foregroundNotification = NotificationCompat.Builder(
            applicationContext,
            foregroundChanId
        )
            .setContentIntent(startupPendingIntent)
            .setContentTitle(getTitleNotifString())
            .setStyle(notificationStyle())
            .setContentText(getString(R.string.foreground_notification))
            .setTicker(getString(R.string.foreground_notification))
            .setWhen(System.currentTimeMillis())
            .setSmallIcon(R.mipmap.ic_launcher)
            .addAction(makeShutdownAction())
            .setCategory(Notification.CATEGORY_SERVICE)

        startForeground(serviceStartedNotifId, foregroundNotification!!.build())
    }

    private fun makeShutdownAction(): NotificationCompat.Action {
        val shutdownIntent = Intent(applicationContext, NotificationReceiver::class.java).apply {
            putExtra(NotificationReceiver.SHUTDOWN_ACTION_KEY, "shutdown")
        }
        return NotificationCompat.Action(
            0,
            getString(R.string.exit),
            PendingIntent.getBroadcast(
                applicationContext,
                0,
                shutdownIntent,
                PendingIntent.FLAG_UPDATE_CURRENT
            )
        )
    }


    private fun makeCompletedNotification(list: Any) {
        val builder = NotificationCompat.Builder(applicationContext, defChanId)
            .setSmallIcon(R.drawable.ic_done)
            .setColor(color(R.color.colorPrimary))
            .setContentTitle(string(R.string.completed))
            .setWhen(System.currentTimeMillis())
            .setDefaults(Notification.DEFAULT_SOUND)
            .setDefaults(Notification.DEFAULT_VIBRATE)
            .setLights(color(R.color.colorPrimary), 1000, 1000)
            .setCategory(Notification.CATEGORY_STATUS)
            .setAutoCancel(true)
            .setContentIntent(startupPendingIntent)


        if (list is List<*>) {
            list.forEach { obj ->
                builder.setContentText(if (obj is Torrent) obj.name else (obj as? Book)?.name)
                notifyManager!!.notify(
                    if (obj is Torrent) obj.hashCode() else (obj as? Book)?.id.hashCode(),
                    builder.build()
                )
            }
        }
    }

    private fun makeErrorNotification(list: Any) {
        val builder = NotificationCompat.Builder(applicationContext, defChanId)
            .setSmallIcon(R.drawable.ic_error)
            .setColor(color(R.color.colorPrimary))
            .setWhen(System.currentTimeMillis())
            .setDefaults(Notification.DEFAULT_SOUND)
            .setDefaults(Notification.DEFAULT_VIBRATE)
            .setLights(color(R.color.colorPrimary), 1000, 1000)
            .setCategory(Notification.CATEGORY_STATUS)
            .setAutoCancel(true)
            .setContentIntent(startupPendingIntent)

        if (list is List<*>) {
            list.forEach { obj ->
                builder.setContentTitle(if (obj is Torrent) obj.name else (obj as Book).name)
                builder.setContentText(if (obj is Torrent) obj.errorMsg else (obj as Book).errorMsg)
                notifyManager!!.notify(
                    if (obj is Torrent) obj.hashCode() else (obj as Book).id.hashCode(),
                    builder.build()
                )
            }
        }
    }


    private fun updateNotification() {
        foregroundNotification!!.setContentTitle(getTitleNotifString())
        foregroundNotification!!.setStyle(notificationStyle())
        notifyManager!!.notify(serviceStartedNotifId, foregroundNotification!!.build())
    }


    private fun notificationStyle(): NotificationCompat.Style {
        val inboxStyle = NotificationCompat.InboxStyle()
        torrentHashMap.values.take(3).forEach {
            inboxStyle.addLine("(T)" + "·" + it.name + " · " + it.downloadSpeed.formatSpeed() + " · " + it.progress.formatProgress())
        }
        bookHashMap.values.take(3).forEach {
            inboxStyle.addLine("(F)" + "·" + it.name + " · " + it.speed.formatSpeed() + " · " + it.progress.formatProgress())
        }
        return inboxStyle
    }

    private fun getTitleNotifString() =
        getString(R.string.service_title_format).format(torrentHashMap.size, bookHashMap.size)

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        unregisterClass(this)
        super.onDestroy()
    }

}