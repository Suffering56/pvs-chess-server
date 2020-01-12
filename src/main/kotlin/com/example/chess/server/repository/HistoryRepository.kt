package com.example.chess.server.repository

import com.example.chess.server.entity.HistoryItem
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface HistoryRepository : CrudRepository<HistoryItem, Long> {

    fun findByGameIdAndPositionLessThanEqualOrderByPositionAsc(gameId: Long, position: Int): List<HistoryItem>

    fun findByGameIdAndPosition(gameId: Long, position: Int): HistoryItem?

    fun removeAllByGameIdAndPositionGreaterThan(gameId: Long, newGamePosition: Int)
}
