package com.ezansaati.app

import com.ezansaati.app.prayer.Prayer
import com.ezansaati.app.prayer.PrayerCalculator
import com.ezansaati.app.prayer.nextPrayer
import com.ezansaati.app.qibla.QiblaCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class CalculatorTest {

    private val istanbulZone = ZoneId.of("Europe/Istanbul")
    private val hhmm = DateTimeFormatter.ofPattern("HH:mm")

    private fun istanbul(date: LocalDate) =
        PrayerCalculator.calculate(date, 41.0082, 28.9784, istanbulZone)

    @Test
    fun istanbulYazGundonumu() {
        val t = istanbul(LocalDate.of(2026, 6, 21))
        // Algoritmanın referans çıktısı; Diyanet takvimiyle ±2 dk içinde olmalı.
        assertEquals("03:24", t[Prayer.IMSAK].format(hhmm))
        assertEquals("05:25", t[Prayer.GUNES].format(hhmm))
        assertEquals("13:11", t[Prayer.OGLE].format(hhmm))
        assertEquals("17:11", t[Prayer.IKINDI].format(hhmm))
        assertEquals("20:47", t[Prayer.AKSAM].format(hhmm))
        assertEquals("22:38", t[Prayer.YATSI].format(hhmm))
    }

    @Test
    fun vakitlerSiraliVeAyniGunde() {
        var date = LocalDate.of(2026, 1, 1)
        repeat(365) {
            val t = istanbul(date)
            val list = Prayer.entries.map { t[it] }
            assertTrue(list.zipWithNext().all { (a, b) -> a.isBefore(b) })
            assertTrue(list.all { it.toLocalDate() == date })
            date = date.plusDays(1)
        }
    }

    @Test
    fun kullaniciInceAyariUygulanir() {
        val date = LocalDate.of(2026, 11, 1)
        val base = istanbul(date)
        val adjusted = PrayerCalculator.calculate(
            date, 41.0082, 28.9784, istanbulZone, mapOf(Prayer.AKSAM to 2),
        )
        assertEquals(base[Prayer.AKSAM].plusMinutes(2), adjusted[Prayer.AKSAM])
        assertEquals(base[Prayer.OGLE], adjusted[Prayer.OGLE])
    }

    @Test
    fun yatsidanSonraSiradakiYarininImsaki() {
        val today = istanbul(LocalDate.of(2026, 11, 1))
        val tomorrow = istanbul(LocalDate.of(2026, 11, 2))
        val lateNight = LocalDate.of(2026, 11, 1).atTime(LocalTime.of(23, 30)).atZone(istanbulZone)
        val next = nextPrayer(lateNight, today, tomorrow)
        assertEquals(Prayer.YATSI, next.current)
        assertEquals(Prayer.IMSAK, next.next)
        assertEquals(tomorrow[Prayer.IMSAK], next.nextTime)

        val noon = today[Prayer.OGLE].plusMinutes(1)
        assertEquals(Prayer.OGLE, nextPrayer(noon, today, tomorrow).current)
        assertEquals(Prayer.IKINDI, nextPrayer(noon, today, tomorrow).next)
    }

    @Test
    fun kibleAcilari() {
        assertEquals(151.6, QiblaCalculator.qiblaDirection(41.0082, 28.9784), 0.1) // İstanbul
        assertEquals(160.2, QiblaCalculator.qiblaDirection(39.9334, 32.8597), 0.1) // Ankara
        assertEquals(0.0, QiblaCalculator.distanceKm(QiblaCalculator.KAABA_LATITUDE, QiblaCalculator.KAABA_LONGITUDE), 0.01)
    }
}
