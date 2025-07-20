import kotlinx.serialization.Serializable

@Serializable
data class PayloadBaseResponse<T>(var payload: T) : BaseResponse() {
    override fun toString(): String {
        return "${super.toString()} DataRestResponse(data=$payload)"
    }
}

@Serializable
open class BaseResponse {
    var code: Int = -1
    var description: String? = null

    override fun toString(): String {
        return "RestResponse(code=$code, error=$description)"
    }
}
