package com.example.chess.server.web

import com.example.chess.server.App
import com.example.chess.server.repository.GameRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * @author v.peschaniy
 *      Date: 22.07.2019
 */
@RestController
@RequestMapping("/api")
class CommonController @Autowired constructor(private val gameRepository: GameRepository) {

    @GetMapping("/version")
    fun get(): String {
        val count = gameRepository.findAll().stream().count()
        return "version=${App.getVersion()}, count=$count"
    }
}