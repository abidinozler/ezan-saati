package com.ezansaati.app

import com.ezansaati.app.prayer.FridayWindow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

class FridayWindowTest {

    private val zone = ZoneId.of("Europe/Istanbul")

    /** Her gün öğle 12:30 olsun. */
    private val dhuhr: (LocalDate) -> ZonedDateTime = { it.atTime(LocalTime.of(12, 30)).atZone(zone) }

    private fun at(date: String, time: String) =
        LocalDate.parse(date).atTime(LocalTime.parse(time)).atZone(zone)

    @Test
    fun haftaIcindeSiradakiCuma() {
        // 4 Kasım 2026 çarşamba → 6 Kasım cuma
        val w = FridayWindow.next(at("2026-11-04", "09:00"), dhuhr)!!
        assertEquals(at("2026-11-06", "12:25"), w.start)
        assertEquals(at("2026-11-06", "12:55"), w.end)
    }

    @Test
    fun cumaAraligininIcindeyken() {
        val now = at("2026-11-06", "12:40")
        val w = FridayWindow.next(now, dhuhr)!!
        assertTrue(w.contains(now))
        assertEquals(at("2026-11-06", "12:55"), w.end)
    }

    @Test
    fun cumaBittiktenSonraGelecekHafta() {
        val now = at("2026-11-06", "12:55")
        val w = FridayWindow.next(now, dhuhr)!!
        assertFalse(w.contains(now))
        assertEquals(at("2026-11-13", "12:25"), w.start)
    }

    @Test
    fun konumYoksaAralikYok() {
        assertEquals(null, FridayWindow.next(at("2026-11-04", "09:00")) { null })
    }
}
