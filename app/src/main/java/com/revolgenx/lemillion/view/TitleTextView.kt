package com.revolgenx.lemillion.view

import android.content.Context
import android.content.res.ColorStateList
import android.os.Build
import android.text.TextUtils
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import com.revolgenx.lemillion.R
import com.revolgenx.lemillion.core.util.dip

typealias ImageCallback = (() -> Unit)?

class TitleTextView(context: Context, attributeSet: AttributeSet?, defStyle: Int) :
    RelativeLayout(context, attributeSet, defStyle) {

    private var showDrawable = false
    @ColorInt
    private var iconColor = 0
    private var imageCallback: ImageCallback = null
    private var imageResource: Int = 0

    constructor(context: Context, attributeSet: AttributeSet?) : this(context, attributeSet, 0) {
        @ColorInt val textColor = TypedValue().apply {
            context.theme.resolveAttribute(android.R.attr.textColorPrimary, this, true)
        }.let { ContextCompat.getColor(context, it.resourceId) }

        iconColor = TypedValue().apply {
            context.theme.resolveAttribute(R.attr.iconColor, this, true)
        }.data

        var titleName = ""
        attributeSet?.let {
            context.theme.obtainStyledAttributes(it, R.styleable.TitleTextView, 0, 0)
                .apply {
                    titleName = getString(R.styleable.TitleTextView_titleName) ?: ""
                    showDrawable = getBoolean(R.styleable.TitleTextView_showDrawable, false)
//                    if (showDrawable)
                    imageResource =
                        getResourceId(R.styleable.TitleTextView_descriptionDrawable, 0)
                }
        }

//        if (showDrawable)
        AppCompatImageView(context).let { iv ->
            iv.id = R.id.descriptionImageView
            iv.layoutParams =
                LayoutParams(dip(20), ViewGroup.LayoutParams.WRAP_CONTENT).also { params ->
                    params.addRule(
                        ALIGN_PARENT_RIGHT
                    )
                    params.addRule(
                        CENTER_IN_PARENT
                    )
                }

            if (imageResource != 0) {
                iv.setImageResource(imageResource)
            } else {
                iv.visibility = View.GONE
            }
            iv.visibility = if (showDrawable) View.VISIBLE else View.GONE

            iv.supportImageTintList = ColorStateList.valueOf(iconColor)
            iv.setOnClickListener {
                imageCallback?.invoke()
            }
            addView(iv)
        }


        TextView(context)
            .let {
                it.id = R.id.customTextViewTitleId
                it.layoutParams = LayoutParams(
                    LayoutParams.MATCH_PARENT,
                    LayoutParams.WRAP_CONTENT
                ).also { params ->
                    if (showDrawable)
                        params.addRule(LEFT_OF, R.id.descriptionImageView)
                }


                if (isInEditMode && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    it.typeface = resources.getFont(R.font.open_sans_light)
                else it.typeface = ResourcesCompat.getFont(context, R.font.open_sans_light)

                it.setTextColor(textColor)
                it.textSize = 12f
                it.text = titleName
                addView(it)
            }

        TextView(context)
            .let {
                it.id = R.id.customTextViewDescriptionId
                it.layoutParams = LayoutParams(
                    LayoutParams.MATCH_PARENT,
                    LayoutParams.WRAP_CONTENT
                ).also { params ->
                    if (showDrawable)
                        params.addRule(LEFT_OF, R.id.descriptionImageView)
                    params.addRule(BELOW, R.id.customTextViewTitleId)
                }
                it.setTextColor(textColor)
                if (isInEditMode && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    it.typeface = resources.getFont(R.font.open_sans_regular)
                else it.typeface = ResourcesCompat.getFont(context, R.font.open_sans_regular)

                if (isInEditMode) it.text = "text test description"
                it.maxLines = 2
                it.ellipsize = TextUtils.TruncateAt.END
                it.textSize = 16f
                addView(it)
            }
    }

    constructor(context: Context) : this(context, null, 0)

    fun titleTextView() = findViewById<TextView>(R.id.customTextViewTitleId)
    fun descriptionTextView() = findViewById<TextView>(R.id.customTextViewDescriptionId)

    var description = ""
        set(value) {
            field = value
            descriptionTextView().text = value
        }

    fun setImageDrawable(@DrawableRes resId: Int = 0, imageCallback: ImageCallback = null) {
        if (!showDrawable) return

        this.imageCallback = imageCallback

        if (resId == 0) return
        findViewById<AppCompatImageView>(R.id.descriptionImageView).setImageResource(resId)
    }

    fun setDrawableClickListener(imageCallback: ImageCallback = null) {
        this.imageCallback = imageCallback
    }

    fun showDrawable(b: Boolean) {
        findViewById<AppCompatImageView>(R.id.descriptionImageView)?.let {
            it.visibility = if (b) View.VISIBLE else View.GONE
        }
    }
}
