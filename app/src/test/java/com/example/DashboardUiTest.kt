package com.example

import android.app.Application
import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.ui.screens.DashboardScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.MatchViewModel
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.util.concurrent.Executor

/**
 * Compose UI tests for the dashboard, running on the JVM via Robolectric (no
 * emulator required). They render the real screen backed by a real ViewModel and
 * drive it through the testTags wired into the production composables.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class DashboardUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var db: AppDatabase
    private lateinit var vm: MatchViewModel
    private val app: Application get() = ApplicationProvider.getApplicationContext()

    @Before
    fun setup() {
        val direct = Executor { it.run() }
        db = Room.inMemoryDatabaseBuilder(app, AppDatabase::class.java)
            .allowMainThreadQueries()
            .setQueryExecutor(direct)
            .setTransactionExecutor(direct)
            .build()
        AppDatabase.setInstanceForTesting(db)
        app.getSharedPreferences("cricket_scorer_prefs", Context.MODE_PRIVATE)
            .edit().clear().putBoolean("did_clear_static_defaults_v3", true).commit()
        vm = MatchViewModel(app)
    }

    @After
    fun tearDown() {
        db.close()
        AppDatabase.setInstanceForTesting(null)
    }

    private fun renderDashboard() {
        composeTestRule.setContent {
            MyApplicationTheme {
                DashboardScreen(viewModel = vm)
            }
        }
    }

    @Test
    fun `new match tab shows team inputs and the start control`() {
        renderDashboard()
        composeTestRule.onNodeWithTag("host_team_input").assertIsDisplayed()
        composeTestRule.onNodeWithTag("visitor_team_input").assertIsDisplayed()
        // the slide-to-start control sits below the fold, so scroll it into view first
        composeTestRule.onNodeWithTag("incept_match_btn_tab").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `typing a team name is reflected in the field`() {
        renderDashboard()
        composeTestRule.onNodeWithTag("host_team_input").performTextInput("India")
        composeTestRule.onNodeWithText("India", substring = true).assertIsDisplayed()
    }

    @Test
    fun `teams tab reveals the add-team control and empty state`() {
        renderDashboard()
        composeTestRule.onNodeWithTag("nav_tab_teams").performClick()
        composeTestRule.onNodeWithTag("add_team_fab").assertIsDisplayed()
        composeTestRule.onNodeWithText("No custom teams", substring = true).assertIsDisplayed()
    }

    @Test
    fun `history tab shows the empty history message`() {
        renderDashboard()
        composeTestRule.onNodeWithTag("nav_tab_history").performClick()
        composeTestRule.onNodeWithText("Start your first match", substring = true).assertIsDisplayed()
    }
}
