/**
 * Copyright (c) 2026 Jacob Worden
 *
 * Feel free to use this code as a reference
 */
package com.bricksmash.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bricksmash.R
import com.bricksmash.data.LeaderboardRepository
import com.bricksmash.databinding.FragmentLeaderboardBinding
import com.bricksmash.game.LevelManager
import com.bricksmash.model.LevelData
import com.bricksmash.model.ScoreEntry
import com.google.android.material.tabs.TabLayout

/**
 * Displays the global and per-level leaderboards with
 * real-time updates from Firestore snapshot listeners.
 *
 * The "By Level" tab uses a dropdown to select which level
 * to view scores for. Real-time listener is rebound each time
 * the selection changes.
 */
class LeaderboardFragment : Fragment() {

    private var _binding: FragmentLeaderboardBinding? = null
    private val binding get() = _binding!!
    private val leaderboardRepo = LeaderboardRepository()
    private val adapter = ScoreAdapter()
    private var allLevels: List<LevelData> = emptyList()

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

        // Load level list for the dropdown
        allLevels = LevelManager(requireContext()).loadBuiltInLevels()
        val levelNames = allLevels.mapIndexed { i, lvl -> "${i + 1}. ${lvl.name}" }

        val spinnerAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            levelNames
        )
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerLevel.adapter = spinnerAdapter

        binding.spinnerLevel.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?, view: View?, position: Int, id: Long
            ) {
                val selectedLevel = allLevels.getOrNull(position) ?: return
                loadLevelScores(selectedLevel.id)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Tab switching between Global and Per-Level
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> {
                        binding.spinnerLevel.visibility = View.GONE
                        loadGlobalScores()
                    }
                    1 -> {
                        binding.spinnerLevel.visibility = View.VISIBLE
                        val selectedPos = binding.spinnerLevel.selectedItemPosition
                        val level = allLevels.getOrNull(selectedPos) ?: allLevels.firstOrNull()
                        level?.let { loadLevelScores(it.id) }
                    }
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        loadGlobalScores()
    }

    private fun loadGlobalScores() {
        binding.progressBar.visibility = View.VISIBLE
        binding.tvEmpty.visibility = View.GONE
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

    private fun loadLevelScores(levelId: String) {
        binding.progressBar.visibility = View.VISIBLE
        binding.tvEmpty.visibility = View.GONE
        adapter.submitList(emptyList())

        leaderboardRepo.listenToLeaderboard(
            levelId = levelId,
            onUpdate = { scores ->
                activity?.runOnUiThread {
                    binding.progressBar.visibility = View.GONE
                    adapter.submitList(scores)
                    if (scores.isEmpty()) {
                        binding.tvEmpty.text = "No scores yet. Be the first!"
                        binding.tvEmpty.visibility = View.VISIBLE
                    } else {
                        binding.tvEmpty.visibility = View.GONE
                    }
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