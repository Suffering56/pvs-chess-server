package com.example.chess.server.service.impl

import com.example.chess.server.logic.IChessboard
import com.example.chess.server.logic.IMove
import com.example.chess.server.logic.IPoint
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
import kotlin.random.Random
import kotlin.streams.toList

@Component
class BotMoveSelector : IBotMoveSelector {

    @Autowired private lateinit var movesProvider: IMovesProvider

    companion object {
        val PAWN_TRANSFORMATION_PIECE_STUB: Piece? = null
    }

    override fun selectBest(game: IUnmodifiableGame, chessboard: IChessboard, botSide: Side): IMove {
        val context = MutableChessboardContext(game, chessboard)
        context.fillNodes()

        return Move.cut(Point.of(1, 1))
    }

    private fun <T> random(list: List<T>): T {
        val index = Random.nextInt(0, list.size)
        return list[index]
    }

    override fun selectRandom(game: IUnmodifiableGame, chessboard: IChessboard, botSide: Side): IMove {
        val pointsFrom = chessboard.cellsStream(botSide).toList()

        var randomPointFrom: IPoint
        var availableMoves: List<IPoint>
        do {
            randomPointFrom = random(pointsFrom).point
            availableMoves = movesProvider.getAvailableMoves(game, chessboard, randomPointFrom).toList()
        } while (availableMoves.isEmpty())

        return Move(
            randomPointFrom,
            random(availableMoves),
            PAWN_TRANSFORMATION_PIECE_STUB
        )
    }


    // unsupported parallel handling!
    private inner class MutableChessboardContext(
        private val game: IUnmodifiableGame,
        originalChessboard: IChessboard
    ) {
        private val chessboard = ChessboardHolder(originalChessboard)
        private val initialPosition: Int = chessboard.position
        private val stackForRollback: Deque<Rollback> = ArrayDeque()
        private val rootNode: Node = Node(null, null)

        fun fillNodes() {
            val maxDeepInclusive = 2
            rootNode.fillChildren(maxDeepInclusive)
        }

        inner class Node(
            val parent: Node?,
            val previousMove: IMove?
        ) {
            lateinit var children: List<Node>

            val deep: Int get() = (parent?.deep ?: 0) + 1            // deep = 0; is root! еще никто не ходил.
            val currentPosition: Int get() = initialPosition + deep
            val isRoot: Boolean get() = parent == null
            val nextTurnSide: Side get() = Side.nextTurnSide(currentPosition)
            val hasChildren: Boolean get() = ::children.isInitialized

            fun fillChildren(deep: Int) {
                if (deep == 0) {
                    return
                }

                chessboard.actualize(this)

                children = chessboard.cellsStream(nextTurnSide)
                    .flatMap { cell ->
                        movesProvider.getAvailableMoves(game, chessboard, cell.point).stream()
                            .map { pointTo -> Move(cell.point, pointTo, PAWN_TRANSFORMATION_PIECE_STUB) }
                    }
                    .map { move -> Node(this, move) }
                    .toList()

                children.forEach { it.fillChildren(deep - 1) }
            }

            inline fun processNeighbors(handler: (Node) -> Unit) {
                if (parent == null) {
                    //root cannot has neighbors
                    return
                }
                for (child in parent.children) {
                    if (child != this) {
                        handler.invoke(child)
                    }
                }
            }

        }


        inner class ChessboardHolder(val base: IChessboard) : IChessboard by base {

            fun actualize(node: Node) {
                if (node.isRoot) {
                    rollbackToRoot()
                    return
                } else if (stackForRollback.isEmpty()) {
                    actualizeFromRoot(node)
                } else if (!tryActualizeByNeighbor(node)) {
                    rollbackToRoot()
                    actualizeFromRoot(node)
                }

                checkPosition(node)
                require(node.parent === stackForRollback.first.node) { TODO("TBD") }
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
                        require((node.parent.isRoot && stackForRollback.isEmpty()) || node.parent === stackForRollback.first.node) { TODO("TBD") }
                        applyMove(node)
                        checkPosition(node)
                        return true
                    }
                }

                return false
            }

            private fun checkPosition(node: Node) {
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
                    actualizeFromRoot(node)
                }
                node.previousMove?.let {
                    applyMove(node)
                }
            }

            private fun applyMove(node: Node) {
                val move = node.previousMove!!
                val fallenPiece = base.getPiece(move.from)
                val additionalMove = base.applyMove(move)

                stackForRollback.addFirst(
                    Rollback(node, additionalMove, fallenPiece)
                )
            }
        }

        inner class Rollback(
            val node: Node,
            private val additionalMove: IMove?,
            private val fallenPiece: Piece?
        ) {
            private val move: IMove get() = node.previousMove!!

            fun execute() {
                chessboard.rollbackMove(move, additionalMove, fallenPiece)
            }
        }
    }

}


//private fun checkPosition() {
//    require(chessboard.position == currentPosition) {
//        "wrong rollback or chessboard state, chessboard.position=${chessboard.position}, " +
//                "node.currentPosition=$currentPosition"
//    }
//}

//private fun fromParentToCurrent() {
//    val nextRollback = stackForRollback.peekFirst()
//
//    if (nextRollback == null) {
//        actualizeByRoot()
//        return
//    }
//
//    require(chessboard.position < currentPosition) { TODO("TBD") }
//    require(this !== nextRollback.node) { TODO("TBD") }
//
//    var node = this
//    while (node.parent != null && node.parent != nextRollback.node) {
//        node = node.parent!!
//    }
//
//    node.parent?.let {
//        applyMove(node.previousMove!!)
//    }
//    if (node != this) {
//        fromParentToCurrent()
//    }
//}


//private fun actualizeChessboard() {
//    if (stackForRollback.isEmpty()) {
//        actualizeByRoot()
//        return
//    }
//
//    if (isRoot) {
//        rollbackToRoot()
//        return
//    }
//    parent!!
//    previousMove!!
//
//    val nextRollback = stackForRollback.first
//    if (nextRollback.node === this) {
//        checkPosition()
//        return
//    }
//
//    when {
//        chessboard.position == currentPosition -> {
//            processNeighbors { neighbor ->
//                if (neighbor === nextRollback.node) {
//                    rollbackLast()!!                      // откатываемся к parent
//                    require((parent.isRoot && stackForRollback.isEmpty()) || parent === stackForRollback.first.node) { TODO("TBD") }
//                    applyMove(previousMove)
//                    checkPosition()
//                    return
//                }
//            }
//
//            fromParentToCurrent()
//        }
//        chessboard.position > currentPosition -> {
//            do {
//                rollbackLast()
//            } while (stackForRollback.peekFirst()?.node != this)
//
//
//            rollbackToRoot()
//        }
//        chessboard.position < currentPosition -> {
//        }
//
//    }
////                while (true) {
////                    val rollbackData = stackForRollback.peekFirst()
////                    if (rollbackData == null) {
////                        if (isRoot) {
////                            return
////                        } else {
////                            actializeByInitial()
////                        }
////                    }
////                }
//}