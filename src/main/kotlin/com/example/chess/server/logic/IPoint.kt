package com.example.chess.shared.api

import com.example.chess.shared.dto.PointDTO

/**
 * @author v.peschaniy
 *      Date: 11.11.2019
 */

interface IPoint {
    val row: Int
    val col: Int

    fun toDTO(): PointDTO

    fun compressToInt(): Int {
        return (row shl COMPRESS_POINT_OFFSET) + col
    }

    fun isEqual(row: Int, col: Int): Boolean {
        return this.row == row && this.col == col
    }

    companion object {
        const val COMPRESS_POINT_OFFSET = 3
    }
}
