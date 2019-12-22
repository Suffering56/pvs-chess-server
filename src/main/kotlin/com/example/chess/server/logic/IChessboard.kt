package com.example.chess.server.logic

import com.example.chess.server.logic.misc.Point
import com.example.chess.server.logic.misc.toPrettyString
import com.example.chess.shared.api.IPoint
import com.example.chess.shared.dto.ChessboardDTO
import com.example.chess.shared.enums.Piece
import com.example.chess.shared.enums.Side

/**
 * @author v.peschaniy
 *      Date: 24.07.2019
 */
interface IChessboard {

    val position: Int

    fun getPieceNullable(rowIndex: Int, columnIndex: Int): Piece?

    fun toDTO(): ChessboardDTO

    fun toPrettyString(): String

    //default
    fun getPieceNullable(point: IPoint) = getPieceNullable(point.row, point.col)

    fun getPiece(point: IPoint) = getPiece(point.row, point.col)

    fun getPiece(rowIndex: Int, columnIndex: Int) = requireNotNull(getPieceNullable(rowIndex, columnIndex)) {
        "piece on position=[${Point.of(rowIndex, columnIndex).toPrettyString()}] cannot be null:\r\n${toPrettyString()}"
    }

    fun getKingPoint(side: Side): IPoint
}