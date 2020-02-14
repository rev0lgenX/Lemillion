package com.revolgenx.lemillion.activity

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.TypedValue
import android.view.Menu
import android.view.MenuItem
import androidx.annotation.ColorInt
import androidx.appcompat.view.menu.MenuBuilder
import androidx.appcompat.widget.AppCompatDrawableManager
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.net.toUri
import androidx.core.view.iterator
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.viewpager.widget.ViewPager
import com.afollestad.materialdialogs.LayoutMode
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.bottomsheets.BottomSheet
import com.arialyy.aria.util.CheckUtil
import com.google.android.material.snackbar.Snackbar
import com.karumi.dexter.Dexter
import com.karumi.dexter.listener.multi.SnackbarOnAnyDeniedMultiplePermissionsListener
import com.leinardi.android.speeddial.SpeedDialActionItem
import com.obsez.android.lib.filechooser.ChooserDialog
import com.revolgenx.lemillion.R
import com.revolgenx.lemillion.core.preference.getThemePref
import com.revolgenx.lemillion.core.preference.setSorting
import com.revolgenx.lemillion.core.preference.storagePath
import com.revolgenx.lemillion.core.sorting.BaseSorting
import com.revolgenx.lemillion.core.sorting.book.BookSorting
import com.revolgenx.lemillion.core.sorting.book.BookSortingComparator
import com.revolgenx.lemillion.core.sorting.torrent.TorrentSorting
import com.revolgenx.lemillion.core.sorting.torrent.TorrentSortingComparator
import com.revolgenx.lemillion.core.torrent.TorrentEngine
import com.revolgenx.lemillion.core.util.*
import com.revolgenx.lemillion.dialog.*
import com.revolgenx.lemillion.event.OnBackPressed
import com.revolgenx.lemillion.event.ShutdownEvent
import com.revolgenx.lemillion.event.TorrentEngineEvent
import com.revolgenx.lemillion.event.TorrentEngineEventTypes
import com.revolgenx.lemillion.fragment.BaseRecyclerFragment
import com.revolgenx.lemillion.fragment.book.BookFragment
import com.revolgenx.lemillion.fragment.torrent.TorrentFragment
import com.revolgenx.lemillion.view.makePagerAdapter
import com.revolgenx.lemillion.view.makeToast
import kotlinx.android.synthetic.main.activity_main.*
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.koin.android.ext.android.inject

class MainActivity : AppCompatActivity() {

    private var adapter: FragmentPagerAdapter? = null

    @ColorInt
    var iconColorInverse: Int = 0
    @ColorInt
    var iconColor: Int = 0

    private val queryKey = "query_key"
    private val uriKey = "uri_key"
    private var query = ""
    private var uri: Uri? = null
    private val engine by inject<TorrentEngine>()

    private val pageChangeListener = object : ViewPager.OnPageChangeListener {
        override fun onPageScrollStateChanged(state: Int) {
        }

        override fun onPageScrolled(
            position: Int,
            positionOffset: Float,
            positionOffsetPixels: Int
        ) {
        }

        override fun onPageSelected(position: Int) {
            getTorrentTab().onPageSelected()
            getBookTab().onPageSelected()
            if (mainToolbar.hasExpandedActionView()) {
                mainToolbar.collapseActionView()
            }
            query = ""
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        registerClass(this)

        val themePref = getThemePref(this)
        if (themePref == 1) {
            setTheme(R.style.BaseTheme_Dark)
        }
        setContentView(R.layout.activity_main)
        checkPermission()
        setSupportActionBar(mainToolbar)
        supportActionBar?.title = getString(R.string.app_name)

        val a = TypedValue()
        theme.resolveAttribute(R.attr.iconColorInverse, a, true)
        iconColorInverse = a.data

        theme.resolveAttribute(R.attr.iconColor, a, true)
        iconColor = a.data

        initSpeedDial(iconColor, iconColorInverse)

        adapter = makePagerAdapter(
            listOf(
                TorrentFragment.newInstance(),
                BookFragment.newInstance()
            )
        )
        mainTabLayout.setupWithViewPager(viewPager)
        viewPager.currentItem = 0
        viewPager.adapter = adapter
        viewPager.addOnPageChangeListener(pageChangeListener)


        if (intent.data != null) {
            uri = intent.data
        } else if (intent.hasExtra("uri")) {
            uri = intent.getParcelableExtra("uri")
        }

        //TODO:SEARCH
        savedInstanceState?.let {
            it.getString(queryKey)?.let {
                query = it
                if (query.isNotEmpty()) {

                }
            }

//            uri = it.getParcelable(uriKey)
//            initDecode()
        }

    }

    private fun initDecode() {
        if (uri == null) return

        when (uri!!.scheme) {
            MAGNET_PREFIX -> {
                AddTorrentBottomSheetDialog.newInstance(uri!!)
                    .show(supportFragmentManager, "add_torrent_bottom_sheet_dialog")
            }
            FILE_PREFIX -> {
                AddTorrentBottomSheetDialog.newInstance(uri!!)
                    .show(supportFragmentManager, "add_torrent_bottom_sheet_dialog")
            }

            HTTP_PREFIX, HTTPS_PREFIX -> {
                if (CheckUtil.checkUrl(uri.toString())) {
                    AddBookBottomSheetDialog.newInstance(uri.toString())
                        .show(supportFragmentManager, "add_book_fragment_tag")

                } else {
                    showErrorDialog(getString(R.string.invalid_url))
                }
            }

            CONTENT_PREFIX -> {
                AddTorrentBottomSheetDialog.newInstance(uri!!)
                    .show(supportFragmentManager, "add_torrent_bottom_sheet_dialog")
            }
            else -> {
                showErrorDialog(getString(R.string.unsupported_format))
            }
        }

        intent.replaceExtras(Bundle())
        intent.removeExtra("uri")
        intent.action = ""
        intent.data = null
        intent.flags = 0
    }


    private fun initSpeedDial(iconColor: Int, iconColorInverse: Int) {
        speedDialView.apply {
            this.overlayLayout?.setOnClickListener {
                this.close(true)
            }

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
                            openFileChooserForLib()
                        }
                        true
                    }

                    R.id.magnetId -> {
                        close()
                        if (checkPermission()) {
                            openLibtorrentMagnetDialog()
                        }
                        true
                    }

                    R.id.linkId -> {
                        close()
                        if (checkPermission()) {
                            openLinkInputDialog()
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

    private fun openLibtorrentMagnetDialog() {
        MaterialDialog(this, BottomSheet(LayoutMode.WRAP_CONTENT)).show {
            title(R.string.magnet_link)
            inputDialog(this@MainActivity) { _, text ->
                AddTorrentBottomSheetDialog.newInstance(text.toString().toUri())
                    .show(supportFragmentManager, "add_torrent_bottom_sheet_dialog")
                dismiss()
            }

            noAutoDismiss()
            positiveButton(R.string.ok)
            negativeButton(R.string.cancel) {
                dismiss()
            }
        }
    }


    private fun openFileChooserForLib() {
        ChooserDialog(
            this,
            if (getThemePref(this) == 1) R.style.FileChooserStyle_Dark else R.style.CustomFileChooserStyle
        )
            .withFilter(false, false, "torrent")
            .withStartFile(storagePath(this)!!)
            .withChosenListener { dir, dirFile ->
                if (dirFile.extension != "torrent") {
                    makeToast(resId = R.string.not_torrent_file_extension)
                    return@withChosenListener
                }
                AddTorrentBottomSheetDialog.newInstance(dirFile.toUri())
                    .show(supportFragmentManager, "add_torrent_bottom_sheet_dialog")
            }
            .withResources(R.string.choose_a_file, R.string.done, R.string.cancel)
            .titleFollowsDir(true)
            .withFileIcons(
                false,
                AppCompatDrawableManager.get().getDrawable(
                    this,
                    R.drawable.ic_file
                ).also { DrawableCompat.setTint(it, iconColor) },
                AppCompatDrawableManager.get().getDrawable(
                    this,
                    R.drawable.ic_folder
                ).also { DrawableCompat.setTint(it, iconColor) }
            )
            .enableOptions(true)
            .build()
            .show()

    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(queryKey, query)
        outState.putParcelable(uriKey, uri)
        super.onSaveInstanceState(outState)
    }


    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        this.menuInflater.inflate(R.menu.main_menu, menu)

        if (menu is MenuBuilder) {
            menu.setOptionalIconsVisible(true)
        }

        menu?.findItem(R.id.search_menu)?.let { item ->
            (item.actionView as SearchView).setOnQueryTextListener(object :
                SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean {
                    return false
                }

                override fun onQueryTextChange(newText: String?): Boolean {
                    query = newText ?: ""
                    val fragment = getCurrentTab()
                    (fragment as BaseRecyclerFragment<*, *>).search(query)
                    return true
                }
            })
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
        return when (item.itemId) {
            R.id.search_menu -> {
                true
            }

            R.id.resume_all_menu -> {
                val currentTab = getCurrentTab()
                (currentTab as BaseRecyclerFragment<*, *>).resumeAll()
                true
            }

            R.id.pause_all_menu -> {
                val currentTab = getCurrentTab()
                (currentTab as BaseRecyclerFragment<*, *>).pauseAll()
                true
            }

            R.id.sort_menu -> {
                makeSortDialog { str, index ->
                    val sort = str.split(" ")
                    val currentTab = getCurrentTab()

                    if (currentTab is TorrentFragment) {
                        TorrentSortingComparator(
                            TorrentSorting(
                                TorrentSorting.SortingColumns.fromValue(sort[0])
                                , BaseSorting.Direction.fromValue(sort[1])
                            )
                        ).let {
                            currentTab.sort(it)
                            setSorting(this, index)
                        }
                    } else if (currentTab is BookFragment) {
                        BookSortingComparator(
                            BookSorting(
                                BookSorting.SortingColumns.fromValue(sort[0])
                                , BaseSorting.Direction.fromValue(sort[1])
                            )
                        ).let {
                            currentTab.sort(it)
                            setSorting(this, index)
                        }
                    }
                }
                true
            }


            R.id.settings_menu -> {
                makeSettingDialog()
                true
            }
            R.id.exit -> {
                postEvent(ShutdownEvent())
                true
            }
            else -> {
                false
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onShutdownEvent(event: ShutdownEvent) {
        val intent = Intent(Intent.ACTION_MAIN)
        intent.addCategory(Intent.CATEGORY_HOME)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        startActivity(intent)
        finish()
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onTorrentEngineEvent(event: TorrentEngineEvent) {
        when (event.engineEventTypes) {
            TorrentEngineEventTypes.ENGINE_STARTED -> {
                initDecode()
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


    override fun onDestroy() {
        unregisterClass(this)
        super.onDestroy()
    }

    override fun onBackPressed() {
        postEvent(OnBackPressed())
        super.onBackPressed()
    }


}
