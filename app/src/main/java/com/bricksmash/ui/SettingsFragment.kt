package com.bricksmash.ui

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.bricksmash.databinding.FragmentSettingsBinding

/**
 * Settings screen for game preferences.
 * Uses SharedPreferences for local storage of settings.
 */
class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

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

        // Save settings on change
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
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
