package com.revolgenx.lemillion.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.revolgenx.lemillion.R
import com.revolgenx.lemillion.core.book.Book
import com.revolgenx.lemillion.core.util.formatSize
import com.revolgenx.lemillion.view.copyToClipBoard
import kotlinx.android.synthetic.main.book_meta_bottom_sheet_dialog_layout.*
//TODO://EDIT NAME AND PATH
class BookMetaBottomSheetDialog : BottomSheetDialogFragment() {

    private val bookKey = "book_key"

    companion object {
        fun newInstance(book: Book) = BookMetaBottomSheetDialog().apply {
            arguments = bundleOf(bookKey to book)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.book_meta_bottom_sheet_dialog_layout, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val book = arguments?.getParcelable<Book>(bookKey) ?: return
        val entity = book.entity ?: return

        bookNameTv.description = book.name
        bookUrlTv.description = if (entity.isRedirect) {
            entity.redirectUrl
        } else {
            entity.url
        }
        bookPathTv.description = entity.filePath
        booktotalSizeTv.description = book.totalSize.formatSize()

        bookUrlTv.setDrawableClickListener {
            context!!.copyToClipBoard(bookUrlTv.description)
        }

    }


    override fun onDestroy() {
        super.onDestroy()
    }

}
