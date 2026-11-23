package com.example.data

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.os.Build
import android.util.Log
import com.example.model.QuranAyah
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit

data class QuranAudioState(
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val currentSurahNumber: Int = 1,
    val currentAyahNumber: Int = 1,
    val currentGlobalAyah: Int = 1,
    val qariId: String = "ar.alafasy",
    val playbackSpeed: Float = 1.0f,
    val repeatAyah: Boolean = false,
    val repeatSurah: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val downloadedSurahs: Set<Int> = emptySet(),
    val isDownloadingSurah: Boolean = false,
    val errorMessage: String? = null
)

class QuranAudioPlayer(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private var mediaPlayer: MediaPlayer? = null
    private var playlist: List<QuranAyah> = emptyList()
    private var currentIndex: Int = -1
    private var progressJob: Job? = null

    private val _state = MutableStateFlow(QuranAudioState())
    val state: StateFlow<QuranAudioState> = _state.asStateFlow()

    private val audioDir: File
        get() = File(context.filesDir, "quran_audio").apply { mkdirs() }

    private val downloadClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    init {
        refreshDownloadedSurahs()
    }

    fun refreshDownloadedSurahs() {
        val qariId = _state.value.qariId
        val dirs = audioDir.listFiles()?.filter { it.isDirectory && it.name.startsWith("${qariId}_surah_") }
            ?.mapNotNull { it.name.substringAfterLast("_").toIntOrNull() }
            ?.toSet() ?: emptySet()
        _state.value = _state.value.copy(downloadedSurahs = dirs)
    }

    fun setQari(qariId: String) {
        _state.value = _state.value.copy(qariId = qariId, errorMessage = null)
        refreshDownloadedSurahs()
    }

    fun setPlaybackSpeed(speed: Float) {
        val clamped = speed.coerceIn(0.75f, 1.5f)
        _state.value = _state.value.copy(playbackSpeed = clamped)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            runCatching {
                mediaPlayer?.let { mp ->
                    if (mp.isPlaying) {
                        mp.playbackParams = PlaybackParams().setSpeed(clamped)
                    }
                }
            }
        }
    }

    fun toggleRepeatAyah() {
        _state.value = _state.value.copy(repeatAyah = !_state.value.repeatAyah)
    }

    fun toggleRepeatSurah() {
        _state.value = _state.value.copy(repeatSurah = !_state.value.repeatSurah)
    }

    fun playSurahFromAyah(ayahs: List<QuranAyah>, startAyahNumber: Int = 1) {
        if (ayahs.isEmpty()) return
        playlist = ayahs
        val idx = ayahs.indexOfFirst { it.ayahNumber == startAyahNumber }.let { if (it >= 0) it else 0 }
        playAtIndex(idx)
    }

    fun togglePlayPause() {
        val mp = mediaPlayer
        if (mp == null) {
            if (playlist.isNotEmpty()) playAtIndex(currentIndex.coerceAtLeast(0))
            return
        }
        try {
            if (mp.isPlaying) {
                mp.pause()
                _state.value = _state.value.copy(isPlaying = false)
            } else {
                mp.start()
                applySpeed(mp)
                _state.value = _state.value.copy(isPlaying = true)
            }
        } catch (e: Exception) {
            Log.e("QuranAudio", "togglePlayPause failed", e)
        }
    }

    fun playNext() {
        if (playlist.isEmpty()) return
        if (currentIndex + 1 < playlist.size) {
            playAtIndex(currentIndex + 1)
        } else if (_state.value.repeatSurah) {
            playAtIndex(0)
        } else {
            stop()
        }
    }

    fun playPrevious() {
        if (playlist.isEmpty()) return
        val prev = (currentIndex - 1).coerceAtLeast(0)
        playAtIndex(prev)
    }

    fun seekTo(ms: Long) {
        runCatching {
            mediaPlayer?.seekTo(ms.toInt())
            _state.value = _state.value.copy(positionMs = ms)
        }
    }

    fun stop() {
        progressJob?.cancel()
        runCatching {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        }
        mediaPlayer = null
        _state.value = _state.value.copy(isPlaying = false, isBuffering = false, positionMs = 0L)
    }

    private fun playAtIndex(index: Int) {
        if (index !in playlist.indices) return
        currentIndex = index
        val ayah = playlist[index]
        val qariMeta = BengaliIslamicData.qaris.firstOrNull { it.id == _state.value.qariId }
            ?: BengaliIslamicData.qaris.first()
        val remoteUrl = "https://cdn.islamic.network/quran/audio/${qariMeta.cdnBitratePath}/${ayah.globalNumber}.mp3"
        val offlineFile = File(audioDir, "${qariMeta.id}_surah_${ayah.surahNumber}/${ayah.globalNumber}.mp3")
        val sourcePath = if (offlineFile.exists() && offlineFile.length() > 1000L) {
            offlineFile.absolutePath
        } else {
            remoteUrl
        }

        progressJob?.cancel()
        runCatching { mediaPlayer?.release() }
        mediaPlayer = null

        _state.value = _state.value.copy(
            isBuffering = true,
            isPlaying = false,
            currentSurahNumber = ayah.surahNumber,
            currentAyahNumber = ayah.ayahNumber,
            currentGlobalAyah = ayah.globalNumber,
            errorMessage = null
        )

        try {
            val mp = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(sourcePath)
                setOnPreparedListener { prepared ->
                    _state.value = _state.value.copy(
                        isBuffering = false,
                        isPlaying = true,
                        durationMs = prepared.duration.toLong().coerceAtLeast(0L)
                    )
                    prepared.start()
                    applySpeed(prepared)
                    startProgressLoop()
                }
                setOnCompletionListener {
                    if (_state.value.repeatAyah) {
                        playAtIndex(currentIndex)
                    } else {
                        playNext()
                    }
                }
                setOnErrorListener { _, what, extra ->
                    Log.e("QuranAudio", "MediaPlayer error what=$what extra=$extra")
                    _state.value = _state.value.copy(
                        isBuffering = false,
                        isPlaying = false,
                        errorMessage = "অডিও প্লে করতে ইন্টারনেট সংযোগ প্রয়োজন (অথবা সূরাটি অফলাইনে সংরক্ষণ করুন)।"
                    )
                    true
                }
                prepareAsync()
            }
            mediaPlayer = mp
        } catch (e: Exception) {
            Log.e("QuranAudio", "Failed preparing audio", e)
            _state.value = _state.value.copy(
                isBuffering = false,
                isPlaying = false,
                errorMessage = "অডিও চালু করা যায়নি: ${e.localizedMessage}"
            )
        }
    }

    private fun applySpeed(mp: MediaPlayer) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            runCatching {
                mp.playbackParams = PlaybackParams().setSpeed(_state.value.playbackSpeed)
            }
        }
    }

    private fun startProgressLoop() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                val mp = mediaPlayer
                if (mp != null && _state.value.isPlaying) {
                    val pos = runCatching { mp.currentPosition.toLong() }.getOrDefault(0L)
                    val dur = runCatching { mp.duration.toLong() }.getOrDefault(_state.value.durationMs)
                    _state.value = _state.value.copy(positionMs = pos, durationMs = dur)
                }
                delay(400L)
            }
        }
    }

    // Download only a single selected Surah for offline playback (never all 114 Surahs at once)
    fun downloadSurahForOffline(surahNumber: Int, ayahs: List<QuranAyah>, onDone: (Result<Unit>) -> Unit) {
        if (ayahs.isEmpty() || _state.value.isDownloadingSurah) return
        val qariMeta = BengaliIslamicData.qaris.firstOrNull { it.id == _state.value.qariId }
            ?: BengaliIslamicData.qaris.first()
        scope.launch {
            _state.value = _state.value.copy(isDownloadingSurah = true, errorMessage = null)
            val result = withContext(Dispatchers.IO) {
                try {
                    val folder = File(audioDir, "${qariMeta.id}_surah_$surahNumber").apply { mkdirs() }
                    for (ayah in ayahs) {
                        val target = File(folder, "${ayah.globalNumber}.mp3")
                        if (target.exists() && target.length() > 1000L) continue
                        val url = "https://cdn.islamic.network/quran/audio/${qariMeta.cdnBitratePath}/${ayah.globalNumber}.mp3"
                        val req = Request.Builder().url(url).build()
                        downloadClient.newCall(req).execute().use { resp ->
                            if (!resp.isSuccessful) error("ডাউনলোড ব্যর্থ (${resp.code})")
                            val bytes = resp.body?.bytes() ?: error("অডিও ডেটা খালি")
                            target.writeBytes(bytes)
                        }
                    }
                    Result.success(Unit)
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }
            _state.value = _state.value.copy(isDownloadingSurah = false)
            refreshDownloadedSurahs()
            onDone(result)
        }
    }

    fun release() {
        stop()
    }
}
