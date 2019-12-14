package com.example.chess.server.service

import com.example.chess.server.entity.Game
import com.example.chess.server.logic.IChessboard
import com.example.chess.server.logic.IGame
import com.example.chess.server.logic.IMutableChessboard
import com.example.chess.shared.api.IMove
import com.example.chess.shared.api.IPoint
import com.example.chess.shared.dto.ChangesDTO

/**
 * @author v.peschaniy
 *      Date: 22.07.2019
 */
interface IGameService {

    fun createNewGame(): Game

    fun saveGame(game: Game): Game

    fun findAndCheckGame(gameId: Long): Game

    fun getMovesByPoint(game: IGame, chessboard: IChessboard, point: IPoint): Set<IPoint>

    fun applyMove(game: Game, chessboard: IMutableChessboard, move: IMove): ChangesDTO

    fun rollback(game: Game, positionsOffset: Int): Game
}