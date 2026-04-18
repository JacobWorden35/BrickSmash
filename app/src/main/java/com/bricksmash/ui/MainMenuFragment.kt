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
import com.google.android.material.bottomnavigation.BottomNavigationView

/**
 * Main menu. Buttons that correspond to bottom-nav tabs select those tabs
 * directly so navigation state stays in sync. How-to-Play opens a dedicated
 * informational screen.
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

        updateAuthUI()

        binding.btnPlay.setOnClickListener {
            activity?.findViewById<BottomNavigationView>(R.id.bottom_nav)
                ?.selectedItemId = R.id.levelSelectFragment
        }

        binding.btnHowToPlay.setOnClickListener {
            findNavController().navigate(R.id.action_mainMenu_to_howToPlay)
        }

        binding.btnLeaderboard.setOnClickListener {
            activity?.findViewById<BottomNavigationView>(R.id.bottom_nav)
                ?.selectedItemId = R.id.leaderboardFragment
        }

        binding.btnLevelEditor.setOnClickListener {
            if (userRepo.isLoggedIn) {
                activity?.findViewById<BottomNavigationView>(R.id.bottom_nav)
                    ?.selectedItemId = R.id.levelEditorFragment
            } else {
                findNavController().navigate(R.id.action_mainMenu_to_login)
            }
        }

        binding.btnSettings.setOnClickListener {
            activity?.findViewById<BottomNavigationView>(R.id.bottom_nav)
                ?.selectedItemId = R.id.settingsFragment
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