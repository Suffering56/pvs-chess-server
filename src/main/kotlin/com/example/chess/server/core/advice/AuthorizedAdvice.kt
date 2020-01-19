package com.example.chess.server.core.advice

import com.example.chess.server.service.impl.GameService
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes

/**
 * @author v.peschaniy
 *      Date: 07.11.2019
 */
@Aspect
@Component
class AuthorizedAdvice {

    @Autowired
    private lateinit var gameService: GameService

    @Around("@annotation(com.example.chess.server.core.Authorized)")
    fun authorize(joinPoint: ProceedingJoinPoint): Any {
        val args = joinPoint.args

        val request = (RequestContextHolder
            .getRequestAttributes() as ServletRequestAttributes).request

        val gameId = requireNotNull(
            request.getParameter("gameId")
        ) { "couldn't find required parameter 'gameId' in authorized request" }
            .toLong()

        val userId = requireNotNull(
            request.getParameter("userId")
        ) { "couldn't find required parameter 'userId' in authorized request" }

        gameService.checkRegistration(gameId, userId)

        return joinPoint.proceed(args)
    }
}