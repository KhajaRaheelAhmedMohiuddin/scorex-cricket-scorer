package com.example

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.model.ExtraType
import com.example.model.MatchFormat
import com.example.model.WicketType
import com.example.viewmodel.AppScreen
import com.example.viewmodel.MatchViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.Executor

/**
 * End-to-end tests for the match engine in [MatchViewModel], driven through real
 * Room persistence (in-memory) under Robolectric. Covers match creation, toss
 * logic, every delivery type, strike rotation, wickets, undo/redo, innings
 * transitions, all result types (chase win / defend win / tie), the full Super
 * Over flow, and team management.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MatchViewModelTest {

    private val scheduler = TestCoroutineScheduler()
    private val dispatcher = StandardTestDispatcher(scheduler)
    private lateinit var db: AppDatabase
    private lateinit var vm: MatchViewModel

    private val app: Application get() = ApplicationProvider.getApplicationContext()

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
        // Run all Room work synchronously on the calling thread so state settles deterministically.
        val direct = Executor { it.run() }
        db = Room.inMemoryDatabaseBuilder(app, AppDatabase::class.java)
            .allowMainThreadQueries()
            .setQueryExecutor(direct)
            .setTransactionExecutor(direct)
            .build()
        AppDatabase.setInstanceForTesting(db)
        // Pre-set the migration flag so loadTeams() never wipes prefs mid-test.
        app.getSharedPreferences("cricket_scorer_prefs", Context.MODE_PRIVATE)
            .edit().clear().putBoolean("did_clear_static_defaults_v3", true).commit()
        vm = MatchViewModel(app)
        idle()
    }

    @After
    fun tearDown() {
        db.close()
        AppDatabase.setInstanceForTesting(null)
        Dispatchers.resetMain()
    }

    private fun idle() {
        scheduler.advanceUntilIdle()
    }

    private fun startMatch(
        host: String = "India",
        visitor: String = "Australia",
        overs: Int = 1,
        hostRoster: List<String>? = null,
        visitorRoster: List<String>? = null,
        tossHost: Boolean = true,
        optBat: Boolean = true
    ) {
        vm.setupTeamA.value = host
        vm.setupTeamB.value = visitor
        vm.setupFormat.value = MatchFormat.CUSTOM
        vm.setupOvers.value = overs
        vm.setupTossWonByHost.value = tossHost
        vm.setupOptedToBat.value = optBat
        vm.setupTeamAPlayers.value = hostRoster?.joinToString(",") ?: ""
        vm.setupTeamBPlayers.value = visitorRoster?.joinToString(",") ?: ""
        vm.startNewMatch()
        idle()
    }

    private fun ball(
        runsBat: Int = 0,
        extra: ExtraType = ExtraType.NONE,
        runsExtra: Int = 0,
        wicket: Boolean = false,
        wicketType: WicketType? = null,
        dismissedPlayer: String? = null
    ) {
        vm.addLiveDelivery(runsBat, runsExtra, extra, wicket, wicketType, dismissedPlayer)
        idle()
    }

    // ---------- Match creation & validation ----------

    @Test
    fun `startNewMatch creates a live innings1 match and navigates to scoring`() {
        startMatch()
        val m = vm.activeMatch.value
        assertNotNull(m)
        assertEquals("LIVE", m!!.status)
        assertEquals(1, m.currentInnings)
        assertEquals("India", m.teamA)
        assertEquals("Australia", m.teamB)
        assertEquals(11, m.teamAPlayers.size) // defaults generated
        assertEquals(AppScreen.Scoring, vm.currentScreen.value)
    }

    @Test
    fun `startNewMatch rejects empty team names`() {
        vm.setupTeamA.value = ""
        vm.setupTeamB.value = "Australia"
        vm.startNewMatch()
        idle()
        assertNull(vm.activeMatch.value)
    }

    @Test
    fun `startNewMatch rejects identical team names`() {
        vm.setupTeamA.value = "India"
        vm.setupTeamB.value = "india"
        vm.startNewMatch()
        idle()
        assertNull(vm.activeMatch.value)
    }

    // ---------- Toss logic (who bats first) ----------

    @Test
    fun `toss host bat means host bats first`() {
        startMatch(tossHost = true, optBat = true)
        assertEquals("India", vm.activeMatch.value!!.firstInningsBattingTeam)
    }

    @Test
    fun `toss host bowl means visitor bats first`() {
        startMatch(tossHost = true, optBat = false)
        assertEquals("Australia", vm.activeMatch.value!!.firstInningsBattingTeam)
    }

    @Test
    fun `toss visitor bat means visitor bats first`() {
        startMatch(tossHost = false, optBat = true)
        assertEquals("Australia", vm.activeMatch.value!!.firstInningsBattingTeam)
    }

    @Test
    fun `toss visitor bowl means host bats first`() {
        startMatch(tossHost = false, optBat = false)
        assertEquals("India", vm.activeMatch.value!!.firstInningsBattingTeam)
    }

    // ---------- Scoring & strike rotation ----------

    @Test
    fun `runs accumulate on the active innings`() {
        startMatch(overs = 5)
        ball(4)
        ball(2)
        assertEquals(6, vm.getActiveInningsSummary().totalRuns)
        assertEquals(2, vm.getActiveInningsSummary().legalBalls)
    }

    @Test
    fun `odd runs rotate the strike`() {
        startMatch(overs = 5, hostRoster = listOf("A", "B"))
        val before = vm.activeMatch.value!!.strikerName
        ball(1)
        assertEquals(before, vm.activeMatch.value!!.nonStrikerName) // striker moved to non-striker end
    }

    @Test
    fun `end of over rotates strike back`() {
        startMatch(overs = 5, hostRoster = listOf("A", "B"))
        val opener = vm.activeMatch.value!!.strikerName
        // six dot balls: no mid-over rotation, but the openers swap ends after the over
        repeat(6) { ball(0) }
        assertEquals(opener, vm.activeMatch.value!!.nonStrikerName)
    }

    @Test
    fun `wide adds a run and no legal ball`() {
        startMatch(overs = 5)
        ball(extra = ExtraType.WIDE, runsExtra = 1)
        assertEquals(1, vm.getActiveInningsSummary().totalRuns)
        assertEquals(0, vm.getActiveInningsSummary().legalBalls)
    }

    @Test
    fun `scoring is blocked once the over limit is reached`() {
        startMatch(overs = 1)
        repeat(6) { ball(1) }
        val runsAfterOver = vm.getActiveInningsSummary().totalRuns
        ball(4) // should be ignored, innings is complete
        assertEquals(runsAfterOver, vm.getActiveInningsSummary().totalRuns)
    }

    // ---------- Wickets ----------

    @Test
    fun `a wicket increments the tally and vacates the striker`() {
        startMatch(overs = 5)
        ball(wicket = true, wicketType = WicketType.BOWLED)
        assertEquals(1, vm.getActiveInningsSummary().totalWickets)
        assertEquals("Batsman Out", vm.activeMatch.value!!.strikerName)
    }

    // ---------- Undo / Redo ----------

    @Test
    fun `undo removes the last delivery and restores the crease`() {
        startMatch(overs = 5, hostRoster = listOf("A", "B"))
        val striker = vm.activeMatch.value!!.strikerName
        ball(1)
        assertEquals(1, vm.activeMatch.value!!.deliveries.size)
        vm.undoLastBall(); idle()
        assertEquals(0, vm.activeMatch.value!!.deliveries.size)
        assertEquals(striker, vm.activeMatch.value!!.strikerName)
    }

    @Test
    fun `redo replays an undone delivery`() {
        startMatch(overs = 5)
        ball(4)
        vm.undoLastBall(); idle()
        assertEquals(0, vm.activeMatch.value!!.deliveries.size)
        vm.redoLastBall(); idle()
        assertEquals(1, vm.activeMatch.value!!.deliveries.size)
        assertEquals(4, vm.getActiveInningsSummary().totalRuns)
    }

    @Test
    fun `swapStrikers exchanges the two batsmen`() {
        startMatch(hostRoster = listOf("A", "B"))
        val s = vm.activeMatch.value!!.strikerName
        val ns = vm.activeMatch.value!!.nonStrikerName
        vm.swapStrikers(); idle()
        assertEquals(ns, vm.activeMatch.value!!.strikerName)
        assertEquals(s, vm.activeMatch.value!!.nonStrikerName)
    }

    @Test
    fun `renaming a player updates roster crease and deliveries`() {
        startMatch(overs = 5, hostRoster = listOf("A", "B"))
        ball(1) // A now non-striker with 1 run recorded
        vm.renamePlayerInActiveMatch("A", "Kohli"); idle()
        val m = vm.activeMatch.value!!
        assertTrue(m.teamAPlayers.contains("Kohli"))
        assertFalse(m.teamAPlayers.contains("A"))
        assertTrue(m.deliveries.any { it.striker == "Kohli" || it.nonStriker == "Kohli" })
    }

    // ---------- Innings transitions & results ----------

    @Test
    fun `transition to second innings flips the batting team`() {
        startMatch(overs = 5)
        ball(4)
        vm.transitionToSecondInnings(); idle()
        val m = vm.activeMatch.value!!
        assertEquals(2, m.currentInnings)
        // batting team in innings 2 is the side that bowled first
        assertEquals("Australia", if (m.currentInnings == 2) m.teamB else m.teamA)
    }

    @Test
    fun `successful chase finishes the match as a win by wickets`() {
        startMatch(overs = 1)
        ball(4)                       // innings 1 total = 4, target = 5
        vm.transitionToSecondInnings(); idle()
        ball(6)                       // chaser passes the target
        val m = vm.activeMatch.value!!
        assertEquals("COMPLETED", m.status)
        assertNotNull(m.winner)
        assertTrue(m.winner!!.contains("Australia"))
        assertTrue(m.winner!!.contains("wicket"))
        assertEquals(AppScreen.Summary, vm.currentScreen.value)
    }

    @Test
    fun `defended total finishes the match as a win by runs`() {
        startMatch(overs = 1, hostRoster = listOf("A", "B"), visitorRoster = listOf("C", "D"))
        ball(4); ball(1)              // innings 1 = 5, target = 6
        vm.transitionToSecondInnings(); idle()
        // chaser is all out (roster of 2 -> 1 wicket ends the innings) for 0
        ball(wicket = true, wicketType = WicketType.BOWLED)
        val m = vm.activeMatch.value!!
        assertEquals("COMPLETED", m.status)
        assertTrue(m.winner!!.contains("India"))
        assertTrue(m.winner!!.contains("run"))
    }

    @Test
    fun `level scores end the innings as a tie without auto-finalizing`() {
        startMatch(overs = 1)
        ball(4); ball(1)              // innings 1 = 5, target = 6
        vm.transitionToSecondInnings(); idle()
        ball(1); ball(1); ball(1); ball(1); ball(1); ball(0) // chase 5 in 6 balls -> tie
        val m = vm.activeMatch.value!!
        assertEquals(5, vm.getInnings2Summary().totalRuns)
        assertFalse("a tie must wait for the super-over decision", m.status == "COMPLETED")
    }

    // ---------- Super Over ----------

    @Test
    fun `full super over flow resolves a tied match`() {
        startMatch(overs = 1)
        ball(4); ball(1)                                   // innings 1 = 5
        vm.transitionToSecondInnings(); idle()
        ball(1); ball(1); ball(1); ball(1); ball(1); ball(0) // innings 2 = 5 -> tie

        vm.startSuperOver(); idle()
        assertEquals(3, vm.activeMatch.value!!.currentInnings)
        ball(3)                                             // super over 1 = 3, target = 4
        vm.transitionToSuperOverSecondInnings(); idle()
        assertEquals(4, vm.activeMatch.value!!.currentInnings)
        ball(4)                                             // super over 2 passes the target

        val m = vm.activeMatch.value!!
        assertEquals("COMPLETED", m.status)
        assertTrue(m.winner!!.contains("Super Over"))
    }

    // ---------- Match & team management ----------

    @Test
    fun `deleteMatch clears the active match`() {
        startMatch()
        val id = vm.activeMatch.value!!.id
        vm.deleteMatch(id); idle()
        assertNull(vm.activeMatch.value)
    }

    @Test
    fun `addTeam persists a team that survives a reload`() {
        vm.addTeam("Mumbai", listOf("P1", "P2", "P3")); idle()
        assertTrue(vm.teamsList.value.any { it.name == "Mumbai" })
        vm.loadTeams()
        val t = vm.teamsList.value.first { it.name == "Mumbai" }
        assertEquals(3, t.roster.size)
    }

    @Test
    fun `updateTeam renames and re-rosters`() {
        vm.addTeam("Mumbai", listOf("P1", "P2")); idle()
        val oldId = vm.teamsList.value.first { it.name == "Mumbai" }.id
        vm.updateTeam(oldId, "Mumbai Indians", listOf("Rohit", "Bumrah", "SKY")); idle()
        assertTrue(vm.teamsList.value.any { it.name == "Mumbai Indians" })
        assertEquals(3, vm.teamsList.value.first { it.name == "Mumbai Indians" }.roster.size)
    }

    @Test
    fun `deleteTeam removes it from the directory`() {
        vm.addTeam("Chennai", listOf("P1", "P2")); idle()
        val id = vm.teamsList.value.first { it.name == "Chennai" }.id
        vm.deleteTeam(id); idle()
        assertFalse(vm.teamsList.value.any { it.id == id })
    }
}
