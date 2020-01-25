package com.revolgenx.weaverx.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import androidx.core.graphics.ColorUtils
import libtorrent.Libtorrent
import kotlin.math.ceil
import kotlin.math.sqrt


class PiecesView(context: Context, attributeSet: AttributeSet?, defAttr: Int) :
    View(context, attributeSet, defAttr) {

    private var cells = 100
    private var mWidth = 0
    private var mHeight = 0
    private var cellWidth = 0f
    private var pieces = mutableListOf<Int>()
    private var unpended = Paint()
    private var empty = Paint()
    private var checking = Paint()
    private var partial = Paint()
    private var complete = Paint()
    private var writing = Paint()


    val ALL = intArrayOf(
        Libtorrent.PieceEmpty,
        Libtorrent.PieceComplete,
        Libtorrent.PieceChecking,
        Libtorrent.PiecePartial,
        Libtorrent.PieceWriting
    )

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attributeSet: AttributeSet?) : this(context, attributeSet, 0) {
        unpended.color = -0x555556 // mid between LTGRAY - GRAY
        empty.color = Color.parseColor("#e0e0e0")
        checking.color = Color.parseColor("#ffee58")
        partial.color = Color.parseColor("#9ccc65")
        writing.color = Color.parseColor("#ffca28")
        complete.color = Color.parseColor("#212121")

//        if (isInEditMode) {
//            for (i in 0 until cells * cells - 10) {
//                val s = ALL[(Math.random() * ALL.totalSize).toInt()]
//                pieces.add(s)
//            }
//        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        mWidth = MeasureSpec.getSize(widthMeasureSpec) - paddingLeft - paddingRight
        mHeight = MeasureSpec.getSize(heightMeasureSpec) - paddingLeft - paddingRight
        setMeasuredDimension(widthMeasureSpec, heightMeasureSpec)
    }

    override fun onDraw(canvas: Canvas?) {
        var paint: Paint = empty

        if(pieces.isEmpty()) return

        cellWidth = mWidth / cells.toFloat()
        for ((pos, i) in (0 until cells).withIndex()) {
            when (pieces[pos]) {
                Libtorrent.PieceWriting -> paint = writing
                Libtorrent.PiecePartial -> paint = partial
                Libtorrent.PieceChecking -> paint = checking
                Libtorrent.PieceComplete -> paint = complete
                Libtorrent.PieceUnpended -> paint = unpended
                Libtorrent.PieceEmpty -> paint = empty
            }

            val left = i.toFloat() * cellWidth
            val right = left + cellWidth
            val bottom = height.toFloat()
            canvas!!.drawRect(left, 0.0f, right, bottom, paint)
        }
    }


    fun drawPieces(handle: Long) {
        if (handle == -1L) return
        if (!Libtorrent.metaTorrent(handle)) return

        var length = Libtorrent.torrentPiecesCount(handle)
        cells = ceil(sqrt(length.toDouble())).toInt()
        pieces.clear()
        length = Libtorrent.torrentPiecesCompactCount(handle, 1)

        for (i in 0 until length)
            pieces.add(Libtorrent.torrentPiecesCompact(handle, i))

        invalidate()
    }


}