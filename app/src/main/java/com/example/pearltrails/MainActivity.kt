package com.example.pearltrails

import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.core.widget.addTextChangedListener
import android.net.Uri
import android.media.MediaPlayer
import android.content.Context
import android.content.Intent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import android.widget.Toast
import com.example.pearltrails.databinding.ActivityMainBinding
import com.example.pearltrails.model.Destination
import com.example.pearltrails.ui.FeaturedAdapter
import com.example.pearltrails.ui.PopularAdapter
import androidx.lifecycle.lifecycleScope
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

// Top-level DataStore delegate (shared)
val Context.dataStore by preferencesDataStore(name = "app_prefs")

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val favoritesKey = stringSetPreferencesKey("favorites")
    private var favoriteNames: MutableSet<String> = mutableSetOf()
    private var showFavoritesOnly: Boolean = false
    private val homeBookmarksFlag = booleanPreferencesKey("home_selected_bookmarks")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize content
        setupLists()
        setupStatTileAnimations()
        setupHeroVideo()

        // Navigate to All Locations on CTA
        binding.btnExploreHero.setOnClickListener {
            startActivity(Intent(this, LocationsActivity::class.java))
        }



        // Navbar interactions
        binding.btnTabHome.setOnClickListener {
            selectTopTab("home")
            binding.scrollMain.smoothScrollTo(0, 0)
        }
        binding.btnTabExplore.setOnClickListener {
            selectTopTab("explore")
            startActivity(Intent(this, LocationsActivity::class.java))
        }
        binding.btnTabProfile.setOnClickListener {
            selectTopTab("profile")
            startActivity(Intent(this, ProfileActivity::class.java))
        }

        // Press feedback (ripple + scale) for tabs
        binding.btnTabHome.applyTabPressAnimation()
        binding.btnTabExplore.applyTabPressAnimation()
        binding.btnTabProfile.applyTabPressAnimation()

        // Logo click -> scroll to top
        binding.imgLogo.setOnClickListener {
            binding.scrollMain.smoothScrollTo(0, 0)
            selectTopTab("home")
        }
    }

    private fun selectTopTab(which: String) {
        fun style(selected: Boolean, btn: com.google.android.material.button.MaterialButton) {
            if (selected) {
                btn.setBackgroundResource(R.drawable.bg_tab_selected)
                btn.setTextColor(0xFF000000.toInt())
                btn.animate()
                    .scaleX(1.06f)
                    .scaleY(1.06f)
                    .alpha(1f)
                    .setDuration(110)
                    .setInterpolator(AccelerateDecelerateInterpolator())
                    .withEndAction {
                        // settle back to 1.0 after the pop
                        btn.animate().scaleX(1f).scaleY(1f).setDuration(60).start()
                    }
                    .start()
            } else {
                btn.setBackgroundColor(0x00000000)
                btn.setTextColor(getColor(android.R.color.white))
                btn.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .alpha(0.9f)
                    .setDuration(90)
                    .setInterpolator(AccelerateDecelerateInterpolator())
                    .start()
            }
        }
        style(which == "home", binding.btnTabHome)
        style(which == "explore", binding.btnTabExplore)
        style(which == "profile", binding.btnTabProfile)
    }

    // Subtle scale feedback on press (works with ripple)
    private fun View.applyTabPressAnimation() {
        setOnTouchListener { v, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    v.animate()
                        .scaleX(0.97f)
                        .scaleY(0.97f)
                        .setDuration(90)
                        .setInterpolator(AccelerateDecelerateInterpolator())
                        .start()
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    v.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(110)
                        .setInterpolator(AccelerateDecelerateInterpolator())
                        .start()
                }
            }
            // do not consume so ripple still works with click
            false
        }
    }

    private fun styleFilterButtons() {
        val allSelected = binding.btnAll.isPressed || binding.btnFavorites.isPressed.not()
        // Explicit styling to match selected/unselected states
        val useAll = !showFavoritesOnly
        if (useAll) {
            binding.btnAll.setBackgroundColor(getColor(R.color.accent_green))
            binding.btnAll.setTextColor(0xFF000000.toInt())
            binding.btnFavorites.setBackgroundColor(0x00000000)
            binding.btnFavorites.setTextColor(getColor(R.color.text_primary))
        } else {
            binding.btnFavorites.setBackgroundColor(getColor(R.color.accent_green))
            binding.btnFavorites.setTextColor(0xFF000000.toInt())
            binding.btnAll.setBackgroundColor(0x00000000)
            binding.btnAll.setTextColor(getColor(R.color.text_primary))
        }
    }

    private fun applyFeaturedFilter(
        allItems: List<Destination>,
        adapter: FeaturedAdapter,
        favoritesOnly: Boolean,
        query: String = binding.etSearch.text?.toString()?.trim().orEmpty()
    ) {
        val base = if (favoritesOnly) allItems.filter { favoriteNames.contains(it.title) } else allItems
        val q = query.lowercase()
        val result = if (q.isEmpty()) base else {
            base.maxByOrNull { dest ->
                val t = dest.title.lowercase()
                val s = dest.subtitle.lowercase()
                val d = dest.description?.lowercase().orEmpty()
                var score = 0
                if (t.contains(q)) score += 3
                if (s.contains(q)) score += 2
                if (d.contains(q)) score += 1
                score
            }?.let { listOf(it) } ?: emptyList()
        }
        adapter.submitList(result.ifEmpty { if (q.isEmpty()) base else emptyList() })
    }

    private fun updateFavBadge() {
        binding.tvFavBadge.text = "❤ ${favoriteNames.size}"
    }


    private fun setupStatTileAnimations() {
        fun View.applyPressAnimation() {
            setOnTouchListener { v, event ->
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        val z = 6f * v.resources.displayMetrics.density
                        v.animate()
                            .scaleX(0.95f)
                            .scaleY(0.95f)
                            .translationZ(z)
                            .setDuration(110)
                            .setInterpolator(AccelerateDecelerateInterpolator())
                            .start()
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        v.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .translationZ(0f)
                            .setDuration(140)
                            .setInterpolator(AccelerateDecelerateInterpolator())
                            .start()
                    }
                }
                // Allow ripple to draw while consuming only for visual effect without click
                false
            }
        }

        binding.statDestinations.applyPressAnimation()
        binding.statRating.applyPressAnimation()
        binding.statProvinces.applyPressAnimation()
    }

    private fun setupLists() {
        val ctx = this
        val featured = listOf(
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
                description = "Ancient white dagoba built by King Dutugemunu—one of the most revered Buddhist stupas in Sri Lanka.",
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
            )
        )

        val popular = listOf(
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
        )

        // Featured - vertical list
        binding.rvFeatured.layoutManager = LinearLayoutManager(this)
        val featuredAdapter = FeaturedAdapter(
            featured,
            onClick = { dest ->
                val intent = Intent(this, LocationDetailActivity::class.java)
                intent.putExtra("title", dest.title)
                intent.putExtra("subtitle", dest.subtitle)
                intent.putExtra("description", dest.description)
                intent.putExtra("rating", dest.rating)
                intent.putExtra("imageRes", dest.imageRes)
                // simple badge (reuse title words)
                intent.putExtra("badge", "Adventure")
                startActivity(intent)
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            },
            onFavoriteChanged = { dest, liked ->
                // Update in-memory set
                if (liked) favoriteNames.add(dest.title) else favoriteNames.remove(dest.title)
                // Persist
                lifecycleScope.launch { dataStore.edit { it[favoritesKey] = favoriteNames } }
                // Snackbar feedback
                val msg = if (liked) "Added to favorites" else "Removed from favorites"
                Snackbar.make(binding.root, msg, Snackbar.LENGTH_SHORT).show()
                // Update header badge count immediately
                updateFavBadge()
            }
        )
        binding.rvFeatured.adapter = featuredAdapter
        binding.rvFeatured.isNestedScrollingEnabled = false
        binding.rvFeatured.setHasFixedSize(true)
        binding.rvFeatured.itemAnimator = null
        binding.rvFeatured.setItemViewCacheSize(12)
        // Load persisted favorites and sync to adapter
        lifecycleScope.launch {
            val set = dataStore.data.first()[favoritesKey] ?: emptySet()
            favoriteNames = set.toMutableSet()
            featuredAdapter.setFavorites(set)
            updateFavBadge()
        }
        // Filter buttons behavior
        binding.btnAll.setOnClickListener {
            showFavoritesOnly = false
            styleFilterButtons()
            applyFeaturedFilter(featured, featuredAdapter, showFavoritesOnly)
            lifecycleScope.launch { dataStore.edit { it[homeBookmarksFlag] = false } }
        }
        binding.btnFavorites.setOnClickListener {
            showFavoritesOnly = true
            styleFilterButtons()
            applyFeaturedFilter(featured, featuredAdapter, showFavoritesOnly)
            lifecycleScope.launch { dataStore.edit { it[homeBookmarksFlag] = true } }
        }
        // Search filter integration
        binding.etSearch.addTextChangedListener { text ->
            applyFeaturedFilter(featured, featuredAdapter, showFavoritesOnly, text?.toString().orEmpty())
        }
        // Popular - grid
        binding.rvPopular.layoutManager = GridLayoutManager(this, 2)
        binding.rvPopular.adapter = PopularAdapter(popular) { dest ->
            val intent = Intent(this, LocationDetailActivity::class.java)
            intent.putExtra("title", dest.title)
            intent.putExtra("subtitle", dest.subtitle)
            intent.putExtra("description", dest.description)
            intent.putExtra("rating", dest.rating)
            intent.putExtra("imageRes", dest.imageRes)
            intent.putExtra("badge", "Explore")
            startActivity(intent)
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }
        binding.rvPopular.isNestedScrollingEnabled = false
        binding.rvPopular.setHasFixedSize(true)
        binding.rvPopular.itemAnimator = null
        binding.rvPopular.setItemViewCacheSize(10)
    }

    private fun setupHeroVideo() {
        val videoView = binding.videoHero
        try {
            val uri = Uri.parse("android.resource://" + packageName + "/" + R.raw.sigiriya)
            videoView.setVideoURI(uri)
            videoView.setOnPreparedListener { mp ->
                mp.isLooping = true
                mp.setVolume(0f, 0f)
                // Center-crop the video to fill the container without gaps
                videoView.post {
                    val viewW = videoView.width.takeIf { it > 0 } ?: return@post
                    val viewH = videoView.height.takeIf { it > 0 } ?: return@post
                    val videoW = mp.videoWidth
                    val videoH = mp.videoHeight
                    if (videoW > 0 && videoH > 0) {
                        val scaleX = viewW.toFloat() / videoW
                        val scaleY = viewH.toFloat() / videoH
                        val scale = maxOf(scaleX, scaleY) * 1.08f // small extra zoom to avoid edges
                        val finalScaleX = videoW * scale / viewW
                        val finalScaleY = videoH * scale / viewH
                        videoView.scaleX = finalScaleX
                        videoView.scaleY = finalScaleY
                    } else {
                        // Fallback: slight zoom
                        videoView.scaleX = 1.1f
                        videoView.scaleY = 1.1f
                    }
                    videoView.start()
                }
            }
        } catch (_: Exception) {
            // If the raw video is not present or playback fails, keep the fallback image.
        }
    }
}