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
import com.revolgenx.lemillion.core.torrent.Torrent
import com.revolgenx.lemillion.core.torrent.TorrentEngine
import com.revolgenx.lemillion.core.util.registerClass
import com.revolgenx.lemillion.core.util.unregisterClass
import com.revolgenx.lemillion.event.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.koin.android.ext.android.inject
import timber.log.Timber

class MainService : Service() {

    private val binder = LocalBinder()
    private var notifyManager: NotificationManager? = null

    private val serviceStartedNotifId: Int = 1
    private val foregroundChanId = "revolgenx.com.weaverx.FOREGROUND_DEFAULT_CHAN_ID"
    private val defChanId = "revolgenx.com.weaverx.DEFAULT_CHAN_ID"
    private var foregroundNotify: NotificationCompat.Builder? = null
    private val torrentUpdateTime = 1000L
    private val saveTimeDelay = 5000L
    private val handler = Handler()

    val torrentHashMap = mutableMapOf<String, Torrent>()
    val bookHashMap = mutableMapOf<Long, Book>()
    private val torrentEngine by inject<TorrentEngine>()
    private val torrentRepository by inject<TorrentRepository>()
    private val bookRepository by inject<BookRepository>()

//    private val runnable = object : Runnable {
//        override fun run() {
//            if (torrentHashMap.isEmpty()) return
//
//            synchronized(torrentHashMap) {
//                val torrents = torrentHashMap.values.iterator()
//
//                torrents.forEach { torrent ->
//                    when (torrent.status) {
//                        TorrentStatus.SEEDING, TorrentStatus.DOWNLOADING, TorrentStatus.QUEUE, TorrentStatus.CHECKING -> {
//                            torrent.update()
//                        }
//                        else -> {
////                        torrentHashMap.remove(torrent.hash)
//                            torrents.remove()
//                            torrent.update()
//                            CoroutineScope(Dispatchers.IO).launch {
//                                torrentRepository.update(torrent)
//                            }
//                        }
//                    }
//                    postEvent(UpdateTorrentEvent(torrent.hash))
//                }
//
//                checkIfServiceIsEmpty()
//                handler.postDelayed(this, torrentUpdateTime)
//            }
//
//        }
//    }

    private val runnable = object : Runnable {
        override fun run() {
            checkIfServiceIsEmpty()
            handler.postDelayed(this, 2000)
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
        notifyManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager?
        makeNotifyChans(notifyManager)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Timber.d("onstartcommand")
        registerClass(this)

        try {
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.cancelAll()
        } catch (e: SecurityException) {

        }

        makeForegroundNotify()
        return START_NOT_STICKY
    }


    //TODO://CHECK FOR DIFFERENT EVENTS
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun torrentEvent(event: TorrentEvent) {
        handler.postDelayed(runnable, 2000)
        when (event.type) {
            TorrentEventType.TORRENT_RESUMED -> {
                synchronized(torrentHashMap) {
                    event.torrents.forEach { torrent ->
                        torrentHashMap[torrent.hash] = torrent
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
                //todo:send notification
                synchronized(torrentHashMap) {
                    CoroutineScope(Dispatchers.IO).launch {
                        torrentRepository.updateAll(event.torrents)
                    }
                }
            }
            TorrentEventType.TORRENT_ERROR -> {
                //todo:send notification
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
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onTorrentRemovedEvent(event: TorrentRemovedEvent) {
        event.hashes.forEach { torrentHashMap.remove(it) }
        checkIfServiceIsEmpty()
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onBookEvent(event: BookEvent) {
        handler.postDelayed(runnable, 2000)
        when (event.bookEventType) {
            BookEventType.BOOK_RESUMED -> {
                synchronized(bookHashMap) {
                    event.books.forEach { book ->
                        bookHashMap[book.entity!!.id] = book
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
                //todo:notification
                synchronized(bookHashMap) {
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
                //todo:notification
                event.books.forEach {
                    removeBook(it.id)
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
//        Libtorrent.pause()
        torrentHashMap.values.forEach { it.pause() }
        torrentHashMap.clear()
        Aria.download(this).stopAllTask()
        bookHashMap.clear()
        stopService()
    }


    private fun removeBook(id: Long) {
        bookHashMap.remove(id)
        checkIfServiceIsEmpty()
    }


    //TODO CHECK FOR FILES EMPTINESS
    private fun checkIfServiceIsEmpty() {
        val torrentIsEmpty = torrentHashMap.isEmpty()
        val bookIsEmtpy = bookHashMap.isEmpty()

        if (torrentIsEmpty) {
            handler.removeCallbacksAndMessages(null)
            Timber.d("stopping service empty torrent")
        }

        if (torrentIsEmpty && bookIsEmtpy) {
            stopService()
        }
    }


    private fun stopService() {
        CoroutineScope(Dispatchers.IO).launch {
            handler.removeCallbacksAndMessages(null)
            if (torrentHashMap.isNotEmpty()) {
                torrentHashMap.forEach {
                    val torrent = it.value
                    torrent.pause()
                    torrentRepository.update(torrent)
                }
            }
            stopSelf()
        }
    }

    override fun onLowMemory() {
        super.onLowMemory()

        stopService()
    }

    //notification
    private fun makeNotifyChans(notifyManager: NotificationManager?) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O)
            return

        val chans = mutableListOf<NotificationChannel>()
        val defaultChan = NotificationChannel(
            defChanId, "Default",
            NotificationManager.IMPORTANCE_DEFAULT
        )


        defaultChan.enableVibration(true)
        defaultChan.vibrationPattern = longArrayOf(1000) /* ms */


        defaultChan.enableLights(true)
        defaultChan.lightColor = Color.BLUE

        chans.add(defaultChan)
        chans.add(
            NotificationChannel(
                foregroundChanId, getString(R.string.foreground_notification),
                NotificationManager.IMPORTANCE_DEFAULT
            )
        )
        notifyManager?.createNotificationChannels(chans)
    }

    private fun makeForegroundNotify() {
        /* For starting main activity */
        val startupIntent = Intent(applicationContext, MainActivity::class.java)
        startupIntent.action = Intent.ACTION_MAIN
        startupIntent.addCategory(Intent.CATEGORY_LAUNCHER)
        startupIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)

        val startupPendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            startupIntent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        foregroundNotify = NotificationCompat.Builder(
            applicationContext,
            foregroundChanId
        )
            .setContentIntent(startupPendingIntent)
            .setContentTitle(getString(R.string.foreground_notification))
            .setTicker(getString(R.string.foreground_notification))
            .setWhen(System.currentTimeMillis())

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            foregroundNotify!!.setSmallIcon(R.mipmap.ic_launcher)
        } else {
            foregroundNotify!!.setSmallIcon(R.mipmap.ic_launcher)
        }

//        foregroundNotify?.addAction(makeFuncButtonAction())
//        foregroundNotify?.addAction(makeShutdownAction())

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            foregroundNotify!!.setCategory(Notification.CATEGORY_SERVICE)

        /* Disallow killing the service process by system */
        startForeground(serviceStartedNotifId, foregroundNotify!!.build())
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        unregisterClass(this)
        super.onDestroy()
    }

}