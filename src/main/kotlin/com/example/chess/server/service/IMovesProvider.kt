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

    fun getAvailableMoves(game: IUnmodifiableGame, chessboard: IUnmodifiableChessboard, pointFrom: Point): List<Point>

    fun isUnderCheck(game: IUnmodifiableGame, chessboard: IUnmodifiableChessboard, kingSide: Side): Boolean

    fun getTargetThreatsCount(game: IUnmodifiableGame, chessboard: IUnmodifiableChessboard, targetPoint: Point): Int

    fun getTargetDefendersCount(game: IUnmodifiableGame, chessboard: IUnmodifiableChessboard, targetPoint: Point): Int

    fun getTargetThreats(game: IUnmodifiableGame, chessboard: IUnmodifiableChessboard, targetPoint: Point, isBatterySupported: Boolean): List<Point>

    fun getTargetDefenders(game: IUnmodifiableGame, chessboard: IUnmodifiableChessboard, targetPoint: Point, isBatterySupported: Boolean): List<Point>
}