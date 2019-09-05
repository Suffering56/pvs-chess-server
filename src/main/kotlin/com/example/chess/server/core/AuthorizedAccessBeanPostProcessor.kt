package com.example.chess.server.core

import com.example.chess.server.service.IGameService
import com.example.chess.server.web.DebugController
import org.springframework.beans.BeansException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.config.BeanPostProcessor
import org.springframework.context.ApplicationContext
import org.springframework.lang.Nullable
import org.springframework.stereotype.Component
import java.lang.reflect.Proxy
import kotlin.reflect.full.declaredFunctions
import kotlin.reflect.full.findAnnotation
import kotlin.streams.toList

/**
 * @author v.peschaniy
 *      Date: 05.09.2019
 */

@Component
class AuthorizedAccessBeanPostProcessor : BeanPostProcessor {

    @Autowired
    lateinit var ctx: ApplicationContext

    @Nullable
    @Throws(BeansException::class)
    override fun postProcessBeforeInitialization(bean: Any, beanName: String): Any? {

        return bean
    }

    @Nullable
    @Throws(BeansException::class)
    override fun postProcessAfterInitialization(bean: Any, beanName: String): Any? {
        if (bean is DebugController) {
            println("bean11 = ${bean}")

            val gameService = ctx.getBean(IGameService::class.java)
            println("gameService = ${gameService}")

//            val proxyBuilder = ByteBuddy()
//                .subclass(bean::class.java)
//                .name(bean::class.java.name + "Proxy")

            val annotatedFunctions = bean::class.declaredFunctions.stream()
                .filter { it.findAnnotation<Authorized>() != null }
                .toList()

            if (annotatedFunctions.any()) {

                return bean::class.java.cast(
                    Proxy.newProxyInstance(
                        bean::class.java.classLoader,
                        bean::class.java.interfaces
                    ) { proxy, method, args ->
                        println("before invocation")
                        method.invoke(bean, args)
                        println("after invocation")
                    }
                )

//                return Proxy.newProxyInstance(
//                    bean::class.java.classLoader,
//                    bean::class.java.interfaces
//                ) { proxy, method, args ->
//                    println("before invocation")
//                    method.invoke(bean, args)
//                    println("after invocation")
//                }


//                return proxyBuilder
//                    .method(ElementMatchers.isAnnotatedWith(Authorized::class.java))
//                    .intercept(InvocationHandlerAdapter.of { proxy, method, args ->
//                        //                    println("beforeInvoke")
////                    method.invoke(args)
////                    println("afterInvoke")
//                    })
//                    .make()
//                    .load(javaClass.classLoader)
//                    .loaded
//                    .newInstance()

            }
        }

        return bean
    }
}