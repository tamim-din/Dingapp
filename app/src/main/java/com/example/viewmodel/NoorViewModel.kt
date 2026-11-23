package com.example.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.BengaliIslamicData
import com.example.data.DeviceUsageSnapshot
import com.example.data.NetworkSafetyChecker
import com.example.data.NoorAiRepository
import com.example.data.NoorReminderHelper
import com.example.data.NoorRepository
import com.example.data.PrayerTimeCalculator
import com.example.data.QuranAudioPlayer
import com.example.data.QuranAudioState
import com.example.data.QuranRepository
import com.example.model.AiConversation
import com.example.model.AiMessage
import com.example.model.CommunityCategory
import com.example.model.CommunityPost
import com.example.model.CommunityReport
import com.example.model.CuratedItem
import com.example.model.DnsCheckResult
import com.example.model.DnsProtectionStatus
import com.example.model.MissionCategory
import com.example.model.MissionHistory
import com.example.model.MissionSchedule
import com.example.model.PrayerLog
import com.example.model.PrayerSchedule
import com.example.model.PrivateJournal
import com.example.model.QuranAyah
import com.example.model.QuranBookmark
import com.example.model.QuranProgress
import com.example.model.QuizResult
import com.example.model.SurahMeta
import com.example.model.TasbihLog
import com.example.model.UiState
import com.example.model.UserMission
import com.example.model.UserProfile
import com.example.ui.theme.ThemeMode
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface AiChatUiState {
    data object Idle : AiChatUiState
    data object Loading : AiChatUiState
    data class Streaming(val text: String) : AiChatUiState
    data class Error(val message: String) : AiChatUiState
}

class SystemBridge(private val appContext: Context) {
    suspend fun checkDns(): DnsCheckResult = NetworkSafetyChecker.checkPrivateDnsAndDoh(appContext)
    fun readScreenTime(): DeviceUsageSnapshot = NoorReminderHelper.readRealScreenTimeMinutes(appContext)
    fun sendNotification(title: String, body: String) =
        NoorReminderHelper.showReminderNotification(appContext, title, body)
}

@OptIn(ExperimentalCoroutinesApi::class)
class NoorViewModel(
    private val repository: NoorRepository,
    private val quranRepository: QuranRepository,
    private val aiRepository: NoorAiRepository,
    private val audioPlayer: QuranAudioPlayer,
    private val systemBridge: SystemBridge,
    val currentUserId: String
) : ViewModel() {

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    private val _isAdmin = MutableStateFlow(false)
    val isAdmin: StateFlow<Boolean> = _isAdmin.asStateFlow()

    val userProfile: StateFlow<UiState<UserProfile?>> =
        repository.observeUserProfile(currentUserId).toUiStateFlow("প্রোফাইল")

    val missions: StateFlow<UiState<List<UserMission>>> =
        repository.observeMissions(currentUserId).toUiStateFlow("মিশন")

    val missionHistory: StateFlow<UiState<List<MissionHistory>>> =
        repository.observeMissionHistory(currentUserId).toUiStateFlow("মিশনের ইতিহাস")

    val prayerLogs: StateFlow<UiState<List<PrayerLog>>> =
        repository.observePrayerLogs(currentUserId).toUiStateFlow("নামাজের লগ")

    val bookmarks: StateFlow<UiState<List<QuranBookmark>>> =
        repository.observeQuranBookmarks(currentUserId).toUiStateFlow("কুরআন বুকমার্ক")

    val quranProgress: StateFlow<UiState<QuranProgress?>> =
        repository.observeQuranProgress(currentUserId).toUiStateFlow("কুরআন অগ্রগতি")

    val conversations: StateFlow<UiState<List<AiConversation>>> =
        repository.observeAiConversations(currentUserId).toUiStateFlow("AI কথোপকথন")

    private val _selectedConversationId = MutableStateFlow<String?>(null)
    val selectedConversationId: StateFlow<String?> = _selectedConversationId.asStateFlow()

    val activeMessages: StateFlow<UiState<List<AiMessage>>> =
        _selectedConversationId.flatMapLatest { convId ->
            if (convId.isNullOrBlank()) {
                flowOf(UiState.Success(emptyList()))
            } else {
                repository.observeAiMessages(currentUserId, convId).mapToUiState("AI বার্তা")
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Success(emptyList()))

    val privateJournals: StateFlow<UiState<List<PrivateJournal>>> =
        repository.observePrivateJournals(currentUserId).toUiStateFlow("ব্যক্তিগত জার্নাল")

    val tasbihLogs: StateFlow<UiState<List<TasbihLog>>> =
        repository.observeTasbihLogs(currentUserId).toUiStateFlow("তাসবিহ লগ")

    val communityPosts: StateFlow<UiState<List<CommunityPost>>> =
        repository.observeCommunityPosts(currentUserId).toUiStateFlow("কমিউনিটি পোস্ট")

    val quizResults: StateFlow<UiState<List<QuizResult>>> =
        repository.observeQuizResults(currentUserId).toUiStateFlow("কুইজ ফলাফল")

    val curatedHadiths: StateFlow<UiState<List<CuratedItem>>> =
        repository.observeCuratedCollection("curated_hadiths", currentUserId).toUiStateFlow("হাদীস")

    val curatedMotivation: StateFlow<UiState<List<CuratedItem>>> =
        repository.observeCuratedCollection("curated_motivation", currentUserId).toUiStateFlow("মোটিভেশন")

    val adminUsers: StateFlow<UiState<List<UserProfile>>> =
        _isAdmin.flatMapLatest { admin ->
            if (!admin) flowOf(UiState.Success(emptyList()))
            else repository.observeAllUsersForAdmin(currentUserId).mapToUiState("ব্যবহারকারী তালিকা")
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Success(emptyList()))

    val adminReports: StateFlow<UiState<List<CommunityReport>>> =
        _isAdmin.flatMapLatest { admin ->
            if (!admin) flowOf(UiState.Success(emptyList()))
            else repository.observeOpenReportsForAdmin(currentUserId).mapToUiState("রিপোর্ট তালিকা")
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Success(emptyList()))

    // Quran Reader & Audio State
    private val _selectedSurah = MutableStateFlow(BengaliIslamicData.allSurahs.first())
    val selectedSurah: StateFlow<SurahMeta> = _selectedSurah.asStateFlow()

    private val _surahAyahs = MutableStateFlow<UiState<List<QuranAyah>>>(UiState.Loading)
    val surahAyahs: StateFlow<UiState<List<QuranAyah>>> = _surahAyahs.asStateFlow()

    val audioState: StateFlow<QuranAudioState> = audioPlayer.state

    // AI Assistant State
    private val _aiChatUiState = MutableStateFlow<AiChatUiState>(AiChatUiState.Idle)
    val aiChatUiState: StateFlow<AiChatUiState> = _aiChatUiState.asStateFlow()

    // DNS & Wellbeing State
    private val _dnsCheckResult = MutableStateFlow(
        DnsCheckResult(
            status = DnsProtectionStatus.INACTIVE,
            activeHostname = null,
            dohReachable = false,
            detailsBn = "সুরক্ষা অবস্থা যাচাই করতে 'সুরক্ষা পরীক্ষা করুন' বাটনে ট্যাপ করুন।"
        )
    )
    val dnsCheckResult: StateFlow<DnsCheckResult> = _dnsCheckResult.asStateFlow()

    private val _isCheckingDns = MutableStateFlow(false)
    val isCheckingDns: StateFlow<Boolean> = _isCheckingDns.asStateFlow()

    private val _usageSnapshot = MutableStateFlow(DeviceUsageSnapshot(false, 0))
    val usageSnapshot: StateFlow<DeviceUsageSnapshot> = _usageSnapshot.asStateFlow()

    // Local Tasbih live counter
    private val _activeDhikrIndex = MutableStateFlow(0)
    val activeDhikrIndex: StateFlow<Int> = _activeDhikrIndex.asStateFlow()

    private val _customDhikrTitle = MutableStateFlow("")
    val customDhikrTitle: StateFlow<String> = _customDhikrTitle.asStateFlow()

    private val _tasbihCount = MutableStateFlow(0L)
    val tasbihCount: StateFlow<Long> = _tasbihCount.asStateFlow()

    private val _tasbihTarget = MutableStateFlow(33L)
    val tasbihTarget: StateFlow<Long> = _tasbihTarget.asStateFlow()

    // Prayer Schedule
    private val _prayerSchedule = MutableStateFlow(
        PrayerTimeCalculator.calculateForCity(PrayerTimeCalculator.findCity("ঢাকা"))
    )
    val prayerSchedule: StateFlow<PrayerSchedule> = _prayerSchedule.asStateFlow()

    init {
        bootstrapSession()
        loadSurah(1)
        runDnsCheck()
        refreshWellbeing()
    }

    private fun bootstrapSession() {
        viewModelScope.launch {
            Firebase.auth.currentUser?.let { fbUser ->
                repository.ensureUserProfileAndDefaults(fbUser)
                _isAdmin.value = repository.checkIsAdmin(fbUser.uid)
            }
        }
    }

    fun clearToast() {
        _toastMessage.value = null
    }

    private fun showMsg(msg: String) {
        _toastMessage.value = msg
    }

    // --- Theme & Profile ---

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            repository.updateThemeMode(mode.name).onFailure {
                showMsg("থিম সংরক্ষণ করা যায়নি: ${it.localizedMessage}")
            }
        }
    }

    fun updateProfile(
        displayName: String,
        username: String,
        country: String,
        locationName: String,
        quranPages: Long,
        tasbihGoal: Long,
        screenGoalMin: Long,
        photoUrl: String? = null
    ) {
        viewModelScope.launch {
            repository.updateProfileFields(
                displayName = displayName,
                username = username,
                country = country,
                locationName = locationName,
                dailyQuranGoalPages = quranPages,
                dailyTasbihGoal = tasbihGoal,
                dailyScreenGoalMinutes = screenGoalMin,
                photoUrl = photoUrl
            ).onSuccess {
                updatePrayerCity(locationName)
                showMsg("প্রোফাইল সফলভাবে হালনাগাদ হয়েছে।")
            }.onFailure {
                showMsg(it.localizedMessage ?: "প্রোফাইল সংরক্ষণ ব্যর্থ হয়েছে।")
            }
        }
    }

    fun updatePrayerCity(cityNameBn: String) {
        _prayerSchedule.value = PrayerTimeCalculator.calculateForCity(PrayerTimeCalculator.findCity(cityNameBn))
    }

    // --- Missions ---

    fun toggleMission(mission: UserMission) {
        viewModelScope.launch {
            repository.toggleMissionComplete(mission).onSuccess {
                if (!mission.completed) {
                    showMsg("মাশাআল্লাহ! '${mission.title}' সম্পন্ন (+${mission.xpReward} XP গেম পয়েন্ট)।")
                }
            }.onFailure {
                showMsg(it.localizedMessage ?: "মিশন আপডেট করা যায়নি।")
            }
        }
    }

    fun addCustomMission(title: String, category: MissionCategory, schedule: MissionSchedule, xp: Long) {
        viewModelScope.launch {
            repository.addCustomMission(title, category, schedule, xp).onSuccess {
                showMsg("নতুন মিশন যুক্ত করা হয়েছে।")
            }.onFailure {
                showMsg(it.localizedMessage ?: "মিশন তৈরি ব্যর্থ হয়েছে।")
            }
        }
    }

    fun resetMissionsFreshStart(list: List<UserMission>) {
        viewModelScope.launch {
            repository.resetMissionsFreshStart(list).onSuccess {
                showMsg("বিসমিল্লাহ! নতুন উদ্যমে আবার শুরু করা হয়েছে।")
            }
        }
    }

    // --- Prayer Tracker ---

    fun togglePrayer(currentLog: PrayerLog?, slot: String, locationName: String) {
        val base = currentLog ?: PrayerLog(
            userId = currentUserId,
            dateKey = PrayerTimeCalculator.todayDateKey(),
            locationName = locationName
        )
        val updated = when (slot) {
            "fajr" -> base.copy(fajr = !base.fajr)
            "dhuhr" -> base.copy(dhuhr = !base.dhuhr)
            "asr" -> base.copy(asr = !base.asr)
            "maghrib" -> base.copy(maghrib = !base.maghrib)
            "isha" -> base.copy(isha = !base.isha)
            "tahajjud" -> base.copy(tahajjud = !base.tahajjud)
            "nafl_inc" -> base.copy(naflCount = (base.naflCount + 1L).coerceAtMost(50L))
            else -> base
        }.copy(locationName = locationName)

        viewModelScope.launch {
            repository.savePrayerLog(updated).onFailure {
                showMsg(it.localizedMessage ?: "নামাজের তথ্য সংরক্ষণ করা যায়নি।")
            }
        }
    }

    // --- Quran Reader & Audio ---

    fun loadSurah(surahNumber: Int) {
        val meta = BengaliIslamicData.allSurahs.firstOrNull { it.number == surahNumber }
            ?: BengaliIslamicData.allSurahs.first()
        _selectedSurah.value = meta
        viewModelScope.launch {
            _surahAyahs.value = UiState.Loading
            val res = quranRepository.getSurahAyahs(meta.number, audioPlayer.state.value.qariId)
            res.onSuccess { ayahs ->
                _surahAyahs.value = UiState.Success(ayahs)
                val firstPara = ayahs.firstOrNull()?.paraNumber ?: meta.startPara
                repository.updateQuranProgress(
                    surahNumber = meta.number,
                    surahNameBn = meta.nameBn,
                    ayahNumber = 1,
                    paraNumber = firstPara,
                    qariId = audioPlayer.state.value.qariId,
                    ayahsIncrement = 0L
                )
            }.onFailure { err ->
                _surahAyahs.value = UiState.Error(
                    err.localizedMessage ?: "সূরা লোড করা যায়নি। ইন্টারনেট সংযোগ পরীক্ষা করুন।"
                )
            }
        }
    }

    fun markAyahRead(ayah: QuranAyah) {
        val meta = _selectedSurah.value
        viewModelScope.launch {
            repository.updateQuranProgress(
                surahNumber = ayah.surahNumber,
                surahNameBn = meta.nameBn,
                ayahNumber = ayah.ayahNumber,
                paraNumber = ayah.paraNumber,
                qariId = audioPlayer.state.value.qariId,
                ayahsIncrement = 1L
            )
            repository.awardXp(5L)
            showMsg("আয়াত ${ayah.ayahNumber} পড়া হিসেবে সংরক্ষিত হয়েছে।")
        }
    }

    fun toggleBookmark(ayah: QuranAyah, existing: QuranBookmark?) {
        val meta = _selectedSurah.value
        viewModelScope.launch {
            repository.toggleQuranBookmark(
                surahNumber = ayah.surahNumber,
                surahNameBn = meta.nameBn,
                ayahNumber = ayah.ayahNumber,
                paraNumber = ayah.paraNumber,
                arabicText = ayah.arabicText,
                banglaText = ayah.banglaTranslation,
                existingBookmark = existing
            ).onSuccess {
                showMsg(if (existing == null) "বুকমার্কে সংরক্ষিত হয়েছে।" else "বুকমার্ক সরানো হয়েছে।")
            }.onFailure {
                showMsg(it.localizedMessage ?: "বুকমার্ক সংরক্ষণ ব্যর্থ।")
            }
        }
    }

    fun playFromAyah(ayahNumber: Int = 1) {
        val state = _surahAyahs.value
        if (state is UiState.Success) {
            audioPlayer.playSurahFromAyah(state.data, ayahNumber)
        }
    }

    fun toggleAudioPlayPause() = audioPlayer.togglePlayPause()
    fun playNextAyah() = audioPlayer.playNext()
    fun playPreviousAyah() = audioPlayer.playPrevious()
    fun setAudioSpeed(speed: Float) = audioPlayer.setPlaybackSpeed(speed)
    fun toggleRepeatAyah() = audioPlayer.toggleRepeatAyah()
    fun toggleRepeatSurah() = audioPlayer.toggleRepeatSurah()
    fun setQari(qariId: String) {
        audioPlayer.setQari(qariId)
        loadSurah(_selectedSurah.value.number)
    }

    fun downloadCurrentSurahAudio() {
        val state = _surahAyahs.value
        if (state is UiState.Success) {
            audioPlayer.downloadSurahForOffline(_selectedSurah.value.number, state.data) { res ->
                res.onSuccess {
                    showMsg("সূরা '${_selectedSurah.value.nameBn}' অফলাইন শোনার জন্য সংরক্ষিত হয়েছে।")
                }.onFailure {
                    showMsg(it.localizedMessage ?: "অডিও ডাউনলোড ব্যর্থ হয়েছে।")
                }
            }
        }
    }

    // --- AI Assistant ---

    fun selectConversation(convId: String?) {
        _selectedConversationId.value = convId
        _aiChatUiState.value = AiChatUiState.Idle
    }

    fun sendAiPrompt(promptText: String, currentHistory: List<AiMessage>) {
        val clean = promptText.trim()
        if (clean.isEmpty()) return
        viewModelScope.launch {
            _aiChatUiState.value = AiChatUiState.Loading
            var convId = _selectedConversationId.value
            if (convId.isNullOrBlank()) {
                val created = repository.createAiConversation(clean.take(45))
                if (created.isFailure) {
                    _aiChatUiState.value = AiChatUiState.Error("নতুন চ্যাট তৈরি করা যায়নি।")
                    return@launch
                }
                convId = created.getOrThrow()
                _selectedConversationId.value = convId
            }

            repository.saveAiMessage(convId, "USER", clean)
            val historyPairs = currentHistory.map { it.role to it.content }
            val finalReply = StringBuilder()
            try {
                aiRepository.streamIslamicReply(historyPairs, clean).collect { partial ->
                    finalReply.clear()
                    finalReply.append(partial)
                    _aiChatUiState.value = AiChatUiState.Streaming(partial)
                }
                if (finalReply.isNotBlank()) {
                    repository.saveAiMessage(convId, "ASSISTANT", finalReply.toString())
                    _aiChatUiState.value = AiChatUiState.Idle
                } else {
                    _aiChatUiState.value = AiChatUiState.Error("AI থেকে কোনো উত্তর পাওয়া যায়নি।")
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("NoorAI", "AI inference failed", e)
                _aiChatUiState.value = AiChatUiState.Error(
                    e.localizedMessage ?: "AI সহকারীর সাথে সংযোগ করতে সমস্যা হয়েছে।"
                )
            }
        }
    }

    fun deleteConversation(convId: String) {
        viewModelScope.launch {
            repository.deleteAiConversation(convId).onSuccess {
                if (_selectedConversationId.value == convId) _selectedConversationId.value = null
                showMsg("কথোপকথন মুছে ফেলা হয়েছে।")
            }
        }
    }

    // --- Private Journal ---

    fun setJournalPin(pin: String) {
        viewModelScope.launch {
            repository.updateJournalPin(pin).onSuccess {
                showMsg(if (pin.isBlank()) "জার্নাল PIN লক বন্ধ করা হয়েছে।" else "জার্নাল PIN লক সক্রিয় হয়েছে।")
            }
        }
    }

    fun addJournalEntry(
        title: String,
        content: String,
        gratitude: String,
        habitReflection: String,
        mood: String
    ) {
        viewModelScope.launch {
            repository.addPrivateJournal(title, content, gratitude, habitReflection, mood)
                .onSuccess { showMsg("ব্যক্তিগত জার্নাল সংরক্ষিত হয়েছে।") }
                .onFailure { showMsg(it.localizedMessage ?: "সংরক্ষণ করা যায়নি।") }
        }
    }

    fun deleteJournalEntry(journalId: String) {
        viewModelScope.launch {
            repository.deletePrivateJournal(journalId).onSuccess {
                showMsg("জার্নাল নোট মুছে ফেলা হয়েছে।")
            }
        }
    }

    // --- Tasbih ---

    fun selectDhikrPreset(index: Int) {
        val preset = BengaliIslamicData.dhikrPresets.getOrNull(index) ?: return
        _activeDhikrIndex.value = index
        _customDhikrTitle.value = ""
        _tasbihCount.value = 0L
        _tasbihTarget.value = preset.third
    }

    fun setCustomDhikr(title: String, target: Long) {
        _activeDhikrIndex.value = -1
        _customDhikrTitle.value = title.trim()
        _tasbihCount.value = 0L
        _tasbihTarget.value = target.coerceIn(1L, 10000L)
    }

    fun incrementTasbih() {
        _tasbihCount.value += 1L
    }

    fun resetTasbih() {
        _tasbihCount.value = 0L
    }

    fun saveTasbihToCloud() {
        val idx = _activeDhikrIndex.value
        val (title, arabic) = if (idx >= 0 && idx < BengaliIslamicData.dhikrPresets.size) {
            val p = BengaliIslamicData.dhikrPresets[idx]
            p.first to p.second
        } else {
            _customDhikrTitle.value.ifBlank { "ব্যক্তিগত যিকির" } to ""
        }
        val count = _tasbihCount.value
        val target = _tasbihTarget.value
        viewModelScope.launch {
            repository.saveTasbihSession(title, arabic, count, target).onSuccess {
                if (count >= 10L) repository.awardXp(10L)
                showMsg("তাসবিহ আমল ডেটাবেজে সংরক্ষিত হয়েছে।")
            }.onFailure {
                showMsg(it.localizedMessage ?: "সংরক্ষণ ব্যর্থ হয়েছে।")
            }
        }
    }

    // --- Community & Quizzes ---

    fun createCommunityPost(alias: String, category: CommunityCategory, content: String) {
        viewModelScope.launch {
            repository.createCommunityPost(alias, category, content)
                .onSuccess { showMsg("পোস্ট প্রকাশিত হয়েছে।") }
                .onFailure { showMsg(it.localizedMessage ?: "পোস্ট করা যায়নি।") }
        }
    }

    fun likePost(postId: String) {
        viewModelScope.launch { repository.likeCommunityPost(postId) }
    }

    fun reportPost(postId: String, reason: String) {
        viewModelScope.launch {
            repository.reportCommunityPost(postId, reason)
                .onSuccess { showMsg("রিপোর্ট জমা দেওয়া হয়েছে। মডারেটররা এটি যাচাই করবেন।") }
        }
    }

    fun blockAuthor(authorId: String) {
        viewModelScope.launch {
            repository.blockCommunityAuthor(authorId)
                .onSuccess { showMsg("ব্যবহারকারীকে ব্লক করা হয়েছে।") }
        }
    }

    fun submitQuizResult(quizTitle: String, score: Int, total: Int) {
        val xp = (score * 15L).coerceAtLeast(10L)
        viewModelScope.launch {
            repository.saveQuizResult(quizTitle, score, total, xp)
                .onSuccess { showMsg("কুইজ সম্পন্ন! আপনি +$xp XP অর্জন করেছেন।") }
        }
    }

    // --- DNS Safety, Wellbeing & Notifications ---

    fun runDnsCheck() {
        viewModelScope.launch {
            _isCheckingDns.value = true
            _dnsCheckResult.value = systemBridge.checkDns()
            _isCheckingDns.value = false
        }
    }

    fun refreshWellbeing() {
        _usageSnapshot.value = systemBridge.readScreenTime()
    }

    fun triggerTestNotification(title: String, body: String) {
        systemBridge.sendNotification(title, body)
        showMsg("নোটিফিকেশন পাঠানো হয়েছে।")
    }

    // --- Admin Actions ---

    fun adminAddCuratedItem(collection: String, title: String, body: String, ref: String, cat: String) {
        viewModelScope.launch {
            repository.adminSaveCuratedItem(collection, title, body, ref, cat)
                .onSuccess { showMsg("কন্টেন্ট সফলভাবে যুক্ত হয়েছে।") }
                .onFailure { showMsg(it.localizedMessage ?: "অ্যাডমিন অনুমোদন ব্যর্থ।") }
        }
    }

    fun adminDeleteCuratedItem(collection: String, docId: String) {
        viewModelScope.launch {
            repository.adminDeleteCuratedItem(collection, docId)
                .onSuccess { showMsg("আইটেম মুছে ফেলা হয়েছে।") }
        }
    }

    fun adminToggleUserStatus(targetUid: String, currentStatus: String) {
        val next = if (currentStatus == "SUSPENDED") "ACTIVE" else "SUSPENDED"
        viewModelScope.launch {
            repository.adminSetUserStatus(targetUid, next)
                .onSuccess { showMsg("ব্যবহারকারীর অবস্থা পরিবর্তিত হয়েছে: $next") }
        }
    }

    fun adminResolveReport(reportId: String, postId: String, hidePost: Boolean) {
        viewModelScope.launch {
            repository.adminResolveReport(reportId, postId, hidePost)
                .onSuccess { showMsg("রিপোর্ট নিষ্পত্তি করা হয়েছে।") }
        }
    }

    fun deleteAccount(onDeleted: () -> Unit) {
        viewModelScope.launch {
            repository.deleteMyAccountData()
                .onSuccess {
                    showMsg("আপনার অ্যাকাউন্ট এবং ডেটা মুছে ফেলা হয়েছে।")
                    onDeleted()
                }
                .onFailure {
                    showMsg(it.localizedMessage ?: "অ্যাকাউন্ট মুছতে সমস্যা হয়েছে। পুনরায় লগইন করে চেষ্টা করুন।")
                }
        }
    }

    override fun onCleared() {
        super.onCleared()
        audioPlayer.release()
    }

    // --- Helpers for Two-Tier Flow Unwrapping ---

    private fun <T> Flow<T>.mapToUiState(label: String): Flow<UiState<T>> =
        this.map<T, UiState<T>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val root = firestoreError ?: error
                Log.w("NoorVM", "Error observing $label", root)
                emit(UiState.Error("$label লোড করতে সমস্যা হয়েছে: ${root.localizedMessage ?: "অজানা ত্রুটি"}"))
            }

    private fun <T> Flow<T>.toUiStateFlow(label: String): StateFlow<UiState<T>> =
        mapToUiState(label).stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UiState.Loading
        )
}
