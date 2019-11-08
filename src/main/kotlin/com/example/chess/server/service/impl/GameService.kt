package com.example.chess.server.service.impl

import com.example.chess.server.entity.Game
import com.example.chess.server.entity.provider.EntityProvider
import com.example.chess.server.logic.misc.Point
import com.example.chess.server.repository.GameRepository
import com.example.chess.server.service.IGameService
import com.example.chess.shared.dto.MoveDTO
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
    private val entityProvider: EntityProvider
) : IGameService {

    override fun createNewGame(): Game {
        return gameRepository.save(entityProvider.createNewGame())
    }

    override fun saveGame(game: Game): Game {
        return gameRepository.save(game)
    }

    override fun findAndCheckGame(gameId: Long): Game {
        return gameRepository.findById(gameId).orElseThrow { RuntimeException("Game with id=$gameId not found") }
    }

    override fun getMovesByPoint(game: Game, point: Point): Stream<Point> {
//        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.

        return setOf(
            Point.valueOf(0, 0),
            Point.valueOf(4, 3),
            Point.valueOf(4, 4),
            Point.valueOf(4, 5),
            Point.valueOf(4, 6),
            Point.valueOf(5, 2)
        ).stream()
    }

    override fun applyMove(game: Game, move: MoveDTO): Any {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }


}