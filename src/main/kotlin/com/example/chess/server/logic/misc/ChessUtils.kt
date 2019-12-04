package com.example.chess.server.logic.misc

import com.example.chess.shared.Constants
import com.example.chess.shared.Constants.ROOK_LONG_COLUMN_INDEX
import com.example.chess.shared.Constants.ROOK_SHORT_COLUMN_INDEX
import com.example.chess.shared.api.IMove
import com.example.chess.shared.api.IPoint
import com.example.chess.shared.enums.Piece
import com.example.chess.shared.enums.PieceType
import com.example.chess.shared.enums.PieceType.BISHOP
import com.example.chess.shared.enums.PieceType.ROOK
import kotlin.math.abs

/**
 * @author v.peschaniy
 *      Date: 11.11.2019
 */
val columnNamesMap = mapOf(
    Pair(0, "h"),
    Pair(1, "g"),
    Pair(2, "f"),
    Pair(3, "e"),
    Pair(4, "d"),
    Pair(5, "c"),
    Pair(6, "b"),
    Pair(7, "a")
)

fun IMove.isPawnAttacks(pieceFrom: Piece): Boolean {
    return pieceFrom.isPawn() && from.col != to.col
}

fun IMove.isEnPassant(pieceFrom: Piece, pieceTo: Piece?): Boolean {
    return isPawnAttacks(pieceFrom) && pieceTo == null
}

fun IMove.isPawnTransformation(pieceFrom: Piece): Boolean {
    return pieceFrom.isPawn() && (to.row == ROOK_SHORT_COLUMN_INDEX || to.row == ROOK_LONG_COLUMN_INDEX)
}

fun IMove.isCastling(pieceFrom: Piece): Boolean {
    return pieceFrom.isKing() && abs(from.col - to.col) == 2
}

fun IMove.isLongCastling(pieceFrom: Piece): Boolean {
    return pieceFrom.isKing() && from.col - to.col == -2
}

fun IMove.isShortCastling(pieceFrom: Piece): Boolean {
    return pieceFrom.isKing() && from.col - to.col == 2
}

fun IMove.isLongPawnMove(pieceFrom: Piece): Boolean {
    return pieceFrom.isPawn() && from.col == to.col && abs(from.row - to.row) == 2
}

fun IMove.toPrettyString(pieceFrom: Piece): String {
    return "${pieceFrom.shortName}: ${from.toPrettyString()} -> ${to.toPrettyString()} (${pieceFrom.side})"
}

fun IPoint.toPrettyString(): String {
    return "${columnNamesMap[col]}${row + 1}"
}

fun isIndexOutOfBounds(index: Int): Boolean {
    return index < 0 || index >= Constants.BOARD_SIZE
}

fun IPoint.hasCommonVectorWith(other: IPoint): Boolean = hasCommonVectorWith(other.row, other.col)

fun IPoint.hasCommonVectorWith(other: IPoint, otherPieceType: PieceType): Boolean = hasCommonVectorWith(other.row, other.col, otherPieceType)

fun IPoint.hasCommonVectorWith(otherRow: Int, otherCol: Int): Boolean {
    return this.row == otherRow
            || this.col == otherCol
            || (abs(this.row - otherRow) == abs(this.col - otherCol))
}

fun IPoint.hasCommonVectorWith(otherRow: Int, otherCol: Int, otherPieceType: PieceType): Boolean {
    return when (otherPieceType) {
        ROOK -> return this.row == otherRow || this.col == otherCol
        BISHOP -> abs(this.row - otherRow) == abs(this.col - otherCol)
        else -> throw UnsupportedOperationException("unsupported piece type: $otherPieceType")
    }
}

fun IPoint.isBorderedWith(otherRow: Int, otherCol: Int): Boolean {
    check(this.row != otherRow || this.col != otherCol) {
        "is bordered with self? are you serious?, self: $this"
    }

    return abs(this.row - otherRow) <= 1
            && abs(this.col - otherCol) <= 1
}

fun IPoint.isBorderedWith(other: IPoint) = isBorderedWith(other.row, other.col)