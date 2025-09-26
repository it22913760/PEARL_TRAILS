package com.example.pearltrails.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.pearltrails.databinding.ItemPopularBinding
import com.example.pearltrails.model.Destination

class PopularAdapter(
    private val items: List<Destination>,
    private val onClick: (Destination) -> Unit = {}
) : RecyclerView.Adapter<PopularAdapter.VH>() {

    inner class VH(val binding: ItemPopularBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemPopularBinding.inflate(inflater, parent, false)
        return VH(binding)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        with(holder.binding) {
            tvTitle.text = item.title
            tvSubtitle.text = item.subtitle
            tvRating.text = String.format("%.1f", item.rating)
            imgPhoto.setImageResource(item.imageRes)
            root.setOnClickListener { onClick(item) }
        }
    }
}
