package com.example.chess.server.repository

import com.example.chess.server.entity.History
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface HistoryRepository : CrudRepository<History, Long> {

    fun findByGameIdAndPositionLessThanEqualOrderByPositionAsc(gameId: Long, position: Int): List<History>

    fun findByGameIdAndPosition(gameId: Long, position: Int): History

    fun removeAllByGameIdAndPositionGreaterThan(gameId: Long, newGamePosition: Int)
}
