package com.revolgenx.lemillion.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import com.arialyy.aria.core.Aria
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.revolgenx.lemillion.R
import com.revolgenx.lemillion.fragment.book.BookFragment

class BookMetaBottomSheetDialog : BottomSheetDialogFragment() {

    private val bookKey = "book_key"

    companion object {
        fun newInstance(book: BookFragment) = BookMetaBottomSheetDialog().apply {
            arguments = bundleOf(bookKey to book)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.book_meta_bottom_sheet_dialog_layout, container, false)
    }


    override fun onDestroy() {
        super.onDestroy()
    }

}
