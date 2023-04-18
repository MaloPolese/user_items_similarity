package fr.malopolese.recommendation.model

data class Video(
    val uid: String,
    val title: String,
    val createdAt: String,
    val publisherId: String,
    val likes: Int,
    val views: Int,
)
