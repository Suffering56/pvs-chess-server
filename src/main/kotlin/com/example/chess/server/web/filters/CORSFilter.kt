package com.example.chess.server.web.filters

import org.springframework.stereotype.Component

import javax.servlet.*
import javax.servlet.http.HttpServletResponse

/**
 * @author v.peschaniy
 *      Date: 22.07.2019
 */
@Component
class CORSFilter : Filter {

    override fun doFilter(req: ServletRequest, res: ServletResponse, chain: FilterChain) {
        val response = res as HttpServletResponse
        response.setHeader("Access-Control-Allow-Origin", "*")
        response.setHeader("Access-Control-Allow-Methods", "POST, GET, OPTIONS, DELETE")
        response.setHeader("Access-Control-Max-Age", "3600")
        response.setHeader("Access-Control-Allow-Headers", "x-requested-with")
        chain.doFilter(req, res)
    }

    override fun init(filterConfig: FilterConfig) {}

    override fun destroy() {}
}