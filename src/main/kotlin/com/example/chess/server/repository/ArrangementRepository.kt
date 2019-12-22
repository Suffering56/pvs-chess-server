package com.example.chess.server.repository

import com.example.chess.server.entity.ArrangementItem
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface ArrangementRepository : CrudRepository<ArrangementItem, Long> {

    fun findAllByGameId(gameId: Long): List<ArrangementItem>
}
