package mappers.kufar

import entities.AppFlat
import entities.ContactInformation
import entities.FlatDevInfo
import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.nodes.Document
import io.flatzen.commoncomponents.commonentities.Coordinates
import io.flatzen.commoncomponents.commonentities.FlatPlatform
import mappers.base.AdditionalParamMapper
import kotlin.time.ExperimentalTime

class KufarDetailHtmlMapper : AdditionalParamMapper<String, AppFlat> {

    override fun map(baseFlat: AppFlat, html: String): AppFlat {
        if (html.isBlank()) return baseFlat
        val doc: Document = Ksoup.parse(html)
        val description = (doc.select("[itemprop=description]").firstOrNull()?.html()
            ?: doc.select("meta[name=description]").attr("content")
                ).replace("<br>", "")
            .ifBlank { baseFlat.description }

        return baseFlat.copy(
            flatDevInfo = FlatDevInfo(
                isDetailData = true,
                isDetailLoaded = true
            ),
            description = description?.ifBlank { baseFlat.description }
        )
    }
}