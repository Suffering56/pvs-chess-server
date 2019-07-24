package com.example.chess.server.service.impl

import com.example.chess.server.entity.Game
import com.example.chess.server.entity.History
import com.example.chess.server.logic.Chessboard
import com.example.chess.server.logic.IChessboard
import com.example.chess.server.service.IChessboardService
import com.example.chess.shared.dto.ChessboardDTO
import org.springframework.stereotype.Service

/**
 * @author v.peschaniy
 *      Date: 24.07.2019
 */
@Service
class ChessboardService : IChessboardService {

    override fun createChessboardForGame(game: Game, position: Int): IChessboard {
        val history: List<History> = emptyList()
        if (position == 0) {
            return Chessboard.byInitial()
        }
        return Chessboard.byHistory(history)
    }

    private fun createChessboardByHistory(): ChessboardDTO {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    private fun createInitialChessboard() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}