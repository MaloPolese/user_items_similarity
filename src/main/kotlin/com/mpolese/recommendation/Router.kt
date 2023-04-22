package com.mpolese.recommendation

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.server.RequestPredicates
import org.springframework.web.reactive.function.server.RouterFunction
import org.springframework.web.reactive.function.server.RouterFunctions
import org.springframework.web.reactive.function.server.ServerResponse

@Configuration(proxyBeanMethods = false)
class Router {
    @Bean
    fun route(handler: RecommendationHandler): RouterFunction<ServerResponse> {

        return RouterFunctions.route(
            RequestPredicates.GET("/v1/recommendations/{id}").and(RequestPredicates.accept(MediaType.APPLICATION_JSON))
        ) { handler.getRecommendationForUser(it) }
            .and(RouterFunctions.route(RequestPredicates.GET("/v1/videoSimilarity")) { handler.processVideoSimilarity(it) })
            .and(RouterFunctions.route(RequestPredicates.GET("/v1/userSimilarity")) { handler.processUserSimilarity(it) })
    }
}