package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Authentic Cricbuzz Premium Dark Athletic Scheme
val DarkBgMain = Color(0xFF0C1621)      // Cricbuzz Signature Sports-Navy Dark Background
val DarkBgSurface = Color(0xFF162534)   // Clean medium-contrast tactical navy for panels
val DarkBgGlass = Color(0x3D162534)     // Frosted premium glass with 24% opacity
val OuterSpace = Color(0xFF223548)      // Richer highlight surface for select cards/headers

// High-Stakes Stadium Accents
val StadiumGreen = Color(0xFF00A352)    // Exact iconic Cricbuzz Sports Green
val WicketCrimson = Color(0xFFF44336)   // Vivid referee-red for wickets and out events
val GoldAccent = Color(0xFFFFD100)      // Sleek cricket-gold for highlights, partnership, and live indicators
val InfoTeal = Color(0xFF00B0FF)        // Deep aqua-cyan for detailed analytical statistics

// Text Gradients & Mutes
val CleanWhite = Color(0xFFF8FAFC)      // Crisp athletic soft-white
val CoolSlate = Color(0xFFE2E8F0)       // Bright light gray for key content
val MutedGrey = Color(0xFF94A3B8)       // Muted slate gray for secondary labels
val DarkTextMuted = Color(0xFF334155)   // Muted outline indicator

// Transparent Glass Borders
val GlassBorder = Color(0x26E2E8F0)     // Soft outline for premium structural layout
val GlassBorderTeal = Color(0x4000A352) // Iconic Cricbuzz Green border halo

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


