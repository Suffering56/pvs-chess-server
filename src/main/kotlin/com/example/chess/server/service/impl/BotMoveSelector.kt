package com.example.chess.server.service.impl

import com.example.chess.server.logic.IChessboard
import com.example.chess.server.logic.IGame
import com.example.chess.server.logic.IUnmodifiableChessboard
import com.example.chess.server.logic.IUnmodifiableGame
import com.example.chess.server.logic.misc.Move
import com.example.chess.server.logic.misc.Point
import com.example.chess.server.service.IBotMoveSelector
import com.example.chess.server.service.IMovesProvider
import com.example.chess.shared.enums.Piece
import com.example.chess.shared.enums.Side
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.random.Random
import kotlin.streams.toList

@Component
class BotMoveSelector : IBotMoveSelector {

    @Autowired private lateinit var movesProvider: IMovesProvider
    private var nodesCounter = AtomicInteger(0)

    companion object {
        val PAWN_TRANSFORMATION_PIECE_STUB: Piece? = null
    }

    fun selectBest(game: IUnmodifiableGame, chessboard: IUnmodifiableChessboard, botSide: Side): Move {
        val maxDeep = 5
        val threadPool = Executors.newFixedThreadPool(1)
        nodesCounter = AtomicInteger(0)

        measure {
            val branches = chessboard.cellsStream(botSide)
                .flatMap { cell ->
                    movesProvider.getAvailableMoves(game, chessboard, cell.point).stream()
                        .map { pointTo -> Move.of(cell.point, pointTo, PAWN_TRANSFORMATION_PIECE_STUB) }
                }
                .map { move ->
                    val branchChessboard = chessboard.copyOf()
                    val branchGame = game.copyOf()

                    branchChessboard.applyMove(move)
                    //TODO

                    MutableChessboardContext(branchGame, branchChessboard)
                }
                .toList()

            branches.forEach {
                it.fillNodes(maxDeep - 1)
                it.clean()
            }

            threadPool.shutdown()
            while (!threadPool.awaitTermination(1, TimeUnit.SECONDS)) {
            }
        }

        return Move.cut(Point.of(1, 1))
    }

    private inline fun measure(action: () -> Unit) {
        val start = System.currentTimeMillis()
        action.invoke()
        val estimated = System.currentTimeMillis() - start

        println()
        println("estimated(millis): $estimated")
        println("estimated(sec): ${estimated / 1000}")
        println("nodesCounter = ${nodesCounter.get()}")
    }

    private inner class MutableChessboardContext(
        game: IGame,
        chessboard: IChessboard
    ) {
        private val chessboard = ChessboardHolder(game, chessboard)
        private val rootNode: Node = Node(null, null)

        fun fillNodes(maxDeep: Int) {
            rootNode.fillChildren(maxDeep)
            chessboard.actualize(rootNode)
        }

        fun clean() {
            rootNode.children = null
        }

        inner class Node(
            val parent: Node?,
            val previousMove: Move?
        ) {
            init {
                nodesCounter.incrementAndGet()
            }

            var children: List<Node>? = null

            val isRoot: Boolean get() = parent == null
            val currentPosition: Int get() = chessboard.initialPosition + deep
            val nextTurnSide: Side get() = Side.nextTurnSide(currentPosition)
            val deep: Int   // deep = 0; is root! еще никто не ходил.
                get() = if (isRoot) {
                    0
                } else {
                    1 + parent!!.deep
                }

            fun fillChildren(deep: Int) {
                if (deep == 0) {
                    return
                }

                chessboard.actualize(this)

                val children = chessboard.cellsStream(nextTurnSide)
                    .flatMap { cell ->
                        chessboard.getAvailableMoves(cell.point).stream()
                            .map { pointTo -> Move.of(cell.point, pointTo, PAWN_TRANSFORMATION_PIECE_STUB) }
                    }
                    .map { move -> Node(this, move) }
                    .toList()

                this.children = children
                children.forEach { it.fillChildren(deep - 1) }
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

        private inner class ChessboardHolder(
            val game: IGame,
            val base: IChessboard,
            val initialPosition: Int = base.position
        ) : IChessboard by base {

            private val stackForRollback: Deque<Rollback> = ArrayDeque()

            fun getAvailableMoves(pointFrom: Point): Set<Point> {
                return movesProvider.getAvailableMoves(game, chessboard, pointFrom)
            }

            fun actualize(node: Node) {
                if (node.isRoot) {
                    rollbackToRoot()
                    return
                } else if (stackForRollback.isEmpty()) {
                    require(chessboard.position == initialPosition) {
                        "incorrect chessboard position=${chessboard.position}, must be equal initialPosition=$initialPosition"
                    }
                    actualizeFromRoot(node)
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
                node.previousMove?.let {
                    applyMove(node)
                }
            }

            private fun applyMove(node: Node) {
                val move = node.previousMove!!
                val initiatorSide = base.getPiece(move.from).side
                val fallenPiece = base.getPieceNullable(move.to)

                val prevCastlingState = game.getCastlingState(initiatorSide)
                val prevLongMoveColumnIndex = game.getPawnLongColumnIndex(initiatorSide)

                val additionalMove = base.applyMove(move)

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
        val pointsFrom = chessboard.cellsStream(botSide).toList()

        var randomPointFrom: Point
        var availableMoves: List<Point>
        do {
            randomPointFrom = random(pointsFrom).point
            availableMoves = movesProvider.getAvailableMoves(game, chessboard, randomPointFrom).toList()
        } while (availableMoves.isEmpty())

        return Move.of(
            randomPointFrom,
            random(availableMoves),
            PAWN_TRANSFORMATION_PIECE_STUB
        )
    }
}
