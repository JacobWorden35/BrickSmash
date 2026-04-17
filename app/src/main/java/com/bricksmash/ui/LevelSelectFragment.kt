package com.bricksmash.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
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
 * Uses a shared GameViewModel to pass the selected level to the Game fragment.
 */
class LevelSelectFragment : Fragment() {

    private var _binding: FragmentLevelSelectBinding? = null
    private val binding get() = _binding!!
    private val levelRepo = LevelRepository()
    private val gameViewModel: GameViewModel by activityViewModels()

    private var builtInLevels = listOf<LevelData>()

    private val adapter = LevelAdapter { position, level ->
        if (level.isCommunity) {
            gameViewModel.setCommunityLevel(level)
        } else {
            gameViewModel.setBuiltInLevel(level, position)
        }
        findNavController().navigate(R.id.action_levelSelect_to_game)
    }

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