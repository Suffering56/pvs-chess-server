package com.example.chess.server.service

import com.example.chess.server.logic.IUnmodifiableChessboard
import com.example.chess.server.logic.IUnmodifiableGame
import com.example.chess.server.logic.misc.Point
import com.example.chess.shared.enums.Side

/**
 * @author v.peschaniy
 *      Date: 20.11.2019
 */
interface IMovesProvider {

    fun getAvailableMoves(game: IUnmodifiableGame, chessboard: IUnmodifiableChessboard, pointFrom: Point): Set<Point>

    fun isUnderCheck(kingSide: Side, chessboard: IUnmodifiableChessboard): Boolean
}