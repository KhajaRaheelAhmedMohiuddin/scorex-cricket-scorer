package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import kotlinx.coroutines.launch
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.MatchEntity
import com.example.model.Delivery
import com.example.model.ExtraType
import com.example.model.WicketType
import com.example.ui.components.GlassmorphicCard
import com.example.ui.theme.*
import java.util.Locale

// Data carrier for cumulative player statistics
data class PlayerCumulativeStats(
    val playerName: String,
    // Batting
    val batMatches: Int,
    val batInnings: Int,
    val batRuns: Int,
    val batNotOuts: Int,
    val batBestScore: Int,
    val batStrikeRate: Double,
    val batAverage: Double,
    val batFours: Int,
    val batSixes: Int,
    val batThirties: Int,
    val batFifties: Int,
    val batHundreds: Int,
    // Bowling
    val bowlMatches: Int,
    val bowlInnings: Int,
    val bowlOvers: String,
    val bowlRunsConceded: Int,
    val bowlWickets: Int,
    val bowlMaidens: Int,
    val bowlBestBowling: String,
    val bowlEconomy: Double,
    val bowlAverage: Double,
    val bowlStrikeRate: Double,
    val bowlThreeWickets: Int,
    val bowlFiveWickets: Int,
    // Fielding
    val fieldMatches: Int,
    val fieldCatches: Int,
    val fieldRunOuts: Int,
    val fieldStumpings: Int,
    val fieldTotalDismissals: Int
)

object StatCalculator {
    fun calculatePlayerCumulativeStats(playerName: String, matches: List<MatchEntity>): PlayerCumulativeStats {
        var batMatches = 0
        var batInnings = 0
        var batRuns = 0
        var batDismissals = 0
        var batBestScore = 0
        var batBalls = 0
        var batFours = 0
        var batSixes = 0
        var batThirties = 0
        var batFifties = 0
        var batHundreds = 0

        var bowlMatches = 0
        var bowlInnings = 0
        var bowlBalls = 0
        var bowlRunsConceded = 0
        var bowlWickets = 0
        var bowlMaidens = 0
        var bestWickets = -1
        var bestRunsConceded = 9999
        var bowlThreeWickets = 0
        var bowlFiveWickets = 0

        var fieldMatches = 0
        var fieldCatches = 0
        var fieldRunOuts = 0
        var fieldStumpings = 0

        val targetLower = playerName.trim().lowercase()

        for (match in matches) {
            val isSquadA = match.teamAPlayers.any { it.trim().lowercase() == targetLower }
            val isSquadB = match.teamBPlayers.any { it.trim().lowercase() == targetLower }
            val inSquad = isSquadA || isSquadB

            if (!inSquad) continue

            // Counts as a match played/squad
            batMatches++
            bowlMatches++
            fieldMatches++

            // Analyze deliveries for this match
            val matchDeliveries = match.deliveries

            // --- BATTING in this match ---
            val myBatBalls = matchDeliveries.filter { it.striker.trim().lowercase() == targetLower }
            val matchesBatInnings = matchDeliveries.any { it.striker.trim().lowercase() == targetLower || it.nonStriker.trim().lowercase() == targetLower }

            if (matchesBatInnings) {
                batInnings++
                val runsInThisMatch = myBatBalls.sumOf { it.runsBat }
                batRuns += runsInThisMatch
                batBalls += myBatBalls.count { it.extraType != ExtraType.WIDE }
                batFours += myBatBalls.count { it.runsBat == 4 }
                batSixes += myBatBalls.count { it.runsBat == 6 }

                if (runsInThisMatch in 30..49) {
                    batThirties++
                } else if (runsInThisMatch in 50..99) {
                    batFifties++
                } else if (runsInThisMatch >= 100) {
                    batHundreds++
                }

                if (runsInThisMatch > batBestScore) {
                    batBestScore = runsInThisMatch
                }

                // Did I get dismissed in this match?
                val wasDismissed = matchDeliveries.any { d ->
                    if (d.wicket) {
                        val outPlayer = d.dismissedPlayer ?: d.striker
                        val isMe = outPlayer.trim().lowercase() == targetLower
                        val isActualWicket = d.wicketType != WicketType.RETIRED_HURT
                        isMe && isActualWicket
                    } else false
                }
                if (wasDismissed) {
                    batDismissals++
                }
            }

            // --- BOWLING in this match ---
            val myBowlDeliveries = matchDeliveries.filter { it.bowler.trim().lowercase() == targetLower }
            val bowledAtLeastOneBall = myBowlDeliveries.isNotEmpty()
            if (bowledAtLeastOneBall) {
                bowlInnings++
                val legalBallsBowled = myBowlDeliveries.count { it.extraType != ExtraType.WIDE && it.extraType != ExtraType.NO_BALL }
                bowlBalls += legalBallsBowled

                val conceded = myBowlDeliveries.sumOf { 
                    it.runsBat + (if (it.extraType == ExtraType.WIDE || it.extraType == ExtraType.NO_BALL) it.runsExtra else 0)
                }
                bowlRunsConceded += conceded

                val isBowlerWicket = { d: Delivery ->
                    d.wicket && d.wicketType != WicketType.RUN_OUT && d.wicketType != WicketType.OTHER && d.wicketType != WicketType.RETIRED_HURT
                }
                val wicketsInMatch = myBowlDeliveries.count { isBowlerWicket(it) }
                bowlWickets += wicketsInMatch

                if (wicketsInMatch >= 5) {
                    bowlFiveWickets++
                } else if (wicketsInMatch >= 3) {
                    bowlThreeWickets++
                }

                // Track maiden overs bowled in this match
                val groupedByOver = myBowlDeliveries.groupBy { it.overIndex }
                for ((_, ballsInOver) in groupedByOver) {
                    val legalBallsInOver = ballsInOver.count { it.extraType != ExtraType.WIDE && it.extraType != ExtraType.NO_BALL }
                    if (legalBallsInOver >= 6) {
                        val runsInOver = ballsInOver.sumOf {
                            it.runsBat + (if (it.extraType == ExtraType.WIDE || it.extraType == ExtraType.NO_BALL) it.runsExtra else 0)
                        }
                        if (runsInOver == 0) {
                            bowlMaidens++
                        }
                    }
                }

                // Best Bowling Figure comparison
                if (wicketsInMatch > bestWickets || (wicketsInMatch == bestWickets && conceded < bestRunsConceded)) {
                    bestWickets = wicketsInMatch
                    bestRunsConceded = conceded
                }
            }

            // --- FIELDING in this match ---
            val catches = matchDeliveries.count { d ->
                d.wicket && d.wicketType == WicketType.CAUGHT && d.fielder?.trim()?.lowercase() == targetLower
            }
            val runOuts = matchDeliveries.count { d ->
                d.wicket && d.wicketType == WicketType.RUN_OUT && d.fielder?.trim()?.lowercase() == targetLower
            }
            val stumpings = matchDeliveries.count { d ->
                d.wicket && d.wicketType == WicketType.STUMPED && d.fielder?.trim()?.lowercase() == targetLower
            }

            fieldCatches += catches
            fieldRunOuts += runOuts
            fieldStumpings += stumpings
        }

        val batNotOuts = if (batInnings > batDismissals) batInnings - batDismissals else 0
        val batStrikeRate = if (batBalls > 0) (batRuns.toDouble() / batBalls) * 100.0 else 0.0
        val batAverage = if (batDismissals > 0) (batRuns.toDouble() / batDismissals) else if (batInnings > 0) batRuns.toDouble() else 0.0

        val bowlOvers = "${bowlBalls / 6}.${bowlBalls % 6}"
        val bowlOversFloat = bowlBalls.toDouble() / 6.0
        val bowlEconomy = if (bowlOversFloat > 0) bowlRunsConceded.toDouble() / bowlOversFloat else 0.0
        val bowlAverage = if (bowlWickets > 0) bowlRunsConceded.toDouble() / bowlWickets else 0.0
        val bowlStrikeRate = if (bowlWickets > 0) bowlBalls.toDouble() / bowlWickets else 0.0
        val bowlBestBowling = if (bestWickets >= 0) "$bestWickets/$bestRunsConceded" else "-"

        val fieldTotalDismissals = fieldCatches + fieldRunOuts + fieldStumpings

        return PlayerCumulativeStats(
            playerName = playerName,
            batMatches = batMatches,
            batInnings = batInnings,
            batRuns = batRuns,
            batNotOuts = batNotOuts,
            batBestScore = batBestScore,
            batStrikeRate = batStrikeRate,
            batAverage = batAverage,
            batFours = batFours,
            batSixes = batSixes,
            batThirties = batThirties,
            batFifties = batFifties,
            batHundreds = batHundreds,
            bowlMatches = bowlMatches,
            bowlInnings = bowlInnings,
            bowlOvers = bowlOvers,
            bowlRunsConceded = bowlRunsConceded,
            bowlWickets = bowlWickets,
            bowlMaidens = bowlMaidens,
            bowlBestBowling = bowlBestBowling,
            bowlEconomy = bowlEconomy,
            bowlAverage = bowlAverage,
            bowlStrikeRate = bowlStrikeRate,
            bowlThreeWickets = bowlThreeWickets,
            bowlFiveWickets = bowlFiveWickets,
            fieldMatches = fieldMatches,
            fieldCatches = fieldCatches,
            fieldRunOuts = fieldRunOuts,
            fieldStumpings = fieldStumpings,
            fieldTotalDismissals = fieldTotalDismissals
        )
    }
}

@Composable
fun PlayerStatsView(
    playerName: String,
    matches: List<MatchEntity>,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val stats = remember(playerName, matches) {
        StatCalculator.calculatePlayerCumulativeStats(playerName, matches)
    }

    val pagerState = rememberPagerState(initialPage = 0, pageCount = { 3 })
    val coroutineScope = rememberCoroutineScope()
    val selectedTab = pagerState.currentPage

    // Cache structural brushes to prevent allocating new Gradient brushes at 120fps during swiping
    val backgroundBrush = remember {
        Brush.verticalGradient(listOf(DarkBgSurface, DarkBgMain))
    }
    val avatarBrush = remember {
        Brush.radialGradient(listOf(InfoTeal.copy(alpha = 0.3f), OuterSpace))
    }

    // Pre-format and pre-chunk lists inside 'remember(stats)' to prevent heavy string.format and list re-allocation
    val battingChunked = remember(stats) {
        listOf(
            "Matches" to stats.batMatches.toString(),
            "Innings" to stats.batInnings.toString(),
            "Runs" to stats.batRuns.toString(),
            "Not Outs" to stats.batNotOuts.toString(),
            "Best Score" to stats.batBestScore.toString(),
            "Strike Rate" to String.format(Locale.US, "%.2f", stats.batStrikeRate),
            "Average" to String.format(Locale.US, "%.2f", stats.batAverage),
            "Fours" to stats.batFours.toString(),
            "Sixes" to stats.batSixes.toString(),
            "Thirties" to stats.batThirties.toString(),
            "Fifties" to stats.batFifties.toString(),
            "Hundreds" to stats.batHundreds.toString()
        ).chunked(3)
    }

    val bowlingChunked = remember(stats) {
        listOf(
            "Matches" to stats.bowlMatches.toString(),
            "Innings" to stats.bowlInnings.toString(),
            "Overs" to stats.bowlOvers,
            "Maidens" to stats.bowlMaidens.toString(),
            "Runs" to stats.bowlRunsConceded.toString(),
            "Wickets" to stats.bowlWickets.toString(),
            "Best Bowling" to stats.bowlBestBowling,
            "Economy" to String.format(Locale.US, "%.2f", stats.bowlEconomy),
            "Average" to String.format(Locale.US, "%.2f", stats.bowlAverage),
            "Strike Rate" to String.format(Locale.US, "%.2f", stats.bowlStrikeRate),
            "3W" to stats.bowlThreeWickets.toString(),
            "5W" to stats.bowlFiveWickets.toString()
        ).chunked(3)
    }

    val fieldingChunked = remember(stats) {
        listOf(
            "Matches" to stats.fieldMatches.toString(),
            "Catches" to stats.fieldCatches.toString(),
            "Run Outs" to stats.fieldRunOuts.toString(),
            "Stumpings" to stats.fieldStumpings.toString(),
            "Total Dismissals" to stats.fieldTotalDismissals.toString()
        ).chunked(3)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBgMain)
    ) {
        // --- 1. Sleek Modern Athletic Top Bar (Same style as MatchSummaryScreen) ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkBgSurface)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            // Surfaced Navigation Tile on the left
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .align(Alignment.CenterStart)
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkBgMain)
                    .border(1.dp, GlassBorder, RoundedCornerShape(12.dp))
                    .clickable { onBack() }
                    .testTag("player_stats_back_btn"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }

            // Centered Editorial Heads
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "PLAYER STATS",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp
                    ),
                    color = InfoTeal,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "DETAILED ANALYTICS",
                    style = MaterialTheme.typography.labelSmall,
                    color = MutedGrey,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
        HorizontalDivider(color = InfoTeal.copy(alpha = 0.15f), thickness = 1.dp)

        // Content below top bar (header is fixed, while each page inside the horizontal pager scrolls independently)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            // --- 2. Dynamic Backdrop Area with Profile placeholder and name ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .background(backgroundBrush),
                contentAlignment = Alignment.Center
            ) {
                // Silhouette Avatar Badge
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(InfoTeal)
                            .border(
                                BorderStroke(
                                    1.5.dp,
                                    Color.White.copy(alpha = 0.35f)
                                ),
                                RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (playerName.isNotEmpty()) playerName.substring(0, 1).uppercase() else "P",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 32.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = playerName,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                        color = CleanWhite,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // --- 3. Dynamic Tab Row (Modern, border-highlighted design) ---
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = DarkBgSurface,
                contentColor = InfoTeal,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = InfoTeal,
                        height = 3.dp
                    )
                }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(0)
                        }
                    },
                    text = {
                        Text(
                            "BATTING",
                            color = if (selectedTab == 0) InfoTeal else MutedGrey,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 12.sp,
                            letterSpacing = 1.sp
                        )
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(1)
                        }
                    },
                    text = {
                        Text(
                            "BOWLING",
                            color = if (selectedTab == 1) InfoTeal else MutedGrey,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 12.sp,
                            letterSpacing = 1.sp
                        )
                    }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(2)
                        }
                    },
                    text = {
                        Text(
                            "FIELDING",
                            color = if (selectedTab == 2) InfoTeal else MutedGrey,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 12.sp,
                            letterSpacing = 1.sp
                        )
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- 4. Render Active Stats Grid via HorizontalPager (Swipe-to-navigate) ---
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) { page ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    when (page) {
                        0 -> StatsGrid(chunked = battingChunked)
                        1 -> StatsGrid(chunked = bowlingChunked)
                        2 -> StatsGrid(chunked = fieldingChunked)
                    }
                    
                    // High bottom spacer so it scrolls perfectly behind the bottom navigation bar or screen bottom
                    Spacer(modifier = Modifier.height(110.dp))
                }
            }
        }
    }
}

@Composable
fun StatsGrid(chunked: List<List<Pair<String, String>>>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        chunked.forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                rowItems.forEach { (label, value) ->
                    Box(modifier = Modifier.weight(1f)) {
                        StatCard(label = label, value = value)
                    }
                }
                // Fill remaining spaces in row with empty boxes if chunk size < 3
                if (rowItems.size < 3) {
                    val missing = 3 - rowItems.size
                    for (i in 0 until missing) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
fun StatCard(label: String, value: String) {
    // Beautiful, high-contrast, light card style to match the user's reference image exactly
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(76.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = CleanWhite, // Exactly high-quality clean white background from the image
            contentColor = DarkBgMain
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(6.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                color = MutedGrey2, // Modern neutral grey for label
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                color = DarkBgMain, // Bold dark color for values
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center
            )
        }
    }
}
