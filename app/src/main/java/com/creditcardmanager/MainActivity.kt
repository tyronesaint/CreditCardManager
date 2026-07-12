package com.creditcardmanager

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.creditcardmanager.databinding.ActivityMainBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        // Setup bottom nav with custom behavior
        binding.bottomNavigation.setupWithNavController(navController)

        // Custom handling: when clicking already selected tab, pop to start destination
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            val currentDestination = navController.currentDestination?.id
            if (currentDestination == item.itemId) {
                // Already on this tab, pop to its start destination
                navController.popBackStack(item.itemId, false)
                true
            } else {
                // Normal navigation
                val handled = androidx.navigation.ui.NavigationUI.onNavDestinationSelected(item, navController)
                if (handled) {
                    // Pop up to start destination to avoid back stack buildup
                    val startId = navController.graph.startDestinationId
                    if (currentDestination != null && currentDestination != startId) {
                        navController.popBackStack(startId, false)
                    }
                }
                handled
            }
        }

        // Also handle re-selection to pop to start
        binding.bottomNavigation.setOnItemReselectedListener { item ->
            navController.popBackStack(item.itemId, false)
        }

        // Handle notification deep links
        handleIntentExtras()
    }

    override fun onNewIntent(intent: android.content.Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntentExtras()
    }

    private fun handleIntentExtras() {
        val navigateTo = intent?.getStringExtra("navigate_to")
        when (navigateTo) {
            "payments" -> navController.navigate(R.id.paymentRemindersFragment)
            "activities" -> navController.navigate(R.id.activitiesFragment)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
}
