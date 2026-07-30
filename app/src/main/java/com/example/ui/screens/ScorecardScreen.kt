package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BatsmanStats
import com.example.model.BowlerStats
import com.example.model.ExtraType
import com.example.model.InningsSummary
import com.example.model.Partnership
import com.example.model.WicketType
import com.example.ui.components.GlassmorphicCard
import com.example.ui.theme.*
import com.example.viewmodel.AppScreen
import com.example.viewmodel.MatchViewModel

@Composable
fun ScorecardScreen(
    viewModel: MatchViewModel,
    modifier: Modifier = Modifier
) {
    val match by viewModel.activeMatch.collectAsState()
    
    if (match == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No active match loaded", color = CoolSlate)
        }
        return
    }

    val activeMatch = match!!
    val summaryInnings1 = remember(activeMatch.deliveries) { viewModel.getInnings1Summary() }
    val summaryInnings2 = remember(activeMatch.deliveries) { viewModel.getInnings2Summary() }
    val summaryInnings3 = remember(activeMatch.deliveries) { viewModel.getInnings3Summary() }
    val summaryInnings4 = remember(activeMatch.deliveries) { viewModel.getInnings4Summary() }

    val hasSuperOver = activeMatch.currentInnings >= 3 || activeMatch.deliveries.any { it.innings >= 3 }

    var selectedInningsTab by remember(activeMatch.id, hasSuperOver) {
        mutableStateOf(if (hasSuperOver) activeMatch.currentInnings.coerceIn(1, 4) else activeMatch.currentInnings.coerceIn(1, 2))
    }

    val activeSummary = when (selectedInningsTab) {
        1 -> summaryInnings1
        2 -> summaryInnings2
        3 -> summaryInnings3
        4 -> summaryInnings4
        else -> summaryInnings1
    }
    val activeBattingTeamName = when (selectedInningsTab) {
        1 -> activeMatch.firstInningsBattingTeam
        2 -> if (activeMatch.firstInningsBattingTeam == activeMatch.teamA) activeMatch.teamB else activeMatch.teamA
        3 -> if (activeMatch.firstInningsBattingTeam == activeMatch.teamA) activeMatch.teamB else activeMatch.teamA
        4 -> activeMatch.firstInningsBattingTeam
        else -> activeMatch.firstInningsBattingTeam
    }
    
    val scrollState = rememberScrollState()
    var viewSuperOverTabs by remember(activeMatch.id, selectedInningsTab) {
        mutableStateOf(selectedInningsTab >= 3)
    }

    // Scorecard Page Container
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBgMain)
    ) {
        Column(
            modifier = modifier
                .fillMaxSize()
        ) {
            // Sticky Header Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Symmetric Header Architecture: Professional Sports-Broadcast Header
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
                                if (activeMatch.status == "COMPLETED") {
                                    viewModel.navigateTo(AppScreen.Summary)
                                } else {
                                    viewModel.navigateTo(AppScreen.Scoring)
                                }
                            }
                            .testTag("scorecard_back_btn"),
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
                            text = "SCORECARD",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = 2.sp
                            ),
                            color = StadiumGreen,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "LIVE BROADCAST SUMMARY",
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

            // Scrollable Body Content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(scrollState)
                    .padding(start = 16.dp, end = 16.dp, bottom = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

            // --- 2. Dynamic Tabbed Navigation ---
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (hasSuperOver) {
                    // Modern pill segmented control to toggle between Regular Match and Super Overs
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .background(DarkBgSurface.copy(alpha = 0.6f))
                            .border(1.dp, GlassBorder, RoundedCornerShape(24.dp))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Option 1: Regular Match
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (!viewSuperOverTabs) InfoTeal.copy(alpha = 0.12f) else Color.Transparent)
                                .clickable {
                                    viewSuperOverTabs = false
                                    selectedInningsTab = if (activeMatch.currentInnings >= 2) 2 else 1
                                }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "MAIN MATCH",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 1.sp
                                ),
                                color = if (!viewSuperOverTabs) InfoTeal else MutedGrey,
                                fontSize = 11.sp
                            )
                        }

                        // Option 2: Super Over
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (viewSuperOverTabs) WicketCrimson.copy(alpha = 0.12f) else Color.Transparent)
                                .clickable {
                                    viewSuperOverTabs = true
                                    selectedInningsTab = if (activeMatch.currentInnings >= 4) 4 else 3
                                }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "SUPER OVER",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 1.sp
                                ),
                                color = if (viewSuperOverTabs) WicketCrimson else MutedGrey,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                if (!viewSuperOverTabs) {
                    // Regular Match Innings Tabs
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val t1Abbr = activeMatch.firstInningsBattingTeam.toAbbreviation().uppercase()
                        InningsTabButton(
                            active = selectedInningsTab == 1,
                            enabled = true,
                            label = "INNINGS 1",
                            teamAbbreviation = t1Abbr,
                            color = InfoTeal,
                            onClick = { selectedInningsTab = 1 },
                            testTag = "tab_innings_1",
                            modifier = Modifier.weight(1f)
                        )

                        val innings2Avail = activeMatch.currentInnings >= 2 || activeMatch.status == "COMPLETED" || hasSuperOver
                        val t2Abbr = (if (activeMatch.firstInningsBattingTeam == activeMatch.teamA) activeMatch.teamB else activeMatch.teamA).toAbbreviation().uppercase()
                        InningsTabButton(
                            active = selectedInningsTab == 2,
                            enabled = innings2Avail,
                            label = "INNINGS 2",
                            teamAbbreviation = t2Abbr,
                            color = GoldAccent,
                            onClick = { if (innings2Avail) selectedInningsTab = 2 },
                            testTag = "tab_innings_2",
                            modifier = Modifier.weight(1f)
                        )
                    }
                } else {
                    // Super Over Innings Tabs
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val t1Abbr = activeMatch.firstInningsBattingTeam.toAbbreviation().uppercase()
                        val t2Abbr = (if (activeMatch.firstInningsBattingTeam == activeMatch.teamA) activeMatch.teamB else activeMatch.teamA).toAbbreviation().uppercase()

                        val innings3Avail = activeMatch.currentInnings >= 3 || activeMatch.deliveries.any { it.innings == 3 }
                        InningsTabButton(
                            active = selectedInningsTab == 3,
                            enabled = innings3Avail,
                            label = "SUPER OVER 1",
                            teamAbbreviation = t2Abbr,
                            color = WicketCrimson,
                            onClick = { if (innings3Avail) selectedInningsTab = 3 },
                            testTag = "tab_innings_3",
                            modifier = Modifier.weight(1f)
                        )

                        val innings4Avail = activeMatch.currentInnings >= 4 || activeMatch.deliveries.any { it.innings == 4 }
                        InningsTabButton(
                            active = selectedInningsTab == 4,
                            enabled = innings4Avail,
                            label = "SUPER OVER 2",
                            teamAbbreviation = t1Abbr,
                            color = StadiumGreen,
                            onClick = { if (innings4Avail) selectedInningsTab = 4 },
                            testTag = "tab_innings_4",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            val tabColor = when (selectedInningsTab) {
                1 -> InfoTeal
                2 -> GoldAccent
                3 -> WicketCrimson
                4 -> StadiumGreen
                else -> InfoTeal
            }

            // --- 3. Hierarchical Score Summary ---
            GlassmorphicCard(
                modifier = Modifier.fillMaxWidth(),
                borderColor = tabColor.copy(alpha = 0.3f), // Layered fine-line borders
                backgroundColor = DarkBgSurface.copy(alpha = 0.40f) // Frosted Dashboard Surface
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "INNINGS SUMMARY",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.5.sp
                        ),
                        color = CoolSlate,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = activeBattingTeamName.toAbbreviation().uppercase(),
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 0.5.sp
                                ),
                                color = CleanWhite
                            )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "OVERS: ${activeSummary.overs}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = tabColor
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End
                    ) {
                        Text(
                            text = "${activeSummary.totalRuns}",
                            style = MaterialTheme.typography.displayMedium.copy(
                                fontWeight = FontWeight.Black
                            ),
                            color = CleanWhite
                        )
                        Text(
                            text = "/",
                            style = MaterialTheme.typography.displayMedium.copy(
                                fontWeight = FontWeight.Normal
                            ),
                            color = tabColor.copy(alpha = 0.5f),
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                        Text(
                            text = "${activeSummary.totalWickets}",
                            style = MaterialTheme.typography.displayMedium.copy(
                                fontWeight = FontWeight.Black
                            ),
                            color = WicketCrimson
                        )
                    }
                }
            }
        }

            Spacer(modifier = Modifier.height(20.dp))

            // --- 4. Batting Performance Card ---
            GlassmorphicCard(
                modifier = Modifier.fillMaxWidth(),
                borderColor = GlassBorder,
                backgroundColor = DarkBgSurface.copy(alpha = 0.40f)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "BATTING PERFORMANCE",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp
                        ),
                        color = CoolSlate,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        textAlign = TextAlign.Center
                    )
                    // Header row with strict column weighting
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("BATTER", modifier = Modifier.weight(1.8f), style = MaterialTheme.typography.labelSmall, color = MutedGrey, fontWeight = FontWeight.Bold)
                        Text("R", modifier = Modifier.weight(0.4f), style = MaterialTheme.typography.labelSmall, color = MutedGrey, fontWeight = FontWeight.Bold, textAlign = TextAlign.End)
                        Text("B", modifier = Modifier.weight(0.4f), style = MaterialTheme.typography.labelSmall, color = MutedGrey, fontWeight = FontWeight.Bold, textAlign = TextAlign.End)
                        Text("4S", modifier = Modifier.weight(0.4f), style = MaterialTheme.typography.labelSmall, color = MutedGrey, fontWeight = FontWeight.Bold, textAlign = TextAlign.End)
                        Text("6S", modifier = Modifier.weight(0.4f), style = MaterialTheme.typography.labelSmall, color = MutedGrey, fontWeight = FontWeight.Bold, textAlign = TextAlign.End)
                        Text("SR", modifier = Modifier.weight(0.6f), style = MaterialTheme.typography.labelSmall, color = MutedGrey, fontWeight = FontWeight.Bold, textAlign = TextAlign.End)
                    }
                    
                    Divider(color = CoolSlate.copy(alpha = 0.15f), thickness = 1.dp, modifier = Modifier.padding(vertical = 8.dp))

                    val isCurrentInningsActive = selectedInningsTab == activeMatch.currentInnings && activeMatch.status != "COMPLETED"
                    val inningsDeliveries = activeMatch.deliveries.filter { it.innings == selectedInningsTab }
                    val lastDeliveryOfInnings = inningsDeliveries.lastOrNull()

                    val currentStriker = if (isCurrentInningsActive) activeMatch.strikerName else lastDeliveryOfInnings?.striker
                    val currentNonStriker = if (isCurrentInningsActive) activeMatch.nonStrikerName else lastDeliveryOfInnings?.nonStriker

                    val facedPlayers = activeSummary.batsmanStats.filter { bat ->
                        bat.balls > 0 || 
                        bat.dismissed || 
                        bat.name == currentStriker || 
                        bat.name == currentNonStriker
                    }.filter { bat ->
                        bat.name != "Batsman Out" && 
                        bat.name != "Next Bowler" && 
                        bat.name.isNotEmpty()
                    }
                    
                    if (facedPlayers.isEmpty()) {
                        Text(
                            text = "No deliveries recorded in this innings.",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = DarkTextMuted,
                            textAlign = TextAlign.Center
                        )
                    } else {
                        facedPlayers.forEach { bat ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1.8f)) {
                                    Text(
                                        text = bat.name,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = CleanWhite,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = getWicketStatusMsg(bat),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MutedGrey,
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                
                                // Hero stat runs: bold & CleanWhite
                                Text(
                                    text = "${bat.runs}",
                                    modifier = Modifier.weight(0.4f),
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Black
                                    ),
                                    color = CleanWhite,
                                    textAlign = TextAlign.End
                                )
                                Text("${bat.balls}", modifier = Modifier.weight(0.4f), style = MaterialTheme.typography.bodySmall, color = CoolSlate, textAlign = TextAlign.End)
                                Text("${bat.fours}", modifier = Modifier.weight(0.4f), style = MaterialTheme.typography.bodySmall, color = MutedGrey, textAlign = TextAlign.End)
                                Text("${bat.sixes}", modifier = Modifier.weight(0.4f), style = MaterialTheme.typography.bodySmall, color = MutedGrey, textAlign = TextAlign.End)
                                
                                // Strike rate: dynamic elite stat highlighting
                                Text(
                                    text = String.format(java.util.Locale.US, "%.1f", bat.strikeRate),
                                    modifier = Modifier.weight(0.6f),
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = if (bat.strikeRate >= 140) StadiumGreen else CoolSlate,
                                    textAlign = TextAlign.End
                                )
                            }
                        }
                    }

                    Divider(color = CoolSlate.copy(alpha = 0.1f), thickness = 0.5.dp, modifier = Modifier.padding(vertical = 10.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val wides = activeSummary.extras[ExtraType.WIDE] ?: 0
                        val nbs = activeSummary.extras[ExtraType.NO_BALL] ?: 0
                        val byes = activeSummary.extras[ExtraType.BYE] ?: 0
                        val lbs = activeSummary.extras[ExtraType.LEG_BYE] ?: 0
                        val totExStr = "WD: $wides | NB: $nbs | BYE: $byes | LBYE: $lbs"

                        Text(
                            text = "EXTRAS",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            ),
                            color = MutedGrey
                        )
                        Text(
                            text = totExStr,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            ),
                            color = GoldAccent,
                            fontSize = 11.sp
                        )
                    }
                }
            }



            Spacer(modifier = Modifier.height(20.dp))

            // --- 5. Bowling Performance Card ---
            GlassmorphicCard(
                modifier = Modifier.fillMaxWidth(),
                borderColor = GlassBorder,
                backgroundColor = DarkBgSurface.copy(alpha = 0.40f)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "BOWLING ANALYSIS",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp
                        ),
                        color = CoolSlate,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        textAlign = TextAlign.Center
                    )
                    // Header row with strict column weighting
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("BOWLER", modifier = Modifier.weight(1.8f), style = MaterialTheme.typography.labelSmall, color = MutedGrey, fontWeight = FontWeight.Bold)
                        Text("O", modifier = Modifier.weight(0.4f), style = MaterialTheme.typography.labelSmall, color = MutedGrey, fontWeight = FontWeight.Bold, textAlign = TextAlign.End)
                        Text("M", modifier = Modifier.weight(0.4f), style = MaterialTheme.typography.labelSmall, color = MutedGrey, fontWeight = FontWeight.Bold, textAlign = TextAlign.End)
                        Text("R", modifier = Modifier.weight(0.4f), style = MaterialTheme.typography.labelSmall, color = MutedGrey, fontWeight = FontWeight.Bold, textAlign = TextAlign.End)
                        Text("W", modifier = Modifier.weight(0.4f), style = MaterialTheme.typography.labelSmall, color = MutedGrey, fontWeight = FontWeight.Bold, textAlign = TextAlign.End)
                        Text("ECON", modifier = Modifier.weight(0.6f), style = MaterialTheme.typography.labelSmall, color = MutedGrey, fontWeight = FontWeight.Bold, textAlign = TextAlign.End)
                    }

                    Divider(color = CoolSlate.copy(alpha = 0.15f), thickness = 1.dp, modifier = Modifier.padding(vertical = 8.dp))

                    val bowledPlayers = activeSummary.bowlerStats.filter { it.balls > 0 }
                    if (bowledPlayers.isEmpty()) {
                        Text(
                            text = "No recorded bowler deliveries yet.",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = DarkTextMuted,
                            textAlign = TextAlign.Center
                        )
                    } else {
                        bowledPlayers.forEach { bowl ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = bowl.name,
                                    modifier = Modifier.weight(1.8f),
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = CleanWhite,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(bowl.overs, modifier = Modifier.weight(0.4f), style = MaterialTheme.typography.bodySmall, color = CleanWhite, fontWeight = FontWeight.Bold, textAlign = TextAlign.End)
                                Text("${bowl.maidens}", modifier = Modifier.weight(0.4f), style = MaterialTheme.typography.bodySmall, color = CoolSlate, textAlign = TextAlign.End)
                                Text("${bowl.runsConceded}", modifier = Modifier.weight(0.4f), style = MaterialTheme.typography.bodySmall, color = CoolSlate, textAlign = TextAlign.End)
                                
                                // Hero stat wickets: bold crimson text
                                Text(
                                    text = "${bowl.wickets}",
                                    modifier = Modifier.weight(0.4f),
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Black
                                    ),
                                    color = WicketCrimson,
                                    textAlign = TextAlign.End
                                )
                                
                                // Economy: dynamic elite stat highlighting under 6.5
                                Text(
                                    text = String.format(java.util.Locale.US, "%.2f", bowl.economy),
                                    modifier = Modifier.weight(0.6f),
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = if (bowl.economy <= 6.5) StadiumGreen else CoolSlate,
                                    textAlign = TextAlign.End
                                )
                            }
                        }
                    }
                }
            }

            // --- 4.5 Partnerships Card ---
            val currentActiveInnings = selectedInningsTab == activeMatch.currentInnings && activeMatch.status != "COMPLETED"
            val livePartnershipList = activeSummary.partnerships.toMutableList()

            if (currentActiveInnings) {
                val striker = activeMatch.strikerName ?: ""
                val nonStriker = activeMatch.nonStrikerName ?: ""
                if (striker.isNotEmpty() && nonStriker.isNotEmpty() &&
                    striker != "Batsman Out" && striker != "Next Batter" &&
                    nonStriker != "Batsman Out" && nonStriker != "Next Batter"
                ) {
                    val hasCurrentActive = livePartnershipList.any { p ->
                        (p.batsman1 == striker && p.batsman2 == nonStriker) ||
                        (p.batsman1 == nonStriker && p.batsman2 == striker)
                    }
                    if (!hasCurrentActive) {
                        livePartnershipList.add(Partnership(striker, nonStriker, 0, 0))
                    }
                }
            }

            val eligiblePartnerships = livePartnershipList.filter { p ->
                val b1 = p.batsman1
                val b2 = p.batsman2
                b1.isNotEmpty() && b2.isNotEmpty() && 
                b1 != "Batsman Out" && b2 != "Batsman Out" &&
                b1 != "Next Batter" && b2 != "Next Batter" &&
                b1 != "Next Bowler" && b2 != "Next Bowler"
            }

            if (eligiblePartnerships.isNotEmpty()) {
                Spacer(modifier = Modifier.height(20.dp))
                GlassmorphicCard(
                    modifier = Modifier.fillMaxWidth().testTag("scorecard_partnership_card"),
                    borderColor = GlassBorder,
                    backgroundColor = DarkBgSurface.copy(alpha = 0.40f)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "PARTNERSHIPS",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.sp
                            ),
                            color = CoolSlate,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                            textAlign = TextAlign.Center
                        )

                        eligiblePartnerships.forEachIndexed { index, p ->
                            val b1Stats = activeSummary.batsmanStats.find { it.name == p.batsman1 }
                            val b2Stats = activeSummary.batsmanStats.find { it.name == p.batsman2 }
                            val b1Runs = b1Stats?.runs ?: 0
                            val b1Balls = b1Stats?.balls ?: 0
                            val b2Runs = b2Stats?.runs ?: 0
                            val b2Balls = b2Stats?.balls ?: 0

                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Left: Batter 1
                                Row(
                                    modifier = Modifier.weight(1.2f),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Start
                                ) {
                                    Text(
                                        text = p.batsman1,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = CleanWhite,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "$b1Runs($b1Balls)",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace),
                                        color = CoolSlate
                                    )
                                }

                                // Center: Partnership
                                Box(
                                    modifier = Modifier.weight(1.0f),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${p.runs}(${p.balls})",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace),
                                        color = GoldAccent
                                    )
                                }

                                // Right: Batter 2
                                Row(
                                    modifier = Modifier.weight(1.2f),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    Text(
                                        text = "$b2Runs($b2Balls)",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace),
                                        color = CoolSlate
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = p.batsman2,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = CleanWhite,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // --- 6. Fall of Wickets Card ---
            GlassmorphicCard(
                modifier = Modifier.fillMaxWidth(),
                borderColor = GlassBorder,
                backgroundColor = DarkBgSurface.copy(alpha = 0.40f)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "FALL OF WICKETS",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp
                        ),
                        color = CoolSlate,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        textAlign = TextAlign.Center
                    )
                    if (activeSummary.fallOfWickets.isEmpty()) {
                        Text(
                            text = "No wickets fallen in this innings.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = DarkTextMuted,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp)
                        )
                    } else {
                        // Badge-Driven Timeline FOW List
                        activeSummary.fallOfWickets.forEach { fow ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1.2f)) {
                                    // Circular Crimson Badge
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(WicketCrimson)
                                            .border(1.dp, WicketCrimson.copy(alpha = 0.3f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "${fow.wicketNumber}",
                                            color = CleanWhite,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Black
                                            ),
                                            fontSize = 11.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = fow.playerOut,
                                        color = CleanWhite,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.SemiBold
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.End,
                                    modifier = Modifier.weight(0.8f)
                                ) {
                                    Text(
                                        text = "${fow.teamScoreAtWicket} runs",
                                        color = CoolSlate,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(DarkBgSurface)
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "${fow.oversAtWicket} ov",
                                            color = MutedGrey,
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold
                                            ),
                                            fontSize = 10.sp
                                        )
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
}

// InningsTabButton Component for Dynamic Tabbed Navigation
@Composable
fun InningsTabButton(
    active: Boolean,
    enabled: Boolean,
    label: String,
    teamAbbreviation: String,
    color: Color,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .alpha(if (enabled) 1f else 0.5f)
            .height(52.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (active) color.copy(alpha = 0.15f) else DarkBgSurface)
            .border(
                BorderStroke(
                    width = if (active) 1.5.dp else 1.0.dp,
                    color = if (active) color else GlassBorder
                ),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(enabled = enabled) { onClick() }
            .testTag(testTag)
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp
                ),
                color = if (active) color else MutedGrey,
                fontSize = 11.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = teamAbbreviation,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                ),
                color = if (active) CleanWhite else DarkTextMuted,
                fontSize = 9.sp
            )
        }
    }
}

// Private helper to retrieve beautiful wicket messages
private fun getWicketStatusMsg(bat: BatsmanStats): String {
    return if (bat.dismissed) {
        val typeStr = when (bat.wicketType ?: WicketType.BOWLED) {
            WicketType.BOWLED -> "b. ${bat.bowlerWhoDismissed ?: "Bowler"}"
            WicketType.CAUGHT -> {
                val fielder = bat.fielderWhoDismissed
                val bowler = bat.bowlerWhoDismissed ?: "Bowler"
                if (!fielder.isNullOrBlank()) {
                    "c. $fielder b. $bowler"
                } else {
                    "c. Fielder b. $bowler"
                }
            }
            WicketType.LBW -> "lbw b. ${bat.bowlerWhoDismissed ?: "Bowler"}"
            WicketType.RUN_OUT -> {
                val fielder = bat.fielderWhoDismissed
                if (!fielder.isNullOrBlank()) {
                    "run out ($fielder)"
                } else {
                    "run out"
                }
            }
            WicketType.STUMPED -> {
                val fielder = bat.fielderWhoDismissed
                val bowler = bat.bowlerWhoDismissed ?: "Bowler"
                if (!fielder.isNullOrBlank()) {
                    "st. $fielder b. $bowler"
                } else {
                    "st. b. $bowler"
                }
            }
            WicketType.HIT_WICKET -> "hit wicket b. ${bat.bowlerWhoDismissed ?: "Bowler"}"
            WicketType.OTHER -> "dismissed"
            WicketType.RETIRED_HURT -> "retired hurt"
        }
        typeStr
    } else {
        "not out"
    }
}
