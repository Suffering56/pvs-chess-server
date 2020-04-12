package com.example.chess.server.service.impl

import com.example.chess.server.PointsCollector
import com.example.chess.server.logic.IUnmodifiableChessboard
import com.example.chess.server.logic.IUnmodifiableGame
import com.example.chess.server.logic.misc.*
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
import java.util.stream.Collectors
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
    override fun getAvailableMovesFrom(game: IUnmodifiableGame, chessboard: IUnmodifiableChessboard, pointFrom: Point): List<Point> {
        var accumulator = Points.empty()

        collectMovesFrom(game, chessboard, pointFrom) {
            accumulator = accumulator.with(it)
        }

        return accumulator
    }

    override fun getTargetAttackers(game: IUnmodifiableGame, chessboard: IUnmodifiableChessboard, targetPoint: Point, isBatterySupported: Boolean): List<Point> {
        val targetPiece = chessboard.getPiece(targetPoint)
        val expectedSide = targetPiece.side.reverse()
        var result: List<Point> = Points.empty()

        collectMovesTo(game, chessboard, targetPoint, expectedSide, isBatterySupported, null) {
            result = result.with(it)
        }
        return result
    }

    override fun getTargetDefenders(game: IUnmodifiableGame, chessboard: IUnmodifiableChessboard, targetPoint: Point, isBatterySupported: Boolean): List<Point> {
        val targetPiece = chessboard.getPiece(targetPoint)
        val expectedSide = targetPiece.side
        var result: List<Point> = Points.empty()

        collectMovesTo(game, chessboard, targetPoint, expectedSide, isBatterySupported, null) {
            result = result.with(it)
        }
        return result
    }

    override fun isUnderCheck(game: IUnmodifiableGame, chessboard: IUnmodifiableChessboard, kingSide: Side): Boolean {
        val kingPoint = chessboard.getKingPoint(kingSide)
        return canAttackTarget(game, chessboard, kingPoint, kingSide.reverse(), null)
    }

    override fun isMoveAvailable(game: IUnmodifiableGame, chessboard: IUnmodifiableChessboard, pointFrom: Point, pointTo: Point): Boolean {
        val pieceFrom = chessboard.getPiece(pointFrom)
        val kingSide = pieceFrom.side
        val kingPoint = chessboard.getKingPoint(kingSide)

        if (pieceFrom.isTypeOf(KING)) {
            //ходы короля слишком непохожи на другие, т.к. нас уже неинтересуют текущие шахи, а так же препятствия
            return isAvailableKingMove(game, chessboard, kingSide, pointFrom, pointTo.row, pointTo.col)
        }

        val kingAttackers = getTargetAttackers(game, chessboard, kingPoint, false)
        require(kingAttackers.size <= 2) {
            "triple check unsupported in chess game.\r\n" +
                    "move: ${Move.of(pointFrom, pointTo).toPrettyString(pieceFrom)}\r\n" +
                    "kingPoint: ${kingPoint.toPrettyString()}\r\n" +
                    "kingAttackers: ${kingAttackers.stream().map { it.toPrettyString() }.collect(Collectors.joining(", "))}\r\n" +
                    chessboard.toPrettyString(pointFrom, pointTo)
        }

        if (kingAttackers.size == 2) {
            //двойной шах, ходить можно только королем
            return false
        }

        val kingAttacker = if (kingAttackers.isNotEmpty()) kingAttackers[0] else null
        val kingPossibleAttackerForObstacle = getKingThreatForCurrentObstacle(chessboard, pointFrom)

        return isNotKingMoveAvailable(chessboard, pieceFrom, pointFrom, pointTo, kingPoint, kingAttacker, kingPossibleAttackerForObstacle)
    }

    /**
     * @param ignoredPoint - если мы проверяем что не встаем королём под шах, нужно в ignoredPoint передать точку где стоит король сейчас,
     *  потому что может оказаться, что эта точка будет считаться препятствием, хотя на самом деле короля там не будет и мы встанем под шах
     */
    private fun canAttackTarget(game: IUnmodifiableGame, chessboard: IUnmodifiableChessboard, targetPoint: Point, attackerSide: Side, ignoredPoint: Point?): Boolean {
        var canAttack = false

        collectMovesTo(game, chessboard, targetPoint, attackerSide, false, ignoredPoint) {
            canAttack = true
        }
        return canAttack
    }

    private fun collectMovesFrom(
        game: IUnmodifiableGame,
        chessboard: IUnmodifiableChessboard,
        pointFrom: Point,
        collector: PointsCollector
    ) {
        val pieceFrom = chessboard.getPiece(pointFrom)
        val kingPoint = chessboard.getKingPoint(pieceFrom.side)

        if (pieceFrom.isTypeOf(KING)) {
            //ходы короля слишком непохожи на другие, т.к. нас уже неинтересуют текущие шахи, а так же препятствия
            return collectKingMoves(game, chessboard, pointFrom, collector)
        }

        val kingAttackers = getTargetAttackers(game, chessboard, kingPoint, false)
        require(kingAttackers.size <= 2) { "triple check unsupported in chess game.\r\n ${chessboard.toPrettyString(*kingAttackers.toTypedArray())}" }

        if (kingAttackers.size == 2) {
            //двойной шах, ходить можно только королем
            return
        }

        val kingAttacker = if (kingAttackers.isNotEmpty()) kingAttackers[0] else null
        val kingPossibleAttackerForObstacle = getKingThreatForCurrentObstacle(chessboard, pointFrom)

        val onlyAvailableCollector: PointsCollector = { pointTo ->
            if (isNotKingMoveAvailable(chessboard, pieceFrom, pointFrom, pointTo, kingPoint, kingAttacker, kingPossibleAttackerForObstacle)) {
                collector.invoke(pointTo)
            }
        }

        when (pieceFrom.type) {
            PAWN -> collectPawnMoves(game, chessboard, pointFrom, onlyAvailableCollector)
            KNIGHT -> collectOffsetMoves(chessboard, knightOffsets, pointFrom, onlyAvailableCollector)
            BISHOP -> collectVectorMoves(chessboard, pieceVectorsMap[BISHOP]!!, pointFrom, onlyAvailableCollector)
            ROOK -> collectVectorMoves(chessboard, pieceVectorsMap[ROOK]!!, pointFrom, onlyAvailableCollector)
            QUEEN -> collectVectorMoves(chessboard, pieceVectorsMap[QUEEN]!!, pointFrom, onlyAvailableCollector)
            else -> throw UnsupportedOperationException("unsupported piece type: ${pieceFrom.type}")
        }
    }

    private fun isNotKingMoveAvailable(
        chessboard: IUnmodifiableChessboard,
        pieceFrom: Piece,
        pointFrom: Point,
        pointTo: Point,
        kingPoint: Point,
        kingAttacker: Point?,
        kingPossibleAttackerForObstacle: Point?
    ): Boolean {

        val pieceTo = chessboard.getPieceNullable(pointTo)

        if (pieceTo == null && isAvailableHarmlessMove(chessboard, pointFrom, kingPoint, kingAttacker, kingPossibleAttackerForObstacle, pointTo)) {
            return true
        } else if (pieceFrom.isEnemyFor(pieceTo) && isAvailableHarmfulMove(kingAttacker, kingPossibleAttackerForObstacle, pointTo)) {
            return true
        }
        return false
    }

    /**
     * Собирает все ходы, которые ведут в targetPoint (move.to == targetPoint),
     * а источником хода является фигура с expectedSide (pieceFrom == expectedSide)
     * Не фильтрует ходы на доступность, потому что:
     * а) будет некрасивая рекурсия.
     * б) не всегда это нужно. например не нужно в: isUnderCheck, либо ситуциях, когда собираем ходы для стороны, которая в общем-то сейчас и не ходит
     *
     * @param isBatterySupported - поддержка конструкций вида Q->R->R->targetPoint, или B->Q->P->targetPoint.
     */
    private fun collectMovesTo(
        game: IUnmodifiableGame,
        chessboard: IUnmodifiableChessboard,
        targetPoint: Point,
        expectedSide: Side,
        isBatterySupported: Boolean,
        ignoredPoint: Point?,
        collector: PointsCollector
    ) {
        collectEnPassantMoveToTarget(game, chessboard, targetPoint, expectedSide, collector)
        collectVectorMovesToTarget(chessboard, targetPoint, expectedSide, isBatterySupported, ignoredPoint, collector)
        collectKnightMovesToTarget(chessboard, targetPoint, expectedSide, collector)
    }

    /**
     * Сей метод может добавить в коллектор только один ход - взятие на проходе пешки, находящейся в targetPoint,
     * при условии, что:
     *  - expectedSide ялвяется противником для пешки из targetPoint,
     *  - а так же что все стандартные условия для взятия на проходе соблюдены,
     *  !- кроме ситуаций с недоступностью хода связанных с шахом
     */
    private fun collectEnPassantMoveToTarget(
        game: IUnmodifiableGame,
        chessboard: IUnmodifiableChessboard,
        targetPoint: Point,
        expectedSide: Side,
        collector: PointsCollector
    ) {
        val targetPiece = chessboard.getPieceNullable(targetPoint)
        // если в targetPoint пусто, то взятия на проходе быть не может (если внимательно читать описание метода)
            ?: return

        if (!targetPiece.isTypeOf(PAWN)) {
            // нас интересуют только пешки
            return
        }

        if (targetPiece.side == expectedSide) {
            // нас не интересуют ситуации с поиском defender-а, только attacker
            return
        }

        if (game.getPawnLongColumnIndex(targetPiece.side) != targetPoint.col) {
            // пешка из targetPoint на прошлом ходу не совершала длинного хода на 2 клетки
            return
        }

        // отлавливаем рассинхрон между стейтом game и chessboard
        require(targetPoint.row != expectedSide.pawnEnPassantStartRow) {
            "targetPoint: ${targetPoint.toPrettyString()} must be on the ${expectedSide.pawnEnPassantStartRow} row, but found in another place." +
                    "\r\n${chessboard.toPrettyString(targetPoint)}"
        }

        var counter = 0
        for (offset in -1..1 step 2) {
            val row = targetPoint.row
            val col = targetPoint.col + offset

            if (isOutOfBoard(row, col)) {
                continue
            }

            val foundPiece = chessboard.getPieceNullable(row, col)
                ?: continue

            if (foundPiece != Piece.of(expectedSide, PAWN)) {
                continue
            }

            collector.invoke(Point.of(row, col))
            counter++
            //TODO: break; возможно придется брать первый найденный
        }

        if (counter == 2) {
            System.err.println("two en-passant moves was added, it may provide incorrect situations,\r\n${chessboard.toPrettyString(targetPoint)}")
        }
    }

    private fun collectKnightMovesToTarget(chessboard: IUnmodifiableChessboard, targetPoint: Point, expectedSide: Side, collectFunction: PointsCollector) {
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
    private fun collectVectorMovesToTarget(
        chessboard: IUnmodifiableChessboard,
        targetPoint: Point,
        expectedSide: Side,
        isBatterySupported: Boolean,
        ignoredPoint: Point?,
        collectFunction: PointsCollector
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

                if (ignoredPoint?.isEqual(row, col) == true) {
                    //здесь стоит фигура, которая на самом деле будет перемещена, а значит эта точка должна считаться пустой при расчетах
                    continue
                }

                // если точка пуста - продолжаем поиск по вектору
                val foundPiece = chessboard.getPieceNullable(row, col) ?: continue

                if (foundPiece.side != expectedSide) { //TODO: && foundPoint != movedPiece
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

                // потенциально-доступный ход. коллектим
                collectFunction.invoke(Point.of(row, col))

                if (isBatterySupported) {
                    // за фигурой может другая фигура такого же типа (назовем такое сочетание батареей)
                    // примеры батарей: R -> R -> targetPoint,  Q -> P -> targetPoint, Q -> B -> targetPoint
                    // так вот батарея из фигур имеет значение для подсчета
                    continue
                } else {
                    break
                }
            }
        }
    }

    private fun isAvailableHarmfulMove(
        kingAttacker: Point?,
        kingPossibleAttackerForObstacle: Point?,
        pointTo: Point
    ): Boolean {

        if (kingAttacker != null && kingPossibleAttackerForObstacle != null) {
            return false
        }

        if (kingAttacker != null) {
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

    private fun collectKingMoves(game: IUnmodifiableGame, chessboard: IUnmodifiableChessboard, pointFrom: Point, collector: PointsCollector) {

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

            if (isAvailableKingMove(game, chessboard, sideFrom, pointFrom, rowTo, colTo)) {
                collector.invoke(Point.of(rowTo, colTo))
            }
        }

        // под шахом рокировка невозможна
        if (isUnderCheck(game, chessboard, sideFrom)) {
            return
        }

        // try add castling moves
        if (game.isLongCastlingAvailable(sideFrom)) {
            tryAddCastlingMove(game, chessboard, kingPoint, true, collector)
        }

        if (game.isShortCastlingAvailable(sideFrom)) {
            tryAddCastlingMove(game, chessboard, kingPoint, false, collector)
        }
    }

    private fun isAvailableKingMove(
        game: IUnmodifiableGame,
        chessboard: IUnmodifiableChessboard,
        kingSide: Side,
        pointFrom: Point,
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

        if (canAttackTarget(game, chessboard, targetPoint = Point.of(rowTo, colTo), attackerSide = kingSide.reverse(), ignoredPoint = pointFrom)) {
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
        collector: PointsCollector
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

            if (offset == 1 && canAttackTarget(game, chessboard, Point.of(row, col), kingSide.reverse(), null)) {
                // рокировка через битое поле невозможна
                return
            }

            if (offset == 2 && canAttackTarget(game, chessboard, Point.of(row, col), kingSide.reverse(), null)) {
                // под шах вставать при рокировке тоже как ни странно - нельзя
                return
            }

            offset++
        }

        collector.invoke(Point.of(kingPoint.row, kingPoint.col + 2 * direction))
    }

    private fun collectPawnMoves(
        game: IUnmodifiableGame,
        chessboard: IUnmodifiableChessboard,
        pointFrom: Point,
        collector: PointsCollector
    ) {
        val pieceFrom = chessboard.getPiece(pointFrom)
        val sideFrom = pieceFrom.side

        val rowOffset = sideFrom.pawnRowDirection
        val rowTo = pointFrom.row + rowOffset
        var colTo = pointFrom.col

        // simple move
        val simpleMoveAvailable = tryAddPawnMove(chessboard, pointFrom, sideFrom, rowTo, colTo, collector)

        // long distance move
        if (simpleMoveAvailable && sideFrom.pawnInitialRow == pointFrom.row) {
            tryAddPawnMove(chessboard, pointFrom, sideFrom, rowTo + rowOffset, colTo, collector)
        }

        colTo = pointFrom.col + 1

        // attack
        tryAddPawnMove(chessboard, pointFrom, sideFrom, rowTo, colTo, collector)
        // en passant
        tryAddPawnEnPassantMove(game, chessboard, pointFrom, sideFrom, rowTo, colTo, collector)

        colTo = pointFrom.col - 1

        // attack
        tryAddPawnMove(chessboard, pointFrom, sideFrom, rowTo, colTo, collector)
        // en passant
        tryAddPawnEnPassantMove(game, chessboard, pointFrom, sideFrom, rowTo, colTo, collector)
    }

    private fun tryAddPawnMove(
        chessboard: IUnmodifiableChessboard,
        pawnPointFrom: Point,
        pawnSide: Side,
        rowTo: Int,
        colTo: Int,
        collector: PointsCollector
    ): Boolean {

        if (isOutOfBoard(rowTo, colTo)) {
            return false
        }

        val attackedPiece = chessboard.getPieceNullable(rowTo, colTo)

        if (pawnPointFrom.col == colTo && attackedPiece != null) {
            // обычный ход возможен только на пустую клетку и не может рубить ни чужих ни своих
            return false
        }

        if (pawnPointFrom.col != colTo && !pawnSide.isEnemyFor(attackedPiece)) {
            // атака доступна только если по диагонали от пешки находится вражеская фигура
            return false
        }

        collector.invoke(Point.of(rowTo, colTo))
        return true
    }

    private fun tryAddPawnEnPassantMove(
        game: IUnmodifiableGame,
        chessboard: IUnmodifiableChessboard,
        pawnPointFrom: Point,
        pawnSide: Side,
        rowTo: Int,
        colTo: Int,
        collector: PointsCollector
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
        collector.invoke(Point.of(rowTo, colTo))
    }

    private fun collectOffsetMoves(
        chessboard: IUnmodifiableChessboard,
        offsets: Set<Vector>,
        pointFrom: Point,
        collector: PointsCollector
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

            collector.invoke(Point.of(rowTo, colTo))
        }
    }

    private fun collectVectorMoves(
        chessboard: IUnmodifiableChessboard,
        directions: Set<Vector>,
        pointFrom: Point,
        collector: PointsCollector
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
                    collector.invoke(Point.of(rowTo, colTo))
                    continue
                }

                if (pieceFrom.side != piece.side) {
                    collector.invoke(Point.of(rowTo, colTo))
                }

                // дальнейшее следование по вектору невозможно, потому что мы уперлись в фигуру(свою или чужую - не важно)
                break
            }
        }
    }

    private fun isAvailableHarmlessMove(
        chessboard: IUnmodifiableChessboard,
        pointFrom: Point,
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

            if (pointTo.col != pointFrom.col
                && (kingAttacker.isEqual(pointFrom.row, pointTo.col))
                && chessboard.getPiece(pointFrom).isTypeOf(PAWN)
            ) {
                //фантастическая ситуация, когда при взятии на проходе (en-passant) мы рубим пешку, объявившую королю шах
                return true
            }
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