package com.revolgenx.lemillion.activity

import android.os.Bundle
import android.util.TypedValue
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.menu.MenuBuilder
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.iterator
import androidx.viewpager.widget.ViewPager
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.WhichButton
import com.afollestad.materialdialogs.actions.setActionButtonEnabled
import com.afollestad.materialdialogs.customview.customView
import com.afollestad.materialdialogs.customview.getCustomView
import com.afollestad.materialdialogs.utils.MDUtil.textChanged
import com.google.android.material.appbar.AppBarLayout
import com.revolgenx.lemillion.R
import com.revolgenx.lemillion.core.preference.getThemePref
import com.revolgenx.lemillion.core.torrent.Torrent
import com.revolgenx.lemillion.core.util.pmap
import com.revolgenx.lemillion.core.util.postEvent
import com.revolgenx.lemillion.dialog.inputDialog
import com.revolgenx.lemillion.dialog.showInputDialog
import com.revolgenx.lemillion.event.TorrentRecheckEvent
import com.revolgenx.lemillion.fragment.torrent.meta.*
import com.revolgenx.lemillion.view.makePagerAdapter
import com.revolgenx.lemillion.view.makeToast
import kotlinx.android.synthetic.main.speed_layout_view.*
import kotlinx.android.synthetic.main.speed_layout_view.view.*
import kotlinx.android.synthetic.main.toolbar_layout.*
import kotlinx.android.synthetic.main.torrent_meta_layout.*
import kotlinx.coroutines.runBlocking
import org.libtorrent4j.AnnounceEntry

class TorrentMetaActivity : AppCompatActivity() {


    companion object {
        const val torrentKey = "torrent_key"
    }

    private lateinit var torrent: Torrent


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val themePref = getThemePref(this)
        if (themePref == 1) {
            setTheme(R.style.BaseTheme_Dark)
        }
        setContentView(R.layout.torrent_meta_layout)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.meta)

        if (!intent.hasExtra(torrentKey)) return

        torrent = intent.getParcelableExtra(torrentKey) ?: return
        if (torrent.handle == null) return

        val viewPager = ViewPager(this)
        viewPager.layoutParams = CoordinatorLayout.LayoutParams(
            CoordinatorLayout.LayoutParams.MATCH_PARENT,
            CoordinatorLayout.LayoutParams.MATCH_PARENT
        ).also { layoutParams ->
            layoutParams.behavior =
                AppBarLayout.ScrollingViewBehavior()
        }
        viewPager.id = R.id.viewPager

        viewPager.offscreenPageLimit = 3
        metaLayout.addView(viewPager)
        viewPager.adapter =
            makePagerAdapter(
                listOf(
                    TorrentBaseMetaFragment.newInstance(
                        torrent,
                        TorrentMetaFragment::class.java
                    ), TorrentBaseMetaFragment.newInstance(
                        torrent,
                        TorrentFileFragment::class.java
                    ), TorrentBaseMetaFragment.newInstance(
                        torrent,
                        TorrentTrackerFragment::class.java
                    ), TorrentBaseMetaFragment.newInstance(
                        torrent,
                        TorrentPeerFragment::class.java
                    )
                )
            )

        tabLayout.setupWithViewPager(viewPager)
        viewPager.currentItem = 0

        if (savedInstanceState != null) {
            torrent.addEngineListener()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        this.menuInflater.inflate(R.menu.torrent_meta_menu, menu)
        val a = TypedValue()
        theme.resolveAttribute(R.attr.iconColor, a, true)
        val iconColor = a.data

        if (menu is MenuBuilder) {
            menu.setOptionalIconsVisible(true)
        }

        menu?.iterator()?.forEach {
            if (it.icon != null)
                DrawableCompat.wrap(it.icon).apply {
                    DrawableCompat.setTint(this, iconColor)
                    it.icon = this
                }
        }

        return true
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }

        if (!::torrent.isInitialized) return false

        return when (item.itemId) {
            R.id.recheckItem -> {
                postEvent(TorrentRecheckEvent(listOf(torrent.hash)))
                true
            }
            R.id.reannounceItem -> {
                torrent.forceReannounce()
                true
            }
            R.id.speedLimitItem -> {
                makeSpeedLimitDialog(
                    torrent.uploadLimit,
                    torrent.downloadLimit
                ) { upload, download ->
                    torrent.uploadLimit = upload * 1024
                    torrent.downloadLimit = download * 1024
                }
                true
            }
            R.id.addTrackerItem -> {
                MaterialDialog(this).show {
                    inputDialog(
                        this@TorrentMetaActivity,
                        hintRes = R.string.add_space_between_trackers
                    ) { materialDialog, it ->
                        runBlocking {
                            it.toString().split(" ").pmap { AnnounceEntry(it) }
                                .let { torrent.torrentAddTracker(it) }
                        }
                    }
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onDestroy() {

        if (::torrent.isInitialized && torrent.handle?.isValid == true) {
            torrent.removeEngineListener()
        }

        super.onDestroy()

    }


    private fun makeSpeedLimitDialog(
        preUpload: Int,
        preDownload: Int,
        callback: (upload: Int, download: Int) -> Unit
    ) {
        val dialog = MaterialDialog(this).customView(R.layout.speed_layout_view)
        dialog.title(R.string.speed_limit_size_format)
        val customView = dialog.getCustomView()

        customView.uploadSpeedEt.apply {
            setText((preUpload / 1024).toString())
            textChanged {
                dialog.setActionButtonEnabled(
                    WhichButton.POSITIVE,
                    it.isNotEmpty() && customView.downloadSpeedEt.text.isNotEmpty()
                )
            }
        }

        customView.downloadSpeedEt.apply {
            setText((preDownload / 1024).toString())
            textChanged {
                dialog.setActionButtonEnabled(
                    WhichButton.POSITIVE,
                    it.isNotEmpty() && customView.uploadSpeedEt.text.isNotEmpty()
                )
            }
        }
        dialog.show {
            cancelOnTouchOutside(false)
            positiveButton {
                try {
                    val upload = customView.uploadSpeedEt.text.toString().toInt()
                    val download = customView.downloadSpeedEt.text.toString().toInt()
                    callback.invoke(upload, download)
                } catch (e: NumberFormatException) {
                    makeToast("invalid number")
                }
            }
            negativeButton {

            }
        }

    }


}
