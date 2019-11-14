package com.example.chess.server.service

import com.example.chess.server.entity.Game
import com.example.chess.server.logic.IMutableChessboard
import com.example.chess.server.logic.misc.Point
import com.example.chess.shared.api.IMove
import com.example.chess.shared.dto.ChangesDTO
import java.util.stream.Stream

/**
 * @author v.peschaniy
 *      Date: 22.07.2019
 */
interface IGameService {

    fun createNewGame(): Game

    fun saveGame(game: Game): Game

    fun findAndCheckGame(gameId: Long): Game

    fun getMovesByPoint(game: Game, point: Point): Stream<Point>

    fun applyMove(game: Game, chessboard: IMutableChessboard, move: IMove): ChangesDTO
}