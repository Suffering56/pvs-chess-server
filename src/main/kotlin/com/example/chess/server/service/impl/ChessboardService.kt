package com.example.chess.server.service.impl

import com.example.chess.server.entity.Game
import com.example.chess.server.logic.Chessboard
import com.example.chess.server.logic.IMutableChessboard
import com.example.chess.server.repository.HistoryRepository
import com.example.chess.server.service.IChessboardService
import com.google.common.collect.Range
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * @author v.peschaniy
 *      Date: 24.07.2019
 */
@Service
class ChessboardService : IChessboardService {

    @Autowired
    private lateinit var historyRepository: HistoryRepository

    override fun createChessboardForGame(game: Game, position: Int): IMutableChessboard {
        val availablePositionsRange = Range.closed(0, game.position)

        require(availablePositionsRange.contains(position)) { "position must be in range: $availablePositionsRange" }

        val history = historyRepository.findByGameIdAndPositionLessThanEqualOrderByPositionAsc(game.id!!, position)
        return Chessboard.byHistory(history)
    }
}