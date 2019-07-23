package com.example.chess.server.service

import com.example.chess.server.entity.Game
import com.example.chess.server.objects.Point
import com.example.chess.shared.dto.ChessboardDTO
import com.example.chess.shared.dto.MoveDTO
import java.util.stream.Stream

/**
 * @author v.peschaniy
 *      Date: 22.07.2019
 */
interface IGameService {

    fun findAndCheckGame(gameId: Long): Game

    fun createPlaygroundByGame(game: Game, position: Int): ChessboardDTO

    fun getMovesByPoint(gameId: Long, point: Point): Stream<Point>

    fun applyMove(game: Game, move: MoveDTO): Any
}