package com.example.chess.server.service.impl

import com.example.chess.server.logic.IUnmodifiableChessboard
import com.example.chess.server.logic.IUnmodifiableGame
import com.example.chess.server.logic.misc.Point
import com.example.chess.server.logic.misc.isIndexOutOfBounds
import com.example.chess.server.service.IMovesProvider
import com.example.chess.shared.Constants.ROOK_LONG_COLUMN_INDEX
import com.example.chess.shared.Constants.ROOK_SHORT_COLUMN_INDEX
import com.example.chess.shared.enums.Piece
import com.example.chess.shared.enums.PieceType
import com.example.chess.shared.enums.PieceType.*
import com.example.chess.shared.enums.Side
import org.springframework.stereotype.Component
import java.lang.Integer.*
import java.util.*
import kotlin.math.abs

/**
 * @author v.peschaniy
 *      Date: 20.11.2019
 *
 *      Особенность данного класса в том, что он практически не создает промежуточных объектов (Point.of - не в счет)
 *          за исключением InnerContext-а и пишет результат сразу в результирующую коллекцию
 *          таким образом минимизируется выделяемая память и сильно снижается нагрузка на GC
 */
@Component
class MovesProvider : IMovesProvider {

    override fun getAvailableMoves(game: IUnmodifiableGame, chessboard: IUnmodifiableChessboard, pointFrom: Point): List<Point> {
        val context = InnerContext(game, chessboard, pointFrom)
        return getAvailableMoves(context)
    }

    override fun isUnderCheck(chessboard: IUnmodifiableChessboard, kingSide: Side): Boolean {
        val kingPoint = chessboard.getKingPoint(kingSide)
        return findKingAttackers(kingPoint, kingSide, chessboard).isNotEmpty()
    }

    override fun getThreatsToTarget(game: IUnmodifiableGame, chessboard: IUnmodifiableChessboard, targetPoint: Point): List<Point> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getThreatsToTargetCount(game: IUnmodifiableGame, chessboard: IUnmodifiableChessboard, targetPoint: Point): Int {
        val targetPiece = chessboard.getPiece(targetPoint)
        val targetPointSide = targetPiece.side

        /*
         * TODO: нужно ли проверять что король под шахом?
         * Пока решил, что в данном кейсе это не очень важная проверка. Но если вдруг потребуется то:
         *
         * - король под шахом, и не может рубить targetPoint -> return 0
         * - король под шахом, и может рубить targetPoint, который никем не защищен -> считаем как есть, или возвращаем 1? (по идее 1 > 0 будет в любом случае) -> return 1
         * - король под шахом, но targetPoint защищен, хоть и король может его рубить (тут есть нюанс в виде такой ситуации kingAttacker -> king -> targetPoint) -> return 0
         *
         * PS: выглядит так, словно нас двойной шах не сильно то и интересует (но интересуют обе шахующие фигуры, независимо друг от друга)
         */

        return sum(
            getThreatsToTargetByAllVectors(chessboard, targetPoint, targetPointSide),
            getKnightThreatsToTarget(targetPoint, targetPointSide, chessboard)
            //TODO: я совершенно точно не учитываю en-passant
        )
    }

    private fun getKnightThreatsToTarget(targetPoint: Point, targetPointSide: Side, chessboard: IUnmodifiableChessboard): Int {
        var counter = 0

        for (vector in knightOffsets) {
            val row = targetPoint.row + vector.row
            val col = targetPoint.col + vector.col

            if (isOutOfBoard(row, col)) {
                continue
            }

            val piece = chessboard.getPieceNullable(row, col)
                ?: continue

            if (piece.side == targetPointSide) {
                // нас интересуют только враги targetPoint-а
                continue
            }

            if (piece.isTypeOf(KNIGHT)) {
                counter++
            }
        }

        return counter
    }

    /**
     * 1) Для каждого из 8 направлений идет от targetPoint по вектору (направлению)
     * 2) Если встретит угрозу (вражескую для targetPoint фигуру), которая может срубить targetPoint здесь и сейчас
     * 3) Увеличивает счетчик на единицу
     * 4) Может продолжить идти по тому же направлению после увеличения счетчика, чтобы вычислить "батарею", которая будет учтена счетчиком
     */
    private fun getThreatsToTargetByAllVectors(chessboard: IUnmodifiableChessboard, targetPoint: Point, targetPointSide: Side): Int {
        var counter = 0
        val allPossibleAttackerVectors = pieceVectorsMap[QUEEN]!!

        for (vector in allPossibleAttackerVectors) {
            var offset = 0

            whileLoop@ while (true) {
                offset++
                val row = targetPoint.row + vector.row * offset
                val col = targetPoint.col + vector.col * offset

                if (isOutOfBoard(row, col)) {
                    // ничего не нашли и уперлись в край доски
                    break
                }

                // если точка пуста - продолжаем поиск по вектору
                val foundPiece = chessboard.getPieceNullable(row, col) ?: continue

                if (foundPiece.side == targetPointSide) {
                    // уперлись в союзную фигуру
                    // дальнейшее следование по вектору не имеет смысла
                    break
                }

                if (offset == 1 && foundPiece.isTypeOf(KING)) {
                    // на этапе подсчета нас вроде не интересует факт того, что король может залезть под шах
                    counter++
                    // батарея Q -> K -> targetPoint невалидна, т.к. она подразумевает что король (K) пойдет в размен, что недопустимо
                    // поэтому дальнейшее следование по этому вектору не имеет смысла
                    break

                } else if (offset == 1 && foundPiece.isTypeOf(PAWN) && targetPointSide.pawnRowDirection == vector.row && vector.isDiagonal()) {
                    // пешка тоже может являться угрозой если все условия выше были выполнены
                    // do nothing, it's ok!
                } else if (vector.isDiagonal() && foundPiece.isTypeOf(BISHOP, QUEEN)) {
                    // do nothing, it's ok!
                } else if (!vector.isDiagonal() && foundPiece.isTypeOf(ROOK, QUEEN)) {
                    // do nothing, it's ok!
                } else {
                    // если мы добрались сюда, значит уперлись в фигуру врага (свои мы отсеяли раньше), которая не может причинить никакого вреда targetPoint
                    // дальнейшее следование по текущему вектору не имеет смысла, несите новый
                    break
                }

                // мы уверены что threat-король не под шахом, либо он может срубить targetPoint а значит стоит вплотную
                val kingAttacker = null
                val kingThreatsForCurrentObstacle = getKingThreatsForCurrentObstacle(chessboard, Point.of(row, col), kingAttacker)

                // выбранную фигуру можно двигать, она не защищает короля от шаха
                // либо рубит фигуру, от шаха которой она защищает короля
                if (kingThreatsForCurrentObstacle == null || kingThreatsForCurrentObstacle == targetPoint) {
                    counter++
                    // за фигурой может другая фигура такого же типа (назовем такое сочетание батареей)
                    // примеры батарей: R -> R -> targetPoint,  Q -> P -> targetPoint, Q -> B -> targetPoint
                    // так вот батарея из фигур имеет значение для подсчета
                    continue
                } else {
                    // если эту фигуру двигать нельзя, то остальные (стоящие сзади) не помогут и подавно
                    break
                }
            }
        }

        return counter
    }

    private fun getAvailableMoves(ctx: InnerContext): List<Point> {
        if (ctx.pieceFrom.isKing()) {
            //ходы короля слишком непохожи на другие, т.к. нас уже неинтересуют текущие шахи, а так же препятствия
            return getKingMoves(ctx)
        }

        val kingAttackers = findKingAttackers(ctx.kingPoint, ctx.sideFrom, ctx.chessboard)
        require(kingAttackers.size <= 2) { "triple check unsupported in chess game.\r\n ${ctx.chessboard.toPrettyString()}" }

        if (kingAttackers.size == 2) {
            //двойной шах, ходить можно только королем
            return Point.pointsOf()
        }

        val kingAttacker = if (kingAttackers.isNotEmpty()) kingAttackers[0] else null
        val kingPossibleAttackerForObstacle = getKingThreatsForCurrentObstacle(ctx.chessboard, ctx.pointFrom, kingAttacker)

        return when (ctx.pieceFrom.type) {
            PAWN -> getPawnMoves(ctx, kingAttacker, kingPossibleAttackerForObstacle)
            KNIGHT -> getKnightMoves(ctx, kingAttacker, kingPossibleAttackerForObstacle)
            BISHOP -> getBishopMoves(ctx, kingAttacker, kingPossibleAttackerForObstacle)
            ROOK -> getRookMoves(ctx, kingAttacker, kingPossibleAttackerForObstacle)
            QUEEN -> getQueenMoves(ctx, kingAttacker, kingPossibleAttackerForObstacle)
            else -> throw UnsupportedOperationException("unsupported piece type: ${ctx.pieceFrom.type}")
        }
    }

    private fun findKingAttackers(kingPoint: Point, kingSide: Side, chessboard: IUnmodifiableChessboard): List<Point> {
        var result: List<Point> = findVectorPieceThreatsToKing(kingPoint, kingSide, chessboard)

        if (result.size == 2) {
            //тройной шах в этой игре невозможен
            return result
        }

        val knightAttacker = findKnightThreatsToKing(kingPoint, kingSide, chessboard)

        if (knightAttacker != null) {
            result = addToResult(result, knightAttacker)

            if (result.size == 2) {
                //тройной шах в этой игре невозможен
                return result
            }
        }

        val pawnAttacker = findPawnThreatsToKing(kingPoint, kingSide, chessboard)

        if (pawnAttacker != null) {
            return addToResult(result, pawnAttacker)
        }

        return result
    }

    /**
     * 1) Находит защищаемого короля, где protectedKing.side == obstaclePoint.side
     * 2) Определяет находится ли препятствие (obstaclePoint) на одной диагонали/вертикали/горизонтали с защищаемым королем (protectedKingPoint)
     * 3) Если нет - возвращает null
     * 4) Если все же находится, то определяет вектор направления "защищаемый король(protectedKing) >>> препятствие(obstacle) >>> угроза?(threat))
     * 5) Вызывает getKingThreatPointForObstacle, чтобы найти угрозу (threatPoint)
     */
    private fun getKingThreatsForCurrentObstacle(
        chessboard: IUnmodifiableChessboard,
        obstaclePoint: Point,
        kingAttacker: Point?
    ): Point? {

        val obstaclePiece = chessboard.getPiece(obstaclePoint)
        val protectedKingPoint = chessboard.getKingPoint(obstaclePiece.side)

        if (obstaclePoint.hasCommonVectorWith(protectedKingPoint)) {
            var rowDirection = 0
            var colDirection = 0
            val expectedThreatType: PieceType?

            when {
                // horizontal vector
                obstaclePoint.row == protectedKingPoint.row -> {
                    colDirection = signum(obstaclePoint.col - protectedKingPoint.col)
                    expectedThreatType = ROOK
                }
                // vertical vector
                obstaclePoint.col == protectedKingPoint.col -> {
                    rowDirection = signum(obstaclePoint.row - protectedKingPoint.row)
                    expectedThreatType = ROOK
                }
                // diagonal vector
                else -> {
                    colDirection = signum(obstaclePoint.col - protectedKingPoint.col)
                    rowDirection = signum(obstaclePoint.row - protectedKingPoint.row)
                    expectedThreatType = BISHOP
                }
            }

            return getKingThreatPointForObstacle(
                chessboard,
                obstaclePoint,
                Vector.of(rowDirection, colDirection),
                kingAttacker,
                expectedThreatType
            )
        }

        return null
    }

    /**
     *
     * @see getKingThreatsForCurrentObstacle
     *
     * Справочник:
     *      obstacle - препятствие, коим является pointFrom
     *      anyObstacle - препятствие, если оно не являтеся pointFrom
     *      out - конец доски
     *      threat - вражеская фигура которая срубит короля, если убрать obstacle
     *      any = любой вариант из списка: threat/obstacle/out/anyObstacle
     *
     * Идем от короля по заданному вектору.
     * Если сначала нашлась obstaclePoint, а потом threatPoint(точка, на которой находится фигура врага,
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
        chessboard: IUnmodifiableChessboard,
        obstaclePoint: Point,
        vector: Vector,
        kingAttacker: Point?,           // используется исключительно для выявления ошибок
        expectedThreatType: PieceType   // используется как вектор и может быть либо BISHOP либо QUEEN
    ): Point? {

        val obstaclePiece = chessboard.getPiece(obstaclePoint)
        val threatSide = obstaclePiece.side.reverse()
        val protectedKingPoint = chessboard.getKingPoint(obstaclePiece.side)

        var row: Int = protectedKingPoint.row
        var col: Int = protectedKingPoint.col
        var obstacleFound = false

        while (true) {
            row += vector.row
            col += vector.col

            if (isOutOfBoard(row, col)) {
                check(obstacleFound) {
                    "board is out[$row, $col], but obstacle($obstaclePoint) not found yet"
                }
                // достигли конца доски. => при этом никаких угроз не было найдено
                return null
            }

            val piece = chessboard.getPieceNullable(row, col)

            if (piece != null) {
                if (piece.side == threatSide && piece.isTypeOf(expectedThreatType, QUEEN)) {
                    if (!obstacleFound) {
                        // прежде чем найти хотя бы одно препятствие по заданному вектору (двигаясь от короля) мы нашли фигуру, которая шахует нашего короля прямо здесь и сейчас
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

                if (!obstacleFound && !obstaclePoint.isEqual(row, col)) {
                    // препятствие найдено, но оно не obstaclePoint, значит короля есть кому защитить а перемещаемая фигура может свободно ходить
                    return null
                }

                // препятствие найдено: оно первое и оно obstaclePoint
                obstacleFound = true
            }
        }
    }

    private fun getKingMoves(ctx: InnerContext): List<Point> {
        check(ctx.kingPoint == ctx.pointFrom) {
            "king expected in point from: $ctx.pointFrom, but found in $ctx.kingPoint"
        }

        var result: List<Point> = Point.pointsOf()
        val kingMoveOffsets = pieceVectorsMap[QUEEN]!!
        val enemyKingPoint = ctx.chessboard.getKingPoint(ctx.enemySide)

        // try add simple king moves
        for (offset in kingMoveOffsets) {
            val rowTo = ctx.kingPoint.row + offset.row
            val colTo = ctx.kingPoint.col + offset.col

            if (isAvailableKingMove(rowTo, colTo, enemyKingPoint, ctx)) {
                result = addToResult(result, Point.of(rowTo, colTo))
            }
        }

        // под шахом рокировка невозможна
        if (findKingAttackers(ctx.kingPoint, ctx.sideFrom, ctx.chessboard).isNotEmpty()) {
            return result
        }

        // try add castling moves
        if (ctx.game.isLongCastlingAvailable(ctx.sideFrom)) {
            tryAddCastlingMove(ctx, result, true)
        }

        if (ctx.game.isShortCastlingAvailable(ctx.sideFrom)) {
            tryAddCastlingMove(ctx, result, false)
        }

        return result
    }

    private fun getPawnMoves(
        ctx: InnerContext,
        kingAttacker: Point?,
        kingPossibleAttackerForObstacle: Point?
    ): List<Point> {
        var result: List<Point> = Point.pointsOf()

        val rowOffset = ctx.sideFrom.pawnRowDirection

        val rowTo = ctx.pointFrom.row + rowOffset
        var colTo = ctx.pointFrom.col

        // simple move
        result = tryAddPawnMove(rowTo, colTo, result, kingAttacker, kingPossibleAttackerForObstacle, ctx)
        val simpleMoveAvailable = result.isNotEmpty()

        // long distance move
        if (simpleMoveAvailable && ctx.sideFrom.pawnInitialRow == ctx.pointFrom.row) {
            result = tryAddPawnMove(rowTo + rowOffset, colTo, result, kingAttacker, kingPossibleAttackerForObstacle, ctx)
        }

        colTo = ctx.pointFrom.col + 1

        // attack
        result = tryAddPawnMove(rowTo, colTo, result, kingAttacker, kingPossibleAttackerForObstacle, ctx)
        // en passant
        result = tryAddPawnEnPassantMove(rowTo, colTo, result, kingAttacker, kingPossibleAttackerForObstacle, ctx)

        colTo = ctx.pointFrom.col - 1

        // attack
        result = tryAddPawnMove(rowTo, colTo, result, kingAttacker, kingPossibleAttackerForObstacle, ctx)
        // en passant
        result = tryAddPawnEnPassantMove(rowTo, colTo, result, kingAttacker, kingPossibleAttackerForObstacle, ctx)

        return result
    }

    private fun getKnightMoves(
        ctx: InnerContext,
        kingAttacker: Point?,
        kingPossibleAttackerForObstacle: Point?
    ): List<Point> {
        return getMovesByOffsets(knightOffsets, kingAttacker, kingPossibleAttackerForObstacle, ctx)
    }

    private fun getBishopMoves(
        ctx: InnerContext,
        kingAttacker: Point?,
        kingPossibleAttackerForObstacle: Point?
    ): List<Point> {
        return getMovesByDirections(pieceVectorsMap[BISHOP]!!, kingAttacker, kingPossibleAttackerForObstacle, ctx)
    }

    private fun getRookMoves(
        ctx: InnerContext,
        kingAttacker: Point?,
        kingPossibleAttackerForObstacle: Point?
    ): List<Point> {
        return getMovesByDirections(pieceVectorsMap[ROOK]!!, kingAttacker, kingPossibleAttackerForObstacle, ctx)
    }

    private fun getQueenMoves(
        ctx: InnerContext,
        kingAttacker: Point?,
        kingPossibleAttackerForObstacle: Point?
    ): List<Point> {
        return getMovesByDirections(pieceVectorsMap[QUEEN]!!, kingAttacker, kingPossibleAttackerForObstacle, ctx)
    }

    private fun tryAddCastlingMove(ctx: InnerContext, result: List<Point>, isLong: Boolean): List<Point> {
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
                return result
            }

            if (offset == 1 && !result.contains(Point.of(row, col))) {
                // рокировка через битое поле невозможна
                return result
            }

            if (offset == 2 && findKingAttackers(Point.of(row, col), ctx.sideFrom, ctx.chessboard).isNotEmpty()) {
                // под шах вставать при рокировке тоже как ни странно - нельзя
                return result
            }

            offset++
        }

        return addToResult(result, Point.of(ctx.kingPoint.row, ctx.kingPoint.col + 2 * direction))
    }

    private fun isAvailableKingMove(
        rowTo: Int,
        colTo: Int,
        enemyKingPoint: Point,
        ctx: InnerContext
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

        if (findKingAttackers(Point.of(rowTo, colTo), ctx.sideFrom, ctx.chessboard).isNotEmpty()) {
            // нельзя вставать под шах (случай когда рубим фигуру под защитой тоже входит в этот кейс)
            return false
        }

        //проверки пройдены. можно ходить
        return true
    }

    private fun tryAddPawnMove(
        rowTo: Int,
        colTo: Int,
        result: List<Point>,
        kingAttacker: Point?,
        kingPossibleAttackerForObstacle: Point?,
        ctx: InnerContext
    ): List<Point> {

        if (isOutOfBoard(rowTo, colTo)) {
            return result
        }

        if (!isAvailableMove(rowTo, colTo, kingAttacker, kingPossibleAttackerForObstacle, ctx)) {
            return result
        }

        if (ctx.pointFrom.col == colTo && ctx.getPieceNullable(rowTo, colTo) != null) {
            // обычный ход возможен только на пустую клетку и не может рубить ни чужих ни своих
            return result
        }

        if (ctx.pointFrom.col != colTo && ctx.getPieceNullable(rowTo, colTo)?.side != ctx.enemySide) {
            // атака доступна только если по диагонали от пешки находится вражеская фигура
            return result
        }

        return addToResult(result, Point.of(rowTo, colTo))
    }

    private fun tryAddPawnEnPassantMove(
        rowTo: Int,
        colTo: Int,
        result: List<Point>,
        kingAttacker: Point?,
        kingPossibleAttackerForObstacle: Point?,
        ctx: InnerContext
    ): List<Point> {
        if (ctx.sideFrom.pawnEnPassantStartRow != ctx.pointFrom.row) {
            // плохая горизонталь
            return result
        }

        if (ctx.game.getPawnLongColumnIndex(ctx.enemySide) != colTo) {
            // плохая вертикаль
            return result
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
            return addToResult(result, Point.of(rowTo, colTo))
        }

        return result
    }

    private fun getMovesByOffsets(
        offsets: Set<Vector>,
        kingAttacker: Point?,
        kingPossibleAttackerForObstacle: Point?,
        ctx: InnerContext
    ): List<Point> {
        var result: List<Point> = Point.pointsOf()

        for (offset in offsets) {
            val rowTo = ctx.pointFrom.row + offset.row
            val colTo = ctx.pointFrom.col + offset.col

            if (isOutOfBoard(rowTo, colTo)) {
                continue
            }

            val piece = ctx.getPieceNullable(rowTo, colTo)

            if (piece == null) {
                // свободная точка - ход доступен (если пройдет проверки)
                if (isAvailableMove(rowTo, colTo, kingAttacker, kingPossibleAttackerForObstacle, ctx)) {
                    result = addToResult(result, Point.of(rowTo, colTo))
                }
            } else if (ctx.isEnemy(piece)) {
                // рубим врага (если ход пройдет проверки)
                if (isDestroyingThreatMove(rowTo, colTo, kingAttacker, kingPossibleAttackerForObstacle)) {
                    result = addToResult(result, Point.of(rowTo, colTo))
                }
            }
        }

        return result
    }

    private fun getMovesByDirections(
        directions: Set<Vector>,
        kingAttacker: Point?,
        kingPossibleAttackerForObstacle: Point?,
        ctx: InnerContext
    ): List<Point> {

        var result: List<Point> = Point.pointsOf()

        for (direction in directions) {
            var rowTo: Int = ctx.pointFrom.row
            var colTo: Int = ctx.pointFrom.col

            while (true) {
                rowTo += direction.row
                colTo += direction.col

                if (isOutOfBoard(rowTo, colTo)) {
                    break
                }

                val piece = ctx.getPieceNullable(rowTo, colTo)

                if (piece == null) {
                    // свободная точка - ход доступен (если пройдет проверки)
                    if (isAvailableMove(rowTo, colTo, kingAttacker, kingPossibleAttackerForObstacle, ctx)) {
                        result = addToResult(result, Point.of(rowTo, colTo))
                    }
                    continue
                }

                if (ctx.isEnemy(piece)) {
                    // рубим врага - текущая точка доступна для хода (если пройдет проверки), но перепрыгнуть ее нельзя, поэтому делаем после break
                    if (isDestroyingThreatMove(rowTo, colTo, kingAttacker, kingPossibleAttackerForObstacle)) {
                        result = addToResult(result, Point.of(rowTo, colTo))
                    }
                }
                // дальнейшее следование по вектору невозможно, потому что мы уперлись в фигуру(свою или чужую - не важно)
                break
            }
        }

        return result
    }

    private fun addToResult(list: List<Point>, point: Point): List<Point> {
        when (list.size) {
            0 -> {
                return Point.pointsOf(point)
            }
            1 -> {
                return Point.pointsOf(list[0], point)
            }
            2 -> {
                return Point.pointsOf(list[0], list[1], point)
            }
            3 -> {
                return mutableListOf(list[0], list[1], list[2], point)
            }
            else -> {
                check(list is MutableList) { "input list must be mutable!" }
                list.add(point)
                return list
            }
        }
    }

    private fun isDestroyingThreatMove(
        rowTo: Int,
        colTo: Int,
        kingAttacker: Point?,
        kingPossibleAttackerForObstacle: Point?
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
        kingAttacker: Point?,
        kingPossibleAttackerForObstacle: Point?,
        ctx: InnerContext
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
        kingPossibleAttackerForObstacle: Point,
        kingPoint: Point
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
    private fun canDefendKing(rowTo: Int, colTo: Int, kingAttacker: Point, ctx: InnerContext): Boolean {
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

    private fun canBeObstacle(rowTo: Int, colTo: Int, kingThreat: Point, kingPoint: Point): Boolean {
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

    private fun findPawnThreatsToKing(kingPoint: Point, kingSide: Side, chessboard: IUnmodifiableChessboard): Point? {
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

    private fun findKnightThreatsToKing(kingPoint: Point, kingSide: Side, chessboard: IUnmodifiableChessboard): Point? {
        val enemySide = kingSide.reverse()

        for (offset in knightOffsets) {
            val row = kingPoint.row + offset.row
            val col = kingPoint.col + offset.col

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

    private fun findVectorPieceThreatsToKing(kingPoint: Point, kingSide: Side, chessboard: IUnmodifiableChessboard): List<Point> {
        var result: List<Point> = Point.pointsOf()

        val allPossibleAttackerVectors = pieceVectorsMap[QUEEN]!!

        for (vector in allPossibleAttackerVectors) {
            val notEmptyPoint = nextPieceByVector(vector.row, vector.col, kingPoint, chessboard)
                ?: continue // ничего не нашли и уперлись в край доски

            val foundPiece = chessboard.getPiece(notEmptyPoint)
            if (foundPiece.side == kingSide || !foundPiece.isTypeOf(BISHOP, ROOK, QUEEN)) {
                //мы ищем только вражеского слона/ладью/ферзя
                continue
            }

            if (foundPiece.type != QUEEN && !kingPoint.hasCommonVectorWith(notEmptyPoint, foundPiece.type)) {
                //найденная фигура не может атаковать короля по заданному вектору
                continue
            }

            result = if (result.isEmpty()) {
                Point.pointsOf(notEmptyPoint)
            } else {
                //двойной шах (оч редкий кейс)
                //тройной шах в этой игре невозможен
                return Point.pointsOf(result[0], notEmptyPoint)
            }
        }

        return result
    }

    private fun nextPieceByVector(
        rowDirection: Int,
        colDirection: Int,
        sourcePoint: Point,
        chessboard: IUnmodifiableChessboard
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

    private fun isOutOfBoard(row: Int, col: Int) = isIndexOutOfBounds(row) || isIndexOutOfBounds(col)

    private fun between(fromExclusive: Int, checkedValue: Int, toExclusive: Int): Boolean {
        return checkedValue > min(fromExclusive, toExclusive)
                && checkedValue < (max(fromExclusive, toExclusive))
    }

    private data class InnerContext(
        val game: IUnmodifiableGame,
        val chessboard: IUnmodifiableChessboard,
        val pointFrom: Point,
        val pieceFrom: Piece = chessboard.getPiece(pointFrom),
        val sideFrom: Side = pieceFrom.side,
        val enemySide: Side = sideFrom.reverse(),
        val kingPoint: Point = chessboard.getKingPoint(sideFrom)
    ) {
        internal fun isEnemy(piece: Piece) = piece.side == enemySide
        internal fun getPieceNullable(row: Int, col: Int) = chessboard.getPieceNullable(row, col)
        internal fun getPiece(point: Point) = chessboard.getPiece(point)
    }

    class Vector private constructor(
        val row: Int,
        val col: Int
    ) {
        companion object {

            private const val offset = 2

            private val VECTORS_POOL: Array<Array<Vector>> = Array(5) { rowDirection ->
                Array(5) { colDirection ->
                    Vector(rowDirection - offset, colDirection - offset)
                }
            }

            fun of(rowDirection: Int, colDirection: Int): Vector {
                return VECTORS_POOL[rowDirection + offset][colDirection + offset]
            }
        }

        fun isDiagonal(): Boolean {
            require(abs(row) <= 1 && abs(col) <= 1) {
                "operation not supported for not normalized vector: ${toString()}"
            }
            return abs(row) == abs(col)
        }
    }

    companion object {
        val pieceVectorsMap = EnumMap<PieceType, Set<Vector>>(PieceType::class.java)
        val knightOffsets = setOf(
            Vector.of(1, 2),
            Vector.of(2, 1),
            Vector.of(1, -2),
            Vector.of(2, -1),

            Vector.of(-1, 2),
            Vector.of(-2, 1),
            Vector.of(-1, -2),
            Vector.of(-2, -1)
        )

        init {
            pieceVectorsMap[ROOK] = setOf(
                Vector.of(1, 0),
                Vector.of(-1, 0),
                Vector.of(0, 1),
                Vector.of(0, -1)
            )
            pieceVectorsMap[BISHOP] = setOf(
                Vector.of(1, 1),
                Vector.of(1, -1),
                Vector.of(-1, 1),
                Vector.of(-1, -1)
            )
            pieceVectorsMap[QUEEN] = setOf(
                //ROOK
                Vector.of(1, 0),
                Vector.of(-1, 0),
                Vector.of(0, 1),
                Vector.of(0, -1),
                //BISHOP
                Vector.of(1, 1),
                Vector.of(1, -1),
                Vector.of(-1, 1),
                Vector.of(-1, -1)
            )
            pieceVectorsMap[KING] = pieceVectorsMap[QUEEN]
        }
    }
}