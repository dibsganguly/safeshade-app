package com.safeshade

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AirQualityMapperTest {

    @Test
    fun `maps aqi and pm25 straight through`() {
        val response = AirQualityResponse(current = CurrentAirQuality(european_aqi = 42, pm2_5 = 12.5f))
        val reading = AirQualityMapper.map(response)
        assertEquals(42, reading.europeanAqi)
        assertEquals(12.5f, reading.pm25!!, 0.001f)
    }

    @Test
    fun `nulls in the response stay null in the reading`() {
        val response = AirQualityResponse(current = CurrentAirQuality(european_aqi = null, pm2_5 = null))
        val reading = AirQualityMapper.map(response)
        assertNull(reading.europeanAqi)
        assertNull(reading.pm25)
    }

    @Test
    fun `one field present and the other absent maps independently`() {
        val response = AirQualityResponse(current = CurrentAirQuality(european_aqi = 15, pm2_5 = null))
        val reading = AirQualityMapper.map(response)
        assertEquals(15, reading.europeanAqi)
        assertNull(reading.pm25)
    }
}
