package com.bricksmash.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.bricksmash.R
import com.bricksmash.data.LeaderboardRepository
import com.bricksmash.data.UserRepository
import com.bricksmash.databinding.FragmentGameBinding
import com.bricksmash.game.LevelManager
import kotlinx.coroutines.launch

/**
 * Fragment that hosts the actual game.
 * Manages game lifecycle, level loading, and score submission.
 */
class GameFragment : Fragment() {

    private var _binding: FragmentGameBinding? = null
    private val binding get() = _binding!!
    private val leaderboardRepo = LeaderboardRepository()
    private val userRepo = UserRepository()
    private var currentLevelIndex = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGameBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        currentLevelIndex = arguments?.getInt("levelIndex", 0) ?: 0
        val levelManager = LevelManager(requireContext())
        val level = levelManager.getLevel(currentLevelIndex)

        if (level != null) {
            binding.gameView.loadLevel(level)

            // Set up game event callbacks
            binding.gameView.engine.onLevelComplete = { score ->
                activity?.runOnUiThread {
                    showLevelComplete(score)
                    submitScore(score, level.id, level.name)
                }
            }

            binding.gameView.engine.onGameOver = { score ->
                activity?.runOnUiThread {
                    showGameOver(score)
                }
            }
        }

        binding.btnPause.setOnClickListener {
            binding.gameView.pause()
        }

        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun showLevelComplete(score: Int) {
        binding.overlayComplete.visibility = View.VISIBLE
        binding.tvCompleteScore.text = "Score: $score"

        binding.btnNextLevel.setOnClickListener {
            binding.overlayComplete.visibility = View.GONE
            currentLevelIndex++
            val levelManager = LevelManager(requireContext())
            val nextLevel = levelManager.getLevel(currentLevelIndex)
            if (nextLevel != null) {
                binding.gameView.loadLevel(nextLevel)
            } else {
                findNavController().navigateUp()
            }
        }

        binding.btnBackToMenu.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun showGameOver(score: Int) {
        binding.overlayGameOver.visibility = View.VISIBLE
        binding.tvGameOverScore.text = "Score: $score"

        binding.btnRetry.setOnClickListener {
            binding.overlayGameOver.visibility = View.GONE
            val levelManager = LevelManager(requireContext())
            val level = levelManager.getLevel(currentLevelIndex)
            if (level != null) {
                binding.gameView.loadLevel(level)
            }
        }

        binding.btnGameOverBack.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun submitScore(score: Int, levelId: String, levelName: String) {
        if (!userRepo.isLoggedIn) return
        lifecycleScope.launch {
            leaderboardRepo.submitScore(score, levelId, levelName)
            userRepo.updateProgress(score, currentLevelIndex)
        }
    }

    override fun onPause() {
        super.onPause()
        binding.gameView.pause()
    }

    override fun onResume() {
        super.onResume()
        binding.gameView.resume()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.gameView.cleanup()
        _binding = null
    }
}
