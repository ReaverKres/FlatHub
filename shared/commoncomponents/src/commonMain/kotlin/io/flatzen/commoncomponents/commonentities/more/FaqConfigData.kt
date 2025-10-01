package io.flatzen.commoncomponents.commonentities.more

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FaqConfigData(
    @SerialName("faqItems") val faqItems: List<FaqItem>
) {
    @Serializable
    data class FaqItem(
        @SerialName("title") val title: String,
        @SerialName("description") val description: String
    )
}
