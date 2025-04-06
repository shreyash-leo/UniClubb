package com.example.uniclubb

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.MenuItem
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.fragment.app.Fragment
import com.example.uniclubb.databinding.ActivityHomeBinding
import com.example.uniclubb.fragments.*
import com.google.firebase.auth.FirebaseAuth

class HomeActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var binding: ActivityHomeBinding
    private lateinit var toggle: ActionBarDrawerToggle

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // View Binding
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Firebase authentication
        auth = FirebaseAuth.getInstance()

        // If user is not logged in, navigate to Login screen
        if (auth.currentUser == null) {
            navigateToLogin()
            return
        }

        // Set the Toolbar (Top Navigation Bar)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        binding.toolbar.setBackgroundColor(ContextCompat.getColor(this, R.color.yellow))

        // Extend the content behind the status bar
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Handle Navigation Drawer Item Clicks
        binding.navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_logout -> {
                    logoutUser()
                    true
                }
                else -> false
            }
        }

        // Set default fragment
        if (savedInstanceState == null) {
            loadFragment(HomeFragment())
        }

        // Bottom Navigation
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            val fragment = when (item.itemId) {
                R.id.nav_home -> HomeFragment()
                R.id.nav_discover -> ClubsFragment()
                R.id.nav_chat -> ChatFragment()
                R.id.nav_join_club -> ConnectedFragment()
                R.id.nav_profile -> ProfileFragment()
                else -> null
            }
            fragment?.let { loadFragment(it) }
            true
        }

        // Drawer Toggle with Custom Menu Icon
        toggle = ActionBarDrawerToggle(
            this, binding.drawerLayout, binding.toolbar,
            R.string.open_drawer, R.string.close_drawer
        )
        binding.drawerLayout.addDrawerListener(toggle)
        toggle.syncState()
        binding.toolbar.navigationIcon = ContextCompat.getDrawable(this, R.drawable.ic_custom_menu)

        // Notification Icon Click
        binding.toolbar.findViewById<android.widget.ImageView>(R.id.notification_icon).setOnClickListener {
            loadFragment(NotificationsFragment())
        }

        // 🔍 Search EditText functionality
        val searchEditText = binding.toolbar.findViewById<EditText>(R.id.search_edit_text)
        searchEditText.setOnEditorActionListener { v, actionId, event ->
            val query = searchEditText.text.toString().trim()
            if (query.isNotEmpty()) {
                Toast.makeText(this, "Searching for: $query", Toast.LENGTH_SHORT).show()
                // ➤ You can call your Search Logic here
            }
            true
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                if (binding.drawerLayout.isDrawerOpen(Gravity.LEFT)) {
                    binding.drawerLayout.closeDrawer(Gravity.LEFT)
                } else {
                    binding.drawerLayout.openDrawer(Gravity.LEFT)
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun loadFragment(fragment: Fragment) {
        when (fragment) {
            is HomeFragment, is ClubsFragment, is ChatFragment, is ConnectedFragment -> {
                binding.toolbar.visibility = android.view.View.VISIBLE
            }
            else -> {
                binding.toolbar.visibility = android.view.View.GONE
            }
        }

        supportFragmentManager.beginTransaction()
            .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    private fun navigateToLogin() {
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }

    private fun logoutUser() {
        auth.signOut()
        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show()
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
