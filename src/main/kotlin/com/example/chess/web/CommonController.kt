package com.example.chess.web

import com.example.chess.App
import com.example.chess.entity.GameRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api")
class CommonController @Autowired constructor(private val gameRepository: GameRepository) {

    @GetMapping("/version")
    fun get(): ResponseEntity<String> {
        val count = gameRepository.findAll().stream().count()
        return ResponseEntity.ok("version=${App.getVersion()}, count=$count")
    }
}