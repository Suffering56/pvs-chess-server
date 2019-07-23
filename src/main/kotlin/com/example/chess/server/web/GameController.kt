package com.example.chess.server.web

import com.example.chess.server.enums.GameMode
import com.example.chess.server.objects.Point
import com.example.chess.server.service.IBotService
import com.example.chess.server.service.IGameService
import com.example.chess.shared.dto.ChessboardDTO
import com.example.chess.shared.dto.MoveDTO
import com.example.chess.shared.dto.PointDTO
import com.example.chess.shared.enums.Side
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*
import java.lang.UnsupportedOperationException
import java.util.stream.Collectors

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

    @GetMapping("/{gameId}/state/{position}")
    fun getPlaygroundByPosition(
        @PathVariable("gameId") gameId: Long,
        @PathVariable("position") position: Int
    ): ChessboardDTO {

        val game = gameService.findAndCheckGame(gameId)
        val result = gameService.createPlaygroundByGame(game, position)

        if (game.mode == GameMode.AI && game.getPlayerSide() == Side.BLACK) {
            botService.applyBotMove(game, null)
        }
        return result
    }

    @GetMapping("/{gameId}/listen")
    fun getActualPlayground(@PathVariable("gameId") gameId: Long): ChessboardDTO {

        val game = gameService.findAndCheckGame(gameId)
        return gameService.createPlaygroundByGame(game, game.position)
    }

    @GetMapping("/{gameId}/move")
    fun getAvailableMoves(
        @PathVariable("gameId") gameId: Long,
        @RequestParam rowIndex: Int,
        @RequestParam columnIndex: Int
    ): Set<PointDTO> {
        
        return gameService.getMovesByPoint(gameId, Point.valueOf(rowIndex, columnIndex))
            .map(Point::toDTO)
            .collect(Collectors.toSet())
    }

    @PostMapping("/{gameId}/move")
    fun applyMove(
        @PathVariable("gameId") gameId: Long,
        @RequestBody move: MoveDTO
    ): ChessboardDTO {

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