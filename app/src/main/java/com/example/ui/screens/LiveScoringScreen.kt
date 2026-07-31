package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.MatchEntity
import com.example.model.*
import com.example.ui.components.GlassmorphicCard
import com.example.ui.components.GlowBorderGlassmorphicCard
import com.example.ui.theme.*
import com.example.viewmodel.AppScreen
import com.example.viewmodel.MatchViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LiveScoringScreen(
    viewModel: MatchViewModel,
    modifier: Modifier = Modifier
) {
    val match by viewModel.activeMatch.collectAsState()
    val currentScreen by viewModel.currentScreen.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current

    if (match == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No active match in scope", color = CoolSlate)
        }
        return
    }

    val activeMatch = match!!
    val firstInnings = activeMatch.currentInnings == 1
    val battingTeam = when (activeMatch.currentInnings) {
        1 -> activeMatch.firstInningsBattingTeam
        2 -> if (activeMatch.firstInningsBattingTeam == activeMatch.teamA) activeMatch.teamB else activeMatch.teamA
        3 -> if (activeMatch.firstInningsBattingTeam == activeMatch.teamA) activeMatch.teamB else activeMatch.teamA
        4 -> activeMatch.firstInningsBattingTeam
        else -> activeMatch.firstInningsBattingTeam
    }
    val bowlingTeam = if (battingTeam == activeMatch.teamA) activeMatch.teamB else activeMatch.teamA
    
    val batRoster = if (battingTeam == activeMatch.teamA) activeMatch.teamAPlayers else activeMatch.teamBPlayers
    val bowlRoster = if (bowlingTeam == activeMatch.teamA) activeMatch.teamAPlayers else activeMatch.teamBPlayers

    val summary = remember(activeMatch.currentInnings, activeMatch.deliveries) { viewModel.getActiveInningsSummary() }
    val innings1 = remember(activeMatch.deliveries) { viewModel.getInnings1Summary() }

    // Dialog state controllers
    var showWicketDialog by remember { mutableStateOf(false) }
    var showBowlerDialog by remember { mutableStateOf(false) }
    var showStrikerDialog by remember { mutableStateOf(false) }
    var showNonStrikerDialog by remember { mutableStateOf(false) }

    // Additional wicket features states
    var showNewBatsmanDialog by remember { mutableStateOf(false) }
    var showNewOverBowlerDialog by remember { mutableStateOf(false) }
    var isStrikerDismissedGlobal by remember { mutableStateOf(true) }

    // New checkers for interactive flow
    val currentInningsDeliveries = remember(activeMatch.id, activeMatch.currentInnings, activeMatch.deliveries) {
        activeMatch.deliveries.filter { it.innings == activeMatch.currentInnings }
    }
    val deliveriesByOver = remember(currentInningsDeliveries) {
        currentInningsDeliveries.groupBy { it.overIndex }.toSortedMap()
    }
    val overList = remember(deliveriesByOver) {
        deliveriesByOver.toList().sortedBy { it.first }
    }
    val lazyListState = rememberLazyListState()
    LaunchedEffect(currentInningsDeliveries.size) {
        if (overList.isNotEmpty()) {
            lazyListState.animateScrollToItem(overList.size - 1)
        }
    }
    val maxWickets = if (activeMatch.currentInnings >= 3) 2 else (batRoster.size - 1).coerceAtLeast(1)
    val maxOvers = if (activeMatch.currentInnings >= 3) 1 else activeMatch.selectedOvers
    
    val isSuperOver1Completed = activeMatch.currentInnings == 3 && (summary.legalBalls >= 6 || summary.totalWickets >= 2)
    val summarySO1 = remember(activeMatch.deliveries) { viewModel.getInnings3Summary() }
    val isSuperOver2Completed = activeMatch.currentInnings == 4 && (summary.legalBalls >= 6 || summary.totalWickets >= 2 || summary.totalRuns >= (summarySO1.totalRuns + 1))

    val isFirstInningsCompleted = (activeMatch.currentInnings == 1 && (summary.legalBalls >= maxOvers * 6 || summary.totalWickets >= maxWickets)) || isSuperOver1Completed
    val isSecondInningsCompleted = (activeMatch.currentInnings == 2 && (summary.legalBalls >= maxOvers * 6 || summary.totalWickets >= maxWickets || summary.totalRuns >= (innings1.totalRuns + 1))) || isSuperOver2Completed
    val isInningsOrMatchCompleted = isFirstInningsCompleted || isSecondInningsCompleted || activeMatch.status == "COMPLETED"

    LaunchedEffect(isInningsOrMatchCompleted) {
        if (isInningsOrMatchCompleted) {
            showNewBatsmanDialog = false
            showNewOverBowlerDialog = false
        }
    }

    var showOpenersSelectionCard by remember(activeMatch.id, activeMatch.currentInnings) {
        mutableStateOf(currentInningsDeliveries.isEmpty())
    }

    LaunchedEffect(summary.legalBalls) {
        val isSuperOver = activeMatch.currentInnings >= 3
        val inningsLimit = if (isSuperOver) 6 else maxOvers * 6
        if (summary.legalBalls > 0 && summary.legalBalls % 6 == 0 && summary.legalBalls < inningsLimit && !isInningsOrMatchCompleted) {
            showNewOverBowlerDialog = true
        }
    }

    LaunchedEffect(activeMatch.strikerName, activeMatch.nonStrikerName, activeMatch.deliveries) {
        val hasOutBatsman = activeMatch.strikerName == "Batsman Out" || activeMatch.nonStrikerName == "Batsman Out" || activeMatch.strikerName == "Retired Hurt" || activeMatch.nonStrikerName == "Retired Hurt"
        if (hasOutBatsman && !isInningsOrMatchCompleted) {
            val testSummary = viewModel.getActiveInningsSummary()
            val dismissedPlayers = testSummary.batsmanStats.filter { it.dismissed && it.wicketType != WicketType.RETIRED_HURT }.map { it.name }
            val currentBatsmen = listOf(activeMatch.strikerName, activeMatch.nonStrikerName)
            val yetToBatList = batRoster.filter { it !in dismissedPlayers && it !in currentBatsmen }
            if (yetToBatList.isNotEmpty()) {
                showNewBatsmanDialog = true
            }
        }
    }

    // Scoring local modifiers
    var selectedExtraType by remember { mutableStateOf(ExtraType.NONE) }

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp, top = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        // --- 1. Screen Header Controls ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            // Surfaced Navigation Tile on the left
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .align(Alignment.CenterStart)
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkBgSurface)
                    .border(1.dp, GlassBorder, RoundedCornerShape(12.dp))
                    .clickable {
                        viewModel.selectedDashboardTab.value = 0
                        viewModel.navigateTo(AppScreen.Dashboard)
                    }
                    .testTag("live_scoring_back_btn"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Exit Scorer",
                    tint = Color.White
                )
            }

            // Centered Team vs Team
            Row(
                modifier = Modifier.align(Alignment.Center),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = activeMatch.teamA.toAbbreviation().uppercase(),
                    color = CleanWhite,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = " VS ",
                    color = GoldAccent,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    ),
                    modifier = Modifier.padding(horizontal = 6.dp)
                )
                Text(
                    text = activeMatch.teamB.toAbbreviation().uppercase(),
                    color = CleanWhite,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Right Actions Side
            Row(
                modifier = Modifier.align(Alignment.CenterEnd),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Analytics Icon Button
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkBgSurface)
                        .border(1.dp, GlassBorder, RoundedCornerShape(12.dp))
                        .clickable { viewModel.navigateTo(AppScreen.Analysis) }
                        .testTag("nav_analytics_btn"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Analytics,
                        contentDescription = "Analytics",
                        tint = InfoTeal,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Scorecard Icon Button
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkBgSurface)
                        .border(1.dp, GlassBorder, RoundedCornerShape(12.dp))
                        .clickable { viewModel.navigateTo(AppScreen.Scorecard) }
                        .testTag("nav_scorecard_btn"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ListAlt,
                        contentDescription = "Full Scorecard",
                        tint = StadiumGreen,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // --- 2. Stadium Jumbotron (Big score block) ---
        GlowBorderGlassmorphicCard(
            modifier = Modifier.fillMaxWidth(),
            elevation = 16.dp,
            glowColors = if (activeMatch.currentInnings == 1 || activeMatch.currentInnings == 2) listOf(InfoTeal, StadiumGreen) else listOf(GoldAccent, WicketCrimson)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val infiniteTransition = rememberInfiniteTransition(label = "live_dot")
                val alpha by infiniteTransition.animateFloat(
                    initialValue = 0.2f,
                    targetValue = 1.0f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1000, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "live_dot_alpha"
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(bottom = 6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(WicketCrimson.copy(alpha = alpha))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "LIVE",
                        style = MaterialTheme.typography.labelLarge,
                        color = WicketCrimson,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.5.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "•",
                        color = MutedGrey,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = when (activeMatch.currentInnings) {
                            3 -> "${battingTeam.toAbbreviation().uppercase()} SUPER OVER"
                            4 -> "${battingTeam.toAbbreviation().uppercase()} SUPER OVER CHASE"
                            else -> "${battingTeam.toAbbreviation().uppercase()} INNINGS ${activeMatch.currentInnings}"
                        },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Black,
                        color = CoolSlate,
                        letterSpacing = 1.sp
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Massive Runs/Wickets
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "${summary.totalRuns}",
                        style = MaterialTheme.typography.displayLarge,
                        color = CleanWhite,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = " / ${summary.totalWickets}",
                        style = MaterialTheme.typography.displayLarge,
                        color = WicketCrimson,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Overs display
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "OVERS: ${summary.overs} ",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = CoolSlate,
                        fontFamily = Manrope
                    )
                    Text(
                        text = "of ${if (activeMatch.currentInnings >= 3) 1 else activeMatch.selectedOvers}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MutedGrey
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Run Rates (CRR / RRR)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "CRR: ${String.format(java.util.Locale.US, "%.2f", summary.runRate)}",
                        style = MaterialTheme.typography.labelLarge,
                        color = StadiumGreen,
                        fontWeight = FontWeight.Bold
                    )
                    val isSecondInningsOfSegment = activeMatch.currentInnings == 2 || activeMatch.currentInnings == 4
                    if (isSecondInningsOfSegment) {
                        Spacer(modifier = Modifier.width(16.dp))
                        val target = if (activeMatch.currentInnings == 4) summarySO1.totalRuns + 1 else innings1.totalRuns + 1
                        val runsNeeded = target - summary.totalRuns
                        val totalBalls = if (activeMatch.currentInnings >= 3) 6 else activeMatch.selectedOvers * 6
                        val legalBallsRemaining = totalBalls - summary.legalBalls
                        val rrr = if (legalBallsRemaining > 0) (runsNeeded.toDouble() / (legalBallsRemaining.toDouble() / 6.0)) else 0.0

                        Text(
                            text = "RRR: ${String.format(java.util.Locale.US, "%.2f", rrr)}",
                            style = MaterialTheme.typography.labelLarge,
                            color = GoldAccent,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // 2nd Innings Target Info Banner
                val isSecondInningsOfSegment = activeMatch.currentInnings == 2 || activeMatch.currentInnings == 4
                if (isSecondInningsOfSegment) {
                    val target = if (activeMatch.currentInnings == 4) summarySO1.totalRuns + 1 else innings1.totalRuns + 1
                    val runsNeeded = target - summary.totalRuns
                    val totalBalls = if (activeMatch.currentInnings >= 3) 6 else activeMatch.selectedOvers * 6
                    val legalBallsRemaining = totalBalls - summary.legalBalls
                    
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .background(Color(0x24FFFF00), RoundedCornerShape(8.dp))
                            .border(0.5.dp, GoldAccent.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (runsNeeded > 0) {
                                "NEED $runsNeeded RUNS IN $legalBallsRemaining BALLS TO WIN"
                            } else {
                                "TARGET CHASED SUCCESSFULLY!"
                            },
                            style = MaterialTheme.typography.titleSmall,
                            color = GoldAccent,
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- 3. Strikers & Bowler Live Cards (Editorial columns) ---
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // BATSMEN PANEL
            GlassmorphicCard(
                modifier = Modifier.fillMaxWidth(),
                borderColor = GlassBorder
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "BATTER",
                            style = MaterialTheme.typography.labelLarge,
                            color = MutedGrey,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Row(
                            modifier = Modifier.width(170.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("R", style = MaterialTheme.typography.labelSmall, color = MutedGrey, fontSize = 12.sp, modifier = Modifier.width(30.dp), textAlign = TextAlign.End)
                            Text("B", style = MaterialTheme.typography.labelSmall, color = MutedGrey, fontSize = 12.sp, modifier = Modifier.width(24.dp), textAlign = TextAlign.End)
                            Text("4S", style = MaterialTheme.typography.labelSmall, color = MutedGrey, fontSize = 12.sp, modifier = Modifier.width(28.dp), textAlign = TextAlign.End)
                            Text("6S", style = MaterialTheme.typography.labelSmall, color = MutedGrey, fontSize = 12.sp, modifier = Modifier.width(28.dp), textAlign = TextAlign.End)
                            Text("SR", style = MaterialTheme.typography.labelSmall, color = MutedGrey, fontSize = 12.sp, modifier = Modifier.width(36.dp), textAlign = TextAlign.End)
                        }
                    }
                    Divider(color = GlassBorder, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 6.dp))

                    val lastInningsDelivery = activeMatch.deliveries.lastOrNull { it.innings == activeMatch.currentInnings }
                    val isAllOut = summary.totalWickets >= maxWickets
                    val isWicketOnLastInningsBall = !isAllOut && lastInningsDelivery?.wicket == true && (summary.legalBalls >= maxOvers * 6)

                    // Determine what to display for Striker
                    val showStrikerRow = when {
                        isAllOut -> activeMatch.strikerName != "Batsman Out"
                        else -> true
                    }

                    val strikerDisplayName = when {
                        isWicketOnLastInningsBall && activeMatch.strikerName == "Batsman Out" -> {
                            lastInningsDelivery?.dismissedPlayer ?: "Batsman Out"
                        }
                        else -> activeMatch.strikerName
                    }

                    // Determine what to display for Non-Striker
                    val showNonStrikerRow = when {
                        isAllOut -> activeMatch.nonStrikerName != "Batsman Out"
                        else -> true
                    }

                    val nonStrikerDisplayName = when {
                        isWicketOnLastInningsBall && activeMatch.nonStrikerName == "Batsman Out" -> {
                            lastInningsDelivery?.dismissedPlayer ?: "Batsman Out"
                        }
                        else -> activeMatch.nonStrikerName
                    }

                    // Striker Row
                    val strStats = summary.batsmanStats.firstOrNull { it.name == strikerDisplayName }
                    if (showStrikerRow) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showStrikerDialog = true }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = strikerDisplayName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = StadiumGreen,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Row(
                                modifier = Modifier.width(170.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("${strStats?.runs ?: 0}", style = MaterialTheme.typography.bodySmall, color = CleanWhite, fontWeight = FontWeight.Bold, modifier = Modifier.width(30.dp), textAlign = TextAlign.End)
                                Text("${strStats?.balls ?: 0}", style = MaterialTheme.typography.bodySmall, color = CleanWhite, fontWeight = FontWeight.Bold, modifier = Modifier.width(24.dp), textAlign = TextAlign.End)
                                Text("${strStats?.fours ?: 0}", style = MaterialTheme.typography.bodySmall, color = CleanWhite, fontWeight = FontWeight.Bold, modifier = Modifier.width(28.dp), textAlign = TextAlign.End)
                                Text("${strStats?.sixes ?: 0}", style = MaterialTheme.typography.bodySmall, color = CleanWhite, fontWeight = FontWeight.Bold, modifier = Modifier.width(28.dp), textAlign = TextAlign.End)
                                val sr = if ((strStats?.balls ?: 0) > 0) {
                                    val rate = (strStats!!.runs.toFloat() / strStats.balls.toFloat()) * 100
                                    String.format(java.util.Locale.US, "%.1f", rate)
                                } else "0.0"
                                Text(sr, style = MaterialTheme.typography.bodySmall, color = CoolSlate, fontWeight = FontWeight.Bold, modifier = Modifier.width(36.dp), textAlign = TextAlign.End)
                            }
                        }
                    }

                    if (showStrikerRow && showNonStrikerRow) {
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    // Non-Striker Row
                    val nonStrStats = summary.batsmanStats.firstOrNull { it.name == nonStrikerDisplayName }
                    if (showNonStrikerRow) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showNonStrikerDialog = true }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = nonStrikerDisplayName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = CoolSlate,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Row(
                                modifier = Modifier.width(170.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("${nonStrStats?.runs ?: 0}", style = MaterialTheme.typography.bodySmall, color = CleanWhite, fontWeight = FontWeight.Bold, modifier = Modifier.width(30.dp), textAlign = TextAlign.End)
                                Text("${nonStrStats?.balls ?: 0}", style = MaterialTheme.typography.bodySmall, color = CleanWhite, fontWeight = FontWeight.Bold, modifier = Modifier.width(24.dp), textAlign = TextAlign.End)
                                Text("${nonStrStats?.fours ?: 0}", style = MaterialTheme.typography.bodySmall, color = CleanWhite, fontWeight = FontWeight.Bold, modifier = Modifier.width(28.dp), textAlign = TextAlign.End)
                                Text("${nonStrStats?.sixes ?: 0}", style = MaterialTheme.typography.bodySmall, color = CleanWhite, fontWeight = FontWeight.Bold, modifier = Modifier.width(28.dp), textAlign = TextAlign.End)
                                val sr = if ((nonStrStats?.balls ?: 0) > 0) {
                                    val rate = (nonStrStats!!.runs.toFloat() / nonStrStats.balls.toFloat()) * 100
                                    String.format(java.util.Locale.US, "%.1f", rate)
                                } else "0.0"
                                Text(sr, style = MaterialTheme.typography.bodySmall, color = CoolSlate, fontWeight = FontWeight.Bold, modifier = Modifier.width(36.dp), textAlign = TextAlign.End)
                            }
                        }
                    }
                }
            }

            // BOWLER PANEL
            GlassmorphicCard(
                modifier = Modifier.fillMaxWidth(),
                borderColor = GlassBorder
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "BOWLER",
                            style = MaterialTheme.typography.labelLarge,
                            color = MutedGrey,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Row(
                            modifier = Modifier.width(170.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("O", style = MaterialTheme.typography.labelSmall, color = MutedGrey, fontSize = 12.sp, modifier = Modifier.width(30.dp), textAlign = TextAlign.End)
                            Text("M", style = MaterialTheme.typography.labelSmall, color = MutedGrey, fontSize = 12.sp, modifier = Modifier.width(24.dp), textAlign = TextAlign.End)
                            Text("R", style = MaterialTheme.typography.labelSmall, color = MutedGrey, fontSize = 12.sp, modifier = Modifier.width(28.dp), textAlign = TextAlign.End)
                            Text("W", style = MaterialTheme.typography.labelSmall, color = MutedGrey, fontSize = 12.sp, modifier = Modifier.width(28.dp), textAlign = TextAlign.End)
                            Text("ECO", style = MaterialTheme.typography.labelSmall, color = MutedGrey, fontSize = 12.sp, modifier = Modifier.width(36.dp), textAlign = TextAlign.End)
                        }
                    }
                    Divider(color = GlassBorder, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 6.dp))

                    val bowlStats = summary.bowlerStats.firstOrNull { it.name == activeMatch.bowlerName }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showBowlerDialog = true }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = activeMatch.bowlerName,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = InfoTeal,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Row(
                            modifier = Modifier.width(170.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(bowlStats?.overs ?: "0.0", style = MaterialTheme.typography.bodySmall, color = CleanWhite, fontWeight = FontWeight.Bold, modifier = Modifier.width(30.dp), textAlign = TextAlign.End)
                            Text("${bowlStats?.maidens ?: 0}", style = MaterialTheme.typography.bodySmall, color = CleanWhite, fontWeight = FontWeight.Bold, modifier = Modifier.width(24.dp), textAlign = TextAlign.End)
                            Text("${bowlStats?.runsConceded ?: 0}", style = MaterialTheme.typography.bodySmall, color = CleanWhite, fontWeight = FontWeight.Bold, modifier = Modifier.width(28.dp), textAlign = TextAlign.End)
                            Text("${bowlStats?.wickets ?: 0}", style = MaterialTheme.typography.bodySmall, color = WicketCrimson, fontWeight = FontWeight.Bold, modifier = Modifier.width(28.dp), textAlign = TextAlign.End)
                            val eco = if (bowlStats != null) {
                                String.format(java.util.Locale.US, "%.1f", bowlStats.economy)
                            } else "0.0"
                            Text(eco, style = MaterialTheme.typography.bodySmall, color = InfoTeal, fontWeight = FontWeight.Bold, modifier = Modifier.width(36.dp), textAlign = TextAlign.End)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // --- 5. Stadium Action Control Deck ---
        val maxOvers = activeMatch.selectedOvers
        val targetRun = innings1.totalRuns + 1
        val isInningsFinished = isInningsOrMatchCompleted

        if (isInningsFinished) {
            GlassmorphicCard(
                modifier = Modifier.fillMaxWidth().testTag("innings_finished_notice"),
                borderColor = when (activeMatch.currentInnings) {
                    1 -> InfoTeal
                    2 -> GoldAccent
                    3 -> WicketCrimson
                    4 -> StadiumGreen
                    else -> InfoTeal
                },
                backgroundColor = DarkBgGlass
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "STADIUM SCORING CONSOLE PANEL",
                        style = MaterialTheme.typography.labelLarge,
                        color = MutedGrey,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = when (activeMatch.currentInnings) {
                            1 -> "🔥 INNINGS 1 COMPLETE 🔥"
                            2 -> if (summary.totalRuns == innings1.totalRuns) "🤝 MATCH TIED 🤝" else "🏆 MATCH FINISHED 🏆"
                            3 -> "🔥 SUPER OVER 1st INNINGS COMPLETE 🔥"
                            4 -> "🏆 SUPER OVER MATCH FINISHED 🏆"
                            else -> "🔥 INNINGS COMPLETE 🔥"
                        },
                        color = when (activeMatch.currentInnings) {
                            1 -> InfoTeal
                            2 -> GoldAccent
                            3 -> WicketCrimson
                            4 -> StadiumGreen
                            else -> InfoTeal
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center
                    )
                    
                    val noticeText = when (activeMatch.currentInnings) {
                        1 -> "Target set: $targetRun runs in $maxOvers overs.\n\nTap \"DECLARE INNINGS | START CHASE\" below to start the second innings."
                        2 -> {
                            if (summary.totalRuns == innings1.totalRuns) {
                                "Match tied and scores level.\n\nTap \"START SUPER OVER (TIE-BREAKER)\" below to continue or tap \"DECLARE MATCH AS TIED\"."
                            } else {
                                "The second innings and match have finished.\n\nTap \"FINALIZE & CLOSE COMPLETED MATCH\" below to see results."
                            }
                        }
                        3 -> "Target set: ${summarySO1.totalRuns + 1} runs in 1 over.\n\nTap \"START CHASE (SUPER OVER 2ND INNINGS)\" below to start the Super Over chase."
                        4 -> "The Super Over has finished.\n\nTap \"FINALIZE SUPER OVER MATCH\" below to finalize and view results."
                        else -> "The innings has been completed."
                    }
                    
                    Text(
                        text = noticeText,
                        color = CleanWhite,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = OuterSpace,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, WicketCrimson),
                            modifier = Modifier
                                .clickable { viewModel.undoLastBall() }
                                .testTag("notice_undo_btn")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Undo, contentDescription = "Undo Last Delivery", tint = CleanWhite, modifier = Modifier.size(16.dp))
                                Text("UNDO LAST BALL", color = CleanWhite, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        } else {
            GlassmorphicCard(
                modifier = Modifier.fillMaxWidth(),
                borderColor = StadiumGreen.copy(alpha = 0.3f),
                backgroundColor = DarkBgGlass
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "STADIUM SCORING CONSOLE PANEL",
                        style = MaterialTheme.typography.labelLarge,
                        color = MutedGrey,
                        fontWeight = FontWeight.Bold
                    )

                    // EXTRAS EXTRA OPTION SELECTOR ROW
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "EXTRAS:",
                            style = MaterialTheme.typography.labelLarge,
                            color = MutedGrey,
                            fontSize = 11.sp
                        )

                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(ExtraType.NONE, ExtraType.WIDE, ExtraType.NO_BALL, ExtraType.BYE, ExtraType.LEG_BYE).forEach { type ->
                                val sel = selectedExtraType == type
                                val color = when (type) {
                                    ExtraType.NONE -> CoolSlate
                                    ExtraType.WIDE, ExtraType.NO_BALL -> GoldAccent
                                    ExtraType.BYE, ExtraType.LEG_BYE -> InfoTeal
                                }
                                
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (sel) color.copy(alpha = 0.25f) else Color.Transparent,
                                    border = if (sel) BorderStroke(1.dp, color) else BorderStroke(0.5.dp, GlassBorder),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { 
                                            selectedExtraType = type
                                        }
                                        .testTag("extra_opt_${type.name}")
                                ) {
                                    Text(
                                        text = when (type) {
                                            ExtraType.NONE -> "NONE"
                                            ExtraType.WIDE -> "WD"
                                            ExtraType.NO_BALL -> "NB"
                                            ExtraType.BYE -> "BYE"
                                            ExtraType.LEG_BYE -> "L-BYE"
                                        },
                                        color = if (sel) color else MutedGrey,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 9.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Clip,
                                        softWrap = false,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 8.dp)
                                    )
                                }
                            }
                        }
                    }

                    // CORE RUN ENTRY ACTION ROW (Buttons 0, 1, 2, 3, 4, 6)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(0, 1, 2, 3, 4, 6).forEach { scoreVal ->
                            val isBoundary = scoreVal == 4 || scoreVal == 6
                            val btnColor = if (isBoundary) StadiumGreen else OuterSpace
                            val txtColor = if (isBoundary) DarkBgMain else CleanWhite

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(btnColor)
                                    .clickable {
                                        val hasOutBatsman = activeMatch.strikerName == "Batsman Out" || activeMatch.nonStrikerName == "Batsman Out" || activeMatch.strikerName == "Retired Hurt" || activeMatch.nonStrikerName == "Retired Hurt"
                                        if (hasOutBatsman) {
                                            android.widget.Toast.makeText(context, "Please select the incoming batsman first!", android.widget.Toast.LENGTH_SHORT).show()
                                            return@clickable
                                        }
                                        // Submit ball delivery!
                                        val (finalRunsBat, finalRunsExtra) = when (selectedExtraType) {
                                            ExtraType.NONE -> Pair(scoreVal, 0)
                                            ExtraType.WIDE -> Pair(0, 1 + scoreVal)
                                            ExtraType.NO_BALL -> Pair(scoreVal, 1)
                                            ExtraType.BYE -> Pair(0, scoreVal)
                                            ExtraType.LEG_BYE -> Pair(0, scoreVal)
                                        }
                                        viewModel.addLiveDelivery(
                                            runsBat = finalRunsBat,
                                            runsExtra = finalRunsExtra,
                                            extraType = selectedExtraType,
                                            wicket = false
                                        )
                                        // Reset extra selections post delivery
                                        selectedExtraType = ExtraType.NONE
                                    }
                                    .testTag("score_btn_${scoreVal}"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = when (scoreVal) {
                                        0 -> "DOT"
                                        else -> scoreVal.toString()
                                    },
                                    style = MaterialTheme.typography.titleMedium,
                                    color = txtColor,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 14.sp,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }

                    // WICKET & QUICK DECK CONTROLS (Wicket Trigger, Swap Striker, Undo, Redo)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Wicket out button
                        Button(
                            onClick = {
                                val hasOutBatsman = activeMatch.strikerName == "Batsman Out" || activeMatch.nonStrikerName == "Batsman Out" || activeMatch.strikerName == "Retired Hurt" || activeMatch.nonStrikerName == "Retired Hurt"
                                if (hasOutBatsman) {
                                    android.widget.Toast.makeText(context, "Please select the incoming batsman first!", android.widget.Toast.LENGTH_SHORT).show()
                                } else {
                                    showWicketDialog = true
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = WicketCrimson),
                            contentPadding = PaddingValues(horizontal = 4.dp),
                            modifier = Modifier
                                .weight(1.2f)
                                .height(44.dp)
                                .testTag("score_wicket_btn"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = "WICKET",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Black,
                                color = CleanWhite,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Clip
                            )
                        }

                        // Strike swap button
                        Button(
                            onClick = { viewModel.swapStrikers() },
                            enabled = activeMatch.strikerName != "Batsman Out" && activeMatch.nonStrikerName != "Batsman Out" && activeMatch.strikerName != "Retired Hurt" && activeMatch.nonStrikerName != "Retired Hurt",
                            colors = ButtonDefaults.buttonColors(
                                containerColor = OuterSpace,
                                disabledContainerColor = OuterSpace.copy(alpha = 0.5f)
                            ),
                            contentPadding = PaddingValues(horizontal = 4.dp),
                            modifier = Modifier
                                .weight(0.9f)
                                .height(44.dp)
                                .testTag("swap_striker_btn"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = "SWAP",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = CoolSlate,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Clip
                            )
                        }

                        // Undo button
                        IconButton(
                            onClick = { viewModel.undoLastBall() },
                            modifier = Modifier
                                .size(44.dp)
                                .background(OuterSpace, RoundedCornerShape(10.dp))
                                .testTag("undo_btn")
                        ) {
                            Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Undo ball", tint = CoolSlate)
                        }

                        // Redo button
                        IconButton(
                            onClick = { viewModel.redoLastBall() },
                            modifier = Modifier
                                .size(44.dp)
                                .background(OuterSpace, RoundedCornerShape(10.dp))
                                .testTag("redo_btn")
                        ) {
                            Icon(imageVector = Icons.Default.ArrowForward, contentDescription = "Redo ball", tint = CoolSlate)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- 4. Innings Recent Balls Timeline Card ---
        GlassmorphicCard(
            modifier = Modifier.fillMaxWidth().testTag("innings_timeline_card"),
            borderColor = GlassBorder
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "INNINGS RECENT BALLS TIMELINE",
                    style = MaterialTheme.typography.labelLarge,
                    color = MutedGrey,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                // Ball timeline chips grouped over-by-over
                if (overList.isEmpty()) {
                    Text(
                        text = "Waiting for first delivery record...",
                        style = MaterialTheme.typography.bodySmall,
                        color = DarkTextMuted
                    )
                } else {
                    LazyRow(
                        state = lazyListState,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().testTag("innings_over_by_over_timeline")
                    ) {
                        itemsIndexed(overList) { index, (overIndex, balls) ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Over X label
                                Text(
                                    text = "Over ${overIndex + 1}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Black,
                                    color = CoolSlate
                                )

                                // Balls in this over
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    balls.forEach { d ->
                                        val itemStr = when {
                                            d.wicket -> "W"
                                            d.extraType == ExtraType.WIDE -> "WD" + (if (d.runsExtra > 1) "+${d.runsExtra - 1}" else "")
                                            d.extraType == ExtraType.NO_BALL -> "NB" + (if (d.runsBat > 0) "+${d.runsBat}" else "")
                                            d.extraType == ExtraType.BYE -> "B${d.runsExtra}"
                                            d.extraType == ExtraType.LEG_BYE -> "LB${d.runsExtra}"
                                            else -> d.runsBat.toString()
                                        }

                                        val isWicket = d.wicket
                                        val isBoundary = d.runsBat == 4 || d.runsBat == 6
                                        val isExtra = d.extraType != ExtraType.NONE

                                        val chipBg = when {
                                            isWicket -> WicketCrimson
                                            isBoundary -> StadiumGreen
                                            isExtra -> GoldAccent
                                            else -> Color.Transparent
                                        }
                                        val chipTc = when {
                                            isWicket -> CleanWhite
                                            isBoundary -> DarkBgMain
                                            isExtra -> DarkBgMain
                                            else -> CleanWhite
                                        }
                                        val chipBorderColor = when {
                                            isWicket || isBoundary || isExtra -> Color.Transparent
                                            else -> GlassBorder
                                        }

                                        Box(
                                            modifier = Modifier
                                                .size(34.dp)
                                                .clip(CircleShape)
                                                .background(chipBg)
                                                .border(1.dp, chipBorderColor, CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = itemStr,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Black,
                                                color = chipTc
                                            )
                                        }
                                    }
                                }

                                // Total runs scored in this over
                                val overTotalRuns = balls.sumOf { d -> d.runsBat + d.runsExtra }
                                Text(
                                    text = "= $overTotalRuns",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Black,
                                    color = CleanWhite
                                )

                                // Vertical separator bar (except for the last over element)
                                if (index < overList.lastIndex) {
                                    Text(
                                        text = "|",
                                        color = GlassBorder.copy(alpha = 0.5f),
                                        fontWeight = FontWeight.Light,
                                        modifier = Modifier.padding(horizontal = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // --- 6. Innings breaks / force complete matches ---
        if (activeMatch.currentInnings == 1) {
            Button(
                onClick = { viewModel.transitionToSecondInnings() },
                colors = ButtonDefaults.buttonColors(containerColor = GoldAccent),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("innings_break_btn"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "DECLARE INNINGS | START CHASE",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = DarkBgMain,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    softWrap = false
                )
            }
        } else if (activeMatch.currentInnings == 2) {
            val isTie = isSecondInningsCompleted && (summary.totalRuns == innings1.totalRuns)
            if (isTie) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { viewModel.startSuperOver() },
                        colors = ButtonDefaults.buttonColors(containerColor = GoldAccent),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("start_super_over_btn"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "START SUPER OVER (TIE-BREAKER)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Black,
                            color = DarkBgMain,
                            fontSize = 12.sp
                        )
                    }
                    Button(
                        onClick = { viewModel.forceDeclareMatchWinner("Match Tied!") },
                        colors = ButtonDefaults.buttonColors(containerColor = StadiumGreen),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("finish_as_tie_btn"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "DECLARE MATCH AS TIED",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Black,
                            color = DarkBgMain,
                            fontSize = 12.sp
                        )
                    }
                }
            } else {
                Button(
                    onClick = {
                        val winMsg = if (summary.totalRuns >= (innings1.totalRuns + 1)) {
                            "$battingTeam won by ${maxWickets - summary.totalWickets} wickets"
                        } else {
                            val runsDiff = innings1.totalRuns - summary.totalRuns
                            "$bowlingTeam won by $runsDiff runs"
                        }
                        viewModel.forceDeclareMatchWinner(winMsg)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StadiumGreen),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("force_finish_btn"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "FINALIZE & CLOSE COMPLETED MATCH",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Black,
                        color = DarkBgMain,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        softWrap = false
                    )
                }
            }
        } else if (activeMatch.currentInnings == 3) {
            if (isSuperOver1Completed) {
                Button(
                    onClick = { viewModel.transitionToSuperOverSecondInnings() },
                    colors = ButtonDefaults.buttonColors(containerColor = GoldAccent),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("super_over_innings_break_btn"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "START CHASE (SUPER OVER 2ND INNINGS)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Black,
                        color = DarkBgMain,
                        fontSize = 12.sp
                    )
                }
            }
        } else if (activeMatch.currentInnings == 4) {
            if (isSuperOver2Completed) {
                Button(
                    onClick = {
                        val winMsg = if (summary.totalRuns >= (summarySO1.totalRuns + 1)) {
                            "$battingTeam won in Super Over!"
                        } else if (summary.totalRuns < summarySO1.totalRuns) {
                            "$bowlingTeam won in Super Over!"
                        } else {
                            "Tie in Super Over!"
                        }
                        viewModel.forceDeclareMatchWinner(winMsg)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StadiumGreen),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("super_over_finalize_btn"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "FINALIZE SUPER OVER MATCH",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Black,
                        color = DarkBgMain,
                        fontSize = 12.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ==========================================
        //         POPUP DIALOG SELECTION CORES
        // ==========================================

        // 1. Wicket Logging Configuration Dialog with Cricket Rule Checks
        if (showWicketDialog) {
            var selectedWicketType by remember { mutableStateOf(WicketType.BOWLED) }
            var selectedPlayerOut by remember { mutableStateOf(activeMatch.strikerName) }
            val potentialFielders = bowlRoster.filter { it != activeMatch.bowlerName }
            var selectedFielder by remember { mutableStateOf(potentialFielders.getOrNull(0) ?: bowlRoster.getOrNull(0) ?: "") }
            var showFielderDropdown by remember { mutableStateOf(false) }

            // Apply strict cricket rules for striker vs non-striker eligibility
            LaunchedEffect(selectedWicketType) {
                if (selectedWicketType != WicketType.RUN_OUT) {
                    selectedPlayerOut = activeMatch.strikerName
                }
            }
            Dialog(
                onDismissRequest = { showWicketDialog = false },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.6f)),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth(0.95f)
                            .widthIn(max = 500.dp)
                            .padding(16.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = DarkBgSurface,
                        border = BorderStroke(1.dp, GlassBorder)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                "RECORD TEAM WICKET",
                                color = WicketCrimson,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black
                            )

                            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                // Dismissal type selector
                                Text("DISMISSAL NATURE TYPE:", color = CoolSlate, style = MaterialTheme.typography.labelLarge)
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    WicketType.values().filter { it != WicketType.OTHER }.forEach { wt ->
                                        val active = selectedWicketType == wt
                                        Box(
                                            modifier = Modifier
                                                .background(if (active) WicketCrimson else OuterSpace, RoundedCornerShape(8.dp))
                                                .clickable { selectedWicketType = wt }
                                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                                .testTag("wicket_nature_${wt.name}"),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(wt.name, color = if (active) CleanWhite else MutedGrey, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }

                                Divider(color = GlassBorder)

                                // Batsman dismissed selector
                                Text("WHICH BATSMAN WAS DISMISSED?", color = CoolSlate, style = MaterialTheme.typography.labelLarge)
                                if (selectedWicketType == WicketType.RUN_OUT || selectedWicketType == WicketType.RETIRED_HURT) {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { selectedPlayerOut = activeMatch.strikerName }
                                        ) {
                                            RadioButton(
                                                selected = selectedPlayerOut == activeMatch.strikerName,
                                                onClick = { selectedPlayerOut = activeMatch.strikerName },
                                                colors = RadioButtonDefaults.colors(selectedColor = WicketCrimson)
                                            )
                                            Text("${activeMatch.strikerName} (Striker)", color = CleanWhite, fontSize = 13.sp)
                                        }
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { selectedPlayerOut = activeMatch.nonStrikerName }
                                        ) {
                                            RadioButton(
                                                selected = selectedPlayerOut == activeMatch.nonStrikerName,
                                                onClick = { selectedPlayerOut = activeMatch.nonStrikerName },
                                                colors = RadioButtonDefaults.colors(selectedColor = WicketCrimson)
                                            )
                                            Text("${activeMatch.nonStrikerName} (Non-Striker)", color = CleanWhite, fontSize = 13.sp)
                                        }
                                    }
                                } else {
                                    Text("${activeMatch.strikerName} (Striker) - Only Striker can be out for $selectedWicketType", color = CleanWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }

                                // Assistant/Fielder selection for Caught, Run out, Stumping
                                if (selectedWicketType == WicketType.CAUGHT || selectedWicketType == WicketType.RUN_OUT || selectedWicketType == WicketType.STUMPED) {
                                    Divider(color = GlassBorder)
                                    val promptLabel = when(selectedWicketType) {
                                        WicketType.CAUGHT -> "CAUGHT BY (Select Fielder):"
                                        WicketType.RUN_OUT -> "RUN OUT BY (Select Fielder):"
                                        WicketType.STUMPED -> "STUMPED BY (Select Keeper/Fielder):"
                                        else -> "ASSISTED BY:"
                                    }
                                    Text(promptLabel, color = CoolSlate, style = MaterialTheme.typography.labelLarge)
                                    var wicketFielderBoxWidth by remember { mutableStateOf(0) }
                                    Box(modifier = Modifier
                                        .fillMaxWidth()
                                        .onSizeChanged { wicketFielderBoxWidth = it.width }
                                    ) {
                                        OutlinedTextField(
                                            value = selectedFielder,
                                            onValueChange = {},
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedTextColor = CleanWhite,
                                                unfocusedTextColor = CoolSlate,
                                                focusedBorderColor = InfoTeal,
                                                unfocusedBorderColor = GlassBorder,
                                                focusedContainerColor = OuterSpace
                                            ),
                                            trailingIcon = {
                                                IconButton(onClick = { showFielderDropdown = !showFielderDropdown }) {
                                                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Select Fielder", tint = InfoTeal)
                                                }
                                            },
                                            modifier = Modifier.fillMaxWidth().testTag("wicket_fielder_input"),
                                            singleLine = true,
                                            readOnly = true
                                        )
                                        DropdownMenu(
                                            expanded = showFielderDropdown,
                                            onDismissRequest = { showFielderDropdown = false },
                                            modifier = Modifier
                                                .width(with(LocalDensity.current) { wicketFielderBoxWidth.toDp() })
                                                .heightIn(max = 240.dp)
                                                .background(DarkBgSurface)
                                                .border(1.dp, GlassBorder, RoundedCornerShape(8.dp))
                                        ) {
                                            bowlRoster.forEach { fName ->
                                                DropdownMenuItem(
                                                    text = { Text(fName, color = CleanWhite) },
                                                    onClick = {
                                                        selectedFielder = fName
                                                        showFielderDropdown = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(onClick = { showWicketDialog = false }) {
                                    Text("CANCEL", color = CoolSlate)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                TextButton(
                                    onClick = {
                                        val runsExtra = when (selectedExtraType) {
                                            ExtraType.WIDE, ExtraType.NO_BALL -> 1
                                            else -> 0
                                        }
                                        val assistFielder = if (selectedWicketType == WicketType.CAUGHT || selectedWicketType == WicketType.RUN_OUT || selectedWicketType == WicketType.STUMPED) selectedFielder else null

                                        viewModel.addLiveDelivery(
                                            runsBat = 0,
                                            runsExtra = runsExtra,
                                            extraType = selectedExtraType,
                                            wicket = true,
                                            wicketType = selectedWicketType,
                                            dismissedPlayer = selectedPlayerOut, retiredPlayer = if (selectedWicketType == WicketType.RETIRED_HURT) selectedPlayerOut else null,
                                            fielder = assistFielder
                                        )

                                        isStrikerDismissedGlobal = (selectedPlayerOut == activeMatch.strikerName)

                                        selectedExtraType = ExtraType.NONE
                                        showWicketDialog = false

                                        // Only trigger incoming batsman dropdown dialog if not completed after this wicket
                                        val projectedWickets = summary.totalWickets + if (selectedWicketType == WicketType.RETIRED_HURT) 0 else 1
                                        val projectedSuperOver1Completed = activeMatch.currentInnings == 3 && (summary.legalBalls >= 6 || projectedWickets >= 2)
                                        val projectedSuperOver2Completed = activeMatch.currentInnings == 4 && (summary.legalBalls >= 6 || projectedWickets >= 2 || summary.totalRuns >= (summarySO1.totalRuns + 1))
                                        val projectedFirstInningsCompleted = (activeMatch.currentInnings == 1 && (summary.legalBalls >= maxOvers * 6 || projectedWickets >= maxWickets)) || projectedSuperOver1Completed
                                        val projectedSecondInningsCompleted = (activeMatch.currentInnings == 2 && (summary.legalBalls >= maxOvers * 6 || projectedWickets >= maxWickets || summary.totalRuns >= (innings1.totalRuns + 1))) || projectedSuperOver2Completed
                                        val projectedCompleted = projectedFirstInningsCompleted || projectedSecondInningsCompleted || activeMatch.status == "COMPLETED"

                                        if (!projectedCompleted) {
                                            showNewBatsmanDialog = true
                                        }
                                    },
                                    modifier = Modifier.testTag("confirm_wicket_log_btn")
                                ) {
                                    val btnLabel = if (selectedWicketType == WicketType.RETIRED_HURT) "RETIRED HURT" else "RECORD WICKET OUT"
                                    Text(btnLabel, color = WicketCrimson, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        // 1b. Incoming Batsman Selection Dropdown
        if (showNewBatsmanDialog && !isInningsOrMatchCompleted) {
            val testSummary = viewModel.getActiveInningsSummary()
            val dismissedPlayers = testSummary.batsmanStats.filter { it.dismissed && it.wicketType != WicketType.RETIRED_HURT }.map { it.name }
            val currentBatsmen = listOf(activeMatch.strikerName, activeMatch.nonStrikerName)
            val yetToBatList = batRoster.filter { it !in dismissedPlayers && it !in currentBatsmen }

            if (yetToBatList.isEmpty()) {
                // All batsmen out, innings will finish
                LaunchedEffect(Unit) {
                    showNewBatsmanDialog = false
                }
            } else {
                var selectedNewBatsman by remember(yetToBatList) { mutableStateOf(yetToBatList.getOrNull(0) ?: "") }
                var showBatsmanDropdown by remember { mutableStateOf(false) }

                Dialog(
                    onDismissRequest = { /* Must choose, don't dismiss */ },
                    properties = DialogProperties(usePlatformDefaultWidth = false)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.6f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth(0.95f)
                                .widthIn(max = 500.dp)
                                .padding(16.dp),
                            shape = RoundedCornerShape(16.dp),
                            color = DarkBgSurface,
                            border = BorderStroke(1.dp, GlassBorder)
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Text(
                                    text = "SELECT INCOMING BATSMAN",
                                    color = StadiumGreen,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Black
                                )

                                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                    Text(
                                        text = "A wicket has been recorded. Please select the next batsman to enter the crease from the squad roster:",
                                        color = CleanWhite,
                                        fontSize = 13.sp
                                    )
                                    var newBatsmanBoxWidth by remember { mutableStateOf(0) }
                                    Box(modifier = Modifier
                                        .fillMaxWidth()
                                        .onSizeChanged { newBatsmanBoxWidth = it.width }
                                    ) {
                                        OutlinedTextField(
                                            value = selectedNewBatsman,
                                            onValueChange = {},
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedTextColor = CleanWhite,
                                                unfocusedTextColor = CoolSlate,
                                                focusedBorderColor = StadiumGreen,
                                                unfocusedBorderColor = GlassBorder,
                                                focusedContainerColor = OuterSpace
                                            ),
                                            trailingIcon = {
                                                IconButton(onClick = { showBatsmanDropdown = !showBatsmanDropdown }) {
                                                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Select Batsman", tint = StadiumGreen)
                                                }
                                            },
                                            modifier = Modifier.fillMaxWidth().testTag("new_batsman_selection_input"),
                                            singleLine = true,
                                            readOnly = true
                                        )
                                        DropdownMenu(
                                            expanded = showBatsmanDropdown,
                                            onDismissRequest = { showBatsmanDropdown = false },
                                            modifier = Modifier
                                                .width(with(LocalDensity.current) { newBatsmanBoxWidth.toDp() })
                                                .heightIn(max = 240.dp)
                                                .background(DarkBgSurface)
                                                .border(1.dp, GlassBorder, RoundedCornerShape(8.dp))
                                        ) {
                                            yetToBatList.forEach { batName ->
                                                DropdownMenuItem(
                                                    text = { Text(batName, color = CleanWhite) },
                                                    onClick = {
                                                        selectedNewBatsman = batName
                                                        showBatsmanDropdown = false
                                                    },
                                                    modifier = Modifier.testTag("select_new_batsman_item_${batName.replace(" ", "_")}")
                                                )
                                            }
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Button(
                                        onClick = {
                                            if (selectedNewBatsman.isNotEmpty()) {
                                                val isStrikerPlaceholder = activeMatch.strikerName == "Batsman Out" || activeMatch.strikerName == "Retired Hurt"
                                                val isNonStrikerPlaceholder = activeMatch.nonStrikerName == "Batsman Out" || activeMatch.nonStrikerName == "Retired Hurt"
                                                if (isStrikerPlaceholder && !isNonStrikerPlaceholder) {
                                                    viewModel.changeActiveStriker(selectedNewBatsman)
                                                } else if (isNonStrikerPlaceholder && !isStrikerPlaceholder) {
                                                    viewModel.changeActiveNonStriker(selectedNewBatsman)
                                                } else {
                                                    if (isStrikerDismissedGlobal) {
                                                        viewModel.changeActiveStriker(selectedNewBatsman)
                                                    } else {
                                                        viewModel.changeActiveNonStriker(selectedNewBatsman)
                                                    }
                                                }
                                                showNewBatsmanDialog = false
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = StadiumGreen),
                                        modifier = Modifier.testTag("confirm_new_batsman_btn")
                                    ) {
                                        Text("CONFIRM SQUAD ENTRANT", color = DarkBgMain, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 1c. Initial Openers Selection Overlay Dialog
        if (showOpenersSelectionCard) {
            val potentialStrikers = batRoster
            var selectedStriker by remember { mutableStateOf(potentialStrikers.getOrNull(0) ?: "") }
            var selectedNonStriker by remember { mutableStateOf(potentialStrikers.getOrNull(1) ?: "") }
            var selectedBowler by remember { mutableStateOf(bowlRoster.getOrNull(10) ?: bowlRoster.getOrNull(0) ?: "") }

            var showStrikerDropdown by remember { mutableStateOf(false) }
            var showNonStrikerDropdown by remember { mutableStateOf(false) }
            var showBowlerDropdown by remember { mutableStateOf(false) }

            Dialog(
                onDismissRequest = { /* Must select */ },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.6f)),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth(0.95f)
                            .widthIn(max = 500.dp)
                            .padding(16.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = DarkBgSurface,
                        border = BorderStroke(1.dp, GlassBorder)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = "CHOOSE OPENING LINEUP",
                                color = StadiumGreen,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black
                            )

                            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                Text(
                                    text = "Please select the starting strikers and the opening bowler for this innings to begin scoring:",
                                    color = CleanWhite,
                                    fontSize = 13.sp
                                )

                                // 1. Striker Selection
                                Column {
                                    Text("STRIKER BATSMAN:", color = CoolSlate, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    var openersStrikerWidth by remember { mutableStateOf(0) }
                                    Box(modifier = Modifier
                                        .fillMaxWidth()
                                        .onSizeChanged { openersStrikerWidth = it.width }
                                    ) {
                                        OutlinedTextField(
                                            value = selectedStriker,
                                            onValueChange = {},
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedTextColor = CleanWhite,
                                                unfocusedTextColor = CoolSlate,
                                                focusedBorderColor = StadiumGreen,
                                                unfocusedBorderColor = GlassBorder,
                                                focusedContainerColor = OuterSpace
                                            ),
                                            trailingIcon = {
                                                IconButton(onClick = { showStrikerDropdown = !showStrikerDropdown }) {
                                                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Select Striker", tint = StadiumGreen)
                                                }
                                            },
                                            modifier = Modifier.fillMaxWidth().testTag("setup_striker_input"),
                                            singleLine = true,
                                            readOnly = true
                                        )
                                        DropdownMenu(
                                            expanded = showStrikerDropdown,
                                            onDismissRequest = { showStrikerDropdown = false },
                                            modifier = Modifier
                                                .width(with(LocalDensity.current) { openersStrikerWidth.toDp() })
                                                .heightIn(max = 240.dp)
                                                .background(DarkBgSurface)
                                                .border(1.dp, GlassBorder, RoundedCornerShape(8.dp))
                                        ) {
                                            potentialStrikers.forEach { batName ->
                                                DropdownMenuItem(
                                                    text = { Text(batName, color = CleanWhite) },
                                                    onClick = {
                                                        selectedStriker = batName
                                                        showStrikerDropdown = false
                                                        if (selectedNonStriker == batName) {
                                                            selectedNonStriker = potentialStrikers.firstOrNull { it != batName } ?: ""
                                                        }
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }

                                // 2. Non-Striker Selection
                                Column {
                                    Text("NON-STRIKER BATSMAN:", color = CoolSlate, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    var openersNonStrikerWidth by remember { mutableStateOf(0) }
                                    Box(modifier = Modifier
                                        .fillMaxWidth()
                                        .onSizeChanged { openersNonStrikerWidth = it.width }
                                    ) {
                                        OutlinedTextField(
                                            value = selectedNonStriker,
                                            onValueChange = {},
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedTextColor = CleanWhite,
                                                unfocusedTextColor = CoolSlate,
                                                focusedBorderColor = StadiumGreen,
                                                unfocusedBorderColor = GlassBorder,
                                                focusedContainerColor = OuterSpace
                                            ),
                                            trailingIcon = {
                                                IconButton(onClick = { showNonStrikerDropdown = !showNonStrikerDropdown }) {
                                                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Select Non-Striker", tint = StadiumGreen)
                                                }
                                            },
                                            modifier = Modifier.fillMaxWidth().testTag("setup_non_striker_input"),
                                            singleLine = true,
                                            readOnly = true
                                        )
                                        DropdownMenu(
                                            expanded = showNonStrikerDropdown,
                                            onDismissRequest = { showNonStrikerDropdown = false },
                                            modifier = Modifier
                                                .width(with(LocalDensity.current) { openersNonStrikerWidth.toDp() })
                                                .heightIn(max = 240.dp)
                                                .background(DarkBgSurface)
                                                .border(1.dp, GlassBorder, RoundedCornerShape(8.dp))
                                        ) {
                                            potentialStrikers.filter { it != selectedStriker }.forEach { batName ->
                                                DropdownMenuItem(
                                                    text = { Text(batName, color = CleanWhite) },
                                                    onClick = {
                                                        selectedNonStriker = batName
                                                        showNonStrikerDropdown = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }

                                // 3. Opening Bowler Selection
                                Column {
                                    Text("OPENING BOWLER:", color = CoolSlate, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    var openersBowlerWidth by remember { mutableStateOf(0) }
                                    Box(modifier = Modifier
                                        .fillMaxWidth()
                                        .onSizeChanged { openersBowlerWidth = it.width }
                                    ) {
                                        OutlinedTextField(
                                            value = selectedBowler,
                                            onValueChange = {},
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedTextColor = CleanWhite,
                                                unfocusedTextColor = CoolSlate,
                                                focusedBorderColor = InfoTeal,
                                                unfocusedBorderColor = GlassBorder,
                                                focusedContainerColor = OuterSpace
                                            ),
                                            trailingIcon = {
                                                IconButton(onClick = { showBowlerDropdown = !showBowlerDropdown }) {
                                                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Select Bowler", tint = InfoTeal)
                                                }
                                            },
                                            modifier = Modifier.fillMaxWidth().testTag("setup_opening_bowler_input"),
                                            singleLine = true,
                                            readOnly = true
                                        )
                                        DropdownMenu(
                                            expanded = showBowlerDropdown,
                                            onDismissRequest = { showBowlerDropdown = false },
                                            modifier = Modifier
                                                .width(with(LocalDensity.current) { openersBowlerWidth.toDp() })
                                                .heightIn(max = 240.dp)
                                                .background(DarkBgSurface)
                                                .border(1.dp, GlassBorder, RoundedCornerShape(8.dp))
                                        ) {
                                            bowlRoster.forEach { bowlName ->
                                                DropdownMenuItem(
                                                    text = { Text(bowlName, color = CleanWhite) },
                                                    onClick = {
                                                        selectedBowler = bowlName
                                                        showBowlerDropdown = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Button(
                                    onClick = {
                                        if (selectedStriker.isNotEmpty() && selectedNonStriker.isNotEmpty() && selectedBowler.isNotEmpty()) {
                                            viewModel.changeActiveStriker(selectedStriker)
                                            viewModel.changeActiveNonStriker(selectedNonStriker)
                                            viewModel.changeActiveBowler(selectedBowler)
                                            showOpenersSelectionCard = false
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = StadiumGreen),
                                    modifier = Modifier.testTag("confirm_openers_btn")
                                ) {
                                    Text("START INNINGS PLAY", color = DarkBgMain, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        // 1d. Over Completion Bowler Selector Card Dialog
        if (showNewOverBowlerDialog && !isInningsOrMatchCompleted) {
            val lastBowler = activeMatch.deliveries.lastOrNull { it.innings == activeMatch.currentInnings }?.bowler
            val eligibleBowlers = bowlRoster.filter { it != lastBowler }
            var selectedNewBowler by remember { mutableStateOf(eligibleBowlers.getOrNull(0) ?: bowlRoster.getOrNull(0) ?: "") }
            var showNewBowlerDropdown by remember { mutableStateOf(false) }

            Dialog(
                onDismissRequest = { /* Must select, cannot dismiss */ },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.6f)),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth(0.95f)
                            .widthIn(max = 500.dp)
                            .padding(16.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = DarkBgSurface,
                        border = BorderStroke(1.dp, GlassBorder)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = "NEW BOWLER REQUIRED | OVER COMPLETED",
                                color = InfoTeal,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black
                            )

                            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                Text(
                                    text = "A full legal over of 6 balls has been bowled. Under cricket regulations, please select a different bowler to deliver the next over:",
                                    color = CleanWhite,
                                    fontSize = 13.sp
                                )
                                var newBowlerBoxWidth by remember { mutableStateOf(0) }
                                Box(modifier = Modifier
                                    .fillMaxWidth()
                                    .onSizeChanged { newBowlerBoxWidth = it.width }
                                ) {
                                    OutlinedTextField(
                                        value = selectedNewBowler,
                                        onValueChange = {},
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = CleanWhite,
                                            unfocusedTextColor = CoolSlate,
                                            focusedBorderColor = InfoTeal,
                                            unfocusedBorderColor = GlassBorder,
                                            focusedContainerColor = OuterSpace
                                        ),
                                        trailingIcon = {
                                            IconButton(onClick = { showNewBowlerDropdown = !showNewBowlerDropdown }) {
                                                Icon(Icons.Default.ArrowDropDown, contentDescription = "Select Bowler", tint = InfoTeal)
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth().testTag("setup_new_over_bowler_input"),
                                        singleLine = true,
                                        readOnly = true
                                    )
                                    DropdownMenu(
                                        expanded = showNewBowlerDropdown,
                                        onDismissRequest = { showNewBowlerDropdown = false },
                                        modifier = Modifier
                                            .width(with(LocalDensity.current) { newBowlerBoxWidth.toDp() })
                                            .heightIn(max = 240.dp)
                                            .background(DarkBgSurface)
                                            .border(1.dp, GlassBorder, RoundedCornerShape(8.dp))
                                    ) {
                                        eligibleBowlers.forEach { bName ->
                                            DropdownMenuItem(
                                                text = { Text(bName, color = CleanWhite) },
                                                onClick = {
                                                    selectedNewBowler = bName
                                                    showNewBowlerDropdown = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Button(
                                    onClick = {
                                        if (selectedNewBowler.isNotEmpty()) {
                                            viewModel.changeActiveBowler(selectedNewBowler)
                                            showNewOverBowlerDialog = false
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = InfoTeal),
                                    modifier = Modifier.testTag("confirm_new_over_bowler_btn")
                                ) {
                                    Text("START NEW OVER", color = DarkBgMain, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. Active Bowler Selector Dialog
        if (showBowlerDialog) {
            var editNameText by remember(activeMatch.bowlerName) { mutableStateOf(activeMatch.bowlerName) }
            AlertDialog(
                onDismissRequest = { showBowlerDialog = false },
                containerColor = DarkBgSurface,
                title = { Text("MANAGE ACTIVE BOWLER", color = InfoTeal, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Column {
                            Text("TYPE OR CHANGE BOWLER NAME:", style = MaterialTheme.typography.labelMedium, color = MutedGrey, modifier = Modifier.padding(bottom = 6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = editNameText,
                                    onValueChange = { editNameText = it },
                                    modifier = Modifier.weight(1f).testTag("bowler_rename_input"),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = InfoTeal,
                                        unfocusedBorderColor = GlassBorder,
                                        focusedTextColor = CleanWhite,
                                        unfocusedTextColor = CoolSlate
                                    )
                                )
                                Button(
                                    onClick = {
                                        if (editNameText.trim().isNotEmpty()) {
                                            viewModel.renamePlayerInActiveMatch(activeMatch.bowlerName, editNameText.trim())
                                            showBowlerDialog = false
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = InfoTeal, contentColor = DarkBgMain),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.height(56.dp).testTag("bowler_rename_btn")
                                ) {
                                    Text("SAVE", fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Divider(color = GlassBorder)

                        Text("OR SELECT ANOTHER TEAM BOWLER:", style = MaterialTheme.typography.labelMedium, color = MutedGrey, modifier = Modifier.padding(bottom = 4.dp))
                        
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            bowlRoster.forEach { bowler ->
                                val current = activeMatch.bowlerName == bowler
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (current) Color(0x3B00E5FF) else Color.Transparent)
                                        .clickable {
                                            viewModel.changeActiveBowler(bowler)
                                            showBowlerDialog = false
                                        }
                                        .padding(horizontal = 12.dp, vertical = 10.dp)
                                        .testTag("select_bowler_${bowler.replace(" ", "_")}"),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = bowler,
                                        color = if (current) InfoTeal else CleanWhite,
                                        fontWeight = if (current) FontWeight.Bold else FontWeight.Normal,
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (current) Icon(Icons.Default.Check, contentDescription = "Active", tint = InfoTeal)
                                }
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { showBowlerDialog = false }) { Text("CANCEL", color = CoolSlate) }
                }
            )
        }

        // 3. Reselect Striker Dialog
        if (showStrikerDialog) {
            var editNameText by remember(activeMatch.strikerName) { mutableStateOf(activeMatch.strikerName) }
            AlertDialog(
                onDismissRequest = { showStrikerDialog = false },
                containerColor = DarkBgSurface,
                title = { Text("MANAGE STRIKER BATSMAN", color = StadiumGreen, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Column {
                            Text("TYPE OR CHANGE STRIKER NAME:", style = MaterialTheme.typography.labelMedium, color = MutedGrey, modifier = Modifier.padding(bottom = 6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = editNameText,
                                    onValueChange = { editNameText = it },
                                    modifier = Modifier.weight(1f).testTag("striker_rename_input"),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = StadiumGreen,
                                        unfocusedBorderColor = GlassBorder,
                                        focusedTextColor = CleanWhite,
                                        unfocusedTextColor = CoolSlate
                                    )
                                )
                                Button(
                                    onClick = {
                                        if (editNameText.trim().isNotEmpty()) {
                                            viewModel.renamePlayerInActiveMatch(activeMatch.strikerName, editNameText.trim())
                                            showStrikerDialog = false
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = StadiumGreen, contentColor = DarkBgMain),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.height(56.dp).testTag("striker_rename_btn")
                                ) {
                                    Text("SAVE", fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Divider(color = GlassBorder)

                        Text("OR SWAP / SELECT ANOTHER PLAYER:", style = MaterialTheme.typography.labelMedium, color = MutedGrey, modifier = Modifier.padding(bottom = 4.dp))

                        val alreadyOutPlayers = remember(summary) {
                            summary.batsmanStats.filter { it.dismissed && it.wicketType != WicketType.RETIRED_HURT }.map { it.name }.toSet()
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            batRoster.filter { it != activeMatch.nonStrikerName && it !in alreadyOutPlayers }.forEach { batsman ->
                                val current = activeMatch.strikerName == batsman
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (current) Color(0x3B00FF87) else Color.Transparent)
                                        .clickable {
                                            viewModel.changeActiveStriker(batsman)
                                            showStrikerDialog = false
                                        }
                                        .padding(horizontal = 12.dp, vertical = 10.dp)
                                        .testTag("select_striker_${batsman.replace(" ", "_")}"),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = batsman,
                                        color = if (current) StadiumGreen else CleanWhite,
                                        fontWeight = if (current) FontWeight.Bold else FontWeight.Normal,
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (current) Icon(Icons.Default.Check, contentDescription = "Striker", tint = StadiumGreen)
                                }
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { showStrikerDialog = false }) { Text("CANCEL", color = CoolSlate) }
                }
            )
        }

        // 4. Reselect Non-Striker Dialog
        if (showNonStrikerDialog) {
            var editNameText by remember(activeMatch.nonStrikerName) { mutableStateOf(activeMatch.nonStrikerName) }
            AlertDialog(
                onDismissRequest = { showNonStrikerDialog = false },
                containerColor = DarkBgSurface,
                title = { Text("MANAGE NON-STRIKER BATSMAN", color = CoolSlate, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Column {
                            Text("TYPE OR CHANGE NON-STRIKER NAME:", style = MaterialTheme.typography.labelMedium, color = MutedGrey, modifier = Modifier.padding(bottom = 6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = editNameText,
                                    onValueChange = { editNameText = it },
                                    modifier = Modifier.weight(1f).testTag("nonstriker_rename_input"),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = StadiumGreen,
                                        unfocusedBorderColor = GlassBorder,
                                        focusedTextColor = CleanWhite,
                                        unfocusedTextColor = CoolSlate
                                    )
                                )
                                Button(
                                    onClick = {
                                        if (editNameText.trim().isNotEmpty()) {
                                            viewModel.renamePlayerInActiveMatch(activeMatch.nonStrikerName, editNameText.trim())
                                            showNonStrikerDialog = false
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = StadiumGreen, contentColor = DarkBgMain),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.height(56.dp).testTag("nonstriker_rename_btn")
                                ) {
                                    Text("SAVE", fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Divider(color = GlassBorder)

                        Text("OR SWAP / SELECT ANOTHER PLAYER:", style = MaterialTheme.typography.labelMedium, color = MutedGrey, modifier = Modifier.padding(bottom = 4.dp))

                        val alreadyOutPlayers = remember(summary) {
                            summary.batsmanStats.filter { it.dismissed && it.wicketType != WicketType.RETIRED_HURT }.map { it.name }.toSet()
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            batRoster.filter { it != activeMatch.strikerName && it !in alreadyOutPlayers }.forEach { batsman ->
                                val current = activeMatch.nonStrikerName == batsman
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (current) Color(0x1FFFFFFF) else Color.Transparent)
                                        .clickable {
                                            viewModel.changeActiveNonStriker(batsman)
                                            showNonStrikerDialog = false
                                        }
                                        .padding(horizontal = 12.dp, vertical = 10.dp)
                                        .testTag("select_nonstriker_${batsman.replace(" ", "_")}"),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = batsman,
                                        color = if (current) StadiumGreen else CleanWhite,
                                        fontWeight = if (current) FontWeight.Bold else FontWeight.Normal,
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (current) Icon(Icons.Default.Check, contentDescription = "Non-Striker", tint = StadiumGreen)
                                }
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { showNonStrikerDialog = false }) { Text("CANCEL", color = CoolSlate) }
                }
            )
        }
    }
}

@Composable
fun CricketBatIcon(modifier: Modifier = Modifier) {
    androidx.compose.foundation.Canvas(modifier = modifier.size(14.dp)) {
        rotate(degrees = -35f, pivot = center) {
            val w = size.width
            val h = size.height
            val handleWidth = w * 0.15f
            val handleHeight = h * 0.35f
            drawRect(
                color = Color(0xFFE0E0E0),
                topLeft = androidx.compose.ui.geometry.Offset((w - handleWidth) / 2f, 0f),
                size = androidx.compose.ui.geometry.Size(handleWidth, handleHeight)
            )
            val bladeWidth = w * 0.35f
            val bladeHeight = h * 0.6f
            drawRoundRect(
                color = Color(0xFFE5A65D), // Nice wood gold-brown
                topLeft = androidx.compose.ui.geometry.Offset((w - bladeWidth) / 2f, handleHeight),
                size = androidx.compose.ui.geometry.Size(bladeWidth, bladeHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(1.5.dp.toPx(), 1.5.dp.toPx())
            )
        }
    }
}

@Composable
fun CricketBallIcon(modifier: Modifier = Modifier) {
    androidx.compose.foundation.Canvas(modifier = modifier.size(14.dp)) {
        val radius = size.minDimension / 2
        // Background - vibrant red cricket ball
        drawCircle(
            color = Color(0xFFE53935),
            radius = radius
        )
        // Draw seam (vertical white arc in the center)
        drawArc(
            color = Color.White.copy(alpha = 0.7f),
            startAngle = -90f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = androidx.compose.ui.geometry.Offset(radius * 0.4f, 0f),
            size = androidx.compose.ui.geometry.Size(radius * 1.2f, radius * 2f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5.dp.toPx())
        )
    }
}

