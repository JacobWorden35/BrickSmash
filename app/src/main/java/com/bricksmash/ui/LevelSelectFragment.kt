package com.bricksmash.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bricksmash.R
import com.bricksmash.data.LevelRepository
import com.bricksmash.databinding.FragmentLevelSelectBinding
import com.bricksmash.game.LevelManager
import com.bricksmash.model.LevelData
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.launch

/**
 * Screen for selecting which level to play.
 * Shows built-in levels and community levels in separate tabs.
 */
class LevelSelectFragment : Fragment() {

    private var _binding: FragmentLevelSelectBinding? = null
    private val binding get() = _binding!!
    private val levelRepo = LevelRepository()
    private val adapter = LevelAdapter { levelIndex, level ->
        if (level.isCommunity) {
            // For community levels, pass the level index offset by built-in count
            // For now, navigate with a special index
            // TODO: pass community level data through ViewModel
            val bundle = Bundle().apply {
                putInt("levelIndex", levelIndex)
            }
            findNavController().navigate(R.id.action_levelSelect_to_game, bundle)
        } else {
            val bundle = Bundle().apply {
                putInt("levelIndex", levelIndex)
            }
            findNavController().navigate(R.id.action_levelSelect_to_game, bundle)
        }
    }

    private var builtInLevels = listOf<LevelData>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLevelSelectBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val levelManager = LevelManager(requireContext())
        builtInLevels = levelManager.loadBuiltInLevels()

        binding.rvLevels.layoutManager = GridLayoutManager(requireContext(), 3)
        binding.rvLevels.adapter = adapter

        // Tab switching between Built-in and Community
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> showBuiltInLevels()
                    1 -> showCommunityLevels()
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        // Default to built-in levels
        showBuiltInLevels()
    }

    private fun showBuiltInLevels() {
        binding.progressBar.visibility = View.GONE
        binding.tvEmpty.visibility = View.GONE
        adapter.submitList(builtInLevels)
    }

    private fun showCommunityLevels() {
        binding.progressBar.visibility = View.VISIBLE
        binding.tvEmpty.visibility = View.GONE

        lifecycleScope.launch {
            val result = levelRepo.getCommunityLevels()
            binding.progressBar.visibility = View.GONE

            result.fold(
                onSuccess = { levels ->
                    if (levels.isEmpty()) {
                        binding.tvEmpty.visibility = View.VISIBLE
                    }
                    adapter.submitList(levels)
                },
                onFailure = { e ->
                    binding.tvEmpty.text = "Error loading levels: ${e.message}"
                    binding.tvEmpty.visibility = View.VISIBLE
                    adapter.submitList(emptyList())
                }
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    /**
     * Adapter for the level selection grid.
     * Handles both built-in and community levels.
     */
    private class LevelAdapter(
        private val onClick: (Int, LevelData) -> Unit
    ) : RecyclerView.Adapter<LevelAdapter.ViewHolder>() {

        private var levels = listOf<LevelData>()

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvNumber: TextView = view.findViewById(R.id.tvLevelNumber)
            val tvName: TextView = view.findViewById(R.id.tvLevelName)
            val tvDifficulty: TextView = view.findViewById(R.id.tvDifficulty)
        }

        fun submitList(newLevels: List<LevelData>) {
            levels = newLevels
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_level, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val level = levels[position]
            holder.tvNumber.text = "${position + 1}"
            holder.tvName.text = level.name
            holder.tvDifficulty.text = if (level.isCommunity) {
                "by ${level.author}"
            } else {
                "★".repeat(level.difficulty)
            }
            holder.itemView.setOnClickListener { onClick(position, level) }
        }

        override fun getItemCount() = levels.size
    }
}