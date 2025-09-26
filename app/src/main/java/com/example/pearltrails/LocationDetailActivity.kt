package com.example.pearltrails

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.pearltrails.databinding.ActivityLocationDetailBinding

class LocationDetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLocationDetailBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityLocationDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val title = intent.getStringExtra("title").orEmpty()
        val subtitle = intent.getStringExtra("subtitle").orEmpty()
        val description = intent.getStringExtra("description").orEmpty()
        val rating = intent.getDoubleExtra("rating", 0.0)
        val imageRes = intent.getIntExtra("imageRes", 0)
        val badge = intent.getStringExtra("badge").orEmpty()

        binding.tvTitle.text = title
        binding.tvSubtitle.text = subtitle
        binding.tvRating.text = String.format("\u2605 %.1f", rating)
        binding.tvBadge.text = badge.ifEmpty { "Explore" }
        binding.tvAbout.text = description
        if (imageRes != 0) binding.imgHeader.setImageResource(imageRes)

        // Stats (example values; can be enhanced with real data later)
        binding.statDuration.imgStatIcon.setImageResource(android.R.drawable.ic_menu_recent_history)
        binding.statDuration.tvStatLabel.text = "Duration"
        binding.statDuration.tvStatValue.text = "6-7 hours"

        binding.statBestTime.imgStatIcon.setImageResource(android.R.drawable.ic_menu_month)
        binding.statBestTime.tvStatLabel.text = "Best Time"
        binding.statBestTime.tvStatValue.text = "Dec - Mar"

        binding.statDifficulty.imgStatIcon.setImageResource(R.drawable.ic_leaf_outline)
        binding.statDifficulty.tvStatLabel.text = "Difficulty"
        binding.statDifficulty.tvStatValue.text = "Easy"

        binding.statPrice.imgStatIcon.setImageResource(android.R.drawable.ic_menu_info_details)
        binding.statPrice.tvStatLabel.text = "From"
        binding.statPrice.tvStatValue.text = "$15"

        // Back and Share
        binding.btnBack.setOnClickListener { onBackPressedDispatcher.onBackPressed() }
        binding.btnShare.setOnClickListener {
            val shareText = "${title} — ${subtitle}\nRating: ${String.format("%.1f", rating)}\n\n${description}"
            val intent = android.content.Intent(android.content.Intent.ACTION_SEND)
            intent.type = "text/plain"
            intent.putExtra(android.content.Intent.EXTRA_TEXT, shareText)
            startActivity(android.content.Intent.createChooser(intent, "Share via"))
        }
    }
}
