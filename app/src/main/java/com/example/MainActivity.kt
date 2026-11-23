package com.example

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.NoorAiRepository
import com.example.data.NoorRepository
import com.example.data.QuranAudioPlayer
import com.example.data.QuranRepository
import com.example.firebase.attemptAutoSignIn
import com.example.firebase.authStateFlow
import com.example.firebase.configureAppCheck
import com.example.firebase.signOutUser
import com.example.model.UiState
import com.example.ui.components.IslamicBackdrop
import com.example.ui.screens.AiAndJournalScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.KnowledgeAndCommunityScreen
import com.example.ui.screens.MissionsAndPrayerScreen
import com.example.ui.screens.NoorSplashScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.QuranScreen
import com.example.ui.screens.SafetyWellbeingAndSettingsScreen
import com.example.ui.theme.DcfbdNoorTheme
import com.example.ui.theme.LocalNoorGlassColors
import com.example.ui.theme.ThemeMode
import com.example.viewmodel.NoorViewModel
import com.example.viewmodel.SystemBridge
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private val Context.noorDataStore by preferencesDataStore(name = "dcfbd_noor_prefs")

private object NoorPrefKeys {
    val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
    val THEME_MODE = stringPreferencesKey("theme_mode")
}

private enum class NoorDestination(
    val routeId: String,
    val labelBn: String,
    val shortLabelBn: String,
    val icon: ImageVector,
    val testTag: String
) {
    HOME("home", "মূল পাতা", "মূল পাতা", Icons.Filled.Home, "nav_home"),
    QURAN("quran", "কুরআন", "কুরআন", Icons.Filled.MenuBook, "nav_quran"),
    MISSIONS("missions", "মিশন ও নামাজ", "মিশন", Icons.Filled.CheckCircle, "nav_missions"),
    AI("ai_journal", "AI ও জার্নাল", "AI", Icons.Filled.AutoAwesome, "nav_ai"),
    KNOWLEDGE("knowledge_tasbih", "জ্ঞান ও তাসবিহ", "তাসবিহ", Icons.Filled.Spa, "nav_knowledge"),
    SAFETY("safety_settings", "সুরক্ষা ও সেটিংস", "সুরক্ষা", Icons.Filled.Shield, "nav_safety")
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Must configure App Check immediately after super.onCreate before any Firebase calls
        configureAppCheck(this, intent)
        enableEdgeToEdge()
        setContent {
            DcfbdNoorRoot()
        }
    }
}

@Composable
private fun DcfbdNoorRoot() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    val onboardingFlow: Flow<Boolean> = remember {
        context.noorDataStore.data.map { prefs -> prefs[NoorPrefKeys.ONBOARDING_DONE] ?: false }
    }
    val localThemeFlow: Flow<ThemeMode> = remember {
        context.noorDataStore.data.map { prefs -> ThemeMode.fromRaw(prefs[NoorPrefKeys.THEME_MODE]) }
    }

    val onboardingDone by onboardingFlow.collectAsStateWithLifecycle(initialValue = true)
    var showOnboardingManually by rememberSaveable { mutableStateOf(false) }
    var splashFinished by rememberSaveable { mutableStateOf(false) }
    val localThemeMode by localThemeFlow.collectAsStateWithLifecycle(initialValue = ThemeMode.AUTO)
    var activeThemeMode by remember { mutableStateOf(ThemeMode.AUTO) }

    LaunchedEffect(localThemeMode) {
        activeThemeMode = localThemeMode
    }

    val authUserFlow = remember { Firebase.auth.authStateFlow() }
    val currentUser by authUserFlow.collectAsStateWithLifecycle(initialValue = Firebase.auth.currentUser)

    LaunchedEffect(Unit) {
        if (Firebase.auth.currentUser == null) {
            attemptAutoSignIn(
                context = context,
                credentialManager = credentialManager,
                onAuthSuccess = {},
                onUnauthenticated = {},
                scope = scope
            )
        }
    }

    DcfbdNoorTheme(themeMode = activeThemeMode) {
        when {
            !splashFinished -> {
                NoorSplashScreen(onFinished = { splashFinished = true })
            }
            !onboardingDone || showOnboardingManually -> {
                OnboardingScreen(
                    onComplete = {
                        showOnboardingManually = false
                        scope.launch {
                            context.noorDataStore.edit { prefs ->
                                prefs[NoorPrefKeys.ONBOARDING_DONE] = true
                            }
                        }
                    }
                )
            }
            currentUser == null -> {
                AuthScreen(
                    onAuthSuccess = {},
                    onShowOnboarding = { showOnboardingManually = true }
                )
            }
            else -> {
                val uid = currentUser!!.uid
                androidx.compose.runtime.key(uid) {
                    NoorAuthenticatedShell(
                        uid = uid,
                        themeMode = activeThemeMode,
                        onThemeChanged = { nextMode ->
                            activeThemeMode = nextMode
                            scope.launch {
                                context.noorDataStore.edit { prefs ->
                                    prefs[NoorPrefKeys.THEME_MODE] = nextMode.name
                                }
                            }
                        },
                        onSignOutRequested = {
                            signOutUser(
                                credentialManager = credentialManager,
                                scope = scope
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun NoorAuthenticatedShell(
    uid: String,
    themeMode: ThemeMode,
    onThemeChanged: (ThemeMode) -> Unit,
    onSignOutRequested: () -> Unit
) {
    val context = LocalContext.current
    val appContext = context.applicationContext
    val viewModelScope = rememberCoroutineScope()

    val viewModel: NoorViewModel = viewModel(
        key = uid,
        factory = viewModelFactory {
            initializer {
                val dbId = appContext.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(dbId)
                val repo = NoorRepository(db)
                val quranRepo = QuranRepository(appContext)
                val aiRepo = NoorAiRepository()
                val audioPlayer = QuranAudioPlayer(appContext, viewModelScope)
                val systemBridge = SystemBridge(appContext)
                NoorViewModel(
                    repository = repo,
                    quranRepository = quranRepo,
                    aiRepository = aiRepo,
                    audioPlayer = audioPlayer,
                    systemBridge = systemBridge,
                    currentUserId = uid
                )
            }
        }
    )

    val noor = LocalNoorGlassColors.current
    val snackbarHostState = remember { SnackbarHostState() }

    val profileState by viewModel.userProfile.collectAsStateWithLifecycle()
    val missionsState by viewModel.missions.collectAsStateWithLifecycle()
    val historyState by viewModel.missionHistory.collectAsStateWithLifecycle()
    val prayerLogsState by viewModel.prayerLogs.collectAsStateWithLifecycle()
    val bookmarksState by viewModel.bookmarks.collectAsStateWithLifecycle()
    val quranProgressState by viewModel.quranProgress.collectAsStateWithLifecycle()
    val conversationsState by viewModel.conversations.collectAsStateWithLifecycle()
    val selectedConversationId by viewModel.selectedConversationId.collectAsStateWithLifecycle()
    val activeMessagesState by viewModel.activeMessages.collectAsStateWithLifecycle()
    val privateJournalsState by viewModel.privateJournals.collectAsStateWithLifecycle()
    val tasbihLogsState by viewModel.tasbihLogs.collectAsStateWithLifecycle()
    val communityPostsState by viewModel.communityPosts.collectAsStateWithLifecycle()
    val quizResultsState by viewModel.quizResults.collectAsStateWithLifecycle()
    val curatedHadithsState by viewModel.curatedHadiths.collectAsStateWithLifecycle()
    val curatedMotivationState by viewModel.curatedMotivation.collectAsStateWithLifecycle()
    val adminUsersState by viewModel.adminUsers.collectAsStateWithLifecycle()
    val adminReportsState by viewModel.adminReports.collectAsStateWithLifecycle()
    val selectedSurah by viewModel.selectedSurah.collectAsStateWithLifecycle()
    val surahAyahsState by viewModel.surahAyahs.collectAsStateWithLifecycle()
    val audioState by viewModel.audioState.collectAsStateWithLifecycle()
    val aiChatUiState by viewModel.aiChatUiState.collectAsStateWithLifecycle()
    val dnsResult by viewModel.dnsCheckResult.collectAsStateWithLifecycle()
    val isCheckingDns by viewModel.isCheckingDns.collectAsStateWithLifecycle()
    val usageSnapshot by viewModel.usageSnapshot.collectAsStateWithLifecycle()
    val activeDhikrIndex by viewModel.activeDhikrIndex.collectAsStateWithLifecycle()
    val customDhikrTitle by viewModel.customDhikrTitle.collectAsStateWithLifecycle()
    val tasbihCount by viewModel.tasbihCount.collectAsStateWithLifecycle()
    val tasbihTarget by viewModel.tasbihTarget.collectAsStateWithLifecycle()
    val prayerSchedule by viewModel.prayerSchedule.collectAsStateWithLifecycle()
    val isAdmin by viewModel.isAdmin.collectAsStateWithLifecycle()
    val toastMessage by viewModel.toastMessage.collectAsStateWithLifecycle()

    LaunchedEffect(profileState) {
        val profile = (profileState as? UiState.Success)?.data ?: return@LaunchedEffect
        val remoteTheme = ThemeMode.fromRaw(profile.themeMode)
        if (remoteTheme != themeMode) {
            onThemeChanged(remoteTheme)
        }
        if (profile.locationName.isNotBlank()) {
            viewModel.updatePrayerCity(profile.locationName)
        }
    }

    LaunchedEffect(toastMessage) {
        val msg = toastMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearToast()
        }
    }

    var currentDestination by rememberSaveable { mutableStateOf(NoorDestination.HOME) }
    var knowledgeInitialTab by rememberSaveable { mutableIntStateOf(0) }
    var knowledgeJumpNonce by rememberSaveable { mutableIntStateOf(0) }
    var safetyInitialTab by rememberSaveable { mutableIntStateOf(0) }
    var safetyJumpNonce by rememberSaveable { mutableIntStateOf(0) }

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
                    NoorGlassTopHeader(
                        currentTitle = currentDestination.labelBn,
                        themeMode = themeMode,
                        isAdmin = isAdmin,
                        onCycleTheme = {
                            val next = when (themeMode) {
                                ThemeMode.AUTO -> ThemeMode.DAY
                                ThemeMode.DAY -> ThemeMode.NIGHT
                                ThemeMode.NIGHT -> ThemeMode.AUTO
                            }
                            onThemeChanged(next)
                            viewModel.setThemeMode(next)
                        },
                        onOpenAdmin = {
                            safetyInitialTab = 3
                            safetyJumpNonce += 1
                            currentDestination = NoorDestination.SAFETY
                        }
                    )
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
                                    profileState = profileState,
                                    missionsState = missionsState,
                                    historyState = historyState,
                                    prayerSchedule = prayerSchedule,
                                    dnsResult = dnsResult,
                                    onToggleMission = viewModel::toggleMission,
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
                                    bookmarksState = bookmarksState,
                                    quranProgressState = quranProgressState,
                                    audioState = audioState,
                                    onSelectSurah = viewModel::loadSurah,
                                    onMarkAyahRead = viewModel::markAyahRead,
                                    onToggleBookmark = viewModel::toggleBookmark,
                                    onPlayFromAyah = viewModel::playFromAyah,
                                    onTogglePlayPause = viewModel::toggleAudioPlayPause,
                                    onPlayNext = viewModel::playNextAyah,
                                    onPlayPrevious = viewModel::playPreviousAyah,
                                    onSetSpeed = viewModel::setAudioSpeed,
                                    onToggleRepeatAyah = viewModel::toggleRepeatAyah,
                                    onToggleRepeatSurah = viewModel::toggleRepeatSurah,
                                    onSelectQari = viewModel::setQari,
                                    onDownloadSurahAudio = viewModel::downloadCurrentSurahAudio
                                )
                            }
                            NoorDestination.MISSIONS -> {
                                MissionsAndPrayerScreen(
                                    profileState = profileState,
                                    missionsState = missionsState,
                                    historyState = historyState,
                                    prayerLogsState = prayerLogsState,
                                    prayerSchedule = prayerSchedule,
                                    onToggleMission = viewModel::toggleMission,
                                    onAddCustomMission = viewModel::addCustomMission,
                                    onResetFreshStart = viewModel::resetMissionsFreshStart,
                                    onTogglePrayer = viewModel::togglePrayer,
                                    onSelectCity = viewModel::updatePrayerCity
                                )
                            }
                            NoorDestination.AI -> {
                                AiAndJournalScreen(
                                    profileState = profileState,
                                    conversationsState = conversationsState,
                                    selectedConversationId = selectedConversationId,
                                    activeMessagesState = activeMessagesState,
                                    aiChatUiState = aiChatUiState,
                                    journalsState = privateJournalsState,
                                    onSelectConversation = viewModel::selectConversation,
                                    onSendAiPrompt = viewModel::sendAiPrompt,
                                    onDeleteConversation = viewModel::deleteConversation,
                                    onSetJournalPin = viewModel::setJournalPin,
                                    onAddJournal = viewModel::addJournalEntry,
                                    onDeleteJournal = viewModel::deleteJournalEntry
                                )
                            }
                            NoorDestination.KNOWLEDGE -> {
                                androidx.compose.runtime.key(knowledgeJumpNonce) {
                                    KnowledgeAndCommunityScreen(
                                        initialTab = knowledgeInitialTab,
                                        profileState = profileState,
                                        curatedHadithsState = curatedHadithsState,
                                        curatedMotivationState = curatedMotivationState,
                                        activeDhikrIndex = activeDhikrIndex,
                                        customDhikrTitle = customDhikrTitle,
                                        tasbihCount = tasbihCount,
                                        tasbihTarget = tasbihTarget,
                                        tasbihLogsState = tasbihLogsState,
                                        communityPostsState = communityPostsState,
                                        quizResultsState = quizResultsState,
                                        onSelectDhikrPreset = viewModel::selectDhikrPreset,
                                        onSetCustomDhikr = viewModel::setCustomDhikr,
                                        onIncrementTasbih = viewModel::incrementTasbih,
                                        onResetTasbih = viewModel::resetTasbih,
                                        onSaveTasbih = viewModel::saveTasbihToCloud,
                                        onCreatePost = viewModel::createCommunityPost,
                                        onLikePost = viewModel::likePost,
                                        onReportPost = viewModel::reportPost,
                                        onBlockAuthor = viewModel::blockAuthor,
                                        onSubmitQuiz = viewModel::submitQuizResult
                                    )
                                }
                            }
                            NoorDestination.SAFETY -> {
                                androidx.compose.runtime.key(safetyJumpNonce) {
                                    SafetyWellbeingAndSettingsScreen(
                                        initialTab = safetyInitialTab,
                                        themeMode = themeMode,
                                        profileState = profileState,
                                        isAdmin = isAdmin,
                                        dnsResult = dnsResult,
                                        isCheckingDns = isCheckingDns,
                                        usageSnapshot = usageSnapshot,
                                        adminUsersState = adminUsersState,
                                        adminReportsState = adminReportsState,
                                        curatedHadithsState = curatedHadithsState,
                                        curatedMotivationState = curatedMotivationState,
                                        onSetThemeMode = { next ->
                                            onThemeChanged(next)
                                            viewModel.setThemeMode(next)
                                        },
                                        onRunDnsCheck = viewModel::runDnsCheck,
                                        onRefreshWellbeing = viewModel::refreshWellbeing,
                                        onUpdateProfile = viewModel::updateProfile,
                                        onTestNotification = viewModel::triggerTestNotification,
                                        onSignOut = onSignOutRequested,
                                        onDeleteAccount = {
                                            viewModel.deleteAccount(onDeleted = onSignOutRequested)
                                        },
                                        onAdminAddCurated = viewModel::adminAddCuratedItem,
                                        onAdminDeleteCurated = viewModel::adminDeleteCuratedItem,
                                        onAdminToggleUserStatus = viewModel::adminToggleUserStatus,
                                        onAdminResolveReport = viewModel::adminResolveReport
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

@Composable
private fun NoorGlassTopHeader(
    currentTitle: String,
    themeMode: ThemeMode,
    isAdmin: Boolean,
    onCycleTheme: () -> Unit,
    onOpenAdmin: () -> Unit
) {
    val noor = LocalNoorGlassColors.current
    Surface(
        color = noor.glassCardElevated,
        border = BorderStroke(1.dp, noor.glassBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Image(
                    painter = painterResource(id = R.drawable.noor_icon_fg),
                    contentDescription = "DCFBD NOOR",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                )
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        text = "DCFBD NOOR",
                        style = MaterialTheme.typography.titleSmall,
                        color = if (noor.isDark) noor.accentGold else noor.emeraldPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = currentTitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = noor.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isAdmin) {
                    Surface(
                        onClick = onOpenAdmin,
                        shape = RoundedCornerShape(50),
                        color = noor.accentGold.copy(alpha = 0.22f),
                        border = BorderStroke(1.dp, noor.accentGold),
                        modifier = Modifier.testTag("top_admin_chip")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.AdminPanelSettings,
                                contentDescription = "অ্যাডমিন প্যানেল",
                                tint = if (noor.isDark) noor.accentGold else noor.emeraldPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "অ্যাডমিন",
                                style = MaterialTheme.typography.labelSmall,
                                color = noor.textPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(Modifier.width(6.dp))
                }

                IconButton(
                    onClick = onCycleTheme,
                    modifier = Modifier.testTag("top_theme_toggle")
                ) {
                    Icon(
                        imageVector = if (noor.isDark) Icons.Default.LightMode else Icons.Default.DarkMode,
                        contentDescription = "থিম পরিবর্তন (${themeMode.labelBn})",
                        tint = if (noor.isDark) noor.accentGold else noor.emeraldPrimary
                    )
                }
            }
        }
    }
}

@Composable
private fun NoorGlassBottomBar(
    currentDestination: NoorDestination,
    onSelect: (NoorDestination) -> Unit
) {
    val noor = LocalNoorGlassColors.current
    Surface(
        color = noor.glassCardElevated,
        border = BorderStroke(1.dp, noor.glassBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        NavigationBar(
            containerColor = Color.Transparent,
            tonalElevation = 0.dp
        ) {
            NoorDestination.entries.forEach { dest ->
                val selected = currentDestination == dest
                NavigationBarItem(
                    selected = selected,
                    onClick = { onSelect(dest) },
                    modifier = Modifier.testTag(dest.testTag),
                    icon = {
                        Icon(
                            imageVector = dest.icon,
                            contentDescription = dest.labelBn,
                            modifier = Modifier.size(22.dp)
                        )
                    },
                    label = {
                        Text(
                            text = dest.shortLabelBn,
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = if (noor.isDark) noor.accentGold else noor.emeraldPrimary,
                        selectedTextColor = if (noor.isDark) noor.accentGold else noor.emeraldPrimary,
                        indicatorColor = noor.accentGold.copy(alpha = 0.22f),
                        unselectedIconColor = noor.textSecondary,
                        unselectedTextColor = noor.textSecondary
                    )
                )
            }
        }
    }
}

@Composable
private fun NoorGlassNavigationRail(
    currentDestination: NoorDestination,
    onSelect: (NoorDestination) -> Unit
) {
    val noor = LocalNoorGlassColors.current
    Surface(
        color = noor.glassCardElevated,
        border = BorderStroke(1.dp, noor.glassBorder),
        modifier = Modifier.fillMaxHeight()
    ) {
        NavigationRail(
            containerColor = Color.Transparent
        ) {
            Spacer(Modifier.size(8.dp))
            NoorDestination.entries.forEach { dest ->
                val selected = currentDestination == dest
                NavigationRailItem(
                    selected = selected,
                    onClick = { onSelect(dest) },
                    modifier = Modifier.testTag(dest.testTag),
                    icon = {
                        Icon(
                            imageVector = dest.icon,
                            contentDescription = dest.labelBn
                        )
                    },
                    label = {
                        Text(
                            text = dest.shortLabelBn,
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1
                        )
                    },
                    colors = NavigationRailItemDefaults.colors(
                        selectedIconColor = if (noor.isDark) noor.accentGold else noor.emeraldPrimary,
                        selectedTextColor = if (noor.isDark) noor.accentGold else noor.emeraldPrimary,
                        indicatorColor = noor.accentGold.copy(alpha = 0.22f),
                        unselectedIconColor = noor.textSecondary,
                        unselectedTextColor = noor.textSecondary
                    )
                )
            }
        }
    }
}
