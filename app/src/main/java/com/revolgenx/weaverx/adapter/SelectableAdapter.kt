package com.revolgenx.weaverx.adapter

import android.util.SparseBooleanArray
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView

abstract class SelectableAdapter<VH : RecyclerView.ViewHolder, T : Any>(diffUtil: DiffUtil.ItemCallback<T>) :
    BaseRecyclerAdapter<VH, T>(diffUtil) {


    val selectedItemCount: Int
        get() = selectedItems.size()

    private val selectedItems: SparseBooleanArray by lazy { SparseBooleanArray() }

    fun isSelected(position: Int): Boolean {
        return getSelectedItems().contains(position)
    }

    fun toggleSelection(position: Int) {
        if (selectedItems.get(position, false)) {
            selectedItems.delete(position)
        } else {
            selectedItems.put(position, true)
        }
        notifyItemChanged(position)
    }


    fun clearSelection() {
        val selection = getSelectedItems()
        for (i in selection) {
            notifyItemChanged(i)
        }
        selectedItems.clear()
    }

    fun getSelectedItems(): List<Int> {
        val items = mutableListOf<Int>()
        for (i in 0 until selectedItems.size()) {
            items.add(selectedItems.keyAt(i))
        }
        return items
    }

    fun setSelectedItems(items: ArrayList<Int>) {
        for (i in items) {
            selectedItems.put(i, true)
        }
    }

    fun selectAll() {
        for (i in 0 until currentList.size) {
            selectedItems.put(i, true)
        }
        notifyDataSetChanged()
    }
}