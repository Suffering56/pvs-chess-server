package com.example.chess.server.logic

import com.example.chess.server.logic.misc.*
import com.example.chess.shared.dto.ChessboardDTO
import com.example.chess.shared.enums.Piece
import com.example.chess.shared.enums.Side
import java.util.stream.Stream

/**
 * @author v.peschaniy
 *      Date: 24.07.2019
 */
interface IChessboard {

    val position: Int

    fun getPieceNullable(compressedPoint: Int): Piece?

    fun getPieceNullable(rowIndex: Int, columnIndex: Int): Piece? {
        val index = compressPoint(rowIndex, columnIndex)
        return getPieceNullable(index)
    }

    fun getPieceNullable(point: IPoint) = getPieceNullable(point.compress())

    fun getPiece(compressedPoint: Int): Piece = requireNotNull(getPieceNullable(compressedPoint)) {
        "piece on position=[${Point.of(compressedPoint).toPrettyString()}] cannot be null:\r\n${toPrettyString()}"
    }

    fun getPiece(rowIndex: Int, columnIndex: Int): Piece {
        val index = compressPoint(rowIndex, columnIndex)
        return getPiece(index)
    }

    fun getPiece(point: IPoint): Piece = getPiece(point.compress())

    fun cellsStream(side: Side): Stream<Cell>

    fun getKingPoint(side: Side): IPoint

    fun toDTO(): ChessboardDTO

    fun toPrettyString(): String

    fun copyOf(): IMutableChessboard
}