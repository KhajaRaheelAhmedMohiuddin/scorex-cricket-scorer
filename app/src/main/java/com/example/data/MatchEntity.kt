package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.model.Delivery

@Entity(tableName = "matches")
data class MatchEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val teamA: String,
    val teamB: String,
    val teamAPlayers: List<String>,
    val teamBPlayers: List<String>,
    val selectedOvers: Int,
    val format: String,             // T20, ODI, TEST, CUSTOM
    val status: String,             // SETUP, LIVE, COMPLETED
    val winner: String? = null,
    val currentInnings: Int = 1,    // 1 or 2
    val firstInningsBattingTeam: String,
    val strikerName: String = "",
    val nonStrikerName: String = "",
    val bowlerName: String = "",
    val deliveries: List<Delivery> = emptyList(),
    val timestamp: Long = System.currentTimeMillis()
)
