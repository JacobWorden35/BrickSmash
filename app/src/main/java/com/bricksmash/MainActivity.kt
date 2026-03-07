package com.bricksmash

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.bricksmash.databinding.ActivityMainBinding

/**
 * Single-activity architecture. Hosts the NavHostFragment and
 * manages the bottom navigation bar visibility.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        // Hide bottom nav when in the game screen
        navController.addOnDestinationChangedListener { _, destination, _ ->
            binding.bottomNav.visibility = when (destination.id) {
                R.id.gameFragment -> android.view.View.GONE
                R.id.loginFragment, R.id.registerFragment -> android.view.View.GONE
                else -> android.view.View.VISIBLE
            }
        }

        binding.bottomNav.setupWithNavController(navController)
    }
}
