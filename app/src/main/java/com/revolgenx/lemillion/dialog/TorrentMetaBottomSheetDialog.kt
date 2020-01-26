package com.revolgenx.lemillion.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.revolgenx.lemillion.R
import kotlinx.android.synthetic.main.torrent_meta_bottom_sheet_dialog_layout.*

class TorrentMetaBottomSheetDialog : BottomSheetDialogFragment() {

    companion object{
        fun newInstance() = TorrentMetaBottomSheetDialog()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.torrent_meta_bottom_sheet_dialog_layout, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        generalViewTv.setOnClickListener {
            scrollViewLayout.smoothScrollTo(
                0,
                torrentGeneralLayout.y.toInt()
            )
        }

        filesViewTv.setOnClickListener {
            scrollViewLayout.smoothScrollTo(
                0
                , torrentFilesLayout.y.toInt()
            )
        }
    }


}