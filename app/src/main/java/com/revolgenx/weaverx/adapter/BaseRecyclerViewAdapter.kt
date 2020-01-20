package com.revolgenx.weaverx.adapter


import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

abstract class BaseRecyclerAdapter<VH : RecyclerView.ViewHolder, T : Any>(diffUtil: DiffUtil.ItemCallback<T>) : ListAdapter<T, VH>(diffUtil) {

    override fun getItemCount(): Int {
        return currentList.size
    }

//    fun submitList(list: List<T>) {
//        currentList.clear()
//        currentList.addAll(list)
//        notifyDataSetChanged()
//    }
//
//    fun submitItem(item: T) {
//        currentList.add(item)
//        notifyDataSetChanged()
//    }
//
//    fun updateItem(item: T, index: Int) {
//        currentList[index] = item
//        notifyDataSetChanged()
//    }
//
//    fun getItem(index: Int): T {
//        return currentList[index]
//    }

}