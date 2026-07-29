package com.fairmeter.app.data.location

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Lightweight Nominatim geocoding client.
 * Usage policy: 1 req/sec max, proper User-Agent header required.
 * See https://operations.osmfoundation.org/policies/nominatim/
 */
class NominatimGeocoder {

    private var lastRequestTime = 0L

    data class GeoResult(
        val lat: Double,
        val lng: Double,
        val displayName: String
    )

    suspend fun search(query: String): List<GeoResult> = withContext(Dispatchers.IO) {
        throttle()
        val encoded = URLEncoder.encode(query, "UTF-8")
        val url = URL("https://nominatim.openstreetmap.org/search?q=$encoded&format=json&limit=5")
        val conn = url.openConnection() as HttpURLConnection
        conn.setRequestProperty("User-Agent", "FairMeter/1.0 (fare estimation app)")
        conn.connectTimeout = 5000
        conn.readTimeout = 5000

        try {
            val response = conn.inputStream.bufferedReader().readText()
            val json = JSONArray(response)
            (0 until json.length()).map { i ->
                val obj = json.getJSONObject(i)
                GeoResult(
                    lat = obj.getString("lat").toDouble(),
                    lng = obj.getString("lon").toDouble(),
                    displayName = obj.optString("display_name", "")
                )
            }
        } catch (e: Exception) {
            emptyList()
        } finally {
            conn.disconnect()
        }
    }

    suspend fun reverseGeocode(lat: Double, lng: Double): GeoResult? = withContext(Dispatchers.IO) {
        throttle()
        val url = URL("https://nominatim.openstreetmap.org/reverse?lat=$lat&lon=$lng&format=json")
        val conn = url.openConnection() as HttpURLConnection
        conn.setRequestProperty("User-Agent", "FairMeter/1.0 (fare estimation app)")
        conn.connectTimeout = 5000
        conn.readTimeout = 5000

        try {
            val response = conn.inputStream.bufferedReader().readText()
            val obj = JSONObject(response)
            GeoResult(
                lat = obj.optString("lat", lat.toString()).toDoubleOrNull() ?: lat,
                lng = obj.optString("lon", lng.toString()).toDoubleOrNull() ?: lng,
                displayName = obj.optString("display_name", "")
            )
        } catch (e: Exception) {
            null
        } finally {
            conn.disconnect()
        }
    }

    private suspend fun throttle() {
        val now = System.currentTimeMillis()
        val elapsed = now - lastRequestTime
        if (elapsed < 1000) {
            delay(1000 - elapsed)
        }
        lastRequestTime = System.currentTimeMillis()
    }
}
