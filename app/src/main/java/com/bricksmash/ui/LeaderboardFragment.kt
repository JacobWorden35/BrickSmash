package com.bricksmash.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bricksmash.R
import com.bricksmash.data.LeaderboardRepository
import com.bricksmash.databinding.FragmentLeaderboardBinding
import com.bricksmash.model.ScoreEntry
import com.google.android.material.tabs.TabLayout

/**
 * Displays the global and per-level leaderboards with
 * real-time updates from Firestore snapshot listeners.
 */
class LeaderboardFragment : Fragment() {

    private var _binding: FragmentLeaderboardBinding? = null
    private val binding get() = _binding!!
    private val leaderboardRepo = LeaderboardRepository()
    private val adapter = ScoreAdapter()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLeaderboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rvScores.layoutManager = LinearLayoutManager(requireContext())
        binding.rvScores.adapter = adapter

        // Tab switching between Global and Per-Level
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> loadGlobalScores()
                    1 -> loadLevelScores()
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        loadGlobalScores()
    }

    private fun loadGlobalScores() {
        binding.progressBar.visibility = View.VISIBLE
        leaderboardRepo.listenToLeaderboard(
            levelId = null,
            onUpdate = { scores ->
                activity?.runOnUiThread {
                    binding.progressBar.visibility = View.GONE
                    adapter.submitList(scores)
                    binding.tvEmpty.visibility = if (scores.isEmpty()) View.VISIBLE else View.GONE
                }
            },
            onError = { e ->
                activity?.runOnUiThread {
                    binding.progressBar.visibility = View.GONE
                    binding.tvEmpty.text = "Error loading scores: ${e.message}"
                    binding.tvEmpty.visibility = View.VISIBLE
                }
            }
        )
    }

    private fun loadLevelScores() {
        // For simplicity, filter to first built-in level
        // In a full implementation, you'd add a level picker
        binding.progressBar.visibility = View.VISIBLE
        leaderboardRepo.listenToLeaderboard(
            levelId = "builtin_1",
            onUpdate = { scores ->
                activity?.runOnUiThread {
                    binding.progressBar.visibility = View.GONE
                    adapter.submitList(scores)
                    binding.tvEmpty.visibility = if (scores.isEmpty()) View.VISIBLE else View.GONE
                }
            },
            onError = { e ->
                activity?.runOnUiThread {
                    binding.progressBar.visibility = View.GONE
                    binding.tvEmpty.text = "Error: ${e.message}"
                    binding.tvEmpty.visibility = View.VISIBLE
                }
            }
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        leaderboardRepo.removeListener()
        _binding = null
    }

    /**
     * RecyclerView adapter for displaying score entries.
     */
    private class ScoreAdapter : RecyclerView.Adapter<ScoreAdapter.ViewHolder>() {
        private var scores = listOf<ScoreEntry>()

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvRank: TextView = view.findViewById(R.id.tvRank)
            val tvName: TextView = view.findViewById(R.id.tvPlayerName)
            val tvScore: TextView = view.findViewById(R.id.tvScore)
            val tvLevel: TextView = view.findViewById(R.id.tvLevelInfo)
        }

        fun submitList(newScores: List<ScoreEntry>) {
            scores = newScores
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_score, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val entry = scores[position]
            holder.tvRank.text = "#${position + 1}"
            holder.tvName.text = entry.displayName
            holder.tvScore.text = "${entry.score}"
            holder.tvLevel.text = entry.levelName
        }

        override fun getItemCount() = scores.size
    }
}
