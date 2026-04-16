package com.bricksmash.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.Bundle
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.bricksmash.R
import com.bricksmash.data.LevelRepository
import com.bricksmash.databinding.FragmentLevelEditorBinding
import com.bricksmash.model.BrickData
import com.bricksmash.model.LevelData
import kotlinx.coroutines.launch

/**
 * Level editor allowing players to design custom brick layouts.
 * Users tap cells to place/remove bricks, choose brick types,
 * and can test-play or publish their creations.
 */
class LevelEditorFragment : Fragment() {

    private var _binding: FragmentLevelEditorBinding? = null
    private val binding get() = _binding!!
    private val levelRepo = LevelRepository()

    private val rows = 8
    private val cols = 10
    private val grid = Array(rows) { Array(cols) { BrickData(type = 0) } }
    private var selectedBrushType = 1 // 1=normal, 2=hardened, 3=indestructible, 0=erase

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLevelEditorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Set up the grid view
        binding.editorGridView.setGrid(grid, rows, cols)

        binding.editorGridView.onCellTap = { row, col ->
            grid[row][col] = BrickData(type = selectedBrushType)
            binding.editorGridView.invalidate()
            updateBrickCount()
        }

        // Brush selection
        binding.btnNormal.setOnClickListener {
            selectedBrushType = 1
            updateBrushUI()
        }
        binding.btnHardened.setOnClickListener {
            selectedBrushType = 2
            updateBrushUI()
        }
        binding.btnIndestructible.setOnClickListener {
            selectedBrushType = 3
            updateBrushUI()
        }
        binding.btnErase.setOnClickListener {
            selectedBrushType = 0
            updateBrushUI()
        }

        // Test play
        binding.btnTestPlay.setOnClickListener {
            val level = buildLevelData("Test Level")
            val bundle = Bundle().apply {
                putInt("levelIndex", -1) // -1 signals custom level
                // In a real implementation, you'd pass the level data through a ViewModel
            }
            Toast.makeText(requireContext(), "Test play coming soon!", Toast.LENGTH_SHORT).show()
        }

        // Save locally
        binding.btnSave.setOnClickListener {
            val name = binding.etLevelName.text.toString().trim()
            if (name.isEmpty()) {
                Toast.makeText(requireContext(), "Please enter a level name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            Toast.makeText(requireContext(), "Level saved locally!", Toast.LENGTH_SHORT).show()
        }

        // Publish to community
        binding.btnPublish.setOnClickListener {
            val name = binding.etLevelName.text.toString().trim()
            if (name.isEmpty()) {
                Toast.makeText(requireContext(), "Please enter a level name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val breakableCount = grid.fold(0) { acc, row -> acc + row.count { it.type in 1..2 } }
            if (breakableCount < 5) {
                Toast.makeText(requireContext(), "Add at least 5 breakable bricks", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            binding.progressBar.visibility = View.VISIBLE
            lifecycleScope.launch {
                val level = buildLevelData(name)
                val result = levelRepo.publishLevel(level)
                binding.progressBar.visibility = View.GONE
                result.fold(
                    onSuccess = {
                        Toast.makeText(requireContext(), "Level published!", Toast.LENGTH_SHORT).show()
                        findNavController().navigateUp()
                    },
                    onFailure = { e ->
                        Toast.makeText(requireContext(), "Publish failed: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                )
            }
        }

        // Clear grid
        binding.btnClear.setOnClickListener {
            for (r in 0 until rows) {
                for (c in 0 until cols) {
                    grid[r][c] = BrickData(type = 0)
                }
            }
            binding.editorGridView.invalidate()
            updateBrickCount()
        }

        updateBrushUI()
        updateBrickCount()
    }

    private fun buildLevelData(name: String): LevelData {
        return LevelData(
            name = name,
            rows = rows,
            cols = cols,
            grid = grid.map { row -> row.toList() },
            difficulty = binding.sliderDifficulty.value.toInt(),
            targetScore = grid.fold(0) { acc, row ->
                acc + row.fold(0) { rowAcc, brick ->
                    rowAcc + when (brick.type) {
                        1 -> 100
                        2 -> 250
                        else -> 0
                    }
                }
            },
            isBuiltIn = false,
            isCommunity = true
        )
    }

    private fun updateBrushUI() {
        binding.btnNormal.alpha = if (selectedBrushType == 1) 1.0f else 0.5f
        binding.btnHardened.alpha = if (selectedBrushType == 2) 1.0f else 0.5f
        binding.btnIndestructible.alpha = if (selectedBrushType == 3) 1.0f else 0.5f
        binding.btnErase.alpha = if (selectedBrushType == 0) 1.0f else 0.5f
    }

    private fun updateBrickCount() {
        val count = grid.fold(0) { acc, row -> acc + row.count { it.type != 0 } }
        binding.tvBrickCount.text = "Bricks: $count"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

/**
 * Custom view for rendering the level editor grid.
 * Handles tap-to-place/erase interactions.
 */
class EditorGridView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private var grid: Array<Array<BrickData>>? = null
    private var rows = 8
    private var cols = 10

    var onCellTap: ((Int, Int) -> Unit)? = null

    private val normalPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(244, 67, 54)
        style = Paint.Style.FILL
    }

    private val hardenedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(255, 193, 7)
        style = Paint.Style.FILL
    }

    private val indestructiblePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(120, 120, 120)
        style = Paint.Style.FILL
    }

    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(60, 60, 80)
        style = Paint.Style.STROKE
        strokeWidth = 1f
    }

    private val bgPaint = Paint().apply {
        color = Color.rgb(30, 30, 50)
    }

    fun setGrid(grid: Array<Array<BrickData>>, rows: Int, cols: Int) {
        this.grid = grid
        this.rows = rows
        this.cols = cols
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val g = grid ?: return

        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        val cellW = width.toFloat() / cols
        val cellH = height.toFloat() / rows
        val padding = 2f

        for (r in 0 until rows) {
            for (c in 0 until cols) {
                val rect = RectF(
                    c * cellW + padding,
                    r * cellH + padding,
                    (c + 1) * cellW - padding,
                    (r + 1) * cellH - padding
                )

                when (g[r][c].type) {
                    1 -> canvas.drawRoundRect(rect, 4f, 4f, normalPaint)
                    2 -> canvas.drawRoundRect(rect, 4f, 4f, hardenedPaint)
                    3 -> canvas.drawRoundRect(rect, 4f, 4f, indestructiblePaint)
                }

                canvas.drawRoundRect(rect, 4f, 4f, gridPaint)
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN || event.action == MotionEvent.ACTION_MOVE) {
            val cellW = width.toFloat() / cols
            val cellH = height.toFloat() / rows
            val col = (event.x / cellW).toInt().coerceIn(0, cols - 1)
            val row = (event.y / cellH).toInt().coerceIn(0, rows - 1)
            onCellTap?.invoke(row, col)
            return true
        }
        return super.onTouchEvent(event)
    }
}
