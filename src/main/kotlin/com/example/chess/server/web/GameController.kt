package com.example.chess.server.web

import com.example.chess.server.core.Authorized
import com.example.chess.server.core.InjectGame
import com.example.chess.server.core.InjectUserId
import com.example.chess.server.entity.Game
import com.example.chess.server.entity.History
import com.example.chess.server.entity.provider.EntityProvider
import com.example.chess.server.logic.misc.Move
import com.example.chess.server.logic.misc.Point
import com.example.chess.server.repository.HistoryRepository
import com.example.chess.server.service.IBotService
import com.example.chess.server.service.IChessboardProvider
import com.example.chess.server.service.IGameService
import com.example.chess.server.service.impl.MovesProvider
import com.example.chess.shared.Constants.INITIAL_PIECES_COUNT
import com.example.chess.shared.api.IPoint
import com.example.chess.shared.dto.ChangesDTO
import com.example.chess.shared.dto.ChessboardDTO
import com.example.chess.shared.dto.MoveDTO
import com.example.chess.shared.dto.PointDTO
import com.example.chess.shared.enums.GameMode
import com.example.chess.shared.enums.Side
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*
import java.util.*
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
    private val botService: IBotService,
    private val entityProvider: EntityProvider,
    private val historyRepository: HistoryRepository,
    private val movesProvider: MovesProvider
) {

    @Authorized
    @GetMapping("/moves")
    fun getAvailableMoves(
        @InjectGame game: Game,
        @RequestParam rowIndex: Int,
        @RequestParam columnIndex: Int
    ): Set<PointDTO> {
        val chessboard = chessboardProvider.createChessboardForGame(game)

        check(Side.ofPosition(game.position) == chessboard.getPiece(rowIndex, columnIndex).side) {
            "incorrect side of pointFrom. expected:${Side.ofPosition(game.position)}, " +
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

    @Authorized
    @PostMapping("/rollback")
    fun rollbackMoves(
        @InjectGame game: Game,
        @RequestParam("positionsOffset") positionsOffset: Int
    ): ChessboardDTO {
        require(positionsOffset > 0) { "position offset must be positive" }

        val rollbackGame = gameService.rollback(game, positionsOffset)
        return chessboardProvider.createChessboardForGame(rollbackGame).toDTO()
    }

    @Authorized
    @PostMapping("/constructor/continue")
    fun continueConstructedGame(
        @InjectGame game: Game,
        @InjectUserId userId: String,
        @RequestBody arrangement: ChessboardDTO
    ): ChangesDTO {
        //TODO: этому коду здесь не место:
        //  - либо перетащить в сервисы
        //  - либо дождаться рефакторинга, где я все это перепишу
        val userSide = game.getUserSide(userId).get()

        val history: MutableList<History> = mutableListOf()
        var position = INITIAL_PIECES_COUNT

        val piecesCount = Arrays.stream(arrangement.matrix)
            .flatMap { Arrays.stream(it) }
            .map { it.piece != null }
            .count()
            .toInt()

        if (userSide != Side.ofPosition(INITIAL_PIECES_COUNT + piecesCount)) {
            val skipTurnHistoryItem = entityProvider.createConstructorHistoryItem(
                game.id!!,
                ++position,
                Point.of(0, 0),
                null
            )
            history.add(skipTurnHistoryItem)
        }

        Arrays.stream(arrangement.matrix)
            .flatMap { Arrays.stream(it) }
            .forEach {
                val historyItem = entityProvider.createConstructorHistoryItem(
                    game.id!!,
                    ++position,
                    Point.of(it.pointDTO),
                    it.piece
                )
                history.add(historyItem)
            }

        val updatedHistory = historyRepository.saveAll(history)
        game.position = position
        val updatedGame = gameService.saveGame(game)

        val chessboard = chessboardProvider.createChessboardForGame(updatedGame)
        //TODO: validate chessboard
        val underCheck = movesProvider.isUnderCheck(userSide.reverse(), chessboard)
        val lastMove = updatedHistory.maxBy { it.position }?.toMove()?.toDTO()

        return ChangesDTO(
            position,
            MoveDTO(PointDTO(0, 0), PointDTO(0, 1), null),
            lastMove,
            if (!underCheck) null else chessboard.getKingPoint(userSide.reverse()).toDTO()
        )
    }
}