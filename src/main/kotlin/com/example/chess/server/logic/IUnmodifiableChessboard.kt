package com.example.chess.server.logic

import com.example.chess.server.logic.misc.Cell
import com.example.chess.server.logic.misc.Move
import com.example.chess.server.logic.misc.Point
import com.example.chess.server.logic.misc.compressPoint
import com.example.chess.shared.dto.ChessboardDTO
import com.example.chess.shared.enums.Piece
import com.example.chess.shared.enums.Side
import java.util.stream.Stream

/**
 * @author v.peschaniy
 *      Date: 24.07.2019
 */
interface IUnmodifiableChessboard {

    val position: Int

    fun getPieceNullable(compressedPoint: Int): Piece?

    fun getPieceNullable(rowIndex: Int, columnIndex: Int): Piece? {
        val index = compressPoint(rowIndex, columnIndex)
        return getPieceNullable(index)
    }

    fun getPieceNullable(point: Point) = getPieceNullable(point.compress())

    fun getPiece(compressedPoint: Int): Piece = requireNotNull(getPieceNullable(compressedPoint)) {
        "piece on position=[${Point.of(compressedPoint).toPrettyString()}] cannot be null:\r\n${toPrettyString(Point.of(compressedPoint))}"
    }

    fun getPiece(rowIndex: Int, columnIndex: Int): Piece {
        val index = compressPoint(rowIndex, columnIndex)
        return getPiece(index)
    }

    fun getPiece(point: Point): Piece = getPiece(point.compress())

    fun cellsStream(side: Side): Stream<Cell>

    fun getKingPoint(side: Side): Point

    fun toDTO(): ChessboardDTO

    fun copyOf(): IChessboard

    fun toPrettyString(vararg highlightedPoints: Point?): String

    fun toPrettyString(previousMove: Move): String

    fun toPrettyString(): String
}
