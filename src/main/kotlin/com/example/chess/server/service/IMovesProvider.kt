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

    /**
     * @return список точек, куда можно пойти фигурой, находящейся в pointFrom.
     * В список не будут добавлены ходы, недоступные из-за ситуаций с шахом.
     */
    fun getAvailableMovesFrom(game: IUnmodifiableGame, chessboard: IUnmodifiableChessboard, pointFrom: Point): List<Point>

    /**
     * @return true, если на текущий момент на доске chessboard король стороны kingSide находится под шахом здесь и сейчас, иначе - false
     */
    fun isUnderCheck(game: IUnmodifiableGame, chessboard: IUnmodifiableChessboard, kingSide: Side): Boolean

    /**
     * @return список точек, откуда можно срубить targetPoint здесь и сейчас (если не включен флаг isBatterySupported).
     *
     * Важно!!! При составлении списка шахи не учитываются, то есть ход Move.of(it, targetPoint) может быть на самом деле недоступен,
     *  потому что король находится под шахом, или встанет под шах.
     *  Для того, чтобы отфильтровать недоступные, воспользуйтесь методом:
     *  @see IMovesProvider.isMoveAvailable
     */
    fun getTargetAttackers(game: IUnmodifiableGame, chessboard: IUnmodifiableChessboard, targetPoint: Point, isBatterySupported: Boolean): List<Point>

    /**
     * @return список точек, где стоят фигуры, защищающие targetPoint здесь и сейчас (если не включен флаг isBatterySupported).
     *
     * Важно!!! При составлении списка шахи не учитываются, то есть ход Move.of(it, targetPoint) может быть на самом деле недоступен,
     *  потому что король находится под шахом, или встанет под шах.
     *  Для того, чтобы отфильтровать недоступные, воспользуйтесь методом:
     *  @see IMovesProvider.isMoveAvailable
     */
    fun getTargetDefenders(game: IUnmodifiableGame, chessboard: IUnmodifiableChessboard, targetPoint: Point, isBatterySupported: Boolean): List<Point>

    /**
     * @return true если ход (Move.of(pointFrom, pointTo)) доступен здесь и сейчас,
     * т.е. после выполнения данного хода король 100% не будет находиться под шахом.
     */
    fun isMoveAvailable(game: IUnmodifiableGame, chessboard: IUnmodifiableChessboard, pointFrom: Point, pointTo: Point): Boolean
}