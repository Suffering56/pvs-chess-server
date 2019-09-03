package com.example.chess.server.web

import com.example.chess.server.logic.misc.Point
import com.example.chess.server.service.IBotService
import com.example.chess.server.service.IChessboardService
import com.example.chess.server.service.IGameService
import com.example.chess.shared.dto.ChessboardDTO
import com.example.chess.shared.dto.MoveDTO
import com.example.chess.shared.dto.PointDTO
import com.example.chess.shared.enums.GameMode
import com.example.chess.shared.enums.Side
import com.google.common.collect.Range
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*
import java.util.stream.Collectors

/**
 * @author v.peschaniy
 *      Date: 22.07.2019
 */
@RestController
@RequestMapping("/api/game")
class GameController @Autowired constructor(
    private val gameService: IGameService,
    private val chessboardService: IChessboardService,
    private val botService: IBotService
) {

    @GetMapping("/move")
    fun getAvailableMoves(
        @RequestParam userId: String, //TODO: можно ввести новую аннотацию @Authorized, которая будет проверять юзера (связка gameId + userId)
        @RequestParam gameId: Long,
        @RequestParam rowIndex: Int,
        @RequestParam columnIndex: Int
    ): Set<PointDTO> {

        return gameService.getMovesByPoint(gameId, Point.valueOf(rowIndex, columnIndex))
            .map(Point::toDTO)
            .collect(Collectors.toSet())
    }


    @GetMapping("/{gameId}/state/{position}")
    fun getChessboardByPosition(
        @PathVariable("gameId") gameId: Long,
        @PathVariable("position") position: Int
    ): ChessboardDTO {

        val game = gameService.findAndCheckGame(gameId)
        val availablePositionsRange = Range.closed(0, game.position)

        require(availablePositionsRange.contains(position)) { "position must be in range: $availablePositionsRange" }

        val result = chessboardService.createChessboardForGame(game, position)

        if (game.mode == GameMode.AI && game.getPlayerSide() == Side.BLACK) {//TODO: сомнительное условие
            botService.applyBotMove(game, null)
        }
        return result.toDTO()
    }

    @GetMapping("/{gameId}/listen")
    fun getActualChessboard(@PathVariable("gameId") gameId: Long): ChessboardDTO {

        val game = gameService.findAndCheckGame(gameId)
        return chessboardService.createChessboardForGame(game, game.position).toDTO()
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