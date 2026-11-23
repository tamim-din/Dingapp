package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mosque
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.data.BengaliIslamicData
import com.example.data.PrayerTimeCalculator
import com.example.model.AchievementBadge
import com.example.model.MissionCategory
import com.example.model.MissionHistory
import com.example.model.MissionSchedule
import com.example.model.PrayerLog
import com.example.model.PrayerSchedule
import com.example.model.UiState
import com.example.model.UserMission
import com.example.model.UserProfile
import com.example.ui.components.GlassCard
import com.example.ui.components.NoorBadgeChip
import com.example.ui.components.NoorProgressBar
import com.example.ui.components.NoorSectionHeader
import com.example.ui.components.toBn
import com.example.ui.theme.LocalNoorGlassColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MissionsAndPrayerScreen(
    profileState: UiState<UserProfile?>,
    missionsState: UiState<List<UserMission>>,
    historyState: UiState<List<MissionHistory>>,
    prayerLogsState: UiState<List<PrayerLog>>,
    prayerSchedule: PrayerSchedule,
    onToggleMission: (UserMission) -> Unit,
    onAddCustomMission: (String, MissionCategory, MissionSchedule, Long) -> Unit,
    onResetFreshStart: (List<UserMission>) -> Unit,
    onTogglePrayer: (PrayerLog?, String, String) -> Unit,
    onSelectCity: (String) -> Unit
) {
    val noor = LocalNoorGlassColors.current
    var subTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("দৈনিক মিশন", "নামাজ ট্র্যাকার", "ইসলামিক ক্যালেন্ডার")

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 760.dp)
                .align(Alignment.TopCenter)
        ) {
            SecondaryTabRow(
                selectedTabIndex = subTab,
                containerColor = Color.Transparent,
                contentColor = if (noor.isDark) noor.accentGold else noor.emeraldPrimary
            ) {
                tabs.forEachIndexed { idx, label ->
                    Tab(
                        selected = subTab == idx,
                        onClick = { subTab = idx },
                        text = { Text(label, style = MaterialTheme.typography.labelLarge, maxLines = 1) }
                    )
                }
            }

            when (subTab) {
                0 -> MissionsTabContent(
                    profile = (profileState as? UiState.Success)?.data,
                    missionsState = missionsState,
                    history = (historyState as? UiState.Success)?.data ?: emptyList(),
                    onToggleMission = onToggleMission,
                    onAddCustomMission = onAddCustomMission,
                    onResetFreshStart = onResetFreshStart
                )
                1 -> PrayerTrackerTabContent(
                    prayerLogsState = prayerLogsState,
                    prayerSchedule = prayerSchedule,
                    onTogglePrayer = onTogglePrayer,
                    onSelectCity = onSelectCity
                )
                else -> IslamicCalendarTabContent(prayerSchedule = prayerSchedule)
            }
        }
    }
}

@Composable
private fun MissionsTabContent(
    profile: UserProfile?,
    missionsState: UiState<List<UserMission>>,
    history: List<MissionHistory>,
    onToggleMission: (UserMission) -> Unit,
    onAddCustomMission: (String, MissionCategory, MissionSchedule, Long) -> Unit,
    onResetFreshStart: (List<UserMission>) -> Unit
) {
    val noor = LocalNoorGlassColors.current
    var selectedSchedule by remember { mutableStateOf(MissionSchedule.DAILY) }
    var selectedCategory by remember { mutableStateOf<MissionCategory?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }

    val xp = profile?.xp ?: 0L
    val level = profile?.level ?: 1L
    val badges = remember(xp) {
        listOf(
            AchievementBadge("b1", "প্রথম পদক্ষেপ", "প্রথম ৫০ XP অর্জন", 50L, xp >= 50L),
            AchievementBadge("b2", "ইবাদতের সাথী", "২০০ XP অর্জন", 200L, xp >= 200L),
            AchievementBadge("b3", "সুন্দর চরিত্র", "৫০০ XP অর্জন", 500L, xp >= 500L),
            AchievementBadge("b4", "আত্মসংযমের আলো", "১০০০ XP অর্জন", 1000L, xp >= 1000L)
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // XP, Level, Achievements Card
            item {
                GlassCard(elevated = true) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "লেভেল ${level.toBn()} / ১০০",
                                style = MaterialTheme.typography.titleLarge,
                                color = noor.textPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "মোট গেম XP: ${xp.toBn()}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (noor.isDark) noor.accentGold else noor.emeraldPrimary
                            )
                        }
                        val missionsList = (missionsState as? UiState.Success)?.data ?: emptyList()
                        OutlinedButton(
                            onClick = { onResetFreshStart(missionsList) },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, noor.glassBorder)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("নতুন করে শুরু")
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    NoorProgressBar(progress = (xp % 100L) / 100f)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "কোনো দিন মিশন বাদ পড়লে হতাশ হবেন না—আল্লাহর রহমত অসীম, যেকোনো মুহূর্তে নতুন উদ্যমে শুরু করা যায়।",
                        style = MaterialTheme.typography.bodySmall,
                        color = noor.textSecondary
                    )
                }
            }

            // Badges
            item {
                GlassCard {
                    NoorSectionHeader(title = "অর্জনের ব্যাজ", icon = Icons.Default.EmojiEvents)
                    Spacer(Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(badges) { badge ->
                            NoorBadgeChip(
                                text = if (badge.unlocked) "✓ ${badge.titleBn}" else "${badge.titleBn} (${badge.requiredXp.toBn()} XP)",
                                color = if (badge.unlocked) noor.success else noor.textSecondary
                            )
                        }
                    }
                }
            }

            // Schedule & Category Filters
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(MissionSchedule.entries) { sched ->
                        FilterChip(
                            selected = selectedSchedule == sched,
                            onClick = { selectedSchedule = sched },
                            label = { Text("${sched.labelBn} মিশন") }
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = selectedCategory == null,
                            onClick = { selectedCategory = null },
                            label = { Text("সব ক্যাটাগরি") }
                        )
                    }
                    items(MissionCategory.entries) { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat.labelBn) }
                        )
                    }
                }
            }

            when (missionsState) {
                is UiState.Loading -> item { CircularProgressIndicator() }
                is UiState.Error -> item {
                    GlassCard { Text(missionsState.message, color = noor.error) }
                }
                is UiState.Success -> {
                    val filtered = missionsState.data.filter {
                        it.schedule == selectedSchedule.name &&
                            (selectedCategory == null || it.category == selectedCategory?.name)
                    }
                    if (filtered.isEmpty()) {
                        item {
                            GlassCard {
                                Text(
                                    "এই বিভাগে এখনো কোনো মিশন নেই। নিচের '+' বাটনে ক্লিক করে নিজের মতো মিশন তৈরি করুন।",
                                    color = noor.textSecondary
                                )
                            }
                        }
                    } else {
                        items(filtered, key = { it.id }) { mission ->
                            GlassCard(onClick = { onToggleMission(mission) }) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = mission.completed,
                                        onCheckedChange = { onToggleMission(mission) },
                                        colors = CheckboxDefaults.colors(
                                            checkedColor = if (noor.isDark) noor.accentGold else noor.emeraldPrimary
                                        )
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = mission.title,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = noor.textPrimary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = "${MissionCategory.fromRaw(mission.category).labelBn} • সম্পন্ন: ${mission.completedCount.toBn()} বার",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = noor.textSecondary
                                        )
                                    }
                                    NoorBadgeChip(text = "+${mission.xpReward.toBn()} XP")
                                }
                            }
                        }
                    }
                }
            }

            if (history.isNotEmpty()) {
                item {
                    GlassCard {
                        NoorSectionHeader(
                            title = "সাম্প্রতিক মিশনের ইতিহাস",
                            icon = Icons.Default.CheckCircle
                        )
                        Spacer(Modifier.height(6.dp))
                        history.take(5).forEach { h ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(h.title, style = MaterialTheme.typography.bodySmall, color = noor.textPrimary, modifier = Modifier.weight(1f))
                                Text("+${h.xpEarned.toBn()} XP", style = MaterialTheme.typography.labelSmall, color = noor.success)
                            }
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(72.dp)) }
        }

        FloatingActionButton(
            onClick = { showCreateDialog = true },
            containerColor = if (noor.isDark) noor.accentGold else noor.emeraldPrimary,
            contentColor = if (noor.isDark) Color(0xFF10271F) else Color.White,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .testTag("add_custom_mission_fab")
        ) {
            Icon(Icons.Default.Add, contentDescription = "নতুন মিশন তৈরি করুন")
        }
    }

    if (showCreateDialog) {
        CreateMissionDialog(
            onDismiss = { showCreateDialog = false },
            onSave = { title, cat, sched, xpVal ->
                showCreateDialog = false
                onAddCustomMission(title, cat, sched, xpVal)
            }
        )
    }
}

@Composable
private fun CreateMissionDialog(
    onDismiss: () -> Unit,
    onSave: (String, MissionCategory, MissionSchedule, Long) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(MissionCategory.IBADAT) }
    var schedule by remember { mutableStateOf(MissionSchedule.DAILY) }
    var xpText by remember { mutableStateOf("30") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("নিজের মিশন তৈরি করুন") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("মিশনের নাম (বাংলায়)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("ক্যাটাগরি:", style = MaterialTheme.typography.labelMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(MissionCategory.entries) { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat.labelBn) }
                        )
                    }
                }
                Text("সময়সূচি:", style = MaterialTheme.typography.labelMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(MissionSchedule.entries) { s ->
                        FilterChip(
                            selected = schedule == s,
                            onClick = { schedule = s },
                            label = { Text(s.labelBn) }
                        )
                    }
                }
                OutlinedTextField(
                    value = xpText,
                    onValueChange = { xpText = it.filter { c -> c.isDigit() } },
                    label = { Text("গেম XP পয়েন্ট (৫–৫০০)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val xp = xpText.toLongOrNull()?.coerceIn(5L, 500L) ?: 25L
                    if (title.isNotBlank()) onSave(title, category, schedule, xp)
                }
            ) {
                Text("সংরক্ষণ")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("বাতিল") }
        }
    )
}

@Composable
private fun PrayerTrackerTabContent(
    prayerLogsState: UiState<List<PrayerLog>>,
    prayerSchedule: PrayerSchedule,
    onTogglePrayer: (PrayerLog?, String, String) -> Unit,
    onSelectCity: (String) -> Unit
) {
    val noor = LocalNoorGlassColors.current
    val context = LocalContext.current
    var showCityMenu by remember { mutableStateOf(false) }

    val locationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            onSelectCity("ঢাকা")
        }
    }

    val logs = (prayerLogsState as? UiState.Success)?.data ?: emptyList()
    val todayKey = PrayerTimeCalculator.todayDateKey()
    val todayLog = logs.firstOrNull { it.dateKey == todayKey }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            GlassCard(elevated = true) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "অবস্থান: ${prayerSchedule.locationName}",
                            style = MaterialTheme.typography.titleLarge,
                            color = noor.textPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "পরবর্তী নামাজ: ${prayerSchedule.nextPrayerNameBn} (${prayerSchedule.nextPrayerTime})",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (noor.isDark) noor.accentGold else noor.emeraldPrimary
                        )
                    }
                    Box {
                        OutlinedButton(
                            onClick = { showCityMenu = true },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("শহর পরিবর্তন")
                        }
                        DropdownMenu(
                            expanded = showCityMenu,
                            onDismissRequest = { showCityMenu = false }
                        ) {
                            PrayerTimeCalculator.presetCities.forEach { city ->
                                DropdownMenuItem(
                                    text = { Text(city.nameBn) },
                                    onClick = {
                                        showCityMenu = false
                                        onSelectCity(city.nameBn)
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("জিপিএস অবস্থান অনুমতি যাচাই") },
                                onClick = {
                                    showCityMenu = false
                                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                                        locationLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                                    }
                                }
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "গুরুত্বপূর্ণ: ব্যবহারকারী নিজে সম্পন্ন না করলে কোনো নামাজ স্বয়ংক্রিয়ভাবে সম্পন্ন হিসেবে চিহ্নিত হয় না।",
                    style = MaterialTheme.typography.labelSmall,
                    color = noor.textSecondary
                )
            }
        }

        val prayerSlots = listOf(
            Triple("fajr", "ফজর", prayerSchedule.fajr to (todayLog?.fajr == true)),
            Triple("dhuhr", "যোহর", prayerSchedule.dhuhr to (todayLog?.dhuhr == true)),
            Triple("asr", "আসর", prayerSchedule.asr to (todayLog?.asr == true)),
            Triple("maghrib", "মাগরিব", prayerSchedule.maghrib to (todayLog?.maghrib == true)),
            Triple("isha", "এশা", prayerSchedule.isha to (todayLog?.isha == true)),
            Triple("tahajjud", "তাহাজ্জুদ (নফল)", "শেষ রাত" to (todayLog?.tahajjud == true))
        )

        items(prayerSlots) { (key, title, timeAndDone) ->
            GlassCard(onClick = { onTogglePrayer(todayLog, key, prayerSchedule.locationName) }) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Mosque,
                            contentDescription = title,
                            tint = if (timeAndDone.second) noor.success else noor.textSecondary
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(title, style = MaterialTheme.typography.titleMedium, color = noor.textPrimary, fontWeight = FontWeight.Bold)
                            Text("সময়: ${timeAndDone.first}", style = MaterialTheme.typography.bodySmall, color = noor.textSecondary)
                        }
                    }
                    Switch(
                        checked = timeAndDone.second,
                        onCheckedChange = { onTogglePrayer(todayLog, key, prayerSchedule.locationName) }
                    )
                }
            }
        }

        item {
            GlassCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("আজকের নফল নামাজ (রাকাত/সংখ্যা)", style = MaterialTheme.typography.titleMedium, color = noor.textPrimary)
                        Text("মোট আদায়: ${(todayLog?.naflCount ?: 0L).toBn()}", style = MaterialTheme.typography.bodyMedium, color = noor.accentGold)
                    }
                    Button(
                        onClick = { onTogglePrayer(todayLog, "nafl_inc", prayerSchedule.locationName) },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("+১ যোগ করুন")
                    }
                }
            }
        }

        item {
            val totalLoggedDays = logs.size
            val totalFarzPrayers = logs.sumOf { log ->
                listOf(log.fajr, log.dhuhr, log.asr, log.maghrib, log.isha).count { it }
            }
            GlassCard {
                NoorSectionHeader(title = "মাসিক নামাজের রিপোর্ট", icon = Icons.Default.CalendarMonth)
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "গত ${totalLoggedDays.toBn()} দিনে রেকর্ডকৃত ফরজ ওয়াক্ত: ${totalFarzPrayers.toBn()} ওয়াক্ত",
                    style = MaterialTheme.typography.bodyMedium,
                    color = noor.textPrimary
                )
            }
        }
    }
}

@Composable
private fun IslamicCalendarTabContent(prayerSchedule: PrayerSchedule) {
    val noor = LocalNoorGlassColors.current
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            GlassCard(elevated = true) {
                NoorSectionHeader(
                    title = "হিজরি ও ইংরেজি ক্যালেন্ডার",
                    subtitle = "চাঁদ দেখার ওপর ভিত্তি করে স্থানীয়ভাবে ১ দিন তারতম্য হতে পারে",
                    icon = Icons.Default.CalendarMonth
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = prayerSchedule.hijriDateTextBn,
                    style = MaterialTheme.typography.headlineSmall,
                    color = if (noor.isDark) noor.accentGold else noor.emeraldPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = prayerSchedule.dateTextBn,
                    style = MaterialTheme.typography.bodyLarge,
                    color = noor.textPrimary
                )
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    GlassCard(modifier = Modifier.weight(1f), contentPadding = 12.dp) {
                        Text("সেহরির শেষ সময়", style = MaterialTheme.typography.labelSmall, color = noor.textSecondary)
                        Text(prayerSchedule.sehriEnd, style = MaterialTheme.typography.titleMedium, color = noor.textPrimary, fontWeight = FontWeight.Bold)
                    }
                    GlassCard(modifier = Modifier.weight(1f), contentPadding = 12.dp) {
                        Text("ইফতারের সময়", style = MaterialTheme.typography.labelSmall, color = noor.textSecondary)
                        Text(prayerSchedule.iftarTime, style = MaterialTheme.typography.titleMedium, color = noor.textPrimary, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "দ্রষ্টব্য: স্থানীয় চাঁদ দেখার কারণে হিজরি তারিখ ও রমজান/ঈদের দিন পরিবর্তিত হতে পারে।",
                    style = MaterialTheme.typography.labelSmall,
                    color = noor.textSecondary
                )
            }
        }

        item {
            GlassCard {
                NoorSectionHeader(title = "গুরুত্বপূর্ণ ইসলামিক দিবস ও নফল রোজা", icon = Icons.Default.Mosque)
                Spacer(Modifier.height(8.dp))
                BengaliIslamicData.importantIslamicDatesBn.forEach { (dateLabel, desc) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = dateLabel,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (noor.isDark) noor.accentGold else noor.emeraldPrimary,
                            modifier = Modifier.weight(0.4f)
                        )
                        Text(
                            text = desc,
                            style = MaterialTheme.typography.bodySmall,
                            color = noor.textPrimary,
                            modifier = Modifier.weight(0.6f)
                        )
                    }
                }
            }
        }
    }
}
