package com.example

import com.example.model.CricketStatCalculator
import com.example.model.Delivery
import com.example.model.ExtraType
import com.example.model.WicketType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Exhaustive validation of the scoring engine that powers every screen in the app.
 * Each test hand-computes the expected cricket figures and asserts the calculator agrees.
 */
class CricketStatCalculatorComprehensiveTest {

    private var seq = 0L

    private fun delivery(
        runsBat: Int = 0,
        runsExtra: Int = 0,
        extraType: ExtraType = ExtraType.NONE,
        wicket: Boolean = false,
        wicketType: WicketType? = null,
        striker: String = "A",
        nonStriker: String = "B",
        bowler: String = "C",
        dismissedPlayer: String? = null,
        fielder: String? = null,
        innings: Int = 1,
        overIndex: Int = 0
    ): Delivery {
        val id = (seq++).toString()
        return Delivery(
            id = id,
            innings = innings,
            overIndex = overIndex,
            ballNumber = 1,
            striker = striker,
            nonStriker = nonStriker,
            bowler = bowler,
            runsBat = runsBat,
            runsExtra = runsExtra,
            extraType = extraType,
            wicket = wicket,
            wicketType = wicketType,
            dismissedPlayer = if (wicket) (dismissedPlayer ?: striker) else null,
            timestamp = seq,
            fielder = fielder
        )
    }

    private fun summarize(
        deliveries: List<Delivery>,
        batting: List<String> = listOf("A", "B"),
        bowling: List<String> = listOf("C")
    ) = CricketStatCalculator.calculateInningsSummary(
        innings = 1,
        battingTeamName = "Home",
        bowlingTeamName = "Away",
        allDeliveries = deliveries,
        battingPlayers = batting,
        bowlingPlayers = bowling
    )

    @Test
    fun `empty innings yields all zeros`() {
        val s = summarize(emptyList())
        assertEquals(0, s.totalRuns)
        assertEquals(0, s.totalWickets)
        assertEquals(0, s.legalBalls)
        assertEquals("0.0", s.overs)
        assertEquals(0.0, s.runRate, 0.0001)
        assertTrue(s.recentBalls.isEmpty())
        assertTrue(s.fallOfWickets.isEmpty())
    }

    @Test
    fun `byes and leg byes count to team but not batsman or bowler`() {
        val s = summarize(
            listOf(
                delivery(runsExtra = 2, extraType = ExtraType.BYE),
                delivery(runsExtra = 1, extraType = ExtraType.LEG_BYE)
            )
        )
        assertEquals(3, s.totalRuns)
        assertEquals(2, s.extras[ExtraType.BYE])
        assertEquals(1, s.extras[ExtraType.LEG_BYE])
        assertEquals(2, s.legalBalls)
        val a = s.batsmanStats.first { it.name == "A" }
        assertEquals(0, a.runs)
        assertEquals(2, a.balls) // byes/leg-byes still consume a ball faced
        val c = s.bowlerStats.single()
        assertEquals(0, c.runsConceded) // byes are not the bowler's fault
        assertEquals(2, c.balls)
    }

    @Test
    fun `wide is not a ball faced and is charged to the bowler`() {
        val s = summarize(listOf(delivery(runsExtra = 1, extraType = ExtraType.WIDE)))
        assertEquals(1, s.totalRuns)
        assertEquals(1, s.extras[ExtraType.WIDE])
        assertEquals(0, s.legalBalls)
        assertEquals("0.0", s.overs)
        val a = s.batsmanStats.first { it.name == "A" }
        assertEquals(0, a.balls) // wide never counts as a ball faced
        val c = s.bowlerStats.single()
        assertEquals(1, c.runsConceded)
        assertEquals(0, c.balls)
    }

    @Test
    fun `no ball charges the penalty and bat runs to the bowler and batsman`() {
        val s = summarize(listOf(delivery(runsBat = 4, runsExtra = 1, extraType = ExtraType.NO_BALL)))
        assertEquals(5, s.totalRuns)
        assertEquals(1, s.extras[ExtraType.NO_BALL])
        assertEquals(0, s.legalBalls) // a no-ball is not a legal delivery
        val a = s.batsmanStats.first { it.name == "A" }
        assertEquals(4, a.runs)
        assertEquals(1, a.fours)
        assertEquals(1, a.balls)
        val c = s.bowlerStats.single()
        assertEquals(5, c.runsConceded) // 4 off the bat + 1 no-ball penalty
        assertEquals(0, c.balls)
    }

    @Test
    fun `six legal dot balls is a maiden over`() {
        val balls = (0 until 6).map { delivery() }
        val s = summarize(balls)
        assertEquals(0, s.totalRuns)
        assertEquals(6, s.legalBalls)
        assertEquals("1.0", s.overs)
        assertEquals(1, s.bowlerStats.single().maidens)
    }

    @Test
    fun `an over with only byes is still a maiden for the bowler`() {
        // 5 dots + 1 bye: no runs off the bat, so the bowler keeps a maiden
        val balls = (0 until 5).map { delivery() } + delivery(runsExtra = 2, extraType = ExtraType.BYE)
        val s = summarize(balls)
        assertEquals(2, s.totalRuns)
        assertEquals(6, s.legalBalls)
        assertEquals(0, s.bowlerStats.single().runsConceded)
        assertEquals(1, s.bowlerStats.single().maidens)
    }

    @Test
    fun `an over with a single off the bat is not a maiden`() {
        val balls = (0 until 5).map { delivery() } + delivery(runsBat = 1)
        val s = summarize(balls)
        assertEquals(0, s.bowlerStats.single().maidens)
    }

    @Test
    fun `bowled stumped caught and lbw are credited to the bowler`() {
        for (wt in listOf(WicketType.BOWLED, WicketType.STUMPED, WicketType.CAUGHT, WicketType.LBW, WicketType.HIT_WICKET)) {
            seq = 0
            val s = summarize(listOf(delivery(wicket = true, wicketType = wt)))
            assertEquals("wickets for $wt", 1, s.totalWickets)
            assertEquals("bowler credit for $wt", 1, s.bowlerStats.single().wickets)
        }
    }

    @Test
    fun `run out is a team wicket but not credited to the bowler`() {
        val s = summarize(listOf(delivery(wicket = true, wicketType = WicketType.RUN_OUT)))
        assertEquals(1, s.totalWickets)
        assertEquals(0, s.bowlerStats.single().wickets)
    }

    @Test
    fun `retired hurt is neither a team wicket nor a fall of wicket`() {
        val s = summarize(listOf(delivery(wicket = true, wicketType = WicketType.RETIRED_HURT)))
        assertEquals(0, s.totalWickets)
        assertTrue(s.fallOfWickets.isEmpty())
        assertEquals(0, s.bowlerStats.single().wickets)
        // the retired player is still flagged so the UI can move them off strike
        assertTrue(s.batsmanStats.first { it.name == "A" }.dismissed)
    }

    @Test
    fun `fall of wickets records number player score and overs`() {
        val s = summarize(
            listOf(
                delivery(runsBat = 4),                                   // score 4, 1 legal ball
                delivery(wicket = true, wicketType = WicketType.BOWLED)  // A out, score 4, 2 legal balls
            )
        )
        assertEquals(1, s.fallOfWickets.size)
        val fow = s.fallOfWickets.single()
        assertEquals(1, fow.wicketNumber)
        assertEquals("A", fow.playerOut)
        assertEquals(4, fow.teamScoreAtWicket)
        assertEquals("0.2", fow.oversAtWicket)
    }

    @Test
    fun `partnership is flushed on a wicket with runs and balls`() {
        val s = summarize(
            listOf(
                delivery(runsBat = 2),
                delivery(wicket = true, wicketType = WicketType.BOWLED)
            )
        )
        assertEquals(1, s.partnerships.size)
        val p = s.partnerships.single()
        assertEquals("A", p.batsman1)
        assertEquals("B", p.batsman2)
        assertEquals(2, p.runs)
        assertEquals(2, p.balls)
    }

    @Test
    fun `overs string handles partial overs`() {
        val s = summarize((0 until 13).map { delivery() })
        assertEquals(13, s.legalBalls)
        assertEquals("2.1", s.overs)
    }

    @Test
    fun `run rate and economy are computed over legal balls only`() {
        // 12 legal balls (2 overs), 12 runs off the bat
        val balls = (0 until 12).map { delivery(runsBat = 1) }
        val s = summarize(balls)
        assertEquals(12, s.totalRuns)
        assertEquals(6.0, s.runRate, 0.0001)
        assertEquals(6.0, s.bowlerStats.single().economy, 0.0001)
    }

    @Test
    fun `boundaries are counted as fours and sixes`() {
        val s = summarize(listOf(delivery(runsBat = 4), delivery(runsBat = 6), delivery(runsBat = 4)))
        val a = s.batsmanStats.first { it.name == "A" }
        assertEquals(14, a.runs)
        assertEquals(2, a.fours)
        assertEquals(1, a.sixes)
        assertEquals(3, a.balls)
        assertEquals(466.67, a.strikeRate, 0.01) // 14 runs off 3 balls

    }

    @Test
    fun `recent balls render each delivery type correctly`() {
        val s = summarize(
            listOf(
                delivery(runsBat = 1),
                delivery(runsExtra = 3, extraType = ExtraType.WIDE),   // Wd+2
                delivery(runsBat = 2, runsExtra = 1, extraType = ExtraType.NO_BALL), // Nb+2
                delivery(runsExtra = 4, extraType = ExtraType.BYE),    // B4
                delivery(runsExtra = 1, extraType = ExtraType.LEG_BYE),// Lb1
                delivery(wicket = true, wicketType = WicketType.CAUGHT)
            )
        )
        assertEquals(listOf("1", "Wd+2", "Nb+2", "B4", "Lb1", "W"), s.recentBalls)
    }

    @Test
    fun `deliveries from other innings are ignored`() {
        val s = summarize(
            listOf(
                delivery(runsBat = 4, innings = 1),
                delivery(runsBat = 6, innings = 2)
            )
        )
        assertEquals(4, s.totalRuns)
        assertEquals(1, s.legalBalls)
    }

    @Test
    fun `all out scenario counts every dismissal`() {
        // three wickets in a row, striker changes each time
        val s = summarize(
            listOf(
                delivery(striker = "A", wicket = true, wicketType = WicketType.BOWLED, dismissedPlayer = "A"),
                delivery(striker = "B", wicket = true, wicketType = WicketType.LBW, dismissedPlayer = "B"),
                delivery(striker = "D", wicket = true, wicketType = WicketType.CAUGHT, dismissedPlayer = "D")
            ),
            batting = listOf("A", "B", "D")
        )
        assertEquals(3, s.totalWickets)
        assertEquals(3, s.fallOfWickets.size)
        assertEquals(3, s.bowlerStats.single().wickets)
        assertFalse(s.batsmanStats.first { it.name == "A" }.dismissed.not())
    }
}
