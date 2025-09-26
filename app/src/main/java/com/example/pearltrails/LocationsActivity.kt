package com.example.pearltrails

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import android.view.View
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.core.widget.addTextChangedListener
import com.example.pearltrails.databinding.ActivityLocationsBinding
import com.example.pearltrails.model.Destination
import com.example.pearltrails.ui.FeaturedAdapter
import com.google.android.material.snackbar.Snackbar
import androidx.lifecycle.lifecycleScope
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class LocationsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLocationsBinding
    private enum class Category { ALL, CULTURE, NATURE, BEACHES, HERITAGE, WILDLIFE, SCENIC }
    private var currentCategory: Category = Category.ALL
    private val favoritesKey = stringSetPreferencesKey("favorites")
    private var favoriteNames: MutableSet<String> = mutableSetOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityLocationsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val all = buildAllLocations()
        binding.rvAll.layoutManager = LinearLayoutManager(this)
        val adapter = FeaturedAdapter(
            all,
            onClick = { /* open detail later */ },
            onFavoriteChanged = { dest, liked ->
                if (liked) favoriteNames.add(dest.title) else favoriteNames.remove(dest.title)
                lifecycleScope.launch { dataStore.edit { it[favoritesKey] = favoriteNames } }
                Snackbar.make(binding.root, if (liked) "Added to favorites" else "Removed from favorites", Snackbar.LENGTH_SHORT).show()
            }
        )
        binding.rvAll.adapter = adapter

        // Initialize favorites from DataStore
        lifecycleScope.launch {
            val set = dataStore.data.first()[favoritesKey] ?: emptySet()
            favoriteNames = set.toMutableSet()
            adapter.setFavorites(set)
        }

        // Navbar actions
        binding.btnTabHome.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
        // Logo click behaves like Home
        binding.imgLogo.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
        binding.btnTabExplore.setOnClickListener {
            // Already on Explore; no-op
        }
        binding.btnTabProfile.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
            finish()
        }

        // Search + category filter
        val applyFilters: () -> Unit = {
            val q = binding.etSearchAll.text?.toString()?.trim().orEmpty().lowercase()
            val base = all.filter { d ->
                when (currentCategory) {
                    Category.ALL -> true
                    Category.CULTURE -> isCulture(d)
                    Category.NATURE -> isNature(d)
                    Category.BEACHES -> isBeach(d)
                    Category.HERITAGE -> isHeritage(d)
                    Category.WILDLIFE -> isWildlife(d)
                    Category.SCENIC -> isScenic(d)
                }
            }
            val filtered = if (q.isEmpty()) base else base.filter { d ->
                d.title.lowercase().contains(q) ||
                d.subtitle.lowercase().contains(q) ||
                (d.description?.lowercase()?.contains(q) == true)
            }
            val sorted = if (q.isEmpty()) filtered else filtered.sortedByDescending { d -> relevanceScore(d, q) }
            adapter.submitList(sorted)
            binding.tvEmpty.visibility = if (sorted.isEmpty()) View.VISIBLE else View.GONE
        }

        binding.etSearchAll.addTextChangedListener { applyFilters() }
        binding.btnAllCat.setOnClickListener { currentCategory = Category.ALL; applyFilters(); styleCatButtons() }
        binding.btnCulture.setOnClickListener { currentCategory = Category.CULTURE; applyFilters(); styleCatButtons() }
        binding.btnNature.setOnClickListener { currentCategory = Category.NATURE; applyFilters(); styleCatButtons() }
        binding.btnBeaches.setOnClickListener { currentCategory = Category.BEACHES; applyFilters(); styleCatButtons() }
        binding.btnHeritage.setOnClickListener { currentCategory = Category.HERITAGE; applyFilters(); styleCatButtons() }
        binding.btnWildlife.setOnClickListener { currentCategory = Category.WILDLIFE; applyFilters(); styleCatButtons() }
        binding.btnScenic.setOnClickListener { currentCategory = Category.SCENIC; applyFilters(); styleCatButtons() }

        // Initial state
        styleCatButtons()
        applyFilters()
    }

    private fun relevanceScore(d: Destination, q: String): Int {
        var score = 0
        val t = d.title.lowercase(); val s = d.subtitle.lowercase(); val desc = d.description?.lowercase().orEmpty()
        if (t.startsWith(q)) score += 5
        if (t.contains(q)) score += 3
        if (s.contains(q)) score += 2
        if (desc.contains(q)) score += 1
        return score
    }

    private fun isCulture(d: Destination): Boolean =
        listOf("temple", "stupa", "pagoda", "dagoba", "buddha", "lighthouse").any { key ->
            d.title.lowercase().contains(key) || d.description?.lowercase()?.contains(key) == true
        }

    private fun isNature(d: Destination): Boolean =
        listOf("peak", "mount", "bridge", "park", "forest", "tea", "waterfall").any { key ->
            d.title.lowercase().contains(key) || d.description?.lowercase()?.contains(key) == true
        }

    private fun isBeach(d: Destination): Boolean =
        listOf("beach", "bay", "mirissa").any { key ->
            d.title.lowercase().contains(key) || d.description?.lowercase()?.contains(key) == true
        }

    private fun isHeritage(d: Destination): Boolean =
        listOf("fort", "ruins", "ancient", "palace", "citadel", "heritage").any { key ->
            d.title.lowercase().contains(key) || d.description?.lowercase()?.contains(key) == true
        }

    private fun isWildlife(d: Destination): Boolean =
        listOf("park", "safari", "wildlife", "elephant", "leopard").any { key ->
            d.title.lowercase().contains(key) || d.description?.lowercase()?.contains(key) == true
        }

    private fun isScenic(d: Destination): Boolean =
        listOf("view", "scenic", "train", "tea", "hill", "bridge").any { key ->
            d.title.lowercase().contains(key) || d.description?.lowercase()?.contains(key) == true
        }

    private fun styleCatButtons() {
        fun select(v: com.google.android.material.button.MaterialButton, selected: Boolean) {
            v.setBackgroundColor(if (selected) getColor(R.color.accent_green) else 0x00000000)
            v.setTextColor(if (selected) 0xFF000000.toInt() else getColor(R.color.text_primary))
        }
        select(binding.btnAllCat, currentCategory == Category.ALL)
        select(binding.btnCulture, currentCategory == Category.CULTURE)
        select(binding.btnNature, currentCategory == Category.NATURE)
        select(binding.btnBeaches, currentCategory == Category.BEACHES)
        select(binding.btnHeritage, currentCategory == Category.HERITAGE)
        select(binding.btnWildlife, currentCategory == Category.WILDLIFE)
        select(binding.btnScenic, currentCategory == Category.SCENIC)
    }

    private fun buildAllLocations(): List<Destination> = listOf(
        Destination(
            title = "Adam's Peak (Sri Pada)",
            subtitle = "Central Highlands",
            description = "Iconic pilgrimage mountain famed for the sacred footprint at its summit and stunning sunrise hikes.",
            rating = 4.8,
            imageRes = R.drawable.adams_peak
        ),
        Destination(
            title = "Ruwanwelisaya Stupa",
            subtitle = "Anuradhapura",
            description = "Ancient white dagoba by King Dutugemunu—one of the most revered Buddhist stupas in Sri Lanka.",
            rating = 4.7,
            imageRes = R.drawable.ruwanwelisaya
        ),
        Destination(
            title = "Nilaveli Beach",
            subtitle = "Trincomalee District",
            description = "Palm-fringed shoreline with powdery sands and calm turquoise waters ideal for swimming and snorkelling.",
            rating = 4.6,
            imageRes = R.drawable.nilaveli_beach
        ),
        Destination(
            title = "Dambulla Cave Temple",
            subtitle = "Matale District",
            description = "UNESCO-listed cave complex filled with centuries-old Buddha statues and vibrant murals carved into the rock.",
            rating = 4.9,
            imageRes = R.drawable.dambulla_cave_temple
        ),
        Destination(
            title = "Nine Arch Bridge",
            subtitle = "Ella, Demodara",
            description = "Iconic colonial-era viaduct amid lush tea country—famous for the blue train crossing through misty hills.",
            rating = 4.8,
            imageRes = R.drawable.nine_arch_bridge
        ),
        Destination(
            title = "Sigiriya Rock Fortress",
            subtitle = "Matale District",
            description = "UNESCO-listed ancient rock citadel famed for its frescoes, mirror wall and royal gardens.",
            rating = 4.8,
            imageRes = R.drawable.sigiriya_rock
        ),
        Destination(
            title = "Yala National Park",
            subtitle = "Southern Province",
            description = "Sri Lanka’s premier wildlife reserve – home to elephants, leopards and lagoons full of birdlife.",
            rating = 4.7,
            imageRes = R.drawable.yala_jungle
        ),
        Destination(
            title = "Galle Fort Lighthouse",
            subtitle = "Galle",
            description = "Historic coastal fort and lighthouse with colonial lanes, ramparts and sunset views.",
            rating = 4.7,
            imageRes = R.drawable.galle_lighthouse
        ),
        Destination(
            title = "Temple of the Sacred Tooth",
            subtitle = "Kandy",
            description = "Sacred temple that houses the Relic of the Tooth of the Buddha, a vital pilgrimage site.",
            rating = 4.8,
            imageRes = R.drawable.temple_of_tooth
        ),
        Destination(
            title = "Tea Country Plantations",
            subtitle = "Hill Country",
            description = "Undulating emerald tea estates, misty hills and scenic factories with tastings.",
            rating = 4.6,
            imageRes = R.drawable.tea_plantation
        ),
        Destination(
            title = "Mirissa Beach",
            subtitle = "Mirissa",
            description = "A photogenic palm-studded headland and sweeping sandy bay on Sri Lanka's south coast.",
            rating = 4.7,
            imageRes = R.drawable.mirissa_beach
        ),
    )
}
