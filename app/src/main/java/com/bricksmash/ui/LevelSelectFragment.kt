package com.bricksmash.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bricksmash.R
import com.bricksmash.databinding.FragmentLevelSelectBinding
import com.bricksmash.game.LevelManager
import com.bricksmash.model.LevelData

/**
 * Screen for selecting which level to play.
 * Shows built-in levels in a grid layout.
 */
class LevelSelectFragment : Fragment() {

    private var _binding: FragmentLevelSelectBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLevelSelectBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val levelManager = LevelManager(requireContext())
        val levels = levelManager.loadBuiltInLevels()

        binding.rvLevels.layoutManager = GridLayoutManager(requireContext(), 3)
        binding.rvLevels.adapter = LevelAdapter(levels) { levelIndex ->
            val bundle = Bundle().apply {
                putInt("levelIndex", levelIndex)
            }
            findNavController().navigate(R.id.action_levelSelect_to_game, bundle)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    /**
     * Simple adapter for the level selection grid.
     */
    private class LevelAdapter(
        private val levels: List<LevelData>,
        private val onClick: (Int) -> Unit
    ) : RecyclerView.Adapter<LevelAdapter.ViewHolder>() {

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvNumber: TextView = view.findViewById(R.id.tvLevelNumber)
            val tvName: TextView = view.findViewById(R.id.tvLevelName)
            val tvDifficulty: TextView = view.findViewById(R.id.tvDifficulty)
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
            holder.tvDifficulty.text = "★".repeat(level.difficulty)
            holder.itemView.setOnClickListener { onClick(position) }
        }

        override fun getItemCount() = levels.size
    }
}
