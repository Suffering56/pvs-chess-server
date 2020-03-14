package com.example.chess.server.web

import com.example.chess.server.core.Authorized
import com.example.chess.server.logic.misc.Move
import com.example.chess.server.logic.misc.Point
import com.example.chess.server.service.IGameService
import com.example.chess.shared.dto.ChangesDTO
import com.example.chess.shared.dto.ChessboardDTO
import com.example.chess.shared.dto.MoveDTO
import com.example.chess.shared.dto.PointDTO
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*
import java.util.stream.Collectors

/**
 * @author v.peschaniy
 *      Date: 22.07.2019
 */
@RestController
@RequestMapping("/api/game")
class GameController {

    @Autowired private lateinit var gameService: IGameService

    @Authorized
    @GetMapping("/moves")
    fun getAvailableMoves(
        @RequestParam gameId: Long,
        @RequestParam userId: String,
        @RequestParam clientPosition: Int,
        @RequestParam rowIndex: Int,
        @RequestParam columnIndex: Int
    ): Set<PointDTO> {      //TODO: List<PointDTO> ?
        return gameService.getMovesByPoint(gameId, Point.of(rowIndex, columnIndex), clientPosition)
            .result
            .stream()
            .map(Point::toDTO)
            .collect(Collectors.toSet())
    }

    @Authorized
    @PostMapping("/move")
    fun applyMove(
        @RequestParam gameId: Long,
        @RequestParam userId: String,
        @RequestParam clientPosition: Int,
        @RequestBody move: MoveDTO
    ): ChangesDTO {
        return gameService.applyPlayerMove(gameId, userId, Move.of(move), clientPosition)
            .result
    }

    @Authorized
    @PostMapping("/rollback")
    fun rollbackMoves(
        @RequestParam gameId: Long,
        @RequestParam userId: String,
        @RequestParam clientPosition: Int,
        @RequestParam positionsOffset: Int
    ): ChessboardDTO {
        return gameService.rollback(gameId, positionsOffset, clientPosition)
            .result
            .toDTO()
    }

    @Authorized
    @GetMapping("/listen")
    fun listenOpponentChanges(
        @RequestParam gameId: Long,
        @RequestParam userId: String,
        @RequestParam clientPosition: Int
    ): ChangesDTO {
        return gameService.listenChanges(gameId, userId, clientPosition)
            .result
    }

    @GetMapping("/chessboard")
    fun getChessboardByPosition(
        @RequestParam gameId: Long,
        @RequestParam userId: String,
        @RequestParam(required = false) position: Int?
    ): ChessboardDTO {
        return gameService.findAndCheckGame(gameId, userId, position)
            .result
            .toDTO()
    }
}