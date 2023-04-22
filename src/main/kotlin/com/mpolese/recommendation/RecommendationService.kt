package com.mpolese.recommendation

import com.mpolese.recommendation.model.Video
import org.apache.commons.math3.stat.correlation.PearsonsCorrelation
import org.apache.tinkerpop.gremlin.process.traversal.Order
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource
import org.apache.tinkerpop.gremlin.structure.T
import org.apache.tinkerpop.gremlin.structure.Vertex
import org.springframework.stereotype.Service
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.`__` as P

@Service
class RecommendationService(private val g: GraphTraversalSource) {
    fun getRecommendationsForUser(userId: String, limit: Int = 5): MutableList<Video> {
        // Get the source user
        val user = g.V().has("uid", userId)

        // Get 10 random video from the user
        val randomUserVideos  = user.outE("watchtime").order().by(Order.shuffle).inV().toList();

        // Get all videos that users most similar users as seen and not the source user.
        val videosFromSimilarUsers = g.V().has("uid", userId).outE("user_similarity")
                .order().by("score", Order.desc)
                .inV().limit(100)
                .outE("watchtime").has("isWatched", true)
                .inV().dedup().not(
                        P.`in`("watchtime").has("uid", userId).has("isWatched", true)
                ).toList().map { it.id() }

        val recommendedVertexes: MutableList<Vertex> = ArrayList()
        var vertexOccurrences = hashMapOf<String, Double>();
        randomUserVideos.forEach { video ->
            val ids = recommendedVertexes.map { it.id().toString().toInt() }.toTypedArray()

            val similarVertexes = g.V(videosFromSimilarUsers)
                .`as`("target")
                .V(video.id())
                .outE("video_similarity")
                .where(P.otherV().`as`("target"))
                .order().by("score", Order.desc).inV()
                .dedup()
                .not(P.hasId<Any>(-1, ids))
                .limit(50).toList();

            if (similarVertexes.isNotEmpty()) {
                recommendedVertexes += similarVertexes.first();

                similarVertexes.forEachIndexed { index, element ->
                    if (vertexOccurrences.containsKey(element.id().toString())) {
                        vertexOccurrences[element.id().toString()] = vertexOccurrences[element.id().toString()]!! + (similarVertexes.size - index)
                    } else {
                        vertexOccurrences[element.id().toString()] = ((similarVertexes.size - index).toDouble())
                    }
                }
            }
        }

        if (recommendedVertexes.isEmpty()) {
            return ArrayList()
        }

        val max = vertexOccurrences.values.max();
        var videos = mutableListOf<Video>()
        recommendedVertexes.forEach {
            val uid = g.V(it.id()).properties<String>("uid").next().value()
            val title = g.V(it.id()).properties<String>("title").next().value()

            val videoOccurrence = vertexOccurrences[it.id().toString()]!!
            val score: Double = videoOccurrence / max

            videos += Video(uid, title, score)
        }
        videos.sortByDescending { it.score }

        if (limit < videos.size) {
            return videos.subList(0, limit )
        }
        return videos
    }

    fun calculateUserSimilarities() {
        // For each video vertex, calculate its similarity score with all other video vertices
        g.V().hasLabel("user").forEachRemaining { user ->
            if (!g.V(user.id()).outE().hasLabel("user_similarity").hasNext()) {
                g.V().hasLabel("user").not(P.`is`<Vertex>(user)).forEachRemaining { otherUser ->
                    // Calculate the similarity score between the two video vertices here...
                    val score: Double = calculateUserSimilarity(user, otherUser)
                    // Add an edge with the similarity score between the two video vertices
                    g.addE("user_similarity").from(user).to(otherUser).property("score", score).next()
                }
            }
        }
    }

    fun calculateVideoSimilarities() {
        // For each video vertex, calculate its similarity score with all other video vertices
        g.V().hasLabel("video").forEachRemaining { video ->
            if (!g.V(video.id()).outE().hasLabel("video_similarity").hasNext()) {
                g.V().hasLabel("video").not(P.`is`<Any>(video)).forEachRemaining { otherVideo ->
                    // Calculate the similarity score between the two video vertices here...
                    val score: Double = calculateVideoSimilarity(video, otherVideo)
                    // Add an edge with the similarity score between the two video vertices
                    g.addE("video_similarity").from(video).to(otherVideo).property("score", score).next()
                }
            }

        }
    }

    fun calculateVideoSimilarity(
        v1: Vertex,
        v2: Vertex
    ): Double {
        // Retrieve the total number of likes for each video.
        val likesV1 = g.V().has(T.id, v1.id()).properties<Any>("likes").next().value() as Int
        val likesV2 = g.V().has(T.id, v2.id()).properties<Any>("likes").next().value() as Int

        // Retrieve the total number of views for each video.
        val viewsV1 = g.V().has(T.id, v1.id()).properties<Any>("views").next().value() as Int
        val viewsV2 = g.V().has(T.id, v2.id()).properties<Any>("views").next().value() as Int

        // Calculate the similarity score using a weighting formula.
        return (likesV1 * 0.5 + viewsV1 * 0.3) * (likesV2 * 0.5 + viewsV2 * 0.3)
    }

    fun calculateUserSimilarity(
        u1: Vertex,
        u2: Vertex
    ): Double {

        // Retrieve the videos liked by each user.
        val likesU1 =
            g.V().has(T.id, u1.id()).outE().has(T.label, "like").inV().toList()
        val likesU2 =
            g.V().has(T.id, u2.id()).outE().has(T.label, "like").inV().toList()

        // Retrieve the videos watched by each user.
        val watchedU1 =
            g.V().has(T.id, u1.id()).outE().has(T.label, "watchtime").has("isWatched", true).inV().toList()
        val watchedU2 =
            g.V().has(T.id, u2.id()).outE().has(T.label, "watchtime").has("isWatched", true).inV().toList()

        // Retrieve the IDs of the videos liked and watched by each user.
        val likedVideosU1 = likesU1.map { it.id().toString() }.toSet();
        val likedVideosU2 = likesU2.map { it.id().toString() }.toSet();
        val watchedVideosU1 = watchedU1.map { it.id().toString() }.toSet();
        val watchedVideosU2 = watchedU2.map { it.id().toString() }.toSet();

        // Calculate the common videos between the two users.
        val commonLikedVideos: MutableSet<String> = HashSet(likedVideosU1)
        commonLikedVideos.retainAll(likedVideosU2)
        val commonWatchedVideos: MutableSet<String> = HashSet(watchedVideosU1)
        commonWatchedVideos.retainAll(watchedVideosU2)

        // Check if the number of common videos is sufficient.
        if (commonLikedVideos.size < 2 || commonWatchedVideos.size < 2) {
            return 0.0 // ou une valeur par défaut
        }

        // Calculate the ratings (0 or 1) for each video liked and watched by each user.
        val u1Likes = calculateVideosRating(likedVideosU1, commonLikedVideos)
        val u2Likes = calculateVideosRating(likedVideosU2, commonLikedVideos)
        val u1Watched = calculateVideosRating(watchedVideosU1, commonWatchedVideos)
        val u2Watched = calculateVideosRating(watchedVideosU2, commonWatchedVideos)

        // Calculate the Pearson correlation between the two users.
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
        return videos.map {if (commonVideo.contains(it)) 1.0 else 0.0 }.toDoubleArray()
    }

}