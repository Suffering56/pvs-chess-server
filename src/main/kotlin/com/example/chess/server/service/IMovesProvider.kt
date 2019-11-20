package com.example.chess.server.service

import com.example.chess.server.logic.IChessboard
import com.example.chess.server.logic.IGame
import com.example.chess.server.logic.misc.Point
import com.example.chess.shared.api.IPoint

/**
 * @author v.peschaniy
 *      Date: 20.11.2019
 */
interface IMovesProvider {

    fun getAvailableMoves(game: IGame, chessboard: IChessboard, point: IPoint): Set<Point>
}