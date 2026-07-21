package io.flatzen.screens.location

import androidx.compose.runtime.Composable
import flatzen.composeapp.generated.resources.Res
import flatzen.composeapp.generated.resources.city_abu_dhabi
import flatzen.composeapp.generated.resources.city_adana
import flatzen.composeapp.generated.resources.city_ajman
import flatzen.composeapp.generated.resources.city_al_ain
import flatzen.composeapp.generated.resources.city_almaty
import flatzen.composeapp.generated.resources.city_ankara
import flatzen.composeapp.generated.resources.city_antalya
import flatzen.composeapp.generated.resources.city_astana
import flatzen.composeapp.generated.resources.city_austin
import flatzen.composeapp.generated.resources.city_bangkok
import flatzen.composeapp.generated.resources.city_barcelona
import flatzen.composeapp.generated.resources.city_batumi
import flatzen.composeapp.generated.resources.city_berlin
import flatzen.composeapp.generated.resources.city_boston
import flatzen.composeapp.generated.resources.city_brest
import flatzen.composeapp.generated.resources.city_bursa
import flatzen.composeapp.generated.resources.city_busan
import flatzen.composeapp.generated.resources.city_bydgoszcz
import flatzen.composeapp.generated.resources.city_changwon
import flatzen.composeapp.generated.resources.city_cheongju
import flatzen.composeapp.generated.resources.city_chiang_mai
import flatzen.composeapp.generated.resources.city_chicago
import flatzen.composeapp.generated.resources.city_chuncheon
import flatzen.composeapp.generated.resources.city_daegu
import flatzen.composeapp.generated.resources.city_daejeon
import flatzen.composeapp.generated.resources.city_denver
import flatzen.composeapp.generated.resources.city_dubai
import flatzen.composeapp.generated.resources.city_duesseldorf
import flatzen.composeapp.generated.resources.city_frankfurt
import flatzen.composeapp.generated.resources.city_fujairah
import flatzen.composeapp.generated.resources.city_fukuoka
import flatzen.composeapp.generated.resources.city_gaziantep
import flatzen.composeapp.generated.resources.city_gdansk
import flatzen.composeapp.generated.resources.city_gomel
import flatzen.composeapp.generated.resources.city_grodno
import flatzen.composeapp.generated.resources.city_gwangju
import flatzen.composeapp.generated.resources.city_hamburg
import flatzen.composeapp.generated.resources.city_hiroshima
import flatzen.composeapp.generated.resources.city_houston
import flatzen.composeapp.generated.resources.city_hua_hin
import flatzen.composeapp.generated.resources.city_incheon
import flatzen.composeapp.generated.resources.city_istanbul
import flatzen.composeapp.generated.resources.city_izmir
import flatzen.composeapp.generated.resources.city_jeju
import flatzen.composeapp.generated.resources.city_jeonju
import flatzen.composeapp.generated.resources.city_karaganda
import flatzen.composeapp.generated.resources.city_katowice
import flatzen.composeapp.generated.resources.city_kobe
import flatzen.composeapp.generated.resources.city_koeln
import flatzen.composeapp.generated.resources.city_koh_samui
import flatzen.composeapp.generated.resources.city_konya
import flatzen.composeapp.generated.resources.city_krakow
import flatzen.composeapp.generated.resources.city_kutaisi
import flatzen.composeapp.generated.resources.city_kyoto
import flatzen.composeapp.generated.resources.city_leipzig
import flatzen.composeapp.generated.resources.city_lodz
import flatzen.composeapp.generated.resources.city_los_angeles
import flatzen.composeapp.generated.resources.city_lublin
import flatzen.composeapp.generated.resources.city_madrid
import flatzen.composeapp.generated.resources.city_malaga
import flatzen.composeapp.generated.resources.city_miami
import flatzen.composeapp.generated.resources.city_minsk
import flatzen.composeapp.generated.resources.city_mogilev
import flatzen.composeapp.generated.resources.city_muenchen
import flatzen.composeapp.generated.resources.city_nagoya
import flatzen.composeapp.generated.resources.city_new_york
import flatzen.composeapp.generated.resources.city_osaka
import flatzen.composeapp.generated.resources.city_pattaya
import flatzen.composeapp.generated.resources.city_phuket
import flatzen.composeapp.generated.resources.city_poznan
import flatzen.composeapp.generated.resources.city_ras_al_khaimah
import flatzen.composeapp.generated.resources.city_rustavi
import flatzen.composeapp.generated.resources.city_san_francisco
import flatzen.composeapp.generated.resources.city_sapporo
import flatzen.composeapp.generated.resources.city_seattle
import flatzen.composeapp.generated.resources.city_sejong
import flatzen.composeapp.generated.resources.city_sendai
import flatzen.composeapp.generated.resources.city_seoul
import flatzen.composeapp.generated.resources.city_sevilla
import flatzen.composeapp.generated.resources.city_sharjah
import flatzen.composeapp.generated.resources.city_shymkent
import flatzen.composeapp.generated.resources.city_stuttgart
import flatzen.composeapp.generated.resources.city_suwon
import flatzen.composeapp.generated.resources.city_szczecin
import flatzen.composeapp.generated.resources.city_tbilisi
import flatzen.composeapp.generated.resources.city_tokyo
import flatzen.composeapp.generated.resources.city_ulsan
import flatzen.composeapp.generated.resources.city_umm_al_quwain
import flatzen.composeapp.generated.resources.city_valencia
import flatzen.composeapp.generated.resources.city_vitebsk
import flatzen.composeapp.generated.resources.city_warszawa
import flatzen.composeapp.generated.resources.city_wroclaw
import flatzen.composeapp.generated.resources.city_yokohama
import flatzen.composeapp.generated.resources.city_zaragoza
import flatzen.composeapp.generated.resources.country_ae
import flatzen.composeapp.generated.resources.country_by
import flatzen.composeapp.generated.resources.country_de
import flatzen.composeapp.generated.resources.country_es
import flatzen.composeapp.generated.resources.country_ge
import flatzen.composeapp.generated.resources.country_jp
import flatzen.composeapp.generated.resources.country_kr
import flatzen.composeapp.generated.resources.country_kz
import flatzen.composeapp.generated.resources.country_pl
import flatzen.composeapp.generated.resources.country_th
import flatzen.composeapp.generated.resources.country_tr
import flatzen.composeapp.generated.resources.country_us
import io.flatzen.commoncomponents.commonentities.CityCode
import io.flatzen.commoncomponents.commonentities.CountryCode
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

fun CountryCode.localizedNameRes(): StringResource = when (this) {
    CountryCode.BY -> Res.string.country_by
    CountryCode.PL -> Res.string.country_pl
    CountryCode.GE -> Res.string.country_ge
    CountryCode.KZ -> Res.string.country_kz
    CountryCode.ES -> Res.string.country_es
    CountryCode.DE -> Res.string.country_de
    CountryCode.TR -> Res.string.country_tr
    CountryCode.AE -> Res.string.country_ae
    CountryCode.TH -> Res.string.country_th
    CountryCode.US -> Res.string.country_us
    CountryCode.KR -> Res.string.country_kr
    CountryCode.JP -> Res.string.country_jp
}

fun CityCode.localizedNameRes(): StringResource = when (this) {
    CityCode.MINSK -> Res.string.city_minsk
    CityCode.BREST -> Res.string.city_brest
    CityCode.VITEBSK -> Res.string.city_vitebsk
    CityCode.GOMEL -> Res.string.city_gomel
    CityCode.GRODNO -> Res.string.city_grodno
    CityCode.MOGILEV -> Res.string.city_mogilev
    CityCode.WARSZAWA -> Res.string.city_warszawa
    CityCode.KRAKOW -> Res.string.city_krakow
    CityCode.WROCLAW -> Res.string.city_wroclaw
    CityCode.POZNAN -> Res.string.city_poznan
    CityCode.GDANSK -> Res.string.city_gdansk
    CityCode.LODZ -> Res.string.city_lodz
    CityCode.SZCZECIN -> Res.string.city_szczecin
    CityCode.LUBLIN -> Res.string.city_lublin
    CityCode.BYDGOSZCZ -> Res.string.city_bydgoszcz
    CityCode.KATOWICE -> Res.string.city_katowice
    CityCode.TBILISI -> Res.string.city_tbilisi
    CityCode.BATUMI -> Res.string.city_batumi
    CityCode.KUTAISI -> Res.string.city_kutaisi
    CityCode.RUSTAVI -> Res.string.city_rustavi
    CityCode.ALMATY -> Res.string.city_almaty
    CityCode.ASTANA -> Res.string.city_astana
    CityCode.SHYMKENT -> Res.string.city_shymkent
    CityCode.KARAGANDA -> Res.string.city_karaganda
    CityCode.MADRID -> Res.string.city_madrid
    CityCode.BARCELONA -> Res.string.city_barcelona
    CityCode.VALENCIA -> Res.string.city_valencia
    CityCode.SEVILLA -> Res.string.city_sevilla
    CityCode.MALAGA -> Res.string.city_malaga
    CityCode.ZARAGOZA -> Res.string.city_zaragoza
    CityCode.BERLIN -> Res.string.city_berlin
    CityCode.MUENCHEN -> Res.string.city_muenchen
    CityCode.HAMBURG -> Res.string.city_hamburg
    CityCode.KOELN -> Res.string.city_koeln
    CityCode.FRANKFURT -> Res.string.city_frankfurt
    CityCode.STUTTGART -> Res.string.city_stuttgart
    CityCode.DUESSELDORF -> Res.string.city_duesseldorf
    CityCode.LEIPZIG -> Res.string.city_leipzig
    CityCode.ISTANBUL -> Res.string.city_istanbul
    CityCode.ANKARA -> Res.string.city_ankara
    CityCode.IZMIR -> Res.string.city_izmir
    CityCode.ANTALYA -> Res.string.city_antalya
    CityCode.BURSA -> Res.string.city_bursa
    CityCode.ADANA -> Res.string.city_adana
    CityCode.GAZIANTEP -> Res.string.city_gaziantep
    CityCode.KONYA -> Res.string.city_konya
    CityCode.DUBAI -> Res.string.city_dubai
    CityCode.ABU_DHABI -> Res.string.city_abu_dhabi
    CityCode.SHARJAH -> Res.string.city_sharjah
    CityCode.AJMAN -> Res.string.city_ajman
    CityCode.AL_AIN -> Res.string.city_al_ain
    CityCode.RAS_AL_KHAIMAH -> Res.string.city_ras_al_khaimah
    CityCode.FUJAIRAH -> Res.string.city_fujairah
    CityCode.UMM_AL_QUWAIN -> Res.string.city_umm_al_quwain
    CityCode.BANGKOK -> Res.string.city_bangkok
    CityCode.PHUKET -> Res.string.city_phuket
    CityCode.CHIANG_MAI -> Res.string.city_chiang_mai
    CityCode.PATTAYA -> Res.string.city_pattaya
    CityCode.HUA_HIN -> Res.string.city_hua_hin
    CityCode.KOH_SAMUI -> Res.string.city_koh_samui
    CityCode.NEW_YORK -> Res.string.city_new_york
    CityCode.LOS_ANGELES -> Res.string.city_los_angeles
    CityCode.CHICAGO -> Res.string.city_chicago
    CityCode.HOUSTON -> Res.string.city_houston
    CityCode.MIAMI -> Res.string.city_miami
    CityCode.SEATTLE -> Res.string.city_seattle
    CityCode.SAN_FRANCISCO -> Res.string.city_san_francisco
    CityCode.AUSTIN -> Res.string.city_austin
    CityCode.BOSTON -> Res.string.city_boston
    CityCode.DENVER -> Res.string.city_denver
    CityCode.SEOUL -> Res.string.city_seoul
    CityCode.BUSAN -> Res.string.city_busan
    CityCode.DAEGU -> Res.string.city_daegu
    CityCode.INCHEON -> Res.string.city_incheon
    CityCode.GWANGJU -> Res.string.city_gwangju
    CityCode.DAEJEON -> Res.string.city_daejeon
    CityCode.ULSAN -> Res.string.city_ulsan
    CityCode.SEJONG -> Res.string.city_sejong
    CityCode.SUWON -> Res.string.city_suwon
    CityCode.CHANGWON -> Res.string.city_changwon
    CityCode.JEONJU -> Res.string.city_jeonju
    CityCode.CHEONGJU -> Res.string.city_cheongju
    CityCode.CHUNCHEON -> Res.string.city_chuncheon
    CityCode.JEJU -> Res.string.city_jeju
    CityCode.TOKYO -> Res.string.city_tokyo
    CityCode.OSAKA -> Res.string.city_osaka
    CityCode.YOKOHAMA -> Res.string.city_yokohama
    CityCode.NAGOYA -> Res.string.city_nagoya
    CityCode.SAPPORO -> Res.string.city_sapporo
    CityCode.FUKUOKA -> Res.string.city_fukuoka
    CityCode.KYOTO -> Res.string.city_kyoto
    CityCode.KOBE -> Res.string.city_kobe
    CityCode.SENDAI -> Res.string.city_sendai
    CityCode.HIROSHIMA -> Res.string.city_hiroshima
}

@Composable
fun CountryCode.localizedName(): String = stringResource(localizedNameRes())

@Composable
fun CityCode.localizedName(): String = stringResource(localizedNameRes())
