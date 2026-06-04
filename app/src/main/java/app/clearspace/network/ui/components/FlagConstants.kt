package app.clearspace.network.ui.components

object FlagConstants {

    enum class FlagType {
        CATEGORY, EMOTION
    }

    val CATEGORIES = listOf(
        "Politics", "Religion", "Sports", "Music", "News", "Science", "Technology",
        "Art", "Literature", "History", "Philosophy", "Health", "Food", "Travel",
        "Nature", "Comedy", "Education", "DIY", "Finance", "Fashion", "Gaming",
        "Movies", "TV", "Photography", "Podcasts", "Automotive", "Pets", "Parenting",
        "Relationships", "Career", "Fitness", "Mental Health", "Culture", "Language",
        "Architecture", "Gardening", "Crafts", "Collecting", "Volunteering",
        "Local Events", "Other"
    )

    val EMOTIONS = listOf(
        "Happy", "Sad", "Cheerful", "Angry", "Inspired", "Anxious", "Calm",
        "Excited", "Nostalgic", "Amused", "Hopeful", "Frustrated", "Grateful",
        "Confused", "Proud", "Disgusted", "Surprised", "Moved", "Bored", "Scared",
        "Empowered", "Lonely", "Peaceful", "Curious", "Overwhelmed", "Determined",
        "Melancholic", "Playful", "Tender", "Rebellious"
    )
}
