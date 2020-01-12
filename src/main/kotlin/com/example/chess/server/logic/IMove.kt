package com.example.chess.server.logic

import com.example.chess.shared.api.IPoint
import com.example.chess.shared.dto.MoveDTO
import com.example.chess.shared.enums.Piece

/**
 * @author v.peschaniy
 *      Date: 11.11.2019
 */
interface IMove {
    val from: IPoint
    val to: IPoint
    val pawnTransformationPiece: Piece?

    fun toDTO(): MoveDTO
    fun isCut() = from == to
}