package com.example.chess.server.service

import com.example.chess.server.entity.Game
import com.example.chess.shared.dto.ChessboardDTO

/**
 * @author v.peschaniy
 *      Date: 24.07.2019
 */
interface IChessboardService {

    fun createChessboardForGame(game: Game, position: Int): ChessboardDTO
}