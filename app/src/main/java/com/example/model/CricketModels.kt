package com.example.model

import com.squareup.moshi.JsonClass

enum class ExtraType {
    NONE, WIDE, NO_BALL, BYE, LEG_BYE
}

enum class WicketType {
    BOWLED, CAUGHT, LBW, RUN_OUT, STUMPED, HIT_WICKET, OTHER, RETIRED_HURT
}

enum class MatchFormat {
    T20, ODI, TEST, CUSTOM
}

enum class MatchStatus {
    SETUP, LIVE, COMPLETED
}

@JsonClass(generateAdapter = true)
data class Delivery(
    val id: String,
    val innings: Int,       // 1 or 2
    val overIndex: Int,     // 0-indexed over number (e.g., 0, 1, 2...)
    val ballNumber: Int,    // 1-indexed ball within the over (only increments on legal balls)
    val striker: String,
    val nonStriker: String,
    val bowler: String,
    val runsBat: Int,       // runs scored off the bat
    val runsExtra: Int,     // extras (wide runs, no-ball runs, byes, legbyes)
    val extraType: ExtraType,
    val wicket: Boolean,
    val wicketType: WicketType?,
    val dismissedPlayer: String?,
    val timestamp: Long = System.currentTimeMillis(),
    val fielder: String? = null
)

// Statistics calculated reactively from deliveries
data class BatsmanStats(
    val name: String,
    val runs: Int = 0,
    val balls: Int = 0,
    val fours: Int = 0,
    val sixes: Int = 0,
    val dismissed: Boolean = false,
    val wicketType: WicketType? = null,
    val bowlerWhoDismissed: String? = null,
    val fielderWhoDismissed: String? = null
) {
    val strikeRate: Double
        get() = if (balls > 0) (runs.toDouble() / balls) * 100.0 else 0.0
}

data class BowlerStats(
    val name: String,
    val balls: Int = 0,
    val runsConceded: Int = 0,
    val wickets: Int = 0,
    val maidens: Int = 0
) {
    val overs: String
        get() {
            val completedOvers = balls / 6
            val remainingBalls = balls % 6
            return "$completedOvers.$remainingBalls"
        }
    
    val economy: Double
        get() {
            val oversFloat = balls.toDouble() / 6.0
            return if (oversFloat > 0) runsConceded.toDouble() / oversFloat else 0.0
        }
}

data class Partnership(
    val batsman1: String,
    val batsman2: String,
    val runs: Int = 0,
    val balls: Int = 0
)

data class FallOfWicket(
    val wicketNumber: Int,
    val playerOut: String,
    val teamScoreAtWicket: Int,
    val oversAtWicket: String
)

data class InningsSummary(
    val totalRuns: Int,
    val totalWickets: Int,
    val legalBalls: Int,
    val overs: String,
    val extras: Map<ExtraType, Int>,
    val runRate: Double,
    val batsmanStats: List<BatsmanStats>,
    val bowlerStats: List<BowlerStats>,
    val partnerships: List<Partnership>,
    val fallOfWickets: List<FallOfWicket>,
    val recentBalls: List<String> // Last 6-12 balls scored in this innings
)

// Dynamic team management model
data class Team(
    val id: String,
    val name: String,
    val matchesPlayed: Int = 0,
    val matchesWon: Int = 0,
    val matchesLost: Int = 0,
    val roster: List<String> = emptyList()
)

