package server_request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OnlinerSearchParams(
    val bounds: OnlinerBounds = OnlinerBounds.MINSK,
    @SerialName("page") val page: Int = 1,
    @SerialName("order") val order: OnlinerOrder = OnlinerOrder.CREATED_DESC,
)

@Serializable
data class OnlinerBounds(
    @SerialName("bounds[lb][lat]") val leftBottomLat: Double,
    @SerialName("bounds[lb][long]") val leftBottomLong: Double,
    @SerialName("bounds[rt][lat]") val rightTopLat: Double,
    @SerialName("bounds[rt][long]") val rightTopLong: Double
) {
    companion object {
        val MINSK = OnlinerBounds(
            leftBottomLat = MinskBounds.LEFT_BOTTOM_LAT,
            leftBottomLong = MinskBounds.LEFT_BOTTOM_LONG,
            rightTopLat = MinskBounds.RIGHT_TOP_LAT,
            rightTopLong = MinskBounds.RIGHT_TOP_LONG
        )
    }
}

object MinskBounds {
    const val LEFT_BOTTOM_LAT = 53.69020141273198
    const val LEFT_BOTTOM_LONG = 27.32505798339844
    const val RIGHT_TOP_LAT = 54.10530722783162
    const val RIGHT_TOP_LONG = 27.798843383789066
}

enum class OnlinerOrder(val value: String) {
    @SerialName("created_at:desc")
    CREATED_DESC("created_at:desc"),

    @SerialName("created_at:asc")
    CREATED_ASC("created_at:asc"),

    @SerialName("price:desc")
    PRICE_DESC("price:desc"),

    @SerialName("price:asc")
    PRICE_ASC("price:asc")
}