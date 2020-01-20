package com.revolgenx.weaverx.fragment.book

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.revolgenx.weaverx.R

class BookFragment :Fragment(){

    companion object{
        fun newInstance() = BookFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.book_fragment_layout,container, false)
    }
}