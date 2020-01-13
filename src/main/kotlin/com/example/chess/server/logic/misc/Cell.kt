package com.example.chess.server.logic.misc

import com.example.chess.shared.enums.Piece

/**
 * @author v.peschaniy
 *      Date: 13.01.2020
 */

data class Cell(
    val row: Int,
    val col: Int,
    val piece: Piece
)