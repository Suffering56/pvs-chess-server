package com.example.chess.server.service

import com.example.chess.server.entity.Game
import com.example.chess.shared.Move
import com.example.chess.shared.Playground
import com.example.chess.shared.Point

/**
 * @author v.peschaniy
 *      Date: 22.07.2019
 */
interface IGameService {

    fun findAndCheckGame(gameId: Long): Game

    fun createPlaygroundByGame(game: Game, position: Int): Playground

    fun getMovesByPoint(gameId: Long, point: Point): Set<Point>

    fun applyMove(game: Game, move: Move): Any
}