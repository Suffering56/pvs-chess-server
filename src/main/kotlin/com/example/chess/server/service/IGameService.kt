package com.example.chess.server.service

import com.example.chess.server.entity.Game
import com.example.chess.server.logic.*
import com.example.chess.shared.dto.ChangesDTO
import com.example.chess.shared.enums.GameMode
import com.example.chess.shared.enums.Side

/**
 * @author v.peschaniy
 *      Date: 22.07.2019
 */
interface IGameService {

    fun createNewGame(userId: String, mode: GameMode, side: Side, isConstructor: Boolean = false): Game

    fun saveGame(game: Game): Game

    fun findAndCheckGame(gameId: Long): Game

    fun getMovesByPoint(game: IGame, chessboard: IChessboard, point: IPoint): Set<IPoint>

    fun applyMove(game: Game, chessboard: IMutableChessboard, move: IMove): ChangesDTO

    fun rollback(game: Game, positionsOffset: Int): Game

    fun getNextMoveChanges(game: Game, prevMoveSide: Side, chessboardPosition: Int): ChangesDTO
}