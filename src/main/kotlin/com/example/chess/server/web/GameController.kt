package com.example.chess.server.web

import com.example.chess.server.enums.GameMode
import com.example.chess.server.logic.ExtendedMove
import com.example.chess.server.service.IBotService
import com.example.chess.server.service.IGameService
import com.example.chess.shared.Move
import com.example.chess.shared.Playground
import com.example.chess.shared.Point
import com.example.chess.shared.enums.Side
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*
import java.lang.UnsupportedOperationException

/**
 * @author v.peschaniy
 *      Date: 22.07.2019
 */
@RestController
@RequestMapping("/api/game")
class GameController @Autowired constructor(
    private val gameService: IGameService,
    private val botService: IBotService
) {

    @GetMapping("/{gameId}/playground/{position}")
    fun getPlaygroundByPosition(
        @PathVariable("gameId") gameId: Long,
        @PathVariable("position") position: Int
    ): Playground {

        val game = gameService.findAndCheckGame(gameId)
        val result = gameService.createPlaygroundByGame(game, position)

        if (game.mode == GameMode.AI && game.getPlayerSide() == Side.BLACK) {
            botService.applyBotMove(game, null)
        }
        return result
    }

    @GetMapping("/{gameId}/listen")
    fun getActualPlayground(@PathVariable("gameId") gameId: Long): Playground {

        val game = gameService.findAndCheckGame(gameId)
        return gameService.createPlaygroundByGame(game, game.position)
    }

    @GetMapping("/{gameId}/move")
    fun getAvailableMoves(
        @PathVariable("gameId") gameId: Long,
        @RequestParam rowIndex: Int,
        @RequestParam columnIndex: Int
    ): Set<Point> {
        return gameService.getMovesByPoint(gameId, Point.valueOf(rowIndex, columnIndex))
    }

    @PostMapping("/{gameId}/move")
    fun applyMove(
        @PathVariable("gameId") gameId: Long,
        @RequestBody move: Move
    ): Playground {

        val game = gameService.findAndCheckGame(gameId)
        val pair = gameService.applyMove(game, move)

        if (game.mode == GameMode.AI) {
//            botService.applyBotMove(game, move.toExtendedMove(pair.getKey()))
            botService.applyBotMove(game, null)
        }
//                return pair.getValue()
        throw UnsupportedOperationException()
    }


}