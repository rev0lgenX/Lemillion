package com.revolgenx.lemillion.activity

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.viewpager.widget.ViewPager
import com.google.android.material.appbar.AppBarLayout
import com.revolgenx.lemillion.R
import com.revolgenx.lemillion.core.preference.getThemePref
import com.revolgenx.lemillion.core.torrent.Torrent
import com.revolgenx.lemillion.fragment.torrent.meta.*
import com.revolgenx.lemillion.view.makePagerAdapter
import kotlinx.android.synthetic.main.toolbar_layout.*
import kotlinx.android.synthetic.main.torrent_meta_layout.*

class TorrentMetaActivity : AppCompatActivity() {


    companion object {
        val torrentKey = "torrent_key"
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
                    ) , TorrentBaseMetaFragment.newInstance(
                        torrent,
                        TorrentPeerFragment::class.java
                    )
                )
            )

        tabLayout.setupWithViewPager(viewPager)
        viewPager.currentItem = 0

        if(savedInstanceState != null){
            torrent.addEngineListener()
        }
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onDestroy() {

        if(::torrent.isInitialized && torrent.handle?.isValid == true){
            torrent.removeEngineListener()
        }

        super.onDestroy()

    }


}
