package com.example.chess.server.logic.misc

import com.example.chess.shared.Constants.BOARD_SIZE
import com.example.chess.shared.Constants.POINT_OFFSET
import com.example.chess.shared.api.IPoint
import com.example.chess.shared.dto.PointDTO

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

        private val pointsPool = Array(BOARD_SIZE) { rowIndex ->
            Array(BOARD_SIZE) { columnIndex ->
                Point(rowIndex, columnIndex)
            }
        }

        fun of(rowIndex: Int, columnIndex: Int): Point {
            checkBoardIndex(rowIndex) { "incorrect rowIndex=$rowIndex" }
            checkBoardIndex(columnIndex) { "incorrect columnIndex=$columnIndex" }

            return pointsPool[rowIndex][columnIndex]
        }

        fun of(dto: PointDTO) = of(dto.row, dto.col)

        private fun checkBoardIndex(index: Int, lazyMessage: () -> Any) {
            if (isIndexOutOfBounds(index)) {
                throw IndexOutOfBoundsException(lazyMessage().toString())
            }
        }
    }

    //TODO: возможно пригодится в будущем
    fun Int.toPoint(): Point {
        val rowIndex = this shr POINT_OFFSET
        val columnIndex = this - (rowIndex shl POINT_OFFSET)
        return of(rowIndex, columnIndex)
    }
}

fun main() {
    val x: Short = 1 shl 3
    print(x)
}