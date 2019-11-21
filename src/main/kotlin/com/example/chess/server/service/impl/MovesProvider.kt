package com.example.chess.server.service.impl

import com.example.chess.server.logic.IChessboard
import com.example.chess.server.logic.IGame
import com.example.chess.server.logic.misc.Point
import com.example.chess.server.logic.misc.isIndexOutOfBounds
import com.example.chess.server.service.IMovesProvider
import com.example.chess.shared.api.IPoint
import com.example.chess.shared.enums.Piece
import com.example.chess.shared.enums.PieceType
import com.example.chess.shared.enums.PieceType.*
import com.example.chess.shared.enums.Side
import org.eclipse.collections.api.set.primitive.IntSet
import org.eclipse.collections.api.tuple.Twin
import org.eclipse.collections.api.tuple.primitive.IntIntPair
import org.eclipse.collections.impl.set.mutable.primitive.IntHashSet
import org.eclipse.collections.impl.tuple.Tuples
import org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples.pair
import org.springframework.stereotype.Component
import java.lang.UnsupportedOperationException
import java.util.*
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
            val knightOffsets = setOf(
                pair(1, 2),
                pair(2, 1),
                pair(1, -2),
                pair(2, -1),

                pair(-1, 2),
                pair(-2, 1),
                pair(-1, -2),
                pair(-2, -1)
            )

            init {
                pieceVectorsMap[ROOK] = setOf(
                    pair(1, 0),
                    pair(-1, 0),
                    pair(0, 1),
                    pair(0, -1)
                )
                pieceVectorsMap[BISHOP] = setOf(
                    pair(1, 1),
                    pair(1, -1),
                    pair(-1, 1),
                    pair(-1, -1)
                )
                pieceVectorsMap[QUEEN] = setOf(
                    //ROOK
                    pair(1, 1),
                    pair(1, -1),
                    pair(-1, 1),
                    pair(-1, -1),
                    //BISHOP
                    pair(1, 1),
                    pair(1, -1),
                    pair(-1, 1),
                    pair(-1, -1)
                )
                pieceVectorsMap[KING] = pieceVectorsMap[QUEEN]

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

        private fun getKingAttackers(): Twin<Point>? {
            var result: Twin<Point>? = findVectorPieceThreatsToKing()

            if (result != null && result.two != null) {
                return result
            }

            val knightAttacker = findKnightThreatsToKing()

            if (knightAttacker != null) {
                if (result == null) {
                    result = Tuples.twin(knightAttacker, null)
                } else {
                    //тройной шах в этой игре невозможен
                    return Tuples.twin(result.one, knightAttacker)
                }
            }

            val pawnAttacker = findPawnThreatsToKing()

            if (pawnAttacker != null) {
                return if (result == null) {
                    Tuples.twin(pawnAttacker, null)
                } else {
                    //тройной шах в этой игре невозможен
                    Tuples.twin(result.one, pawnAttacker)
                }
            }

            return result
        }


        private fun findPawnThreatsToKing(): Point? {
            val rowOffset = if (sideFrom == Side.WHITE) 1 else -1   //TODO: to set[side]

            var row = kingPoint.row + rowOffset
            var col = kingPoint.col + 1

            val piece = chessboard.getPieceNullable(row, col)
            if (piece != null && piece.side != sideFrom && piece.isPawn()) {
                return Point.of(col, row)
            }

            row = kingPoint.row + rowOffset
            col = kingPoint.col - 1

            if (piece != null && piece.side != sideFrom && piece.isPawn()) {
                return Point.of(col, row)
            }

            return null
        }

        private fun findKnightThreatsToKing(): Point? {
            for (offset in knightOffsets) {
                val row = kingPoint.row + offset.one
                val col = kingPoint.col + offset.two

                if (isOutOfBoard(row, col)) {
                    continue
                }

                val foundPiece = chessboard.getPieceNullable(row, col)

                if (foundPiece != null && foundPiece.side != sideFrom && foundPiece.type == KNIGHT) {
                    //сразу выходим, потому что двойного шаха от двух коней быть не может
                    return Point.of(row, col)
                }
            }
            return null
        }

        private fun findVectorPieceThreatsToKing(): Twin<Point>? {
            var result: Twin<Point>? = null

            val allPossibleAttackerVectors = pieceVectorsMap[QUEEN]!!

            for (vector in allPossibleAttackerVectors) {
                val notEmptyPoint = nextPieceByVector(vector.one, vector.two, kingPoint)
                    ?: continue // ничего не нашли и уперлись в край доски

                val foundPiece = chessboard.getPiece(notEmptyPoint)
                if (foundPiece.side == sideFrom || !isVectorPiece(foundPiece.type)) {
                    //мы ищем только вражеского слона/ладью/ферзя
                    continue
                }

                result = if (result == null) {
                    Tuples.twin(notEmptyPoint, null)
                } else {
                    //двойной шах (оч редкий кейс)
                    //тройной шах в этой игре невозможен
                    return Tuples.twin(result.one, notEmptyPoint)
                }
            }

            return result
        }

        private fun isVectorPiece(pieceType: PieceType): Boolean {
            return when (pieceType) {
                BISHOP -> true
                ROOK -> true
                QUEEN -> true
                else -> false
            }
        }

        private fun getKingThreatPointForObstacle(rowDirection: Int, colDirection: Int): Point? {
            var row: Int = kingPoint.row
            var col: Int = kingPoint.col
            var isPossibleObstacleFound = false

            while (true) {
                row += rowDirection
                col += colDirection

                if (isOutOfBoard(row, col)) {
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

        private fun nextPieceByVector(rowDirection: Int, colDirection: Int, sourcePoint: IPoint): Point? {
            var row: Int = sourcePoint.row
            var col: Int = sourcePoint.col

            while (true) {
                row += rowDirection
                col += colDirection

                if (isOutOfBoard(row, col)) {
                    return null
                }

                val piece = chessboard.getPieceNullable(row, col)
                if (piece != null) {
                    return Point.of(row, col)
                }
            }
        }

        private fun isOutOfBoard(row: Int, col: Int) = isIndexOutOfBounds(row) || isIndexOutOfBounds(col)
    }
}