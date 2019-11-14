package com.example.chess.server.logic.misc

import com.example.chess.shared.api.IMove
import com.example.chess.shared.dto.MoveDTO
import com.example.chess.shared.enums.PieceType

/**
 * @author v.peschaniy
 *      Date: 11.11.2019
 */
class Move(
    override val from: Point,
    override val to: Point,
    override val pawnTransformationPieceType: PieceType?
) : IMove {

    constructor(move: MoveDTO) : this(Point.of(move.from), Point.of(move.to), move.pawnTransformationPieceType)

    override fun toDTO() = MoveDTO(from.toDTO(), to.toDTO(), pawnTransformationPieceType)
}