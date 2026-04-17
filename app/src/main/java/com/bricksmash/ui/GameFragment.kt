package com.bricksmash.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.bricksmash.data.LeaderboardRepository
import com.bricksmash.data.UserRepository
import com.bricksmash.databinding.FragmentGameBinding
import com.bricksmash.game.LevelManager
import com.bricksmash.model.LevelData
import kotlinx.coroutines.launch

/**
 * Fragment that hosts the actual game.
 * Reads the selected level from the shared GameViewModel so both built-in
 * and community levels can be played without index-based lookups.
 */
class GameFragment : Fragment() {

    private var _binding: FragmentGameBinding? = null
    private val binding get() = _binding!!
    private val leaderboardRepo = LeaderboardRepository()
    private val userRepo = UserRepository()
    private val gameViewModel: GameViewModel by activityViewModels()

    private var currentLevel: LevelData? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGameBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        currentLevel = gameViewModel.selectedLevel
        val level = currentLevel
        if (level == null) {
            // No level selected — should not normally happen, but bail safely
            findNavController().navigateUp()
            return
        }

        binding.gameView.loadLevel(level)

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

        // "Next Level" only makes sense for built-in levels
        if (gameViewModel.builtInIndex >= 0) {
            binding.btnNextLevel.visibility = View.VISIBLE
            binding.btnNextLevel.setOnClickListener {
                binding.overlayComplete.visibility = View.GONE
                val nextIndex = gameViewModel.builtInIndex + 1
                val levelManager = LevelManager(requireContext())
                val nextLevel = levelManager.getLevel(nextIndex)
                if (nextLevel != null) {
                    gameViewModel.setBuiltInLevel(nextLevel, nextIndex)
                    currentLevel = nextLevel
                    binding.gameView.loadLevel(nextLevel)
                } else {
                    findNavController().navigateUp()
                }
            }
        } else {
            binding.btnNextLevel.visibility = View.GONE
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
            currentLevel?.let { binding.gameView.loadLevel(it) }
        }

        binding.btnGameOverBack.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun submitScore(score: Int, levelId: String, levelName: String) {
        if (!userRepo.isLoggedIn) return
        lifecycleScope.launch {
            leaderboardRepo.submitScore(score, levelId, levelName)
            // Only update progress for built-in levels
            if (gameViewModel.builtInIndex >= 0) {
                userRepo.updateProgress(score, gameViewModel.builtInIndex)
            }
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