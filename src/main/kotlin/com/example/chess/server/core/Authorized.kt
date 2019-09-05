package com.example.chess.server.core

/**
 * @author v.peschaniy
 *      Date: 05.09.2019
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Authorized(val required: Boolean = true)