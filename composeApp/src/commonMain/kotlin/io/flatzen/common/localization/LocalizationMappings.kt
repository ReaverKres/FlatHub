package io.flatzen.common.localization

import androidx.compose.runtime.Composable
import flatzen.composeapp.generated.resources.Res
import flatzen.composeapp.generated.resources.add
import flatzen.composeapp.generated.resources.back
import flatzen.composeapp.generated.resources.cancel
import flatzen.composeapp.generated.resources.chip_from_owner
import flatzen.composeapp.generated.resources.chip_new
import flatzen.composeapp.generated.resources.chip_with_photo
import flatzen.composeapp.generated.resources.close
import flatzen.composeapp.generated.resources.commercial_property_all
import flatzen.composeapp.generated.resources.commercial_property_industrial
import flatzen.composeapp.generated.resources.commercial_property_office
import flatzen.composeapp.generated.resources.commercial_property_other
import flatzen.composeapp.generated.resources.commercial_property_retail
import flatzen.composeapp.generated.resources.commercial_property_services
import flatzen.composeapp.generated.resources.commercial_property_warehouses
import flatzen.composeapp.generated.resources.copy_success
import flatzen.composeapp.generated.resources.delete
import flatzen.composeapp.generated.resources.detail_about_apartment
import flatzen.composeapp.generated.resources.detail_about_commercial
import flatzen.composeapp.generated.resources.detail_address
import flatzen.composeapp.generated.resources.detail_agent
import flatzen.composeapp.generated.resources.detail_amenities
import flatzen.composeapp.generated.resources.detail_balcony
import flatzen.composeapp.generated.resources.detail_bathroom
import flatzen.composeapp.generated.resources.detail_building
import flatzen.composeapp.generated.resources.detail_contact_info
import flatzen.composeapp.generated.resources.detail_contact_on_source
import flatzen.composeapp.generated.resources.detail_contact_person
import flatzen.composeapp.generated.resources.detail_description
import flatzen.composeapp.generated.resources.detail_district
import flatzen.composeapp.generated.resources.detail_floor
import flatzen.composeapp.generated.resources.detail_for_whom
import flatzen.composeapp.generated.resources.detail_in_apartment
import flatzen.composeapp.generated.resources.detail_in_kitchen
import flatzen.composeapp.generated.resources.detail_infrastructure
import flatzen.composeapp.generated.resources.detail_kitchen_area
import flatzen.composeapp.generated.resources.detail_living_area
import flatzen.composeapp.generated.resources.detail_living_conditions
import flatzen.composeapp.generated.resources.detail_location
import flatzen.composeapp.generated.resources.detail_metro
import flatzen.composeapp.generated.resources.detail_open_in_map
import flatzen.composeapp.generated.resources.detail_open_source
import flatzen.composeapp.generated.resources.detail_owner
import flatzen.composeapp.generated.resources.detail_parking
import flatzen.composeapp.generated.resources.detail_phone
import flatzen.composeapp.generated.resources.detail_prepayment
import flatzen.composeapp.generated.resources.detail_repair
import flatzen.composeapp.generated.resources.detail_rooms_count
import flatzen.composeapp.generated.resources.detail_share
import flatzen.composeapp.generated.resources.detail_share_subject
import flatzen.composeapp.generated.resources.detail_sleeping_places
import flatzen.composeapp.generated.resources.detail_tab_conditions
import flatzen.composeapp.generated.resources.detail_tab_description
import flatzen.composeapp.generated.resources.detail_tab_location
import flatzen.composeapp.generated.resources.detail_total_area
import flatzen.composeapp.generated.resources.detail_total_floors
import flatzen.composeapp.generated.resources.detail_windows
import flatzen.composeapp.generated.resources.detail_year_built
import flatzen.composeapp.generated.resources.detail_year_suffix
import flatzen.composeapp.generated.resources.error
import flatzen.composeapp.generated.resources.faq_title
import flatzen.composeapp.generated.resources.filter_active_areas_prefix
import flatzen.composeapp.generated.resources.filter_active_none
import flatzen.composeapp.generated.resources.filter_active_title
import flatzen.composeapp.generated.resources.filter_add_to_my_filters
import flatzen.composeapp.generated.resources.filter_address_prefix
import flatzen.composeapp.generated.resources.filter_area
import flatzen.composeapp.generated.resources.filter_booking_date
import flatzen.composeapp.generated.resources.filter_city_prefix
import flatzen.composeapp.generated.resources.filter_commercial
import flatzen.composeapp.generated.resources.filter_commercial_rent
import flatzen.composeapp.generated.resources.filter_commercial_sale
import flatzen.composeapp.generated.resources.filter_daily
import flatzen.composeapp.generated.resources.filter_deal_type
import flatzen.composeapp.generated.resources.filter_districts_prefix
import flatzen.composeapp.generated.resources.filter_location
import flatzen.composeapp.generated.resources.filter_metro_prefix
import flatzen.composeapp.generated.resources.filter_my_filters
import flatzen.composeapp.generated.resources.filter_name_empty_error
import flatzen.composeapp.generated.resources.filter_name_length_error
import flatzen.composeapp.generated.resources.filter_owner_only
import flatzen.composeapp.generated.resources.filter_photo_only
import flatzen.composeapp.generated.resources.filter_price
import flatzen.composeapp.generated.resources.filter_price_daily
import flatzen.composeapp.generated.resources.filter_price_label
import flatzen.composeapp.generated.resources.filter_price_per_square
import flatzen.composeapp.generated.resources.filter_price_per_square_label
import flatzen.composeapp.generated.resources.filter_property_type
import flatzen.composeapp.generated.resources.filter_push_notifications
import flatzen.composeapp.generated.resources.filter_rent
import flatzen.composeapp.generated.resources.filter_room_only
import flatzen.composeapp.generated.resources.filter_rooms_count
import flatzen.composeapp.generated.resources.filter_rooms_in_apartment
import flatzen.composeapp.generated.resources.filter_sale
import flatzen.composeapp.generated.resources.filter_save_hint
import flatzen.composeapp.generated.resources.filter_save_title
import flatzen.composeapp.generated.resources.filter_save_validation
import flatzen.composeapp.generated.resources.filter_selected_date_range
import flatzen.composeapp.generated.resources.filter_sorting
import flatzen.composeapp.generated.resources.filter_type_prefix
import flatzen.composeapp.generated.resources.filters_title
import flatzen.composeapp.generated.resources.force_update_description
import flatzen.composeapp.generated.resources.force_update_title
import flatzen.composeapp.generated.resources.from
import flatzen.composeapp.generated.resources.list_area_format
import flatzen.composeapp.generated.resources.list_commercial_rooms_suffix
import flatzen.composeapp.generated.resources.list_load_more
import flatzen.composeapp.generated.resources.list_no_more_flats
import flatzen.composeapp.generated.resources.list_page
import flatzen.composeapp.generated.resources.list_rooms_few
import flatzen.composeapp.generated.resources.list_rooms_many
import flatzen.composeapp.generated.resources.list_rooms_one
import flatzen.composeapp.generated.resources.list_rooms_suffix
import flatzen.composeapp.generated.resources.location_address_hint
import flatzen.composeapp.generated.resources.location_city
import flatzen.composeapp.generated.resources.location_districts
import flatzen.composeapp.generated.resources.location_metro
import flatzen.composeapp.generated.resources.location_saved_areas
import flatzen.composeapp.generated.resources.location_search_district
import flatzen.composeapp.generated.resources.location_search_station
import flatzen.composeapp.generated.resources.location_title
import flatzen.composeapp.generated.resources.map_area_name_empty_error
import flatzen.composeapp.generated.resources.map_area_name_hint
import flatzen.composeapp.generated.resources.map_area_name_length_error
import flatzen.composeapp.generated.resources.map_area_name_validation
import flatzen.composeapp.generated.resources.map_draw_instructions
import flatzen.composeapp.generated.resources.map_exit
import flatzen.composeapp.generated.resources.map_refresh
import flatzen.composeapp.generated.resources.map_save_area_title
import flatzen.composeapp.generated.resources.map_saved_areas
import flatzen.composeapp.generated.resources.map_saved_areas_empty
import flatzen.composeapp.generated.resources.map_too_many_objects
import flatzen.composeapp.generated.resources.map_undo
import flatzen.composeapp.generated.resources.more_title
import flatzen.composeapp.generated.resources.no_internet
import flatzen.composeapp.generated.resources.notifications_allow
import flatzen.composeapp.generated.resources.notifications_params
import flatzen.composeapp.generated.resources.notifications_permission_message
import flatzen.composeapp.generated.resources.notifications_title
import flatzen.composeapp.generated.resources.ok
import flatzen.composeapp.generated.resources.premium_active_subtitle
import flatzen.composeapp.generated.resources.premium_active_title
import flatzen.composeapp.generated.resources.premium_active_unlimited
import flatzen.composeapp.generated.resources.premium_active_until
import flatzen.composeapp.generated.resources.premium_debug_active
import flatzen.composeapp.generated.resources.premium_debug_auto
import flatzen.composeapp.generated.resources.premium_debug_label
import flatzen.composeapp.generated.resources.premium_debug_purchase
import flatzen.composeapp.generated.resources.premium_done
import flatzen.composeapp.generated.resources.premium_error_ad_disabled
import flatzen.composeapp.generated.resources.premium_error_ad_unavailable
import flatzen.composeapp.generated.resources.premium_error_billing_unavailable
import flatzen.composeapp.generated.resources.premium_error_generic
import flatzen.composeapp.generated.resources.premium_error_item_already_owned
import flatzen.composeapp.generated.resources.premium_error_item_unavailable
import flatzen.composeapp.generated.resources.premium_error_load_plans
import flatzen.composeapp.generated.resources.premium_error_network
import flatzen.composeapp.generated.resources.premium_error_no_active_subscription
import flatzen.composeapp.generated.resources.premium_error_purchases_unavailable
import flatzen.composeapp.generated.resources.premium_error_restore_failed
import flatzen.composeapp.generated.resources.premium_error_restored
import flatzen.composeapp.generated.resources.premium_error_rewarded_activated
import flatzen.composeapp.generated.resources.premium_error_service_unavailable
import flatzen.composeapp.generated.resources.premium_error_subscription_activated
import flatzen.composeapp.generated.resources.premium_feature_location
import flatzen.composeapp.generated.resources.premium_feature_no_ads
import flatzen.composeapp.generated.resources.premium_feature_realtime
import flatzen.composeapp.generated.resources.premium_location_toast
import flatzen.composeapp.generated.resources.premium_manage
import flatzen.composeapp.generated.resources.premium_menu
import flatzen.composeapp.generated.resources.premium_plan_month
import flatzen.composeapp.generated.resources.premium_plan_quarter
import flatzen.composeapp.generated.resources.premium_plan_week
import flatzen.composeapp.generated.resources.premium_plans_title
import flatzen.composeapp.generated.resources.premium_recommended
import flatzen.composeapp.generated.resources.premium_restore
import flatzen.composeapp.generated.resources.premium_savings
import flatzen.composeapp.generated.resources.premium_source_cache
import flatzen.composeapp.generated.resources.premium_source_fallback
import flatzen.composeapp.generated.resources.premium_source_rewarded
import flatzen.composeapp.generated.resources.premium_source_store
import flatzen.composeapp.generated.resources.premium_source_trial
import flatzen.composeapp.generated.resources.premium_subscribe
import flatzen.composeapp.generated.resources.premium_subtitle
import flatzen.composeapp.generated.resources.premium_title
import flatzen.composeapp.generated.resources.premium_upsell_banner
import flatzen.composeapp.generated.resources.premium_watch_ad
import flatzen.composeapp.generated.resources.referral_activate
import flatzen.composeapp.generated.resources.referral_code
import flatzen.composeapp.generated.resources.referral_description
import flatzen.composeapp.generated.resources.referral_input_hint
import flatzen.composeapp.generated.resources.referral_my_code
import flatzen.composeapp.generated.resources.referral_notifications_available
import flatzen.composeapp.generated.resources.referral_remaining_invites
import flatzen.composeapp.generated.resources.reset
import flatzen.composeapp.generated.resources.save
import flatzen.composeapp.generated.resources.scroll_to_top
import flatzen.composeapp.generated.resources.search_error_title
import flatzen.composeapp.generated.resources.sort_cheapest
import flatzen.composeapp.generated.resources.sort_expensive
import flatzen.composeapp.generated.resources.sort_newest
import flatzen.composeapp.generated.resources.system_notifications_description
import flatzen.composeapp.generated.resources.system_notifications_title
import flatzen.composeapp.generated.resources.system_open_settings
import flatzen.composeapp.generated.resources.tab_favorites
import flatzen.composeapp.generated.resources.tab_home
import flatzen.composeapp.generated.resources.tab_map
import flatzen.composeapp.generated.resources.tab_more
import flatzen.composeapp.generated.resources.telegram_support
import flatzen.composeapp.generated.resources.telegram_support_description
import flatzen.composeapp.generated.resources.theme_dark
import flatzen.composeapp.generated.resources.theme_light
import flatzen.composeapp.generated.resources.theme_system
import flatzen.composeapp.generated.resources.theme_title
import flatzen.composeapp.generated.resources.to
import io.flatzen.commoncomponents.localization.LocalizationKeys
import io.flatzen.monetization.MonetizationDefaults
import io.flatzen.monetization.billing.SubscriptionProduct
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

private val formattedLocalizationKeys = setOf(
    LocalizationKeys.COPY_SUCCESS,
    LocalizationKeys.FILTER_SELECTED_DATE_RANGE,
    LocalizationKeys.FILTER_PRICE,
    LocalizationKeys.FILTER_PRICE_DAILY,
    LocalizationKeys.FILTER_PRICE_PER_SQUARE,
    LocalizationKeys.REFERRAL_MY_CODE,
    LocalizationKeys.REFERRAL_REMAINING_INVITES,
    LocalizationKeys.LIST_PAGE,
    LocalizationKeys.LIST_AREA_FORMAT,
    LocalizationKeys.LIST_ROOMS_ONE,
    LocalizationKeys.LIST_ROOMS_FEW,
    LocalizationKeys.LIST_ROOMS_MANY,
    LocalizationKeys.DETAIL_OPEN_SOURCE,
    LocalizationKeys.PREMIUM_SAVINGS,
    LocalizationKeys.PREMIUM_ACTIVE_UNTIL,
)

@Composable
fun stringResource(key: LocalizationKeys): String {
    return stringResource(key.resource)
}

@Composable
fun stringResource(key: LocalizationKeys, formatArg: Any): String {
    require(key in formattedLocalizationKeys) {
        "Localization key $key does not support a single format argument"
    }
    return stringResource(key.resource, formatArg)
}

@Composable
fun stringResource(key: LocalizationKeys, formatArg1: Any, formatArg2: Any): String {
    require(key == LocalizationKeys.FILTER_SELECTED_DATE_RANGE) {
        "Localization key $key does not support two format arguments"
    }
    return stringResource(key.resource, formatArg1, formatArg2)
}

@Composable
fun localizedProductTitle(product: SubscriptionProduct): String = when (product.id) {
    MonetizationDefaults.PRODUCT_WEEK -> stringResource(LocalizationKeys.PREMIUM_PLAN_WEEK)
    MonetizationDefaults.PRODUCT_MONTH -> stringResource(LocalizationKeys.PREMIUM_PLAN_MONTH)
    MonetizationDefaults.PRODUCT_QUARTER -> stringResource(LocalizationKeys.PREMIUM_PLAN_QUARTER)
    else -> product.title
}

val LocalizationKeys.resource: StringResource
    get() = when (this) {
        LocalizationKeys.TAB_HOME -> Res.string.tab_home
        LocalizationKeys.TAB_FAVORITES -> Res.string.tab_favorites
        LocalizationKeys.TAB_MAP -> Res.string.tab_map
        LocalizationKeys.TAB_MORE -> Res.string.tab_more
        LocalizationKeys.BACK -> Res.string.back
        LocalizationKeys.OK -> Res.string.ok
        LocalizationKeys.CANCEL -> Res.string.cancel
        LocalizationKeys.RESET -> Res.string.reset
        LocalizationKeys.CLOSE -> Res.string.close
        LocalizationKeys.ADD -> Res.string.add
        LocalizationKeys.SAVE -> Res.string.save
        LocalizationKeys.DELETE -> Res.string.delete
        LocalizationKeys.ERROR -> Res.string.error
        LocalizationKeys.NO_INTERNET -> Res.string.no_internet
        LocalizationKeys.COPY_SUCCESS -> Res.string.copy_success
        LocalizationKeys.FROM -> Res.string.from
        LocalizationKeys.TO -> Res.string.`to`
        LocalizationKeys.MORE_TITLE -> Res.string.more_title
        LocalizationKeys.FAQ_TITLE -> Res.string.faq_title
        LocalizationKeys.REFERRAL_CODE -> Res.string.referral_code
        LocalizationKeys.TELEGRAM_SUPPORT -> Res.string.telegram_support
        LocalizationKeys.TELEGRAM_SUPPORT_DESCRIPTION -> Res.string.telegram_support_description
        LocalizationKeys.THEME_TITLE -> Res.string.theme_title
        LocalizationKeys.THEME_SYSTEM -> Res.string.theme_system
        LocalizationKeys.THEME_LIGHT -> Res.string.theme_light
        LocalizationKeys.THEME_DARK -> Res.string.theme_dark
        LocalizationKeys.FILTERS_TITLE -> Res.string.filters_title
        LocalizationKeys.FILTER_MY_FILTERS -> Res.string.filter_my_filters
        LocalizationKeys.FILTER_PROPERTY_TYPE -> Res.string.filter_property_type
        LocalizationKeys.FILTER_DEAL_TYPE -> Res.string.filter_deal_type
        LocalizationKeys.FILTER_RENT -> Res.string.filter_rent
        LocalizationKeys.FILTER_SALE -> Res.string.filter_sale
        LocalizationKeys.FILTER_DAILY -> Res.string.filter_daily
        LocalizationKeys.FILTER_COMMERCIAL -> Res.string.filter_commercial
        LocalizationKeys.FILTER_COMMERCIAL_RENT -> Res.string.filter_commercial_rent
        LocalizationKeys.FILTER_COMMERCIAL_SALE -> Res.string.filter_commercial_sale
        LocalizationKeys.FILTER_SORTING -> Res.string.filter_sorting
        LocalizationKeys.FILTER_LOCATION -> Res.string.filter_location
        LocalizationKeys.FILTER_OWNER_ONLY -> Res.string.filter_owner_only
        LocalizationKeys.FILTER_PHOTO_ONLY -> Res.string.filter_photo_only
        LocalizationKeys.FILTER_ROOM_ONLY -> Res.string.filter_room_only
        LocalizationKeys.FILTER_ROOMS_IN_APARTMENT -> Res.string.filter_rooms_in_apartment
        LocalizationKeys.FILTER_ROOMS_COUNT -> Res.string.filter_rooms_count
        LocalizationKeys.FILTER_BOOKING_DATE -> Res.string.filter_booking_date
        LocalizationKeys.FILTER_SELECTED_DATE_RANGE -> Res.string.filter_selected_date_range
        LocalizationKeys.FILTER_PRICE -> Res.string.filter_price
        LocalizationKeys.FILTER_PRICE_LABEL -> Res.string.filter_price_label
        LocalizationKeys.FILTER_PRICE_DAILY -> Res.string.filter_price_daily
        LocalizationKeys.FILTER_PRICE_PER_SQUARE -> Res.string.filter_price_per_square
        LocalizationKeys.FILTER_PRICE_PER_SQUARE_LABEL -> Res.string.filter_price_per_square_label
        LocalizationKeys.FILTER_AREA -> Res.string.filter_area
        LocalizationKeys.FILTER_ADD_TO_MY_FILTERS -> Res.string.filter_add_to_my_filters
        LocalizationKeys.FILTER_SAVE_TITLE -> Res.string.filter_save_title
        LocalizationKeys.FILTER_SAVE_HINT -> Res.string.filter_save_hint
        LocalizationKeys.FILTER_SAVE_VALIDATION -> Res.string.filter_save_validation
        LocalizationKeys.FILTER_NAME_EMPTY_ERROR -> Res.string.filter_name_empty_error
        LocalizationKeys.FILTER_NAME_LENGTH_ERROR -> Res.string.filter_name_length_error
        LocalizationKeys.FILTER_PUSH_NOTIFICATIONS -> Res.string.filter_push_notifications
        LocalizationKeys.FILTER_ACTIVE_NONE -> Res.string.filter_active_none
        LocalizationKeys.FILTER_ACTIVE_TITLE -> Res.string.filter_active_title
        LocalizationKeys.FILTER_TYPE_PREFIX -> Res.string.filter_type_prefix
        LocalizationKeys.FILTER_METRO_PREFIX -> Res.string.filter_metro_prefix
        LocalizationKeys.FILTER_ADDRESS_PREFIX -> Res.string.filter_address_prefix
        LocalizationKeys.FILTER_CITY_PREFIX -> Res.string.filter_city_prefix
        LocalizationKeys.FILTER_DISTRICTS_PREFIX -> Res.string.filter_districts_prefix
        LocalizationKeys.FILTER_ACTIVE_AREAS_PREFIX -> Res.string.filter_active_areas_prefix
        LocalizationKeys.COMMERCIAL_PROPERTY_ALL -> Res.string.commercial_property_all
        LocalizationKeys.COMMERCIAL_PROPERTY_INDUSTRIAL -> Res.string.commercial_property_industrial
        LocalizationKeys.COMMERCIAL_PROPERTY_OFFICE -> Res.string.commercial_property_office
        LocalizationKeys.COMMERCIAL_PROPERTY_RETAIL -> Res.string.commercial_property_retail
        LocalizationKeys.COMMERCIAL_PROPERTY_SERVICES -> Res.string.commercial_property_services
        LocalizationKeys.COMMERCIAL_PROPERTY_WAREHOUSES -> Res.string.commercial_property_warehouses
        LocalizationKeys.COMMERCIAL_PROPERTY_OTHER -> Res.string.commercial_property_other
        LocalizationKeys.SORT_NEWEST -> Res.string.sort_newest
        LocalizationKeys.SORT_CHEAPEST -> Res.string.sort_cheapest
        LocalizationKeys.SORT_EXPENSIVE -> Res.string.sort_expensive
        LocalizationKeys.NOTIFICATIONS_TITLE -> Res.string.notifications_title
        LocalizationKeys.NOTIFICATIONS_PARAMS -> Res.string.notifications_params
        LocalizationKeys.NOTIFICATIONS_PERMISSION_MESSAGE -> Res.string.notifications_permission_message
        LocalizationKeys.NOTIFICATIONS_ALLOW -> Res.string.notifications_allow
        LocalizationKeys.REFERRAL_MY_CODE -> Res.string.referral_my_code
        LocalizationKeys.REFERRAL_REMAINING_INVITES -> Res.string.referral_remaining_invites
        LocalizationKeys.REFERRAL_DESCRIPTION -> Res.string.referral_description
        LocalizationKeys.REFERRAL_INPUT_HINT -> Res.string.referral_input_hint
        LocalizationKeys.REFERRAL_ACTIVATE -> Res.string.referral_activate
        LocalizationKeys.REFERRAL_NOTIFICATIONS_AVAILABLE -> Res.string.referral_notifications_available
        LocalizationKeys.MAP_SAVE_AREA_TITLE -> Res.string.map_save_area_title
        LocalizationKeys.MAP_AREA_NAME_HINT -> Res.string.map_area_name_hint
        LocalizationKeys.MAP_AREA_NAME_VALIDATION -> Res.string.map_area_name_validation
        LocalizationKeys.MAP_AREA_NAME_EMPTY_ERROR -> Res.string.map_area_name_empty_error
        LocalizationKeys.MAP_AREA_NAME_LENGTH_ERROR -> Res.string.map_area_name_length_error
        LocalizationKeys.MAP_DRAW_INSTRUCTIONS -> Res.string.map_draw_instructions
        LocalizationKeys.MAP_UNDO -> Res.string.map_undo
        LocalizationKeys.MAP_EXIT -> Res.string.map_exit
        LocalizationKeys.MAP_REFRESH -> Res.string.map_refresh
        LocalizationKeys.MAP_TOO_MANY_OBJECTS -> Res.string.map_too_many_objects
        LocalizationKeys.MAP_SAVED_AREAS -> Res.string.map_saved_areas
        LocalizationKeys.MAP_SAVED_AREAS_EMPTY -> Res.string.map_saved_areas_empty
        LocalizationKeys.LOCATION_TITLE -> Res.string.location_title
        LocalizationKeys.LOCATION_ADDRESS_HINT -> Res.string.location_address_hint
        LocalizationKeys.LOCATION_CITY -> Res.string.location_city
        LocalizationKeys.LOCATION_METRO -> Res.string.location_metro
        LocalizationKeys.LOCATION_DISTRICTS -> Res.string.location_districts
        LocalizationKeys.LOCATION_SAVED_AREAS -> Res.string.location_saved_areas
        LocalizationKeys.LOCATION_SEARCH_STATION -> Res.string.location_search_station
        LocalizationKeys.LOCATION_SEARCH_DISTRICT -> Res.string.location_search_district
        LocalizationKeys.LIST_NO_MORE_FLATS -> Res.string.list_no_more_flats
        LocalizationKeys.LIST_LOAD_MORE -> Res.string.list_load_more
        LocalizationKeys.LIST_PAGE -> Res.string.list_page
        LocalizationKeys.LIST_ROOMS_SUFFIX -> Res.string.list_rooms_suffix
        LocalizationKeys.LIST_COMMERCIAL_ROOMS_SUFFIX -> Res.string.list_commercial_rooms_suffix
        LocalizationKeys.LIST_AREA_FORMAT -> Res.string.list_area_format
        LocalizationKeys.LIST_ROOMS_ONE -> Res.string.list_rooms_one
        LocalizationKeys.LIST_ROOMS_FEW -> Res.string.list_rooms_few
        LocalizationKeys.LIST_ROOMS_MANY -> Res.string.list_rooms_many
        LocalizationKeys.CHIP_FROM_OWNER -> Res.string.chip_from_owner
        LocalizationKeys.CHIP_WITH_PHOTO -> Res.string.chip_with_photo
        LocalizationKeys.CHIP_NEW -> Res.string.chip_new
        LocalizationKeys.SCROLL_TO_TOP -> Res.string.scroll_to_top
        LocalizationKeys.DETAIL_TAB_DESCRIPTION -> Res.string.detail_tab_description
        LocalizationKeys.DETAIL_TAB_CONDITIONS -> Res.string.detail_tab_conditions
        LocalizationKeys.DETAIL_TAB_LOCATION -> Res.string.detail_tab_location
        LocalizationKeys.DETAIL_CONTACT_ON_SOURCE -> Res.string.detail_contact_on_source
        LocalizationKeys.DETAIL_OWNER -> Res.string.detail_owner
        LocalizationKeys.DETAIL_AGENT -> Res.string.detail_agent
        LocalizationKeys.DETAIL_OPEN_SOURCE -> Res.string.detail_open_source
        LocalizationKeys.DETAIL_SHARE_SUBJECT -> Res.string.detail_share_subject
        LocalizationKeys.DETAIL_SHARE -> Res.string.detail_share
        LocalizationKeys.DETAIL_CONTACT_INFO -> Res.string.detail_contact_info
        LocalizationKeys.DETAIL_CONTACT_PERSON -> Res.string.detail_contact_person
        LocalizationKeys.DETAIL_PHONE -> Res.string.detail_phone
        LocalizationKeys.DETAIL_LOCATION -> Res.string.detail_location
        LocalizationKeys.DETAIL_DISTRICT -> Res.string.detail_district
        LocalizationKeys.DETAIL_ADDRESS -> Res.string.detail_address
        LocalizationKeys.DETAIL_METRO -> Res.string.detail_metro
        LocalizationKeys.DETAIL_DESCRIPTION -> Res.string.detail_description
        LocalizationKeys.DETAIL_BUILDING -> Res.string.detail_building
        LocalizationKeys.DETAIL_TOTAL_FLOORS -> Res.string.detail_total_floors
        LocalizationKeys.DETAIL_YEAR_BUILT -> Res.string.detail_year_built
        LocalizationKeys.DETAIL_PARKING -> Res.string.detail_parking
        LocalizationKeys.DETAIL_INFRASTRUCTURE -> Res.string.detail_infrastructure
        LocalizationKeys.DETAIL_ABOUT_COMMERCIAL -> Res.string.detail_about_commercial
        LocalizationKeys.DETAIL_ABOUT_APARTMENT -> Res.string.detail_about_apartment
        LocalizationKeys.DETAIL_ROOMS_COUNT -> Res.string.detail_rooms_count
        LocalizationKeys.DETAIL_TOTAL_AREA -> Res.string.detail_total_area
        LocalizationKeys.DETAIL_LIVING_AREA -> Res.string.detail_living_area
        LocalizationKeys.DETAIL_KITCHEN_AREA -> Res.string.detail_kitchen_area
        LocalizationKeys.DETAIL_FLOOR -> Res.string.detail_floor
        LocalizationKeys.DETAIL_BATHROOM -> Res.string.detail_bathroom
        LocalizationKeys.DETAIL_BALCONY -> Res.string.detail_balcony
        LocalizationKeys.DETAIL_REPAIR -> Res.string.detail_repair
        LocalizationKeys.DETAIL_WINDOWS -> Res.string.detail_windows
        LocalizationKeys.DETAIL_SLEEPING_PLACES -> Res.string.detail_sleeping_places
        LocalizationKeys.DETAIL_AMENITIES -> Res.string.detail_amenities
        LocalizationKeys.DETAIL_IN_APARTMENT -> Res.string.detail_in_apartment
        LocalizationKeys.DETAIL_IN_KITCHEN -> Res.string.detail_in_kitchen
        LocalizationKeys.DETAIL_LIVING_CONDITIONS -> Res.string.detail_living_conditions
        LocalizationKeys.DETAIL_FOR_WHOM -> Res.string.detail_for_whom
        LocalizationKeys.DETAIL_PREPAYMENT -> Res.string.detail_prepayment
        LocalizationKeys.DETAIL_OPEN_IN_MAP -> Res.string.detail_open_in_map
        LocalizationKeys.DETAIL_YEAR_SUFFIX -> Res.string.detail_year_suffix
        LocalizationKeys.SYSTEM_NOTIFICATIONS_TITLE -> Res.string.system_notifications_title
        LocalizationKeys.SYSTEM_NOTIFICATIONS_DESCRIPTION -> Res.string.system_notifications_description
        LocalizationKeys.SYSTEM_OPEN_SETTINGS -> Res.string.system_open_settings
        LocalizationKeys.FORCE_UPDATE_TITLE -> Res.string.force_update_title
        LocalizationKeys.FORCE_UPDATE_DESCRIPTION -> Res.string.force_update_description
        LocalizationKeys.SEARCH_ERROR_TITLE -> Res.string.search_error_title
        LocalizationKeys.PREMIUM_TITLE -> Res.string.premium_title
        LocalizationKeys.PREMIUM_SUBTITLE -> Res.string.premium_subtitle
        LocalizationKeys.PREMIUM_FEATURE_REALTIME -> Res.string.premium_feature_realtime
        LocalizationKeys.PREMIUM_FEATURE_LOCATION -> Res.string.premium_feature_location
        LocalizationKeys.PREMIUM_FEATURE_NO_ADS -> Res.string.premium_feature_no_ads
        LocalizationKeys.PREMIUM_SAVINGS -> Res.string.premium_savings
        LocalizationKeys.PREMIUM_RECOMMENDED -> Res.string.premium_recommended
        LocalizationKeys.PREMIUM_SUBSCRIBE -> Res.string.premium_subscribe
        LocalizationKeys.PREMIUM_RESTORE -> Res.string.premium_restore
        LocalizationKeys.PREMIUM_WATCH_AD -> Res.string.premium_watch_ad
        LocalizationKeys.PREMIUM_UPSELL_BANNER -> Res.string.premium_upsell_banner
        LocalizationKeys.PREMIUM_LOCATION_TOAST -> Res.string.premium_location_toast
        LocalizationKeys.PREMIUM_MENU -> Res.string.premium_menu
        LocalizationKeys.PREMIUM_ACTIVE_TITLE -> Res.string.premium_active_title
        LocalizationKeys.PREMIUM_ACTIVE_SUBTITLE -> Res.string.premium_active_subtitle
        LocalizationKeys.PREMIUM_ACTIVE_UNTIL -> Res.string.premium_active_until
        LocalizationKeys.PREMIUM_ACTIVE_UNLIMITED -> Res.string.premium_active_unlimited
        LocalizationKeys.PREMIUM_SOURCE_STORE -> Res.string.premium_source_store
        LocalizationKeys.PREMIUM_SOURCE_TRIAL -> Res.string.premium_source_trial
        LocalizationKeys.PREMIUM_SOURCE_REWARDED -> Res.string.premium_source_rewarded
        LocalizationKeys.PREMIUM_SOURCE_CACHE -> Res.string.premium_source_cache
        LocalizationKeys.PREMIUM_SOURCE_FALLBACK -> Res.string.premium_source_fallback
        LocalizationKeys.PREMIUM_MANAGE -> Res.string.premium_manage
        LocalizationKeys.PREMIUM_DONE -> Res.string.premium_done
        LocalizationKeys.PREMIUM_PLANS_TITLE -> Res.string.premium_plans_title
        LocalizationKeys.PREMIUM_DEBUG_LABEL -> Res.string.premium_debug_label
        LocalizationKeys.PREMIUM_DEBUG_AUTO -> Res.string.premium_debug_auto
        LocalizationKeys.PREMIUM_DEBUG_ACTIVE -> Res.string.premium_debug_active
        LocalizationKeys.PREMIUM_DEBUG_PURCHASE -> Res.string.premium_debug_purchase
        LocalizationKeys.PREMIUM_PLAN_WEEK -> Res.string.premium_plan_week
        LocalizationKeys.PREMIUM_PLAN_MONTH -> Res.string.premium_plan_month
        LocalizationKeys.PREMIUM_PLAN_QUARTER -> Res.string.premium_plan_quarter
        LocalizationKeys.PREMIUM_ERROR_LOAD_PLANS -> Res.string.premium_error_load_plans
        LocalizationKeys.PREMIUM_ERROR_SUBSCRIPTION_ACTIVATED -> Res.string.premium_error_subscription_activated
        LocalizationKeys.PREMIUM_ERROR_PURCHASES_UNAVAILABLE -> Res.string.premium_error_purchases_unavailable
        LocalizationKeys.PREMIUM_ERROR_RESTORED -> Res.string.premium_error_restored
        LocalizationKeys.PREMIUM_ERROR_NO_ACTIVE_SUBSCRIPTION -> Res.string.premium_error_no_active_subscription
        LocalizationKeys.PREMIUM_ERROR_RESTORE_FAILED -> Res.string.premium_error_restore_failed
        LocalizationKeys.PREMIUM_ERROR_REWARDED_ACTIVATED -> Res.string.premium_error_rewarded_activated
        LocalizationKeys.PREMIUM_ERROR_AD_UNAVAILABLE -> Res.string.premium_error_ad_unavailable
        LocalizationKeys.PREMIUM_ERROR_AD_DISABLED -> Res.string.premium_error_ad_disabled
        LocalizationKeys.PREMIUM_ERROR_GENERIC -> Res.string.premium_error_generic
        LocalizationKeys.PREMIUM_ERROR_SERVICE_UNAVAILABLE -> Res.string.premium_error_service_unavailable
        LocalizationKeys.PREMIUM_ERROR_BILLING_UNAVAILABLE -> Res.string.premium_error_billing_unavailable
        LocalizationKeys.PREMIUM_ERROR_ITEM_UNAVAILABLE -> Res.string.premium_error_item_unavailable
        LocalizationKeys.PREMIUM_ERROR_NETWORK -> Res.string.premium_error_network
        LocalizationKeys.PREMIUM_ERROR_ITEM_ALREADY_OWNED -> Res.string.premium_error_item_already_owned
    }
