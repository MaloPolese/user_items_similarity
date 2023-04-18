package fr.malopolese.recommendation

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

@SpringBootApplication
@ConfigurationPropertiesScan
class RecommendationApplication

fun main(args: Array<String>) {
	runApplication<RecommendationApplication>(*args)
}
