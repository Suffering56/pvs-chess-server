package com.example.chess.server.service.impl

import com.example.chess.server.Destiny
import com.example.chess.server.logic.IChessboard
import com.example.chess.server.logic.IGame
import com.example.chess.server.logic.IUnmodifiableChessboard
import com.example.chess.server.logic.IUnmodifiableGame
import com.example.chess.server.logic.misc.*
import com.example.chess.server.service.IBotMoveSelector
import com.example.chess.server.service.IMovesProvider
import com.example.chess.server.tabs
import com.example.chess.shared.enums.Piece
import com.example.chess.shared.enums.PieceType
import com.example.chess.shared.enums.Side
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import java.util.stream.Stream
import kotlin.Comparator
import kotlin.collections.HashMap
import kotlin.random.Random
import kotlin.streams.toList

@Component
@ExperimentalUnsignedTypes
class BotMoveSelector : IBotMoveSelector {

    @Autowired private lateinit var movesProvider: IMovesProvider
    @Autowired private lateinit var applyMoveHandler: ApplyMoveHandler

    companion object {
        val PAWN_TRANSFORMATION_PIECE_STUB: Piece? = null
        const val CALCULATED_DEEP = 5
        const val THREADS_COUNT = 8
    }

    private class Statistic {
        val countersMap: MutableMap<String, AtomicLong> = HashMap()

        init {
            countersMap["totalTime"] = AtomicLong(0)
            countersMap["deepExchangeTime"] = AtomicLong(0)
            countersMap["actualizeTime"] = AtomicLong(0)
            countersMap["calculateDeepExchangeTime"] = AtomicLong(0)
            countersMap["getAvailableMovesTime"] = AtomicLong(0)
            countersMap["getTargetAttackersTime"] = AtomicLong(0)
            countersMap["getTargetDefendersTime"] = AtomicLong(0)
            countersMap["filterDeepAttackersTime"] = AtomicLong(0)

            countersMap["totalNodesCount"] = AtomicLong(0)
            countersMap["deepAttackersCount"] = AtomicLong(0)
        }

        companion object {
            fun <T> measureAndPrint(title: String, action: () -> T): T {
                val start = System.currentTimeMillis()
                val result: T = action.invoke()
                val invokeTime = System.currentTimeMillis() - start
                println("$title(sec) = ${invokeTime / 1000.0}")
                return result
            }
        }

        fun addCounterValue(counterName: String, count: Long) {
            try {
                requireNotNull(countersMap[counterName]) { "counter with name $counterName not found" }
                    .addAndGet(count)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        fun <T> measure(counterName: String, action: () -> T): T {
            val start = System.currentTimeMillis()
            val result: T = action.invoke()
            val invokeTime = System.currentTimeMillis() - start

            addCounterValue(counterName, invokeTime)
            return result
        }

        fun addValues(other: Statistic) {
            countersMap.forEach { (key, value) ->
                value.addAndGet(other.countersMap[key]!!.get())
            }
        }

        fun print(threadsCount: Int) {
            println("\r\nstatistic:")

            countersMap.forEach { (key, value) ->
                if (key.endsWith("Time")) {
                    println("$key(avg, sec): ${BigDecimal.valueOf(value.get() / 1000.0 / threadsCount).setScale(2, RoundingMode.HALF_UP).toPlainString()}")
                } else {
                    println("$key: ${value.get()}")
                }
            }
            println()
        }
    }

    fun selectBest(game: IUnmodifiableGame, chessboard: IUnmodifiableChessboard, botSide: Side): Move {
        return Statistic.measureAndPrint("globalInvokeTime") {
            val branches = chessboard.cellsStream(botSide)
                .flatMap { cell ->
                    movesProvider.getAvailableMovesFrom(game, chessboard, cell.point).stream()
                        .map { pointTo ->
                            Move.of(
                                cell.point, pointTo,
                                PAWN_TRANSFORMATION_PIECE_STUB
                            )
                        }
                }
                .map { move ->
                    val branchChessboard = chessboard.copyOf()
                    val branchGame = game.copyOf()

                    val killedPiece = branchChessboard.getPieceNullable(move.to)?.type
                    applyMoveHandler.applyMove(branchGame, branchChessboard, move)

                    MutableChessboardContext(branchGame, branchChessboard, move, killedPiece)
                }
                .toList()

            val threadsCount = THREADS_COUNT
            val threadPool = Executors.newFixedThreadPool(threadsCount)

            branches.forEach {
                threadPool.submit {
                    try {
                        it.fillNodes(CALCULATED_DEEP - 1)
                        it.updateWeight(CALCULATED_DEEP - 1)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }

            threadPool.shutdown()
            while (!threadPool.awaitTermination(1, TimeUnit.SECONDS)) {
                //do nothing, all ok!
            }

            val totalStatistic = Statistic()
            branches.forEach {
                totalStatistic.addValues(it.statistic)
            }

            branches.stream().max(Comparator.comparingInt(MutableChessboardContext::totalWeight))
                .ifPresent {
                    //println("BEST MOVE FOUND(totalWeight=${it.totalWeight}): ${it.rootNode.previousMove.toPrettyString()}")

                    val context = branches.stream().max(Comparator.comparingInt(MutableChessboardContext::totalWeight)).get()
                    val board = context.chessboard
                    var node: MutableChessboardContext.Node? = context.rootNode
                    val stringBuilder = StringBuilder()

                    while (node != null) {
                        board.actualize(node)
                        stringBuilder.append(node.toPrettyString(true))
                        stringBuilder.append("\r\n\r\n")

                        val children = node.children
                        node = if (children?.size == 1) children[0] else null
                    }

                    println("BEST MOVE FOUND(totalWeight=${it.totalWeight}, branchName:${it.toString()}):")
                    println(stringBuilder.toString())
                }

            totalStatistic.print(threadsCount)
            Move.cut(Point.of(1, 1))
        }
    }

    private inner class MutableChessboardContext(
        game: IGame,
        chessboard: IChessboard,
        previousMove: Move,
        killedPiece: PieceType?
    ) {
        val statistic: Statistic = Statistic()
        val chessboard = ChessboardStateHolder(game, chessboard)
        val rootNode: Node = Node(null, previousMove, killedPiece)
        var totalWeight = 0

        override fun toString(): String {
            return "context: ${rootNode.previousMove.toPrettyString()}"
        }

        fun fillNodes(deep: Int) {
            statistic.measure("totalTime") {
                rootNode.fillChildren(deep)
                chessboard.actualize(rootNode)
            }
        }

        fun clean() {
            rootNode.children = null
        }

        fun updateWeight(deep: Int) {
            totalWeight = rootNode.calculateDeepWeightAndFilterChildren(deep)
        }

        inner class Node(
            val parent: Node?,
            val previousMove: Move,
            val killedPiece: PieceType?
        ) {
            init {
                statistic.addCounterValue("totalNodesCount", 1)
            }

            var children: List<Node>? = null

            /**
             * Рассчитывается относительно текущей ноды.
             * То есть положительный рейтинг выгоден для стороны, которая передвинула фигуру в previousMove, а отрицательный - для ее противника
             */
            var weight: Int = 0

            val isRoot: Boolean get() = parent == null
            val currentPosition: Int get() = chessboard.initialPosition + getDeep()
            val nextTurnSide: Side get() = Side.nextTurnSide(currentPosition)


            fun calculateDeepWeightAndFilterChildren(reverseDeep: Int): Int {
                val childrenLocal = children

                if (reverseDeep == 0 || childrenLocal == null) {
                    //TODO: need support of the deep exchange
                    return weight
                }

                if (childrenLocal.isEmpty()) {
                    //TODO: CHECKMATE HERE!
                    //-нужно проверить всех соседей на checkmate
                    //возможно deep - это некий коэффициент на негарантированный checkmateValue
                    return weight
                }

                val maxChildrenNode: Node = childrenLocal.stream()
                    .max(Comparator.comparingInt { child -> child.calculateDeepWeightAndFilterChildren(reverseDeep - 1) })
                    .orElseThrow()

                children = listOf(maxChildrenNode)

                return weight - maxChildrenNode.weight
            }

            fun calculateWeight(): Int {
                //NOT ACTUALIZED CHESSBOARD HERE!
                val materialWeight = (killedPiece?.value ?: 0) * 100

                val childrenLocal = children
                val parentChildren = parent?.children

                if (childrenLocal == null || parentChildren == null) {
                    return materialWeight
                }

                val tacticalWeight = childrenLocal.size - parentChildren.size

//                return materialWeight + tacticalWeight
                return materialWeight
            }

            fun fillChildren(reverseDeep: Int) {
                weight = calculateWeight()

                if (reverseDeep == 0) {
                    statistic.measure("deepExchangeTime") {
                        fillDeepExchange()
                    }
                    return
                }

                actualizeChessboard()

                val children = chessboard.cellsStream(nextTurnSide)
                    .flatMap { cell -> getAvailableMovesByCell(cell) }
                    .map { move -> Node(this, move, chessboard.getPieceNullable(move.to)?.type) }
                    .toList()

                this.children = children
                children.forEach { it.fillChildren(reverseDeep - 1) }
            }

            private fun getAvailableMovesByCell(cell: Cell): Stream<Move> {
                return chessboard.getAvailableMoves(cell.point).stream()
                    .map { pointTo ->
                        Move.of(
                            cell.point,
                            pointTo,
                            PAWN_TRANSFORMATION_PIECE_STUB
                        )
                    }
            }

            private fun fillDeepExchange() {
                if (killedPiece == null) {
                    return
                }

                require(children == null) { "children must be null" }

                val targetPoint = previousMove.to
//                if (targetPoint != parent!!.previousMove.to) {
//                    // решил считать deepExchange только для продолжения уже начатых разменов во имя производительности
//                    //TODO: а что делать с обычными ходами? ведь нам важно знать текущий ход отдает фигуру или нет
//                    return
//                }

                actualizeChessboard()

                val attackers = statistic.measure("getTargetAttackersTime") {
                    movesProvider.getTargetAttackers(chessboard.game, chessboard, targetPoint, true)
                }

                if (attackers.isEmpty()) {
                    // текущий ход (previousMove) безопасен, потому что никто не может срубить, передвинутую фигуру
                    // разменов не будет
//                    deepWeight = (killedPiece?.value ?: 0).toUByte()
//                    weight = killedPiece?.value ?: 0
                    return
                }

                val filteredAttackers = statistic.measure("filterDeepAttackersTime") {
                    var filtered = Points.empty()

                    attackers.stream()
                        .filter { movesProvider.isMoveAvailable(chessboard.game, chessboard, it, targetPoint) }
                        .forEach { filtered = filtered.with(it) }
                    filtered
                }

                if (filteredAttackers.isEmpty()) {
                    // текущий ход (previousMove) безопасен, потому что никто не может срубить, передвинутую фигуру
                    // разменов не будет
//                    deepWeight = (killedPiece?.value ?: 0).toUByte()
//                    weight = killedPiece?.value ?: 0
                    return
                }

                val defenders = statistic.measure("getTargetDefendersTime") {
                    movesProvider.getTargetDefenders(chessboard.game, chessboard, targetPoint, true)
                }

                val movedPiece = chessboard.getPiece(targetPoint).type

                if (defenders.isEmpty()) {
                    // previousMove текущей ноды поставил под удар передвинутую фигуру. а защиты у нее нет
                    // противник ее может срубить любым из доступных способов (хотя бы 1 такой способ гарантированно есть)
//                    deepWeight = ((killedPiece?.value ?: 0) - movedPiece.value).toUByte()
//                    weight = (killedPiece?.value ?: 0) - movedPiece.value
                    return
                }

                //если мы сюда дошли, значит нас постиг deepExchange

                statistic.measure("calculateDeepExchangeTime") {
                    statistic.addCounterValue("deepAttackersCount", attackers.size.toLong())
                }
            }

            private fun actualizeChessboard() {
                statistic.measure("actualizeTime") {
                    chessboard.actualize(this)
                }
            }

            inline fun processNeighbors(handler: (Node) -> Unit) {
                if (parent == null) {
                    //root cannot has neighbors
                    return
                }
                for (child in parent.children!!) {
                    if (child != this) {
                        handler.invoke(child)
                    }
                }
            }

            fun getDeep(): Int {
                // deep = 0; is root! еще никто не ходил.
                return if (isRoot) {
                    0
                } else {
                    1 + parent!!.getDeep()
                }
            }

            fun toPrettyString(withTabs: Boolean = false): String {
                val movedPiece = chessboard.getPiece(previousMove.to)
                val str = StringBuilder()
                str.append("move[deep=${getDeep() + 1}, weight=$weight]: ${previousMove.toPrettyString(movedPiece)}\r\n")
                str.append("chessboard:\r\n${chessboard.toPrettyString(previousMove)}\r\n")
                str.append("---------------------------------------------------\r\n")

                if (withTabs) {
                    return str.toString().tabs(getDeep())
                }
                return str.toString()
            }
        }

        inner class ChessboardStateHolder(
            val game: IGame,
            val base: IChessboard,
            val initialPosition: Int = base.position
        ) : IChessboard by base {

            private val stackForRollback: Deque<Rollback> = ArrayDeque()

            fun getAvailableMoves(pointFrom: Point): List<Point> {
                return statistic.measure("getAvailableMovesTime") {
                    movesProvider.getAvailableMovesFrom(game, base, pointFrom)
                }
            }

            fun actualize(node: Node) {
                if (node.isRoot) {
                    rollbackToRoot()
                    return
                } else if (stackForRollback.isEmpty()) {
                    require(base.position == initialPosition) {
                        "incorrect chessboard position=${base.position}, must be equal initialPosition=$initialPosition, \r\n ${node.toPrettyString()}"
                    }
                    actualizeFromRoot(node)
                    return
                } else if (stackForRollback.first.node == node) {
                    //мы там где нужно, ничего актуализировать не требуется
                    return
                } else if (!tryActualizeByNeighbor(node)) {
                    rollbackToRoot()
                    actualizeFromRoot(node)
                }

                checkChessboardPositionEquals(node)
                checkNextStackNodeEquals(node)
            }

            private fun tryActualizeByNeighbor(node: Node): Boolean {
                if (base.position != node.currentPosition) {
                    return false
                }

                requireNotNull(node.parent) { "action does not support the root node" }

                val nextRollback = requireNotNull(stackForRollback.peekFirst()) {
                    "stack for rollback can't be empty"
                }

                node.processNeighbors { neighbor ->
                    if (neighbor === nextRollback.node) {
                        rollbackLast()!!                      // откатываемся к parent

                        if (stackForRollback.isEmpty()) {
                            require(node.parent.isRoot) { "stack is empty, but parent: ${node.parent} is not a root" }
                        } else {
                            checkNextStackNodeEquals(node.parent)
                        }

                        applyMove(node)
                        checkChessboardPositionEquals(node)
                        return true
                    }
                }

                return false
            }

            private fun checkNextStackNodeEquals(node: Node) {
                require(node === stackForRollback.first.node) {
                    "next rollback node: ${stackForRollback.first.node} must be equal actualized node: $node"
                }
            }

            private fun checkChessboardPositionEquals(node: Node) {
                require(base.position == node.currentPosition) {
                    "wrong rollback or chessboard state, chessboard.position=${base.position}, " +
                            "node.currentPosition=${node.currentPosition}"
                }
            }

            private fun rollbackToRoot() {
                while (!stackForRollback.isEmpty()) {
                    stackForRollback.removeFirst()
                        .execute()
                }
            }

            private fun rollbackLast(): Rollback? {
                val removed = stackForRollback.pollFirst()
                removed?.execute()
                return removed
            }

            private fun actualizeFromRoot(node: Node) {
                node.parent?.let {
                    actualizeFromRoot(it)
                    applyMove(node)
                }
            }

            private fun applyMove(node: Node) {
                val move = node.previousMove
                val initiatorSide = base.getPiece(move.from).side
                val fallenPiece = base.getPieceNullable(move.to)

                val prevCastlingState = game.getCastlingState(initiatorSide)
                val prevLongMoveColumnIndex = game.getPawnLongColumnIndex(initiatorSide)

                val additionalMove = applyMoveHandler.applyMove(game, base, move)

                val currentCastlingState = game.getCastlingState(initiatorSide)
                val currentLongMoveColumnIndex = game.getPawnLongColumnIndex(initiatorSide)

                val rollback = when {
                    currentCastlingState != prevCastlingState -> ChangeCastlingStateRollback(
                        node,
                        additionalMove,
                        fallenPiece,
                        prevCastlingState
                    )
                    prevLongMoveColumnIndex != currentLongMoveColumnIndex -> ChangeEnPassantStateRollback(
                        node,
                        additionalMove,
                        fallenPiece,
                        currentLongMoveColumnIndex
                    )
                    else -> Rollback(node, additionalMove, fallenPiece)
                }

                stackForRollback.addFirst(rollback)
            }

            private open inner class Rollback(
                val node: Node,
                protected val additionalMove: Move?,
                protected val fallenPiece: Piece?
            ) {
                protected val move: Move get() = node.previousMove

                open fun execute() {
                    base.rollbackMove(move, additionalMove, fallenPiece)
                }
            }

            private inner class ChangeCastlingStateRollback(
                node: Node,
                additionalMove: Move?,
                fallenPiece: Piece?,
                val prevCastlingState: Int
            ) : Rollback(node, additionalMove, fallenPiece) {

                override fun execute() {
                    val moveInitiatorSide = base.getPiece(move.to).side
                    game.setCastlingState(moveInitiatorSide, prevCastlingState)
                    super.execute()
                }
            }

            private inner class ChangeEnPassantStateRollback(
                node: Node,
                additionalMove: Move?,
                fallenPiece: Piece?,
                val prevLongMoveColumnIndex: Int?
            ) : Rollback(node, additionalMove, fallenPiece) {

                override fun execute() {
                    val moveInitiatorSide = base.getPiece(move.to).side
                    game.setPawnLongMoveColumnIndex(moveInitiatorSide, prevLongMoveColumnIndex)
                    super.execute()
                }
            }
        }
    }

    private fun <T> random(list: List<T>): T {
        val index = Random.nextInt(0, list.size)
        return list[index]
    }

    override fun select(game: IUnmodifiableGame, chessboard: IChessboard, botSide: Side): Move {
        selectBest(game, chessboard, botSide)

        //TODO: TMP block
        val random = selectRandom(chessboard, botSide, game)
        return Destiny.predictMove(game.position)
    }

    private fun selectRandom(
        chessboard: IChessboard,
        botSide: Side,
        game: IUnmodifiableGame
    ): Move {
        val pointsFrom = chessboard.cellsStream(botSide).toList()

        var randomPointFrom: Point
        var availableMoves: List<Point>
        do {
            randomPointFrom = random(pointsFrom).point
            availableMoves = movesProvider.getAvailableMovesFrom(game, chessboard, randomPointFrom)
        } while (availableMoves.isEmpty())

        return Move.of(
            randomPointFrom,
            random(availableMoves),
            PAWN_TRANSFORMATION_PIECE_STUB
        )
    }
}
