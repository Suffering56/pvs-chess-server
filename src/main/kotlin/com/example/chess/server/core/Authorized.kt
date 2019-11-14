package com.example.chess.server.core

/**
 * @author v.peschaniy
 *      Date: 05.09.2019
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Authorized(

    /**
     * Если true - ты не пройдешь неавторизованным,
     *  если false - пройдешь, и даже заинжектится game
     */
    val youShallNotPass: Boolean = true
)   