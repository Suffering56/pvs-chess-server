package com.example.chess.server.service

import com.example.chess.server.entity.Game
import com.example.chess.server.logic.misc.Point
import com.example.chess.shared.dto.MoveDTO
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

    fun applyMove(game: Game, move: MoveDTO): Any
}