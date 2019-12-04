package com.example.chess.server.web

import com.example.chess.server.core.Authorized
import com.example.chess.server.core.InjectGame
import com.example.chess.server.core.InjectUserId
import com.example.chess.server.entity.Game
import com.example.chess.server.logic.misc.Move
import com.example.chess.server.logic.misc.Point
import com.example.chess.server.service.IBotService
import com.example.chess.server.service.IChessboardProvider
import com.example.chess.server.service.IGameService
import com.example.chess.shared.api.IPoint
import com.example.chess.shared.dto.ChangesDTO
import com.example.chess.shared.dto.ChessboardDTO
import com.example.chess.shared.dto.MoveDTO
import com.example.chess.shared.dto.PointDTO
import com.example.chess.shared.enums.GameMode
import com.example.chess.shared.enums.Side
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
    private val chessboardProvider: IChessboardProvider,
    private val botService: IBotService
) {

    @Authorized
    @GetMapping("/move")
    fun getAvailableMoves(
        @InjectGame game: Game,
        @RequestParam rowIndex: Int,
        @RequestParam columnIndex: Int
    ): Set<PointDTO> {
        val chessboard = chessboardProvider.createChessboardForGame(game)

        check(game.getNextTurnSideByPosition() == chessboard.getPiece(rowIndex, columnIndex).side) {
            "incorrect side of pointFrom. expected:${game.getNextTurnSideByPosition()}, " +
                    "found: ${chessboard.getPiece(rowIndex, columnIndex).side}"
        }

        return gameService.getMovesByPoint(game, chessboard, Point.of(rowIndex, columnIndex))
            .stream()
            .map(IPoint::toDTO)
            .collect(Collectors.toSet())
    }

    @Authorized
    @PostMapping("/move")
    fun applyMove(
        @InjectGame game: Game,
        @RequestBody move: MoveDTO
    ): ChangesDTO {
        val chessboard = chessboardProvider.createChessboardForGame(game)
        val changes = gameService.applyMove(game, chessboard, Move.of(move))

        if (game.mode == GameMode.AI) {
            botService.fireBotMove(game, chessboard)
        }
        return changes
    }

    @Authorized(false)
    @GetMapping("/chessboard")
    fun getChessboardByPosition(
        @InjectGame game: Game,
        @InjectUserId userId: String,
        @RequestParam(required = false) position: Int?
    ): ChessboardDTO {
        val chessboard = chessboardProvider.createChessboardForGame(game, position ?: game.position)

        if (game.position == 0
            && game.mode == GameMode.AI
            && game.getUserSide(userId).orElseGet(null) == Side.BLACK   //если null - значит это зритель -> а зритель не должен триггерить бота
        ) {
            //еще никто не ходил, а игрок(человек) играет за черных -> нужно пнуть бота, чтобы тот походил
            botService.fireBotMove(game, null)
        }
        return chessboard.toDTO()
    }
}