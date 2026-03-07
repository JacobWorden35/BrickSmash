package com.bricksmash.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.bricksmash.R
import com.bricksmash.data.UserRepository
import com.bricksmash.databinding.FragmentMainMenuBinding

/**
 * Main menu screen with options for Play, Leaderboard,
 * Level Editor, and Settings.
 */
class MainMenuFragment : Fragment() {

    private var _binding: FragmentMainMenuBinding? = null
    private val binding get() = _binding!!
    private val userRepo = UserRepository()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMainMenuBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Update login status text
        updateAuthUI()

        binding.btnPlay.setOnClickListener {
            findNavController().navigate(R.id.action_mainMenu_to_levelSelect)
        }

        binding.btnLeaderboard.setOnClickListener {
            findNavController().navigate(R.id.action_mainMenu_to_leaderboard)
        }

        binding.btnLevelEditor.setOnClickListener {
            if (userRepo.isLoggedIn) {
                findNavController().navigate(R.id.action_mainMenu_to_levelEditor)
            } else {
                findNavController().navigate(R.id.action_mainMenu_to_login)
            }
        }

        binding.btnSettings.setOnClickListener {
            findNavController().navigate(R.id.action_mainMenu_to_settings)
        }

        binding.btnLogin.setOnClickListener {
            if (userRepo.isLoggedIn) {
                userRepo.logout()
                updateAuthUI()
            } else {
                findNavController().navigate(R.id.action_mainMenu_to_login)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updateAuthUI()
    }

    private fun updateAuthUI() {
        if (userRepo.isLoggedIn) {
            binding.btnLogin.text = "Sign Out"
            binding.tvWelcome.text = "Welcome, ${userRepo.currentUser?.displayName ?: "Player"}!"
            binding.tvWelcome.visibility = View.VISIBLE
        } else {
            binding.btnLogin.text = "Sign In"
            binding.tvWelcome.visibility = View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
