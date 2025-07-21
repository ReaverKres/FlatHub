package di

import org.koin.core.qualifier.named

object DataQualifiers {
    val ONLINER_KTORFIT = named("ONLINER_KTORFIT_QUALIFIER")
    val KUFAR_KTORFIT = named("KUFAR_KTORFIT_QUALIFIER")

    val KUFAR_FLAT_MAPPER = named("KUFAR_FLAT_MAPPER")
    val ONLINER_FLAT_MAPPER = named("ONLINER_FLAT_MAPPER")
}