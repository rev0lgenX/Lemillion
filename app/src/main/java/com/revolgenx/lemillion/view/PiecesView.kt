package com.revolgenx.lemillion.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import androidx.annotation.Nullable
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import com.revolgenx.lemillion.R

class PiecesView(context: Context, attrs: AttributeSet?, defStyleAttr: Int) :
    View(context, attrs, defStyleAttr) {
    private val empty: Paint = Paint()
    private val complete: Paint = Paint()
    private val partialPaint: Paint = Paint()
    private var cells = 0
    private var cellWidth = 0f
    private var mWidth = 0
    private var mHeight = 0
    private var cellPerPixel = 0f
    private var hsl: FloatArray = floatArrayOf(197.0f, 0.991f, 0f)
    private var pieces: BooleanArray = booleanArrayOf()

    constructor(context: Context) : this(context, null)
    constructor(context: Context, @Nullable attrs: AttributeSet?) : this(context, attrs, 0) {
        empty.color = ContextCompat.getColor(context, R.color.empty_piece)
        complete.color = ContextCompat.getColor(context, R.color.complete_piece)

        if (isInEditMode) {
            val list = listOf(true, false)
            pieces = BooleanArray(1000)
            for (i in 0 until 1000) {
                pieces[i] = list.random()
            }
            cells = pieces.size
        }
    }


    fun drawPieces(pieces: BooleanArray) {
        if (pieces.isEmpty()) {
            return
        }
        this.pieces = pieces
        cells = pieces.size
        if (cells == pieces.size) {
            postInvalidate()
        } else {
            requestLayout()
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        mWidth = MeasureSpec.getSize(widthMeasureSpec) - paddingLeft - paddingRight
        mHeight = MeasureSpec.getSize(heightMeasureSpec) - paddingLeft - paddingRight
        setMeasuredDimension(widthMeasureSpec, heightMeasureSpec)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (cells <= mWidth) {
            cellWidth = mWidth / cells.toFloat()
            for (i in 0 until cells) {
                val paint: Paint = if (pieces[i]) complete else empty
                val left = i * cellWidth
                val right = left + cellWidth
                canvas.drawRect(
                    left, 0.0f,
                    right, mHeight.toFloat(), paint
                )
            }
        } else {
            cellPerPixel = cells / mWidth.toFloat()
            for (i in 0 until mWidth) {
                val beginPiece = (cellPerPixel * i).toInt()
                val endPiece = (beginPiece + cellPerPixel).toInt()
                var pieceSum = 0
                for (piecePosition in beginPiece until endPiece) {
                    if (pieces[piecePosition]) pieceSum++
                }
                hsl[2] = (0.447f + 0.553 * (0.98 - pieceSum / cellPerPixel)).toFloat()
                partialPaint.color = ColorUtils.HSLToColor(hsl)
                canvas.drawRect(
                    i.toFloat(), 0.0f,
                    mWidth.toFloat(), mHeight.toFloat(), partialPaint
                )
            }
        }
    }
}