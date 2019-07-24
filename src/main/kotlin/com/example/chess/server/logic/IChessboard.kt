package com.example.chess.server.logic

import com.example.chess.shared.dto.ChessboardDTO

/**
 * @author v.peschaniy
 *      Date: 24.07.2019
 */
interface IChessboard {

    fun toMutable(): IMutableChessboard

    fun toDTO(): ChessboardDTO
}