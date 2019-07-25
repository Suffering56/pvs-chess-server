package com.example.chess.server.logic

import com.example.chess.server.entity.History
import com.example.chess.server.logic.misc.Point
import com.example.chess.shared.ArrayTable
import com.example.chess.shared.Constants.BOARD_SIZE
import com.example.chess.shared.dto.CellDTO
import com.example.chess.shared.dto.ChessboardDTO
import com.example.chess.shared.dto.PointDTO
import com.example.chess.shared.enums.Piece
import com.example.chess.shared.enums.PieceType
import com.example.chess.shared.enums.Side

/**
 * @author v.peschaniy
 *      Date: 23.07.2019
 */
open class Chessboard protected constructor(
    val position: Int,
    protected val matrix: ArrayTable<Piece?>,     //TODO: AbstractChessboard
    protected val kingPoints: Map<Side, Point>
) : IChessboard {

    override fun toDTO(): ChessboardDTO {
        val matrixDto = Array(BOARD_SIZE) row@{ rowIndex ->
            val row = matrix[rowIndex]
            Array(BOARD_SIZE) cell@{ columnIndex ->
                val piece = row[columnIndex]
                CellDTO(PointDTO(rowIndex, columnIndex), piece)
            }
        }

        return ChessboardDTO(position, matrixDto, null)
    }

    override fun toMutable(): IMutableChessboard {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    fun getCell(rowIndex: Int, columnIndex: Int) {

    }

    companion object {

        private fun generate(position: Int, pieceGenerator: (Int, Int) -> Piece?): Chessboard {
            val kingPoints = mutableMapOf<Side, Point>()

            val matrix = Array(BOARD_SIZE) row@{ rowIndex ->
                Array(BOARD_SIZE) cell@{ columnIndex ->

                    val piece = pieceGenerator(rowIndex, columnIndex)

                    if (piece != null && piece.isKing()) {
                        kingPoints[piece.side] = Point.valueOf(rowIndex, columnIndex)
                    }

                    piece
                }
            }

            return Chessboard(position, matrix, kingPoints)
        }

        private fun initialChessboardGenerator() = gen@{ rowIndex: Int, columnIndex: Int ->
            val side = when (rowIndex) {
                0, 1 -> Side.WHITE
                6, 7 -> Side.BLACK
                else -> return@gen null
            }

            val pieceType = when (rowIndex) {
                1, 6 -> PieceType.PAWN
                else -> when (columnIndex) {
                    0, 7 -> PieceType.ROOK
                    1, 6 -> PieceType.KNIGHT
                    2, 5 -> PieceType.BISHOP
                    3 -> PieceType.KING
                    4 -> PieceType.QUEEN
                    else -> throw UnsupportedOperationException("incorrect columnIndex=$columnIndex")
                }
            }

            return@gen Piece.of(side, pieceType)
        }

        fun byInitial(): IChessboard {
            return generate(0, initialChessboardGenerator())
        }

        fun byHistory(history: List<History>): IChessboard {
            return generate(0, initialChessboardGenerator())
        }
    }
}