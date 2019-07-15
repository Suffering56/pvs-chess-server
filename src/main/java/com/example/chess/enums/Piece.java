package com.example.chess.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

import java.util.Objects;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public enum Piece {

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

    Piece(Side side, PieceType type) {
        this.side = side;
        this.type = type;

        jsonNode = JsonNodeFactory.instance.objectNode()
                .put("type", type.toString())
                .put("side", side.toString());
    }

    Side side;
    PieceType type;
    ObjectNode jsonNode;

    @SuppressWarnings("Duplicates")
    public static Piece of(Side side, PieceType type) {
        Objects.requireNonNull(side);
        if (side == Side.WHITE) {
            switch (type) {
                case PAWN:
                    return PAWN_WHITE;
                case KNIGHT:
                    return KNIGHT_WHITE;
                case BISHOP:
                    return BISHOP_WHITE;
                case ROOK:
                    return ROOK_WHITE;
                case QUEEN:
                    return QUEEN_WHITE;
                case KING:
                    return KING_WHITE;
            }
        } else {
            switch (type) {
                case PAWN:
                    return PAWN_BLACK;
                case KNIGHT:
                    return KNIGHT_BLACK;
                case BISHOP:
                    return BISHOP_BLACK;
                case ROOK:
                    return ROOK_BLACK;
                case QUEEN:
                    return QUEEN_BLACK;
                case KING:
                    return KING_BLACK;
            }
        }

        throw new UnsupportedOperationException();
    }

    @JsonValue
    public ObjectNode toJson() {
        return jsonNode;
    }

    public boolean isPawn() {
        return type == PieceType.PAWN;
    }

    public boolean isKing() {
        return type == PieceType.KING;
    }

    public boolean isRook() {
        return type == PieceType.ROOK;
    }
}
