package core

import de.jensklingenberg.ktorfit.Ktorfit
import de.jensklingenberg.ktorfit.converter.Converter
import de.jensklingenberg.ktorfit.converter.KtorfitResult
import de.jensklingenberg.ktorfit.converter.TypeData
import io.ktor.client.statement.HttpResponse
import io.ktor.util.reflect.TypeInfo

class KtorConverterFactory : Converter.Factory {

    private suspend fun HttpResponse.body(typeInfo: TypeInfo): Any {
        return this.call.body(typeInfo)
    }

    override fun suspendResponseConverter(
        typeData: TypeData,
        ktorfit: Ktorfit
    ): Converter.SuspendResponseConverter<HttpResponse, *>? {
        if (typeData.typeInfo.type == NetworkResponseWrapper::class) {
            return object : Converter.SuspendResponseConverter<HttpResponse, Any> {
                override suspend fun convert(result: KtorfitResult): Any {
                    return when (result) {
                        is KtorfitResult.Success -> {
                            val innerType = typeData.typeArgs.first().typeInfo
                            val data = result.response.body(innerType)
                            NetworkResponseWrapper.success(data)
                        }
                        is KtorfitResult.Failure -> {
                            val throwable = result.throwable
                            NetworkResponseWrapper.error(throwable, null)
                        }
                    }
                }
            }
        }
        return null
    }
}