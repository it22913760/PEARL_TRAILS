package com.example.pearltrails

import android.content.Intent
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.GridLayoutManager
import androidx.core.content.ContextCompat
import android.content.res.ColorStateList
import kotlin.random.Random
import android.widget.ArrayAdapter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.appcompat.app.AlertDialog
import com.example.pearltrails.databinding.ActivityProfileSimpleBinding
import com.example.pearltrails.ui.AchievementsAdapter
import com.example.pearltrails.ui.FeaturedAdapter
import com.example.pearltrails.model.Destination
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.core.booleanPreferencesKey

class ProfileActivity : AppCompatActivity() {
    private lateinit var binding: ActivityProfileSimpleBinding
    private lateinit var placesAdapter: FeaturedAdapter
    private val homeBookmarksFlag = booleanPreferencesKey("home_selected_bookmarks")
    // Cache: avoid rebuilding location list repeatedly
    private val catalog: List<Destination> by lazy { buildAllLocations() }
    // Cache time formatter to avoid recreating on each convert tap
    private val timeFormat by lazy { SimpleDateFormat("h:mm:ss a", Locale.getDefault()) }

    // DataStore keys
    private val keyName = stringPreferencesKey("profile_name")
    private val keyBio = stringPreferencesKey("profile_bio")
    private val keyEmail = booleanPreferencesKey("pref_email")
    private val keyPush = booleanPreferencesKey("pref_push")
    private val keyLocation = booleanPreferencesKey("pref_location")
    private val keyJoined = stringPreferencesKey("profile_joined")
    private val keyAvatar = stringPreferencesKey("profile_avatar_uri")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityProfileSimpleBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Navbar interactions
        binding.btnTabHome.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
        binding.btnTabExplore.setOnClickListener {
            startActivity(Intent(this, LocationsActivity::class.java))
            finish()
        }
        binding.btnTabProfile.setOnClickListener { /* already here */ }
        // Press feedback
        binding.btnTabHome.applyTabPressAnimation()
        binding.btnTabExplore.applyTabPressAnimation()
        binding.btnTabProfile.applyTabPressAnimation()

        // Left logo -> Home
        binding.imgLogo.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        // Edit profile button -> toggle inline editor
        binding.btnEdit.setOnClickListener {
            val editor = binding.llEditInline
            if (editor.visibility == View.VISIBLE) {
                editor.visibility = View.GONE
            } else {
                // preload current values
                binding.etEditName.setText(binding.tvName.text)
                binding.etEditBio.setText(binding.tvBio.text)
                binding.tvEditTitle.text = "Edit Profile"
                binding.tilEditEmailField.visibility = View.GONE
                binding.tilEditPasswordField.visibility = View.GONE
                editor.visibility = View.VISIBLE
            }
        }

        // Inline Save
        binding.btnInlineSave.setOnClickListener {
            val newName = binding.etEditName.text?.toString()?.trim().orEmpty()
            val newBio = binding.etEditBio.text?.toString()?.trim().orEmpty()
            val isSignup = binding.tvEditTitle.text?.toString()?.contains("Sign up", ignoreCase = true) == true
            val email = binding.etEditEmail.text?.toString()?.trim().orEmpty()
            val password = binding.etEditPassword.text?.toString()?.trim().orEmpty()

            // Basic validation if sign up
            if (isSignup) {
                if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    android.widget.Toast.makeText(this, "Enter a valid email", android.widget.Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                if (password.length < 6) {
                    android.widget.Toast.makeText(this, "Password must be at least 6 characters", android.widget.Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
            }

            lifecycleScope.launch {
                dataStore.edit {
                    it[stringPreferencesKey("profile_name")] = newName
                    it[stringPreferencesKey("profile_bio")] = newBio
                    if (isSignup) {
                        it[stringPreferencesKey("signup_email")] = email
                        it[stringPreferencesKey("signup_password")] = password
                    }
                }
                binding.tvName.text = newName
                binding.tvBio.text = newBio
                binding.llEditInline.visibility = View.GONE
                binding.btnAddAccount.visibility = View.GONE
                // Show achievements again after a successful save
                binding.tvAchievementsTitle.visibility = View.VISIBLE
                binding.rvAchievements.visibility = View.VISIBLE
                android.widget.Toast.makeText(this@ProfileActivity, "Saved", android.widget.Toast.LENGTH_SHORT).show()
            }
        }

        // Inline Delete (clear profile data only)
        binding.btnInlineDelete.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Clear Profile")
                .setMessage("This will clear your name and bio.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Clear") { _, _ ->
                    lifecycleScope.launch {
                        dataStore.edit {
                            it.remove(stringPreferencesKey("profile_name"))
                            it.remove(stringPreferencesKey("profile_bio"))
                        }
                        binding.tvName.text = "Travel Explorer"
                        binding.tvBio.text = "Sri Lanka Enthusiast"
                        binding.llEditInline.visibility = View.GONE
                        binding.btnAddAccount.visibility = View.VISIBLE
                        // Hide achievements until user clicks Add Account
                        binding.tvAchievementsTitle.visibility = View.GONE
                        binding.rvAchievements.visibility = View.GONE
                        android.widget.Toast.makeText(this@ProfileActivity, "Profile cleared", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
                .show()
        }

        // Add Account: open inline editor as Sign up
        binding.btnAddAccount.setOnClickListener {
            binding.tvEditTitle.text = "Sign up"
            binding.etEditName.setText("")
            binding.etEditBio.setText("")
            binding.tilEditEmailField.visibility = View.VISIBLE
            binding.tilEditPasswordField.visibility = View.VISIBLE
            binding.llEditInline.visibility = View.VISIBLE
        }
        // Share button (placeholder)
        binding.btnShare.setOnClickListener { /* TODO share profile */ }

        // Achievements list (horizontal)
        binding.rvAchievements.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        val achievements = listOf(
            AchievementsAdapter.Item("First Bookmark", "Dec 2024", R.drawable.ic_star_outline),
            AchievementsAdapter.Item("Explorer", "5+ destinations", R.drawable.ic_pin_outline),
            AchievementsAdapter.Item("Culture Lover", "Visited temples", R.drawable.ic_leaf_outline)
        )
        binding.rvAchievements.adapter = AchievementsAdapter(achievements)
        binding.rvAchievements.setHasFixedSize(true)
        binding.rvAchievements.itemAnimator = null

        // Profile places grid setup (2-column grid)
        binding.rvProfilePlaces.layoutManager = GridLayoutManager(this, 2)
        placesAdapter = FeaturedAdapter(emptyList())
        binding.rvProfilePlaces.adapter = placesAdapter
        binding.rvProfilePlaces.setHasFixedSize(true)
        binding.rvProfilePlaces.itemAnimator = null
        binding.rvProfilePlaces.setItemViewCacheSize(10)

        // Load profile basics from DataStore
        lifecycleScope.launch {
            val prefs = dataStore.data.first()
            val name = prefs[keyName] ?: "Travel Explorer"
            val bio = prefs[keyBio] ?: "Sri Lanka Enthusiast"
            val joined = prefs[keyJoined] ?: "Joined Dec 2024"
            val avatarUri = prefs[keyAvatar]
            binding.tvName.text = name
            binding.tvBio.text = bio
            binding.tvJoined.text = joined
            if (!avatarUri.isNullOrEmpty()) {
                try { binding.imgAvatarProfile.setImageURI(android.net.Uri.parse(avatarUri)) } catch (_: Exception) {}
            }
        }

        // Default view: show random visited suggestions
        showVisitedRandom()
        styleProfileTabs(active = "visited")
        updateStats()

        // Currency converter setup
        setupCurrencyConverter()

        // Toggle buttons
        binding.btnProfileVisited.setOnClickListener {
            showVisitedRandom()
            styleProfileTabs(active = "visited")
        }
        binding.btnProfileBookmarks.setOnClickListener {
            showBookmarkedPlaces()
            styleProfileTabs(active = "bookmarks")
        }
    }

    private fun updateStats() {
        val favoritesKey = stringSetPreferencesKey("favorites")
        lifecycleScope.launch {
            val favCount = dataStore.data.first()[favoritesKey]?.size ?: 0

            // Bookmarks
            binding.statBookmarks.imgStatIcon.setImageResource(R.drawable.ic_star_outline)
            binding.statBookmarks.tvStatLabel.text = "Bookmarks"
            binding.statBookmarks.tvStatValue.text = favCount.toString()

            // Visited (randomized fake count)
            val visited = Random.nextInt(6, 24)
            binding.statVisited.imgStatIcon.setImageResource(R.drawable.ic_pin_outline)
            binding.statVisited.tvStatLabel.text = "Visited"
            binding.statVisited.tvStatValue.text = visited.toString()

            // Days joined (randomized fake days)
            val days = Random.nextInt(30, 365)
            binding.statDays.imgStatIcon.setImageResource(R.drawable.ic_leaf_outline)
            binding.statDays.tvStatLabel.text = "Days Joined"
            binding.statDays.tvStatValue.text = days.toString()

            // Trips (randomized)
            val trips = Random.nextInt(1, 9)
            binding.statTrips.imgStatIcon.setImageResource(R.drawable.ic_pin_outline)
            binding.statTrips.tvStatLabel.text = "Trips"
            binding.statTrips.tvStatValue.text = trips.toString()
        }
    }

    private fun showVisitedRandom() {
        val random = catalog.shuffled().take(6)
        placesAdapter.submitList(random)
    }

    private fun showBookmarkedPlaces() {
        val favoritesKey = stringSetPreferencesKey("favorites")
        lifecycleScope.launch {
            val set = dataStore.data.first()[favoritesKey] ?: emptySet()
            val filtered = catalog.filter { it.title in set }
            placesAdapter.submitList(filtered)
            if (filtered.isEmpty()) {
                android.widget.Toast.makeText(
                    this@ProfileActivity,
                    "No bookmarks yet. Tap the heart on Home/Explore to add.",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun styleProfileTabs(active: String) {
        val green = ContextCompat.getColor(this, R.color.accent_green)
        val black = 0xFF000000.toInt()
        val primary = ContextCompat.getColor(this, R.color.text_primary)
        val divider = ContextCompat.getColor(this, R.color.divider)

        fun fill(btn: com.google.android.material.button.MaterialButton) {
            btn.backgroundTintList = ColorStateList.valueOf(green)
            btn.setTextColor(black)
            btn.strokeWidth = 0
        }
        fun outline(btn: com.google.android.material.button.MaterialButton) {
            btn.backgroundTintList = ColorStateList.valueOf(0x00000000)
            btn.setTextColor(primary)
            btn.strokeColor = ColorStateList.valueOf(divider)
            btn.strokeWidth = (btn.resources.displayMetrics.density * 1).toInt() // ~1dp
        }

        if (active == "bookmarks") {
            fill(binding.btnProfileBookmarks)
            outline(binding.btnProfileVisited)
        } else {
            fill(binding.btnProfileVisited)
            outline(binding.btnProfileBookmarks)
        }
    }

    // Reuse the same catalog as LocationsActivity
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

    // --- Currency Converter ---
    private fun setupCurrencyConverter() {
        // Supported currencies and simple demo LKR rates
        val currencies = listOf("USD","EUR","GBP","JPY","AUD","CAD","SGD","INR","CNY","AED")
        val lkrRates = mapOf(
            "USD" to 307.50,
            "EUR" to 328.10,
            "GBP" to 390.25,
            "JPY" to 2.05,
            "AUD" to 202.40,
            "CAD" to 227.30,
            "SGD" to 226.90,
            "INR" to 3.70,
            "CNY" to 42.80,
            "AED" to 83.70,
        )

        // Exposed dropdown (visible, Material look)
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, currencies)
        binding.etFxFrom.setAdapter(adapter)
        binding.etFxFrom.setText("USD", false)

        // Convert action
        binding.btnFxConvert.setOnClickListener {
            val code = binding.etFxFrom.text?.toString()?.trim().takeUnless { it.isNullOrEmpty() } ?: "USD"
            val rate = lkrRates[code] ?: lkrRates["USD"]!!
            val amt = binding.etFxAmount.text?.toString()?.trim()?.toDoubleOrNull() ?: 0.0
            val result = amt * rate
            binding.tvFxToTitle.text = "To Sri Lankan Rupees (LKR)"
            binding.tvFxResult.text = String.format(Locale.getDefault(), "Rs. %,.2f", result)
            binding.tvFxRate.text = String.format(Locale.getDefault(), "1 %s = Rs. %,.2f", code, rate)
            val time = timeFormat.format(Date())
            binding.tvFxUpdated.text = "Updated $time"
        }
    }
}

private fun View.applyTabPressAnimation() {
    setOnTouchListener { v, event ->
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                v.animate().scaleX(0.97f).scaleY(0.97f).setDuration(90)
                    .setInterpolator(AccelerateDecelerateInterpolator()).start()
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                v.animate().scaleX(1f).scaleY(1f).setDuration(110)
                    .setInterpolator(AccelerateDecelerateInterpolator()).start()
            }
        }
        false
    }
}
