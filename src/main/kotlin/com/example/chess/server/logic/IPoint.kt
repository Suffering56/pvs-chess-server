package com.example.chess.server.logic

import com.example.chess.shared.dto.PointDTO

/**
 * @author v.peschaniy
 *      Date: 11.11.2019
 */

interface IPoint {
    val row: Int
    val col: Int

    fun toDTO(): PointDTO

    fun isEqual(row: Int, col: Int): Boolean {
        return this.row == row && this.col == col
    }
}
