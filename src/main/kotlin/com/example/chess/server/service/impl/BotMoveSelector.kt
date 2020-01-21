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
import kotlin.random.Random
import kotlin.streams.toList

@Component
class BotMoveSelector : IBotMoveSelector {

    @Autowired private lateinit var movesProvider: IMovesProvider

    // deep = 0; is root! еще никто не ходил.

    internal class InternalContext(
        val chessboard: IChessboard,
        val initialPosition: Int = chessboard.position
    ) {

        inner class Node(
            val parent: Node?,
            val previousMove: IMove?
        ) {
            val deep: Int get() = (parent?.deep ?: 0) + 1

            var additionalMove: IMove? = null
            var fallenPiece: Piece? = null
            lateinit var children: Array<Node>

            fun fillChildren() {

            }

            fun actualizeChessboard() {
                //TODO: if position

                parent?.let {
                    actualizeChessboard()
                }
                previousMove?.let {
                    fallenPiece = chessboard.getPiece(it.to)
                    additionalMove = chessboard.applyMove(it)
                }
            }

            fun rollbackChessboard() {
                previousMove?.let {
                    chessboard.rollbackMove(it, additionalMove, fallenPiece)
                }
                parent?.let {
                    rollbackChessboard()
                }
            }
        }
    }

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
}