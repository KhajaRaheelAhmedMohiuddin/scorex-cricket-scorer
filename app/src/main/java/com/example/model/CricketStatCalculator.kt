package com.example.model

import java.util.Locale

object CricketStatCalculator {

    fun calculateInningsSummary(
        innings: Int,
        battingTeamName: String,
        bowlingTeamName: String,
        allDeliveries: List<Delivery>,
        battingPlayers: List<String>,
        bowlingPlayers: List<String>
    ): InningsSummary {
        val deliveries = allDeliveries.filter { it.innings == innings }
        
        // 1. Calculate Runs & Extras
        var runsBatTotal = 0
        var wides = 0
        var noBalls = 0
        var byes = 0
        var legByes = 0

        for (d in deliveries) {
            runsBatTotal += d.runsBat
            when (d.extraType) {
                ExtraType.WIDE -> wides += d.runsExtra
                ExtraType.NO_BALL -> noBalls += d.runsExtra
                ExtraType.BYE -> byes += d.runsExtra
                ExtraType.LEG_BYE -> legByes += d.runsExtra
                ExtraType.NONE -> { /* No extras */ }
            }
        }

        val totalExtras = wides + noBalls + byes + legByes
        val totalRuns = runsBatTotal + totalExtras
        val totalWickets = deliveries.count { it.wicket && it.wicketType != WicketType.RETIRED_HURT }

        // 2. Count Legal Balls & Overs
        var legalBalls = 0
        for (d in deliveries) {
            // A retired-hurt entry is recorded between deliveries, so it is not a ball.
            val isRetirement = d.wicket && d.wicketType == WicketType.RETIRED_HURT
            if (!isRetirement && d.extraType != ExtraType.WIDE && d.extraType != ExtraType.NO_BALL) {
                legalBalls++
            }
        }
        val completedOvers = legalBalls / 6
        val remainingBalls = legalBalls % 6
        val oversStr = "$completedOvers.$remainingBalls"

        // 3. Batting Stats Map (Initialize for all players)
        val batMap = battingPlayers.associateWith { name ->
            BatsmanStats(name = name)
        }.toMutableMap()

        // 4. Bowler Stats Map (Initialize for all players)
        val bowlMap = bowlingPlayers.associateWith { name ->
            BowlerStats(name = name)
        }.toMutableMap()

        // Track live runs/faced for bowler maidens calculation per overIndex
        // Over index -> List of deliveries
        val overDeliveries = deliveries.groupBy { it.overIndex }

        // Chronological Processing of Batting and Bowling
        // Sorting deliveries to ensure strict timeline
        val sortedDeliveries = deliveries.sortedBy { it.timestamp }

        // Active partnerships tracker
        val partnershipsList = mutableListOf<Partnership>()
        var currentPartnershipRuns = 0
        var currentPartnershipBalls = 0
        var pBat1 = ""
        var pBat2 = ""

        val fallOfWicketsList = mutableListOf<FallOfWicket>()
        var wicketCounter = 0
        
        var runningScore = 0
        var runningLegalBalls = 0

        for (d in sortedDeliveries) {
            // A retired-hurt entry is not a delivery: it consumes no ball for anyone.
            val isRetirement = d.wicket && d.wicketType == WicketType.RETIRED_HURT
            // Batsman charged ballfaced if not a wide (and not a retirement)
            val chargesBall = !isRetirement && d.extraType != ExtraType.WIDE
            val isLegal = !isRetirement && d.extraType != ExtraType.WIDE && d.extraType != ExtraType.NO_BALL

            if (isLegal) {
                runningLegalBalls++
            }

            // Accumulate running score
            val ballTotalRuns = d.runsBat + d.runsExtra
            runningScore += ballTotalRuns

            // 1) Update Batting Map
            val currentBat = batMap[d.striker] ?: BatsmanStats(d.striker)
            val updatedBat = currentBat.copy(
                runs = currentBat.runs + d.runsBat,
                balls = currentBat.balls + (if (chargesBall) 1 else 0),
                fours = currentBat.fours + (if (d.runsBat == 4) 1 else 0),
                sixes = currentBat.sixes + (if (d.runsBat == 6) 1 else 0)
            )
            batMap[d.striker] = updatedBat

            // 2) Update Bowler Map
            val currentBowl = bowlMap[d.bowler] ?: BowlerStats(d.bowler)
            // Bowler concedes bat runs + wides + no balls (byes & legbyes are not bowler's fault)
            val conceded = d.runsBat + (if (d.extraType == ExtraType.WIDE || d.extraType == ExtraType.NO_BALL) d.runsExtra else 0)
            
            // Bowler credited wicket only if clean bowler dismissal (not run-out)
            val isBowlerWicket = d.wicket && d.wicketType != WicketType.RUN_OUT && d.wicketType != WicketType.OTHER && d.wicketType != WicketType.RETIRED_HURT
            val updatedBowl = currentBowl.copy(
                balls = currentBowl.balls + (if (isLegal) 1 else 0),
                runsConceded = currentBowl.runsConceded + conceded,
                wickets = currentBowl.wickets + (if (isBowlerWicket) 1 else 0)
            )
            bowlMap[d.bowler] = updatedBowl

            // 3) Partnerships Tracker
            if (pBat1.isEmpty() || pBat2.isEmpty()) {
                pBat1 = d.striker
                pBat2 = d.nonStriker
            }
            // Add runs to partnership
            currentPartnershipRuns += ballTotalRuns
            if (chargesBall) currentPartnershipBalls++

            // 4) Handling Wicket Out
            if (d.wicket) {
                val isActualWicket = d.wicketType != WicketType.RETIRED_HURT
                if (isActualWicket) {
                    wicketCounter++
                }
                val outPlayer = d.dismissedPlayer ?: d.striker
                
                // Finalize out batsman stats
                val outBat = batMap[outPlayer] ?: BatsmanStats(outPlayer)
                batMap[outPlayer] = outBat.copy(
                    dismissed = true,
                    wicketType = d.wicketType ?: WicketType.BOWLED,
                    bowlerWhoDismissed = if (isActualWicket) d.bowler else null,
                    fielderWhoDismissed = if (isActualWicket) d.fielder else null
                )

                if (isActualWicket) {
                    // Record Fall of Wicket
                    val fOvers = "${runningLegalBalls / 6}.${runningLegalBalls % 6}"
                    fallOfWicketsList.add(
                        FallOfWicket(
                            wicketNumber = wicketCounter,
                            playerOut = outPlayer,
                            teamScoreAtWicket = runningScore,
                            oversAtWicket = fOvers
                        )
                    )
                }

                // Finalize active partnership and flush
                partnershipsList.add(Partnership(pBat1, pBat2, currentPartnershipRuns, currentPartnershipBalls))
                currentPartnershipRuns = 0
                currentPartnershipBalls = 0
                pBat1 = ""
                pBat2 = ""
            }
        }

        // Add remaining live partnership if any balls played
        if (sortedDeliveries.isNotEmpty() && pBat1.isNotEmpty() && pBat2.isNotEmpty()) {
            partnershipsList.add(Partnership(pBat1, pBat2, currentPartnershipRuns, currentPartnershipBalls))
        }

        // Calculate Bowler Maidens
        for ((overIndex, ballsInOver) in overDeliveries) {
            // For a complete maiden over, bowler must bowl 6 legal deliveries with 0 runs conceded
            val bowlerForOver = ballsInOver.firstOrNull()?.bowler
            if (bowlerForOver != null) {
                val legalBallsInOver = ballsInOver.count { !(it.wicket && it.wicketType == WicketType.RETIRED_HURT) && it.extraType != ExtraType.WIDE && it.extraType != ExtraType.NO_BALL }
                if (legalBallsInOver >= 6) {
                    val runsInOver = ballsInOver.sumOf { 
                        it.runsBat + (if (it.extraType == ExtraType.WIDE || it.extraType == ExtraType.NO_BALL) it.runsExtra else 0)
                    }
                    if (runsInOver == 0) {
                        val currB = bowlMap[bowlerForOver]
                        if (currB != null) {
                            bowlMap[bowlerForOver] = currB.copy(maidens = currB.maidens + 1)
                        }
                    }
                }
            }
        }

        // Format Recent Balls summary (a retirement is not a ball, so it is not shown)
        val recentBalls = sortedDeliveries
            .filter { !(it.wicket && it.wicketType == WicketType.RETIRED_HURT) }
            .takeLast(10).map { d ->
            when {
                d.wicket -> "W"
                d.extraType == ExtraType.WIDE -> "Wd" + (if (d.runsExtra > 1) "+${d.runsExtra - 1}" else "")
                d.extraType == ExtraType.NO_BALL -> "Nb" + (if (d.runsBat > 0) "+${d.runsBat}" else "")
                d.extraType == ExtraType.BYE -> "B${d.runsExtra}"
                d.extraType == ExtraType.LEG_BYE -> "Lb${d.runsExtra}"
                else -> d.runsBat.toString()
            }
        }

        val runRate = if (legalBalls > 0) (totalRuns.toDouble() / (legalBalls.toDouble() / 6.0)) else 0.0

        val extrasMap = mapOf(
            ExtraType.WIDE to wides,
            ExtraType.NO_BALL to noBalls,
            ExtraType.BYE to byes,
            ExtraType.LEG_BYE to legByes
        )

        return InningsSummary(
            totalRuns = totalRuns,
            totalWickets = totalWickets,
            legalBalls = legalBalls,
            overs = oversStr,
            extras = extrasMap,
            runRate = runRate,
            batsmanStats = batMap.values.toList(),
            bowlerStats = bowlMap.values.toList(),
            partnerships = partnershipsList,
            fallOfWickets = fallOfWicketsList,
            recentBalls = recentBalls
        )
    }
}
