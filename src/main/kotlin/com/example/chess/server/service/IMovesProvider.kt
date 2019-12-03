package com.example.chess.server.service

import com.example.chess.server.logic.IChessboard
import com.example.chess.server.logic.IGame
import com.example.chess.server.logic.misc.Point
import com.example.chess.shared.api.IPoint
import com.example.chess.shared.enums.Side

/**
 * @author v.peschaniy
 *      Date: 20.11.2019
 */
interface IMovesProvider {

    fun getAvailableMoves(pointFrom: IPoint, chessboard: IChessboard, game: IGame): Set<Point>

    fun isUnderCheck(kingSide: Side, chessboard: IChessboard): Boolean
}