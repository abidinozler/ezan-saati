package com.ezansaati.app

import com.ezansaati.app.calendar.HijriDate
import com.ezansaati.app.calendar.Lunar
import com.ezansaati.app.calendar.MoonPhase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class LunarTest {

    @Test
    fun tutulmaGunuYeniAy() {
        // 8 Nisan 2024 tam güneş tutulması = yeni ay
        val age = Lunar.moonAgeDays(Instant.parse("2024-04-08T18:21:00Z"))
        assertTrue("yaş: $age", age < 0.5 || age > Lunar.SYNODIC_MONTH_DAYS - 0.5)
        assertEquals(MoonPhase.NEW, Lunar.phase(age))
    }

    @Test
    fun dolunayAydinlanmasi() {
        assertEquals(1.0, Lunar.illumination(Lunar.SYNODIC_MONTH_DAYS / 2), 1e-9)
        assertEquals(0.0, Lunar.illumination(0.0), 1e-9)
        assertEquals(MoonPhase.FULL, Lunar.phase(Lunar.SYNODIC_MONTH_DAYS / 2))
    }

    @Test
    fun ramazanBaslangici() {
        assertEquals(HijriDate(1, 9, 1445), Lunar.hijriDate(LocalDate.of(2024, 3, 11)))
    }
}
