package com.example.viewmodel

import android.app.Application
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.MatchEntity
import com.example.data.MatchRepository
import com.example.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

sealed class AppScreen {
    object Dashboard : AppScreen()
    object Scoring : AppScreen()
    object Scorecard : AppScreen()
    object Summary : AppScreen()
    object Analysis : AppScreen()
}

sealed class BatterState {
    data class Active(val name: String) : BatterState()
    object Out : BatterState()
    object RetiredHurt : BatterState()
}

class MatchViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: MatchRepository
    val allMatches: StateFlow<List<MatchEntity>>

    private val _currentScreen = MutableStateFlow<AppScreen>(AppScreen.Dashboard)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    val selectedDashboardTab = MutableStateFlow(0)

    private val _activeMatch = MutableStateFlow<MatchEntity?>(null)
    val activeMatch: StateFlow<MatchEntity?> = _activeMatch.asStateFlow()

    // For Undo / Redo tracking
    private var redoStack = ArrayList<Delivery>()

    // Setup input state carriers helper
    val setupTeamA = MutableStateFlow("")
    val setupTeamB = MutableStateFlow("")
    val setupOvers = MutableStateFlow(20)
    val setupFormat = MutableStateFlow(MatchFormat.T20)
    val setupTossWonByHost = MutableStateFlow(true) // true -> Host/TeamA, false -> Visitor/TeamB
    val setupOptedToBat = MutableStateFlow(true) // true -> Bat, false -> Bowl
    val setupTeamAPlayers = MutableStateFlow("")
    val setupTeamBPlayers = MutableStateFlow("")

    // Team management state
    private val prefs = application.getSharedPreferences("cricket_scorer_prefs", android.content.Context.MODE_PRIVATE)
    val teamsList = MutableStateFlow<List<Team>>(emptyList())

    init {
        val database = AppDatabase.getDatabase(application)
        repository = MatchRepository(database.matchDao())
        allMatches = repository.allMatches.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            loadTeams()
        }
    }

    fun loadTeams() {
        if (!prefs.getBoolean("did_clear_static_defaults_v3", false)) {
            prefs.edit { clear(); putBoolean("did_clear_static_defaults_v3", true) }
        }
        val totalTeams = prefs.getInt("teams_count", 0)
        val list = ArrayList<Team>()
        for (i in 0 until totalTeams) {
            val id = prefs.getString("team_${i}_id", "") ?: ""
            val name = prefs.getString("team_${i}_name", "") ?: ""
            val played = prefs.getInt("team_${i}_played", 0)
            val won = prefs.getInt("team_${i}_won", 0)
            val lost = prefs.getInt("team_${i}_lost", 0)
            val rosterStr = prefs.getString("team_${i}_roster", "") ?: ""
            val roster = if (rosterStr.isEmpty()) emptyList() else rosterStr.split(",")
            if (id.isNotEmpty() && name.isNotEmpty()) {
                list.add(Team(id, name, played, won, lost, roster))
            }
        }
        teamsList.value = list
    }

    private fun saveTeamsList(list: List<Team>) {
        prefs.edit {
            putInt("teams_count", list.size)
            list.forEachIndexed { i, team ->
                putString("team_${i}_id", team.id)
                putString("team_${i}_name", team.name)
                putInt("team_${i}_played", team.matchesPlayed)
                putInt("team_${i}_won", team.matchesWon)
                putInt("team_${i}_lost", team.matchesLost)
                putString("team_${i}_roster", team.roster.joinToString(","))
            }
        }
    }

    fun addTeam(name: String, roster: List<String>) {
        val id = name.lowercase().replace(" ", "").trim()
        val current = teamsList.value.toMutableList()
        val idx = current.indexOfFirst { it.id == id }
        val finalRoster = if (roster.isEmpty()) {
            if (idx != -1) {
                current[idx].roster
            } else {
                List(11) { i -> "$name Player ${i + 1}" }
            }
        } else {
            roster.take(11)
        }

        val newTeam = Team(id = id, name = name, roster = finalRoster)
        if (idx != -1) {
            current[idx] = newTeam
        } else {
            current.add(newTeam)
        }
        teamsList.value = current
        saveTeamsList(current)
    }

    fun updateTeam(oldId: String, newName: String, roster: List<String>) {
        val newId = newName.lowercase().replace(" ", "").trim()
        val current = teamsList.value.toMutableList()
        val idx = current.indexOfFirst { it.id == oldId }
        val finalRoster = roster.take(11)

        val updatedTeam = Team(
            id = newId,
            name = newName,
            matchesPlayed = if (idx != -1) current[idx].matchesPlayed else 0,
            matchesWon = if (idx != -1) current[idx].matchesWon else 0,
            matchesLost = if (idx != -1) current[idx].matchesLost else 0,
            roster = finalRoster
        )

        if (idx != -1) {
            current[idx] = updatedTeam
        } else {
            current.add(updatedTeam)
        }
        teamsList.value = current
        saveTeamsList(current)
    }

    fun deleteTeam(teamId: String) {
        val current = teamsList.value.filter { it.id != teamId }
        teamsList.value = current
        saveTeamsList(current)
    }

    fun navigateTo(screen: AppScreen) {
        _currentScreen.value = screen
    }

    fun startNewMatch() {
        val hostName = setupTeamA.value.trim()
        val visitorName = setupTeamB.value.trim()

        if (hostName.isEmpty() || visitorName.isEmpty()) {
            android.widget.Toast.makeText(
                getApplication(),
                "Please enter both Host Team and Visitor Team names to start the match.",
                android.widget.Toast.LENGTH_LONG
            ).show()
            return
        }

        if (hostName.equals(visitorName, ignoreCase = true)) {
            android.widget.Toast.makeText(
                getApplication(),
                "Host and visitor teams must be different.",
                android.widget.Toast.LENGTH_LONG
            ).show()
            return
        }

        val format = setupFormat.value
        val overs = when (format) {
            MatchFormat.T20 -> 20
            MatchFormat.ODI -> 50
            MatchFormat.TEST -> 90
            MatchFormat.CUSTOM -> setupOvers.value
        }

        val teamAId = hostName.lowercase().replace(" ", "").trim()
        val teamBId = visitorName.lowercase().replace(" ", "").trim()

        val teamAPlayersRaw = setupTeamAPlayers.value.split(",").map { it.trim() }.filter { it.isNotEmpty() }.distinct()
        val teamAPlayers = if (teamAPlayersRaw.isNotEmpty()) {
            teamAPlayersRaw.take(11)
        } else {
            teamsList.value.firstOrNull { it.id == teamAId }?.roster ?: List(11) { i -> "$hostName Player ${i + 1}" }
        }

        val teamBPlayersRaw = setupTeamBPlayers.value.split(",").map { it.trim() }.filter { it.isNotEmpty() }.distinct()
        val teamBPlayers = if (teamBPlayersRaw.isNotEmpty()) {
            teamBPlayersRaw.take(11)
        } else {
            teamsList.value.firstOrNull { it.id == teamBId }?.roster ?: List(11) { i -> "$visitorName Player ${i + 1}" }
        }

        if (teamAPlayers.size < 2 || teamBPlayers.size < 2) {
            android.widget.Toast.makeText(
                getApplication(),
                "Each team needs at least two distinct players.",
                android.widget.Toast.LENGTH_LONG
            ).show()
            return
        }

        // Dynamic batting first team based on Toss and Choice rules
        val firstInningsBattingTeam = if (setupTossWonByHost.value) {
            if (setupOptedToBat.value) hostName else visitorName
        } else {
            if (setupOptedToBat.value) visitorName else hostName
        }

        val batRoster = if (firstInningsBattingTeam == hostName) teamAPlayers else teamBPlayers
        val bowlRoster = if (firstInningsBattingTeam == hostName) teamBPlayers else teamAPlayers

        addTeam(hostName, teamAPlayers)
        addTeam(visitorName, teamBPlayers)

        val match = MatchEntity(
            teamA = hostName,
            teamB = visitorName,
            teamAPlayers = teamAPlayers,
            teamBPlayers = teamBPlayers,
            selectedOvers = overs,
            format = format.name,
            status = MatchStatus.LIVE.name,
            currentInnings = 1,
            firstInningsBattingTeam = firstInningsBattingTeam,
            strikerName = batRoster.getOrElse(0) { "Batsman 1" },
            nonStrikerName = batRoster.getOrElse(1) { "Batsman 2" },
            bowlerName = bowlRoster.getOrElse(10) { "Bowler" },
            deliveries = emptyList()
        )

        // Clear setup fields immediately to keep dashboard and setup screens fresh for the next match
        setupTeamA.value = ""
        setupTeamB.value = ""
        setupTeamAPlayers.value = ""
        setupTeamBPlayers.value = ""
        setupOvers.value = 20
        setupFormat.value = MatchFormat.T20
        setupTossWonByHost.value = true
        setupOptedToBat.value = true

        viewModelScope.launch {
            val insertedId = repository.insertMatch(match)
            _activeMatch.value = match.copy(id = insertedId.toInt())
            redoStack.clear()
            _currentScreen.value = AppScreen.Scoring
        }
    }

    fun selectSavedMatch(match: MatchEntity) {
        _activeMatch.value = match
        redoStack.clear()
        if (match.status == MatchStatus.COMPLETED.name) {
            _currentScreen.value = AppScreen.Summary
        } else {
            _currentScreen.value = AppScreen.Scoring
        }
    }

    fun deleteMatch(id: Int) {
        viewModelScope.launch {
            repository.deleteMatchById(id)
            if (_activeMatch.value?.id == id) {
                _activeMatch.value = null
            }
        }
    }

    fun addLiveDelivery(
        runsBat: Int,
        runsExtra: Int,
        extraType: ExtraType,
        wicket: Boolean,
        wicketType: WicketType? = null,
        dismissedPlayer: String? = null,
        fielder: String? = null,
        retiredPlayer: String? = null
    ) {
        val match = _activeMatch.value ?: return
        
        if (match.status == MatchStatus.COMPLETED.name) return

        val summary = getActiveInningsSummary()
        val maxOvers = match.selectedOvers

        val battingTeam = when (match.currentInnings) {
            1 -> match.firstInningsBattingTeam
            2 -> getOpponent(match.firstInningsBattingTeam, match)
            3 -> getOpponent(match.firstInningsBattingTeam, match)
            4 -> match.firstInningsBattingTeam
            else -> match.firstInningsBattingTeam
        }
        val bowlingTeam = getOpponent(battingTeam, match)
        val batRoster = if (battingTeam == match.teamA) match.teamAPlayers else match.teamBPlayers
        val bowlRoster = if (bowlingTeam == match.teamA) match.teamAPlayers else match.teamBPlayers
        val maxWickets = if (match.currentInnings >= 3) 2 else (batRoster.size - 1).coerceAtLeast(1)

        val isSuper1 = match.currentInnings == 3
        val isSuper2 = match.currentInnings == 4

        if (isSuper1) {
            if (summary.legalBalls >= 6 || summary.totalWickets >= 2) {
                return
            }
        } else if (isSuper2) {
            val target = getInnings3Summary().totalRuns + 1
            if (summary.legalBalls >= 6 || summary.totalWickets >= 2 || summary.totalRuns >= target) {
                return
            }
        } else if (match.currentInnings == 1) {
            if (summary.legalBalls >= maxOvers * 6 || summary.totalWickets >= maxWickets) {
                return
            }
        } else {
            val target = getInnings1Summary().totalRuns + 1
            if (summary.legalBalls >= maxOvers * 6 || summary.totalWickets >= maxWickets || summary.totalRuns >= target) {
                return
            }
        }

        val currentLegalBallsCount = summary.legalBalls
        val nextOverIndex = currentLegalBallsCount / 6
        val nextBallNumber = (currentLegalBallsCount % 6) + 1

        val dId = UUID.randomUUID().toString()
        val delivery = Delivery(
            id = dId,
            innings = match.currentInnings,
            overIndex = nextOverIndex,
            ballNumber = if (extraType == ExtraType.WIDE || extraType == ExtraType.NO_BALL) 0 else nextBallNumber,
            striker = match.strikerName,
            nonStriker = match.nonStrikerName,
            bowler = match.bowlerName,
            runsBat = runsBat,
            runsExtra = runsExtra,
            extraType = extraType,
            wicket = wicket,
            wicketType = wicketType,
            dismissedPlayer = if (wicket) (dismissedPlayer ?: match.strikerName) else null,
            fielder = fielder
        )

        val updatedDeliveries = match.deliveries + delivery
        redoStack.clear() // clear redo on new actions

        // 2. Adjust Striker / Non-Striker due to runs (Strike Rotation BEFORE dismissal)
        var updatedStriker = match.strikerName
        var updatedNonStriker = match.nonStrikerName
        val completedRuns = when (extraType) {
            ExtraType.NONE -> runsBat
            ExtraType.WIDE -> (runsExtra - 1).coerceAtLeast(0)
            ExtraType.NO_BALL -> runsBat
            ExtraType.BYE, ExtraType.LEG_BYE -> runsExtra
        }

        if (completedRuns % 2 != 0) {
            // Swap striker on odd completed physical runs
            val temp = updatedStriker
            updatedStriker = updatedNonStriker
            updatedNonStriker = temp
        }

        // Process Batter State Updates (Supports Active, RetiredHurt, and Out states)
        var strikerState: BatterState = if (updatedStriker.isNotEmpty() && updatedStriker != "Batsman Out" && updatedStriker != "Retired Hurt") {
            BatterState.Active(updatedStriker)
        } else if (updatedStriker == "Retired Hurt") {
            BatterState.RetiredHurt
        } else {
            BatterState.Out
        }

        var nonStrikerState: BatterState = if (updatedNonStriker.isNotEmpty() && updatedNonStriker != "Batsman Out" && updatedNonStriker != "Retired Hurt") {
            BatterState.Active(updatedNonStriker)
        } else if (updatedNonStriker == "Retired Hurt") {
            BatterState.RetiredHurt
        } else {
            BatterState.Out
        }

        // Handle Retired Hurt or Dismissal
        if (wicket && wicketType == WicketType.RETIRED_HURT) {
            val targetRetired = retiredPlayer ?: dismissedPlayer ?: match.strikerName
            if (targetRetired == updatedStriker) {
                strikerState = BatterState.RetiredHurt
            } else if (targetRetired == updatedNonStriker) {
                nonStrikerState = BatterState.RetiredHurt
            } else {
                strikerState = BatterState.RetiredHurt
            }
        } else if (wicket) {
            val targetDismissed = if (wicketType == WicketType.RUN_OUT) dismissedPlayer else (dismissedPlayer ?: match.strikerName)
            if (wicketType == WicketType.RUN_OUT) {
                if (targetDismissed != null) {
                    if (targetDismissed == updatedStriker) {
                        strikerState = BatterState.Out
                    } else if (targetDismissed == updatedNonStriker) {
                        nonStrikerState = BatterState.Out
                    } else {
                        // Fallback if dismissedPlayer doesn't match either, but we know runout happened
                        strikerState = BatterState.Out
                    }
                } else {
                    strikerState = BatterState.Out
                }
            } else {
                // For other dismissals, striker is out
                strikerState = BatterState.Out
            }
        }

        updatedStriker = when (val state = strikerState) {
            is BatterState.Active -> state.name
            is BatterState.RetiredHurt -> "Retired Hurt"
            is BatterState.Out -> "Batsman Out"
        }
        updatedNonStriker = when (val state = nonStrikerState) {
            is BatterState.Active -> state.name
            is BatterState.RetiredHurt -> "Retired Hurt"
            is BatterState.Out -> "Batsman Out"
        }

        // Check if over completed (which rotates strike, but is resolved post legal over increment)
        val isLegal = extraType != ExtraType.WIDE && extraType != ExtraType.NO_BALL
        var nextBowler = match.bowlerName

        // Calculate if innings finishes on this ball
        val totalWicketsNow = summary.totalWickets + if (wicket && wicketType != WicketType.RETIRED_HURT) 1 else 0
        val totalLegalBallsNow = summary.legalBalls + if (isLegal) 1 else 0
        val ballTotalRuns = runsBat + runsExtra
        val totalRunsNow = summary.totalRuns + ballTotalRuns

        val isInningsFinishedNow = when {
            isSuper1 -> totalLegalBallsNow >= 6 || totalWicketsNow >= 2
            isSuper2 -> {
                val target = getInnings3Summary().totalRuns + 1
                totalLegalBallsNow >= 6 || totalWicketsNow >= 2 || totalRunsNow >= target
            }
            match.currentInnings == 1 -> {
                totalLegalBallsNow >= maxOvers * 6 || totalWicketsNow >= maxWickets
            }
            else -> { // currentInnings == 2
                val target = getInnings1Summary().totalRuns + 1
                totalLegalBallsNow >= maxOvers * 6 || totalWicketsNow >= maxWickets || totalRunsNow >= target
            }
        }

        if (isLegal && nextBallNumber == 6) {
            // End of over! Swap striker & non-striker
            val temp = updatedStriker
            updatedStriker = updatedNonStriker
            updatedNonStriker = temp
            
            if (!isInningsFinishedNow) {
                // Auto prompt next bowler selection in UI by picking the next bowler from roster
                val currentBowlerIndex = bowlRoster.indexOf(match.bowlerName)
                val nextBowlerIndex = if (currentBowlerIndex != -1) (currentBowlerIndex + 1) % bowlRoster.size else 0
                nextBowler = bowlRoster.getOrElse(nextBowlerIndex) { "Next Bowler" }
            }
        }

        val updatedMatch = match.copy(
            deliveries = updatedDeliveries,
            strikerName = updatedStriker,
            nonStrikerName = updatedNonStriker,
            bowlerName = nextBowler
        )

        _activeMatch.value = updatedMatch
        saveAndPostUpdate(updatedMatch)
        checkMatchDeadLock(updatedMatch)
    }

    fun swapStrikers() {
        val match = _activeMatch.value ?: return
        val updated = match.copy(
            strikerName = match.nonStrikerName,
            nonStrikerName = match.strikerName
        )
        _activeMatch.value = updated
        saveAndPostUpdate(updated)
    }

    fun changeActiveBowler(name: String) {
        val match = _activeMatch.value ?: return
        val updated = match.copy(bowlerName = name)
        _activeMatch.value = updated
        saveAndPostUpdate(updated)
    }

    fun changeActiveStriker(name: String) {
        val match = _activeMatch.value ?: return
        val updated = match.copy(strikerName = name)
        _activeMatch.value = updated
        saveAndPostUpdate(updated)
    }

    fun changeActiveNonStriker(name: String) {
        val match = _activeMatch.value ?: return
        val updated = match.copy(nonStrikerName = name)
        _activeMatch.value = updated
        saveAndPostUpdate(updated)
    }

    fun renamePlayerInActiveMatch(oldName: String, newName: String) {
        val match = _activeMatch.value ?: return
        val nameToSet = newName.trim()
        if (nameToSet.isEmpty() || oldName == nameToSet) return

        val updatedTeamAPlayers = match.teamAPlayers.map { if (it == oldName) nameToSet else it }
        val updatedTeamBPlayers = match.teamBPlayers.map { if (it == oldName) nameToSet else it }

        val updatedStriker = if (match.strikerName == oldName) nameToSet else match.strikerName
        val updatedNonStriker = if (match.nonStrikerName == oldName) nameToSet else match.nonStrikerName
        val updatedBowler = if (match.bowlerName == oldName) nameToSet else match.bowlerName

        val updatedDeliveries = match.deliveries.map { delivery ->
            delivery.copy(
                striker = if (delivery.striker == oldName) nameToSet else delivery.striker,
                nonStriker = if (delivery.nonStriker == oldName) nameToSet else delivery.nonStriker,
                bowler = if (delivery.bowler == oldName) nameToSet else delivery.bowler,
                dismissedPlayer = if (delivery.dismissedPlayer == oldName) nameToSet else delivery.dismissedPlayer
            )
        }

        val updatedMatch = match.copy(
            teamAPlayers = updatedTeamAPlayers,
            teamBPlayers = updatedTeamBPlayers,
            strikerName = updatedStriker,
            nonStrikerName = updatedNonStriker,
            bowlerName = updatedBowler,
            deliveries = updatedDeliveries
        )

        _activeMatch.value = updatedMatch
        saveAndPostUpdate(updatedMatch)
    }

    fun undoLastBall() {
        val match = _activeMatch.value ?: return
        if (match.deliveries.isEmpty()) return

        val lastDelivery = match.deliveries.last()
        // Ensure that back/undo only works respectively to current and respective innings.
        // Once scored, runs are final to an innings.
        if (lastDelivery.innings != match.currentInnings) return

        val remainingDeliveries = match.deliveries.dropLast(1)
        redoStack.add(lastDelivery)

        // The state before lastDelivery was bowled is extremely simple and robust to restore:
        // it is exactly the state recorded in lastDelivery itself!
        val prevStriker = lastDelivery.striker
        val prevNonStriker = lastDelivery.nonStriker
        val prevBowler = lastDelivery.bowler

        val updatedMatch = match.copy(
            deliveries = remainingDeliveries,
            strikerName = prevStriker,
            nonStrikerName = prevNonStriker,
            bowlerName = prevBowler,
            status = MatchStatus.LIVE.name, // allow resume
            winner = null
        )

        _activeMatch.value = updatedMatch
        saveAndPostUpdate(updatedMatch)
    }

    fun redoLastBall() {
        if (redoStack.isEmpty()) return
        val match = _activeMatch.value ?: return
        val nextBall = redoStack.removeAt(redoStack.size - 1)

        // Simply play the ball again, preserving fielder and retired-hurt detail
        addLiveDelivery(
            runsBat = nextBall.runsBat,
            runsExtra = nextBall.runsExtra,
            extraType = nextBall.extraType,
            wicket = nextBall.wicket,
            wicketType = nextBall.wicketType,
            dismissedPlayer = nextBall.dismissedPlayer,
            fielder = nextBall.fielder,
            retiredPlayer = if (nextBall.wicketType == WicketType.RETIRED_HURT) nextBall.dismissedPlayer else null
        )
    }

    // Explicit transition of innings
    fun transitionToSecondInnings() {
        val match = _activeMatch.value ?: return
        if (match.currentInnings != 1) return

        val battingTeam1 = match.firstInningsBattingTeam
        val targetTeam = if (battingTeam1 == match.teamA) match.teamB else match.teamA
        val teamBPlayers = if (targetTeam == match.teamA) match.teamAPlayers else match.teamBPlayers
        val teamAPlayers = if (battingTeam1 == match.teamA) match.teamAPlayers else match.teamBPlayers

        val updatedMatch = match.copy(
            currentInnings = 2,
            strikerName = teamBPlayers.getOrElse(0) { "Batsman B1" },
            nonStrikerName = teamBPlayers.getOrElse(1) { "Batsman B2" },
            bowlerName = teamAPlayers.getOrElse(10) { "Bowler A1" }
        )
        _activeMatch.value = updatedMatch
        saveAndPostUpdate(updatedMatch)
    }

    // Checking if Innings or Match has naturally Completed
    private fun checkMatchDeadLock(match: MatchEntity) {
        val firstInningsRoster = if (match.firstInningsBattingTeam == match.teamA) match.teamAPlayers else match.teamBPlayers
        val maxWicketsFirst = (firstInningsRoster.size - 1).coerceAtLeast(1)

        val summaryFirst = CricketStatCalculator.calculateInningsSummary(
            1, match.firstInningsBattingTeam, getOpponent(match.firstInningsBattingTeam, match), match.deliveries,
            firstInningsRoster,
            if (match.firstInningsBattingTeam == match.teamA) match.teamBPlayers else match.teamAPlayers
        )

        // Check if first innings is completed (maxWicketsFirst wickets OR overs limit reached)
        if (match.currentInnings == 1) {
            val maxOvers = match.selectedOvers
            if (summaryFirst.totalWickets >= maxWicketsFirst || summaryFirst.legalBalls >= maxOvers * 6) {
                // Auto trigger Innings Break or wait for manual. Let's do a state update!
                // We don't force a popup, we give user option to transition, but let's notify of end.
            }
        } else if (match.currentInnings == 2) {
            // Innings 2 has started! Check check
            val chaserTeam = getOpponent(match.firstInningsBattingTeam, match)
            val defendTeam = match.firstInningsBattingTeam
            val chaserRoster = if (chaserTeam == match.teamA) match.teamAPlayers else match.teamBPlayers
            val defendRoster = if (defendTeam == match.teamA) match.teamAPlayers else match.teamBPlayers
            val maxWicketsSecond = (chaserRoster.size - 1).coerceAtLeast(1)

            val summarySecond = CricketStatCalculator.calculateInningsSummary(
                2, chaserTeam, defendTeam, match.deliveries, chaserRoster, defendRoster
            )

            val target = summaryFirst.totalRuns + 1
            val maxOvers = match.selectedOvers

            if (summarySecond.totalRuns >= target) {
                // Second innings chased successfully! Chaser wins!
                val wicketsLeft = maxWicketsSecond - summarySecond.totalWickets
                val winMsg = "$chaserTeam won by $wicketsLeft wickets!"
                finalizeMatch(match, winMsg)
            } else if (summarySecond.totalWickets >= maxWicketsSecond || summarySecond.legalBalls >= maxOvers * 6) {
                // All out or overs completed, failed to score target
                val runsDefended = target - 1 - summarySecond.totalRuns
                if (runsDefended == 0) {
                    // Match Tied! Do not automatically call finalizeMatch here so they can start Super Over
                } else {
                    val winMsg = "$defendTeam won by $runsDefended runs!"
                    finalizeMatch(match, winMsg)
                }
            }
        } else if (match.currentInnings == 3) {
            // Super Over Innings 1
            val battingTeam = getOpponent(match.firstInningsBattingTeam, match)
            val bowlingTeam = match.firstInningsBattingTeam
            val batRoster = if (battingTeam == match.teamA) match.teamAPlayers else match.teamBPlayers
            val bowlRoster = if (bowlingTeam == match.teamA) match.teamAPlayers else match.teamBPlayers
            val summarySuper1 = CricketStatCalculator.calculateInningsSummary(
                3, battingTeam, bowlingTeam, match.deliveries, batRoster, bowlRoster
            )
            // Managed in UI transition to 4th Innings (Super Over Chase)
        } else if (match.currentInnings == 4) {
            // Super Over Innings 2
            val battingTeam = match.firstInningsBattingTeam
            val bowlingTeam = getOpponent(match.firstInningsBattingTeam, match)
            val batRoster = if (battingTeam == match.teamA) match.teamAPlayers else match.teamBPlayers
            val bowlRoster = if (bowlingTeam == match.teamA) match.teamAPlayers else match.teamBPlayers

            val summarySuper1 = getInnings3Summary()
            val summarySuper2 = CricketStatCalculator.calculateInningsSummary(
                4, battingTeam, bowlingTeam, match.deliveries, batRoster, bowlRoster
            )

            val target = summarySuper1.totalRuns + 1
            if (summarySuper2.totalRuns >= target) {
                val wicketsLeft = 2 - summarySuper2.totalWickets
                val winMsg = "$battingTeam won in Super Over by $wicketsLeft wickets!"
                finalizeMatch(match, winMsg)
            } else if (summarySuper2.totalWickets >= 2 || summarySuper2.legalBalls >= 6) {
                val runsDefended = target - 1 - summarySuper2.totalRuns
                val winMsg = if (runsDefended == 0) {
                    "Match Tied in Super Over!"
                } else {
                    val defendingTeam = getOpponent(match.firstInningsBattingTeam, match)
                    "$defendingTeam won in Super Over by $runsDefended runs!"
                }
                finalizeMatch(match, winMsg)
            }
        }
    }

    fun startSuperOver() {
        val match = _activeMatch.value ?: return
        val chaserTeam = getOpponent(match.firstInningsBattingTeam, match)
        val defendTeam = match.firstInningsBattingTeam
        val chaserRoster = if (chaserTeam == match.teamA) match.teamAPlayers else match.teamBPlayers
        val defendRoster = if (defendTeam == match.teamA) match.teamAPlayers else match.teamBPlayers

        val updatedMatch = match.copy(
            currentInnings = 3,
            strikerName = chaserRoster.getOrElse(0) { "Batsman C1" },
            nonStrikerName = chaserRoster.getOrElse(1) { "Batsman C2" },
            bowlerName = defendRoster.getOrElse(10) { "Bowler D1" }
        )
        _activeMatch.value = updatedMatch
        saveAndPostUpdate(updatedMatch)
    }

    fun transitionToSuperOverSecondInnings() {
        val match = _activeMatch.value ?: return
        if (match.currentInnings != 3) return

        val battingTeamSO2 = match.firstInningsBattingTeam
        val bowlingTeamSO2 = getOpponent(match.firstInningsBattingTeam, match)
        val batRoster = if (battingTeamSO2 == match.teamA) match.teamAPlayers else match.teamBPlayers
        val bowlRoster = if (bowlingTeamSO2 == match.teamA) match.teamAPlayers else match.teamBPlayers

        val updatedMatch = match.copy(
            currentInnings = 4,
            strikerName = batRoster.getOrElse(0) { "Batsman S1" },
            nonStrikerName = batRoster.getOrElse(1) { "Batsman S2" },
            bowlerName = bowlRoster.getOrElse(10) { "Bowler S1" }
        )
        _activeMatch.value = updatedMatch
        saveAndPostUpdate(updatedMatch)
    }

    private fun finalizeMatch(match: MatchEntity, message: String) {
        val completedMatch = match.copy(
            status = MatchStatus.COMPLETED.name,
            winner = message
        )
        _activeMatch.value = completedMatch
        saveAndPostUpdate(completedMatch)
        _currentScreen.value = AppScreen.Summary
    }

    fun forceDeclareMatchWinner(winnerMsg: String) {
        val match = _activeMatch.value ?: return
        finalizeMatch(match, winnerMsg)
    }

    private fun getOpponent(team: String, match: MatchEntity): String {
        return if (team == match.teamA) match.teamB else match.teamA
    }

    private fun saveAndPostUpdate(match: MatchEntity) {
        viewModelScope.launch {
            repository.updateMatch(match)
        }
    }

    // Helper statistics builders for the view layer
    fun getActiveInningsSummary(): InningsSummary {
        val match = _activeMatch.value ?: return createEmptySummary()
        val current = match.currentInnings
        val battingTeam = when (current) {
            1 -> match.firstInningsBattingTeam
            2 -> getOpponent(match.firstInningsBattingTeam, match)
            3 -> getOpponent(match.firstInningsBattingTeam, match)
            4 -> match.firstInningsBattingTeam
            else -> match.firstInningsBattingTeam
        }
        val bowlingTeam = getOpponent(battingTeam, match)
        
        val batRoster = if (battingTeam == match.teamA) match.teamAPlayers else match.teamBPlayers
        val bowlRoster = if (bowlingTeam == match.teamA) match.teamAPlayers else match.teamBPlayers

        return CricketStatCalculator.calculateInningsSummary(
            match.currentInnings, battingTeam, bowlingTeam, match.deliveries, batRoster, bowlRoster
        )
    }

    fun getInnings1Summary(): InningsSummary {
        val match = _activeMatch.value ?: return createEmptySummary()
        val battingTeam = match.firstInningsBattingTeam
        val bowlingTeam = getOpponent(battingTeam, match)
        val batRoster = if (battingTeam == match.teamA) match.teamAPlayers else match.teamBPlayers
        val bowlRoster = if (bowlingTeam == match.teamA) match.teamAPlayers else match.teamBPlayers

        return CricketStatCalculator.calculateInningsSummary(
            1, battingTeam, bowlingTeam, match.deliveries, batRoster, bowlRoster
        )
    }

    fun getInnings2Summary(): InningsSummary {
        val match = _activeMatch.value ?: return createEmptySummary()
        val battingTeam = getOpponent(match.firstInningsBattingTeam, match)
        val bowlingTeam = match.firstInningsBattingTeam
        val batRoster = if (battingTeam == match.teamA) match.teamAPlayers else match.teamBPlayers
        val bowlRoster = if (bowlingTeam == match.teamA) match.teamAPlayers else match.teamBPlayers

        return CricketStatCalculator.calculateInningsSummary(
            2, battingTeam, bowlingTeam, match.deliveries, batRoster, bowlRoster
        )
    }

    fun getInnings3Summary(): InningsSummary {
        val match = _activeMatch.value ?: return createEmptySummary()
        val battingTeam = getOpponent(match.firstInningsBattingTeam, match)
        val bowlingTeam = match.firstInningsBattingTeam
        val batRoster = if (battingTeam == match.teamA) match.teamAPlayers else match.teamBPlayers
        val bowlRoster = if (bowlingTeam == match.teamA) match.teamAPlayers else match.teamBPlayers

        return CricketStatCalculator.calculateInningsSummary(
            3, battingTeam, bowlingTeam, match.deliveries, batRoster, bowlRoster
        )
    }

    fun getInnings4Summary(): InningsSummary {
        val match = _activeMatch.value ?: return createEmptySummary()
        val battingTeam = match.firstInningsBattingTeam
        val bowlingTeam = getOpponent(match.firstInningsBattingTeam, match)
        val batRoster = if (battingTeam == match.teamA) match.teamAPlayers else match.teamBPlayers
        val bowlRoster = if (bowlingTeam == match.teamA) match.teamAPlayers else match.teamBPlayers

        return CricketStatCalculator.calculateInningsSummary(
            4, battingTeam, bowlingTeam, match.deliveries, batRoster, bowlRoster
        )
    }

    private fun createEmptySummary(): InningsSummary {
        return InningsSummary(
            totalRuns = 0, totalWickets = 0, legalBalls = 0, overs = "0.0",
            extras = emptyMap(), runRate = 0.0, batsmanStats = emptyList(),
            bowlerStats = emptyList(), partnerships = emptyList(),
            fallOfWickets = emptyList(), recentBalls = emptyList()
        )
    }
}
