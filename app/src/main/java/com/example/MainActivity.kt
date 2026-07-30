package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.LiveScoringScreen
import com.example.ui.screens.MatchSetupScreen
import com.example.ui.screens.MatchSummaryScreen
import com.example.ui.screens.ScorecardScreen
import com.example.ui.theme.DarkBgMain
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.AppScreen
import com.example.viewmodel.MatchViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val viewModel: MatchViewModel = viewModel()
                val currentScreen by viewModel.currentScreen.collectAsState()
                val activeMatch by viewModel.activeMatch.collectAsState()
                val activeTab by viewModel.selectedDashboardTab.collectAsState()

                if (currentScreen != AppScreen.Dashboard || activeTab != 0) {
                    BackHandler {
                        if (currentScreen == AppScreen.Dashboard) {
                            if (activeTab == 1 || activeTab == 2) {
                                viewModel.selectedDashboardTab.value = 0
                            }
                        } else {
                            when (currentScreen) {
                                is AppScreen.Setup -> viewModel.navigateTo(AppScreen.Dashboard)
                                is AppScreen.Scoring -> viewModel.navigateTo(AppScreen.Dashboard)
                                is AppScreen.Summary -> viewModel.navigateTo(AppScreen.Dashboard)
                                is AppScreen.Scorecard -> {
                                    val match = activeMatch
                                    if (match != null && match.status == "COMPLETED") {
                                        viewModel.navigateTo(AppScreen.Summary)
                                    } else {
                                        viewModel.navigateTo(AppScreen.Scoring)
                                    }
                                }
                                is AppScreen.Analysis -> {
                                    val match = activeMatch
                                    if (match != null && match.status == "COMPLETED") {
                                        viewModel.navigateTo(AppScreen.Summary)
                                    } else {
                                        viewModel.navigateTo(AppScreen.Scoring)
                                    }
                                }
                                else -> viewModel.navigateTo(AppScreen.Dashboard)
                            }
                        }
                    }
                }

                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(DarkBgMain),
                    containerColor = DarkBgMain
                ) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        when (currentScreen) {
                            is AppScreen.Dashboard -> {
                                DashboardScreen(viewModel = viewModel)
                            }
                            is AppScreen.Setup -> {
                                MatchSetupScreen(viewModel = viewModel)
                            }
                            is AppScreen.Scoring -> {
                                LiveScoringScreen(viewModel = viewModel)
                            }
                            is AppScreen.Scorecard -> {
                                ScorecardScreen(viewModel = viewModel)
                            }
                            is AppScreen.Summary -> {
                                MatchSummaryScreen(viewModel = viewModel)
                            }
                            is AppScreen.Analysis -> {
                                com.example.ui.screens.AnalysisScreen(viewModel = viewModel)
                            }
                        }
                    }
                }
            }
        }
    }
}
