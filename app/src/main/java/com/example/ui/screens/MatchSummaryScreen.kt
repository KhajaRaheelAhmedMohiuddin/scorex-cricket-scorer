package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.example.R
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BowlerStats
import com.example.model.InningsSummary
import com.example.model.MatchStatus
import com.example.ui.components.GlassmorphicCard
import com.example.ui.components.GlowBorderGlassmorphicCard
import com.example.ui.theme.*
import com.example.viewmodel.AppScreen
import com.example.viewmodel.MatchViewModel

@Composable
fun MatchSummaryScreen(
    viewModel: MatchViewModel,
    modifier: Modifier = Modifier
) {
    val match by viewModel.activeMatch.collectAsState()
    
    if (match == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No completed match loaded", color = CoolSlate)
        }
        return
    }

    val activeMatch = match!!
    val hasPlayed2ndInnings = activeMatch.currentInnings == 2 || activeMatch.status == "COMPLETED"

    val summaryInn1 = remember(activeMatch.deliveries) { viewModel.getInnings1Summary() }
    val summaryInn2 = remember(activeMatch.deliveries) { viewModel.getInnings2Summary() }
    val summaryInn3 = remember(activeMatch.deliveries) { viewModel.getInnings3Summary() }
    val summaryInn4 = remember(activeMatch.deliveries) { viewModel.getInnings4Summary() }
    
    val hasSuperOver = activeMatch.currentInnings >= 3 || activeMatch.deliveries.any { it.innings >= 3 }

    val teamBName = if (activeMatch.firstInningsBattingTeam == activeMatch.teamA) activeMatch.teamB else activeMatch.teamA

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
    ) {
        // Sticky Header: Static container
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Professional Sports-Broadcast Header using Box relative alignment
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 0.dp),
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
                            viewModel.selectedDashboardTab.value = 2 // Go to History screen tab
                            viewModel.navigateTo(AppScreen.Dashboard)
                        }
                        .testTag("summary_back_btn"),
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
                        text = "MATCH RECAP",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 2.sp
                        ),
                        color = StadiumGreen,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "THE FINAL VERDICT",
                        style = MaterialTheme.typography.labelSmall,
                        color = MutedGrey,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Scrollable content body below the static header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(scrollState)
                .padding(start = 16.dp, end = 16.dp, bottom = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

        // --- 1. Winner Banner Card ---
        GlowBorderGlassmorphicCard(
            modifier = Modifier.fillMaxWidth(),
            glowColors = listOf(InfoTeal, StadiumGreen),
            elevation = 16.dp
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Premium Trophy Emblem Centerpiece
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(GoldAccent.copy(alpha = 0.15f))
                        .border(2.dp, GoldAccent.copy(alpha = 0.40f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.MilitaryTech,
                        contentDescription = "Winner Trophy",
                        tint = GoldAccent,
                        modifier = Modifier.size(64.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "WINNER REPORT",
                    style = MaterialTheme.typography.labelLarge,
                    color = MutedGrey
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = (activeMatch.winner ?: "MATCH CONCLUDED").abbreviateTeams(activeMatch.teamA, activeMatch.teamB).uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    color = CleanWhite,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center,
                    fontSize = 20.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // --- 1.5. Tactical Navigation Hub (Quick Action Tiles) ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SummaryActionButton(
                onClick = { viewModel.navigateTo(AppScreen.Scorecard) },
                icon = Icons.Filled.ListAlt,
                label = "Scorecard",
                accentColor = InfoTeal,
                modifier = Modifier.weight(1f),
                testTag = "summary_nav_scorecard_btn"
            )

            SummaryActionButton(
                onClick = { viewModel.navigateTo(AppScreen.Analysis) },
                icon = Icons.Filled.Analytics,
                label = "Analytics",
                accentColor = GoldAccent,
                modifier = Modifier.weight(1f),
                testTag = "summary_nav_analytics_btn"
            )
        }

        // --- 2. Comparative Inning Stats ---
        GlassmorphicCard(
            modifier = Modifier.fillMaxWidth(),
            borderColor = GlassBorder
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "INNINGS SUMMARY & COMPARISON",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = CoolSlate,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                )
                // Inning 1 line
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = activeMatch.firstInningsBattingTeam.toAbbreviation().uppercase(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = InfoTeal
                        )
                        Text(
                            text = "First Innings - Overs: ${summaryInn1.overs}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MutedGrey,
                            fontSize = 10.sp
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${summaryInn1.totalRuns}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = CleanWhite
                        )
                        Text(
                            text = " / ${summaryInn1.totalWickets}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = WicketCrimson,
                            modifier = Modifier.padding(start = 2.dp)
                        )
                    }
                }

                Divider(color = GlassBorder, thickness = 0.5.dp)

                // Inning 2 line (if played)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = teamBName.toAbbreviation().uppercase(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (hasPlayed2ndInnings) GoldAccent else DarkTextMuted
                        )
                        Text(
                            text = if (hasPlayed2ndInnings) "Second Innings - Overs: ${summaryInn2.overs}" else "Innings 2 not played",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (hasPlayed2ndInnings) MutedGrey else DarkTextMuted,
                            fontSize = 10.sp
                        )
                    }

                    if (hasPlayed2ndInnings) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${summaryInn2.totalRuns}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Black,
                                color = CleanWhite
                            )
                            Text(
                                text = " / ${summaryInn2.totalWickets}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Black,
                                color = WicketCrimson,
                                modifier = Modifier.padding(start = 2.dp)
                            )
                        }
                    } else {
                        Text(
                            text = "—",
                            style = MaterialTheme.typography.titleLarge,
                            color = DarkTextMuted
                        )
                    }
                }

                if (hasSuperOver) {
                    Divider(color = GlassBorder, thickness = 0.5.dp)

                    // Super Over Innings 1 (Innings 3)
                    val hasSuperInn1 = activeMatch.currentInnings >= 3 || activeMatch.deliveries.any { it.innings == 3 }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = teamBName.toAbbreviation().uppercase(),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (hasSuperInn1) WicketCrimson else DarkTextMuted
                            )
                            Text(
                                text = if (hasSuperInn1) "Super Over Innings 1 - Overs: ${summaryInn3.overs}" else "Super Over Innings 1 not played",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (hasSuperInn1) MutedGrey else DarkTextMuted,
                                fontSize = 10.sp
                            )
                        }

                        if (hasSuperInn1) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "${summaryInn3.totalRuns}",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Black,
                                    color = CleanWhite
                                )
                                Text(
                                    text = " / ${summaryInn3.totalWickets}",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Black,
                                    color = WicketCrimson,
                                    modifier = Modifier.padding(start = 2.dp)
                                )
                            }
                        } else {
                            Text(
                                text = "—",
                                style = MaterialTheme.typography.titleLarge,
                                color = DarkTextMuted
                            )
                        }
                    }

                    Divider(color = GlassBorder, thickness = 0.5.dp)

                    // Super Over Innings 2 (Innings 4)
                    val hasSuperInn2 = activeMatch.currentInnings >= 4 || activeMatch.deliveries.any { it.innings == 4 }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = activeMatch.firstInningsBattingTeam.toAbbreviation().uppercase(),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (hasSuperInn2) StadiumGreen else DarkTextMuted
                            )
                            Text(
                                text = if (hasSuperInn2) "Super Over Innings 2 - Overs: ${summaryInn4.overs}" else "Super Over Innings 2 not played",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (hasSuperInn2) MutedGrey else DarkTextMuted,
                                fontSize = 10.sp
                            )
                        }

                        if (hasSuperInn2) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "${summaryInn4.totalRuns}",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Black,
                                    color = CleanWhite
                                )
                                Text(
                                    text = " / ${summaryInn4.totalWickets}",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Black,
                                    color = WicketCrimson,
                                    modifier = Modifier.padding(start = 2.dp)
                                )
                            }
                        } else {
                            Text(
                                text = "—",
                                style = MaterialTheme.typography.titleLarge,
                                color = DarkTextMuted
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // --- 3. Key Performers Recognition (Rich stats wrapped in a GlassmorphicCard) ---
        GlassmorphicCard(
            modifier = Modifier.fillMaxWidth(),
            borderColor = CleanWhite.copy(alpha = 0.15f),
            contentPadding = 14.dp
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "KEY PERFORMERS",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = CoolSlate,
                    modifier = Modifier.padding(bottom = 14.dp)
                )

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Top Scorer Card
                    val topBatsman = (summaryInn1.batsmanStats + summaryInn2.batsmanStats)
                        .maxByOrNull { it.runs }

                    GlassmorphicCard(
                        modifier = Modifier.fillMaxWidth().height(115.dp),
                        borderColor = StadiumGreen.copy(alpha = 0.3f),
                        contentPadding = 0.dp
                    ) {
                        Box(modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp))) {
                            Image(
                                painter = painterResource(id = R.drawable.top_batter_bg),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.matchParentSize()
                            )
                            Column(
                                modifier = Modifier.fillMaxSize().padding(14.dp),
                                horizontalAlignment = Alignment.Start,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text("TOP BATTER", color = StadiumGreen, style = MaterialTheme.typography.labelLarge, fontSize = 10.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                
                                if (topBatsman != null && topBatsman.runs > 0) {
                                    Text(
                                        text = topBatsman.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = CleanWhite,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${topBatsman.runs} Runs",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Black,
                                        color = StadiumGreen
                                    )
                                    Text(
                                        text = "off ${topBatsman.balls} balls (SR: ${String.format(java.util.Locale.US, "%.1f", topBatsman.strikeRate)})",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MutedGrey,
                                        fontSize = 10.sp
                                    )
                                } else {
                                    Text("No runs scored", style = MaterialTheme.typography.bodySmall, color = DarkTextMuted)
                                }
                            }
                        }
                    }

                    // Top Bowler Card
                    val validBowlers = (summaryInn1.bowlerStats + summaryInn2.bowlerStats).filter { it.balls > 0 }
                    val sortedBowlers = validBowlers.sortedWith(
                        compareByDescending<BowlerStats> { it.wickets }
                            .thenBy { it.runsConceded }
                            .thenBy { it.economy }
                    )
                    val topBowlerPrimary = sortedBowlers.firstOrNull()
                    val tiedBowlers = if (topBowlerPrimary != null) {
                        sortedBowlers.filter {
                            it.wickets == topBowlerPrimary.wickets &&
                            it.runsConceded == topBowlerPrimary.runsConceded &&
                            it.economy == topBowlerPrimary.economy
                        }
                    } else {
                        emptyList()
                    }

                    GlassmorphicCard(
                        modifier = Modifier.fillMaxWidth().height(115.dp),
                        borderColor = InfoTeal.copy(alpha = 0.3f),
                        contentPadding = 0.dp
                    ) {
                        Box(modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp))) {
                            Image(
                                painter = painterResource(id = R.drawable.top_bowler_bg),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.matchParentSize()
                            )
                             Column(
                                modifier = Modifier.fillMaxSize().padding(14.dp),
                                horizontalAlignment = Alignment.Start,
                                verticalArrangement = Arrangement.Center
                             ) {
                                 val cardTitle = if (tiedBowlers.size > 1) "TOP BOWLERS" else "TOP BOWLER"

                                 if (tiedBowlers.isNotEmpty()) {
                                     if (tiedBowlers.size > 1) {
                                         val pagerState = rememberPagerState { tiedBowlers.size }
                                         
                                         Text(cardTitle, color = InfoTeal, style = MaterialTheme.typography.labelLarge, fontSize = 10.sp)
                                         Spacer(modifier = Modifier.height(4.dp))

                                         // Swipable Carousel
                                         HorizontalPager(
                                             state = pagerState,
                                             modifier = Modifier.fillMaxWidth()
                                         ) { page ->
                                             val bowler = tiedBowlers[page]
                                             val compOvers = bowler.balls / 6
                                             val remBalls = bowler.balls % 6
                                             val formattedOvers = "$compOvers.$remBalls"
                                             
                                             Column(
                                                 modifier = Modifier.fillMaxWidth(),
                                                 verticalArrangement = Arrangement.spacedBy(2.dp)
                                             ) {
                                                 Text(
                                                     text = bowler.name,
                                                     style = MaterialTheme.typography.bodyMedium,
                                                     fontWeight = FontWeight.Bold,
                                                     color = CleanWhite,
                                                     maxLines = 1,
                                                     overflow = TextOverflow.Ellipsis
                                                 )
                                                 Text(
                                                     text = "${bowler.wickets} Wkts",
                                                     style = MaterialTheme.typography.titleMedium,
                                                     fontWeight = FontWeight.Black,
                                                     color = InfoTeal
                                                 )
                                                 Text(
                                                     text = "for ${bowler.runsConceded} runs (Ovs: $formattedOvers)",
                                                     style = MaterialTheme.typography.bodySmall,
                                                     color = MutedGrey,
                                                     fontSize = 10.sp
                                                 )
                                             }
                                         }

                                         Spacer(modifier = Modifier.height(10.dp))

                                         // Modern indicator dots below the text
                                         Row(
                                             horizontalArrangement = Arrangement.spacedBy(4.dp),
                                             verticalAlignment = Alignment.CenterVertically
                                         ) {
                                             repeat(tiedBowlers.size) { index ->
                                                 val active = pagerState.currentPage == index
                                                 Box(
                                                     modifier = Modifier
                                                         .size(if (active) 6.dp else 4.dp)
                                                         .clip(CircleShape)
                                                         .background(if (active) InfoTeal else MutedGrey.copy(alpha = 0.5f))
                                                 )
                                             }
                                         }
                                     } else {
                                         val singleBowler = tiedBowlers.first()
                                         val compOvers = singleBowler.balls / 6
                                         val remBalls = singleBowler.balls % 6
                                         val formattedOvers = "$compOvers.$remBalls"

                                         Text(cardTitle, color = InfoTeal, style = MaterialTheme.typography.labelLarge, fontSize = 10.sp)
                                         Spacer(modifier = Modifier.height(4.dp))

                                         Text(
                                             text = singleBowler.name,
                                             style = MaterialTheme.typography.bodyMedium,
                                             fontWeight = FontWeight.Bold,
                                             color = CleanWhite,
                                             maxLines = 1,
                                             overflow = TextOverflow.Ellipsis
                                         )
                                         Spacer(modifier = Modifier.height(2.dp))
                                         Text(
                                             text = "${singleBowler.wickets} Wkts",
                                             style = MaterialTheme.typography.titleMedium,
                                             fontWeight = FontWeight.Black,
                                             color = InfoTeal
                                         )
                                         Text(
                                             text = "for ${singleBowler.runsConceded} runs (Ovs: $formattedOvers)",
                                             style = MaterialTheme.typography.bodySmall,
                                             color = MutedGrey,
                                             fontSize = 10.sp
                                         )
                                     }
                                 } else {
                                     Text(cardTitle, color = InfoTeal, style = MaterialTheme.typography.labelLarge, fontSize = 10.sp)
                                     Spacer(modifier = Modifier.height(4.dp))
                                     Text("No deliveries", style = MaterialTheme.typography.bodySmall, color = DarkTextMuted)
                                 }
                             }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
}

@Composable
fun SummaryActionButton(
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    accentColor: Color,
    modifier: Modifier = Modifier,
    testTag: String = ""
) {
    val gradientBrush = Brush.verticalGradient(
        colors = listOf(
            accentColor.copy(alpha = 0.12f),
            DarkBgSurface.copy(alpha = 0.6f)
        )
    )

    Box(
        modifier = modifier
            .height(64.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(gradientBrush)
            .border(
                border = BorderStroke(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            accentColor.copy(alpha = 0.40f),
                            accentColor.copy(alpha = 0.08f)
                        )
                    )
                ),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable { onClick() }
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        // Soft glowing radial background spotlight behind the icon
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            accentColor.copy(alpha = 0.1f),
                            Color.Transparent
                        ),
                        radius = 120f
                    )
                )
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon locked in a sleek circular container
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.15f))
                    .border(1.dp, accentColor.copy(alpha = 0.30f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = accentColor,
                    modifier = Modifier.size(18.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(10.dp))
            
            Column(
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = label.uppercase(),
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.2.sp
                    ),
                    color = CleanWhite,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (label.equals("Scorecard", ignoreCase = true)) "Detailed Stats" else "Match Insights",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    color = MutedGrey,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
