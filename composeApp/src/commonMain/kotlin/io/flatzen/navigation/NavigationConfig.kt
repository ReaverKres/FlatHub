package io.flatzen.navigation

import androidx.navigation3.runtime.NavKey
import androidx.savedstate.serialization.SavedStateConfiguration
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic

val navigationConfig = SavedStateConfiguration {
    serializersModule = SerializersModule {
        polymorphic(NavKey::class) {
            subclass(Route.List::class, Route.List.serializer())
            subclass(Route.Favorites::class, Route.Favorites.serializer())
            subclass(Route.Swipe::class, Route.Swipe.serializer())
            subclass(Route.Settings::class, Route.Settings.serializer())
            subclass(Route.Map::class, Route.Map.serializer())
            subclass(Route.Detail::class, Route.Detail.serializer())
            subclass(Route.Filter::class, Route.Filter.serializer())
            subclass(Route.Location::class, Route.Location.serializer())
            subclass(Route.CitySelect::class, Route.CitySelect.serializer())
            subclass(Route.MetroSelect::class, Route.MetroSelect.serializer())
            subclass(Route.DistrictSelect::class, Route.DistrictSelect.serializer())
            subclass(Route.Faq::class, Route.Faq.serializer())
            subclass(Route.Referral::class, Route.Referral.serializer())
            subclass(Route.Notifications::class, Route.Notifications.serializer())
        }
    }
}
