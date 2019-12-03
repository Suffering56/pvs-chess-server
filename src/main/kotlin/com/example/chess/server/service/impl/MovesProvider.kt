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

    override fun getAvailableMoves(pointFrom: IPoint, chessboard: IChessboard, game: IGame): Set<Point> {
        val context = InternalContext(game, chessboard, pointFrom)
        return getAvailableMoves(context)
    }

    override fun isUnderCheck(kingSide: Side, chessboard: IChessboard): Boolean {
        val kingPoint = chessboard.getKingPoint(kingSide)
        return findKingAttackers(kingPoint, kingSide, chessboard) != null
    }

    private fun getAvailableMoves(ctx: InternalContext): Set<Point> {
        if (ctx.pieceFrom.isKing()) {
            //ходы короля слишком непохожи на другие, т.к. нас уже неинтересуют текущие шахи, а так же препятствия
            return getKingMoves(ctx)
        }

        val kingAttackersPair = findKingAttackers(ctx.kingPoint, ctx.sideFrom, ctx.chessboard)
        if (kingAttackersPair != null) {
            if (kingAttackersPair.two != null) {
                //двойной шах, ходить можно только королем
                return EMPTY_MOVES
            }
        }

        val kingAttacker = kingAttackersPair?.one
        val kingPossibleAttackerForObstacle = getPossibleKingAttackerForCurrentObstacle(ctx)

        return when (ctx.pieceFrom.type) {
            PAWN -> getPawnMoves(ctx, kingAttacker, kingPossibleAttackerForObstacle)
            KNIGHT -> getKnightMoves(ctx, kingAttacker, kingPossibleAttackerForObstacle)
            BISHOP -> getBishopMoves(ctx, kingAttacker, kingPossibleAttackerForObstacle)
            ROOK -> getRookMoves(ctx, kingAttacker, kingPossibleAttackerForObstacle)
            QUEEN -> getQueenMoves(ctx, kingAttacker, kingPossibleAttackerForObstacle)
            else -> throw UnsupportedOperationException("unsupported piece type: ${ctx.pieceFrom.type}")
        }
    }

    private fun findKingAttackers(kingPoint: IPoint, kingSide: Side, chessboard: IChessboard): Twin<Point>? {
        var result: Twin<Point>? = findVectorPieceThreatsToKing(kingPoint, kingSide, chessboard)

        if (result != null && result.two != null) {
            return result
        }

        val knightAttacker = findKnightThreatsToKing(kingPoint, kingSide, chessboard)

        if (knightAttacker != null) {
            if (result == null) {
                result = Tuples.twin(knightAttacker, null)
            } else {
                //тройной шах в этой игре невозможен
                return Tuples.twin(result.one, knightAttacker)
            }
        }

        val pawnAttacker = findPawnThreatsToKing(kingPoint, kingSide, chessboard)

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

    /**
     * Предполагается что если идти от короля по заданному вектору, то сначала обязательно встретится pointFrom.
     * Если встретится что-то другое или конец доски, будет брошен IllegalArgumentException
     */
    private fun getKingThreatPointForObstacle(rowDirection: Int, colDirection: Int, ctx: InternalContext): Point? {
        var row: Int = ctx.kingPoint.row
        var col: Int = ctx.kingPoint.col
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

            val piece = ctx.getPieceNullable(row, col)

            if (piece != null) {
                if (!isPossibleObstacleFound) {
                    if (ctx.pointFrom.isEqual(row, col)) {
                        // по заданным векторам нашлась перемещаемая фигура, которая может оказаться возможным препятствием
                        isPossibleObstacleFound = true
                    } else {
                        throw IllegalArgumentException("moving piece(pieceFrom) is not an obstacle to the king, please use correct direction")
                    }
                } else {
                    return if (piece.side == ctx.sideFrom) {
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

    private fun getPossibleKingAttackerForCurrentObstacle(ctx: InternalContext): Point? {
        if (ctx.pointFrom.hasCommonVectorWith(ctx.kingPoint)) {
            var rowDirection = 0
            var colDirection = 0

            when {
                // horizontal vector
                ctx.pointFrom.row == ctx.kingPoint.row -> colDirection = signum(ctx.pointFrom.col - ctx.kingPoint.col)
                // vertical vector
                ctx.pointFrom.col == ctx.kingPoint.col -> rowDirection = signum(ctx.pointFrom.row - ctx.kingPoint.row)
                // diagonal vector
                else -> {
                    colDirection = signum(ctx.pointFrom.col - ctx.kingPoint.col)
                    rowDirection = signum(ctx.pointFrom.row - ctx.kingPoint.row)
                }
            }
            return getKingThreatPointForObstacle(rowDirection, colDirection, ctx)
        }

        return null
    }

    private fun getKingMoves(ctx: InternalContext): Set<Point> {
        check(ctx.kingPoint == ctx.pointFrom) {
            "king expected in point from: $ctx.pointFrom, but found in $ctx.kingPoint"
        }

        val result: MutableSet<Point> = HashSet()
        val kingMoveOffsets = pieceVectorsMap[QUEEN]!!
        val enemyKingPoint = ctx.chessboard.getKingPoint(ctx.enemySide)

        // try add simple king moves
        for (offset in kingMoveOffsets) {
            val rowTo = ctx.kingPoint.row + offset.one
            val colTo = ctx.kingPoint.col + offset.two

            if (isAvailableKingMove(rowTo, colTo, enemyKingPoint, ctx)) {
                result.add(Point.of(rowTo, colTo))
            }
        }

        // try add castling moves
        // под шахом рокировка невозможна
        if (findKingAttackers(ctx.kingPoint, ctx.sideFrom, ctx.chessboard) == null) {
            val sideFeatures = ctx.game.getSideFeatures(ctx.sideFrom)

            if (sideFeatures.longCastlingAvailable) {
                tryAddCastlingMove(ctx.kingPoint, result, 1)
            }

            if (sideFeatures.shortCastlingAvailable) {
                tryAddCastlingMove(ctx.kingPoint, result, -1)
            }
        }

        return result
    }

    private fun getPawnMoves(ctx: InternalContext, kingAttacker: Point?, kingPossibleAttackerForObstacle: Point?): Set<Point> {
        val result: MutableSet<Point> = HashSet()

        val rowOffset = ctx.sideFrom.pawnRowDirection

        var rowTo = ctx.pointFrom.row + rowOffset * 2
        var colTo = ctx.pointFrom.col

        if (ctx.sideFrom.pawnInitialRow == ctx.pointFrom.row) {
            //first pawn long distance move
            tryAddPawnMove(rowTo, colTo, result, kingAttacker, kingPossibleAttackerForObstacle, ctx)
        }

        rowTo = ctx.pointFrom.row + rowOffset
        tryAddPawnMove(rowTo, colTo, result, kingAttacker, kingPossibleAttackerForObstacle, ctx)

        colTo = ctx.pointFrom.col + 1
        // simple attack
        tryAddPawnMove(rowTo, colTo, result, kingAttacker, kingPossibleAttackerForObstacle, ctx)
        // en passant
        tryAddPawnEnPassantMove(rowTo, colTo, result, kingAttacker, kingPossibleAttackerForObstacle, ctx)

        colTo = ctx.pointFrom.col - 1
        // simple attack
        tryAddPawnMove(rowTo, colTo, result, kingAttacker, kingPossibleAttackerForObstacle, ctx)
        // en passant
        tryAddPawnEnPassantMove(rowTo, colTo, result, kingAttacker, kingPossibleAttackerForObstacle, ctx)

        return result
    }

    private fun getKnightMoves(ctx: InternalContext, kingAttacker: Point?, kingPossibleAttackerForObstacle: Point?): Set<Point> {
        return getMovesByOffsets(knightOffsets, kingAttacker, kingPossibleAttackerForObstacle, ctx)
    }

    private fun getBishopMoves(ctx: InternalContext, kingAttacker: Point?, kingPossibleAttackerForObstacle: Point?): Set<Point> {
        return getMovesByDirections(pieceVectorsMap[BISHOP]!!, kingAttacker, kingPossibleAttackerForObstacle, ctx)
    }

    private fun getRookMoves(ctx: InternalContext, kingAttacker: Point?, kingPossibleAttackerForObstacle: Point?): Set<Point> {
        return getMovesByDirections(pieceVectorsMap[ROOK]!!, kingAttacker, kingPossibleAttackerForObstacle, ctx)
    }

    private fun getQueenMoves(ctx: InternalContext, kingAttacker: Point?, kingPossibleAttackerForObstacle: Point?): Set<Point> {
        return getMovesByDirections(pieceVectorsMap[QUEEN]!!, kingAttacker, kingPossibleAttackerForObstacle, ctx)
    }

    private fun tryAddCastlingMove(kingPoint: IPoint, result: MutableSet<Point>, direction: Int) {
        val crossPoint = Point.of(kingPoint.row, kingPoint.col + 1 * direction)

        // рокировка через битое поле так же невозможна
        if (result.contains(crossPoint)) {
            result.add(Point.of(kingPoint.row, kingPoint.col + 2 * direction))
        }
    }

    private fun isAvailableKingMove(
        rowTo: Int,
        colTo: Int,
        enemyKingPoint: IPoint,
        ctx: InternalContext
    ): Boolean {
        if (isOutOfBoard(rowTo, colTo)) {
            // нельзя вставать за пределы доски
            return false
        }

        if (enemyKingPoint.isBorderedWith(rowTo, colTo)) {
            // нельзя вставать вплотную к вражескому королю
            return false
        }

        if (findKingAttackers(Point.of(rowTo, colTo), ctx.sideFrom, ctx.chessboard) != null) {
            // нельзя вставать под шах (случай когда рубим фигуру под защитой тоже входит в этот кейс)
            return false
        }

        //проверки пройдены. можно ходить
        return true
    }

    private fun tryAddPawnMove(
        rowTo: Int,
        colTo: Int,
        result: MutableSet<Point>,
        kingAttacker: Point?,
        kingPossibleAttackerForObstacle: Point?,
        ctx: InternalContext
    ) {
        if (!isAvailableMove(rowTo, colTo, kingAttacker, kingPossibleAttackerForObstacle, ctx)) {
            return
        }

        // simple pawn move
        if ((ctx.pointFrom.col == colTo && ctx.getPieceNullable(rowTo, colTo) == null)
            // attack
            || (ctx.pointFrom.col != colTo && ctx.getPieceNullable(rowTo, colTo)?.side == ctx.enemySide)
        ) {
            result.add(Point.of(rowTo, colTo))
        }
    }

    private fun tryAddPawnEnPassantMove(
        rowTo: Int,
        colTo: Int,
        result: MutableSet<Point>,
        kingAttacker: Point?,
        kingPossibleAttackerForObstacle: Point?,
        ctx: InternalContext
    ) {
        if (ctx.game.getSideFeatures(ctx.enemySide).pawnLongMoveColumnIndex != colTo) {
            // плохая вертикаль
            return
        }
        if (ctx.sideFrom.pawnEnPassantStartRow != ctx.pointFrom.row) {
            // плохая горизонталь
            return
        }

        check(!isOutOfBoard(rowTo, colTo)) {
            // все плохое
            "isOutOfBoard: $ctx.pointFrom -> ${Point.of(rowTo, colTo)}"
        }

        if (ctx.getPieceNullable(rowTo, colTo) != null) {
            // при взятии на проходе пешку можно ставить только на пустую клетку
            return
        }

        check(ctx.getPieceNullable(ctx.pointFrom.row, colTo) == Piece.of(ctx.enemySide, PAWN)) {
            "en passant is not available for current state: expected: ${Piece.of(ctx.enemySide, PAWN)} " +
                    "on position=${Point.of(ctx.pointFrom.row, colTo)}, " +
                    "but found: chessboard.getPieceNullable(pointFrom.row, colTo)"
        }

        if (isAvailableMove(rowTo, colTo, kingAttacker, kingPossibleAttackerForObstacle, ctx)
            //если при взятии на проходе мы рубим пешку, объявившую шах - то такой ход тоже допустИм
            || (kingAttacker != null && kingPossibleAttackerForObstacle == null && kingAttacker.isEqual(ctx.pointFrom.row, colTo))
        ) {
            // наконец все проверки пройдены. ход ЭВЭЙЛЭБЛ!
            result.add(Point.of(rowTo, colTo))
        }
    }

    private fun getMovesByOffsets(
        offsets: Set<IntIntPair>,
        kingAttacker: Point?,
        kingPossibleAttackerForObstacle: Point?,
        ctx: InternalContext
    ): Set<Point> {
        val result: MutableSet<Point> = HashSet()

        for (offset in offsets) {
            val rowTo = ctx.pointFrom.row + offset.one
            val colTo = ctx.pointFrom.col + offset.two

            if (isOutOfBoard(rowTo, colTo)) {
                continue
            }

            val piece = ctx.getPieceNullable(rowTo, colTo)

            if (piece == null) {
                // свободная точка - ход доступен (если пройдет проверки)
                if (isAvailableMove(rowTo, colTo, kingAttacker, kingPossibleAttackerForObstacle, ctx)) {
                    result.add(Point.of(rowTo, colTo))
                }
            } else if (ctx.isEnemy(piece)) {
                // рубим врага (если ход пройдет проверки)
                if (isDestroyingThreatMove(rowTo, colTo, kingAttacker, kingPossibleAttackerForObstacle)) {
                    result.add(Point.of(rowTo, colTo))
                }
            }
        }

        return result
    }

    private fun getMovesByDirections(
        directions: Set<IntIntPair>,
        kingAttacker: Point?,
        kingPossibleAttackerForObstacle: Point?,
        ctx: InternalContext
    ): Set<Point> {

        val result: MutableSet<Point> = HashSet()

        var rowTo: Int = ctx.pointFrom.row
        var colTo: Int = ctx.pointFrom.col

        for (direction in directions) {
            while (true) {
                rowTo += direction.one
                colTo += direction.two

                if (isOutOfBoard(rowTo, colTo)) {
                    break
                }

                val piece = ctx.getPieceNullable(rowTo, colTo)

                if (piece == null) {
                    // свободная точка - ход доступен (если пройдет проверки)
                    if (isAvailableMove(rowTo, colTo, kingAttacker, kingPossibleAttackerForObstacle, ctx)) {
                        result.add(Point.of(rowTo, colTo))
                    }
                    continue
                }

                if (ctx.isEnemy(piece)) {
                    // рубим врага - текущая точка доступна для хода (если пройдет проверки), но перепрыгнуть ее нельзя, поэтому делаем после break
                    if (isDestroyingThreatMove(rowTo, colTo, kingAttacker, kingPossibleAttackerForObstacle)) {
                        result.add(Point.of(rowTo, colTo))
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

    private fun isAvailableMove(
        rowTo: Int,
        colTo: Int,
        kingAttacker: Point?,
        kingPossibleAttackerForObstacle: Point?,
        ctx: InternalContext
    ): Boolean {
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
            return canDefendKing(rowTo, colTo, kingAttacker, ctx)
        }

        if (kingPossibleAttackerForObstacle != null) {
            return canMoveObstacle(rowTo, colTo, kingPossibleAttackerForObstacle, ctx.kingPoint)
        }

        // ничем не ограниченный ход: kingAttacker == null && kingPossibleAttackerForObstacle == null
        return true
    }

    private fun canMoveObstacle(rowTo: Int, colTo: Int, kingPossibleAttackerForObstacle: Point, kingPoint: IPoint): Boolean {
        if (kingPossibleAttackerForObstacle.isEqual(rowTo, colTo)) {
            // рубим фигуру, из-за которой перемещаемая фигура являлась obstacle
            return true
        }
        // перемещение в рамках вектора допустимо
        return canBeObstacle(rowTo, colTo, kingPossibleAttackerForObstacle, kingPoint)
    }

    /**
     * return может ли данный ход защитить короля от шаха, источником которого являеится kingAttacker
     *
     * Защитить короля можно срубив атакующую фигуру, либо закрыться (если возможно)
     */
    private fun canDefendKing(rowTo: Int, colTo: Int, kingAttacker: Point, ctx: InternalContext): Boolean {
        if (kingAttacker.isEqual(rowTo, colTo)) {
            // рубим фигуру, объявившую шах.
            return true
        }

        if (kingAttacker.isBorderedWith(rowTo, colTo)) {
            // фигура, объявившая шах стоит вплотную к нашему королю, но при этом мы ее не рубим. такой ход недопустим
            return false
        }

        val kingAttackerPiece = ctx.getPiece(kingAttacker)

        if (kingAttackerPiece.type == KNIGHT) {
            // нам объявили шах или конем
            // мы его не рубим, а загородиться от коня невозможно. false
            return false
        }

        return canBeObstacle(rowTo, colTo, kingAttacker, ctx.kingPoint)
    }

    private fun canBeObstacle(rowTo: Int, colTo: Int, kingThreat: Point, kingPoint: IPoint): Boolean {
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

    private fun findPawnThreatsToKing(kingPoint: IPoint, kingSide: Side, chessboard: IChessboard): Point? {
        val enemySide = kingSide.reverse()
        val rowOffset = enemySide.pawnRowDirection

        val row = kingPoint.row + rowOffset
        var col = kingPoint.col + 1

        val piece = chessboard.getPieceNullable(row, col)
        if (piece != null && piece.isPawn() && piece.side == enemySide) {
            //короля не могут атаковать две пешки одновременно. поэтому сразу выходим
            return Point.of(row, col)
        }

        col = kingPoint.col - 1

        if (piece != null && piece.isPawn() && piece.side == enemySide) {
            return Point.of(row, col)
        }

        return null
    }

    private fun findKnightThreatsToKing(kingPoint: IPoint, kingSide: Side, chessboard: IChessboard): Point? {
        val enemySide = kingSide.reverse()

        for (offset in knightOffsets) {
            val row = kingPoint.row + offset.one
            val col = kingPoint.col + offset.two

            if (isOutOfBoard(row, col)) {
                continue
            }

            val foundPiece = chessboard.getPieceNullable(row, col)

            if (foundPiece != null && foundPiece.side == enemySide && foundPiece.type == KNIGHT) {
                //сразу выходим, потому что двойного шаха от двух коней быть не может
                return Point.of(row, col)
            }
        }
        return null
    }

    private fun findVectorPieceThreatsToKing(kingPoint: IPoint, kingSide: Side, chessboard: IChessboard): Twin<Point>? {
        var result: Twin<Point>? = null

        val allPossibleAttackerVectors = pieceVectorsMap[QUEEN]!!

        for (vector in allPossibleAttackerVectors) {
            val notEmptyPoint = nextPieceByVector(vector.one, vector.two, kingPoint, chessboard)
                ?: continue // ничего не нашли и уперлись в край доски

            val foundPiece = chessboard.getPiece(notEmptyPoint)
            if (foundPiece.side == kingSide || !isVectorPiece(foundPiece.type)) {
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

    private fun nextPieceByVector(rowDirection: Int, colDirection: Int, sourcePoint: IPoint, chessboard: IChessboard): Point? {
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

    private fun isVectorPiece(pieceType: PieceType): Boolean {
        return when (pieceType) {
            BISHOP -> true
            ROOK -> true
            QUEEN -> true
            else -> false
        }
    }

    private fun isOutOfBoard(row: Int, col: Int) = isIndexOutOfBounds(row) || isIndexOutOfBounds(col)

    private fun between(fromExclusive: Int, checkedValue: Int, toExclusive: Int): Boolean {
        return checkedValue > min(fromExclusive, toExclusive)
                && checkedValue < (max(fromExclusive, toExclusive))
    }

    private data class InternalContext(
        val game: IGame,
        val chessboard: IChessboard,
        val pointFrom: IPoint,
        val pieceFrom: Piece = chessboard.getPiece(pointFrom),
        val sideFrom: Side = pieceFrom.side,
        val enemySide: Side = sideFrom.reverse(),
        val kingPoint: IPoint = chessboard.getKingPoint(sideFrom)
    ) {
        internal fun isEnemy(piece: Piece) = piece.side == enemySide
        internal fun getPieceNullable(row: Int, col: Int) = chessboard.getPieceNullable(row, col)
        internal fun getPiece(point: Point) = chessboard.getPiece(point)
    }
}