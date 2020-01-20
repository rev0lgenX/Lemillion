package com.revolgenx.weaverx.activity

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.TypedValue
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.fragment.app.FragmentStatePagerAdapter
import com.google.android.material.snackbar.Snackbar
import com.karumi.dexter.Dexter
import com.karumi.dexter.listener.multi.SnackbarOnAnyDeniedMultiplePermissionsListener
import com.leinardi.android.speeddial.SpeedDialActionItem
import com.revolgenx.weaverx.R
import com.revolgenx.weaverx.dialog.openFileChooser
import com.revolgenx.weaverx.dialog.openMagnetDialog
import com.revolgenx.weaverx.fragment.book.BookFragment
import com.revolgenx.weaverx.fragment.torrent.TorrentFragment
import kotlinx.android.synthetic.main.activity_main.*
import timber.log.Timber

class MainActivity : AppCompatActivity() {

    private var adapter: MainPagerAdapter? = null

    @ColorInt
    var iconColorInverse: Int = 0
    @ColorInt
    var iconColor: Int = 0


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(mainToolbar)
        supportActionBar?.title = getString(R.string.app_name)

        val a = TypedValue()
        theme.resolveAttribute(R.attr.iconColorInverse, a, true)
        iconColorInverse = a.data

        theme.resolveAttribute(R.attr.iconColor, a, true)
        iconColor = a.data

        initSpeedDial(iconColor, iconColorInverse)

        adapter = MainPagerAdapter(supportFragmentManager)
        mainTabLayout.setupWithViewPager(viewPager)
        viewPager.currentItem = 0
        viewPager.adapter = adapter

    }

    private fun initSpeedDial(iconColor: Int, iconColorInverse: Int) {
        speedDialView.apply {
            mainFabClosedIconColor =
                ContextCompat.getColor(context, R.color.colorPrimaryInverseDark)
            mainFabOpenedIconColor =
                ContextCompat.getColor(context, R.color.colorPrimaryInverseDark)
            mainFabOpenedBackgroundColor = ContextCompat.getColor(context, R.color.colorPrimary)
            mainFabClosedBackgroundColor = ContextCompat.getColor(context, R.color.colorPrimary)
            addActionItem(
                SpeedDialActionItem.Builder(
                    R.id.fileId,
                    R.drawable.ic_file
                )
                    .setFabImageTintColor(iconColor)
                    .setFabBackgroundColor(iconColorInverse)
                    .setLabel(R.string.add_torrent)
                    .setLabelBackgroundColor(iconColor)
                    .setLabelColor(iconColorInverse)
                    .create()
            )
            addActionItem(
                SpeedDialActionItem.Builder(
                    R.id.magnetId,
                    R.drawable.ic_magnet
                ).setFabImageTintColor(iconColor)
                    .setFabBackgroundColor(iconColorInverse)
                    .setLabel(R.string.add_magnet)
                    .setLabelBackgroundColor(iconColor)
                    .setLabelColor(iconColorInverse)
                    .create()
            )

            addActionItem(
                SpeedDialActionItem.Builder(
                    R.id.linkId,
                    R.drawable.ic_link
                ).setFabImageTintColor(iconColor)
                    .setFabBackgroundColor(iconColorInverse)
                    .setLabel(R.string.add_link)
                    .setLabelBackgroundColor(iconColor)
                    .setLabelColor(iconColorInverse)
                    .create()
            )

            setOnActionSelectedListener {
                when (it.id) {
                    R.id.fileId -> {
                        close()
                        if (checkPermission()) {
                            openFileChooser()
                        }
                        true
                    }

                    R.id.magnetId -> {
                        close()
                        if (checkPermission()) {
                            openMagnetDialog()
                        }
                        true
                    }

                    R.id.linkId -> {
                        close()
                        if (checkPermission()) {
//                            openPageComposerDialog { page, start ->
//                                getBookTab().addPage(page, start)
//                            }

//                            openPage2ComposerDialog { page ->
//                                Timber.d("page page ${page.name}")
//                                getBookTab().insertPage(page)
//                            }
                        }
                        true
                    }
                    else -> {
                        false
                    }
                }
            }
        }
    }


    private fun checkPermission(): Boolean {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                Dexter.withActivity(this)
                    .withPermissions(
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ).withListener(permissionsListener())
                    .check()
                return false
            }
        }
        return true
    }

    private fun permissionsListener() = SnackbarOnAnyDeniedMultiplePermissionsListener
        .Builder.with(
        coordinatorLayout,
        "Storage permission is required"
    ).withOpenSettingsButton("Settings")
        .withCallback(object : Snackbar.Callback() {
            override fun onShown(sb: Snackbar?) {

            }

            override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {

            }
        }).build()

    fun getCurrentTab() = getTabFragmentFromPos(viewPager.currentItem)
    fun getTorrentTab() = getTabFragmentFromPos(0) as TorrentFragment
    fun getBookTab() = getTabFragmentFromPos(1) as BookFragment

    private fun getTabFragmentFromPos(pos: Int): Fragment =
        supportFragmentManager.findFragmentByTag("android:switcher:${R.id.viewPager}:$pos")
            ?: Fragment()


    inner class MainPagerAdapter(fragmentManager: FragmentManager) :
        FragmentPagerAdapter(fragmentManager) {
        override fun getItem(position: Int): Fragment {
            return when (position) {
                0 -> {
                    TorrentFragment.newInstance()
                }
                1 -> {
                    BookFragment.newInstance()
                }
                else -> Fragment()
            }
        }

        override fun getCount(): Int = 2

        override fun getPageTitle(position: Int): CharSequence? = when (position) {
            0 -> getString(R.string.torrent)
            1 -> getString(R.string.file)
            else -> ""
        }
    }


}
