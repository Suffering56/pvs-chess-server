package com.example.chess.service

import com.example.chess.entity.Game
import com.example.chess.repository.GameRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.lang.RuntimeException

@Service
class InitService @Autowired constructor(private val gameRepository: GameRepository) {

    fun findAndCheckGame(gameId: Long): Game {
        return gameRepository.findById(gameId).orElseThrow { RuntimeException("Game with id=$gameId not found") }
    }
}