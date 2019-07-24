package com.example.chess.server.service.impl

import com.example.chess.server.entity.Game
import com.example.chess.server.entity.History
import com.example.chess.server.logic.Chessboard
import com.example.chess.server.service.IChessboardService
import com.example.chess.shared.dto.ChessboardDTO
import org.springframework.stereotype.Service

/**
 * @author v.peschaniy
 *      Date: 24.07.2019
 */
@Service
class ChessboardService : IChessboardService {

    override fun createChessboardForGame(game: Game, position: Int): ChessboardDTO {
        val history: List<History>
        if (position == 0) {

        }
//        return Chessboard.ofHistory(history)
        TODO()
    }

    private fun createChessboardByHistory(): ChessboardDTO {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    private fun createInitialChessboard() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}