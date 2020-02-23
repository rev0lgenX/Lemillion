package com.revolgenx.lemillion.activity

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.checkbox.checkBoxPrompt
import com.afollestad.materialdialogs.customview.customView
import com.revolgenx.lemillion.BuildConfig
import com.revolgenx.lemillion.R
import com.revolgenx.lemillion.core.preference.getCrashReportPref
import com.revolgenx.lemillion.core.preference.getThemePref
import com.revolgenx.lemillion.core.preference.setCrashReportPref
import com.revolgenx.lemillion.core.util.createLinkIntent
import com.revolgenx.lemillion.view.string
import kotlinx.android.synthetic.main.about_activity_layout.*
import mehdi.sakout.aboutpage.AboutPage
import mehdi.sakout.aboutpage.Element

class AboutActivity : AppCompatActivity() {

    private val aboutLayout by lazy {
        AboutPage(this)
            .setImage(R.mipmap.ic_launcher)
            .setCustomFont("fonts/open_sans_regular.ttf")
            .setDescription(string(R.string.about_lemillion))
            .addGroup(string(R.string.general_information))
            .addItem(
                Element().setTitle(
                    getString(
                        R.string.version,
                        BuildConfig.VERSION_NAME + "." + BuildConfig.VERSION_CODE
                    )
                )
            )
            .addPlayStore("com.revolgenx.lemillion")
            .addGroup(string(R.string.additional_info))
            .addItem(Element().setTitle(string(R.string.change_log)).setIconDrawable(R.drawable.ic_new).setOnClickListener {

            })
            .addItem(Element().setTitle(string(R.string.help_translation)).setIconDrawable(R.drawable.ic_translate).setIntent(
               createLinkIntent("https://github.com/rev0lgenX/lemillion-lang")
            ))
            .addGroup(string(R.string.legal_information))
            .addItem(
                Element().setTitle(string(R.string.privacy_policy)).setIntent(
                    createLinkIntent("https://rev0lgenx.github.io/lemillion.github.io/")
                ).setIconDrawable(R.drawable.ic_lock)
            )
            .addItem(Element().setTitle(string(R.string.legal)).setOnClickListener {
                MaterialDialog(this).show {
                    title(R.string.legal)
                    customView(view = legalPage)
                }
            }.setIconDrawable(R.drawable.ic_lock))
            .addGroup(string(R.string.reports))
            .addItem(
                Element(
                    string(R.string.send_report),
                    R.drawable.ic_report
                ).setOnClickListener {
                    MaterialDialog(this).show {
                        title(R.string.send_report)
                        checkBoxPrompt(
                            R.string.send_crash_report,
                            isCheckedDefault = getCrashReportPref(context)
                        ) {
                            setCrashReportPref(context, it)
                        }
                    }
                })
            .create()
    }

    private val legalPage by lazy {
        AboutPage(this)
            .setDescription(string(R.string.agreement))
            .addGroup(string(R.string.license))
            .addWebsite(
                "https://github.com/aldenml/libtorrent4j/blob/master/LICENSE.md",
                string(R.string.libtorrent4j_license)
            )
            .addWebsite(
                "https://github.com/AriaLyy/Aria/blob/master/LICENSE",
                string(R.string.aria_license)
            )
            .create().also { it.findViewById<ImageView>(R.id.image)?.visibility = View.GONE }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val themePref = getThemePref(this)
        if (themePref == 1) {
            setTheme(R.style.BaseTheme_Dark)
        }
        setContentView(R.layout.about_activity_layout)
        setSupportActionBar(aboutToolbar)
        supportActionBar!!.title = string(R.string.about)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        aboutContainer.addView(aboutLayout)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finish()
            }
        }
        return super.onOptionsItemSelected(item)
    }
}
