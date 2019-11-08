package com.example.chess.server.core.advice

import com.example.chess.server.core.Authorized
import com.example.chess.server.core.InjectGame
import com.example.chess.server.core.InjectGameId
import com.example.chess.server.core.InjectUserId
import com.example.chess.server.service.impl.GameService
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.reflect.MethodSignature
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
        val signature = joinPoint.signature as MethodSignature
        val authorized = signature.method.getAnnotation(Authorized::class.java)
        val args = joinPoint.args

        if (authorized.enabled) {

            val request = (RequestContextHolder
                .getRequestAttributes() as ServletRequestAttributes).request

            val gameId = requireNotNull(
                request.getParameter("gameId")
            ) { "couldn't find required parameter 'gameId' in authorized request" }
                .toLong()

            val userId = requireNotNull(
                request.getParameter("userId")
            ) { "couldn't find required parameter 'userId' in authorized request" }

            val game = gameService.findAndCheckGame(gameId)

            if (!authorized.viewerMode) {
                require(game.isUserRegistered(userId)) {
                    "user(id=$userId) has no access to game=$gameId"
                }
                //TODO: можно еще защиту паролем потом прикрутить
            }

            val parameters = signature.method.parameters

            for (i in parameters.indices) {
                if (parameters[i].isAnnotationPresent(InjectGameId::class.java)) {
                    args[i] = gameId
                }
                if (parameters[i].isAnnotationPresent(InjectUserId::class.java)) {
                    args[i] = userId
                }
                if (parameters[i].isAnnotationPresent(InjectGame::class.java)) {
                    args[i] = game
                }
            }
        }

        return joinPoint.proceed(args)
    }
}