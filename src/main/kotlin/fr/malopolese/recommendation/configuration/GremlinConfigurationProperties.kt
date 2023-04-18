package fr.malopolese.recommendation.configuration

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("gremlin")
data class GremlinConfigurationProperties(
    val host: String,
    val port: Int,
    val remoteTraversalSource: String
)