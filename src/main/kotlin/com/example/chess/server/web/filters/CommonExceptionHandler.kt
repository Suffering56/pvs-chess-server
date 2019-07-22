package com.example.chess.server.web.filters

import com.fasterxml.jackson.databind.JsonMappingException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class CommonExceptionHandler {

    @ResponseBody
    @ExceptionHandler(JsonMappingException::class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    fun authenticationError(e: JsonMappingException): ResponseEntity<String> {
        System.err.println(e.message)
        e.printStackTrace()
        return ResponseEntity(e.message, HttpStatus.INTERNAL_SERVER_ERROR)
    }
}
