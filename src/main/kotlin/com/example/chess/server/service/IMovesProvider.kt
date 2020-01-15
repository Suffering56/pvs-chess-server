package com.example.chess.server.service

import com.example.chess.server.logic.IUnmodifiableChessboard
import com.example.chess.server.logic.IPoint
import com.example.chess.server.logic.IUnmodifiableGame
import com.example.chess.shared.enums.Side

/**
 * @author v.peschaniy
 *      Date: 20.11.2019
 */
interface IMovesProvider {

    fun getAvailableMoves(game: IUnmodifiableGame, chessboard: IUnmodifiableChessboard, pointFrom: IPoint): Set<IPoint>

    fun isUnderCheck(kingSide: Side, chessboard: IUnmodifiableChessboard): Boolean
}