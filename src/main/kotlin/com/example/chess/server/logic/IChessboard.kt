package com.example.chess.server.logic

import com.example.chess.shared.api.IPoint
import com.example.chess.shared.dto.ChessboardDTO
import com.example.chess.shared.enums.Piece

/**
 * @author v.peschaniy
 *      Date: 24.07.2019
 */
interface IChessboard {

    val position: Int

    fun getPieceNullable(from: IPoint): Piece?

    fun getPiece(from: IPoint): Piece

    fun toDTO(): ChessboardDTO
}