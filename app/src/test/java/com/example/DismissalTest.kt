package com.example

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.model.ExtraType
import com.example.model.MatchFormat
import com.example.model.WicketType
import com.example.viewmodel.MatchViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.Executor

/**
 * Focused coverage of the run-out (striker vs non-striker selection) and
 * retired-hurt flows in [MatchViewModel].
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DismissalTest {

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

    /** Openers are A (striker) and B (non-striker). */
    private fun startMatch(roster: List<String> = listOf("A", "B", "C", "D", "E")) {
        vm.setupTeamA.value = "India"
        vm.setupTeamB.value = "Australia"
        vm.setupFormat.value = MatchFormat.CUSTOM
        vm.setupOvers.value = 20
        vm.setupTossWonByHost.value = true
        vm.setupOptedToBat.value = true
        vm.setupTeamAPlayers.value = roster.joinToString(",")
        vm.setupTeamBPlayers.value = "X,Y,Z"
        vm.startNewMatch()
        idle()
    }

    private fun runsBall(runs: Int) {
        vm.addLiveDelivery(runs, 0, ExtraType.NONE, false, null, null)
        idle()
    }

    private fun wicket(type: WicketType, dismissed: String) {
        vm.addLiveDelivery(0, 0, ExtraType.NONE, true, type, dismissed)
        idle()
    }

    private fun summary() = vm.getActiveInningsSummary()

    // ---------- Run out ----------

    @Test
    fun `run out of the striker vacates the striker end`() {
        startMatch()
        val m0 = vm.activeMatch.value!!
        assertEquals("A", m0.strikerName)
        wicket(WicketType.RUN_OUT, dismissed = "A")
        val m = vm.activeMatch.value!!
        assertEquals("Batsman Out", m.strikerName)   // striker is gone
        assertEquals("B", m.nonStrikerName)           // non-striker stays
        assertEquals(1, summary().totalWickets)
    }

    @Test
    fun `run out of the non-striker vacates the non-striker end and keeps the striker`() {
        startMatch()
        wicket(WicketType.RUN_OUT, dismissed = "B")   // non-striker run out
        val m = vm.activeMatch.value!!
        assertEquals("A", m.strikerName)              // striker unaffected
        assertEquals("Batsman Out", m.nonStrikerName) // non-striker is gone
        assertEquals(1, summary().totalWickets)
        // the fall-of-wicket records the correct player
        assertEquals("B", summary().fallOfWickets.single().playerOut)
    }

    @Test
    fun `a run out is never credited to the bowler`() {
        startMatch()
        wicket(WicketType.RUN_OUT, dismissed = "A")
        val bowler = summary().bowlerStats.firstOrNull { it.wickets > 0 }
        assertEquals("no bowler should be credited for a run out", null, bowler)
    }

    // ---------- Retired hurt ----------

    @Test
    fun `retired hurt of the striker is not a wicket and vacates the striker`() {
        startMatch()
        wicket(WicketType.RETIRED_HURT, dismissed = "A")
        val m = vm.activeMatch.value!!
        assertEquals("Retired Hurt", m.strikerName)
        assertEquals("B", m.nonStrikerName)
        assertEquals("retired hurt must not count as a wicket", 0, summary().totalWickets)
        assertTrue("retired hurt must not create a fall of wicket", summary().fallOfWickets.isEmpty())
    }

    @Test
    fun `retired hurt of the non-striker is not a wicket and vacates the non-striker`() {
        startMatch()
        wicket(WicketType.RETIRED_HURT, dismissed = "B")
        val m = vm.activeMatch.value!!
        assertEquals("A", m.strikerName)
        assertEquals("Retired Hurt", m.nonStrikerName)
        assertEquals(0, summary().totalWickets)
    }

    @Test
    fun `a retired hurt batsman keeps the runs already scored`() {
        startMatch()
        runsBall(4)                       // A scores a boundary (even runs, keeps strike)
        runsBall(2)                       // A now 6 off 2
        wicket(WicketType.RETIRED_HURT, dismissed = "A")
        val a = summary().batsmanStats.first { it.name == "A" }
        assertEquals(6, a.runs)
        assertEquals("retiring must not add a ball faced", 2, a.balls)
        assertTrue(a.dismissed)
        assertEquals(WicketType.RETIRED_HURT, a.wicketType)
    }

    @Test
    fun `retired hurt does not consume a legal ball`() {
        startMatch()
        runsBall(1)                       // 1 legal ball bowled
        val ballsBefore = summary().legalBalls
        wicket(WicketType.RETIRED_HURT, dismissed = vm.activeMatch.value!!.strikerName)
        assertEquals("retiring hurt is not a delivery", ballsBefore, summary().legalBalls)
    }
}
