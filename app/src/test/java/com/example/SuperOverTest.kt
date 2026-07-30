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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.Executor

/**
 * Dedicated coverage of the complete Super Over tie-breaker in [MatchViewModel]:
 * reaching a tie, entering the Super Over, both innings, and every outcome
 * (won by wickets, defended by runs, and a tied Super Over), plus the 6-ball cap.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SuperOverTest {

    private val scheduler = TestCoroutineScheduler()
    private val dispatcher = StandardTestDispatcher(scheduler)
    private lateinit var db: AppDatabase
    private lateinit var vm: MatchViewModel
    private val app: Application get() = ApplicationProvider.getApplicationContext()

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
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
        idle()
    }

    @After
    fun tearDown() {
        db.close()
        AppDatabase.setInstanceForTesting(null)
        Dispatchers.resetMain()
    }

    private fun idle() = scheduler.advanceUntilIdle()

    private fun startMatch() {
        vm.setupTeamA.value = "India"
        vm.setupTeamB.value = "Australia"
        vm.setupFormat.value = MatchFormat.CUSTOM
        vm.setupOvers.value = 1
        vm.setupTossWonByHost.value = true
        vm.setupOptedToBat.value = true
        vm.setupTeamAPlayers.value = ""
        vm.setupTeamBPlayers.value = ""
        vm.startNewMatch()
        idle()
    }

    private fun ball(runsBat: Int = 0, extra: ExtraType = ExtraType.NONE, runsExtra: Int = 0) {
        vm.addLiveDelivery(runsBat, runsExtra, extra, false, null, null)
        idle()
    }

    /** Leaves the ViewModel at a tied main match: innings 2, scores level 5-5, status LIVE. */
    private fun tieTheMatch() {
        startMatch()                              // India bats first
        ball(4); ball(1)                          // India innings 1 = 5
        vm.transitionToSecondInnings(); idle()
        ball(1); ball(1); ball(1); ball(1); ball(1); ball(0) // Australia innings 2 = 5 -> tie
    }

    @Test
    fun `a level chase produces a tie that is not auto-finalized`() {
        tieTheMatch()
        val m = vm.activeMatch.value!!
        assertEquals(5, vm.getInnings1Summary().totalRuns)
        assertEquals(5, vm.getInnings2Summary().totalRuns)
        assertEquals(2, m.currentInnings)
        assertFalse("a tie must wait for the super-over decision", m.status == "COMPLETED")
    }

    @Test
    fun `startSuperOver enters innings 3 with the chasing side batting`() {
        tieTheMatch()
        vm.startSuperOver(); idle()
        val m = vm.activeMatch.value!!
        assertEquals(3, m.currentInnings)
        // the side that chased in the main match (Australia / teamB) bats the Super Over first
        assertTrue(m.teamBPlayers.contains(m.strikerName))
        assertTrue(m.teamBPlayers.contains(m.nonStrikerName))
    }

    @Test
    fun `super over first innings is capped at six legal balls`() {
        tieTheMatch()
        vm.startSuperOver(); idle()
        repeat(6) { ball(1) }                     // 6 legal balls = full super over
        val runsAfterSix = vm.getInnings3Summary().totalRuns
        ball(4)                                    // must be ignored
        assertEquals(6, vm.getInnings3Summary().legalBalls)
        assertEquals(runsAfterSix, vm.getInnings3Summary().totalRuns)
    }

    @Test
    fun `super over won by the chasing side by wickets`() {
        tieTheMatch()
        vm.startSuperOver(); idle()
        ball(3)                                    // SO innings 1 (Australia) = 3, target = 4
        vm.transitionToSuperOverSecondInnings(); idle()
        assertEquals(4, vm.activeMatch.value!!.currentInnings)
        ball(4)                                    // SO innings 2 (India) passes the target
        val m = vm.activeMatch.value!!
        assertEquals("COMPLETED", m.status)
        assertTrue(m.winner!!.contains("Super Over"))
        assertTrue(m.winner!!.contains("India"))
        assertTrue(m.winner!!.contains("wicket"))
        assertEquals(AppScreen.Summary, vm.currentScreen.value)
    }

    @Test
    fun `super over defended and won by runs`() {
        tieTheMatch()
        vm.startSuperOver(); idle()
        ball(4); ball(6)                           // SO innings 1 (Australia) = 10, target = 11
        vm.transitionToSuperOverSecondInnings(); idle()
        ball(1); ball(1); ball(1); ball(0); ball(0); ball(0) // SO innings 2 (India) = 3 in 6 balls
        val m = vm.activeMatch.value!!
        assertEquals("COMPLETED", m.status)
        assertTrue(m.winner!!.contains("Super Over"))
        assertTrue(m.winner!!.contains("Australia"))
        assertTrue(m.winner!!.contains("run"))
    }

    @Test
    fun `a level super over is declared a tie`() {
        tieTheMatch()
        vm.startSuperOver(); idle()
        ball(4); ball(1)                           // SO innings 1 (Australia) = 5, target = 6
        vm.transitionToSuperOverSecondInnings(); idle()
        ball(1); ball(1); ball(1); ball(1); ball(1); ball(0) // SO innings 2 (India) = 5 -> level
        val m = vm.activeMatch.value!!
        assertEquals("COMPLETED", m.status)
        assertTrue(m.winner!!.contains("Super Over"))
        assertTrue(m.winner!!.lowercase().contains("tie"))
    }
}
