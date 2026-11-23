package com.example.data

import android.content.Context
import android.util.Log
import com.example.model.QuranAyah
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

class QuranRepository(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private val cacheDir: File
        get() = File(context.filesDir, "quran_surah_cache").apply { mkdirs() }

    suspend fun getSurahAyahs(
        surahNumber: Int,
        qariId: String
    ): Result<List<QuranAyah>> = withContext(Dispatchers.IO) {
        val safeSurah = surahNumber.coerceIn(1, 114)
        val qariMeta = BengaliIslamicData.qaris.firstOrNull { it.id == qariId }
            ?: BengaliIslamicData.qaris.first()

        // 1. Try disk cache first
        val cacheFile = File(cacheDir, "surah_$safeSurah.json")
        if (cacheFile.exists()) {
            val cachedJson = runCatching { cacheFile.readText() }.getOrNull()
            if (!cachedJson.isNullOrBlank()) {
                val parsed = parseAlQuranCloudJson(cachedJson, safeSurah, qariMeta.cdnBitratePath)
                if (parsed.isNotEmpty()) {
                    return@withContext Result.success(parsed)
                }
            }
        }

        // 2. Fetch full Surah (Arabic Uthmani + Bengali translation + transliteration) from AlQuran.cloud
        try {
            val url = "https://api.alquran.cloud/v1/surah/$safeSurah/editions/quran-uthmani,bn.bengali,en.transliteration"
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IllegalStateException("কুরআন সার্ভার থেকে সাড়া পাওয়া যায়নি (কোড: ${response.code})")
                }
                val body = response.body?.string()
                    ?: throw IllegalStateException("কুরআনের তথ্য খালি পাওয়া গেছে।")
                val parsed = parseAlQuranCloudJson(body, safeSurah, qariMeta.cdnBitratePath)
                if (parsed.isEmpty()) {
                    throw IllegalStateException("সূরার আয়াত পার্স করা যায়নি।")
                }
                runCatching { cacheFile.writeText(body) }
                return@withContext Result.success(parsed)
            }
        } catch (e: Exception) {
            Log.w("QuranRepo", "Failed live Quran fetch for surah $safeSurah: ${e.message}")
            val fallback = BengaliIslamicData.offlineFallbackAyahs[safeSurah]
            if (!fallback.isNullOrEmpty()) {
                return@withContext Result.success(
                    fallback.map { ayah ->
                        ayah.copy(
                            audioUrl = "https://cdn.islamic.network/quran/audio/${qariMeta.cdnBitratePath}/${ayah.globalNumber}.mp3"
                        )
                    }
                )
            }
            Result.failure(e)
        }
    }

    private fun parseAlQuranCloudJson(
        rawJson: String,
        surahNumber: Int,
        cdnBitratePath: String
    ): List<QuranAyah> {
        return try {
            val root = JSONObject(rawJson)
            val dataArray = root.optJSONArray("data") ?: return emptyList()
            if (dataArray.length() < 2) return emptyList()

            val arabicAyahs = dataArray.getJSONObject(0).getJSONArray("ayahs")
            val banglaAyahs = dataArray.getJSONObject(1).getJSONArray("ayahs")
            val translitAyahs = if (dataArray.length() >= 3) {
                dataArray.getJSONObject(2).optJSONArray("ayahs")
            } else null

            val list = ArrayList<QuranAyah>(arabicAyahs.length())
            for (i in 0 until arabicAyahs.length()) {
                val arObj = arabicAyahs.getJSONObject(i)
                val bnObj = banglaAyahs.optJSONObject(i)
                val trObj = translitAyahs?.optJSONObject(i)

                val ayahInSurah = arObj.getInt("numberInSurah")
                val globalNumber = arObj.getInt("number")
                val juz = arObj.optInt("juz", 1)
                val arText = arObj.getString("text")
                val bnText = bnObj?.optString("text", "") ?: ""
                val trText = trObj?.optString("text", "") ?: ""

                list.add(
                    QuranAyah(
                        surahNumber = surahNumber,
                        ayahNumber = ayahInSurah,
                        globalNumber = globalNumber,
                        paraNumber = juz,
                        arabicText = arText,
                        banglaTranslation = bnText,
                        banglaPronunciation = trText,
                        audioUrl = "https://cdn.islamic.network/quran/audio/$cdnBitratePath/$globalNumber.mp3"
                    )
                )
            }
            list
        } catch (e: Exception) {
            Log.e("QuranRepo", "JSON parse failed", e)
            emptyList()
        }
    }
}
