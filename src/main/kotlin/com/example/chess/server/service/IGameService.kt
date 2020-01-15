package com.example.chess.server.service

import com.example.chess.server.logic.IMove
import com.example.chess.server.logic.IPoint
import com.example.chess.server.logic.IUnmodifiableChessboard
import com.example.chess.server.logic.IUnmodifiableGame
import com.example.chess.server.logic.misc.GameResult
import com.example.chess.shared.dto.ChangesDTO
import com.example.chess.shared.enums.GameMode
import com.example.chess.shared.enums.Side

/**
 * @author v.peschaniy
 *      Date: 22.07.2019
 */
interface IGameService {

    fun createNewGame(userId: String, mode: GameMode, side: Side, isConstructor: Boolean = false): IUnmodifiableGame

    fun registerPlayer(userId: String, gameId: Long, side: Side, forced: Boolean = false): IUnmodifiableGame

    fun findAndCheckGame(gameId: Long): IUnmodifiableGame

    fun getMovesByPoint(gameId: Long, chessboard: IUnmodifiableChessboard, point: IPoint): GameResult<Set<IPoint>>

    fun applyPlayerMove(gameId: Long, userId: String, move: IMove): GameResult<ChangesDTO>

    fun applyBotMove(gameId: Long, moveProvider: (IUnmodifiableGame, IUnmodifiableChessboard) -> IMove): GameResult<ChangesDTO>

    fun rollback(gameId: Long, positionsOffset: Int): IUnmodifiableGame

    fun listenChanges(gameId: Long, prevMoveSide: Side, chessboardPosition: Int): GameResult<ChangesDTO>
}