package com.example.chess.server.service.impl

import com.example.chess.server.logic.IChessboard
import com.example.chess.server.logic.IMove
import com.example.chess.server.logic.IPoint
import com.example.chess.server.logic.IUnmodifiableGame
import com.example.chess.server.logic.misc.Move
import com.example.chess.server.logic.misc.Point
import com.example.chess.server.service.IBotMoveSelector
import com.example.chess.server.service.IMovesProvider
import com.example.chess.shared.Constants
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

    fun selectBest(game: IUnmodifiableGame, chessboard: IChessboard, botSide: Side): IMove {
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
            null
        )
    }

    override fun selectFake(game: IUnmodifiableGame, chessboard: IChessboard, botSide: Side): IMove {
        val col = (game.position / 2)
        check(col < Constants.BOARD_SIZE) { "game.position too large for this fake implementation" }

        return if (botSide == Side.BLACK) {
            Move(
                Point.of(6, col),
                Point.of(5, col),
                null
            )
        } else {
            Move(
                Point.of(1, col),
                Point.of(2, col),
                null
            )
        }
    }


    internal class InternalContext(val chessboard: IChessboard) {

        val initialPosition: Int = chessboard.position
        val stackForRollback: Deque<RollbackData> = ArrayDeque()



        inner class Node(
            val id: Int,        // должен быть уникальным для всех нод
            val parent: Node?,
            val previousMove: IMove?
        ) {

            // deep = 0; is root! еще никто не ходил.
            val deep: Int get() = (parent?.deep ?: 0) + 1
            val isRoot: Boolean get() = parent == null
//
//            var additionalMove: IMove? = null
//            var fallenPiece: Piece? = null
            lateinit var children: Array<Node>

            fun fillChildren() {

            }

            private fun actualizeChessboard() {
                val rollbackData = stackForRollback.peekFirst()

                if (rollbackData == null) {
                    if (isRoot) {
                        return
                    } else {
                        actializeByInitial()
                    }
                }

//                if (isRoot && rollbackData) {
//                    return
//                }
//                stackForRollback.peekFirst()
            }

            fun actializeByInitial() {
                parent?.let {
                    actializeByInitial()
                }
                previousMove?.let {
                    chessboard.applyMove(it)
                }
            }
//
//            fun rollbackChessboard() {
//                previousMove?.let {
//                    chessboard.rollbackMove(it, additionalMove, fallenPiece)
//                }
//                parent?.let {
//                    rollbackChessboard()
//                }
//            }
        }

        inner class RollbackData(
            val move: IMove,
            val additionalMove: IMove?,
            val fallenPiece: Piece?,
            // уникальный id ноды, для которой выполняется условие:
            // rollbackData.move == node.previousMove и nextRollbackData.nodeId == node.parent.id и так далее пока стек не кончится
            // нужен для того чтобы не откатывать доску к первоначальному состоянию, если оно достигается ранее
            val nodeId: Int
        )
    }

}