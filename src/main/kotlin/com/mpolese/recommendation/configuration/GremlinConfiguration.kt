package com.mpolese.recommendation.configuration

import org.apache.tinkerpop.gremlin.driver.remote.DriverRemoteConnection
import org.apache.tinkerpop.gremlin.process.traversal.AnonymousTraversalSource
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class GremlinConfiguration(private val gremlinProperties: GremlinConfigurationProperties) {
    @Bean
    fun getTraversalSource(): GraphTraversalSource {
        return AnonymousTraversalSource.traversal().withRemote(
            DriverRemoteConnection.using(
                gremlinProperties.host,
                gremlinProperties.port,
                gremlinProperties.remoteTraversalSource
            )
        )
    }
}