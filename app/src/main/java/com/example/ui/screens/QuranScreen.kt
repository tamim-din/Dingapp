package com.example.ui.screens

import android.content.Intent
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
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.BengaliIslamicData
import com.example.data.QuranAudioState
import com.example.model.QuranAyah
import com.example.model.QuranBookmark
import com.example.model.QuranProgress
import com.example.model.SurahMeta
import com.example.model.UiState
import com.example.ui.components.GlassCard
import com.example.ui.components.NoorBadgeChip
import com.example.ui.components.NoorProgressBar
import com.example.ui.components.NoorSectionHeader
import com.example.ui.components.toBn
import com.example.ui.theme.AmiriQuranFontFamily
import com.example.ui.theme.LocalNoorGlassColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuranScreen(
    selectedSurah: SurahMeta,
    surahAyahsState: UiState<List<QuranAyah>>,
    bookmarksState: UiState<List<QuranBookmark>>,
    quranProgressState: UiState<QuranProgress?>,
    audioState: QuranAudioState,
    onSelectSurah: (Int) -> Unit,
    onMarkAyahRead: (QuranAyah) -> Unit,
    onToggleBookmark: (QuranAyah, QuranBookmark?) -> Unit,
    onPlayFromAyah: (Int) -> Unit,
    onTogglePlayPause: () -> Unit,
    onPlayNext: () -> Unit,
    onPlayPrevious: () -> Unit,
    onSetSpeed: (Float) -> Unit,
    onToggleRepeatAyah: () -> Unit,
    onToggleRepeatSurah: () -> Unit,
    onSelectQari: (String) -> Unit,
    onDownloadSurahAudio: () -> Unit
) {
    val noor = LocalNoorGlassColors.current
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

    var subTab by remember { mutableIntStateOf(0) } // 0: পাঠ, 1: ১১৪ সূরা, 2: ৩০ পারা, 3: বুকমার্ক
    var searchQuery by remember { mutableStateOf("") }
    var ayahSearchQuery by remember { mutableStateOf("") }
    var showQariMenu by remember { mutableStateOf(false) }

    val bookmarks = (bookmarksState as? UiState.Success)?.data ?: emptyList()
    val progress = (quranProgressState as? UiState.Success)?.data
    val tabs = listOf("পাঠক", "১১৪ সূরা", "৩০ পারা", "বুকমার্ক")

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
                        text = {
                            Text(
                                label,
                                style = MaterialTheme.typography.labelLarge,
                                maxLines = 1
                            )
                        }
                    )
                }
            }

            when (subTab) {
                0 -> {
                    // Surah Reader + Audio Controls
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Last read progress banner
                        item {
                            GlassCard(elevated = true) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "সূরা ${selectedSurah.number.toBn()}: ${selectedSurah.nameBn} (${selectedSurah.meaningBn})",
                                            style = MaterialTheme.typography.titleLarge,
                                            color = noor.textPrimary,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "${selectedSurah.revelationBn} • আয়াত: ${selectedSurah.ayahCount.toBn()} • পারা ${selectedSurah.startPara.toBn()}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = noor.textSecondary
                                        )
                                    }
                                    Text(
                                        text = selectedSurah.nameAr,
                                        fontFamily = AmiriQuranFontFamily,
                                        fontSize = 24.sp,
                                        color = if (noor.isDark) noor.accentGold else noor.emeraldPrimary
                                    )
                                }

                                if (progress != null) {
                                    Spacer(Modifier.height(10.dp))
                                    val goal = progress.dailyGoalAyahs.coerceAtLeast(1L)
                                    val dailyFrac = (progress.dailyAyahsRead.toFloat() / goal).coerceIn(0f, 1f)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "সর্বশেষ পড়া: সূরা ${progress.lastSurahNameBn}, আয়াত ${progress.lastAyahNumber.toBn()}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = noor.textSecondary
                                        )
                                        Text(
                                            text = "দৈনিক লক্ষ্য: ${progress.dailyAyahsRead.toBn()}/${goal.toBn()}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = noor.textPrimary
                                        )
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    NoorProgressBar(progress = dailyFrac, height = 6.dp)
                                }

                                Spacer(Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Button(
                                        onClick = { onPlayFromAyah(1) },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (noor.isDark) noor.accentGold else noor.emeraldPrimary,
                                            contentColor = if (noor.isDark) Color(0xFF10271F) else Color.White
                                        ),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.testTag("play_surah_button")
                                    ) {
                                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                                        Spacer(Modifier.width(4.dp))
                                        Text("তিলাওয়াত শুনুন")
                                    }

                                    Box {
                                        OutlinedButton(
                                            onClick = { showQariMenu = true },
                                            shape = RoundedCornerShape(12.dp),
                                            border = BorderStroke(1.dp, noor.glassBorder)
                                        ) {
                                            Icon(Icons.Default.GraphicEq, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(4.dp))
                                            val qariName = BengaliIslamicData.qaris.firstOrNull { it.id == audioState.qariId }?.nameBn?.substringBefore(" ") ?: "ক্বারী"
                                            Text(qariName, maxLines = 1)
                                        }
                                        DropdownMenu(
                                            expanded = showQariMenu,
                                            onDismissRequest = { showQariMenu = false }
                                        ) {
                                            BengaliIslamicData.qaris.forEach { qari ->
                                                DropdownMenuItem(
                                                    text = { Text("${qari.nameBn} (${qari.styleBn})") },
                                                    onClick = {
                                                        showQariMenu = false
                                                        onSelectQari(qari.id)
                                                    }
                                                )
                                            }
                                        }
                                    }

                                    val isDownloaded = audioState.downloadedSurahs.contains(selectedSurah.number)
                                    IconButton(
                                        onClick = onDownloadSurahAudio,
                                        enabled = !audioState.isDownloadingSurah
                                    ) {
                                        if (audioState.isDownloadingSurah) {
                                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                        } else {
                                            Icon(
                                                imageVector = if (isDownloaded) Icons.Default.DownloadDone else Icons.Default.Download,
                                                contentDescription = "অফলাইন সূরা অডিও ডাউনলোড",
                                                tint = if (isDownloaded) noor.success else noor.textPrimary
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Ayah search inside Surah
                        item {
                            OutlinedTextField(
                                value = ayahSearchQuery,
                                onValueChange = { ayahSearchQuery = it },
                                placeholder = { Text("আয়াত বা বাংলা অর্থ অনুসন্ধান করুন...") },
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp)
                            )
                        }

                        when (surahAyahsState) {
                            is UiState.Loading -> {
                                item {
                                    GlassCard {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            CircularProgressIndicator(modifier = Modifier.size(22.dp))
                                            Spacer(Modifier.width(12.dp))
                                            Text("সূরার আয়াত ও বাংলা অনুবাদ লোড হচ্ছে...", color = noor.textPrimary)
                                        }
                                    }
                                }
                            }
                            is UiState.Error -> {
                                item {
                                    GlassCard {
                                        Text(surahAyahsState.message, color = noor.error)
                                        Spacer(Modifier.height(8.dp))
                                        Button(onClick = { onSelectSurah(selectedSurah.number) }) {
                                            Text("পুনরায় চেষ্টা করুন")
                                        }
                                    }
                                }
                            }
                            is UiState.Success -> {
                                val filteredAyahs = surahAyahsState.data.filter { ayah ->
                                    ayahSearchQuery.isBlank() ||
                                        ayah.banglaTranslation.contains(ayahSearchQuery, ignoreCase = true) ||
                                        ayah.ayahNumber.toString() == ayahSearchQuery.trim()
                                }
                                items(filteredAyahs, key = { it.globalNumber }) { ayah ->
                                    val existingBookmark = bookmarks.firstOrNull {
                                        it.surahNumber.toInt() == ayah.surahNumber &&
                                            it.ayahNumber.toInt() == ayah.ayahNumber
                                    }
                                    val isPlayingThis = audioState.currentGlobalAyah == ayah.globalNumber &&
                                        (audioState.isPlaying || audioState.isBuffering)

                                    GlassCard(elevated = isPlayingThis) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            NoorBadgeChip(
                                                text = "আয়াত ${ayah.ayahNumber.toBn()} • পারা ${ayah.paraNumber.toBn()}",
                                                color = if (isPlayingThis) noor.success else null
                                            )
                                            Row {
                                                IconButton(onClick = { onPlayFromAyah(ayah.ayahNumber) }) {
                                                    Icon(
                                                        imageVector = if (isPlayingThis) Icons.Default.GraphicEq else Icons.Default.PlayArrow,
                                                        contentDescription = "এই আয়াত থেকে তিলাওয়াত শুনুন",
                                                        tint = if (noor.isDark) noor.accentGold else noor.emeraldPrimary
                                                    )
                                                }
                                                IconButton(onClick = { onToggleBookmark(ayah, existingBookmark) }) {
                                                    Icon(
                                                        imageVector = if (existingBookmark != null) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                                        contentDescription = "বুকমার্ক করুন",
                                                        tint = noor.accentGold
                                                    )
                                                }
                                                IconButton(onClick = {
                                                    val shareText = "${ayah.arabicText}\n\n${ayah.banglaTranslation}\n— সূরা ${selectedSurah.nameBn}, আয়াত ${ayah.ayahNumber.toBn()}"
                                                    clipboard.setText(AnnotatedString(shareText))
                                                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                                        type = "text/plain"
                                                        putExtra(Intent.EXTRA_TEXT, shareText)
                                                    }
                                                    context.startActivity(Intent.createChooser(sendIntent, "আয়াত শেয়ার করুন"))
                                                }) {
                                                    Icon(
                                                        imageVector = Icons.Default.Share,
                                                        contentDescription = "কপি ও শেয়ার",
                                                        tint = noor.textSecondary
                                                    )
                                                }
                                            }
                                        }

                                        Spacer(Modifier.height(10.dp))
                                        Text(
                                            text = ayah.arabicText,
                                            fontFamily = AmiriQuranFontFamily,
                                            fontSize = 25.sp,
                                            lineHeight = 44.sp,
                                            color = noor.textPrimary,
                                            textAlign = TextAlign.End,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        if (ayah.banglaPronunciation.isNotBlank()) {
                                            Spacer(Modifier.height(6.dp))
                                            Text(
                                                text = "উচ্চারণ সহায়ক: ${ayah.banglaPronunciation}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = noor.textSecondary
                                            )
                                        }
                                        Spacer(Modifier.height(8.dp))
                                        Text(
                                            text = ayah.banglaTranslation,
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = noor.textPrimary
                                        )
                                        Spacer(Modifier.height(8.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.End
                                        ) {
                                            TextButton(onClick = { onMarkAyahRead(ayah) }) {
                                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(Modifier.width(4.dp))
                                                Text("পড়া সম্পন্ন")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                1 -> {
                    // All 114 Surahs list + Search
                    Column(modifier = Modifier.weight(1f).padding(16.dp)) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("সূরার নাম বা নম্বর অনুসন্ধান করুন...") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp)
                        )
                        Spacer(Modifier.height(10.dp))
                        val filteredSurahs = remember(searchQuery) {
                            BengaliIslamicData.allSurahs.filter {
                                searchQuery.isBlank() ||
                                    it.nameBn.contains(searchQuery, ignoreCase = true) ||
                                    it.meaningBn.contains(searchQuery, ignoreCase = true) ||
                                    it.number.toString() == searchQuery.trim()
                            }
                        }
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(filteredSurahs, key = { it.number }) { surah ->
                                GlassCard(
                                    onClick = {
                                        onSelectSurah(surah.number)
                                        subTab = 0
                                    },
                                    elevated = surah.number == selectedSurah.number
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                            NoorBadgeChip(text = surah.number.toBn())
                                            Spacer(Modifier.width(12.dp))
                                            Column {
                                                Text(
                                                    text = "${surah.nameBn} (${surah.meaningBn})",
                                                    style = MaterialTheme.typography.titleMedium,
                                                    color = noor.textPrimary,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = "${surah.revelationBn} • ${surah.ayahCount.toBn()} আয়াত • পারা ${surah.startPara.toBn()}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = noor.textSecondary
                                                )
                                            }
                                        }
                                        Text(
                                            text = surah.nameAr,
                                            fontFamily = AmiriQuranFontFamily,
                                            fontSize = 22.sp,
                                            color = if (noor.isDark) noor.accentGold else noor.emeraldPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                2 -> {
                    // All 30 Paras navigation
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(BengaliIslamicData.allParas, key = { it.number }) { para ->
                            GlassCard(
                                onClick = {
                                    onSelectSurah(para.startSurah)
                                    subTab = 0
                                }
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = para.nameBn,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = noor.textPrimary,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = para.descriptionBn,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = noor.textSecondary
                                        )
                                    }
                                    NoorBadgeChip(text = "সূরা ${para.startSurah.toBn()}")
                                }
                            }
                        }
                    }
                }

                else -> {
                    // Bookmarks
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (bookmarks.isEmpty()) {
                            item {
                                GlassCard {
                                    NoorSectionHeader(
                                        title = "কোনো বুকমার্ক নেই",
                                        subtitle = "পছন্দের আয়াত বুকমার্ক করলে এখানে সহজে খুঁজে পাবেন।",
                                        icon = Icons.Default.MenuBook
                                    )
                                }
                            }
                        } else {
                            items(bookmarks, key = { it.id }) { bm ->
                                GlassCard(
                                    onClick = {
                                        onSelectSurah(bm.surahNumber.toInt())
                                        subTab = 0
                                    }
                                ) {
                                    Text(
                                        text = "সূরা ${bm.surahNameBn} • আয়াত ${bm.ayahNumber.toBn()} (পারা ${bm.paraNumber.toBn()})",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = if (noor.isDark) noor.accentGold else noor.emeraldPrimary
                                    )
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        text = bm.arabicText,
                                        fontFamily = AmiriQuranFontFamily,
                                        fontSize = 21.sp,
                                        color = noor.textPrimary,
                                        textAlign = TextAlign.End,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        text = bm.banglaText,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = noor.textPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Sticky Glassmorphic Audio Player Bar
            Surface(
                color = noor.glassCardElevated,
                border = BorderStroke(1.dp, noor.glassBorder),
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
                    if (!audioState.errorMessage.isNullOrBlank()) {
                        Text(
                            text = audioState.errorMessage,
                            style = MaterialTheme.typography.labelSmall,
                            color = noor.error
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            val surahName = BengaliIslamicData.allSurahs
                                .firstOrNull { it.number == audioState.currentSurahNumber }?.nameBn ?: selectedSurah.nameBn
                            Text(
                                text = "তিলাওয়াত: $surahName (আয়াত ${audioState.currentAyahNumber.toBn()})",
                                style = MaterialTheme.typography.labelLarge,
                                color = noor.textPrimary,
                                maxLines = 1
                            )
                            val qariTitle = BengaliIslamicData.qaris.firstOrNull { it.id == audioState.qariId }?.nameBn ?: ""
                            Text(
                                text = qariTitle,
                                style = MaterialTheme.typography.labelSmall,
                                color = noor.textSecondary,
                                maxLines = 1
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = onToggleRepeatAyah) {
                                Icon(
                                    imageVector = Icons.Default.RepeatOne,
                                    contentDescription = "আয়াত রিপিট",
                                    tint = if (audioState.repeatAyah) noor.accentGold else noor.textSecondary
                                )
                            }
                            IconButton(onClick = onPlayPrevious) {
                                Icon(Icons.Default.SkipPrevious, contentDescription = "পূর্ববর্তী আয়াত", tint = noor.textPrimary)
                            }
                            IconButton(onClick = onTogglePlayPause) {
                                if (audioState.isBuffering) {
                                    CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                                } else {
                                    Icon(
                                        imageVector = if (audioState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = if (audioState.isPlaying) "বিরতি" else "প্লে",
                                        tint = if (noor.isDark) noor.accentGold else noor.emeraldPrimary,
                                        modifier = Modifier.size(30.dp)
                                    )
                                }
                            }
                            IconButton(onClick = onPlayNext) {
                                Icon(Icons.Default.SkipNext, contentDescription = "পরবর্তী আয়াত", tint = noor.textPrimary)
                            }
                            IconButton(onClick = onToggleRepeatSurah) {
                                Icon(
                                    imageVector = Icons.Default.Repeat,
                                    contentDescription = "সূরা রিপিট",
                                    tint = if (audioState.repeatSurah) noor.accentGold else noor.textSecondary
                                )
                            }
                        }
                    }

                    if (audioState.durationMs > 0L) {
                        val frac = (audioState.positionMs.toFloat() / audioState.durationMs.toFloat()).coerceIn(0f, 1f)
                        Slider(
                            value = frac,
                            onValueChange = {},
                            modifier = Modifier.height(18.dp)
                        )
                    }

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        items(listOf(0.75f, 1.0f, 1.25f, 1.5f)) { speed ->
                            FilterChip(
                                selected = audioState.playbackSpeed == speed,
                                onClick = { onSetSpeed(speed) },
                                label = { Text("${speed.toBn()}x", style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                }
            }
        }
    }
}
