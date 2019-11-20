package com.example.chess.server.service.impl

import com.example.chess.server.logic.IChessboard
import com.example.chess.server.logic.IGame
import com.example.chess.server.logic.misc.Point
import com.example.chess.server.logic.misc.isIndexOutOfBounds
import com.example.chess.server.service.IMovesProvider
import com.example.chess.shared.api.IPoint
import com.example.chess.shared.enums.Piece
import com.example.chess.shared.enums.Piece.*
import com.example.chess.shared.enums.PieceType
import com.example.chess.shared.enums.PieceType.*
import com.example.chess.shared.enums.Side
import com.google.common.collect.Iterables
import org.eclipse.collections.api.set.primitive.IntSet
import org.eclipse.collections.api.tuple.primitive.IntIntPair
import org.eclipse.collections.impl.set.mutable.primitive.IntHashSet
import org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples
import org.springframework.stereotype.Component
import java.lang.UnsupportedOperationException
import java.util.*
import java.util.stream.Collectors
import java.util.stream.Stream
import kotlin.collections.HashSet

/**
 * @author v.peschaniy
 *      Date: 20.11.2019
 */
@Component
class MovesProvider : IMovesProvider {

    //TODO: longPawnMoveIndex надо обрабатывать где-то здесь
    override fun getAvailableMoves(game: IGame, chessboard: IChessboard, point: IPoint): Set<Point> {
        return InternalContext(game, chessboard, point).getAvailableMoves()
//
//        return setOf(
//            Point.of(0, 0),
//            Point.of(4, 3),
//            Point.of(4, 4),
//            Point.of(4, 5),
//            Point.of(4, 6),
//            Point.of(5, 2)
//        )
    }

    private class InternalContext(
        val game: IGame,
        val chessboard: IChessboard,
        val pointFrom: IPoint,
        val pieceFrom: Piece = chessboard.getPiece(pointFrom),
        val sideFrom: Side = pieceFrom.side,
        val kingPoint: IPoint = chessboard.getKingPoint(sideFrom)
    ) {
        companion object {
            val pieceVectorsMap = EnumMap<PieceType, Set<IntIntPair>>(PieceType::class.java)

            init {
                pieceVectorsMap[ROOK] = setOf(
                    PrimitiveTuples.pair(1, 0),
                    PrimitiveTuples.pair(-1, 0),
                    PrimitiveTuples.pair(0, 1),
                    PrimitiveTuples.pair(0, -1)
                )
                pieceVectorsMap[BISHOP] = setOf(
                    PrimitiveTuples.pair(1, 1),
                    PrimitiveTuples.pair(1, -1),
                    PrimitiveTuples.pair(-1, 1),
                    PrimitiveTuples.pair(-1, -1)
                )
                pieceVectorsMap[QUEEN] = setOf(
                    PrimitiveTuples.pair(1, 1),
                    PrimitiveTuples.pair(1, -1),
                    PrimitiveTuples.pair(-1, 1),
                    PrimitiveTuples.pair(-1, -1),

                    PrimitiveTuples.pair(1, 1),
                    PrimitiveTuples.pair(1, -1),
                    PrimitiveTuples.pair(-1, 1),
                    PrimitiveTuples.pair(-1, -1)
                )

            }
        }

        val set: IntSet = IntHashSet()

        internal fun getAvailableMoves(): Set<Point> {
            if (pieceFrom.isKing()) {
                return getKingMoves()
            }

            when (pieceFrom.type) {
                PAWN -> TODO()
                KNIGHT -> TODO()
                BISHOP -> TODO()
                ROOK -> TODO()
                QUEEN -> TODO()
                else -> throw UnsupportedOperationException("unsupported piece type: ${pieceFrom.type}")
            }


            getPotentialAttackerPiece()
            getCurrentAttackerPiece()


            if (isKingObstacle() && !canDefendKing()) {
                return HashSet()
            }
            if (isKingUnderCheck() && !canDefendKing()) {
                return HashSet()
            }
        }


        private fun getPotentialAttackerPiece() {
            //0) if pieceFrom != KING
            //1) сначала найти вектор по которому pointFrom может являться препятствием (vector or null)
            //2) если вектор есть
            //3) getKingThreatPointForObstacle()
        }

        private fun getPawnMoves() {

        }

        private fun getKnightMoves() {

        }

        private fun getBishopMoves() {

        }

        private fun getRookMoves() {

        }

        private fun getQueenMoves() {

        }

        private fun getKingMoves(): Set<Point> {
            TODO("NYI")
        }

        private fun getKingThreatPointForObstacle(rowDirection: Int, colDirection: Int): Point? {
            var row: Int = kingPoint.row
            var col: Int = kingPoint.col
            var isPossibleObstacleFound = false

            while (true) {
                row += rowDirection
                col += colDirection

                if (isIndexOutOfBounds(row) || isIndexOutOfBounds(col)) {
                    // угроза до сих пор не найдена, а доска кончилась
                    return null
                }

                val piece = chessboard.getPieceNullable(row, col)

                if (piece != null) {
                    if (!isPossibleObstacleFound) {
                        if (pointFrom.isEqual(row, col)) {
                            // по заданным векторам нашлась перемещаемая фигура, которая может оказаться возможным препятствием
                            isPossibleObstacleFound = true
                        } else {
                            // нас уже атакуют  TODO: возможно стоит кидать UnsupportedOperationException
                            return null
                        }
                    } else {
                        return if (piece.side == sideFrom) {
                            // два союзных препятствия = защита от любых угроз. можно ходить любым из obstacle
                            null
                        } else {
                            // оно! вражеская фигура, которая срубит нашего короля, если мы уберем obstacle
                            Point.of(row, col)
                        }
                    }
                }
            }
        }

        private fun nextPieceByVector(rowDirection: Int, colDirection: Int, sourcePoint: IPoint): Piece? {
            var row: Int = sourcePoint.row
            var col: Int = sourcePoint.col

            while (true) {
                row += rowDirection
                col += colDirection

                if (isIndexOutOfBounds(row) || isIndexOutOfBounds(col)) {
                    return null
                }

                val piece = chessboard.getPieceNullable(row, col)
                if (piece != null) {
                    return piece
                }
            }
        }
    }
}