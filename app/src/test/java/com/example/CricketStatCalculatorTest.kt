package com.example

import com.example.model.CricketStatCalculator
import com.example.model.Delivery
import com.example.model.ExtraType
import com.example.model.WicketType
import org.junit.Assert.assertEquals
import org.junit.Test

class CricketStatCalculatorTest {

    @Test
    fun `calculates runs extras balls wickets and bowler figures accurately`() {
        val deliveries = listOf(
            delivery("1", runsBat = 4),
            delivery("2", runsExtra = 1, extraType = ExtraType.WIDE),
            delivery("3", runsBat = 1, runsExtra = 1, extraType = ExtraType.NO_BALL),
            delivery("4", runsExtra = 2, extraType = ExtraType.BYE),
            delivery("5", wicket = true, wicketType = WicketType.BOWLED)
        )

        val summary = CricketStatCalculator.calculateInningsSummary(
            innings = 1,
            battingTeamName = "Home",
            bowlingTeamName = "Away",
            allDeliveries = deliveries,
            battingPlayers = listOf("A", "B"),
            bowlingPlayers = listOf("C")
        )

        assertEquals(9, summary.totalRuns)
        assertEquals(1, summary.totalWickets)
        assertEquals(3, summary.legalBalls)
        assertEquals("0.3", summary.overs)
        assertEquals(1, summary.extras[ExtraType.WIDE])
        assertEquals(1, summary.extras[ExtraType.NO_BALL])
        assertEquals(2, summary.extras[ExtraType.BYE])
        assertEquals(5, summary.batsmanStats.first { it.name == "A" }.runs)
        assertEquals(4, summary.batsmanStats.first { it.name == "A" }.balls)
        assertEquals(7, summary.bowlerStats.single().runsConceded)
        assertEquals(3, summary.bowlerStats.single().balls)
        assertEquals(1, summary.bowlerStats.single().wickets)
        assertEquals(listOf("4", "Wd", "Nb+1", "B2", "W"), summary.recentBalls)
    }

    @Test
    fun `does not credit a run out to the bowler`() {
        val summary = CricketStatCalculator.calculateInningsSummary(
            innings = 1,
            battingTeamName = "Home",
            bowlingTeamName = "Away",
            allDeliveries = listOf(delivery("1", wicket = true, wicketType = WicketType.RUN_OUT)),
            battingPlayers = listOf("A", "B"),
            bowlingPlayers = listOf("C")
        )

        assertEquals(1, summary.totalWickets)
        assertEquals(0, summary.bowlerStats.single().wickets)
    }

    private fun delivery(
        id: String,
        runsBat: Int = 0,
        runsExtra: Int = 0,
        extraType: ExtraType = ExtraType.NONE,
        wicket: Boolean = false,
        wicketType: WicketType? = null
    ) = Delivery(
        id = id,
        innings = 1,
        overIndex = 0,
        ballNumber = id.toInt(),
        striker = "A",
        nonStriker = "B",
        bowler = "C",
        runsBat = runsBat,
        runsExtra = runsExtra,
        extraType = extraType,
        wicket = wicket,
        wicketType = wicketType,
        dismissedPlayer = if (wicket) "A" else null,
        timestamp = id.toLong()
    )
}
