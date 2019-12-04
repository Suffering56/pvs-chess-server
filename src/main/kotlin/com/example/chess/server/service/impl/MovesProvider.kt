package com.example.chess.server.service.impl

import com.example.chess.server.logic.IChessboard
import com.example.chess.server.logic.IGame
import com.example.chess.server.logic.misc.*
import com.example.chess.server.service.IMovesProvider
import com.example.chess.shared.Constants.ROOK_LONG_COLUMN_INDEX
import com.example.chess.shared.Constants.ROOK_SHORT_COLUMN_INDEX
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
 *
 *      Особенность данного класса в том, что он практически не создает промежуточных объектов (Point.of - не в счет)
 *          за исключением MoveContext-а и пишет результат сразу в результирующую коллекцию
 *          таким образом минимизируется выделяемая память и сильно снижается нагрузка на GC
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
                pair(1, 0),
                pair(-1, 0),
                pair(0, 1),
                pair(0, -1),
                //BISHOP
                pair(1, 1),
                pair(1, -1),
                pair(-1, 1),
                pair(-1, -1)
            )
            pieceVectorsMap[KING] = pieceVectorsMap[QUEEN]
        }
    }

    override fun getAvailableMoves(pointFrom: IPoint, chessboard: IChessboard, game: IGame): Set<IPoint> {
        val context = MoveContext(game, chessboard, pointFrom)
        return getAvailableMoves(context)
    }

    override fun isUnderCheck(kingSide: Side, chessboard: IChessboard): Boolean {
        val kingPoint = chessboard.getKingPoint(kingSide)
        return findKingAttackers(kingPoint, kingSide, chessboard) != null
    }

    private fun getAvailableMoves(ctx: MoveContext): Set<IPoint> {
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
        val kingPossibleAttackerForObstacle = getPossibleKingAttackerForCurrentObstacle(ctx, kingAttacker)

        return when (ctx.pieceFrom.type) {
            PAWN -> getPawnMoves(ctx, kingAttacker, kingPossibleAttackerForObstacle)
            KNIGHT -> getKnightMoves(ctx, kingAttacker, kingPossibleAttackerForObstacle)
            BISHOP -> getBishopMoves(ctx, kingAttacker, kingPossibleAttackerForObstacle)
            ROOK -> getRookMoves(ctx, kingAttacker, kingPossibleAttackerForObstacle)
            QUEEN -> getQueenMoves(ctx, kingAttacker, kingPossibleAttackerForObstacle)
            else -> throw UnsupportedOperationException("unsupported piece type: ${ctx.pieceFrom.type}")
        }
    }

    private fun findKingAttackers(kingPoint: IPoint, kingSide: Side, chessboard: IChessboard): Twin<IPoint>? {
        var result: Twin<IPoint>? = findVectorPieceThreatsToKing(kingPoint, kingSide, chessboard)

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

    private fun getPossibleKingAttackerForCurrentObstacle(
        ctx: MoveContext,
        kingAttacker: IPoint?
    ): IPoint? {
        if (ctx.pointFrom.hasCommonVectorWith(ctx.kingPoint)) {
            var rowDirection = 0
            var colDirection = 0
            val expectedThreatType: PieceType?

            when {
                // horizontal vector
                ctx.pointFrom.row == ctx.kingPoint.row -> {
                    colDirection = signum(ctx.pointFrom.col - ctx.kingPoint.col)
                    expectedThreatType = ROOK
                }
                // vertical vector
                ctx.pointFrom.col == ctx.kingPoint.col -> {
                    rowDirection = signum(ctx.pointFrom.row - ctx.kingPoint.row)
                    expectedThreatType = ROOK
                }
                // diagonal vector
                else -> {
                    colDirection = signum(ctx.pointFrom.col - ctx.kingPoint.col)
                    rowDirection = signum(ctx.pointFrom.row - ctx.kingPoint.row)
                    expectedThreatType = BISHOP
                }
            }
            return getKingThreatPointForObstacle(rowDirection, colDirection, ctx, kingAttacker, expectedThreatType)
        }

        return null
    }

    /**
     *
     * Справочник:
     *      obstacle - препятствие, коим является pointFrom
     *      anyObstacle - препятствие, если оно не являтеся pointFrom
     *      out - конец доски
     *      threat - вражеская фигура которая срубит короля, если убрать obstacle
     *      any = любой вариант из списка: threat/obstacle/out/anyObstacle
     *
     * Идем от короля по заданному вектору.
     * Если сначала нашлась pointFrom, а потом threatPoint(точка, на которой находится фигура врага,
     *      которая шаховала бы врага, если бы не препятствие в виде pointFrom), то вернет threatPoint:
     *      kingPoint->threat->any
     *
     * Во всех остальных случаях вернет null. Остальные случаи:
     * - pointFrom ни от чего не защищает: kingPoint->obstacle->out
     * - два препятствия подряд: kingPoint->anyObstacle->anyObstacle->any
     * - первое препятствие не pointFrom: kingPoint->anyObstacle->any
     *
     * TODO: написать тест для проверки того, что possibleObstacle обязательно найдется если от kingPoint идти по вектору (rowDirection, colDirection)
     */
    private fun getKingThreatPointForObstacle(
        rowDirection: Int,
        colDirection: Int,
        ctx: MoveContext,
        kingAttacker: IPoint?,
        expectedThreatType: PieceType
    ): IPoint? {
        var row: Int = ctx.kingPoint.row
        var col: Int = ctx.kingPoint.col
        var obstacleFound = false

        while (true) {
            row += rowDirection
            col += colDirection

            if (isOutOfBoard(row, col)) {
                check(obstacleFound) {
                    "board is out[$row, $col], but obstacle(${ctx.pointFrom}) not found yet"
                }
                // достигли конца доски. => при этом никаких угроз не было найдено
                return null
            }

            val piece = ctx.getPieceNullable(row, col)

            if (piece != null) {
                if (piece.side == ctx.enemySide && (piece.type == expectedThreatType || piece.type == QUEEN)) {
                    if (!obstacleFound) {
                        // прежде чем найти хотя бы одно препятствие по заданному вектору (двигаясь от короля) мы нашли фигуру, которая шахует нашего короля прямо здесь и сейчас
                        // TODO: can remove it later for optimize
                        check(kingAttacker != null && kingAttacker.isEqual(row, col)) {
                            "kingThreat(${Point.of(row, col)}) is not equals with kingAttacker($kingAttacker)"
                        }
                        // дальнейшее следование по вектору не имеет смысла, а possibleObstacle не является препятствием
                        return null
                    }

                    if (obstacleFound) {
                        return Point.of(row, col)
                    }
                }

                // проверки пройдены! значит - препятствие найдено в текущей позиции (row, col)

                if (obstacleFound) {
                    // но т.к. ранее уже было найдено препятствие, то сразу выходим ибо 2 препятствия подряд = защита от любых угроз
                    return null
                }

                if (!obstacleFound && !ctx.pointFrom.isEqual(row, col)) {
                    // препятствие найдено, но оно не pointFrom, значит короля есть кому защитить а перемещаемая фигура может свободно ходить
                    return null
                }

                // препятствие найдено: оно первое и оно pointFrom
                obstacleFound = true
            }
        }
    }

    private fun getKingMoves(ctx: MoveContext): Set<Point> {
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

        // под шахом рокировка невозможна
        if (findKingAttackers(ctx.kingPoint, ctx.sideFrom, ctx.chessboard) != null) {
            return result
        }

        // try add castling moves
        val sideFeatures = ctx.game.getSideFeatures(ctx.sideFrom)

        if (sideFeatures.longCastlingAvailable) {
            tryAddCastlingMove(ctx, result, true)
        }

        if (sideFeatures.shortCastlingAvailable) {
            tryAddCastlingMove(ctx, result, false)
        }

        return result
    }

    private fun getPawnMoves(
        ctx: MoveContext,
        kingAttacker: IPoint?,
        kingPossibleAttackerForObstacle: IPoint?
    ): Set<IPoint> {
        val result: MutableSet<IPoint> = HashSet()

        val rowOffset = ctx.sideFrom.pawnRowDirection

        val rowTo = ctx.pointFrom.row + rowOffset
        var colTo = ctx.pointFrom.col

        // simple move
        val simpleMoveAvailable =
            tryAddPawnMove(rowTo, colTo, result, kingAttacker, kingPossibleAttackerForObstacle, ctx)

        // long distance move
        if (simpleMoveAvailable && ctx.sideFrom.pawnInitialRow == ctx.pointFrom.row) {
            tryAddPawnMove(rowTo + rowOffset, colTo, result, kingAttacker, kingPossibleAttackerForObstacle, ctx)
        }

        colTo = ctx.pointFrom.col + 1

        // attack
        tryAddPawnMove(rowTo, colTo, result, kingAttacker, kingPossibleAttackerForObstacle, ctx)
        // en passant
        tryAddPawnEnPassantMove(rowTo, colTo, result, kingAttacker, kingPossibleAttackerForObstacle, ctx)

        colTo = ctx.pointFrom.col - 1

        // attack
        tryAddPawnMove(rowTo, colTo, result, kingAttacker, kingPossibleAttackerForObstacle, ctx)
        // en passant
        tryAddPawnEnPassantMove(rowTo, colTo, result, kingAttacker, kingPossibleAttackerForObstacle, ctx)

        return result
    }

    private fun getKnightMoves(
        ctx: MoveContext,
        kingAttacker: IPoint?,
        kingPossibleAttackerForObstacle: IPoint?
    ): Set<IPoint> {
        return getMovesByOffsets(knightOffsets, kingAttacker, kingPossibleAttackerForObstacle, ctx)
    }

    private fun getBishopMoves(
        ctx: MoveContext,
        kingAttacker: IPoint?,
        kingPossibleAttackerForObstacle: IPoint?
    ): Set<IPoint> {
        return getMovesByDirections(pieceVectorsMap[BISHOP]!!, kingAttacker, kingPossibleAttackerForObstacle, ctx)
    }

    private fun getRookMoves(
        ctx: MoveContext,
        kingAttacker: IPoint?,
        kingPossibleAttackerForObstacle: IPoint?
    ): Set<IPoint> {
        return getMovesByDirections(pieceVectorsMap[ROOK]!!, kingAttacker, kingPossibleAttackerForObstacle, ctx)
    }

    private fun getQueenMoves(
        ctx: MoveContext,
        kingAttacker: IPoint?,
        kingPossibleAttackerForObstacle: IPoint?
    ): Set<IPoint> {
        return getMovesByDirections(pieceVectorsMap[QUEEN]!!, kingAttacker, kingPossibleAttackerForObstacle, ctx)
    }

    private fun tryAddCastlingMove(ctx: MoveContext, result: MutableSet<Point>, isLong: Boolean) {
        val direction = if (isLong) 1 else -1
        val rookCol = if (isLong) ROOK_LONG_COLUMN_INDEX else ROOK_SHORT_COLUMN_INDEX

        val row = ctx.kingPoint.row
        var offset = 1

        while (true) {
            val col = ctx.kingPoint.col + direction * offset

            if (col == rookCol) {
                // достигли ладьи, с которой рокируемся
                break
            }

            if (ctx.getPieceNullable(row, col) != null) {
                // между королем и ладьей есть фигура. рокировка невозможна
                return
            }

            if (offset == 1 && !result.contains(Point.of(row, col))) {
                // рокировка через битое поле невозможна
                return
            }

            if (offset == 2 && findKingAttackers(Point.of(row, col), ctx.sideFrom, ctx.chessboard) != null) {
                // под шах вставать при рокировке тоже как ни странно - нельзя
                return
            }

            offset++
        }

        result.add(Point.of(ctx.kingPoint.row, ctx.kingPoint.col + 2 * direction))
    }

    private fun isAvailableKingMove(
        rowTo: Int,
        colTo: Int,
        enemyKingPoint: IPoint,
        ctx: MoveContext
    ): Boolean {
        if (isOutOfBoard(rowTo, colTo)) {
            // нельзя вставать за пределы доски
            return false
        }

        if (enemyKingPoint.isBorderedWith(rowTo, colTo)) {
            // нельзя вставать вплотную к вражескому королю
            return false
        }

        if (ctx.getPieceNullable(rowTo, colTo)?.side == ctx.sideFrom) {
            // нельзя рубить своих
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
        result: MutableSet<IPoint>,
        kingAttacker: IPoint?,
        kingPossibleAttackerForObstacle: IPoint?,
        ctx: MoveContext
    ): Boolean {
        if (isOutOfBoard(rowTo, colTo)) {
            return false
        }

        if (!isAvailableMove(rowTo, colTo, kingAttacker, kingPossibleAttackerForObstacle, ctx)) {
            return false
        }

        if (ctx.pointFrom.col == colTo && ctx.getPieceNullable(rowTo, colTo) != null) {
            // обычный ход возможен только на пустую клетку и не может рубить ни чужих ни своих
            return false
        }

        if (ctx.pointFrom.col != colTo && ctx.getPieceNullable(rowTo, colTo)?.side != ctx.enemySide) {
            // атака доступна только если по диагонали от пешки находится вражеская фигура
            return false
        }

        result.add(Point.of(rowTo, colTo))
        return true
    }

    private fun tryAddPawnEnPassantMove(
        rowTo: Int,
        colTo: Int,
        result: MutableSet<IPoint>,
        kingAttacker: IPoint?,
        kingPossibleAttackerForObstacle: IPoint?,
        ctx: MoveContext
    ) {
        if (ctx.sideFrom.pawnEnPassantStartRow != ctx.pointFrom.row) {
            // плохая горизонталь
            return
        }

        if (ctx.game.getSideFeatures(ctx.enemySide).pawnLongMoveColumnIndex != colTo) {
            // плохая вертикаль
            return
        }

        check(!isOutOfBoard(rowTo, colTo)) {
            // все плохое
            "isOutOfBoard: $ctx.pointFrom -> ${Point.of(rowTo, colTo)}"
        }

        check(ctx.getPieceNullable(rowTo, colTo) == null) {
            // при взятии на проходе пешку можно ставить только на пустую клетку,
            //      но факт, что само по себе взятие на проходе доступно означает что эту клетку
            //      только что перепрыгнула пешка противника делая long distance move, что фантастически странно
            "en passant point[move.to]=${Point.of(rowTo, colTo)} can't be not empty, but found: ${ctx.getPieceNullable(
                rowTo,
                colTo
            )}"
        }

        check(ctx.getPieceNullable(ctx.pointFrom.row, colTo) == Piece.of(ctx.enemySide, PAWN)) {
            // справа(или слева) от пешки на pointFrom должна стоять пешка противника.
            // только такая позиция может привести к взятию на проходе
            "en passant is not available for current state: expected: ${Piece.of(ctx.enemySide, PAWN)} " +
                    "on position=${Point.of(ctx.pointFrom.row, colTo)}, " +
                    "but found: chessboard.getPieceNullable(pointFrom.row, colTo)"
        }

        // ход допустИм
        if (isAvailableMove(rowTo, colTo, kingAttacker, kingPossibleAttackerForObstacle, ctx)
            // или при взятии на проходе мы рубим пешку, объявившую шах
            || (kingAttacker != null && kingPossibleAttackerForObstacle == null
                    && kingAttacker.isEqual(ctx.pointFrom.row, colTo))
        ) {
            // наконец все проверки пройдены. ход ЭВЭЙЛЭБЛ!
            result.add(Point.of(rowTo, colTo))
        }
    }

    private fun getMovesByOffsets(
        offsets: Set<IntIntPair>,
        kingAttacker: IPoint?,
        kingPossibleAttackerForObstacle: IPoint?,
        ctx: MoveContext
    ): Set<IPoint> {
        val result: MutableSet<IPoint> = HashSet()

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
        kingAttacker: IPoint?,
        kingPossibleAttackerForObstacle: IPoint?,
        ctx: MoveContext
    ): Set<IPoint> {

        val result: MutableSet<IPoint> = HashSet()

        for (direction in directions) {
            var rowTo: Int = ctx.pointFrom.row
            var colTo: Int = ctx.pointFrom.col

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

    private fun isDestroyingThreatMove(
        rowTo: Int,
        colTo: Int,
        kingAttacker: IPoint?,
        kingPossibleAttackerForObstacle: IPoint?
    ): Boolean {
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
        kingAttacker: IPoint?,
        kingPossibleAttackerForObstacle: IPoint?,
        ctx: MoveContext
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

    private fun canMoveObstacle(
        rowTo: Int,
        colTo: Int,
        kingPossibleAttackerForObstacle: IPoint,
        kingPoint: IPoint
    ): Boolean {
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
    private fun canDefendKing(rowTo: Int, colTo: Int, kingAttacker: IPoint, ctx: MoveContext): Boolean {
        if (kingAttacker.isEqual(rowTo, colTo)) {
            // рубим фигуру, объявившую шах.
            return true
        }

        if (kingAttacker.isBorderedWith(ctx.kingPoint)) {
            // фигура, объявившая шах стоит вплотную к нашему королю, но при этом мы ее не рубим. такой ход недопустим
            return false
        }

        val kingAttackerPiece = ctx.getPiece(kingAttacker)

        if (kingAttackerPiece.type == KNIGHT) {
            // нам объявили шах конем
            // мы его не рубим, а загородиться от коня невозможно. false
            return false
        }

        return canBeObstacle(rowTo, colTo, kingAttacker, ctx.kingPoint)
    }

    private fun canBeObstacle(rowTo: Int, colTo: Int, kingThreat: IPoint, kingPoint: IPoint): Boolean {
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
        val rowOffset = kingSide.pawnRowDirection
        val enemySide = kingSide.reverse()

        val row = kingPoint.row + rowOffset
        var col = kingPoint.col + 1

        if (!isOutOfBoard(row, col)) {
            val piece = chessboard.getPieceNullable(row, col)
            if (piece != null && piece.isPawn() && piece.side == enemySide) {
                //короля не могут атаковать две пешки одновременно. поэтому сразу выходим
                return Point.of(row, col)
            }
        }

        col = kingPoint.col - 1

        if (!isOutOfBoard(row, col)) {
            val piece = chessboard.getPieceNullable(row, col)
            if (piece != null && piece.isPawn() && piece.side == enemySide) {
                return Point.of(row, col)
            }
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

    private fun findVectorPieceThreatsToKing(kingPoint: IPoint, kingSide: Side, chessboard: IChessboard): Twin<IPoint>? {
        var result: Twin<IPoint>? = null

        val allPossibleAttackerVectors = pieceVectorsMap[QUEEN]!!

        for (vector in allPossibleAttackerVectors) {
            val notEmptyPoint = nextPieceByVector(vector.one, vector.two, kingPoint, chessboard)
                ?: continue // ничего не нашли и уперлись в край доски

            val foundPiece = chessboard.getPiece(notEmptyPoint)
            if (foundPiece.side == kingSide || !isVectorPiece(foundPiece.type)) {
                //мы ищем только вражеского слона/ладью/ферзя
                continue
            }

            if (foundPiece.type != QUEEN && !kingPoint.hasCommonVectorWith(notEmptyPoint, foundPiece.type)) {
                //найденная фигура не может атаковать короля по заданному вектору
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

    private fun nextPieceByVector(
        rowDirection: Int,
        colDirection: Int,
        sourcePoint: IPoint,
        chessboard: IChessboard
    ): Point? {
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

    private data class MoveContext(
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
        internal fun getPiece(point: IPoint) = chessboard.getPiece(point)
        internal fun pointFromToString() = "${pointFrom.toPrettyString()}($pointFrom): $pieceFrom"
        internal fun kingPointToString() = "${kingPoint.toPrettyString()}($kingPoint): ${chessboard.getPiece(kingPoint)}"
    }
}