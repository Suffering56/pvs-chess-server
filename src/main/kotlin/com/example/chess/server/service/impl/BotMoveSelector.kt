package com.example.chess.server.service.impl

import com.example.chess.server.Destiny
import com.example.chess.server.logic.IChessboard
import com.example.chess.server.logic.IGame
import com.example.chess.server.logic.IUnmodifiableChessboard
import com.example.chess.server.logic.IUnmodifiableGame
import com.example.chess.server.logic.misc.Cell
import com.example.chess.server.logic.misc.Move
import com.example.chess.server.logic.misc.Point
import com.example.chess.server.service.IBotMoveSelector
import com.example.chess.server.service.IMovesProvider
import com.example.chess.shared.enums.Piece
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
import kotlin.collections.HashMap
import kotlin.random.Random
import kotlin.streams.toList

@Component
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
            countersMap["fillDeepChildrenTime"] = AtomicLong(0)
            countersMap["getAvailableMovesTime"] = AtomicLong(0)
            countersMap["getThreatsToTargetCountTime"] = AtomicLong(0)
            countersMap["getTargetDefendersCountTime"] = AtomicLong(0)

            countersMap["totalNodesCount"] = AtomicLong(0)
            countersMap["deepNodesCount"] = AtomicLong(0)
        }

        companion object {
            inline fun <T> measureAndPrint(title: String, action: () -> T): T {
                val start = System.currentTimeMillis()
                val result: T = action.invoke()
                val invokeTime = System.currentTimeMillis() - start
                println("$title(sec) = ${invokeTime / 1000.0}")
                return result
            }
        }

        fun addCounterValue(counterName: String, count: Long) {
            countersMap[counterName]!!.addAndGet(count)
        }

        inline fun <T> measure(counterName: String, action: () -> T): T {
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

        Statistic.measureAndPrint("globalInvokeTime") {
            val branches = chessboard.cellsStream(botSide)
                .flatMap { cell ->
                    movesProvider.getAvailableMoves(game, chessboard, cell.point).stream()
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

                    applyMoveHandler.applyMove(branchGame, branchChessboard, move)

                    MutableChessboardContext(branchGame, branchChessboard)
                }
                .toList()

            val threadsCount = THREADS_COUNT
            val threadPool = Executors.newFixedThreadPool(threadsCount)

            branches.forEach {
                threadPool.submit {
                    it.fillNodes(CALCULATED_DEEP - 1)
                    it.clean()
                }
            }

            threadPool.shutdown()
            while (!threadPool.awaitTermination(1, TimeUnit.SECONDS)) {
            }

            val totalStatistic = Statistic()
            branches.forEach {
                totalStatistic.addValues(it.statistic)
            }
            totalStatistic.print(threadsCount)
        }

        return Move.cut(Point.of(1, 1))
    }

    private inner class MutableChessboardContext(
        game: IGame,
        chessboard: IChessboard
    ) {
        val statistic: Statistic = Statistic()

        private val chessboard = ChessboardStateHolder(game, chessboard)
        private val rootNode: Node = Node(null, null)

        fun fillNodes(deep: Int) {
            statistic.measure("totalTime") {
                rootNode.fillChildren(deep)
                chessboard.actualize(rootNode)
            }
        }

        fun clean() {
            rootNode.children = null
        }

        inner class Node(
            val parent: Node?,
            val previousMove: Move?
        ) {
            init {
                statistic.addCounterValue("totalNodesCount", 1)
            }

            var children: List<Node>? = null
            var weight: Byte = 0    //100 - checkmate

            val isRoot: Boolean get() = parent == null || previousMove == null
            val currentPosition: Int get() = chessboard.initialPosition + getDeep()
            val nextTurnSide: Side get() = Side.nextTurnSide(currentPosition)

            fun getDeep(): Int {
                // deep = 0; is root! еще никто не ходил.
                return if (isRoot) {
                    0
                } else {
                    1 + parent!!.getDeep()
                }
            }

            fun fillChildren(reverseDeep: Int) {
                if (reverseDeep == 0) {
                    statistic.measure("deepExchangeTime") {
                        fillDeepExchange()
                    }
                    return
                }

                actualizeChessboard()

                val children = chessboard.cellsStream(nextTurnSide)
                    .flatMap { cell -> getAvailableMovesByCell(cell) }
                    .map { move -> Node(this, move) }
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
                requireNotNull(previousMove) { "operation not supported for root node" }
                require(children == null) { "children must be null" }

                actualizeChessboard()

                val targetPoint = previousMove.to

                val threatsToTargetCount = statistic.measure("getThreatsToTargetCountTime") {
                    movesProvider.getThreatsToTargetCount(chessboard.game, chessboard, targetPoint)
                }

                if (threatsToTargetCount == 0) {
                    return
                }

                val targetDefendersCount = statistic.measure("getTargetDefendersCountTime") {
                    movesProvider.getTargetDefendersCount(chessboard.game, chessboard, targetPoint)
                }


                if (targetDefendersCount == 0) {
                    val targetWeight = chessboard.getPiece(targetPoint).type.value
                    weight = targetWeight.toByte()
                    return
                }

//                if (threatsToTargetCount == 1) {
//                    weight
//                }


                val children = statistic.measure("fillDeepChildrenTime") {
                    movesProvider.getThreatsToTarget(chessboard.game, chessboard, targetPoint)
                        .stream()
                        .map { Move.of(it, targetPoint, PAWN_TRANSFORMATION_PIECE_STUB) }
                        .map { move -> Node(this, move) }
                        .toList()
                }

                statistic.addCounterValue("deepNodesCount", children.size.toLong())

                this.children = children
                children.forEach { it.fillDeepExchange() }
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
        }

        private inner class ChessboardStateHolder(
            val game: IGame,
            val base: IChessboard,
            val initialPosition: Int = base.position
        ) : IChessboard by base {

            private val stackForRollback: Deque<Rollback> = ArrayDeque()

            fun getAvailableMoves(pointFrom: Point): List<Point> {
                return statistic.measure("getAvailableMovesTime") {
                    movesProvider.getAvailableMoves(game, base, pointFrom)
                }
            }

            fun actualize(node: Node) {
                if (node.isRoot) {
                    rollbackToRoot()
                    return
                } else if (stackForRollback.isEmpty()) {
                    require(base.position == initialPosition) {
                        "incorrect chessboard position=${base.position}, must be equal initialPosition=$initialPosition, \r\n ${chessboard.toPrettyString()}"
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
                node.previousMove!!

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
                }
                node.previousMove?.let { _ ->
                    applyMove(node)
                }
            }

            private fun applyMove(node: Node) {
                val move = node.previousMove!!
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
                protected val move: Move get() = node.previousMove!!

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
            availableMoves = movesProvider.getAvailableMoves(game, chessboard, randomPointFrom)
        } while (availableMoves.isEmpty())

        return Move.of(
            randomPointFrom,
            random(availableMoves),
            PAWN_TRANSFORMATION_PIECE_STUB
        )
    }
}
