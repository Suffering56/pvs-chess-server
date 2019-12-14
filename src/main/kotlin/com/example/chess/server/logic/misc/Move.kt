package com.example.chess.server.logic.misc

import com.example.chess.shared.api.IMove
import com.example.chess.shared.dto.MoveDTO
import com.example.chess.shared.enums.Piece

/**
 * @author v.peschaniy
 *      Date: 11.11.2019
 */
class Move(
    override val from: Point,
    override val to: Point,
    override val pawnTransformationPiece: Piece?
) : IMove {

    override fun toDTO() = MoveDTO(from.toDTO(), to.toDTO(), pawnTransformationPiece)

    companion object {
        fun of(move: MoveDTO) = Move(Point.of(move.from), Point.of(move.to), move.pawnTransformationPiece)

        fun cut(point: Point) = Move(point, point, null)
    }
}