package com.example.chess.server.web

import com.example.chess.server.App
import com.example.chess.server.logic.Chessboard
import com.example.chess.server.service.impl.GameService
import com.example.chess.shared.dto.ChessboardDTO
import com.example.chess.shared.dto.GameDTO
import com.example.chess.shared.dto.PointDTO
import com.example.chess.shared.enums.Side
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import javax.servlet.http.HttpServletRequest

/**
 * @author v.peschaniy
 *      Date: 22.07.2019
 */
@RestController
@RequestMapping("/api/debug")
class DebugController @Autowired constructor(
    private val gameService: GameService
) {

    @GetMapping("/version")
    fun get() = "version=${App.getVersion()}"

    /**
     * да-да... меняем данные в GET запросе... здесь можно - это debug!!!
     * через GET удобнее - потому что это дает возможность перерегистрировать игрока через браузер
     */
    @GetMapping("/{gameId}/register/{side}")
    fun registerPlayerForced(
        @PathVariable("gameId") gameId: Long,
        @PathVariable("side") side: Side,
        request: HttpServletRequest                 //TODO: create new annotation: @SessionId
    ): GameDTO {

        val game = gameService.findAndCheckGame(gameId)

        //проставляем sessionId принудительно, чтобы продолжить игру после рестарта сервера (когда session.id клиента поменяется)
        game.setSessionId(side, request.session.id)
        return gameService.saveGame(game).toDTO()
    }

    @GetMapping("/chessboard")
    fun getInitialChessboard(): ChessboardDTO {
        return Chessboard.byInitial().toDTO()
    }

    @GetMapping("/point")
    fun getPoint(): PointDTO {
        return PointDTO(3, 4)
    }
}