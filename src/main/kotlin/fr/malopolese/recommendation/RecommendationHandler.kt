package fr.malopolese.recommendation

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
        logger.info("Getting recommendations for user $id")

        val items = recommendationService.getRecommendationsForUser(id)
            ?: return ServerResponse
                .status(500)
                .body(BodyInserters.fromValue<String>("Failed to fetch recommendations for user $id"))

        return ServerResponse
            .ok()
            .contentType(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue<Any>(items))
    }
}