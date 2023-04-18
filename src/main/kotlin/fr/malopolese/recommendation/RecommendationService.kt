package fr.malopolese.recommendation

import fr.malopolese.recommendation.model.Video
import org.apache.commons.lang3.ArrayUtils
import org.apache.tinkerpop.gremlin.process.traversal.Order
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource
import org.apache.tinkerpop.gremlin.structure.T
import org.apache.tinkerpop.gremlin.structure.Vertex
import org.springframework.stereotype.Service
import org.springframework.web.reactive.result.view.ViewResolutionResultHandler
import java.util.*
import java.util.stream.Collectors
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.`__` as P
import org.apache.commons.math3.stat.correlation.PearsonsCorrelation

@Service
class RecommendationService(private val g: GraphTraversalSource, private val viewResolutionResultHandler: ViewResolutionResultHandler) {
    fun getRecommendationsForUser(userId: String, count: Int = 5): MutableList<MutableMap<String, Video>>? {

        // Get the source user
        val user = g.V().has("uid", userId)

        // Get 10 random video from the user
        val randomUserVideos  = user.outE("watchtime").order().by(Order.shuffle).limit(10).inV().toList();

        // Get all videos that users most similar users as seen and not the source user.
        val videosFromSimilarUsers = g.V().has("uid", userId).outE("user_similarity")
                .order().by("score", Order.desc)
                .inV().limit(10)
                .outE("watchtime")
                .inV().dedup().not(
                        P.`in`("watchtime").has("uid", userId)
                ).toList().map { it.id() }

        val recommendedVideos: MutableList<Any> = ArrayList()
        randomUserVideos.forEach { video ->

            val ids = recommendedVideos.stream().map { id: Any ->
                id.toString().toInt()
            }.mapToInt { obj: Int -> obj }.toArray()
            val idObjects: Array<out Int>? = ArrayUtils.toObject(ids)

            val similarVideo = g.V(videosFromSimilarUsers)
                .`as`("target")
                .V(video.id())
                .outE("video_similarity")
                .where(P.otherV().`as`("target"))
                .order().by("score", Order.desc).inV()
                .dedup()
                .not(P.hasId<Any>(0, idObjects))
                .limit(1).next()

            recommendedVideos += similarVideo.id()
        }

        return g.V(recommendedVideos).project<Video>("uid", "title", "createdAt", "publisherId", "likes", "views")
            .by("uid")
            .by("title")
            .by("createdAt")
            .by("publisherId")
            .by("likes")
            .by("views")
            .toList()
    }

    fun calculateUserSimilarities() {
        // For each video vertex, calculate its similarity score with all other video vertices
        g.V().hasLabel("user").forEachRemaining { user ->
            g.V().hasLabel("user").not(P.`is`<Any>(user)).forEachRemaining { otherUser ->
                // Calculate the similarity score between the two video vertices here...
                val score: Double = calculateUserSimilarity(user, otherUser)
                // Add an edge with the similarity score between the two video vertices
                g.addE("user_similarity").from(user).to(otherUser).property("score", score).next()
            }
        }
    }

    fun calculateVideoSimilarities() {
        // For each video vertex, calculate its similarity score with all other video vertices
        g.V().hasLabel("video").forEachRemaining { video ->
            g.V().hasLabel("video").not(P.`is`<Any>(video)).forEachRemaining { otherVideo ->
                // Calculate the similarity score between the two video vertices here...
                val score: Double = calculateVideoSimilarity(video, otherVideo)
                // Add an edge with the similarity score between the two video vertices
                g.addE("video_similarity").from(video).to(otherVideo).property("score", score).next()
            }
        }
    }

    fun calculateVideoSimilarity(
        v1: Vertex,
        v2: Vertex
    ): Double {
        // Récupérer le nombre total de likes pour chaque vidéo
        val likesV1 = g.V().has(T.id, v1.id()).properties<Any>("likes").next().value() as Int
        val likesV2 = g.V().has(T.id, v2.id()).properties<Any>("likes").next().value() as Int

        // Récupérer le nombre total de vues pour chaque vidéo
        val viewsV1 = g.V().has(T.id, v1.id()).properties<Any>("views").next().value() as Int
        val viewsV2 = g.V().has(T.id, v2.id()).properties<Any>("views").next().value() as Int

        // Calculer le score de similarité en utilisant une formule de pondération
        return (likesV1 * 0.5 + viewsV1 * 0.3) * (likesV2 * 0.5 + viewsV2 * 0.3)
    }

    fun calculateUserSimilarity(
        u1: Vertex,
        u2: Vertex
    ): Double {

        // Récupérer les vidéos aimées par chaque utilisateur
        val likesU1 =
            g.V().has(T.id, u1.id()).outE().has(T.label, "like").inV().toList()
        val likesU2 =
            g.V().has(T.id, u2.id()).outE().has(T.label, "like").inV().toList()


        // Récupérer les vidéos regardées par chaque utilisateur
        val watchedU1 =
            g.V().has(T.id, u1.id()).outE().has(T.label, "watchtime").inV().toList()
        val watchedU2 =
            g.V().has(T.id, u2.id()).outE().has(T.label, "watchtime").inV().toList()

        // Récupérer les identifiants des vidéos aimées et regardées par chaque utilisateur
        val likedVideosU1 =
            likesU1.stream().map { v: Vertex ->
                v.id().toString()
            }.collect(Collectors.toSet())
        val likedVideosU2 =
            likesU2.stream().map { v: Vertex ->
                v.id().toString()
            }.collect(Collectors.toSet())
        val watchedVideosU1 =
            watchedU1.stream().map { v: Vertex ->
                v.id().toString()
            }.collect(Collectors.toSet())
        val watchedVideosU2 =
            watchedU2.stream().map { v: Vertex ->
                v.id().toString()
            }.collect(Collectors.toSet())


        // Calculer les vidéos en commun entre les deux utilisateurs
        val commonLikedVideos: MutableSet<String> = HashSet(likedVideosU1)
        commonLikedVideos.retainAll(likedVideosU2)
        val commonWatchedVideos: MutableSet<String> = HashSet(watchedVideosU1)
        commonWatchedVideos.retainAll(watchedVideosU2)

        // Vérifier si le nombre de vidéos en commun est suffisant
        if (commonLikedVideos.size < 2 || commonWatchedVideos.size < 2) {
            return 0.0 // ou une valeur par défaut
        }

        // Calculer les notes (0 ou 1) pour chaque vidéo aimée et regardée par chaque utilisateur
        val u1Likes: DoubleArray = calculateVideosRating(likedVideosU1, commonLikedVideos)
        val u2Likes: DoubleArray = calculateVideosRating(likedVideosU2, commonLikedVideos)
        val u1Watched: DoubleArray = calculateVideosRating(watchedVideosU1, commonWatchedVideos)
        val u2Watched: DoubleArray = calculateVideosRating(watchedVideosU2, commonWatchedVideos)

        // Calculer la corrélation de Pearson entre les deux utilisateurs
        var minLen = u1Likes.size.coerceAtMost(u2Likes.size)
        val truncatedU1Likes = u1Likes.copyOfRange(0, minLen)
        val truncatedU2Likes = u2Likes.copyOfRange(0, minLen)
        minLen = u1Watched.size.coerceAtMost(u2Watched.size)
        val truncatedU1Watched = u1Watched.copyOfRange(0, minLen)
        val truncatedU2Watched = u2Watched.copyOfRange(0, minLen)
        return PearsonsCorrelation().correlation(
            truncatedU1Likes,
            truncatedU2Likes
        ) + PearsonsCorrelation().correlation(truncatedU1Watched, truncatedU2Watched)
    }

    fun calculateVideosRating(videos: Set<String?>, commonVideo: Set<String?>): DoubleArray {
        return videos.stream().map { v: String? ->
            if (commonVideo.contains(
                    v
                )
            ) 1.0 else 0.0
        }.mapToDouble { obj: Double -> obj }.toArray()
    }

}