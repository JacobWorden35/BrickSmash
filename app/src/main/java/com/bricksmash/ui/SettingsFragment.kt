package com.bricksmash.ui

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.bricksmash.data.UserRepository
import com.bricksmash.databinding.FragmentSettingsBinding
import kotlinx.coroutines.launch

/**
 * Settings screen for game preferences and account management.
 * Uses SharedPreferences for local toggle settings.
 * Displays the Account section only when the user is signed in.
 */
class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private val userRepo = UserRepository()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val prefs = requireContext().getSharedPreferences("bricksmash_prefs", Context.MODE_PRIVATE)

        // Load saved settings
        binding.switchSound.isChecked = prefs.getBoolean("sound_enabled", true)
        binding.switchVibration.isChecked = prefs.getBoolean("vibration_enabled", true)
        binding.switchParticles.isChecked = prefs.getBoolean("particles_enabled", true)

        binding.switchSound.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("sound_enabled", isChecked).apply()
        }

        binding.switchVibration.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("vibration_enabled", isChecked).apply()
        }

        binding.switchParticles.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("particles_enabled", isChecked).apply()
        }

        binding.btnResetProgress.setOnClickListener {
            prefs.edit().clear().apply()
            Toast.makeText(requireContext(), "Progress reset!", Toast.LENGTH_SHORT).show()
        }

        // Account section (Change Display Name)
        setupAccountSection()
    }

    /**
     * Shows the Account section and wires up the Update Name button
     * if the user is logged in. Otherwise, the section stays hidden.
     */
    private fun setupAccountSection() {
        if (!userRepo.isLoggedIn) {
            binding.accountSection.visibility = View.GONE
            return
        }

        binding.accountSection.visibility = View.VISIBLE
        val currentName = userRepo.currentUser?.displayName ?: "Player"
        binding.tvCurrentName.text = "Current: $currentName"
        binding.etDisplayName.setText(currentName)

        binding.btnUpdateName.setOnClickListener {
            val newName = binding.etDisplayName.text.toString().trim()
            if (newName.isEmpty()) {
                Toast.makeText(requireContext(), "Name cannot be empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (newName.length > 30) {
                Toast.makeText(requireContext(), "Name must be 30 characters or fewer", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (newName == currentName) {
                Toast.makeText(requireContext(), "That's already your name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            binding.progressBarName.visibility = View.VISIBLE
            binding.btnUpdateName.isEnabled = false

            lifecycleScope.launch {
                val result = userRepo.updateDisplayName(newName)
                binding.progressBarName.visibility = View.GONE
                binding.btnUpdateName.isEnabled = true

                result.fold(
                    onSuccess = {
                        Toast.makeText(requireContext(), "Display name updated!", Toast.LENGTH_SHORT).show()
                        binding.tvCurrentName.text = "Current: $newName"
                    },
                    onFailure = { e ->
                        Toast.makeText(
                            requireContext(),
                            "Update failed: ${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                )
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}