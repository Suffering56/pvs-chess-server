package com.example.chess

import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication

@SpringBootApplication
open class App : CommandLineRunner {

    private val log = LoggerFactory.getLogger(this.javaClass)

    companion object {
        fun getVersion(): String {
            return "1.0.2"
        }
    }

    override fun run(vararg args: String?) {
        log.info("APP_VERSION = ${getVersion()}")
    }
}

fun main(args: Array<String>) {
    SpringApplication.run(App::class.java, *args)
}

