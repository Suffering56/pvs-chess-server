package com.example.chess.server.logic

import com.example.chess.shared.enums.Piece

/**
 * @author v.peschaniy
 *      Date: 24.07.2019
 */
interface IMutableChessboard : IChessboard {

    /**
     * Выполняет ход, перемещая фигуру из move.from в move.to
     * Так же выполняет дополнительные операции с доской в случае если ход является рокировкой или взятием на проходе
     *
     * return ход, имитирующий дополнительную операцию, описанную выше, или null - если ход был простым
     */
    fun applyMove(move: IMove): IMove?

    /**
     * Откатывает выполненный ход, перемещая фигуру из move.to в move.from
     * Так же выполняет дополнительные операции с доской в случае если move являлся рокировкой или взятием на проходе (additionalMove != null)
     *
     * @param fallenPiece - фигура, которая была срублена в результате выполнения move, или null - если никто не был срублен
     * @param additionalMove - ход, имитирующий дополнительную операцию для рокировки и взятия на проходе
     */
    fun rollbackMove(move: IMove, additionalMove: IMove?, fallenPiece: Piece?)
}