import org.apache.tinkerpop.gremlin.structure.Edge
import org.apache.tinkerpop.gremlin.structure.Graph
import org.apache.tinkerpop.gremlin.structure.Vertex
import org.apache.tinkerpop.gremlin.structure.T

/**
 * Loads the movie lens dataset into a given graph.  See readme for more information.
 *
 */
class MovieLensParser {

    public static void parse(final Graph graph, final String dataDirectory) {

        def g = graph.traversal()

        println "Add Users"
        // Open ./video_public_user.json and for each item in json array create a vertex
        def userJson = new File(dataDirectory + '/video_public_user.json').text
        def userJsonArray = new groovy.json.JsonSlurper().parseText(userJson)
        userJsonArray.each {
            def userVertex = graph.addVertex(label, 'user', 'uid', 'u' + it.userId, "firstName", it.firstName, "lastName", it.lastName)
        }

        println "Add Videos"
        // Open ./video_public_video.json and for each item in json array create a vertex
        def videoJson = new File(dataDirectory + '/video_public_video.json').text
        def videoJsonArray = new groovy.json.JsonSlurper().parseText(videoJson)
        videoJsonArray.each {
            def videoVertex = graph.addVertex(label, 'video', 'uid', 'v' + it.id, 'title', it.title, 'createdAt', it.createdAt, 'likes', it.likes, 'views', it.views, 'publisherId', it.publisherId)

            if (!g.V().has('uid', 'u' + it.publisherId).hasNext()) {
                return
            }
            // Add a publisher edge from the user to the video
            def publisherVertex = g.V().has('uid', 'u' + it.publisherId).next()
            publisherVertex.addEdge('publisher', videoVertex)
        }

        println 'Add likes'
        // Open ./video_public_like.json and for each item in json array create a vertex
        def likeJson = new File(dataDirectory + '/video_public_like.json').text
        def likeJsonArray = new groovy.json.JsonSlurper().parseText(likeJson)
        likeJsonArray.each {
            // Each items has an index, a user id, and a video id
            // For each items create an edge from the user to the video with a label of 'like'
            if (!g.V().has('uid', 'u' + it.userId).hasNext() || !g.V().has('uid', 'v' + it.videoId).hasNext()) {
                return
            }

            def userVertex = g.V().has('uid', 'u' + it.userId).next()
            def videoVertex = g.V().has('uid', 'v' + it.videoId).next()

            def likeEdge = userVertex.addEdge('like', videoVertex)
            // def likeVertex = graph.addVertex(label, 'like', 'uid', 'l' + it.index, 'userId', it.userId, 'videoId', it.videoId)
        }

        println 'Add views'
        // Open ./video_public_watchtime.json   and for each item in json array create a vertex
        // each item has an index, a user id, watchedSeconds, watchedPercent, isWatched, videoId
        def viewJson = new File(dataDirectory + '/video_public_watchtime.json').text
        def viewJsonArray = new groovy.json.JsonSlurper().parseText(viewJson)
        viewJsonArray.each {
            if (!g.V().has('uid', 'u' + it.userId).hasNext() || !g.V().has('uid', 'v' + it.videoId).hasNext()) {
                return
            }
            def userVertex = g.V().has('uid', 'u' + it.userId).next()
            def videoVertex = g.V().has('uid', 'v' + it.videoId).next()
            def viewEdge = userVertex.addEdge('watchtime', videoVertex, 'watchedSeconds', it.watchedSeconds.toDouble(), 'watchedPercent', it.watchedPercent.toDouble(), 'isWatched', it.isWatched)


            // def viewVertex = graph.addVertex(label, 'watchtime', 'uid', 'w' + it.index, 'userId', it.userId, 'videoId', it.videoId, 'watchedSeconds', it.watchedSeconds, 'watchedPercent', it.watchedPercent, 'isWatched', it.isWatched)
        }

    }

    public static void load(final Graph graph, final String dataDirectory) {
        println 'Start'
       // TinkerPop dependent.. should be a factory for crossImplementation use.
        // graph.createIndex('uid', Vertex.class) 
        def start = System.currentTimeMillis()
        parse(graph, dataDirectory)
        println "Loading took (ms): " + (System.currentTimeMillis() - start)
    }
}