package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.BengaliIslamicData
import com.example.data.DeviceUsageSnapshot
import com.example.data.NoorReminderHelper
import com.example.model.CommunityReport
import com.example.model.CuratedItem
import com.example.model.DnsCheckResult
import com.example.model.DnsProtectionStatus
import com.example.model.UiState
import com.example.model.UserProfile
import com.example.ui.components.GlassCard
import com.example.ui.components.NoorBadgeChip
import com.example.ui.components.NoorProgressBar
import com.example.ui.components.NoorSectionHeader
import com.example.ui.components.toBn
import com.example.ui.theme.LocalNoorGlassColors
import com.example.ui.theme.ThemeMode
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SafetyWellbeingAndSettingsScreen(
    initialTab: Int = 0,
    themeMode: ThemeMode,
    profileState: UiState<UserProfile?>,
    isAdmin: Boolean,
    dnsResult: DnsCheckResult,
    isCheckingDns: Boolean,
    usageSnapshot: DeviceUsageSnapshot,
    adminUsersState: UiState<List<UserProfile>>,
    adminReportsState: UiState<List<CommunityReport>>,
    curatedHadithsState: UiState<List<CuratedItem>>,
    curatedMotivationState: UiState<List<CuratedItem>>,
    onSetThemeMode: (ThemeMode) -> Unit,
    onRunDnsCheck: () -> Unit,
    onRefreshWellbeing: () -> Unit,
    onUpdateProfile: (String, String, String, String, Long, Long, Long, String?) -> Unit,
    onTestNotification: (String, String) -> Unit,
    onSignOut: () -> Unit,
    onDeleteAccount: () -> Unit,
    onAdminAddCurated: (String, String, String, String, String) -> Unit,
    onAdminDeleteCurated: (String, String) -> Unit,
    onAdminToggleUserStatus: (String, String) -> Unit,
    onAdminResolveReport: (String, String, Boolean) -> Unit
) {
    val noor = LocalNoorGlassColors.current
    var subTab by remember(initialTab) { mutableIntStateOf(initialTab.coerceIn(0, 3)) }
    val tabs = if (isAdmin) {
        listOf("নিরাপদ ইন্টারনেট", "ডিজিটাল ওয়েলবিইং", "প্রোফাইল ও সেটিংস", "অ্যাডমিন প্যানেল")
    } else {
        listOf("নিরাপদ ইন্টারনেট", "ডিজিটাল ওয়েলবিইং", "প্রোফাইল ও সেটিংস")
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 760.dp)
                .align(Alignment.TopCenter)
        ) {
            SecondaryTabRow(
                selectedTabIndex = subTab.coerceAtMost(tabs.lastIndex),
                containerColor = Color.Transparent,
                contentColor = if (noor.isDark) noor.accentGold else noor.emeraldPrimary
            ) {
                tabs.forEachIndexed { idx, label ->
                    Tab(
                        selected = subTab == idx,
                        onClick = { subTab = idx },
                        text = { Text(label, style = MaterialTheme.typography.labelMedium, maxLines = 1) }
                    )
                }
            }

            when (subTab) {
                0 -> DnsSafetyTab(
                    dnsResult = dnsResult,
                    isCheckingDns = isCheckingDns,
                    onRunDnsCheck = onRunDnsCheck
                )
                1 -> DigitalWellbeingTab(
                    profile = (profileState as? UiState.Success)?.data,
                    usageSnapshot = usageSnapshot,
                    onRefreshWellbeing = onRefreshWellbeing
                )
                2 -> ProfileAndSettingsTab(
                    themeMode = themeMode,
                    profile = (profileState as? UiState.Success)?.data,
                    isAdmin = isAdmin,
                    onOpenAdmin = { subTab = 3 },
                    onSetThemeMode = onSetThemeMode,
                    onUpdateProfile = onUpdateProfile,
                    onTestNotification = onTestNotification,
                    onSignOut = onSignOut,
                    onDeleteAccount = onDeleteAccount
                )
                else -> if (isAdmin) {
                    AdminPanelTab(
                        adminUsersState = adminUsersState,
                        adminReportsState = adminReportsState,
                        curatedHadithsState = curatedHadithsState,
                        curatedMotivationState = curatedMotivationState,
                        onAddCurated = onAdminAddCurated,
                        onDeleteCurated = onAdminDeleteCurated,
                        onToggleUserStatus = onAdminToggleUserStatus,
                        onResolveReport = onAdminResolveReport,
                        onSendAnnouncement = onTestNotification
                    )
                }
            }
        }
    }
}

@Composable
private fun DnsSafetyTab(
    dnsResult: DnsCheckResult,
    isCheckingDns: Boolean,
    onRunDnsCheck: () -> Unit
) {
    val noor = LocalNoorGlassColors.current
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

    val statusColor = when (dnsResult.status) {
        DnsProtectionStatus.ACTIVE_FAMILY -> noor.success
        DnsProtectionStatus.CONFIGURED_STANDARD -> noor.warning
        DnsProtectionStatus.INACTIVE -> noor.error
        DnsProtectionStatus.UNAVAILABLE -> noor.textSecondary
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            GlassCard(elevated = true) {
                NoorSectionHeader(
                    title = "নিরাপদ ইন্টারনেট ও DNS সুরক্ষা",
                    subtitle = "প্রাপ্তবয়স্ক অনুপযুক্ত ওয়েবসাইট, পর্নোগ্রাফিক ডোমেইন ও ফিশিং প্রতিরোধ",
                    icon = Icons.Default.Shield
                )
                Spacer(Modifier.height(10.dp))
                NoorBadgeChip(text = "অবস্থা: ${dnsResult.status.labelBn}", color = statusColor)
                Spacer(Modifier.height(8.dp))
                Text(dnsResult.detailsBn, style = MaterialTheme.typography.bodyMedium, color = noor.textPrimary)
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "নিরাপদ DNS DoH সংযোগ: ${if (dnsResult.dohReachable) "যাচাইকৃত ও সচল" else "যাচাই করা যায়নি"}",
                    style = MaterialTheme.typography.labelSmall,
                    color = noor.textSecondary
                )
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onRunDnsCheck,
                        enabled = !isCheckingDns,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        if (isCheckingDns) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
                        } else {
                            Text("সুরক্ষা পরীক্ষা")
                        }
                    }
                    OutlinedButton(
                        onClick = {
                            runCatching {
                                context.startActivity(Intent(Settings.ACTION_WIRELESS_SETTINGS))
                            }.onFailure {
                                context.startActivity(Intent(Settings.ACTION_SETTINGS))
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("DNS সেটিংস")
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "গোপনীয়তা ও স্বচ্ছতা: এই অ্যাপ আপনার ব্রাউজিং ইতিহাস সংগ্রহ করে না। পুরো ডিভাইসের ওয়েবসাইট ফিল্টার করতে Android-এর সিস্টেম Private DNS চালু থাকা আবশ্যক—সিস্টেমে চালু না থাকলে কখনোই 'সক্রিয়' দেখানো হয় না।",
                    style = MaterialTheme.typography.labelSmall,
                    color = noor.textSecondary
                )
            }
        }

        item {
            GlassCard {
                NoorSectionHeader(
                    title = "Android Private DNS সেটআপ গাইড",
                    subtitle = "সহজ ৪ ধাপ — কোনো বাড়তি VPN ব্যাটারি খরচ ছাড়াই",
                    icon = Icons.Default.Security
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "১. উপরের 'DNS সেটিংস' বাটনে ট্যাপ করে Network & Internet / Connection & Sharing-এ যান।\n" +
                        "২. 'Private DNS' অপশন নির্বাচন করুন।\n" +
                        "৩. 'Private DNS provider hostname' বেছে নিন।\n" +
                        "৪. নিচের যেকোনো একটি ফ্যামিলি-সেফ হোস্টনেম কপি করে পেস্ট ও সেভ করুন।",
                    style = MaterialTheme.typography.bodySmall,
                    color = noor.textPrimary
                )
            }
        }

        items(BengaliIslamicData.trustedDnsProviders) { provider ->
            GlassCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(provider.nameBn, style = MaterialTheme.typography.titleSmall, color = noor.textPrimary, fontWeight = FontWeight.Bold)
                        Text(provider.hostname, style = MaterialTheme.typography.labelLarge, color = if (noor.isDark) noor.accentGold else noor.emeraldPrimary)
                        Text("IPv4: ${provider.ipv4Primary}, ${provider.ipv4Secondary}", style = MaterialTheme.typography.labelSmall, color = noor.textSecondary)
                    }
                    IconButton(onClick = { clipboard.setText(AnnotatedString(provider.hostname)) }) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "হোস্টনেম কপি", tint = noor.accentGold)
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(provider.descriptionBn, style = MaterialTheme.typography.bodySmall, color = noor.textSecondary)
            }
        }
    }
}

@Composable
private fun DigitalWellbeingTab(
    profile: UserProfile?,
    usageSnapshot: DeviceUsageSnapshot,
    onRefreshWellbeing: () -> Unit
) {
    val noor = LocalNoorGlassColors.current
    val context = LocalContext.current
    var focusSecondsLeft by remember { mutableIntStateOf(25 * 60) }
    var focusRunning by remember { mutableStateOf(false) }

    LaunchedEffect(focusRunning) {
        while (focusRunning && focusSecondsLeft > 0) {
            delay(1000L)
            focusSecondsLeft--
        }
        if (focusSecondsLeft == 0) focusRunning = false
    }

    val goalMins = (profile?.dailyScreenGoalMinutes ?: 120L).toInt()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            GlassCard(elevated = true) {
                NoorSectionHeader(
                    title = "ডিজিটাল ওয়েলবিইং ও স্ক্রিন টাইম সচেতনতা",
                    subtitle = "শুধু ডিভাইসের অনুমোদিত ব্যবহারের তথ্য প্রদর্শিত হয়",
                    icon = Icons.Default.PhoneAndroid
                )
                Spacer(Modifier.height(10.dp))
                if (usageSnapshot.hasUsagePermission) {
                    val frac = (usageSnapshot.todayScreenMinutes.toFloat() / goalMins.coerceAtLeast(1)).coerceIn(0f, 1f)
                    Text(
                        text = "আজকের আনুমানিক স্ক্রিন টাইম: ${usageSnapshot.todayScreenMinutes.toBn()} মিনিট / লক্ষ্য ${goalMins.toBn()} মিনিট",
                        style = MaterialTheme.typography.titleMedium,
                        color = noor.textPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(6.dp))
                    NoorProgressBar(progress = frac)
                } else {
                    Text(
                        text = "ডিভাইসের স্ক্রিন টাইম দেখার অনুমতি (Usage Access) এখনো দেওয়া হয়নি। নিচের বাটন থেকে অনুমতি দিলে প্রকৃত স্ক্রিন টাইম দেখতে পাবেন।",
                        style = MaterialTheme.typography.bodySmall,
                        color = noor.textSecondary
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = {
                            runCatching { context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) }
                        }) {
                            Text("Usage Access অনুমতি")
                        }
                        Button(onClick = onRefreshWellbeing) {
                            Text("তথ্য রিফ্রেশ")
                        }
                    }
                }
            }
        }

        item {
            val mins = focusSecondsLeft / 60
            val secs = focusSecondsLeft % 60
            GlassCard {
                NoorSectionHeader(
                    title = "ফোকাস মোড (২৫ মিনিট ইবাদত/অধ্যয়ন)",
                    subtitle = "সোশ্যাল মিডিয়া থেকে বিরতি নিয়ে কুরআন বা প্রয়োজনীয় কাজে মনোযোগ দিন",
                    icon = Icons.Default.HourglassTop
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "${mins.toBn()}:${"%02d".format(secs).let { com.example.ui.components.toBanglaDigits(it) }}",
                    style = MaterialTheme.typography.displayLarge,
                    color = if (noor.isDark) noor.accentGold else noor.emeraldPrimary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = { focusRunning = !focusRunning },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (focusRunning) "বিরতি" else "ফোকাস শুরু করুন")
                    }
                    OutlinedButton(
                        onClick = {
                            focusRunning = false
                            focusSecondsLeft = 25 * 60
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("রিসেট")
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileAndSettingsTab(
    themeMode: ThemeMode,
    profile: UserProfile?,
    isAdmin: Boolean,
    onOpenAdmin: () -> Unit,
    onSetThemeMode: (ThemeMode) -> Unit,
    onUpdateProfile: (String, String, String, String, Long, Long, Long, String?) -> Unit,
    onTestNotification: (String, String) -> Unit,
    onSignOut: () -> Unit,
    onDeleteAccount: () -> Unit
) {
    val noor = LocalNoorGlassColors.current
    val context = LocalContext.current
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var selectedPhotoUri by remember { mutableStateOf<String?>(null) }

    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            onTestNotification("DCFBD NOOR রিমাইন্ডার", "আলহামদুলিল্লাহ! নামাজ ও কুরআনের রিমাইন্ডার সক্রিয় হয়েছে।")
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let {
            selectedPhotoUri = it.toString()
            if (profile != null) {
                onUpdateProfile(
                    profile.displayName,
                    profile.username,
                    profile.country,
                    profile.locationName,
                    profile.dailyQuranGoalPages,
                    profile.dailyTasbihGoal,
                    profile.dailyScreenGoalMinutes,
                    it.toString()
                )
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Profile Header
        item {
            GlassCard(elevated = true) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val photoModel = selectedPhotoUri ?: profile?.photoUrl?.takeIf { it.isNotBlank() }
                    if (photoModel != null) {
                        AsyncImage(
                            model = photoModel,
                            contentDescription = "প্রোফাইল ছবি",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "প্রোফাইল",
                            tint = noor.accentGold,
                            modifier = Modifier.size(56.dp)
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = profile?.displayName ?: "ব্যবহারকারী",
                            style = MaterialTheme.typography.titleLarge,
                            color = noor.textPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "@${profile?.username ?: "noor"} • ${profile?.email ?: ""}",
                            style = MaterialTheme.typography.bodySmall,
                            color = noor.textSecondary
                        )
                        Text(
                            text = "${profile?.country ?: "বাংলাদেশ"} • ${profile?.locationName ?: "ঢাকা"}",
                            style = MaterialTheme.typography.labelSmall,
                            color = noor.accentGold
                        )
                    }
                    IconButton(onClick = { showEditDialog = true }) {
                        Icon(Icons.Default.Edit, contentDescription = "প্রোফাইল সম্পাদনা", tint = noor.textPrimary)
                    }
                }
                Spacer(Modifier.height(10.dp))
                OutlinedButton(
                    onClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("গ্যালারি থেকে প্রোফাইল ছবি পরিবর্তন করুন")
                }
            }
        }

        // Theme Control (Day / Night / Auto)
        item {
            GlassCard {
                NoorSectionHeader(
                    title = "থিম নিয়ন্ত্রণ (দিনের ও রাতের মুড)",
                    subtitle = "আপনার পছন্দ ডেটাবেজে সংরক্ষিত থাকবে",
                    icon = Icons.Default.DarkMode
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ThemeMode.entries.forEach { mode ->
                        FilterChip(
                            selected = themeMode == mode,
                            onClick = { onSetThemeMode(mode) },
                            label = { Text(mode.labelBn) },
                            modifier = Modifier.testTag("theme_chip_${mode.name}")
                        )
                    }
                }
            }
        }

        // Notifications
        item {
            GlassCard {
                NoorSectionHeader(
                    title = "বিজ্ঞপ্তি ও রিমাইন্ডার",
                    subtitle = "নামাজ, কুরআন, দৈনিক মিশন ও রমজানের রিমাইন্ডার",
                    icon = Icons.Default.NotificationsActive
                )
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                            !NoorReminderHelper.hasNotificationPermission(context)
                        ) {
                            notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            onTestNotification("DCFBD NOOR রিমাইন্ডার", "আল্লাহর স্মরণে হৃদয় প্রশান্ত হয়। আজকের কুরআন ও মিশন দেখুন।")
                        }
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("বিজ্ঞপ্তি চালু ও পরীক্ষা করুন")
                }
            }
        }

        if (isAdmin) {
            item {
                GlassCard(onClick = onOpenAdmin, elevated = true) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = noor.accentGold)
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text("অ্যাডমিন প্যানেল (/admin)", style = MaterialTheme.typography.titleMedium, color = noor.textPrimary, fontWeight = FontWeight.Bold)
                                Text("ব্যবহারকারী, হাদীস, মোটিভেশন ও রিপোর্ট পরিচালনা", style = MaterialTheme.typography.bodySmall, color = noor.textSecondary)
                            }
                        }
                        NoorBadgeChip(text = "প্রবেশ করুন")
                    }
                }
            }
        }

        // About, Privacy, Sign Out & Delete Account
        item {
            GlassCard {
                Text("অ্যাপ সম্পর্কে ও গোপনীয়তা", style = MaterialTheme.typography.titleMedium, color = noor.textPrimary, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text(
                    "DCFBD NOOR — পরিচালনায়: DarkNet Cyber Force BD\nস্লোগান: একটি সুন্দর জীবনের পথে (A Journey Towards Hidayah)",
                    style = MaterialTheme.typography.bodySmall,
                    color = noor.textSecondary
                )
                Spacer(Modifier.height(10.dp))
                OutlinedButton(
                    onClick = { showPrivacyDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("গোপনীয়তা নীতি ও নিরাপত্তা")
                }
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = onSignOut,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("sign_out_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = noor.emeraldPrimary, contentColor = Color.White)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("লগআউট করুন")
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { showDeleteConfirm = true },
                    modifier = Modifier.fillMaxWidth(),
                    border = BorderStroke(1.dp, noor.error),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.DeleteForever, contentDescription = null, tint = noor.error)
                    Spacer(Modifier.width(6.dp))
                    Text("অ্যাকাউন্ট ও ডেটা মুছে ফেলুন", color = noor.error)
                }
            }
        }
    }

    if (showEditDialog && profile != null) {
        var name by remember { mutableStateOf(profile.displayName) }
        var username by remember { mutableStateOf(profile.username) }
        var country by remember { mutableStateOf(profile.country) }
        var city by remember { mutableStateOf(profile.locationName) }
        var quranPages by remember { mutableStateOf(profile.dailyQuranGoalPages.toString()) }
        var tasbihGoal by remember { mutableStateOf(profile.dailyTasbihGoal.toString()) }
        var screenGoal by remember { mutableStateOf(profile.dailyScreenGoalMinutes.toString()) }

        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("প্রোফাইল সম্পাদনা") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("নাম") }, singleLine = true)
                    OutlinedTextField(value = username, onValueChange = { username = it }, label = { Text("ইউজারনেম") }, singleLine = true)
                    OutlinedTextField(value = country, onValueChange = { country = it }, label = { Text("দেশ") }, singleLine = true)
                    OutlinedTextField(value = city, onValueChange = { city = it }, label = { Text("শহর (নামাজের সময়ের জন্য)") }, singleLine = true)
                    OutlinedTextField(value = quranPages, onValueChange = { quranPages = it.filter { c -> c.isDigit() } }, label = { Text("দৈনিক কুরআন লক্ষ্য (পৃষ্ঠা)") }, singleLine = true)
                    OutlinedTextField(value = tasbihGoal, onValueChange = { tasbihGoal = it.filter { c -> c.isDigit() } }, label = { Text("দৈনিক তাসবিহ লক্ষ্য") }, singleLine = true)
                    OutlinedTextField(value = screenGoal, onValueChange = { screenGoal = it.filter { c -> c.isDigit() } }, label = { Text("দৈনিক স্ক্রিন টাইম লক্ষ্য (মিনিট)") }, singleLine = true)
                }
            },
            confirmButton = {
                Button(onClick = {
                    onUpdateProfile(
                        name,
                        username,
                        country,
                        city,
                        quranPages.toLongOrNull() ?: 4L,
                        tasbihGoal.toLongOrNull() ?: 100L,
                        screenGoal.toLongOrNull() ?: 120L,
                        null
                    )
                    showEditDialog = false
                }) { Text("সংরক্ষণ") }
            },
            dismissButton = { TextButton(onClick = { showEditDialog = false }) { Text("বাতিল") } }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("অ্যাকাউন্ট মুছে ফেলবেন?") },
            text = { Text("এটি আপনার সকল ব্যক্তিগত মিশন, নামাজের লগ, বুকমার্ক, জার্নাল ও অ্যাকাউন্ট স্থায়ীভাবে মুছে ফেলবে।") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDeleteAccount()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = noor.error, contentColor = Color.White)
                ) { Text("স্থায়ীভাবে মুছুন") }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("বাতিল") } }
        )
    }

    if (showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            title = { Text("গোপনীয়তা নীতি") },
            text = {
                Text(
                    "১. আপনার ব্যক্তিগত জার্নাল, নামাজের লগ, মিশন ও AI চ্যাট কেবল আপনার নিজস্ব UID-তে সংরক্ষিত থাকে এবং Firestore Security Rules দ্বারা সুরক্ষিত।\n" +
                        "২. কোনো ব্যক্তিগত আমল বা গুনাহের তথ্য কমিউনিটিতে প্রদর্শিত হয় না।\n" +
                        "৩. DNS সেকশন আপনার ব্রাউজিং ইতিহাস সংগ্রহ করে না।"
                )
            },
            confirmButton = { Button(onClick = { showPrivacyDialog = false }) { Text("বুঝেছি") } }
        )
    }
}

@Composable
private fun AdminPanelTab(
    adminUsersState: UiState<List<UserProfile>>,
    adminReportsState: UiState<List<CommunityReport>>,
    curatedHadithsState: UiState<List<CuratedItem>>,
    curatedMotivationState: UiState<List<CuratedItem>>,
    onAddCurated: (String, String, String, String, String) -> Unit,
    onDeleteCurated: (String, String) -> Unit,
    onToggleUserStatus: (String, String) -> Unit,
    onResolveReport: (String, String, Boolean) -> Unit,
    onSendAnnouncement: (String, String) -> Unit
) {
    val noor = LocalNoorGlassColors.current
    val users = (adminUsersState as? UiState.Success)?.data ?: emptyList()
    val reports = (adminReportsState as? UiState.Success)?.data ?: emptyList()
    val hadiths = (curatedHadithsState as? UiState.Success)?.data ?: emptyList()
    val motivations = (curatedMotivationState as? UiState.Success)?.data ?: emptyList()

    var newTitle by remember { mutableStateOf("") }
    var newBody by remember { mutableStateOf("") }
    var newRef by remember { mutableStateOf("") }
    var targetCollection by remember { mutableStateOf("curated_hadiths") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            GlassCard(elevated = true) {
                NoorSectionHeader(
                    title = "অ্যাডমিন ড্যাশবোর্ড (/admin)",
                    subtitle = "সার্ভার-যাচাইকৃত অ্যাডমিন রোল দ্বারা সুরক্ষিত",
                    icon = Icons.Default.AdminPanelSettings
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    NoorBadgeChip(text = "ব্যবহারকারী: ${users.size.toBn()}")
                    NoorBadgeChip(text = "খোলা রিপোর্ট: ${reports.count { it.status == "OPEN" }.toBn()}")
                    NoorBadgeChip(text = "কন্টেন্ট: ${(hadiths.size + motivations.size).toBn()}")
                }
            }
        }

        // Add Curated Content (Hadith / Motivation / Mission Template)
        item {
            GlassCard {
                Text("হাদীস / মোটিভেশন / মিশন যুক্ত করুন", style = MaterialTheme.typography.titleMedium, color = noor.textPrimary, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(selected = targetCollection == "curated_hadiths", onClick = { targetCollection = "curated_hadiths" }, label = { Text("হাদীস") })
                    FilterChip(selected = targetCollection == "curated_motivation", onClick = { targetCollection = "curated_motivation" }, label = { Text("মোটিভেশন") })
                    FilterChip(selected = targetCollection == "mission_templates", onClick = { targetCollection = "mission_templates" }, label = { Text("মিশন") })
                }
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(value = newTitle, onValueChange = { newTitle = it }, label = { Text("শিরোনাম") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(value = newBody, onValueChange = { newBody = it }, label = { Text("বিস্তারিত বার্তা") }, minLines = 2, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(value = newRef, onValueChange = { newRef = it }, label = { Text("যাচাইকৃত রেফারেন্স (যেমন: সহিহ বুখারি)") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            if (newTitle.isNotBlank() && newBody.isNotBlank()) {
                                onAddCurated(targetCollection, newTitle, newBody, newRef, "অ্যাডমিন")
                                newTitle = ""
                                newBody = ""
                                newRef = ""
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text("প্রকাশ করুন") }
                    OutlinedButton(
                        onClick = {
                            if (newTitle.isNotBlank() && newBody.isNotBlank()) {
                                onSendAnnouncement(newTitle, newBody)
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text("ঘোষণা পাঠান") }
                }
            }
        }

        // Users List
        item {
            GlassCard {
                Text("ব্যবহারকারী পরিচালনা", style = MaterialTheme.typography.titleMedium, color = noor.textPrimary, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                users.take(20).forEach { u ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("${u.displayName} (${u.accountStatus})", style = MaterialTheme.typography.bodyMedium, color = noor.textPrimary)
                            Text("XP: ${u.xp.toBn()} • লেভেল: ${u.level.toBn()}", style = MaterialTheme.typography.labelSmall, color = noor.textSecondary)
                        }
                        TextButton(onClick = { onToggleUserStatus(u.userId, u.accountStatus) }) {
                            Text(if (u.accountStatus == "SUSPENDED") "সক্রিয় করুন" else "স্থগিত করুন")
                        }
                    }
                }
            }
        }

        // Moderation Reports
        if (reports.isNotEmpty()) {
            item {
                GlassCard {
                    Text("কমিউনিটি রিপোর্ট পর্যালোচনা", style = MaterialTheme.typography.titleMedium, color = noor.textPrimary, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    reports.forEach { rep ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("কারণ: ${rep.reason}", style = MaterialTheme.typography.bodySmall, color = noor.textPrimary)
                                Text("অবস্থা: ${rep.status}", style = MaterialTheme.typography.labelSmall, color = noor.accentGold)
                            }
                            if (rep.status == "OPEN") {
                                Row {
                                    TextButton(onClick = { onResolveReport(rep.id, rep.postId, true) }) { Text("লুকান") }
                                    TextButton(onClick = { onResolveReport(rep.id, rep.postId, false) }) { Text("বাতিল") }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Delete Curated items
        items(hadiths + motivations, key = { it.id }) { item ->
            GlassCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(item.title, style = MaterialTheme.typography.titleSmall, color = noor.textPrimary)
                        Text(item.body.take(80), style = MaterialTheme.typography.bodySmall, color = noor.textSecondary)
                    }
                    TextButton(onClick = {
                        val col = if (hadiths.any { it.id == item.id }) "curated_hadiths" else "curated_motivation"
                        onDeleteCurated(col, item.id)
                    }) { Text("মুছুন", color = noor.error) }
                }
            }
        }
    }
}
