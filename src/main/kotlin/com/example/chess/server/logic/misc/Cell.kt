package com.example.chess.server.logic.misc

import com.example.chess.shared.Constants
import com.example.chess.shared.dto.CellDTO
import com.example.chess.shared.enums.Piece

/**
 * @author v.peschaniy
 *      Date: 23.07.2019
 */
class Cell private constructor(
    val point: Point,
    val piece: Piece?
) {

    val rowIndex get() = point.rowIndex

    val columnIndex get() = point.columnIndex

    fun toDTO() = CellDTO(point.toDTO(), piece)

    companion object {

        private val nullIndex = Piece.values().size

        private val cellsPool: Array<Array<Array<Cell>>> = Array(Constants.BOARD_SIZE) row@{ rowIndex ->
            Array(Constants.BOARD_SIZE) point@{ columnIndex ->
                val point = Point.valueOf(rowIndex, columnIndex)

                Array(Piece.values().size) cell@{ pieceOrdinal ->
                    if (pieceOrdinal == nullIndex) {
                        return@cell Cell(point, null)
                    }
                    return@cell Cell(point, Piece.values()[pieceOrdinal])
                }
            }
        }

        fun valueOf(rowIndex: Int, columnIndex: Int, piece: Piece? = null): Cell {
            return cellsPool[rowIndex][columnIndex][piece?.ordinal ?: nullIndex]
        }

        fun valueOf(point: Point, piece: Piece? = null) = valueOf(point.rowIndex, point.columnIndex, piece)
    }
}