package com.ezansaati.app.prayer

import androidx.annotation.StringRes
import com.ezansaati.app.R

/**
 * Ülkelere göre yaygın vakit hesaplama yöntemleri.
 * Yatsı ya güneşin ufkun altındaki açısıyla ya da akşamdan sonra sabit bir süreyle belirlenir.
 */
enum class CalculationMethod(
    @StringRes val nameRes: Int,
    val fajrAngle: Double,
    val ishaAngle: Double? = null,
    val ishaMinutesAfterMaghrib: Int? = null,
    /** Hesaplanan vakte eklenen ihtiyat (temkin) süreleri, dakika. */
    val temkin: Map<Prayer, Int> = emptyMap(),
) {
    DIYANET(
        R.string.method_diyanet, fajrAngle = 18.0, ishaAngle = 17.0,
        temkin = mapOf(Prayer.GUNES to -7, Prayer.OGLE to 5, Prayer.IKINDI to 4, Prayer.AKSAM to 7),
    ),
    MWL(R.string.method_mwl, fajrAngle = 18.0, ishaAngle = 17.0),
    ISNA(R.string.method_isna, fajrAngle = 15.0, ishaAngle = 15.0),
    EGYPT(R.string.method_egypt, fajrAngle = 19.5, ishaAngle = 17.5),
    KARACHI(R.string.method_karachi, fajrAngle = 18.0, ishaAngle = 18.0),
    UMM_AL_QURA(R.string.method_umm_al_qura, fajrAngle = 18.5, ishaMinutesAfterMaghrib = 90);

    companion object {
        /** Konuma göre o bölgede yaygın kullanılan yöntem. */
        fun suggestFor(latitude: Double, longitude: Double): CalculationMethod = when {
            latitude in 35.8..42.2 && longitude in 25.6..44.9 -> DIYANET
            latitude in 21.5..31.7 && longitude in 24.6..35.0 -> EGYPT
            latitude in 12.0..32.5 && longitude in 34.0..60.0 -> UMM_AL_QURA
            latitude in 5.0..37.5 && longitude in 60.0..92.7 -> KARACHI
            latitude in 14.0..72.0 && longitude in -170.0..-52.0 -> ISNA
            else -> MWL
        }
    }
}

/** İkindi vakti: gölgenin cismin boyunun bir (Şafii) ya da iki (Hanefi) katı olduğu an. */
enum class AsrMethod(@StringRes val nameRes: Int, val shadowFactor: Double) {
    SHAFII(R.string.asr_shafii, 1.0),
    HANAFI(R.string.asr_hanafi, 2.0),
}
