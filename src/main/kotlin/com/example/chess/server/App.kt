package com.example.chess.server

import com.example.chess.server.logic.misc.Point
import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication

/**
 * @author v.peschaniy
 *      Date: 22.07.2019
 */
@SpringBootApplication
class App : CommandLineRunner {

    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        const val DEBUG_ENABLED: Boolean = true
        fun getVersion() = "1.0.4"
    }

    override fun run(vararg args: String?) {
        log.info("APP_VERSION = ${getVersion()}")
    }
}

fun main(args: Array<String>) {
    SpringApplication.run(App::class.java, *args)
}

typealias PointsCollector = (Point) -> Unit

