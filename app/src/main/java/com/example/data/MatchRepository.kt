package com.example.data

import kotlinx.coroutines.flow.Flow

class MatchRepository(private val matchDao: MatchDao) {
    val allMatches: Flow<List<MatchEntity>> = matchDao.getAllMatches()

    suspend fun getMatchById(id: Int): MatchEntity? {
        return matchDao.getMatchById(id)
    }

    suspend fun insertMatch(match: MatchEntity): Long {
        return matchDao.insertMatch(match)
    }

    suspend fun updateMatch(match: MatchEntity) {
        matchDao.updateMatch(match)
    }

    suspend fun deleteMatch(match: MatchEntity) {
        matchDao.deleteMatch(match)
    }

    suspend fun deleteMatchById(id: Int) {
        matchDao.deleteMatchById(id)
    }
}
