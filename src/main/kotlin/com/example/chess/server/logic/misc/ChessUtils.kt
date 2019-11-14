package com.example.chess.server.logic.misc

import com.example.chess.shared.api.IMove
import com.example.chess.shared.api.IPoint
import com.example.chess.shared.enums.Piece
import kotlin.math.abs

/**
 * @author v.peschaniy
 *      Date: 11.11.2019
 */

const val ROOK_SHORT_COLUMN_INDEX = 0
const val ROOK_LONG_COLUMN_INDEX = 7

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

fun IMove.toChessString(pieceFrom: Piece): String {
    return "${pieceFrom.shortName}: ${from.toChessString()} -> ${to.toChessString()} (${pieceFrom.side})"
}

fun IPoint.toChessString(): String {
    return "${columnNamesMap[col]}${row + 1}"
}