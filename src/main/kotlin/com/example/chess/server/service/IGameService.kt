package com.example.chess.server.service

import com.example.chess.server.logic.IUnmodifiableChessboard
import com.example.chess.server.logic.IUnmodifiableGame
import com.example.chess.server.logic.misc.GameResult
import com.example.chess.server.logic.misc.Move
import com.example.chess.server.logic.misc.Point
import com.example.chess.shared.dto.ChangesDTO
import com.example.chess.shared.dto.ChessboardDTO
import com.example.chess.shared.dto.ConstructorGameDTO
import com.example.chess.shared.enums.GameMode
import com.example.chess.shared.enums.Side

/**
 * @author v.peschaniy
 *      Date: 22.07.2019
 */
interface IGameService {

    // init
    fun createNewGame(userId: String, mode: GameMode, side: Side): IUnmodifiableGame

    fun createConstructedGame(userId: String, mode: GameMode, side: Side, clientChessboard: ChessboardDTO): GameResult<ConstructorGameDTO>

    fun registerPlayer(gameId: Long, userId: String, side: Side, forced: Boolean = false): IUnmodifiableGame

    fun checkRegistration(gameId: Long, userId: String)

    fun findAndCheckGame(gameId: Long, userId: String, chessboardPosition: Int? = null): GameResult<IUnmodifiableChessboard>

    // read
    fun getMovesByPoint(gameId: Long, point: Point, clientPosition: Int): GameResult<List<Point>>

    fun listenChanges(gameId: Long, userId: String, clientPosition: Int): GameResult<ChangesDTO>

    // write
    fun applyPlayerMove(gameId: Long, userId: String, move: Move, clientPosition: Int): GameResult<ChangesDTO>

    fun applyBotMove(gameId: Long, clientPosition: Int, moveProvider: (IUnmodifiableGame, IUnmodifiableChessboard) -> Move): GameResult<ChangesDTO>

    fun rollback(gameId: Long, positionsOffset: Int, clientPosition: Int): GameResult<IUnmodifiableChessboard>
}