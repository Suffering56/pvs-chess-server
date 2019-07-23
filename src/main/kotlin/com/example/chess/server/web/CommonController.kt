package com.example.chess.server.web

import com.example.chess.server.App
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * @author v.peschaniy
 *      Date: 22.07.2019
 */
@RestController
@RequestMapping("/api")
class CommonController {

    @GetMapping("/version")
    fun get() = "version=${App.getVersion()}"
}