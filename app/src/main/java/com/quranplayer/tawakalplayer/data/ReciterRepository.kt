package com.quranplayer.tawakalplayer.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

// Resolves the real audio server URL for each of the three target reciters
// from mp3quran.net's free, key-less API. Server URLs are fetched fresh
// at runtime rather than hardcoded, so this keeps working if servers change.
object ReciterRepository {

    private const val RECITERS_ENDPOINT = "https://www.mp3quran.net/api/v3/reciters?language=eng"
    private val client = OkHttpClient()

    // Name fragments matched against the API's reciter names.
    private val targetReciters = listOf(
        "Basit" to "Abdul Basit 'Abd us-Samad",
        "Khalid Al-Jileel" to "Khalid Al-Jalil",
        "Yasser Al-Dosari" to "Yasser Al-Dosari",
        "Maher" to "Maher Al-Muaiqly"

    )
    suspend fun fetchTargetReciters(): List<Reciter> = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(RECITERS_ENDPOINT).build()
        val response = client.newCall(request).execute()
        val body = response.body?.string() ?: return@withContext emptyList()

        val json = JSONObject(body)
        val recitersArray = json.getJSONArray("reciters")
        val found = mutableListOf<Reciter>()

        for ((matchFragment, displayName) in targetReciters) {
            var matched = false
            for (i in 0 until recitersArray.length()) {
                if (matched) break
                val reciterObj = recitersArray.getJSONObject(i)
                val name = reciterObj.getString("name")

                if (name.contains(matchFragment, ignoreCase = true)) {
                    val moshafArray = reciterObj.getJSONArray("moshaf")
                    if (moshafArray.length() > 0) {
                        var chosenMoshaf = moshafArray.getJSONObject(0)
                        for (m in 0 until moshafArray.length()) {
                            val candidate = moshafArray.getJSONObject(m)
                            if (candidate.getString("name").contains("Murattal", ignoreCase = true)) {
                                chosenMoshaf = candidate
                                break
                            }
                        }
                        val server = chosenMoshaf.getString("server")
                        found.add(
                            Reciter(
                                id = reciterObj.getInt("id"),
                                displayName = displayName,
                                serverUrl = server
                            )
                        )
                        matched = true
                    }
                }
            }
        }
        found
    }

    // Builds the direct mp3 URL for a given surah, e.g.
    // https://server8.mp3quran.net/basit/001.mp3
    fun surahUrl(reciter: Reciter, surahNumber: Int): String {
        val padded = surahNumber.toString().padStart(3, '0')
        return "${reciter.serverUrl}$padded.mp3"
    }
}
