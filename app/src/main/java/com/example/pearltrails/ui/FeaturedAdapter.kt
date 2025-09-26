package com.example.pearltrails.ui

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.pearltrails.R
import com.example.pearltrails.databinding.ItemFeaturedBinding
import com.example.pearltrails.model.Destination

class FeaturedAdapter(
    items: List<Destination>,
    private val onClick: (Destination) -> Unit = {},
    private val onFavoriteChanged: (Destination, Boolean) -> Unit = { _, _ -> }
) : RecyclerView.Adapter<FeaturedAdapter.VH>() {

    private val data: MutableList<Destination> = items.toMutableList()
    private val favorites = mutableSetOf<String>()

    inner class VH(val binding: ItemFeaturedBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemFeaturedBinding.inflate(inflater, parent, false)
        return VH(binding)
    }

    // Simple heuristic-based badge classification to mirror LocationsActivity filters
    private fun classifyBadge(d: Destination): String {
        val t = d.title.lowercase()
        val s = d.subtitle.lowercase()
        val desc = d.description?.lowercase().orEmpty()
        fun has(vararg keys: String) = keys.any { k -> t.contains(k) || s.contains(k) || desc.contains(k) }

        return when {
            has("beach", "bay", "mirissa") -> "Beaches"
            has("park", "safari", "wildlife", "elephant", "leopard") -> "Wildlife"
            has("temple", "stupa", "pagoda", "dagoba", "buddha") -> "Culture"
            has("fort", "ruins", "ancient", "palace", "citadel", "heritage", "lighthouse") -> "Heritage"
            has("peak", "mount", "bridge", "forest", "tea", "waterfall") -> "Nature"
            has("view", "scenic", "train", "hill") -> "Scenic"
            else -> "Explore"
        }
    }

    override fun getItemCount(): Int = data.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = data[position]
        with(holder.binding) {
            tvTitle.text = item.title
            tvSubtitle.text = item.subtitle
            tvDescription.text = item.description ?: ""
            tvRating.text = String.format("%.1f", item.rating)
            imgPhoto.setImageResource(item.imageRes)

            // Category badge
            val badge = classifyBadge(item)
            tvBadge.text = badge
            tvBadge.visibility = View.VISIBLE

            val key = item.title // stable key for favorites
            val liked = favorites.contains(key)
            btnFav.setImageResource(if (liked) R.drawable.ic_heart_filled else R.drawable.ic_heart_outline)
            val tintColor = if (liked) ContextCompat.getColor(root.context, R.color.accent_green)
                            else ContextCompat.getColor(root.context, R.color.text_primary)
            btnFav.imageTintList = ColorStateList.valueOf(tintColor)

            btnFav.setOnClickListener {
                val nowLiked = favorites.toggle(key)
                animatePop(it)
                btnFav.setImageResource(if (nowLiked) R.drawable.ic_heart_filled else R.drawable.ic_heart_outline)
                val tColor = if (nowLiked) ContextCompat.getColor(root.context, R.color.accent_green)
                             else ContextCompat.getColor(root.context, R.color.text_primary)
                btnFav.imageTintList = ColorStateList.valueOf(tColor)
                onFavoriteChanged(item, nowLiked)
            }

            root.setOnClickListener { onClick(item) }
        }
    }

    private fun animatePop(view: View) {
        view.animate()
            .scaleX(1.15f)
            .scaleY(1.15f)
            .setDuration(90)
            .withEndAction {
                view.animate().scaleX(1f).scaleY(1f).setDuration(90)
                    .setInterpolator(AccelerateDecelerateInterpolator())
                    .start()
            }
            .start()
    }

    private fun MutableSet<String>.toggle(key: String): Boolean {
        return if (contains(key)) { remove(key); false } else { add(key); true }
    }

    fun submitList(newItems: List<Destination>) {
        data.clear()
        data.addAll(newItems)
        notifyDataSetChanged()
    }

    fun setFavorites(newFavorites: Set<String>) {
        favorites.clear()
        favorites.addAll(newFavorites)
        notifyDataSetChanged()
    }
}
