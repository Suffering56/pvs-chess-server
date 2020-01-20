package com.example.chess.server.logic.misc

import com.example.chess.server.logic.IPoint
import com.example.chess.shared.ArrayCoub
import com.example.chess.shared.Constants.BOARD_SIZE
import com.example.chess.shared.enums.Piece

/**
 * @author v.peschaniy
 *      Date: 13.01.2020
 */

class Cell private constructor(
    val row: Int,
    val col: Int,
    val piece: Piece
) {
    val point: IPoint get() = Point.of(row, col)

    companion object {
        private val provider: ArrayCoub<Cell> = Array(BOARD_SIZE) { rowIndex ->
            Array(BOARD_SIZE) { columnIndex ->
                Array(Piece.values().size) { pieceOrdinal ->
                    val piece = Piece.values()[pieceOrdinal]
                    Cell(rowIndex, columnIndex, piece)
                }
            }
        }

        fun of(row: Int, col: Int, piece: Piece): Cell {
            checkBoardIndices(row, col)
            return provider[row][col][piece.ordinal]
        }

        fun of(point: IPoint, piece: Piece): Cell = of(point.row, point.col, piece)

        fun of(compressedPoint: Int, piece: Piece): Cell = of(Point.of(compressedPoint), piece)
    }
}