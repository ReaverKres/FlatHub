package server_response


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class KufarListResponse(
    @SerialName("ads")
    val ads: List<Ad?>?,
    @SerialName("pagination")
    val pagination: Pagination?,
    @SerialName("total")
    val total: Int?
) {
    @Serializable
    data class Ad(
        @SerialName("account_id")
        val accountId: String?,
        @SerialName("account_parameters")
        val accountParameters: List<AdParameter?>?,
        @SerialName("ad_id")
        val adId: Int?,
        @SerialName("ad_link")
        val adLink: String?,
        @SerialName("ad_parameters")
        val adParameters: List<AdParameter?>?,
        @SerialName("body_short")
        val bodyShort: String?,
        @SerialName("category")
        val category: String?,
        @SerialName("company_ad")
        val companyAd: Boolean?,
        @SerialName("currency")
        val currency: String?,
        @SerialName("images")
        val images: List<Image?>?,
        @SerialName("is_mine")
        val isMine: Boolean?,
        @SerialName("list_id")
        val listId: Int?,
        @SerialName("list_time")
        val listTime: String?,
        @SerialName("message_id")
        val messageId: String?,
        @SerialName("paid_services")
        val paidServices: PaidServices?,
        @SerialName("phone_hidden")
        val phoneHidden: Boolean?,
        @SerialName("price_byn")
        val priceByn: String?,
        @SerialName("price_usd")
        val priceUsd: String?,
        @SerialName("remuneration_type")
        val remunerationType: String?,
        @SerialName("show_parameters")
        val showParameters: ShowParameters?,
        @SerialName("subject")
        val subject: String?,
        @SerialName("type")
        val type: String?
    ) {
        @Serializable
        data class AccountParameter(
            @SerialName("g")
            val g: List<G?>?,
            @SerialName("p")
            val p: String?,
            @SerialName("pl")
            val pl: String?,
            @SerialName("pu")
            val pu: String?,
            @SerialName("v")
            val v: JsonElement? = null,
            @SerialName("vl")
            val vl: JsonElement? = null
        ) {
            @Serializable
            data class G(
                @SerialName("gi")
                val gi: Int?,
                @SerialName("gl")
                val gl: String?,
                @SerialName("go")
                val go: Int?,
                @SerialName("po")
                val po: Int?
            )
        }

        @Serializable
        data class AdParameter(
            @SerialName("g")
            val g: List<G?>?,
            @SerialName("p")
            val p: String?,
            @SerialName("pl")
            val pl: String?,
            @SerialName("pu")
            val pu: String?,
            @SerialName("v")
            val v: JsonElement? = null, //
            @SerialName("vl")
            val vl: JsonElement? = null //
        ) {
            @Serializable
            data class G(
                @SerialName("gi")
                val gi: Int?,
                @SerialName("gl")
                val gl: String?,
                @SerialName("go")
                val go: Int?,
                @SerialName("po")
                val po: Int?
            )
        }

        @Serializable
        data class Image(
            @SerialName("id")
            val id: String?,
            @SerialName("media_storage")
            val mediaStorage: String?,
            @SerialName("path")
            val path: String?,
            @SerialName("yams_storage")
            val yamsStorage: Boolean?
        )

        @Serializable
        data class PaidServices(
            @SerialName("halva")
            val halva: Boolean?,
            @SerialName("highlight")
            val highlight: Boolean?,
            @SerialName("polepos")
            val polepos: Boolean?
        )

        @Serializable
        data class ShowParameters(
            @SerialName("show_call")
            val showCall: Boolean?,
            @SerialName("show_chat")
            val showChat: Boolean?,
            @SerialName("show_import_link")
            val showImportLink: Boolean?,
            @SerialName("show_web_shop_link")
            val showWebShopLink: Boolean?
        )
    }

    @Serializable
    data class Pagination(
        @SerialName("pages")
        val pages: List<Page?>?
    ) {
        @Serializable
        data class Page(
            @SerialName("label")
            val label: String?,
            @SerialName("num")
            val num: Int?,
            @SerialName("token")
            val token: String?
        )
    }
}