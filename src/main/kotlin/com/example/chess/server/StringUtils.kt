package com.example.chess.server

fun String.tabs(offset: Int): String {
    if (offset <= 0) {
        return this
    }
    val replacement = "\t".repeat(offset)
    val result = this.replace("\r\n", "\r\n$replacement")

    if (!this.startsWith("\r\n")) {
        return replacement + result
    }

    return result
}

