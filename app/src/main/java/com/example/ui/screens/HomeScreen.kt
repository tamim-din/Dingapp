package com.example.ui.screens

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.BengaliIslamicData
import com.example.model.DnsCheckResult
import com.example.model.DnsProtectionStatus
import com.example.model.MissionHistory
import com.example.model.PrayerSchedule
import com.example.model.UiState
import com.example.model.UserMission
import com.example.model.UserProfile
import com.example.ui.components.GlassCard
import com.example.ui.components.NoorBadgeChip
import com.example.ui.components.NoorProgressBar
import com.example.ui.components.NoorSectionHeader
import com.example.ui.components.toBn
import com.example.ui.theme.AmiriQuranFontFamily
import com.example.ui.theme.LocalNoorGlassColors
import java.util.Calendar

@Composable
fun HomeScreen(
    profileState: UiState<UserProfile?>,
    missionsState: UiState<List<UserMission>>,
    historyState: UiState<List<MissionHistory>>,
    prayerSchedule: PrayerSchedule,
    dnsResult: DnsCheckResult,
    onToggleMission: (UserMission) -> Unit,
    onNavigateQuran: () -> Unit,
    onNavigateAi: () -> Unit,
    onNavigateTasbih: () -> Unit,
    onNavigateSafety: () -> Unit,
    onNavigateMissions: () -> Unit
) {
    val noor = LocalNoorGlassColors.current
    val dayOfYear = remember { Calendar.getInstance().get(Calendar.DAY_OF_YEAR) }
    val todayVerse = remember(dayOfYear) {
        BengaliIslamicData.dailyVerses[dayOfYear % BengaliIslamicData.dailyVerses.size]
    }
    val todayHadith = remember(dayOfYear) {
        BengaliIslamicData.verifiedHadiths[dayOfYear % BengaliIslamicData.verifiedHadiths.size]
    }

    val profile = (profileState as? UiState.Success)?.data
    val missions = (missionsState as? UiState.Success)?.data ?: emptyList()
    val history = (historyState as? UiState.Success)?.data ?: emptyList()

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp)
                .align(Alignment.TopCenter),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Greeting & Hijri Date Hero Card
            item {
                GlassCard(elevated = true, contentPadding = 20.dp) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "আসসালামু আলাইকুম,",
                                style = MaterialTheme.typography.labelLarge,
                                color = if (noor.isDark) noor.accentGold else noor.emeraldPrimary
                            )
                            Text(
                                text = profile?.displayName?.ifBlank { "প্রিয় ভাই/বোন" } ?: "প্রিয় ভাই/বোন",
                                style = MaterialTheme.typography.headlineMedium,
                                color = noor.textPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = prayerSchedule.hijriDateTextBn,
                                style = MaterialTheme.typography.titleMedium,
                                color = noor.textPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = prayerSchedule.dateTextBn,
                                style = MaterialTheme.typography.bodySmall,
                                color = noor.textSecondary
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            NoorBadgeChip(
                                text = "লেভেল ${(profile?.level ?: 1L).toBn()}",
                                icon = Icons.Default.AutoAwesome
                            )
                            Spacer(Modifier.height(6.dp))
                            NoorBadgeChip(
                                text = "ধারাবাহিকতা ${(profile?.streakDays ?: 1L).toBn()} দিন",
                                color = noor.success
                            )
                        }
                    }

                    Spacer(Modifier.height(14.dp))
                    val xp = profile?.xp ?: 0L
                    val xpInLevel = (xp % 100L).toFloat() / 100f
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "মোট গেম XP: ${xp.toBn()}",
                            style = MaterialTheme.typography.labelMedium,
                            color = noor.textPrimary
                        )
                        Text(
                            text = "পরবর্তী লেভেল: ${(100L - (xp % 100L)).toBn()} XP বাকি",
                            style = MaterialTheme.typography.labelSmall,
                            color = noor.textSecondary
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    NoorProgressBar(progress = xpInLevel)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "দ্রষ্টব্য: XP শুধু অভ্যাসের উৎসাহমূলক গেম পয়েন্ট—এটি কখনো প্রকৃত সওয়াব বা ঈমানের পরিমাপ নয়।",
                        style = MaterialTheme.typography.labelSmall,
                        color = noor.textSecondary
                    )
                }
            }

            // 2. Quick Shortcuts Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuickShortcutCard(
                        title = "কুরআন পড়ুন",
                        icon = Icons.Default.MenuBook,
                        onClick = onNavigateQuran,
                        tag = "shortcut_quran",
                        modifier = Modifier.weight(1f)
                    )
                    QuickShortcutCard(
                        title = "AI সহকারী",
                        icon = Icons.Default.AutoAwesome,
                        onClick = onNavigateAi,
                        tag = "shortcut_ai",
                        modifier = Modifier.weight(1f)
                    )
                    QuickShortcutCard(
                        title = "ডিজিটাল তাসবিহ",
                        icon = Icons.Default.Spa,
                        onClick = onNavigateTasbih,
                        tag = "shortcut_tasbih",
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // 3. Prayer Times & Safe Internet Status Row
            item {
                GlassCard(onClick = onNavigateMissions) {
                    NoorSectionHeader(
                        title = "নামাজের সময়সূচি (${prayerSchedule.locationName})",
                        subtitle = "পরবর্তী ওয়াক্ত: ${prayerSchedule.nextPrayerNameBn} • ${prayerSchedule.nextPrayerTime} (${prayerSchedule.remainingTimeBn})",
                        icon = Icons.Default.AccessTime
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        PrayerTimeMini("ফজর", prayerSchedule.fajr)
                        PrayerTimeMini("যোহর", prayerSchedule.dhuhr)
                        PrayerTimeMini("আসর", prayerSchedule.asr)
                        PrayerTimeMini("মাগরিব", prayerSchedule.maghrib)
                        PrayerTimeMini("এশা", prayerSchedule.isha)
                    }
                }
            }

            item {
                val statusColor = when (dnsResult.status) {
                    DnsProtectionStatus.ACTIVE_FAMILY -> noor.success
                    DnsProtectionStatus.CONFIGURED_STANDARD -> noor.warning
                    DnsProtectionStatus.INACTIVE -> noor.error
                    DnsProtectionStatus.UNAVAILABLE -> noor.textSecondary
                }
                GlassCard(onClick = onNavigateSafety, modifier = Modifier.testTag("home_safety_card")) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = "নিরাপদ ইন্টারনেটের অবস্থা",
                                tint = statusColor,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "নিরাপদ ইন্টারনেটের অবস্থা",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = noor.textPrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = dnsResult.status.labelBn,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = statusColor,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                        NoorBadgeChip(text = "বিস্তারিত", color = statusColor)
                    }
                }
            }

            // 4. Today's Quran Ayah
            item {
                GlassCard(onClick = onNavigateQuran) {
                    NoorSectionHeader(
                        title = "আজকের কুরআনের আয়াত",
                        subtitle = todayVerse.third,
                        icon = Icons.Default.MenuBook
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = todayVerse.first,
                        fontFamily = AmiriQuranFontFamily,
                        fontSize = 24.sp,
                        lineHeight = 40.sp,
                        color = if (noor.isDark) noor.accentGold else noor.emeraldPrimary,
                        textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = todayVerse.second,
                        style = MaterialTheme.typography.bodyLarge,
                        color = noor.textPrimary
                    )
                }
            }

            // 5. Today's Verified Hadith
            item {
                GlassCard {
                    NoorSectionHeader(
                        title = "আজকের সহিহ হাদীস",
                        subtitle = "${todayHadith.referenceBn} • ${todayHadith.gradeBn}",
                        icon = Icons.Default.WbSunny
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = todayHadith.arabicText,
                        fontFamily = AmiriQuranFontFamily,
                        fontSize = 20.sp,
                        lineHeight = 34.sp,
                        color = noor.textPrimary,
                        textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = todayHadith.banglaText,
                        style = MaterialTheme.typography.bodyLarge,
                        color = noor.textPrimary
                    )
                }
            }

            // 6. Today's Missions Preview
            item {
                GlassCard {
                    NoorSectionHeader(
                        title = "আজকের দৈনিক মিশন",
                        subtitle = "নিজেকে গড়ার ছোট ছোট পদক্ষেপ",
                        icon = Icons.Default.CheckCircle,
                        trailing = {
                            TextButton(onClick = onNavigateMissions) {
                                Text(
                                    "সব দেখুন",
                                    color = if (noor.isDark) noor.accentGold else noor.emeraldPrimary
                                )
                            }
                        }
                    )
                    Spacer(Modifier.height(8.dp))
                    when (missionsState) {
                        is UiState.Loading -> CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        is UiState.Error -> Text(missionsState.message, color = noor.error)
                        is UiState.Success -> {
                            val dailyPreview = missions.filter { it.schedule == "DAILY" }.take(4)
                            if (dailyPreview.isEmpty()) {
                                Text(
                                    "এখনো কোনো মিশন নেই। 'মিশন ও নামাজ' ট্যাব থেকে নতুন মিশন যোগ করুন।",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = noor.textSecondary
                                )
                            } else {
                                dailyPreview.forEach { m ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Checkbox(
                                            checked = m.completed,
                                            onCheckedChange = { onToggleMission(m) },
                                            colors = CheckboxDefaults.colors(
                                                checkedColor = if (noor.isDark) noor.accentGold else noor.emeraldPrimary
                                            )
                                        )
                                        Text(
                                            text = m.title,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = noor.textPrimary,
                                            modifier = Modifier.weight(1f)
                                        )
                                        NoorBadgeChip(text = "+${m.xpReward.toBn()} XP")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 7. Weekly Progress Bar Chart
            item {
                GlassCard {
                    NoorSectionHeader(
                        title = "সাপ্তাহিক অগ্রগতি",
                        subtitle = "গত ৭ দিনের সম্পন্ন মিশন ও অনুশীলন",
                        icon = Icons.Default.RadioButtonUnchecked
                    )
                    Spacer(Modifier.height(12.dp))
                    WeeklyProgressChart(history = history)
                }
            }
        }
    }
}

@Composable
private fun QuickShortcutCard(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    tag: String,
    modifier: Modifier = Modifier
) {
    val noor = LocalNoorGlassColors.current
    GlassCard(
        modifier = modifier.testTag(tag),
        onClick = onClick,
        contentPadding = 12.dp,
        cornerRadius = 18.dp
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (noor.isDark) noor.accentGold else noor.emeraldPrimary,
                modifier = Modifier.size(26.dp)
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = noor.textPrimary,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2
            )
        }
    }
}

@Composable
private fun PrayerTimeMini(name: String, time: String) {
    val noor = LocalNoorGlassColors.current
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = name,
            style = MaterialTheme.typography.labelSmall,
            color = noor.textSecondary
        )
        Text(
            text = time.replace(" পূর্বাহ্ণ", "").replace(" অপরাহ্ণ", ""),
            style = MaterialTheme.typography.labelMedium,
            color = noor.textPrimary,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun WeeklyProgressChart(history: List<MissionHistory>) {
    val noor = LocalNoorGlassColors.current
    val dayBuckets = remember(history) {
        val cal = Calendar.getInstance()
        val labels = mutableListOf<String>()
        val counts = mutableListOf<Int>()
        for (i in 6 downTo 0) {
            val c = (cal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -i) }
            val key = "%04d-%02d-%02d".format(
                c.get(Calendar.YEAR),
                c.get(Calendar.MONTH) + 1,
                c.get(Calendar.DAY_OF_MONTH)
            )
            val shortDay = when (c.get(Calendar.DAY_OF_WEEK)) {
                Calendar.SATURDAY -> "শনি"
                Calendar.SUNDAY -> "রবি"
                Calendar.MONDAY -> "সোম"
                Calendar.TUESDAY -> "মঙ্গল"
                Calendar.WEDNESDAY -> "বুধ"
                Calendar.THURSDAY -> "বৃহ"
                else -> "শুক্র"
            }
            labels.add(shortDay)
            counts.add(history.count { it.dateKey == key })
        }
        labels to counts
    }

    val maxCount = (dayBuckets.second.maxOrNull() ?: 1).coerceAtLeast(3)
    val barColor = if (noor.isDark) noor.accentGold else noor.emeraldPrimary
    val trackColor = noor.glassBorder.copy(alpha = 0.25f)

    Column {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(92.dp)
        ) {
            val barCount = dayBuckets.second.size
            val slotWidth = size.width / barCount
            val barWidth = (slotWidth * 0.44f).coerceAtMost(28.dp.toPx())
            dayBuckets.second.forEachIndexed { idx, value ->
                val x = idx * slotWidth + (slotWidth - barWidth) / 2f
                drawRoundRect(
                    color = trackColor,
                    topLeft = Offset(x, 0f),
                    size = Size(barWidth, size.height),
                    cornerRadius = CornerRadius(12f, 12f)
                )
                val barHeight = if (value == 0) 8f else (value.toFloat() / maxCount) * size.height
                drawRoundRect(
                    color = barColor,
                    topLeft = Offset(x, size.height - barHeight),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(12f, 12f)
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            dayBuckets.first.forEachIndexed { idx, label ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = noor.textSecondary
                    )
                    Text(
                        text = dayBuckets.second[idx].toBn(),
                        style = MaterialTheme.typography.labelSmall,
                        color = noor.textPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
