package com.example.chess.server.service.impl

import com.example.chess.server.logic.IUnmodifiableChessboard
import com.example.chess.server.logic.IUnmodifiableGame
import com.example.chess.server.logic.misc.Point
import com.example.chess.server.logic.misc.Points
import com.example.chess.server.logic.misc.isIndexOutOfBounds
import com.example.chess.server.logic.misc.with
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
 *      Особенность данного класса в том, что он практически не создает промежуточных объектов (Point.empty - не в счет)
 *          за исключением InnerContext-а и пишет результат сразу в результирующую коллекцию
 *          таким образом минимизируется выделяемая память и сильно снижается нагрузка на GC
 */
@Component
class MovesProvider : IMovesProvider {

    //collectTargetAttackers/Defenders

    override fun getAvailableMoves(game: IUnmodifiableGame, chessboard: IUnmodifiableChessboard, pointFrom: Point): List<Point> {
        var accumulator = Points.empty()

        collectMovesFrom(game, chessboard, pointFrom) {
            accumulator = accumulator.with(it)
            true
        }

        return accumulator
    }

    override fun isUnderCheck(game: IUnmodifiableGame, chessboard: IUnmodifiableChessboard, kingSide: Side): Boolean {
        val kingPoint = chessboard.getKingPoint(kingSide)
        return canAttackTarget(game, chessboard, kingPoint, kingSide.reverse())
    }

    private fun canAttackTarget(game: IUnmodifiableGame, chessboard: IUnmodifiableChessboard, targetPoint: Point, attackerSide: Side): Boolean {
        collectTargetThreatsOrDefenders(game, chessboard, targetPoint, attackerSide, false) {
            return true
        }
        return false
    }

    override fun getTargetThreats(game: IUnmodifiableGame, chessboard: IUnmodifiableChessboard, targetPoint: Point, isBatterySupported: Boolean): List<Point> {
        val targetPiece = chessboard.getPiece(targetPoint)
        val expectedSide = targetPiece.side.reverse()
        var result: List<Point> = Points.empty()

        collectTargetThreatsOrDefenders(game, chessboard, targetPoint, expectedSide, isBatterySupported) {
            result = result.with(it)
        }
        return result
    }

    override fun getTargetDefenders(game: IUnmodifiableGame, chessboard: IUnmodifiableChessboard, targetPoint: Point, isBatterySupported: Boolean): List<Point> {
        val targetPiece = chessboard.getPiece(targetPoint)
        val expectedSide = targetPiece.side
        var result: List<Point> = Points.empty()

        collectTargetThreatsOrDefenders(game, chessboard, targetPoint, expectedSide, isBatterySupported) {
            result = result.with(it)
        }
        return result
    }

    override fun getTargetThreatsCount(game: IUnmodifiableGame, chessboard: IUnmodifiableChessboard, targetPoint: Point): Int {
        val targetPiece = chessboard.getPiece(targetPoint)
        val expectedSide = targetPiece.side.reverse()
        var counter = 0

        collectTargetThreatsOrDefenders(game, chessboard, targetPoint, expectedSide, true) {
            counter++
        }
        return counter
    }

    override fun getTargetDefendersCount(game: IUnmodifiableGame, chessboard: IUnmodifiableChessboard, targetPoint: Point): Int {
        val targetPiece = chessboard.getPiece(targetPoint)
        val expectedSide = targetPiece.side
        var counter = 0

        collectTargetThreatsOrDefenders(game, chessboard, targetPoint, expectedSide, true) {
            counter++
        }
        return counter
    }

    private inline fun collectTargetThreatsOrDefenders(
        game: IUnmodifiableGame,
        chessboard: IUnmodifiableChessboard,
        targetPoint: Point,
        expectedSide: Side,
        isBatterySupported: Boolean,
        collectFunction: (Point) -> Unit
    ) {
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

        //TODO: мы больше не уверены что король не под шахом, из-за изменений работы алгоритма -> больше нельзя передавать null просто так
        //TODO: я совершенно точно не учитываю en-passant
        collectVectorMovesToTarget(chessboard, targetPoint, expectedSide, isBatterySupported, collectFunction)
        collectKnightMovesToTarget(chessboard, targetPoint, expectedSide, collectFunction)
    }

    private inline fun collectKnightMovesToTarget(chessboard: IUnmodifiableChessboard, targetPoint: Point, expectedSide: Side, collectFunction: (Point) -> Unit) {
        for (vector in knightOffsets) {
            val row = targetPoint.row + vector.row
            val col = targetPoint.col + vector.col

            if (isOutOfBoard(row, col)) {
                continue
            }

            val piece = chessboard.getPieceNullable(row, col)
                ?: continue

            if (piece.side != expectedSide) {
                // нас интересует только expectedSide
                continue
            }

            if (piece.isTypeOf(KNIGHT)) {
                collectFunction.invoke(Point.of(row, col))
            }
        }
    }

    /**
     * 1) Для каждого из 8 направлений идет от targetPoint по вектору (направлению)
     * 2) Если встретит угрозу (вражескую для targetPoint фигуру), которая может срубить targetPoint здесь и сейчас
     * 3) Увеличивает счетчик на единицу
     * 4) Может продолжить идти по тому же направлению после увеличения счетчика, чтобы вычислить "батарею", которая будет учтена счетчиком
     */
    private inline fun collectVectorMovesToTarget(
        chessboard: IUnmodifiableChessboard,
        targetPoint: Point,
        expectedSide: Side,
        isBatterySupported: Boolean,
        collectFunction: (Point) -> Unit
    ) {
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

                if (foundPiece.side != expectedSide) {
                    // уперлись в фигуру не того цвета
                    // дальнейшее следование по вектору не имеет смысла
                    break
                }

                if (offset == 1 && foundPiece.isTypeOf(KING)) {
                    // на этапе подсчета нас вроде не интересует факт того, что король может залезть под шах
                    collectFunction.invoke(Point.of(row, col))
                    // батарея Q -> K -> targetPoint невалидна, т.к. она подразумевает что король (K) пойдет в размен, что недопустимо
                    // поэтому дальнейшее следование по этому вектору не имеет смысла
                    break

                } else if (offset == 1 && foundPiece.isTypeOf(PAWN) && expectedSide.reverse().pawnRowDirection == vector.row && vector.isDiagonal()) {
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

                val kingThreatsForCurrentObstacle = getKingThreatForCurrentObstacle(chessboard, Point.of(row, col))

                // выбранную фигуру можно двигать, она не защищает короля от шаха
                // либо рубит фигуру, от шаха которой она защищает короля
                if (kingThreatsForCurrentObstacle == null || kingThreatsForCurrentObstacle == targetPoint) {
                    collectFunction.invoke(Point.of(row, col))
                    // за фигурой может другая фигура такого же типа (назовем такое сочетание батареей)
                    // примеры батарей: R -> R -> targetPoint,  Q -> P -> targetPoint, Q -> B -> targetPoint
                    // так вот батарея из фигур имеет значение для подсчета
                    if (isBatterySupported) {
                        continue
                    } else {
                        break
                    }
                } else {
                    // если эту фигуру двигать нельзя, то остальные (стоящие сзади) не помогут и подавно
                    break
                }
            }
        }
    }

    private fun collectMovesFrom(game: IUnmodifiableGame, chessboard: IUnmodifiableChessboard, pointFrom: Point, accumulator: (Point) -> Boolean) {
        val pieceFrom = chessboard.getPiece(pointFrom)
        val kingPoint = chessboard.getKingPoint(pieceFrom.side)
        val enemySide = pieceFrom.side.reverse()

        if (pieceFrom.isKing()) {
            //ходы короля слишком непохожи на другие, т.к. нас уже неинтересуют текущие шахи, а так же препятствия
            return collectKingMoves(game, chessboard, pointFrom, accumulator)
        }

        val kingAttackers = getTargetThreats(game, chessboard, kingPoint, false)
        require(kingAttackers.size <= 2) { "triple check unsupported in chess game.\r\n ${chessboard.toPrettyString()}" }

        if (kingAttackers.size == 2) {
            //двойной шах, ходить можно только королем
            return
        }

        val kingAttacker = if (kingAttackers.isNotEmpty()) kingAttackers[0] else null
        val kingPossibleAttackerForObstacle = getKingThreatForCurrentObstacle(chessboard, pointFrom)

        val onlyAvailableAccumulator: (Point) -> Boolean = lambda@{
            val pieceTo = chessboard.getPieceNullable(it)

            if (pieceTo == null && isAvailableHarmlessMove(chessboard, kingPoint, kingAttacker, kingPossibleAttackerForObstacle, it)) {
                return@lambda accumulator.invoke(it)
            } else if (pieceTo?.side == enemySide && isAvailableHarmfulMove(pointFrom, pieceFrom, kingAttacker, kingPossibleAttackerForObstacle, it)) {
                return@lambda accumulator.invoke(it)
            }
            return@lambda true
        }

        when (pieceFrom.type) {
            PAWN -> collectPawnMoves(game, chessboard, pointFrom, onlyAvailableAccumulator)
            KNIGHT -> collectOffsetMoves(chessboard, knightOffsets, pointFrom, onlyAvailableAccumulator)
            BISHOP -> collectVectorMoves(chessboard, pieceVectorsMap[BISHOP]!!, pointFrom, onlyAvailableAccumulator)
            ROOK -> collectVectorMoves(chessboard, pieceVectorsMap[ROOK]!!, pointFrom, onlyAvailableAccumulator)
            QUEEN -> collectVectorMoves(chessboard, pieceVectorsMap[QUEEN]!!, pointFrom, onlyAvailableAccumulator)
            else -> throw UnsupportedOperationException("unsupported piece type: ${pieceFrom.type}")
        }
    }

    private fun isAvailableHarmfulMove(
        pointFrom: Point,
        pieceFrom: Piece,
        kingAttacker: Point?,
        kingPossibleAttackerForObstacle: Point?,
        pointTo: Point
    ): Boolean {

        if (kingAttacker != null && kingPossibleAttackerForObstacle != null) {
            return false
        }

        if (kingAttacker != null) {
            if (pieceFrom.isPawn()) {
                return kingAttacker.isEqual(pointFrom.row, pointTo.col)
            }

            return kingAttacker.isEqual(pointTo.row, pointTo.col)
        }

        if (kingPossibleAttackerForObstacle != null) {
            return kingPossibleAttackerForObstacle.isEqual(pointTo.row, pointTo.col)
        }

        return true
    }

    /**
     * 1) Находит защищаемого короля, где protectedKing.side == obstaclePoint.side
     * 2) Определяет находится ли препятствие (obstaclePoint) на одной диагонали/вертикали/горизонтали с защищаемым королем (protectedKingPoint)
     * 3) Если нет - возвращает null
     * 4) Если все же находится, то определяет вектор направления "защищаемый король(protectedKing) >>> препятствие(obstacle) >>> угроза?(threat))
     * 5) Вызывает getKingThreatPointForObstacle, чтобы найти угрозу (threatPoint)
     */
    private fun getKingThreatForCurrentObstacle(
        chessboard: IUnmodifiableChessboard,
        obstaclePoint: Point
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
                expectedThreatType
            )
        }

        return null
    }

    /**
     *
     * @see getKingThreatForCurrentObstacle
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
                        //TODO: проверка упразднена, потому что теперь валидный код может провоцировать исключение ниже
//                        check(kingAttacker != null && kingAttacker.isEqual(row, col)) {
//                            "kingThreat(${Point.of(row, col)}) is not equals with kingAttacker($kingAttacker)"
//                        }
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

    private fun collectKingMoves(game: IUnmodifiableGame, chessboard: IUnmodifiableChessboard, pointFrom: Point, accumulator: (Point) -> Boolean) {

        val pieceFrom = chessboard.getPiece(pointFrom)
        val sideFrom = pieceFrom.side
        val kingPoint = chessboard.getKingPoint(sideFrom)

        check(kingPoint == pointFrom) {
            "king expected in point from: $pointFrom, but found in $kingPoint"
        }

        val kingMoveOffsets = pieceVectorsMap[QUEEN]!!

        // try add simple king moves
        for (offset in kingMoveOffsets) {
            val rowTo = kingPoint.row + offset.row
            val colTo = kingPoint.col + offset.col

            if (isAvailableKingMove(game, chessboard, sideFrom, rowTo, colTo)) {
                accumulator.invoke(Point.of(rowTo, colTo))
            }
        }

        // под шахом рокировка невозможна
        if (isUnderCheck(game, chessboard, sideFrom)) {
            return
        }

        // try add castling moves
        if (game.isLongCastlingAvailable(sideFrom)) {
            tryAddCastlingMove(game, chessboard, kingPoint, true, accumulator)
        }

        if (game.isShortCastlingAvailable(sideFrom)) {
            tryAddCastlingMove(game, chessboard, kingPoint, false, accumulator)
        }
    }

    private fun isAvailableKingMove(
        game: IUnmodifiableGame,
        chessboard: IUnmodifiableChessboard,
        kingSide: Side,
        rowTo: Int,
        colTo: Int
    ): Boolean {

        if (isOutOfBoard(rowTo, colTo)) {
            // нельзя вставать за пределы доски
            return false
        }

        if (chessboard.getKingPoint(kingSide.reverse()).isBorderedWith(rowTo, colTo)) {
            // нельзя вставать вплотную к вражескому королю
            return false
        }

        if (chessboard.getPieceNullable(rowTo, colTo)?.side == kingSide) {
            // нельзя рубить своих
            return false
        }

        if (isUnderCheck(game, chessboard, kingSide)) {
            // нельзя вставать под шах (случай когда рубим фигуру под защитой тоже входит в этот кейс)
            return false
        }

        //проверки пройдены. можно ходить
        return true
    }

    private fun tryAddCastlingMove(
        game: IUnmodifiableGame,
        chessboard: IUnmodifiableChessboard,
        kingPoint: Point,
        isLong: Boolean,
        accumulator: (Point) -> Boolean
    ) {
        val kingSide = chessboard.getPiece(kingPoint).side

        val direction = if (isLong) 1 else -1
        val rookCol = if (isLong) ROOK_LONG_COLUMN_INDEX else ROOK_SHORT_COLUMN_INDEX

        val row = kingPoint.row
        var offset = 1

        while (true) {
            val col = kingPoint.col + direction * offset

            if (col == rookCol) {
                // достигли ладьи, с которой рокируемся
                break
            }

            if (chessboard.getPieceNullable(row, col) != null) {
                // между королем и ладьей есть фигура. рокировка невозможна
                return
            }

            if (offset == 1 && canAttackTarget(game, chessboard, Point.of(row, col), kingSide.reverse())) {
                // рокировка через битое поле невозможна
                return
            }

            if (offset == 2 && isUnderCheck(game, chessboard, kingSide)) {
                // под шах вставать при рокировке тоже как ни странно - нельзя
                return
            }

            offset++
        }

        accumulator.invoke(Point.of(kingPoint.row, kingPoint.col + 2 * direction))
    }

    private fun collectPawnMoves(
        game: IUnmodifiableGame,
        chessboard: IUnmodifiableChessboard,
        pointFrom: Point,
        accumulator: (Point) -> Boolean
    ) {
        val pieceFrom = chessboard.getPiece(pointFrom)
        val sideFrom = pieceFrom.side

        val rowOffset = sideFrom.pawnRowDirection
        val rowTo = pointFrom.row + rowOffset
        var colTo = pointFrom.col

        // simple move
        val simpleMoveAvailable = tryAddPawnMove(chessboard, pointFrom, sideFrom, rowTo, colTo, accumulator)

        // long distance move
        if (simpleMoveAvailable && sideFrom.pawnInitialRow == pointFrom.row) {
            tryAddPawnMove(chessboard, pointFrom, sideFrom, rowTo + rowOffset, colTo, accumulator)
        }

        colTo = pointFrom.col + 1

        // attack
        tryAddPawnMove(chessboard, pointFrom, sideFrom, rowTo, colTo, accumulator)
        // en passant
        tryAddPawnEnPassantMove(game, chessboard, pointFrom, sideFrom, rowTo, colTo, accumulator)

        colTo = pointFrom.col - 1

        // attack
        tryAddPawnMove(chessboard, pointFrom, sideFrom, rowTo, colTo, accumulator)
        // en passant
        tryAddPawnEnPassantMove(game, chessboard, pointFrom, sideFrom, rowTo, colTo, accumulator)
    }

    private fun tryAddPawnMove(
        chessboard: IUnmodifiableChessboard,
        pawnPointFrom: Point,
        pawnSide: Side,
        rowTo: Int,
        colTo: Int,
        accumulator: (Point) -> Boolean
    ): Boolean {

        if (isOutOfBoard(rowTo, colTo)) {
            return false
        }

        if (pawnPointFrom.col == colTo && chessboard.getPieceNullable(rowTo, colTo) != null) {
            // обычный ход возможен только на пустую клетку и не может рубить ни чужих ни своих
            return false
        }

        if (pawnPointFrom.col != colTo && chessboard.getPieceNullable(rowTo, colTo)?.side != pawnSide.reverse()) {
            // атака доступна только если по диагонали от пешки находится вражеская фигура
            return false
        }

        accumulator.invoke(Point.of(rowTo, colTo))
        return true
    }

    private fun tryAddPawnEnPassantMove(
        game: IUnmodifiableGame,
        chessboard: IUnmodifiableChessboard,
        pawnPointFrom: Point,
        pawnSide: Side,
        rowTo: Int,
        colTo: Int,
        accumulator: (Point) -> Boolean
    ) {
        if (pawnSide.pawnEnPassantStartRow != pawnPointFrom.row) {
            // плохая горизонталь
            return
        }

        if (game.getPawnLongColumnIndex(pawnSide.reverse()) != colTo) {
            // плохая вертикаль
            return
        }

        check(!isOutOfBoard(rowTo, colTo)) {
            // все плохое
            "isOutOfBoard: $pawnPointFrom -> ${Point.of(rowTo, colTo)}"
        }

        check(chessboard.getPieceNullable(rowTo, colTo) == null) {
            // при взятии на проходе пешку можно ставить только на пустую клетку,
            //      но факт, что само по себе взятие на проходе доступно означает что эту клетку
            //      только что перепрыгнула пешка противника делая long distance move, что фантастически странно
            "en passant point[move.to]=${Point.of(rowTo, colTo)} can't be not empty, but found: ${chessboard.getPieceNullable(
                rowTo,
                colTo
            )}"
        }

        check(chessboard.getPieceNullable(pawnPointFrom.row, colTo) == Piece.of(pawnSide.reverse(), PAWN)) {
            // справа(или слева) от пешки на pointFrom должна стоять пешка противника.
            // только такая позиция может привести к взятию на проходе
            "en passant is not available for current state: expected: ${Piece.of(pawnSide.reverse(), PAWN)} " +
                    "on position=${Point.of(pawnPointFrom.row, colTo)}, " +
                    "but found: chessboard.getPieceNullable(pointFrom.row, colTo)"
        }

        // наконец все проверки пройдены. ход ЭВЭЙЛЭБЛ!
        accumulator.invoke(Point.of(rowTo, colTo))
    }

    private fun collectOffsetMoves(
        chessboard: IUnmodifiableChessboard,
        offsets: Set<Vector>,
        pointFrom: Point,
        accumulator: (Point) -> Boolean
    ) {
        val pieceFrom = chessboard.getPiece(pointFrom)

        for (offset in offsets) {
            val rowTo = pointFrom.row + offset.row
            val colTo = pointFrom.col + offset.col

            if (isOutOfBoard(rowTo, colTo)) {
                continue
            }

            if (pieceFrom.side == chessboard.getPieceNullable(rowTo, colTo)?.side) {
                //защита от friendlyFire
                //мы можем либо встать на пустую клетку, либо срубить вражескую фигуру, а вот своих рубить нельзя =)
                continue
            }

            accumulator.invoke(Point.of(rowTo, colTo))
        }
    }

    private fun collectVectorMoves(
        chessboard: IUnmodifiableChessboard,
        directions: Set<Vector>,
        pointFrom: Point,
        accumulator: (Point) -> Boolean
    ) {
        val pieceFrom = chessboard.getPiece(pointFrom)

        for (direction in directions) {
            var rowTo: Int = pointFrom.row
            var colTo: Int = pointFrom.col

            while (true) {
                rowTo += direction.row
                colTo += direction.col

                if (isOutOfBoard(rowTo, colTo)) {
                    break
                }

                val piece = chessboard.getPieceNullable(rowTo, colTo)

                if (piece == null) {
                    accumulator.invoke(Point.of(rowTo, colTo))
                    continue
                }

                if (pieceFrom.side != piece.side) {
                    accumulator.invoke(Point.of(rowTo, colTo))
                }

                // дальнейшее следование по вектору невозможно, потому что мы уперлись в фигуру(свою или чужую - не важно)
                break
            }
        }
    }

    private fun isAvailableHarmlessMove(
        chessboard: IUnmodifiableChessboard,
        kingPoint: Point,
        kingAttacker: Point?,
        kingPossibleAttackerForObstacle: Point?,
        pointTo: Point
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
            return canDefendKing(chessboard, kingPoint, kingAttacker, pointTo)
        }

        if (kingPossibleAttackerForObstacle != null) {
            return canMoveObstacle(kingPoint, kingPossibleAttackerForObstacle, pointTo)
        }

        // ничем не ограниченный ход: kingAttacker == null && kingPossibleAttackerForObstacle == null
        return true
    }

    private fun canMoveObstacle(kingPoint: Point, kingPossibleAttackerForObstacle: Point, pointTo: Point): Boolean {
        if (kingPossibleAttackerForObstacle == pointTo) {
            // рубим фигуру, из-за которой перемещаемая фигура являлась obstacle
            return true
        }
        // перемещение в рамках вектора допустимо
        return canBeObstacle(kingPoint, kingPossibleAttackerForObstacle, pointTo)
    }

    /**
     * return может ли данный ход защитить короля от шаха, источником которого являеится kingAttacker
     *
     * Защитить короля можно срубив атакующую фигуру, либо закрыться (если возможно)
     */
    private fun canDefendKing(
        chessboard: IUnmodifiableChessboard,
        kingPoint: Point,
        kingAttacker: Point,
        pointTo: Point
    ): Boolean {
        if (kingAttacker == pointTo) {
            // рубим фигуру, объявившую шах.
            return true
        }

        if (kingAttacker.isBorderedWith(kingPoint)) {
            // фигура, объявившая шах стоит вплотную к нашему королю, но при этом мы ее не рубим. такой ход недопустим
            return false
        }

        val kingAttackerPiece = chessboard.getPiece(kingAttacker)

        if (kingAttackerPiece.type == KNIGHT) {
            // нам объявили шах конем
            // мы его не рубим, а загородиться от коня невозможно. false
            return false
        }

        return canBeObstacle(kingPoint, kingAttacker, pointTo)
    }

    private fun canBeObstacle(kingPoint: Point, kingAttacker: Point, pointTo: Point): Boolean {
        if (!kingAttacker.hasCommonVectorWith(pointTo)) {
            // данный ход находится где-то сбоку от вектора шаха и никак не защищает короля
            return false
        }

        return when {
            // horizontal vector
            kingPoint.row == kingAttacker.row -> pointTo.row == kingPoint.row && between(kingPoint.col, pointTo.col, kingAttacker.col)
            // vertical vector
            kingPoint.col == kingAttacker.col -> pointTo.col == kingPoint.col && between(kingPoint.row, pointTo.row, kingAttacker.row)
            // diagonal vector
            else -> {
                val rowOffset = kingAttacker.row - kingPoint.row
                val colOffset = kingAttacker.col - kingPoint.col

                val absAttackerRowOffset = abs(rowOffset)
                check(absAttackerRowOffset == abs(colOffset)) {
                    "kingThreat($kingAttacker) has different diagonal vector with kingPoint($kingPoint)"
                }

                return between(kingPoint.row, pointTo.row, kingAttacker.row)
                        && between(kingPoint.col, pointTo.col, kingAttacker.col)
                        // moveTo доложен быть на одной диагонали с атакуемым королем
                        && abs(pointTo.row - kingPoint.row) == abs(pointTo.col - kingPoint.col)
            }
        }
    }

    private fun isOutOfBoard(row: Int, col: Int) = isIndexOutOfBounds(row) || isIndexOutOfBounds(col)

    private fun between(fromExclusive: Int, checkedValue: Int, toExclusive: Int): Boolean {
        return checkedValue > min(fromExclusive, toExclusive)
                && checkedValue < (max(fromExclusive, toExclusive))
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