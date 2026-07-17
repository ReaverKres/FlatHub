package io.flatzen.commoncomponents.location

import android.content.Context
import android.telephony.TelephonyManager
import org.koin.java.KoinJavaComponent.getKoin

actual fun networkCountryIso(): String? {
    return runCatching {
        val context = getKoin().get<Context>()
        val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
        tm?.networkCountryIso?.trim()?.takeIf { it.isNotEmpty() }?.uppercase()
    }.getOrNull()
}
