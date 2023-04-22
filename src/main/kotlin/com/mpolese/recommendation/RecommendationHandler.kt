package com.mpolese.recommendation

import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import reactor.core.publisher.Mono

@Component
class RecommendationHandler(private val recommendationService: RecommendationService) {
    private val logger = LoggerFactory.getLogger(javaClass)
    fun getRecommendationForUser(request: ServerRequest?): Mono<ServerResponse> {
        val id = request?.pathVariable("id").toString()
        val limit = request?.queryParam("limit")?.orElse("5")?.toInt()

        logger.info("Getting recommendations for user $id")

        val items = recommendationService.getRecommendationsForUser(id, limit!!)

        return ServerResponse
            .ok()
            .contentType(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue<Any>(items))
    }

    fun processUserSimilarity(request: ServerRequest?): Mono<ServerResponse> {
        logger.info("Process user similarity calculation")

        var start = System.currentTimeMillis()
        recommendationService.calculateUserSimilarities()
        var end = System.currentTimeMillis()

        logger.info("En process user similarity calculation in ${end - start}ms")

        return ServerResponse
            .ok()
            .contentType(MediaType.TEXT_PLAIN)
            .body(BodyInserters.fromValue<String>("Done successfully in ${end - start}ms"))
    }

    fun processVideoSimilarity(request: ServerRequest?): Mono<ServerResponse> {
        logger.info("Process video similarity calculation")

        var start = System.currentTimeMillis()
        recommendationService.calculateVideoSimilarities()
        var end = System.currentTimeMillis()

        logger.info("En process video similarity calculation in ${end - start}ms")
        return ServerResponse
            .ok()
            .contentType(MediaType.TEXT_PLAIN)
            .body(BodyInserters.fromValue<String>("Done successfully in ${end - start}ms"))
    }
}