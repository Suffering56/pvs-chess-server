package com.example.chess.server.web

import com.example.chess.server.core.Authorized
import com.example.chess.server.core.InjectGame
import com.example.chess.server.core.InjectUserId
import com.example.chess.server.entity.Game
import com.example.chess.server.logic.misc.Point
import com.example.chess.server.service.IBotService
import com.example.chess.server.service.IChessboardService
import com.example.chess.server.service.IGameService
import com.example.chess.shared.dto.ChangesDTO
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

    @Authorized
    @GetMapping("/move")
    fun getAvailableMoves(
        @InjectGame game: Game,
        @RequestParam rowIndex: Int,
        @RequestParam columnIndex: Int
    ): Set<PointDTO> {

        return gameService.getMovesByPoint(game, Point.valueOf(rowIndex, columnIndex))
            .map(Point::toDTO)
            .collect(Collectors.toSet())
    }

    @Authorized
    @PostMapping("/move")
    fun applyMove(
        @InjectGame game: Game,
        @RequestBody move: MoveDTO
    ): ChangesDTO {
//        val pair = gameService.applyMove(game, move)
//
//        if (game.mode == GameMode.AI) {
//            botService.fireBotMove(game, move.toExtendedMove(pair.getKey()))
//            botService.fireBotMove(game, null)
//        }
//                return pair.getValue()
//        throw UnsupportedOperationException()

        return ChangesDTO(game.position + 1, move, PointDTO(7, 3))
    }

    @Authorized(viewerMode = true)
    @GetMapping("/chessboard")
    fun getChessboardByPosition(
        @InjectGame game: Game,
        @InjectUserId userId: String,
        @RequestParam(required = false) position: Int?
    ): ChessboardDTO {

        position?.also {
            val availablePositionsRange = Range.closed(0, game.position)
            require(availablePositionsRange.contains(position)) { "position must be in range: $availablePositionsRange" }
        }

        val result = chessboardService.createChessboardForGame(game, position ?: game.position)
        val isViewer = !game.isUserRegistered(userId)

        if (!isViewer
            && game.position == 0
            && game.mode == GameMode.AI
            && game.getPlayerSide() == Side.BLACK
        ) {
            //еще никто не ходил, а игрок(человек) играет за черных -> нужно пнуть бота, чтобы тот походил
            botService.fireBotMove(game, null)
        }
        return result.toDTO()
    }
}