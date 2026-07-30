package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MatchDao {
    @Query("SELECT * FROM matches ORDER BY timestamp DESC")
    fun getAllMatches(): Flow<List<MatchEntity>>

    @Query("SELECT * FROM matches WHERE id = :id")
    suspend fun getMatchById(id: Int): MatchEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMatch(match: MatchEntity): Long

    @Update
    suspend fun updateMatch(match: MatchEntity)

    @Delete
    suspend fun deleteMatch(match: MatchEntity)

    @Query("DELETE FROM matches WHERE id = :id")
    suspend fun deleteMatchById(id: Int)
}
