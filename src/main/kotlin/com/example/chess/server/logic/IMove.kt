package com.example.chess.shared.api

import com.example.chess.shared.dto.MoveDTO
import com.example.chess.shared.enums.PieceType

/**
 * @author v.peschaniy
 *      Date: 11.11.2019
 */
interface IMove {
    val from: IPoint
    val to: IPoint
    val pawnTransformationPieceType: PieceType?
    
    fun toDTO(): MoveDTO
}