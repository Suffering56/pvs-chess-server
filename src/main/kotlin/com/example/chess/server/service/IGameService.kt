package com.example.chess.server.service

import com.example.chess.server.logic.*
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

    fun getMovesByPoint(gameId: Long, chessboard: IChessboard, point: IPoint): Set<IPoint>

    fun applyMove(gameId: Long, chessboard: IMutableChessboard, move: IMove): ChangesDTO

    fun rollback(gameId: Long, positionsOffset: Int): IUnmodifiableGame

    fun getNextMoveChanges(gameId: Long, prevMoveSide: Side, chessboardPosition: Int): ChangesDTO
}