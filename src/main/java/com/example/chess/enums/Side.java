package com.example.chess.enums;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

@Getter
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public enum Side {
    WHITE(1),
    BLACK(-1);

    int pawnMoveVector;

    public Side reverse() {
        if (this == WHITE) {
            return BLACK;
        }
        return WHITE;
    }
}
