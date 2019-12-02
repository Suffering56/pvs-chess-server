package com.example.chess.server.service.impl

import com.example.chess.server.logic.IChessboard
import com.example.chess.server.logic.IGame
import com.example.chess.server.logic.misc.Point
import com.example.chess.server.logic.misc.hasCommonVectorWith
import com.example.chess.server.logic.misc.isBorderedWith
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
import java.lang.Integer.*
import java.util.*
import kotlin.collections.HashSet
import kotlin.math.abs

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
            return getMovesByOffsets(knightOffsets, kingAttacker, kingPossibleAttackerForObstacle)
        }

        private fun getBishopMoves(kingAttacker: Point?, kingPossibleAttackerForObstacle: Point?): Set<Point> {
            return getMovesByDirections(pieceVectorsMap[BISHOP]!!, kingAttacker, kingPossibleAttackerForObstacle)
        }

        private fun getRookMoves(kingAttacker: Point?, kingPossibleAttackerForObstacle: Point?): Set<Point> {
            return getMovesByDirections(pieceVectorsMap[ROOK]!!, kingAttacker, kingPossibleAttackerForObstacle)
        }

        private fun getQueenMoves(kingAttacker: Point?, kingPossibleAttackerForObstacle: Point?): Set<Point> {
            return getMovesByDirections(pieceVectorsMap[QUEEN]!!, kingAttacker, kingPossibleAttackerForObstacle)
        }

        private fun getMovesByOffsets(
            offsets: Set<IntIntPair>,
            kingAttacker: Point?,
            kingPossibleAttackerForObstacle: Point?
        ): Set<Point> {
            val result: MutableSet<Point> = HashSet()

            var row: Int = pointFrom.row
            var col: Int = pointFrom.col

            for (offset in offsets) {
                row += offset.one
                col += offset.two

                if (isOutOfBoard(row, col)) {
                    continue
                }

                val piece = chessboard.getPieceNullable(row, col)

                if (piece == null) {
                    // свободная точка - ход доступен (если пройдет проверки)
                    if (isAvailableMove(row, col, kingAttacker, kingPossibleAttackerForObstacle)) {
                        result.add(Point.of(row, col))
                    }
                } else if (piece.side != sideFrom) {
                    // рубим врага (если ход пройдет проверки)
                    if (isDestroyingThreatMove(row, col, kingAttacker, kingPossibleAttackerForObstacle)) {
                        result.add(Point.of(row, col))
                    }
                }
            }

            return result
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
                        // свободная точка - ход доступен (если пройдет проверки)
                        if (isAvailableMove(row, col, kingAttacker, kingPossibleAttackerForObstacle)) {
                            result.add(Point.of(row, col))
                        }
                        continue
                    }

                    if (piece.side != sideFrom) {
                        // рубим врага - текущая точка доступна для хода (если пройдет проверки), но перепрыгнуть ее нельзя, поэтому делаем после break
                        if (isDestroyingThreatMove(row, col, kingAttacker, kingPossibleAttackerForObstacle)) {
                            result.add(Point.of(row, col))
                        }
                    }
                    // дальнейшее следование по вектору невозможно, потому что мы уперлись в фигуру(свою или чужую - не важно)
                    break
                }
            }

            return result
        }

        private fun isDestroyingThreatMove(rowTo: Int, colTo: Int, kingAttacker: Point?, kingPossibleAttackerForObstacle: Point?): Boolean {
            if (kingAttacker != null && kingPossibleAttackerForObstacle != null) {
                return false
            }

            if (kingAttacker != null) {
                return kingAttacker.isEqual(rowTo, colTo)
            }

            if (kingPossibleAttackerForObstacle != null) {
                return kingPossibleAttackerForObstacle.isEqual(rowTo, colTo)
            }

            return true
        }

        private fun isAvailableMove(rowTo: Int, colTo: Int, kingAttacker: Point?, kingPossibleAttackerForObstacle: Point?): Boolean {
            if (kingAttacker != null && kingPossibleAttackerForObstacle != null) {
                // нам шах, но при этом текущая фигура еще и является припятствием. в таком случае:
                // вектора '1: (kingPoint -> kingAttacker]' и '2: (kingPoint -> kingPossibleAttackerForObstacle]' не могут лежать в одном направлении,
                // соответственно мы не можем оставшись в рамках вектора 1, заслониться от фигуры из вектора 2, или срубить ее.

                // PS: единственный ход, где мы рубим фигуру, но при этом не встаем на ее место - взятие на проходе,
                // но оно тоже не сможет помочь, т.к. шах королю пешка может сделать только по диагонали, и если такое было, то после взятия
                // наша пешка окажется по отношению к королю буквой Г, а такое положение не может прикрыть ни от какого векторного шаха
                return false
            }

            if (kingAttacker != null) {
                return canDefendKing(rowTo, colTo, kingAttacker)
            }

            if (kingPossibleAttackerForObstacle != null) {
                return canMoveObstacle(rowTo, colTo, kingPossibleAttackerForObstacle)
            }

            // ничем не ограниченный ход: kingAttacker == null && kingPossibleAttackerForObstacle == null
            return true
        }

        private fun canMoveObstacle(rowTo: Int, colTo: Int, kingPossibleAttackerForObstacle: Point): Boolean {
            if (kingPossibleAttackerForObstacle.isEqual(rowTo, colTo)) {
                // рубим фигуру, из-за которой перемещаемая фигура являлась obstacle
                return true
            }
            // перемещение в рамках вектора допустимо
            return canBeObstacle(rowTo, colTo, kingPossibleAttackerForObstacle)
        }

        /**
         * return может ли данный ход защитить короля от шаха, источником которого являеится kingAttacker
         *
         * Защитить короля можно срубив атакующую фигуру, либо закрыться (если возможно)
         */
        private fun canDefendKing(rowTo: Int, colTo: Int, kingAttacker: Point): Boolean {
            if (kingAttacker.isEqual(rowTo, colTo)) {
                // рубим фигуру, объявившую шах.
                return true
            }

            if (kingAttacker.isBorderedWith(rowTo, colTo)) {
                // фигура, объявившая шах стоит вплотную к нашему королю, но при этом мы ее не рубим. такой ход недопустим
                return false
            }

            val kingAttackerPiece = chessboard.getPiece(kingAttacker)

            if (kingAttackerPiece.type == KNIGHT) {
                // нам объявили шах или конем
                // мы его не рубим, а загородиться от коня невозможно. false
                return false
            }

            return canBeObstacle(rowTo, colTo, kingAttacker)
        }

        private fun canBeObstacle(rowTo: Int, colTo: Int, kingThreat: Point): Boolean {
            if (!kingThreat.hasCommonVectorWith(rowTo, colTo)) {
                // данный ход находится где-то сбоку от вектора шаха и никак не защищает короля
                return false
            }

            return when {
                // horizontal vector
                kingPoint.row == kingThreat.row -> rowTo == kingPoint.row && between(kingPoint.col, colTo, kingThreat.col)
                // vertical vector
                kingPoint.col == kingThreat.col -> colTo == kingPoint.col && between(kingPoint.row, rowTo, kingThreat.row)
                // diagonal vector
                else -> {
                    val rowOffset = kingThreat.row - kingPoint.row
                    val colOffset = kingThreat.col - kingPoint.col

                    val absAttackerRowOffset = abs(rowOffset)
                    check(absAttackerRowOffset == abs(colOffset)) {
                        "kingThreat($kingThreat) has different diagonal vector with kingPoint($kingPoint)"
                    }

                    return between(kingPoint.row, rowTo, kingThreat.row)
                            && between(kingPoint.col, colTo, kingThreat.col)
                            // moveTo доложен быть на одной диагонали с атакуемым королем
                            && abs(rowTo - kingPoint.row) == abs(colTo - kingPoint.col)
                }
            }
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

        private fun between(fromExclusive: Int, checkedValue: Int, toExclusive: Int): Boolean {
            return checkedValue > min(fromExclusive, toExclusive)
                    && checkedValue < (max(fromExclusive, toExclusive))
        }
    }
}
//
//fun main() {
//    val from = 0
//    val to = 8
//
//    val checked = -4
//
//    val between = checked > min(from, to) && checked < (max(from, to))
//
////    val between = x in to..from
//    println("between = ${between}")
//}
