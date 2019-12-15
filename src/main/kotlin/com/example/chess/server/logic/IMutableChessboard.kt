package com.example.chess.server.logic

import com.example.chess.shared.api.IMove

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
     *
     * PS: может являеться частным случаем ConstructorMove:
     *      Устанавливает фигуру из movePieceFromPawn в move.to, игнорируя move.from
     *      Используется в режиме конструктора, когда воссоздать историю ходов невозможно (точнее не имеет смысла)
     *      return null
     */
    fun applyMove(move: IMove): IMove?
}