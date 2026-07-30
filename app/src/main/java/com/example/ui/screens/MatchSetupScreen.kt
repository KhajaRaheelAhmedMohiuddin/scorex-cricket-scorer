package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.MatchFormat
import com.example.ui.components.GlassmorphicCard
import com.example.ui.theme.*
import com.example.viewmodel.AppScreen
import com.example.viewmodel.MatchViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchSetupScreen(
    viewModel: MatchViewModel,
    modifier: Modifier = Modifier
) {
    val teamA by viewModel.setupTeamA.collectAsState()
    val teamB by viewModel.setupTeamB.collectAsState()
    val overs by viewModel.setupOvers.collectAsState()
    val format by viewModel.setupFormat.collectAsState()

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
    ) {
        // Sticky/Constant Header Section
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 16.dp)
        ) {
            // Back Navigation Arrow Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { viewModel.navigateTo(AppScreen.Dashboard) },
                    modifier = Modifier.testTag("setup_back_arrow")
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = CoolSlate
                    )
                }
                Text(
                    text = "BACK TO DASHBOARD",
                    style = MaterialTheme.typography.labelLarge,
                    color = MutedGrey,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Large Editorial Setup Header
            Text(
                text = "SCOREX SETUP/",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Black,
                color = CleanWhite
            )
            Text(
                text = "DEFINE TEAMS, SQUADS, LIMITS & CHASE PARAMETERS",
                style = MaterialTheme.typography.labelLarge,
                color = StadiumGreen,
                letterSpacing = 1.2.sp
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Scrollable Body Content
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(scrollState)
                .padding(start = 16.dp, end = 16.dp, bottom = 20.dp)
        ) {
        
        // Team Config Segment Table
        Text(
            text = "TEAM ROSTER IDENTIFIERS",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = CoolSlate,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        GlassmorphicCard(
            modifier = Modifier.fillMaxWidth(),
            borderColor = GlassBorder
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Team A Field
                Column {
                    Text(
                        text = "TEAM A (BATS FIRST COVERS ROSTER)",
                        style = MaterialTheme.typography.labelLarge,
                        color = MutedGrey,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    OutlinedTextField(
                        value = teamA,
                        onValueChange = { viewModel.setupTeamA.value = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("team_a_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = StadiumGreen,
                            unfocusedBorderColor = GlassBorder,
                            focusedContainerColor = Color(0x12FFFFFF),
                            unfocusedContainerColor = Color(0x0AFFFFFF),
                            focusedLabelColor = StadiumGreen,
                            unfocusedLabelColor = MutedGrey,
                            focusedTextColor = CleanWhite,
                            unfocusedTextColor = CoolSlate
                        ),
                        placeholder = { Text("Host Team", color = DarkTextMuted) },
                        singleLine = true
                    )
                }

                // Team B Field
                Column {
                    Text(
                        text = "TEAM B (FIELD/CHASE COVERS ROSTER)",
                        style = MaterialTheme.typography.labelLarge,
                        color = MutedGrey,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    OutlinedTextField(
                        value = teamB,
                        onValueChange = { viewModel.setupTeamB.value = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("team_b_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = StadiumGreen,
                            unfocusedBorderColor = GlassBorder,
                            focusedContainerColor = Color(0x12FFFFFF),
                            unfocusedContainerColor = Color(0x0AFFFFFF),
                            focusedLabelColor = StadiumGreen,
                            unfocusedLabelColor = MutedGrey,
                            focusedTextColor = CleanWhite,
                            unfocusedTextColor = CoolSlate
                        ),
                        placeholder = { Text("Visitor Team", color = DarkTextMuted) },
                        singleLine = true
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Match Format Section
        Text(
            text = "OVERS FORMAT SELECTOR",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = CoolSlate,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(MatchFormat.T20, MatchFormat.ODI, MatchFormat.TEST, MatchFormat.CUSTOM).forEach { fmt ->
                val selected = format == fmt
                val contentCol = if (selected) StadiumGreen else MutedGrey
                val bgCol = if (selected) Color(0x1F00FF87) else DarkBgGlass
                val borderCol = if (selected) StadiumGreen else GlassBorder

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp)
                        .background(bgCol, RoundedCornerShape(12.dp))
                        .clickable { viewModel.setupFormat.value = fmt }
                        .testTag("format_opt_${fmt.name}")
                        .then(
                            if (selected) Modifier.border(1.5.dp, borderCol, RoundedCornerShape(12.dp))
                            else Modifier.border(1.dp, borderCol, RoundedCornerShape(12.dp))
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = fmt.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = contentCol,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        maxLines = 1
                    )
                }
            }
        }

        // Custom Slider Input
        AnimatedVisibility(
            visible = format == MatchFormat.CUSTOM,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Column(modifier = Modifier.padding(top = 18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "CUSTOM OVERS TOTAL",
                        style = MaterialTheme.typography.labelLarge,
                        color = MutedGrey
                    )
                    Text(
                        text = "$overs OVERS",
                        style = MaterialTheme.typography.titleMedium,
                        color = StadiumGreen,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
                Slider(
                    value = overs.toFloat(),
                    onValueChange = { viewModel.setupOvers.value = it.toInt() },
                    valueRange = 1f..100f,
                    steps = 99,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("overs_slider"),
                    colors = SliderDefaults.colors(
                        thumbColor = StadiumGreen,
                        activeTrackColor = StadiumGreen,
                        inactiveTrackColor = DarkTextMuted
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(34.dp))

        // Submit Button
        Button(
            onClick = { viewModel.startNewMatch() },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .testTag("incept_match_btn"),
            colors = ButtonDefaults.buttonColors(
                containerColor = StadiumGreen,
                contentColor = DarkBgMain
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "START MATCH INNINGS 1",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
            }
        }
        
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = "Completing setup instantiates local storage data and sets roster lists dynamically. Swipe left to right to check history later.",
            style = MaterialTheme.typography.bodySmall,
            color = DarkTextMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
}
