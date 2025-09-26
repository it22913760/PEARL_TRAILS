package com.example.pearltrails

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.pearltrails.databinding.ActivityEditProfileBinding
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.google.android.material.snackbar.Snackbar

class EditProfileActivity : AppCompatActivity() {
    private lateinit var binding: ActivityEditProfileBinding

    // DataStore keys (shared with ProfileActivity)
    private val keyName = stringPreferencesKey("profile_name")
    private val keyBio = stringPreferencesKey("profile_bio")
    private val keyEmail = booleanPreferencesKey("pref_email")
    private val keyPush = booleanPreferencesKey("pref_push")
    private val keyLocation = booleanPreferencesKey("pref_location")
    private val keyJoined = stringPreferencesKey("profile_joined")
    private val keyAvatar = stringPreferencesKey("profile_avatar_uri")

    private var pickedAvatarUri: Uri? = null

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            pickedAvatarUri = it
            binding.imgAvatarEdit.setImageURI(it)
            Snackbar.make(binding.root, "Avatar selected", Snackbar.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityEditProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val sys = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(sys.left, sys.top, sys.right, sys.bottom)
            insets
        }

        // Load existing values (Read)
        lifecycleScope.launch { loadFromStore() }

        // Close
        binding.btnClose.setOnClickListener { finish(); overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out) }

        // Save (Create/Update)
        binding.btnSave.setOnClickListener {
            lifecycleScope.launch {
                saveToStore()
                Snackbar.make(binding.root, "Profile saved", Snackbar.LENGTH_SHORT).show()
                finish()
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            }
        }

        // Delete account -> clear preferences (Delete)
        binding.btnDelete.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Delete Account")
                .setMessage("This action cannot be undone. All your data will be permanently deleted.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Delete") { _, _ ->
                    lifecycleScope.launch {
                        dataStore.edit { it.clear() }
                        Snackbar.make(binding.root, "Account deleted", Snackbar.LENGTH_LONG).show()
                        finish()
                        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                    }
                }
                .show()
        }

        // Avatar change button (placeholder)
        binding.btnChangeAvatar.setOnClickListener {
            pickImage.launch("image/*")
        }
    }

    private suspend fun loadFromStore() {
        val prefs = dataStore.data.first()
        binding.etName.setText(prefs[keyName] ?: "Travel Explorer")
        binding.etBio.setText(prefs[keyBio] ?: "Sri Lanka Enthusiast")
        binding.swEmail.isChecked = prefs[keyEmail] ?: true
        binding.swPush.isChecked = prefs[keyPush] ?: false
        binding.swLocation.isChecked = prefs[keyLocation] ?: true
        // Joined date and avatar preview
        val joined = prefs[keyJoined]
        if (joined != null) {
            // no view here; ProfileActivity displays it
        }
        prefs[keyAvatar]?.let { uri ->
            try { binding.imgAvatarEdit.setImageURI(Uri.parse(uri)) } catch (_: Exception) {}
        }
    }

    private suspend fun saveToStore() {
        val name = binding.etName.text?.toString()?.trim().orEmpty()
        val bio = binding.etBio.text?.toString()?.trim().orEmpty()
        val email = binding.swEmail.isChecked
        val push = binding.swPush.isChecked
        val location = binding.swLocation.isChecked
        dataStore.edit {
            it[keyName] = name
            it[keyBio] = bio
            it[keyEmail] = email
            it[keyPush] = push
            it[keyLocation] = location
            // Persist avatar if picked
            pickedAvatarUri?.let { uri -> it[keyAvatar] = uri.toString() }
            // Ensure joined date exists
            if (!it.contains(keyJoined)) {
                val fmt = SimpleDateFormat("MMM yyyy", Locale.getDefault())
                it[keyJoined] = "Joined ${fmt.format(Date())}"
            }
        }
    }
}
