package com.example.chess.server.logic

import com.example.chess.server.logic.misc.Move
import com.example.chess.shared.enums.Piece

/**
 * @author v.peschaniy
 *      Date: 24.07.2019
 */
interface IChessboard : IUnmodifiableChessboard {

    /**
     * Выполняет ход, перемещая фигуру из move.from в move.to
     * Так же выполняет дополнительные операции с доской в случае если ход является рокировкой или взятием на проходе
     *
     * return ход, имитирующий дополнительную операцию, описанную выше, или null - если ход был простым
     */
    fun applyMove(move: Move): Move?

    /**
     * Откатывает выполненный ход, перемещая фигуру из move.to в move.from
     * Так же выполняет дополнительные операции с доской в случае если move являлся рокировкой или взятием на проходе (additionalMove != null)
     *
     * @param fallenPiece - фигура, которая была срублена в результате выполнения move, или null - если никто не был срублен
     * @param additionalMove - ход, имитирующий дополнительную операцию для рокировки и взятия на проходе
     */
    fun rollbackMove(move: Move, additionalMove: Move?, fallenPiece: Piece?)
}