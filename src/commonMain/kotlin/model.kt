import kotlinx.serialization.Serializable

@Serializable
data class NewMessageEvent(val message: String)

@Serializable
data class DataEntry(val userId: Int, val message: String)