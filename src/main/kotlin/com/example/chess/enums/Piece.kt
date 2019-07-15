package com.example.chess.enums

import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.fasterxml.jackson.databind.node.ObjectNode

enum class Piece(
    val side: Side,
    val type: PieceType,
    val jsonNode: ObjectNode = JsonNodeFactory.instance.objectNode()
        .put("type", type.toString())
        .put("side", side.toString())
) {

    PAWN_WHITE(Side.WHITE, PieceType.PAWN),
    KNIGHT_WHITE(Side.WHITE, PieceType.KNIGHT),
    BISHOP_WHITE(Side.WHITE, PieceType.BISHOP),
    ROOK_WHITE(Side.WHITE, PieceType.ROOK),
    QUEEN_WHITE(Side.WHITE, PieceType.QUEEN),
    KING_WHITE(Side.WHITE, PieceType.KING),

    PAWN_BLACK(Side.BLACK, PieceType.PAWN),
    KNIGHT_BLACK(Side.BLACK, PieceType.KNIGHT),
    BISHOP_BLACK(Side.BLACK, PieceType.BISHOP),
    ROOK_BLACK(Side.BLACK, PieceType.ROOK),
    QUEEN_BLACK(Side.BLACK, PieceType.QUEEN),
    KING_BLACK(Side.BLACK, PieceType.KING);

    fun of(side: Side, type: PieceType): Piece {
        return if (side === Side.WHITE) {
            when (type) {
                PieceType.PAWN -> PAWN_WHITE
                PieceType.KNIGHT -> KNIGHT_WHITE
                PieceType.BISHOP -> BISHOP_WHITE
                PieceType.ROOK -> ROOK_WHITE
                PieceType.QUEEN -> QUEEN_WHITE
                PieceType.KING -> KING_WHITE
            }
        } else {
            when (type) {
                PieceType.PAWN -> PAWN_BLACK
                PieceType.KNIGHT -> KNIGHT_BLACK
                PieceType.BISHOP -> BISHOP_BLACK
                PieceType.ROOK -> ROOK_BLACK
                PieceType.QUEEN -> QUEEN_BLACK
                PieceType.KING -> KING_BLACK
            }
        }
    }

    @JsonValue
    fun toJson(): ObjectNode {
        return jsonNode
    }

    fun isPawn(): Boolean {
        return type === PieceType.PAWN
    }

    fun isKing(): Boolean {
        return type === PieceType.KING
    }

    fun isRook(): Boolean {
        return type === PieceType.ROOK
    }
}