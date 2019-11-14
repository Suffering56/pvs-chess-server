package com.example.chess.server.service.impl

import com.example.chess.server.entity.Game
import com.example.chess.server.entity.provider.EntityProvider
import com.example.chess.server.logic.IMutableChessboard
import com.example.chess.server.logic.misc.Point
import com.example.chess.server.repository.GameRepository
import com.example.chess.server.repository.HistoryRepository
import com.example.chess.server.service.IGameService
import com.example.chess.shared.api.IMove
import com.example.chess.shared.dto.ChangesDTO
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.stream.Stream

/**
 * @author v.peschaniy
 *      Date: 22.07.2019
 */
@Service
class GameService @Autowired constructor(
    private val gameRepository: GameRepository,
    private val historyRepository: HistoryRepository,
    private val entityProvider: EntityProvider
) : IGameService {

    override fun createNewGame(): Game {
        return gameRepository.save(entityProvider.createNewGame())
    }

    override fun saveGame(game: Game): Game {
        return gameRepository.save(game)
    }

    override fun findAndCheckGame(gameId: Long): Game {
        return gameRepository.findById(gameId)
            .orElseThrow { IllegalArgumentException("Game with id=$gameId not found") }
    }

    override fun getMovesByPoint(game: Game, point: Point): Stream<Point> {
//        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.

        return setOf(
            Point.of(0, 0),
            Point.of(4, 3),
            Point.of(4, 4),
            Point.of(4, 5),
            Point.of(4, 6),
            Point.of(5, 2)
        ).stream()
    }

    override fun applyMove(game: Game, chessboard: IMutableChessboard, move: IMove): ChangesDTO {
        //TODO: longPawnMoveIndex надо обрабатывать где-то здесь (как и то что availableMoves.contains(move)

        val piece = chessboard.getPiece(move.from)

        chessboard.applyMove(move)
        game.position = chessboard.position
        //TODO: game.disableShortCastling etc
        val historyItem = entityProvider.createHistoryItem(game.id!!, chessboard.position, move, piece)

        gameRepository.save(game)
        historyRepository.save(historyItem)

        return ChangesDTO(
            chessboard.position,
            move.toDTO(),
            null    //TODO: stub
        )
    }
}