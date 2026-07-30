package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.roundToInt
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalDensity
import kotlinx.coroutines.launch
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.ImeAction
import com.example.model.MatchFormat
import com.example.model.Team
import com.example.data.MatchEntity
import com.example.ui.components.GlassmorphicCard
import com.example.ui.components.GlowBorderGlassmorphicCard
import com.example.ui.theme.*
import com.example.viewmodel.AppScreen
import com.example.viewmodel.MatchViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DashboardScreen(
    viewModel: MatchViewModel,
    modifier: Modifier = Modifier
) {
    val activeTabByViewModel by viewModel.selectedDashboardTab.collectAsState()
    val activeTab = activeTabByViewModel

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBgMain),
        containerColor = DarkBgMain,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                    .background(DarkBgSurface.copy(alpha = 1.0f))
                    .border(0.5.dp, GlassBorder, RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Tab 0
                val tab0Selected = activeTab == 0
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable { viewModel.selectedDashboardTab.value = 0 }
                        .testTag("nav_tab_setup"),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (tab0Selected) Color(0x1F00FF87) else Color.Transparent)
                                .padding(horizontal = 14.dp, vertical = 2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "New Match",
                                tint = if (tab0Selected) StadiumGreen else MutedGrey,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "New Match",
                            color = if (tab0Selected) StadiumGreen else MutedGrey,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                // Tab 1
                val tab1Selected = activeTab == 1
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable { viewModel.selectedDashboardTab.value = 1 }
                        .testTag("nav_tab_teams"),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (tab1Selected) Color(0x1F00E5FF) else Color.Transparent)
                                .padding(horizontal = 14.dp, vertical = 2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Teams",
                                tint = if (tab1Selected) InfoTeal else MutedGrey,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Teams",
                            color = if (tab1Selected) InfoTeal else MutedGrey,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                // Tab 2
                val tab2Selected = activeTab == 2
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable { viewModel.selectedDashboardTab.value = 2 }
                        .testTag("nav_tab_history"),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (tab2Selected) Color(0x1FFFC107) else Color.Transparent)
                                .padding(horizontal = 14.dp, vertical = 2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.List,
                                contentDescription = "History",
                                tint = if (tab2Selected) GoldAccent else MutedGrey,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "History",
                            color = if (tab2Selected) GoldAccent else MutedGrey,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(
                    top = innerPadding.calculateTopPadding(),
                    bottom = 0.dp
                )
                .fillMaxSize()
        ) {
            when (activeTab) {
                0 -> QuickSetupTab(viewModel = viewModel)
                1 -> TeamsTab(viewModel = viewModel)
                2 -> HistoryTab(viewModel = viewModel)
            }
        }
    }
}

// =====================================
//      1. QUICK SETUP PANEL TAB
// =====================================
@Composable
fun QuickSetupTab(viewModel: MatchViewModel) {
    val teamA by viewModel.setupTeamA.collectAsState()
    val teamB by viewModel.setupTeamB.collectAsState()
    val overs by viewModel.setupOvers.collectAsState()
    val format by viewModel.setupFormat.collectAsState()
    val tossWonHost by viewModel.setupTossWonByHost.collectAsState()
    val optedToBat by viewModel.setupOptedToBat.collectAsState()
    val teamAPlayersList by viewModel.setupTeamAPlayers.collectAsState()
    val teamBPlayersList by viewModel.setupTeamBPlayers.collectAsState()
    val teams by viewModel.teamsList.collectAsState()

    var showTeamADropdown by remember { mutableStateOf(false) }
    var showTeamBDropdown by remember { mutableStateOf(false) }
    var showRosterInfoDialog by remember { mutableStateOf(false) }

    val filteredTeamsA = remember(teams, teamA) {
        if (teamA.trim().isEmpty()) {
            teams
        } else {
            teams.filter { it.name.contains(teamA, ignoreCase = true) }
        }
    }

    val filteredTeamsB = remember(teams, teamB) {
        if (teamB.trim().isEmpty()) {
            teams
        } else {
            teams.filter { it.name.contains(teamB, ignoreCase = true) }
        }
    }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        // Sticky Header: Static container
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 16.dp)
        ) {
            // Large Editorial Header with Cricbuzz Brand Accent
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(StadiumGreen)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "PRO",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        color = DarkBgMain
                    )
                }
                Text(
                    text = "SCOREX - CRICKET SCORER",
                    style = MaterialTheme.typography.titleSmall,
                    color = StadiumGreen,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 3.sp
                )
            }
            Text(
                text = "MATCH CENTER",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Black,
                color = CleanWhite
            )
            Text(
                text = "EASY SCORING, REAL-TIME STATS, & LIVE INSIGHTS",
                style = MaterialTheme.typography.labelLarge,
                color = MutedGrey
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Scrollable content
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(scrollState)
                .padding(start = 16.dp, end = 16.dp, bottom = 80.dp)
        ) {

        // Teams Setup Card
        GlassmorphicCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "TEAM ROSTERS SELECTOR",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = CleanWhite,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                    IconButton(
                        onClick = { showRosterInfoDialog = true },
                        modifier = Modifier
                            .testTag("team_rosters_info_btn")
                            .size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Info",
                            tint = CoolSlate,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Host team
                Column {
                    Text(
                        text = "HOST TEAM",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MutedGrey
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = teamA,
                        onValueChange = { newValue ->
                            viewModel.setupTeamA.value = newValue
                            showTeamADropdown = newValue.isNotEmpty()
                            if (newValue.isEmpty()) {
                                viewModel.setupTeamAPlayers.value = ""
                            }
                        },
                        placeholder = { Text("Select or Enter Host Team", color = MutedGrey, fontSize = 14.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = CleanWhite,
                            unfocusedTextColor = CoolSlate,
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        ),
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Host Team Icon",
                                tint = StadiumGreen,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        trailingIcon = {
                            IconButton(onClick = { showTeamADropdown = !showTeamADropdown }) {
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Select saved team",
                                    tint = StadiumGreen
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("host_team_input")
                            .background(Color(0xFF0B1420), RoundedCornerShape(10.dp))
                            .border(2.dp, StadiumGreen, RoundedCornerShape(10.dp)),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )
                    
                    if (showTeamADropdown && filteredTeamsA.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Column(
                             modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 200.dp)
                                .verticalScroll(rememberScrollState())
                                .background(DarkBgSurface, RoundedCornerShape(8.dp))
                                .border(1.dp, GlassBorder, RoundedCornerShape(8.dp))
                                .padding(vertical = 4.dp)
                        ) {
                            Text(
                                text = "MATCHING SAVED TEAMS (Tap to select):",
                                style = MaterialTheme.typography.bodySmall,
                                color = GoldAccent,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                fontWeight = FontWeight.Bold
                            )
                            filteredTeamsA.forEach { team ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.setupTeamA.value = team.name.uppercase()
                                            viewModel.setupTeamAPlayers.value = team.roster.joinToString(", ")
                                            showTeamADropdown = false
                                        }
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        tint = StadiumGreen,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = team.name.uppercase(),
                                        color = CleanWhite,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = teamAPlayersList,
                        onValueChange = { viewModel.setupTeamAPlayers.value = it },
                        placeholder = { Text("Choose team to view players", color = MutedGrey, fontSize = 14.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = CleanWhite,
                            unfocusedTextColor = CoolSlate,
                            focusedBorderColor = GlassBorder,
                            unfocusedBorderColor = GlassBorder,
                            focusedContainerColor = Color(0xFF0B1420).copy(alpha = 0.5f),
                            unfocusedContainerColor = Color(0xFF0B1420).copy(alpha = 0.3f)
                        ),
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Groups,
                                contentDescription = "Players Icon",
                                tint = MutedGrey,
                                modifier = Modifier.size(20.dp).padding(start = 4.dp)
                            )
                        },
                        modifier = Modifier.fillMaxWidth().testTag("host_team_players_input"),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = false,
                        maxLines = 3
                    )
                }

                // Visitor team
                Column {
                    Text(
                        text = "VISITOR TEAM",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MutedGrey
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = teamB,
                        onValueChange = { newValue ->
                            viewModel.setupTeamB.value = newValue
                            showTeamBDropdown = newValue.isNotEmpty()
                            if (newValue.isEmpty()) {
                                viewModel.setupTeamBPlayers.value = ""
                            }
                        },
                        placeholder = { Text("Select or Enter Visitor Team", color = MutedGrey, fontSize = 14.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = CleanWhite,
                            unfocusedTextColor = CoolSlate,
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        ),
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Visitor Team Icon",
                                tint = InfoTeal,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        trailingIcon = {
                            IconButton(onClick = { showTeamBDropdown = !showTeamBDropdown }) {
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Select saved team",
                                    tint = InfoTeal
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("visitor_team_input")
                            .background(Color(0xFF0B1420), RoundedCornerShape(10.dp))
                            .border(2.dp, InfoTeal, RoundedCornerShape(10.dp)),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )

                    if (showTeamBDropdown && filteredTeamsB.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 200.dp)
                                .verticalScroll(rememberScrollState())
                                .background(DarkBgSurface, RoundedCornerShape(8.dp))
                                .border(1.dp, GlassBorder, RoundedCornerShape(8.dp))
                                .padding(vertical = 4.dp)
                        ) {
                            Text(
                                text = "MATCHING SAVED TEAMS (Tap to select):",
                                style = MaterialTheme.typography.bodySmall,
                                color = GoldAccent,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                fontWeight = FontWeight.Bold
                            )
                            filteredTeamsB.forEach { team ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.setupTeamB.value = team.name.uppercase()
                                            viewModel.setupTeamBPlayers.value = team.roster.joinToString(", ")
                                            showTeamBDropdown = false
                                        }
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        tint = StadiumGreen,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = team.name.uppercase(),
                                        color = CleanWhite,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = teamBPlayersList,
                        onValueChange = { viewModel.setupTeamBPlayers.value = it },
                        placeholder = { Text("Choose team to view players", color = MutedGrey, fontSize = 14.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = CleanWhite,
                            unfocusedTextColor = CoolSlate,
                            focusedBorderColor = GlassBorder,
                            unfocusedBorderColor = GlassBorder,
                            focusedContainerColor = Color(0xFF0B1420).copy(alpha = 0.5f),
                            unfocusedContainerColor = Color(0xFF0B1420).copy(alpha = 0.3f)
                        ),
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Groups,
                                contentDescription = "Players Icon",
                                tint = MutedGrey,
                                modifier = Modifier.size(20.dp).padding(start = 4.dp)
                            )
                        },
                        modifier = Modifier.fillMaxWidth().testTag("visitor_team_players_input"),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = false,
                        maxLines = 3
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Toss & Opt Decision Card
        GlassmorphicCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    text = "TOSS & MATCH INNINGS CORES",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = CleanWhite,
                    modifier = Modifier.padding(bottom = 2.dp)
                )

                // Toss Won By selection
                Column {
                    Text("TOSS WON BY?", style = MaterialTheme.typography.labelLarge, color = MutedGrey)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Min),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Option Host
                        val selHost = tossWonHost
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (selHost) InfoTeal else Color(0xFF111C2B),
                            border = if (selHost) null else BorderStroke(1.dp, Color(0xFF1D2C3F)),
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clickable { viewModel.setupTossWonByHost.value = true }
                                .testTag("toss_won_host")
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (teamA.isNotEmpty()) teamA.uppercase() else "HOST",
                                    color = if (selHost) DarkBgMain else Color(0xFF7E8F9F),
                                    fontWeight = FontWeight.ExtraBold,
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp)
                                )
                            }
                        }

                        // Option Visitor
                        val selVisitor = !tossWonHost
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (selVisitor) InfoTeal else Color(0xFF111C2B),
                            border = if (selVisitor) null else BorderStroke(1.dp, Color(0xFF1D2C3F)),
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clickable { viewModel.setupTossWonByHost.value = false }
                                .testTag("toss_won_visitor")
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (teamB.isNotEmpty()) teamB.uppercase() else "VISITOR",
                                    color = if (selVisitor) DarkBgMain else Color(0xFF7E8F9F),
                                    fontWeight = FontWeight.ExtraBold,
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp)
                                )
                            }
                        }
                    }
                }

                // Opted is Bat/Bowl selection
                Column {
                    Text("OPTED TO?", style = MaterialTheme.typography.labelLarge, color = MutedGrey)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Bat
                        val optBat = optedToBat
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (optBat) StadiumGreen else Color(0xFF111C2B),
                            border = if (optBat) null else BorderStroke(1.dp, Color(0xFF1D2C3F)),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { viewModel.setupOptedToBat.value = true }
                                .testTag("opt_bat")
                        ) {
                            Text(
                                text = "BAT",
                                color = if (optBat) DarkBgMain else Color(0xFF7E8F9F),
                                fontWeight = FontWeight.ExtraBold,
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 10.dp)
                            )
                        }

                        // Bowl
                        val optBowl = !optedToBat
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (optBowl) StadiumGreen else Color(0xFF111C2B),
                            border = if (optBowl) null else BorderStroke(1.dp, Color(0xFF1D2C3F)),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { viewModel.setupOptedToBat.value = false }
                                .testTag("opt_bowl")
                        ) {
                            Text(
                                text = "BOWL",
                                color = if (optBowl) DarkBgMain else Color(0xFF7E8F9F),
                                fontWeight = FontWeight.ExtraBold,
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 10.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // New upgraded card design for MATCH FORMAT & OVERS LIMIT as per the uploaded mockup
        GlassmorphicCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "MATCH FORMAT & OVERS LIMIT",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = CleanWhite,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                val itemShape = RoundedCornerShape(14.dp)
                val totalFormats = listOf(MatchFormat.T20, MatchFormat.ODI, MatchFormat.TEST, MatchFormat.CUSTOM)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    totalFormats.forEach { fmt ->
                        val sel = format == fmt
                        val bg = if (sel) GoldAccent else Color(0xFF111C2B)
                        val borderStroke = if (sel) null else BorderStroke(1.dp, Color(0xFF1D2C3F))
                        val textClr = if (sel) DarkBgMain else Color(0xFF7E8F9F)

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(itemShape)
                                .background(bg)
                                .then(
                                    if (borderStroke != null) {
                                        Modifier.border(borderStroke, itemShape)
                                    } else {
                                        Modifier
                                    }
                                )
                                .clickable { viewModel.setupFormat.value = fmt },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = fmt.name,
                                fontWeight = FontWeight.ExtraBold,
                                color = textClr,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                                maxLines = 1
                            )
                        }
                    }
                }

                if (format == MatchFormat.CUSTOM) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("CUSTOM OVERS MAXIMUM", style = MaterialTheme.typography.bodyMedium, color = MutedGrey)
                        Text("$overs OVERS", style = MaterialTheme.typography.titleMedium, color = StadiumGreen, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    CustomModernOversSlider(
                        value = overs.coerceIn(1, 50),
                        onValueChange = { viewModel.setupOvers.value = it }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Start Match CTA Button (interactive slide-to-start as per mockup)
        SlideToStartButton(
            onStartMatch = { viewModel.startNewMatch() },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

    if (showRosterInfoDialog) {
        Dialog(
            onDismissRequest = { showRosterInfoDialog = false },
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
                        .fillMaxWidth(0.9f)
                        .widthIn(max = 440.dp)
                        .padding(16.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = DarkBgSurface,
                    border = BorderStroke(1.dp, GlassBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "TEAM ROSTERS SELECTOR INFO",
                                color = InfoTeal,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Black,
                                textAlign = TextAlign.Center
                            )
                        }

                        Text(
                            text = "You can quickly create teams here without going to the Teams screen.\n\nEnter the team name and add player names separated by commas. Once you start the match, the teams will be created automatically and saved for future use.",
                            color = CleanWhite,
                            style = MaterialTheme.typography.bodyMedium,
                            lineHeight = 22.sp
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Button(
                                onClick = { showRosterInfoDialog = false },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = InfoTeal,
                                    contentColor = DarkBgMain
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("GOT IT", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

// =====================================
//      2. EASY TEAMS TAB PANEL
// =====================================
@Composable
fun TeamsTab(viewModel: MatchViewModel) {
    val teams by viewModel.teamsList.collectAsState()
    val matches by viewModel.allMatches.collectAsState()
    var showAddTeamDialog by remember { mutableStateOf(false) }
    var selectedTeamForRosterEdit by remember { mutableStateOf<Team?>(null) }
    var selectedPlayerForStats by remember { mutableStateOf<String?>(null) }

    val computedTeams = remember(teams, matches) {
        teams.map { team ->
            val targetTeamName = team.name.trim().lowercase()
            var played = 0
            var won = 0
            var lost = 0
            
            for (match in matches) {
                if (match.status == "COMPLETED") {
                    val matchTeamA = match.teamA.trim().lowercase()
                    val matchTeamB = match.teamB.trim().lowercase()
                    val hasPlayed = (targetTeamName == matchTeamA || targetTeamName == matchTeamB)
                    if (hasPlayed) {
                        played++
                        val winLower = match.winner?.lowercase() ?: ""
                        if (!winLower.contains("tied") && !winLower.contains("tie")) {
                            val winTeam = if (winLower.startsWith(matchTeamA)) {
                                matchTeamA
                            } else if (winLower.startsWith(matchTeamB)) {
                                matchTeamB
                            } else if (winLower.contains(matchTeamA)) {
                                matchTeamA
                            } else if (winLower.contains(matchTeamB)) {
                                matchTeamB
                            } else {
                                null
                            }
                            
                            if (winTeam == targetTeamName) {
                                won++
                            } else if (winTeam != null) {
                                lost++
                            }
                        }
                    }
                }
            }
            team.copy(
                matchesPlayed = played,
                matchesWon = won,
                matchesLost = lost
            )
        }
    }

    if (selectedPlayerForStats != null) {
        androidx.activity.compose.BackHandler(enabled = true) {
            selectedPlayerForStats = null
        }
        PlayerStatsView(
            playerName = selectedPlayerForStats!!,
            matches = matches,
            onBack = { selectedPlayerForStats = null }
        )
    } else {
        Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(start = 16.dp, end = 16.dp, bottom = 0.dp, top = 16.dp)) {

            // Tab header
            Text(
                text = "TEAM DIRECTORY",
                style = MaterialTheme.typography.titleSmall,
                color = InfoTeal,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 3.sp
            )
            Text(
                text = "SAVED CLUBS",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Black,
                color = CleanWhite
            )
            Text(
                text = "SQUAD ROSTERS & HISTORICAL PLAYER METRICS",
                style = MaterialTheme.typography.labelLarge,
                color = MutedGrey
            )

            Spacer(modifier = Modifier.height(18.dp))

            if (computedTeams.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Groups,
                            contentDescription = "No teams placeholder",
                            tint = DarkTextMuted,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No custom teams configured. Click the \"+\" button to start your club.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = DarkTextMuted,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 32.dp)
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(items = computedTeams, key = { it.id }) { team ->
                        TeamItemCard(
                            team = team,
                            onClick = { selectedTeamForRosterEdit = team },
                            onDelete = { viewModel.deleteTeam(team.id) }
                        )
                    }
                }
            }
        }

        // Add Team FAB
        FloatingActionButton(
            onClick = { showAddTeamDialog = true },
            containerColor = InfoTeal,
            contentColor = DarkBgMain,
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 74.dp, end = 16.dp)
                .testTag("add_team_fab")
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Team")
        }
    }

    // A. Dialog to Add Team
    if (showAddTeamDialog) {
        var teamNameInput by remember { mutableStateOf("") }
        Dialog(
            onDismissRequest = { showAddTeamDialog = false },
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
                            text = "ADD NEW TEAM",
                            color = InfoTeal,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("ENTER TEAM NAME:", color = MutedGrey, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            OutlinedTextField(
                                value = teamNameInput,
                                onValueChange = { teamNameInput = it },
                                modifier = Modifier.fillMaxWidth().testTag("add_team_name_input"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = CleanWhite,
                                    unfocusedTextColor = CoolSlate,
                                    focusedBorderColor = InfoTeal,
                                    unfocusedBorderColor = GlassBorder,
                                    focusedContainerColor = OuterSpace
                                ),
                                singleLine = true
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = { showAddTeamDialog = false }) {
                                Text("CANCEL", color = CoolSlate)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    if (teamNameInput.trim().isNotEmpty()) {
                                        viewModel.addTeam(teamNameInput.trim(), emptyList())
                                        showAddTeamDialog = false
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = InfoTeal, contentColor = DarkBgMain)
                            ) {
                                Text("ADD", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    // B. Dialog to Edit Players Roster (11 Players)
    if (selectedTeamForRosterEdit != null) {
        val editingTeam = selectedTeamForRosterEdit!!
        val context = androidx.compose.ui.platform.LocalContext.current
        val playerListState = remember(editingTeam) { 
            mutableStateListOf<String>().apply {
                addAll(editingTeam.roster)
            }
        }
        var teamNameState by remember(editingTeam) { mutableStateOf(editingTeam.name) }

        Dialog(
            onDismissRequest = { selectedTeamForRosterEdit = null },
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
                        .fillMaxWidth(0.88f)
                        .fillMaxHeight(0.85f)
                        .widthIn(max = 500.dp)
                        .navigationBarsPadding()
                        .imePadding()
                        .padding(vertical = 12.dp),
                    shape = RoundedCornerShape(20.dp),
                    color = DarkBgSurface,
                    border = BorderStroke(1.dp, GlassBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Title Header
                        Text(
                            text = "EDIT SQUAD LINEUP",
                            color = InfoTeal,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )

                        // Team Name Input Field
                        Column(
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "TEAM NAME",
                                color = MutedGrey,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                letterSpacing = 0.5.sp
                            )
                            OutlinedTextField(
                                value = teamNameState,
                                onValueChange = { teamNameState = it },
                                placeholder = { Text("Enter team name...", color = CoolSlate.copy(alpha = 0.5f), fontSize = 13.sp) },
                                modifier = Modifier.fillMaxWidth(),
                                textStyle = MaterialTheme.typography.bodyMedium.copy(color = CleanWhite, fontWeight = FontWeight.SemiBold),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = CleanWhite,
                                    unfocusedTextColor = CoolSlate,
                                    focusedBorderColor = InfoTeal,
                                    unfocusedBorderColor = GlassBorder,
                                    focusedContainerColor = OuterSpace,
                                    unfocusedContainerColor = OuterSpace
                                ),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    capitalization = KeyboardCapitalization.Words,
                                    imeAction = ImeAction.Next
                                )
                            )
                        }

                        // Players Roster Title with Status Count
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "PLAYERS SQUAD",
                                color = MutedGrey,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(InfoTeal.copy(alpha = 0.15f))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = "${playerListState.size} Members",
                                    color = InfoTeal,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Dynamic weight list so it takes maximum available physical height comfortably
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        ) {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(playerListState.size) { index ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        // Player Name Input Box supporting high display quality
                                        OutlinedTextField(
                                            value = playerListState[index],
                                            onValueChange = { playerListState[index] = it },
                                            modifier = Modifier.weight(1f),
                                            textStyle = MaterialTheme.typography.bodyMedium.copy(color = CleanWhite),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedTextColor = CleanWhite,
                                                unfocusedTextColor = CoolSlate,
                                                focusedBorderColor = InfoTeal,
                                                unfocusedBorderColor = GlassBorder,
                                                focusedContainerColor = OuterSpace,
                                                unfocusedContainerColor = OuterSpace
                                            ),
                                            singleLine = true,
                                            keyboardOptions = KeyboardOptions(
                                                capitalization = KeyboardCapitalization.Words,
                                                imeAction = if (index == playerListState.lastIndex) ImeAction.Done else ImeAction.Next
                                            )
                                        )

                                        // Action Button Cluster with reduced size to prioritize the input field width
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            IconButton(
                                                onClick = {
                                                    val pName = playerListState[index].trim()
                                                    if (pName.isNotEmpty()) {
                                                        selectedPlayerForStats = pName
                                                    }
                                                },
                                                modifier = Modifier.size(34.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.TrendingUp,
                                                    contentDescription = "View Player Stats",
                                                    tint = StadiumGreen,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                            IconButton(
                                                onClick = {
                                                    if (playerListState.size > 2) {
                                                        playerListState.removeAt(index)
                                                    } else {
                                                        android.widget.Toast.makeText(
                                                            context,
                                                            "A cricket team needs at least 2 players.",
                                                            android.widget.Toast.LENGTH_SHORT
                                                        ).show()
                                                    }
                                                },
                                                modifier = Modifier.size(34.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Remove Player",
                                                    tint = WicketCrimson,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Add Squad Player Trigger Button
                        if (playerListState.size < 11) {
                            Button(
                                onClick = {
                                    playerListState.add("${teamNameState.ifBlank { "Team" }} Player ${playerListState.size + 1}")
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = InfoTeal.copy(alpha = 0.12f),
                                    contentColor = InfoTeal
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(42.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Add Player",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("ADD SQUAD PLAYER", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Closing & Saving Triggers
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = { selectedTeamForRosterEdit = null },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("CLOSE", color = CoolSlate, fontWeight = FontWeight.SemiBold)
                            }

                            Button(
                                onClick = {
                                    if (teamNameState.trim().isNotEmpty()) {
                                        viewModel.updateTeam(editingTeam.id, teamNameState.trim(), playerListState.toList())
                                        selectedTeamForRosterEdit = null
                                    } else {
                                        android.widget.Toast.makeText(
                                            context,
                                            "Team name cannot be empty.",
                                            android.widget.Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = InfoTeal, contentColor = DarkBgMain),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.height(42.dp)
                            ) {
                                Text("SAVE CHANGES", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
    }
}

@Composable
fun TeamItemCard(
    team: Team,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val bColor = GlassBorder
    val bg = DarkBgGlass
    val shape = RoundedCornerShape(16.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clip(shape)
            .background(bg)
            .border(
                border = BorderStroke(1.dp, bColor),
                shape = shape
            )
            .clickable { onClick() }
            .testTag("team_item_${team.id}")
    ) {
        // Left Accent Strip: Vibrant InfoTeal vertical stripe running down the master far-left edge, flush against the curved boundary
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(6.dp)
                .background(InfoTeal)
                .align(Alignment.CenterStart)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 22.dp, top = 16.dp, end = 16.dp, bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Team Initials Badge Icon (Upgraded with a solid color #00B0FF InfoTeal Theme Accordance)
            val initials = team.name.toAbbreviation()
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(InfoTeal)
                    .border(
                        BorderStroke(
                            1.5.dp,
                            Color.White.copy(alpha = 0.35f) // High contrast clean white border
                        ),
                        RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                // Subtle shine/flare overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.15f),
                                    Color.Transparent
                                )
                            )
                        )
                )
                Text(
                    text = initials,
                    color = CleanWhite,
                    fontWeight = FontWeight.Black,
                    fontSize = 15.sp,
                    letterSpacing = 1.sp,
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = team.name.uppercase(),
                    style = MaterialTheme.typography.titleSmall,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = CleanWhite
                )
                Text(
                    text = "M: ${team.matchesPlayed}  |  W: ${team.matchesWon}  |  L: ${team.matchesLost}",
                    fontSize = 11.sp,
                    color = MutedGrey,
                    fontWeight = FontWeight.Medium
                )
            }

            // Edit icon
            IconButton(onClick = onClick) {
                Icon(Icons.Default.Edit, contentDescription = "EditSquad", tint = InfoTeal)
            }

            // Delete icon
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "DeleteTeam", tint = WicketCrimson.copy(alpha = 0.7f))
            }
        }
    }
}

// =====================================
//      3. RESUME/HISTORY LOGS TAB
// =====================================
@Composable
fun HistoryTab(viewModel: MatchViewModel) {
    val matches by viewModel.allMatches.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(start = 16.dp, end = 16.dp, bottom = 0.dp, top = 16.dp)) {

        // Large Editorial Header
        Text(
            text = "MATCH ARCHIVES",
            style = MaterialTheme.typography.titleSmall,
            color = GoldAccent,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 3.sp
        )
        Text(
            text = "HISTORY LOGS",
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Black,
            color = CleanWhite
        )
        Text(
            text = "RESUME IN-PROGRESS SESSIONS OR VIEW FINAL ARCHIVED SCORECARDS",
            style = MaterialTheme.typography.labelLarge,
            color = MutedGrey
        )

        Spacer(modifier = Modifier.height(18.dp))

        if (matches.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = "History ghost placeholder",
                        tint = DarkTextMuted,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Start your first match to see logs here",
                        style = MaterialTheme.typography.bodyMedium,
                        color = DarkTextMuted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(matches) { match ->
                    HistoryItemCard(
                        match = match,
                        onSelect = { viewModel.selectSavedMatch(match) },
                        onDelete = { viewModel.deleteMatch(match.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun HistoryItemCard(
    match: MatchEntity,
    onSelect: () -> Unit,
    onDelete: () -> Unit
) {
    val isLive = match.status == "LIVE"
    val bColor = if (isLive) StadiumGreen.copy(alpha = 0.4f) else GlassBorder
    val bg = if (isLive) Color(0x1F00FF87) else DarkBgGlass

    val sdf = SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault())
    val dateStr = sdf.format(Date(match.timestamp))

    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clip(shape)
            .background(bg)
            .border(
                border = BorderStroke(1.dp, bColor),
                shape = shape
            )
            .testTag("match_history_item_${match.id}")
    ) {
        // Left Accent Strip: GoldAccent vertical stripe running down the master far-left edge, flush against the curved boundary
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(6.dp)
                .background(GoldAccent)
                .align(Alignment.CenterStart)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 22.dp, top = 16.dp, end = 16.dp, bottom = 16.dp)
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.align(Alignment.TopStart)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = "Calendar",
                            tint = GoldAccent,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = dateStr.uppercase(),
                            fontSize = 11.sp,
                            color = MutedGrey.copy(alpha = 0.85f),
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    val oversSuffix = if (match.selectedOvers == 1) "OVER" else "OVERS"
                    Text(
                        text = "${match.format.uppercase()} MATCH - ${match.selectedOvers} $oversSuffix",
                        fontSize = 14.sp,
                        color = GoldAccent,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.5.sp
                    )
                }

                Surface(
                    color = if (isLive) Color(0x3300FF87) else Color(0x1F94A3B8),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Text(
                        text = if (isLive) "LIVE/IN-PLAY" else "COMPLETED",
                        color = if (isLive) StadiumGreen else CoolSlate,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "${match.teamA.toAbbreviation()} vs ${match.teamB.toAbbreviation()}",
                    style = MaterialTheme.typography.titleMedium,
                    color = CleanWhite,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                if (match.winner != null) {
                    Text(
                        text = match.winner.abbreviateTeams(match.teamA, match.teamB),
                        style = MaterialTheme.typography.bodySmall,
                        color = GoldAccent,
                        fontWeight = FontWeight.Bold
                    )
                } else if (isLive) {
                    val progressionText = when (match.currentInnings) {
                        3 -> "Score progression: Super Over Innings 1 in play"
                        4 -> "Score progression: Super Over Innings 2 in play"
                        else -> "Score progression: Innings ${match.currentInnings} in play"
                    }
                    Text(
                        text = progressionText,
                        style = MaterialTheme.typography.bodySmall,
                        color = StadiumGreen
                    )
                } else {
                    Text(
                        text = "No runs recorded yet",
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedGrey
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // MATCH INSIGHTS button occupying 86% of the row space
                Button(
                    onClick = onSelect,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isLive) StadiumGreen else GoldAccent,
                        contentColor = DarkBgMain
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier
                        .weight(0.86f)
                        .height(36.dp)
                ) {
                    Text(
                        text = if (isLive) "RESUME" else "MATCH INSIGHTS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black
                    )
                }

                // Delete button occupying 14% of the row space
                Box(
                    modifier = Modifier
                        .weight(0.14f)
                        .height(36.dp),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("delete_history_${match.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = WicketCrimson,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CustomModernOversSlider(
    value: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    range: ClosedRange<Int> = 1..50
) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val width = constraints.maxWidth.toFloat()
        if (width > 0) {
            val viewRange = range.endInclusive - range.start
            val normalizedValue = (value - range.start).toFloat() / viewRange.toFloat()
            
            val dragModifier = Modifier
                .pointerInput(range, width) {
                    detectTapGestures { offset ->
                        val rawFraction = (offset.x / width).coerceIn(0f, 1f)
                        val computedValue = range.start + (rawFraction * viewRange).roundToInt()
                        onValueChange(computedValue.coerceIn(range))
                    }
                }
                .pointerInput(range, width) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        val offset = change.position.x
                        val rawFraction = (offset / width).coerceIn(0f, 1f)
                        val computedValue = range.start + (rawFraction * viewRange).roundToInt()
                        onValueChange(computedValue.coerceIn(range))
                    }
                }

            Column(modifier = dragModifier) {
                // Main Slider Area allowing protruding thumb
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(26.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    // 1. The Track (Height = 20.dp, fully clipped so active/inactive stays bounded)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(20.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF0F1822))
                            .border(BorderStroke(1.dp, Color(0xFF1D2C3F)), RoundedCornerShape(10.dp))
                    ) {
                        // Active Track (clipped to its bounds so diagonal stripes don't bleed out into the inactive region)
                        if (normalizedValue > 0f) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(normalizedValue)
                                    .fillMaxHeight()
                                    .clipToBounds()
                            ) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    // Base green
                                    drawRect(color = StadiumGreen)
                                    
                                    // Stripes
                                    val stripeSpacing = 12.dp.toPx()
                                    val strokeWidth = 3.dp.toPx()
                                    val stripeColor = Color(0xFF23BE68) // Slightly brighter sports green
                                    
                                    var x = -size.height
                                    while (x < size.width) {
                                        drawLine(
                                            color = stripeColor,
                                            start = Offset(x, size.height),
                                            end = Offset(x + size.height, 0f),
                                            strokeWidth = strokeWidth
                                        )
                                        x += stripeSpacing
                                    }
                                }
                            }
                        }
                    }
                    
                    // 2. The Thumb (protrudes vertically up and down, height = 26.dp on 20.dp track)
                    if (normalizedValue > 0f) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(3.dp)
                                .graphicsLayer {
                                    translationX = (width * normalizedValue) - 1.5f
                                }
                                .background(Color.White)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Label row: "1" ... "50" under the slider
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${range.start}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF4B5E70),
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                    Text(
                        text = "${range.endInclusive}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF4B5E70),
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

@Composable
fun SlideToStartButton(
    onStartMatch: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val swipeOffset = remember { Animatable(0f) }
    
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(StadiumGreen)
            .border(BorderStroke(1.5.dp, StadiumGreen), RoundedCornerShape(14.dp))
            .testTag("incept_match_btn_tab"),
        contentAlignment = Alignment.CenterStart
    ) {
        val totalWidth = constraints.maxWidth.toFloat()
        val thumbWidth = 60.dp
        val thumbWidthPx = with(LocalDensity.current) { thumbWidth.toPx() }
        val maxDragOffset = totalWidth - thumbWidthPx

        // 1. Swiped region (left of handle) is filled with Dark background
        if (swipeOffset.value > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(with(LocalDensity.current) { swipeOffset.value.toDp() })
                    .background(Color(0xFF0F1822))
            )
        }

        // 2. Centered Text "START MATCH" on the remaining green background (it fades as swiped)
        val textAlpha = (1f - (swipeOffset.value / (maxDragOffset * 0.7f))).coerceIn(0f, 1f)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = textAlpha },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "START MATCH",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                color = DarkBgMain,
                letterSpacing = 1.sp
            )
        }

        // 3. The Slide Handle (Thumb)
        val dragModifier = Modifier
            .pointerInput(maxDragOffset) {
                detectDragGestures(
                    onDragEnd = {
                        coroutineScope.launch {
                            if (swipeOffset.value > maxDragOffset * 0.8f) {
                                // Action complete! Animate to end
                                swipeOffset.animateTo(maxDragOffset, animationSpec = tween(150))
                                onStartMatch()
                                // Reset to start
                                swipeOffset.snapTo(0f)
                            } else {
                                // Snap back
                                swipeOffset.animateTo(0f, animationSpec = spring())
                            }
                        }
                    },
                    onDragCancel = {
                        coroutineScope.launch {
                            swipeOffset.animateTo(0f, animationSpec = spring())
                        }
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        coroutineScope.launch {
                            val targetValue = swipeOffset.value + dragAmount.x
                            swipeOffset.snapTo(targetValue.coerceIn(0f, maxDragOffset))
                        }
                    }
                )
            }

        Box(
            modifier = Modifier
                .offset(x = with(LocalDensity.current) { swipeOffset.value.toDp() })
                .width(thumbWidth)
                .fillMaxHeight()
                .background(Color(0xFF0B141C))
                .then(dragModifier)
        ) {
            // Icon: Chevron right
            Icon(
                imageVector = Icons.Filled.KeyboardArrowRight,
                contentDescription = "Slide Arrow",
                tint = StadiumGreen,
                modifier = Modifier
                    .size(28.dp)
                    .align(Alignment.Center)
            )

            // Right vertical dividing line separating the handle from the rest of the button
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .width(1.5.dp)
                    .background(StadiumGreen)
            )
        }
    }
}
