package com.example.chess.server

import com.example.chess.server.logic.misc.Move

/**
 * @author v.peschaniy
 *      Date: 28.01.2020
 */

class Destiny {

    companion object {

        private val movesMap = mapOf(
            Pair(0, Move.of("e2-e4")),  //white: pawn
            Pair(1, Move.of("e7-e5")),  //black: pawn
            Pair(2, Move.of("d2-d4")),  //white: pawn
            Pair(3, Move.of("d7-d5")),  //black: pawn
            Pair(4, Move.of("d1-h5")),  //white: queen
            Pair(5, Move.of("c7-c6")),  //black: pawn
            Pair(6, Move.of("g1-f3")),  //white: knight
            Pair(7, Move.of("g8-h6")),  //black: knight
            Pair(8, Move.of("b2-b3")),  //white: pawn
            Pair(9, Move.of("b7-b5")),  //black: pawn
            Pair(10, Move.of("b1-c3")), //white: knight
            Pair(11, Move.of("b8-a6"))  //black: knight
        )

        fun predictMove(position: Int): Move {
            return movesMap.getOrElse(position) {
                throw IllegalArgumentException("wrong position: $position for predict")
            }
        }
    }
}