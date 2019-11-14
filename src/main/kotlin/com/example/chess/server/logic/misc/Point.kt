package com.example.chess.server.logic.misc

import com.example.chess.shared.Constants.BOARD_SIZE
import com.example.chess.shared.api.IPoint
import com.example.chess.shared.dto.PointDTO
import com.google.common.collect.Range

/**
 * @author v.peschaniy
 *      Date: 22.07.2019
 */
class Point private constructor(
    override val row: Int,
    override val col: Int
) : IPoint {

    override fun toDTO() = PointDTO(row, col)

    companion object {

        private val availableIndexesRange = Range.closedOpen(0, BOARD_SIZE)

        private val pointsPool = Array(BOARD_SIZE) { rowIndex ->
            Array(BOARD_SIZE) { columnIndex ->
                Point(rowIndex, columnIndex)
            }
        }

        fun of(rowIndex: Int, columnIndex: Int): Point {
            require(availableIndexesRange.contains(rowIndex)) { "incorrect rowIndex=$rowIndex" }
            require(availableIndexesRange.contains(columnIndex)) { "incorrect columnIndex=$columnIndex" }

            return pointsPool[rowIndex][columnIndex]
        }

        fun of(dto: PointDTO) = of(dto.row, dto.col)
    }
}