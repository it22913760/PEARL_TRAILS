package com.example.pearltrails.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.pearltrails.databinding.ItemAchievementBinding

class AchievementsAdapter(private val items: List<Item>) : RecyclerView.Adapter<AchievementsAdapter.VH>() {

    data class Item(
        val title: String,
        val subtitle: String,
        val iconRes: Int
    )

    inner class VH(val binding: ItemAchievementBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemAchievementBinding.inflate(inflater, parent, false)
        return VH(binding)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        with(holder.binding) {
            imgIcon.setImageResource(item.iconRes)
            tvTitle.text = item.title
            tvSubtitle.text = item.subtitle
        }
    }
}
