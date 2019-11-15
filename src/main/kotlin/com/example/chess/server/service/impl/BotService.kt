package com.example.chess.server.service.impl

import com.example.chess.server.entity.Game
import com.example.chess.server.service.IBotService
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentSkipListSet
/**
 * @author v.peschaniy
 *      Date: 22.07.2019
 */
@Service
class BotService : IBotService {

    var processingGameIds: MutableSet<Long> = ConcurrentSkipListSet()

    override fun fireBotMove(game: Game, nothing: Any?) {
        if (processingGameIds.add(game.id!!)) {
            try {
                //if bot turn -> process apply bot move
            } finally {
                processingGameIds.remove(game.id)
            }
        }
    }
}