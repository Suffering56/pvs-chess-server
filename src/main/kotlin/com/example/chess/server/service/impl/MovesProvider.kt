package com.example.chess.server.service.impl

import com.example.chess.server.logic.IChessboard
import com.example.chess.server.logic.IGame
import com.example.chess.server.logic.misc.Point
import com.example.chess.server.logic.misc.hasCommonVectorWith
import com.example.chess.server.logic.misc.isIndexOutOfBounds
import com.example.chess.server.service.IMovesProvider
import com.example.chess.shared.api.IPoint
import com.example.chess.shared.enums.Piece
import com.example.chess.shared.enums.PieceType
import com.example.chess.shared.enums.PieceType.*
import com.example.chess.shared.enums.Side
import org.eclipse.collections.api.tuple.Twin
import org.eclipse.collections.api.tuple.primitive.IntIntPair
import org.eclipse.collections.impl.tuple.Tuples
import org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples.pair
import org.springframework.stereotype.Component
import java.lang.Integer.signum
import java.util.*
import kotlin.collections.HashSet

/**
 * @author v.peschaniy
 *      Date: 20.11.2019
 */
@Component
class MovesProvider : IMovesProvider {

    companion object {
        internal val EMPTY_MOVES: Set<Point> = HashSet()
    }

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

        internal fun getAvailableMoves(): Set<Point> {
            if (pieceFrom.isKing()) {
                //ходы короля слишком непохожи на другие, т.к. нас уже неинтересуют текущие шахи, а так же препятствия
                return getKingMoves()
            }

            val kingAttackersPair = getKingAttackers()
            if (kingAttackersPair != null) {
                if (kingAttackersPair.two != null) {
                    //двойной шах, ходить можно только королем
                    return EMPTY_MOVES
                }
            }

            val kingAttacker = kingAttackersPair?.one
            val kingPossibleAttackerForObstacle = getPossibleKingAttackerForCurrentObstacle()

            return when (pieceFrom.type) {
                PAWN -> getPawnMoves(kingAttacker, kingPossibleAttackerForObstacle)
                KNIGHT -> getKnightMoves(kingAttacker, kingPossibleAttackerForObstacle)
                BISHOP -> getBishopMoves(kingAttacker, kingPossibleAttackerForObstacle)
                ROOK -> getRookMoves(kingAttacker, kingPossibleAttackerForObstacle)
                QUEEN -> getQueenMoves(kingAttacker, kingPossibleAttackerForObstacle)
                else -> throw UnsupportedOperationException("unsupported piece type: ${pieceFrom.type}")
            }
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
            val rowOffset = if (sideFrom == Side.WHITE) 1 else -1

            var row = kingPoint.row + rowOffset
            var col = kingPoint.col + 1

            val piece = chessboard.getPieceNullable(row, col)
            if (piece != null && piece.isPawn() && piece.side != sideFrom) {
                //короля не могут атаковать две пешки одновременно. поэтому сразу выходим
                return Point.of(col, row)
            }

            row = kingPoint.row + rowOffset
            col = kingPoint.col - 1

            if (piece != null && piece.isPawn() && piece.side != sideFrom) {
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

        /**
         * Предполагается что если идти от короля по заданному вектору, то сначала обязательно встретится pointFrom.
         * Если встретится что-то другое или конец доски, будет брошен IllegalArgumentException
         */
        private fun getKingThreatPointForObstacle(rowDirection: Int, colDirection: Int): Point? {
            var row: Int = kingPoint.row
            var col: Int = kingPoint.col
            var isPossibleObstacleFound = false

            while (true) {
                row += rowDirection
                col += colDirection

                if (isOutOfBoard(row, col)) {
                    if (isPossibleObstacleFound) {
                        // препятствие ни от кого не защищает. за ним по направлению вектора - конец доски. => можно ходить сколько душе угодно
                        return null
                    } else {
                        // доска кончилась, а мы не даже не нашли obstacle(pointFrom), значит был выбран плохой вектор поиска
                        throw IllegalArgumentException("moving piece(pieceFrom) is not an obstacle to the king, please use correct direction")
                    }
                }

                val piece = chessboard.getPieceNullable(row, col)

                if (piece != null) {
                    if (!isPossibleObstacleFound) {
                        if (pointFrom.isEqual(row, col)) {
                            // по заданным векторам нашлась перемещаемая фигура, которая может оказаться возможным препятствием
                            isPossibleObstacleFound = true
                        } else {
                            throw IllegalArgumentException("moving piece(pieceFrom) is not an obstacle to the king, please use correct direction")
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

        private fun getPossibleKingAttackerForCurrentObstacle(): Point? {
            if (pointFrom.hasCommonVectorWith(kingPoint)) {
                var rowDirection = 0
                var colDirection = 0

                when {
                    // horizontal vector
                    pointFrom.row == kingPoint.row -> colDirection = signum(pointFrom.col - kingPoint.col)
                    // vertical vector
                    pointFrom.col == kingPoint.col -> rowDirection = signum(pointFrom.row - kingPoint.row)
                    // diagonal vector
                    else -> {
                        colDirection = signum(pointFrom.col - kingPoint.col)
                        rowDirection = signum(pointFrom.row - kingPoint.row)
                    }
                }
                return getKingThreatPointForObstacle(rowDirection, colDirection)
            }

            return null
        }


        private fun getPawnMoves(kingAttacker: Point?, kingPossibleAttackerForObstacle: Point?): Set<Point> {
            TODO("NYI")
        }

        private fun getKnightMoves(kingAttacker: Point?, kingPossibleAttackerForObstacle: Point?): Set<Point> {
            TODO("NYI")
        }

        private fun getBishopMoves(kingAttacker: Point?, kingPossibleAttackerForObstacle: Point?): Set<Point> {
            TODO("NYI")
        }

        private fun getRookMoves(kingAttacker: Point?, kingPossibleAttackerForObstacle: Point?): Set<Point> {
            TODO("NYI")
        }


        private fun getMovesByDirections(
            directions: Set<IntIntPair>,
            kingAttacker: Point?,
            kingPossibleAttackerForObstacle: Point?
        ): Set<Point> {

            val result: MutableSet<Point> = HashSet()

            var row: Int = pointFrom.row
            var col: Int = pointFrom.col

            for (direction in directions) {
                while (true) {
                    row += direction.one
                    col += direction.two

                    if (isOutOfBoard(row, col)) {
                        break
                    }

                    val piece = chessboard.getPieceNullable(row, col)

                    if (piece == null) {
                        // здесь не занято - ход доступен
                        result.add(Point.of(row, col))
                        continue
                    }

                    if (piece.side != sideFrom) {
                        // рубим врага - текущая точка доступна для хода, но перепрыгнуть ее нельзя, поэтому делаем после break
                        result.add(Point.of(row, col))
                    }
                    // дальнейшее следование по вектору невозможно, потому что мы уперлись в фигуру(свою или чужую - не важно)
                    break
                }
            }


            while (true) {


                if (kingAttacker != null) {
                    //мы можем либо срубить атакующую фигуру, либо загородиться от нее
                    //иными словами, ходить можно только внутри диапазона [kingPoint -> attackerPoint) при векторном шахе,
                    //либо, если атакующий - конь [attackerPoint]
                }

                if (kingPossibleAttackerForObstacle != null) {
                    //мы можем ходить только внутри диапазона (pointFrom -> possibleAttackerPoint]
                    //для ускорения - я бы сделал проверку на совместимость фигур: например если obstacle - ладья, а нас атакует слон. то ходить нельзя.
                }

                TODO("NYI")
            }
        }

        private fun getQueenMoves(kingAttacker: Point?, kingPossibleAttackerForObstacle: Point?): Set<Point> {
            TODO("NYI")
        }

        private fun getKingMoves(): Set<Point> {
            TODO("NYI")
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