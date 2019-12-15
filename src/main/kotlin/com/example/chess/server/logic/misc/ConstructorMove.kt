package com.example.chess.server.logic.misc

import com.example.chess.shared.api.IMove
import com.example.chess.shared.dto.MoveDTO
import com.example.chess.shared.dto.PointDTO
import com.example.chess.shared.enums.Piece

class ConstructorMove(
    override val from: Point,       // ignored
    override val to: Point,
    override val pawnTransformationPiece: Piece?
) : IMove {

    val pieceFrom get() = pawnTransformationPiece

    override fun toDTO(): MoveDTO {
        return MoveDTO(PointDTO(-1, -1), to.toDTO(), pieceFrom)
    }
}