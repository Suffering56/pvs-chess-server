package com.example.chess

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api")
class CommonController {

    @GetMapping("/version")
    fun get(): ResponseEntity<String> {
        return ResponseEntity.ok(App.getVersion())
    }
}