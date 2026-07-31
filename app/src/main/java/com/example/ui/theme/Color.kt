package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// ScoreX palette — an original scheme anchored on the app logo:
// ink-navy backdrop, silver/white wordmark, and a signal red from the "X".

// Ink-navy backgrounds
val DarkBgMain = Color(0xFF0A1524)      // app background
val DarkBgSurface = Color(0xFF13202F)   // panels / cards
val DarkBgGlass = Color(0x3D13202F)     // frosted glass surface (~24% opacity)
val OuterSpace = Color(0xFF203247)      // elevated surface for select cards/headers

// Accents
val StadiumGreen = Color(0xFF14B866)    // scoring green — positive actions, runs, run-rate
val WicketCrimson = Color(0xFFE43C43)   // signal red from the logo "X" — brand, live, wickets
val GoldAccent = Color(0xFFFFC633)      // highlights, partnerships, live indicators
val InfoTeal = Color(0xFF37B6F0)        // analytics / secondary information

// Text — silver/white from the wordmark
val CleanWhite = Color(0xFFF5F8FC)      // primary text
val CoolSlate = Color(0xFFDCE4EF)       // bright secondary text
val MutedGrey = Color(0xFF8B9AAE)       // muted labels
val MutedGrey2 = Color(0xFF7E8F9F)      // dimmer label grey (unselected states)
val DarkTextMuted = Color(0xFF35485F)   // faint outlines / disabled

// Structural surfaces & borders
val SurfaceMuted = Color(0xFF111C2B)    // unselected pills / cards
val InputBg = Color(0xFF0B1420)         // text-field container
val HairlineBorder = Color(0xFF1C2B3E)  // subtle unselected outline
val GlassBorder = Color(0x26DCE4EF)     // soft structural outline
val GlassBorderTeal = Color(0x4014B866) // green border halo

// Illustration
val BatWood = Color(0xFFE5A65D)         // cricket-bat blade in the drawn icons

fun String.toAbbreviation(): String {
    val trimmed = this.trim()
    if (trimmed.isEmpty()) return ""
    val words = trimmed.split(Regex("\\s+"))
    return if (words.size >= 2) {
        words.mapNotNull { it.firstOrNull()?.uppercaseChar() }.joinToString("")
    } else {
        if (trimmed.length >= 3) trimmed.substring(0, 3).uppercase()
        else trimmed.uppercase()
    }
}

fun String.abbreviateTeams(teamA: String, teamB: String): String {
    if (teamA.trim().isEmpty() || teamB.trim().isEmpty()) return this
    var result = this
    val abbrA = teamA.toAbbreviation()
    val abbrB = teamB.toAbbreviation()
    if (teamA.length >= teamB.length) {
        result = result.replace(teamA, abbrA, ignoreCase = true)
        result = result.replace(teamB, abbrB, ignoreCase = true)
    } else {
        result = result.replace(teamB, abbrB, ignoreCase = true)
        result = result.replace(teamA, abbrA, ignoreCase = true)
    }
    return result
}
