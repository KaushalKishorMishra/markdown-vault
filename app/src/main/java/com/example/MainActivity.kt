package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import com.example.data.GitSyncScheduler
import com.example.ui.screens.DashboardScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.VaultViewModel
import com.example.ui.viewmodel.VaultViewModelFactory

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Schedule the background periodic automatic Git synchronizer
        GitSyncScheduler.schedulePeriodicSync(applicationContext)

        val viewModel = ViewModelProvider(
            this,
            VaultViewModelFactory(application)
        )[VaultViewModel::class.java]

        setContent {
            val selectedTheme by viewModel.selectedTheme.collectAsState()
            MyApplicationTheme(themeName = selectedTheme) {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    DashboardScreen(
                        viewModel = viewModel,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}
