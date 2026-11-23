package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.NoorDestination
import com.example.NoorGlassBottomBar
import com.example.NoorGlassNavigationRail
import com.example.NoorGlassTopHeader
import com.example.data.BengaliIslamicData
import com.example.data.DeviceUsageSnapshot
import com.example.data.NetworkSafetyChecker
import com.example.data.NoorReminderHelper
import com.example.data.PrayerTimeCalculator
import com.example.data.QuranAudioPlayer
import com.example.data.QuranRepository
import com.example.model.DnsCheckResult
import com.example.model.DnsProtectionStatus
import com.example.model.MissionCategory
import com.example.model.MissionSchedule
import com.example.model.PrayerLog
import com.example.model.PrivateJournal
import com.example.model.QuranAyah
import com.example.model.QuranBookmark
import com.example.model.QuranProgress
import com.example.model.QuizResult
import com.example.model.TasbihLog
import com.example.model.UiState
import com.example.model.UserMission
import com.example.model.UserProfile
import com.example.ui.components.IslamicBackdrop
import com.example.ui.theme.LocalNoorGlassColors
import com.example.ui.theme.ThemeMode
import com.example.viewmodel.AiChatUiState
import kotlinx.coroutines.launch

@Composable
internal fun NoorStandaloneShell(
    themeMode: ThemeMode,
    onThemeChanged: (ThemeMode) -> Unit
) {
    val context = LocalContext.current
    val appContext = context.applicationContext
    val scope = rememberCoroutineScope()
    val noor = LocalNoorGlassColors.current
    val snackbarHostState = remember { SnackbarHostState() }

    val quranRepo = remember { QuranRepository(appContext) }
    val audioPlayer = remember { QuranAudioPlayer(appContext, scope) }
    DisposableEffect(audioPlayer) {
        onDispose { audioPlayer.release() }
    }

    val audioState by audioPlayer.state.collectAsStateWithLifecycle()

    var currentDestination by rememberSaveable { mutableStateOf(NoorDestination.HOME) }
    var knowledgeInitialTab by rememberSaveable { mutableIntStateOf(0) }
    var knowledgeJumpNonce by rememberSaveable { mutableIntStateOf(0) }
    var safetyInitialTab by rememberSaveable { mutableIntStateOf(0) }
    var safetyJumpNonce by rememberSaveable { mutableIntStateOf(0) }

    var profile by remember {
        mutableStateOf(
            UserProfile(
                id = "local_user",
                userId = "local_user",
                displayName = "প্রিয় ব্যবহারকারী",
                username = "noor_user",
                locationName = "ঢাকা",
                themeMode = themeMode.name
            )
        )
    }

    var missions by remember {
        mutableStateOf(
            listOf(
                UserMission(id = "m1", title = "ফজরের নামাজ আদায় করা", category = MissionCategory.IBADAT.name, schedule = MissionSchedule.DAILY.name, xpReward = 30L),
                UserMission(id = "m2", title = "কুরআন তিলাওয়াত (কমপক্ষে ৫ আয়াত)", category = MissionCategory.IBADAT.name, schedule = MissionSchedule.DAILY.name, xpReward = 25L),
                UserMission(id = "m3", title = "সকাল-সন্ধ্যা যিকির ও তাসবিহ", category = MissionCategory.IBADAT.name, schedule = MissionSchedule.DAILY.name, xpReward = 20L),
                UserMission(id = "m4", title = "পিতা-মাতা ও মানুষের সাথে সদ্ব্যবহার", category = MissionCategory.CHARACTER.name, schedule = MissionSchedule.DAILY.name, xpReward = 20L),
                UserMission(id = "m5", title = "অপ্রয়োজনীয় স্ক্রিন টাইম ও গুনাহ থেকে বিরত থাকা", category = MissionCategory.SELF_CONTROL.name, schedule = MissionSchedule.DAILY.name, xpReward = 30L)
            )
        )
    }

    var prayerLog by remember {
        mutableStateOf(
            PrayerLog(
                id = "today",
                userId = "local_user",
                dateKey = PrayerTimeCalculator.todayDateKey(),
                locationName = "ঢাকা"
            )
        )
    }
    var prayerSchedule by remember {
        mutableStateOf(PrayerTimeCalculator.calculateForCity(PrayerTimeCalculator.findCity("ঢাকা")))
    }

    var selectedSurah by remember { mutableStateOf(BengaliIslamicData.allSurahs.first()) }
    var surahAyahsState by remember { mutableStateOf<UiState<List<QuranAyah>>>(UiState.Loading) }
    var bookmarks by remember { mutableStateOf<List<QuranBookmark>>(emptyList()) }
    var quranProgress by remember { mutableStateOf(QuranProgress(id = "local", userId = "local_user")) }

    var activeDhikrIndex by remember { mutableIntStateOf(0) }
    var customDhikrTitle by remember { mutableStateOf("") }
    var tasbihCount by remember { mutableLongStateOf(0L) }
    var tasbihTarget by remember { mutableLongStateOf(33L) }
    var tasbihLogs by remember { mutableStateOf<List<TasbihLog>>(emptyList()) }
    var quizResults by remember { mutableStateOf<List<QuizResult>>(emptyList()) }
    var localJournals by remember { mutableStateOf<List<PrivateJournal>>(emptyList()) }

    var dnsResult by remember {
        mutableStateOf(
            DnsCheckResult(
                status = DnsProtectionStatus.INACTIVE,
                activeHostname = null,
                dohReachable = false,
                detailsBn = "সুরক্ষা অবস্থা যাচাই করতে 'সুরক্ষা পরীক্ষা করুন' বাটনে ট্যাপ করুন।"
            )
        )
    }
    var isCheckingDns by remember { mutableStateOf(false) }
    var usageSnapshot by remember { mutableStateOf(DeviceUsageSnapshot(false, 0)) }

    fun notifyUser(msg: String) {
        scope.launch { snackbarHostState.showSnackbar(msg) }
    }

    fun loadSurahLocal(surahNumber: Int) {
        val meta = BengaliIslamicData.allSurahs.firstOrNull { it.number == surahNumber }
            ?: BengaliIslamicData.allSurahs.first()
        selectedSurah = meta
        scope.launch {
            surahAyahsState = UiState.Loading
            quranRepo.getSurahAyahs(meta.number, audioState.qariId).onSuccess { ayahs ->
                surahAyahsState = UiState.Success(ayahs)
                quranProgress = quranProgress.copy(
                    lastSurahNumber = meta.number.toLong(),
                    lastSurahNameBn = meta.nameBn,
                    lastParaNumber = (ayahs.firstOrNull()?.paraNumber ?: meta.startPara).toLong()
                )
            }.onFailure { err ->
                surahAyahsState = UiState.Error(
                    err.localizedMessage ?: "সূরা লোড করা যায়নি। ইন্টারনেট সংযোগ পরীক্ষা করুন।"
                )
            }
        }
    }

    LaunchedEffect(Unit) {
        loadSurahLocal(1)
        isCheckingDns = true
        dnsResult = NetworkSafetyChecker.checkPrivateDnsAndDoh(appContext)
        isCheckingDns = false
        usageSnapshot = NoorReminderHelper.readRealScreenTimeMinutes(appContext)
    }

    BackHandler(enabled = currentDestination != NoorDestination.HOME) {
        currentDestination = NoorDestination.HOME
    }

    IslamicBackdrop(showMosqueHeaderArt = currentDestination == NoorDestination.HOME) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val useNavRail = maxWidth >= 600.dp

            Scaffold(
                containerColor = Color.Transparent,
                contentWindowInsets = WindowInsets.safeDrawing,
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                topBar = {
                    Column {
                        NoorGlassTopHeader(
                            currentTitle = currentDestination.labelBn,
                            themeMode = themeMode,
                            isAdmin = false,
                            onCycleTheme = {
                                val next = when (themeMode) {
                                    ThemeMode.AUTO -> ThemeMode.DAY
                                    ThemeMode.DAY -> ThemeMode.NIGHT
                                    ThemeMode.NIGHT -> ThemeMode.AUTO
                                }
                                onThemeChanged(next)
                            },
                            onOpenAdmin = {}
                        )
                        Surface(
                            color = noor.accentGold.copy(alpha = 0.16f),
                            border = BorderStroke(1.dp, noor.accentGold.copy(alpha = 0.45f)),
                            shape = RoundedCornerShape(0.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "অফলাইন/লোকাল মোড সক্রিয় — কুরআন, অডিও, নামাজ, তাসবিহ ও DNS পরীক্ষা চালু আছে। (ক্লাউড সিঙ্ক ও AI-এর জন্য Firebase কনফিগার প্রয়োজন)",
                                style = MaterialTheme.typography.labelSmall,
                                color = noor.textPrimary,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                            )
                        }
                    }
                },
                bottomBar = {
                    if (!useNavRail) {
                        NoorGlassBottomBar(
                            currentDestination = currentDestination,
                            onSelect = { dest ->
                                if (dest == NoorDestination.KNOWLEDGE && currentDestination != NoorDestination.KNOWLEDGE) {
                                    knowledgeInitialTab = 0
                                }
                                if (dest == NoorDestination.SAFETY && currentDestination != NoorDestination.SAFETY) {
                                    safetyInitialTab = 0
                                }
                                currentDestination = dest
                            }
                        )
                    }
                }
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    if (useNavRail) {
                        NoorGlassNavigationRail(
                            currentDestination = currentDestination,
                            onSelect = { dest ->
                                if (dest == NoorDestination.KNOWLEDGE && currentDestination != NoorDestination.KNOWLEDGE) {
                                    knowledgeInitialTab = 0
                                }
                                if (dest == NoorDestination.SAFETY && currentDestination != NoorDestination.SAFETY) {
                                    safetyInitialTab = 0
                                }
                                currentDestination = dest
                            }
                        )
                    }

                    Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        when (currentDestination) {
                            NoorDestination.HOME -> {
                                HomeScreen(
                                    profileState = UiState.Success(profile),
                                    missionsState = UiState.Success(missions),
                                    historyState = UiState.Success(emptyList()),
                                    prayerSchedule = prayerSchedule,
                                    dnsResult = dnsResult,
                                    onToggleMission = { target ->
                                        val nextDone = !target.completed
                                        missions = missions.map {
                                            if (it.id == target.id) it.copy(completed = nextDone) else it
                                        }
                                        if (nextDone) {
                                            profile = profile.copy(xp = profile.xp + target.xpReward)
                                            notifyUser("মাশাআল্লাহ! '${target.title}' সম্পন্ন।")
                                        }
                                    },
                                    onNavigateQuran = { currentDestination = NoorDestination.QURAN },
                                    onNavigateAi = { currentDestination = NoorDestination.AI },
                                    onNavigateTasbih = {
                                        knowledgeInitialTab = 1
                                        knowledgeJumpNonce += 1
                                        currentDestination = NoorDestination.KNOWLEDGE
                                    },
                                    onNavigateSafety = {
                                        safetyInitialTab = 0
                                        safetyJumpNonce += 1
                                        currentDestination = NoorDestination.SAFETY
                                    },
                                    onNavigateMissions = { currentDestination = NoorDestination.MISSIONS }
                                )
                            }
                            NoorDestination.QURAN -> {
                                QuranScreen(
                                    selectedSurah = selectedSurah,
                                    surahAyahsState = surahAyahsState,
                                    bookmarksState = UiState.Success(bookmarks),
                                    quranProgressState = UiState.Success(quranProgress),
                                    audioState = audioState,
                                    onSelectSurah = { loadSurahLocal(it) },
                                    onMarkAyahRead = { ayah ->
                                        quranProgress = quranProgress.copy(
                                            lastSurahNumber = ayah.surahNumber.toLong(),
                                            lastSurahNameBn = selectedSurah.nameBn,
                                            lastAyahNumber = ayah.ayahNumber.toLong(),
                                            lastParaNumber = ayah.paraNumber.toLong(),
                                            totalAyahsRead = quranProgress.totalAyahsRead + 1L,
                                            dailyAyahsRead = quranProgress.dailyAyahsRead + 1L
                                        )
                                        notifyUser("আয়াত ${ayah.ayahNumber} পড়া হিসেবে চিহ্নিত হয়েছে।")
                                    },
                                    onToggleBookmark = { ayah, existing ->
                                        bookmarks = if (existing != null) {
                                            bookmarks.filterNot { it.id == existing.id }
                                        } else {
                                            bookmarks + QuranBookmark(
                                                id = "${ayah.surahNumber}_${ayah.ayahNumber}",
                                                userId = "local_user",
                                                surahNumber = ayah.surahNumber.toLong(),
                                                surahNameBn = selectedSurah.nameBn,
                                                ayahNumber = ayah.ayahNumber.toLong(),
                                                paraNumber = ayah.paraNumber.toLong(),
                                                arabicText = ayah.arabicText,
                                                banglaText = ayah.banglaTranslation
                                            )
                                        }
                                    },
                                    onPlayFromAyah = { ayahNum ->
                                        val list = (surahAyahsState as? UiState.Success)?.data
                                        if (list != null) audioPlayer.playSurahFromAyah(list, ayahNum)
                                    },
                                    onTogglePlayPause = audioPlayer::togglePlayPause,
                                    onPlayNext = audioPlayer::playNext,
                                    onPlayPrevious = audioPlayer::playPrevious,
                                    onSetSpeed = audioPlayer::setPlaybackSpeed,
                                    onToggleRepeatAyah = audioPlayer::toggleRepeatAyah,
                                    onToggleRepeatSurah = audioPlayer::toggleRepeatSurah,
                                    onSelectQari = { qari ->
                                        audioPlayer.setQari(qari)
                                        loadSurahLocal(selectedSurah.number)
                                    },
                                    onDownloadSurahAudio = {
                                        val list = (surahAyahsState as? UiState.Success)?.data
                                        if (list != null) {
                                            audioPlayer.downloadSurahForOffline(selectedSurah.number, list) { res ->
                                                res.onSuccess {
                                                    notifyUser("সূরা '${selectedSurah.nameBn}' অফলাইন শোনার জন্য সংরক্ষিত হয়েছে।")
                                                }.onFailure {
                                                    notifyUser(it.localizedMessage ?: "ডাউনলোড ব্যর্থ হয়েছে।")
                                                }
                                            }
                                        }
                                    }
                                )
                            }
                            NoorDestination.MISSIONS -> {
                                MissionsAndPrayerScreen(
                                    profileState = UiState.Success(profile),
                                    missionsState = UiState.Success(missions),
                                    historyState = UiState.Success(emptyList()),
                                    prayerLogsState = UiState.Success(listOf(prayerLog)),
                                    prayerSchedule = prayerSchedule,
                                    onToggleMission = { target ->
                                        val nextDone = !target.completed
                                        missions = missions.map {
                                            if (it.id == target.id) it.copy(completed = nextDone) else it
                                        }
                                        if (nextDone) profile = profile.copy(xp = profile.xp + target.xpReward)
                                    },
                                    onAddCustomMission = { title, cat, sched, xp ->
                                        val id = "custom_${System.currentTimeMillis()}"
                                        missions = missions + UserMission(
                                            id = id,
                                            title = title,
                                            category = cat.name,
                                            schedule = sched.name,
                                            xpReward = xp,
                                            custom = true
                                        )
                                        notifyUser("নতুন মিশন যুক্ত হয়েছে।")
                                    },
                                    onResetFreshStart = {
                                        missions = missions.map { it.copy(completed = false) }
                                        notifyUser("বিসমিল্লাহ! নতুন উদ্যমে শুরু করা হয়েছে।")
                                    },
                                    onTogglePrayer = { _, slot, loc ->
                                        prayerLog = when (slot) {
                                            "fajr" -> prayerLog.copy(fajr = !prayerLog.fajr)
                                            "dhuhr" -> prayerLog.copy(dhuhr = !prayerLog.dhuhr)
                                            "asr" -> prayerLog.copy(asr = !prayerLog.asr)
                                            "maghrib" -> prayerLog.copy(maghrib = !prayerLog.maghrib)
                                            "isha" -> prayerLog.copy(isha = !prayerLog.isha)
                                            "tahajjud" -> prayerLog.copy(tahajjud = !prayerLog.tahajjud)
                                            "nafl_inc" -> prayerLog.copy(naflCount = (prayerLog.naflCount + 1L).coerceAtMost(50L))
                                            else -> prayerLog
                                        }.copy(locationName = loc)
                                    },
                                    onSelectCity = { city ->
                                        prayerSchedule = PrayerTimeCalculator.calculateForCity(PrayerTimeCalculator.findCity(city))
                                        profile = profile.copy(locationName = city)
                                    }
                                )
                            }
                            NoorDestination.AI -> {
                                AiAndJournalScreen(
                                    profileState = UiState.Success(profile),
                                    conversationsState = UiState.Success(emptyList()),
                                    selectedConversationId = null,
                                    activeMessagesState = UiState.Success(emptyList()),
                                    aiChatUiState = AiChatUiState.Error(
                                        "ক্লাউড AI সহকারী চালু করতে Firebase কনফিগারেশন (google-services.json) প্রয়োজন। অন্য সব অফলাইন/লোকাল সুবিধা এখনই ব্যবহার করা যাবে।"
                                    ),
                                    journalsState = UiState.Success(localJournals),
                                    onSelectConversation = {},
                                    onSendAiPrompt = { _, _ ->
                                        notifyUser("এই বিল্ডে ক্লাউড AI যুক্ত নেই (google-services.json প্রয়োজন)।")
                                    },
                                    onDeleteConversation = {},
                                    onSetJournalPin = { _ ->
                                        notifyUser("লোকাল মোডে PIN স্থায়ীভাবে সংরক্ষিত হয় না।")
                                    },
                                    onAddJournal = { title, content, grat, habit, mood ->
                                        localJournals = listOf(
                                            PrivateJournal(
                                                id = "j_${System.currentTimeMillis()}",
                                                title = title,
                                                content = content,
                                                gratitudeNote = grat,
                                                habitReflection = habit,
                                                moodTag = mood,
                                                dateKey = PrayerTimeCalculator.todayDateKey()
                                            )
                                        ) + localJournals
                                        notifyUser("নোট এই সেশনের জন্য যুক্ত হয়েছে।")
                                    },
                                    onDeleteJournal = { jId ->
                                        localJournals = localJournals.filterNot { it.id == jId }
                                    }
                                )
                            }
                            NoorDestination.KNOWLEDGE -> {
                                androidx.compose.runtime.key(knowledgeJumpNonce) {
                                    KnowledgeAndCommunityScreen(
                                        initialTab = knowledgeInitialTab,
                                        profileState = UiState.Success(profile),
                                        curatedHadithsState = UiState.Success(emptyList()),
                                        curatedMotivationState = UiState.Success(emptyList()),
                                        activeDhikrIndex = activeDhikrIndex,
                                        customDhikrTitle = customDhikrTitle,
                                        tasbihCount = tasbihCount,
                                        tasbihTarget = tasbihTarget,
                                        tasbihLogsState = UiState.Success(tasbihLogs),
                                        communityPostsState = UiState.Success(emptyList()),
                                        quizResultsState = UiState.Success(quizResults),
                                        onSelectDhikrPreset = { idx ->
                                            val preset = BengaliIslamicData.dhikrPresets.getOrNull(idx)
                                            if (preset != null) {
                                                activeDhikrIndex = idx
                                                customDhikrTitle = ""
                                                tasbihCount = 0L
                                                tasbihTarget = preset.third
                                            }
                                        },
                                        onSetCustomDhikr = { title, target ->
                                            activeDhikrIndex = -1
                                            customDhikrTitle = title.trim()
                                            tasbihCount = 0L
                                            tasbihTarget = target.coerceIn(1L, 10000L)
                                        },
                                        onIncrementTasbih = { tasbihCount += 1L },
                                        onResetTasbih = { tasbihCount = 0L },
                                        onSaveTasbih = {
                                            val (title, ar) = if (activeDhikrIndex in BengaliIslamicData.dhikrPresets.indices) {
                                                val p = BengaliIslamicData.dhikrPresets[activeDhikrIndex]
                                                p.first to p.second
                                            } else {
                                                customDhikrTitle.ifBlank { "ব্যক্তিগত যিকির" } to ""
                                            }
                                            tasbihLogs = listOf(
                                                TasbihLog(
                                                    id = "t_${System.currentTimeMillis()}",
                                                    dhikrTitle = title,
                                                    arabicText = ar,
                                                    count = tasbihCount,
                                                    targetCount = tasbihTarget,
                                                    dateKey = PrayerTimeCalculator.todayDateKey()
                                                )
                                            ) + tasbihLogs
                                            notifyUser("তাসবিহ আমল সংরক্ষিত হয়েছে।")
                                        },
                                        onCreatePost = { _, _, _ ->
                                            notifyUser("কমিউনিটি পোস্ট করতে ক্লাউড লগইন প্রয়োজন।")
                                        },
                                        onLikePost = {},
                                        onReportPost = { _, _ -> },
                                        onBlockAuthor = {},
                                        onSubmitQuiz = { title, score, total ->
                                            val xp = (score * 15L).coerceAtLeast(10L)
                                            quizResults = listOf(
                                                QuizResult(
                                                    id = "q_${System.currentTimeMillis()}",
                                                    quizTitle = title,
                                                    score = score.toLong(),
                                                    totalQuestions = total.toLong(),
                                                    xpEarned = xp
                                                )
                                            ) + quizResults
                                            profile = profile.copy(xp = profile.xp + xp)
                                            notifyUser("কুইজ সম্পন্ন! +$xp XP গেম পয়েন্ট অর্জিত।")
                                        }
                                    )
                                }
                            }
                            NoorDestination.SAFETY -> {
                                androidx.compose.runtime.key(safetyJumpNonce) {
                                    SafetyWellbeingAndSettingsScreen(
                                        initialTab = safetyInitialTab,
                                        themeMode = themeMode,
                                        profileState = UiState.Success(profile),
                                        isAdmin = false,
                                        dnsResult = dnsResult,
                                        isCheckingDns = isCheckingDns,
                                        usageSnapshot = usageSnapshot,
                                        adminUsersState = UiState.Success(emptyList()),
                                        adminReportsState = UiState.Success(emptyList()),
                                        curatedHadithsState = UiState.Success(emptyList()),
                                        curatedMotivationState = UiState.Success(emptyList()),
                                        onSetThemeMode = onThemeChanged,
                                        onRunDnsCheck = {
                                            scope.launch {
                                                isCheckingDns = true
                                                dnsResult = NetworkSafetyChecker.checkPrivateDnsAndDoh(appContext)
                                                isCheckingDns = false
                                            }
                                        },
                                        onRefreshWellbeing = {
                                            usageSnapshot = NoorReminderHelper.readRealScreenTimeMinutes(appContext)
                                        },
                                        onUpdateProfile = { name, user, country, city, qPages, tGoal, sGoal, photo ->
                                            profile = profile.copy(
                                                displayName = name,
                                                username = user,
                                                country = country,
                                                locationName = city,
                                                dailyQuranGoalPages = qPages,
                                                dailyTasbihGoal = tGoal,
                                                dailyScreenGoalMinutes = sGoal,
                                                photoUrl = photo ?: profile.photoUrl
                                            )
                                            prayerSchedule = PrayerTimeCalculator.calculateForCity(PrayerTimeCalculator.findCity(city))
                                            notifyUser("প্রোফাইল হালনাগাদ হয়েছে।")
                                        },
                                        onTestNotification = { title, body ->
                                            NoorReminderHelper.showReminderNotification(appContext, title, body)
                                            notifyUser("নোটিফিকেশন পাঠানো হয়েছে।")
                                        },
                                        onSignOut = {
                                            notifyUser("আপনি অফলাইন/লোকাল মোডে আছেন।")
                                        },
                                        onDeleteAccount = {},
                                        onAdminAddCurated = { _, _, _, _, _ -> },
                                        onAdminDeleteCurated = { _, _ -> },
                                        onAdminToggleUserStatus = { _, _ -> },
                                        onAdminResolveReport = { _, _, _ -> }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
