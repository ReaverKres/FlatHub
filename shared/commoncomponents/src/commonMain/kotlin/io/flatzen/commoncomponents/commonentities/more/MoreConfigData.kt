package io.flatzen.commoncomponents.commonentities.more

import io.flatzen.commoncomponents.commonentities.more.MoreConfigData.DonateConfigDataItem
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MoreConfigData(
    @SerialName("isVisible") val isVisible: Boolean?,
    @SerialName("telegramSupport") val telegramSupport: DonateConfigDataItem?,
    @SerialName("donateDescription") val donateDescription: String?,
    @SerialName("donateItems") val donateItems: List<DonateConfigDataItem>
) {
    @Serializable
    data class DonateConfigDataItem(
        @SerialName("imageUrl") val imageUrl: String? = null,
        @SerialName("instrumentType") val instrumentType: InstrumentType,
        @SerialName("name") val text: String,
        @SerialName("type") val type: MoreConfigType,
        @SerialName("value") val value: String
    )

    @Serializable
    enum class MoreConfigType {
        @SerialName("link") LINK,
        @SerialName("text") TEXT,
        @SerialName("crypto") CRYPTO
    }

    @Serializable
    enum class InstrumentType {
        @SerialName("Telegram") TELEGRAM,
        @SerialName("Boosty") BOOSTY,
        @SerialName("Bitcoin") BITCOIN,
        @SerialName("USDT") USDT,
        @SerialName("ETH") ETH
    }
}