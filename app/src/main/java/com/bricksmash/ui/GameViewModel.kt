/**
 * Copyright (c) 2026 Jacob Worden
 *
 * Feel free to use this code as a reference
 */
package com.bricksmash.ui

import androidx.lifecycle.ViewModel
import com.bricksmash.model.LevelData

/**
 * Shared ViewModel scoped to the Activity that holds the currently
 * selected level for gameplay. This allows Level Select to pass a full
 * LevelData object (including community levels fetched from Firestore)
 * to the Game fragment without serializing it through nav arguments.
 */
class GameViewModel : ViewModel() {

    /** The level to load when GameFragment starts. */
    var selectedLevel: LevelData? = null

    /** Index within the built-in list, or -1 if community level. Used for "Next Level" flow. */
    var builtInIndex: Int = 0

    /**
     * Sets a built-in level to play, tracked by index so "Next Level"
     * can progress through the list.
     */
    fun setBuiltInLevel(level: LevelData, index: Int) {
        selectedLevel = level
        builtInIndex = index
    }

    /**
     * Sets a community level to play. Index is set to -1 because there
     * is no meaningful "next level" progression for community levels.
     */
    fun setCommunityLevel(level: LevelData) {
        selectedLevel = level
        builtInIndex = -1
    }
}