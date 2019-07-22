package com.example.chess.server.service.impl

import com.example.chess.server.entity.Game
import com.example.chess.server.repository.GameRepository
import com.example.chess.server.service.IGameService
import com.example.chess.shared.Move
import com.example.chess.shared.Playground
import com.example.chess.shared.Point
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.lang.RuntimeException

/**
 * @author v.peschaniy
 *      Date: 22.07.2019
 */
@Service
class GameService @Autowired constructor(
    private val gameRepository: GameRepository

) : IGameService {

    override fun findAndCheckGame(gameId: Long): Game {
        return gameRepository.findById(gameId).orElseThrow { RuntimeException("Game with id=$gameId not found") }
    }

    override fun createPlaygroundByGame(game: Game, position: Int): Playground {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getMovesByPoint(gameId: Long, point: Point): Set<Point> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun applyMove(game: Game, move: Move): Any {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }


}