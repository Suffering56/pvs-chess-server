package com.example.chess.enums

enum class PieceType(
    val value: Int,
    val startColumnIndex: Int = 0
) {
    PAWN(1),
    KNIGHT(3),
    BISHOP(3),
    ROOK(5),
    QUEEN(9, 4),
    KING(9999, 3);
}