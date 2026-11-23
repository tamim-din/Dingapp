package com.example.ui.screens

import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SecondaryTabRow
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.BengaliIslamicData
import com.example.model.CommunityCategory
import com.example.model.CommunityPost
import com.example.model.CuratedItem
import com.example.model.IslamicQuiz
import com.example.model.QuizResult
import com.example.model.TasbihLog
import com.example.model.UiState
import com.example.model.UserProfile
import com.example.ui.components.GlassCard
import com.example.ui.components.NoorBadgeChip
import com.example.ui.components.NoorSectionHeader
import com.example.ui.components.toBn
import com.example.ui.theme.AmiriQuranFontFamily
import com.example.ui.theme.LocalNoorGlassColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KnowledgeAndCommunityScreen(
    initialTab: Int = 0,
    profileState: UiState<UserProfile?>,
    curatedHadithsState: UiState<List<CuratedItem>>,
    curatedMotivationState: UiState<List<CuratedItem>>,
    activeDhikrIndex: Int,
    customDhikrTitle: String,
    tasbihCount: Long,
    tasbihTarget: Long,
    tasbihLogsState: UiState<List<TasbihLog>>,
    communityPostsState: UiState<List<CommunityPost>>,
    quizResultsState: UiState<List<QuizResult>>,
    onSelectDhikrPreset: (Int) -> Unit,
    onSetCustomDhikr: (String, Long) -> Unit,
    onIncrementTasbih: () -> Unit,
    onResetTasbih: () -> Unit,
    onSaveTasbih: () -> Unit,
    onCreatePost: (String, CommunityCategory, String) -> Unit,
    onLikePost: (String) -> Unit,
    onReportPost: (String, String) -> Unit,
    onBlockAuthor: (String) -> Unit,
    onSubmitQuiz: (String, Int, Int) -> Unit
) {
    val noor = LocalNoorGlassColors.current
    var subTab by remember(initialTab) { mutableIntStateOf(initialTab.coerceIn(0, 2)) }
    val tabs = listOf("জ্ঞানকেন্দ্র ও দোয়া", "ডিজিটাল তাসবিহ", "কমিউনিটি ও কুইজ")

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
                0 -> KnowledgeHubTab(
                    curatedHadiths = (curatedHadithsState as? UiState.Success)?.data ?: emptyList(),
                    curatedMotivation = (curatedMotivationState as? UiState.Success)?.data ?: emptyList()
                )
                1 -> DigitalTasbihTab(
                    activeIndex = activeDhikrIndex,
                    customTitle = customDhikrTitle,
                    count = tasbihCount,
                    target = tasbihTarget,
                    logs = (tasbihLogsState as? UiState.Success)?.data ?: emptyList(),
                    onSelectPreset = onSelectDhikrPreset,
                    onSetCustom = onSetCustomDhikr,
                    onIncrement = onIncrementTasbih,
                    onReset = onResetTasbih,
                    onSave = onSaveTasbih
                )
                else -> CommunityAndQuizTab(
                    profile = (profileState as? UiState.Success)?.data,
                    postsState = communityPostsState,
                    quizResults = (quizResultsState as? UiState.Success)?.data ?: emptyList(),
                    onCreatePost = onCreatePost,
                    onLikePost = onLikePost,
                    onReportPost = onReportPost,
                    onBlockAuthor = onBlockAuthor,
                    onSubmitQuiz = onSubmitQuiz
                )
            }
        }
    }
}

@Composable
private fun KnowledgeHubTab(
    curatedHadiths: List<CuratedItem>,
    curatedMotivation: List<CuratedItem>
) {
    val noor = LocalNoorGlassColors.current
    var section by remember { mutableIntStateOf(0) }
    val chips = listOf("সহিহ হাদীস", "দোয়া ও যিকির", "প্রবন্ধ ও জীবনী")

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                itemsIndexed(chips) { idx, title ->
                    FilterChip(
                        selected = section == idx,
                        onClick = { section = idx },
                        label = { Text(title) }
                    )
                }
            }
        }

        when (section) {
            0 -> {
                items(BengaliIslamicData.verifiedHadiths, key = { it.id }) { h ->
                    GlassCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(h.titleBn, style = MaterialTheme.typography.titleMedium, color = noor.textPrimary, fontWeight = FontWeight.Bold)
                            NoorBadgeChip(text = h.gradeBn, color = noor.success)
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = h.arabicText,
                            fontFamily = AmiriQuranFontFamily,
                            fontSize = 20.sp,
                            lineHeight = 36.sp,
                            color = if (noor.isDark) noor.accentGold else noor.emeraldPrimary,
                            textAlign = TextAlign.End,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(h.banglaText, style = MaterialTheme.typography.bodyLarge, color = noor.textPrimary)
                        Spacer(Modifier.height(6.dp))
                        Text("সূত্র: ${h.referenceBn}", style = MaterialTheme.typography.labelSmall, color = noor.textSecondary)
                    }
                }
                items(curatedHadiths, key = { "c_${it.id}" }) { item ->
                    GlassCard {
                        Text(item.title, style = MaterialTheme.typography.titleMedium, color = noor.textPrimary, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(6.dp))
                        Text(item.body, style = MaterialTheme.typography.bodyLarge, color = noor.textPrimary)
                        if (item.reference.isNotBlank()) {
                            Spacer(Modifier.height(4.dp))
                            Text("সূত্র: ${item.reference}", style = MaterialTheme.typography.labelSmall, color = noor.textSecondary)
                        }
                    }
                }
            }
            1 -> {
                items(BengaliIslamicData.duasAndAdhkar, key = { it.id }) { dua ->
                    GlassCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(dua.titleBn, style = MaterialTheme.typography.titleMedium, color = noor.textPrimary, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            NoorBadgeChip(text = dua.categoryBn)
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = dua.arabicText,
                            fontFamily = AmiriQuranFontFamily,
                            fontSize = 22.sp,
                            lineHeight = 38.sp,
                            color = if (noor.isDark) noor.accentGold else noor.emeraldPrimary,
                            textAlign = TextAlign.End,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(6.dp))
                        Text("উচ্চারণ: ${dua.pronunciationBn}", style = MaterialTheme.typography.bodySmall, color = noor.textSecondary)
                        Spacer(Modifier.height(6.dp))
                        Text("অর্থ: ${dua.translationBn}", style = MaterialTheme.typography.bodyMedium, color = noor.textPrimary)
                        Spacer(Modifier.height(4.dp))
                        Text("রেফারেন্স: ${dua.referenceBn}", style = MaterialTheme.typography.labelSmall, color = noor.textSecondary)
                    }
                }
            }
            else -> {
                items(BengaliIslamicData.knowledgeArticles, key = { it.id }) { art ->
                    GlassCard {
                        NoorSectionHeader(
                            title = art.titleBn,
                            subtitle = art.categoryBn,
                            icon = Icons.Default.AutoStories
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(art.summaryBn, style = MaterialTheme.typography.titleSmall, color = if (noor.isDark) noor.accentGold else noor.emeraldPrimary)
                        Spacer(Modifier.height(6.dp))
                        Text(art.contentBn, style = MaterialTheme.typography.bodyMedium, color = noor.textPrimary)
                        Spacer(Modifier.height(6.dp))
                        Text("তথ্যসূত্র: ${art.referenceBn}", style = MaterialTheme.typography.labelSmall, color = noor.textSecondary)
                    }
                }
                items(curatedMotivation, key = { "m_${it.id}" }) { item ->
                    GlassCard {
                        Text(item.title, style = MaterialTheme.typography.titleMedium, color = noor.textPrimary, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(6.dp))
                        Text(item.body, style = MaterialTheme.typography.bodyMedium, color = noor.textPrimary)
                        if (item.reference.isNotBlank()) {
                            Spacer(Modifier.height(4.dp))
                            Text("তথ্যসূত্র: ${item.reference}", style = MaterialTheme.typography.labelSmall, color = noor.textSecondary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DigitalTasbihTab(
    activeIndex: Int,
    customTitle: String,
    count: Long,
    target: Long,
    logs: List<TasbihLog>,
    onSelectPreset: (Int) -> Unit,
    onSetCustom: (String, Long) -> Unit,
    onIncrement: () -> Unit,
    onReset: () -> Unit,
    onSave: () -> Unit
) {
    val noor = LocalNoorGlassColors.current
    val context = LocalContext.current
    var showCustomDialog by remember { mutableStateOf(false) }

    val activeTitle: String
    val activeArabic: String
    if (activeIndex >= 0 && activeIndex < BengaliIslamicData.dhikrPresets.size) {
        val p = BengaliIslamicData.dhikrPresets[activeIndex]
        activeTitle = p.first
        activeArabic = p.second
    } else {
        activeTitle = customTitle.ifBlank { "ব্যক্তিগত যিকির" }
        activeArabic = ""
    }

    val progress by animateFloatAsState(
        targetValue = (count.toFloat() / target.coerceAtLeast(1L).toFloat()).coerceIn(0f, 1f),
        label = "tasbihRing"
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                itemsIndexed(BengaliIslamicData.dhikrPresets) { idx, item ->
                    FilterChip(
                        selected = activeIndex == idx,
                        onClick = { onSelectPreset(idx) },
                        label = { Text(item.first) }
                    )
                }
                item {
                    FilterChip(
                        selected = activeIndex == -1,
                        onClick = { showCustomDialog = true },
                        label = { Text("+ নিজের যিকির") }
                    )
                }
            }
        }

        item {
            GlassCard(elevated = true) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (activeArabic.isNotBlank()) {
                        Text(
                            text = activeArabic,
                            fontFamily = AmiriQuranFontFamily,
                            fontSize = 28.sp,
                            color = if (noor.isDark) noor.accentGold else noor.emeraldPrimary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(4.dp))
                    }
                    Text(
                        text = activeTitle,
                        style = MaterialTheme.typography.titleLarge,
                        color = noor.textPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "দৈনিক লক্ষ্য: ${target.toBn()} বার",
                        style = MaterialTheme.typography.bodySmall,
                        color = noor.textSecondary
                    )

                    Spacer(Modifier.height(22.dp))

                    val ringColor = if (noor.isDark) noor.accentGold else noor.emeraldPrimary
                    val trackColor = noor.glassBorder.copy(alpha = 0.35f)
                    Box(
                        modifier = Modifier
                            .size(210.dp)
                            .clip(CircleShape)
                            .clickable {
                                onIncrement()
                                val vib = context.getSystemService(Vibrator::class.java)
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    vib?.vibrate(VibrationEffect.createOneShot(18, VibrationEffect.DEFAULT_AMPLITUDE))
                                }
                            }
                            .testTag("tasbih_tap_circle"),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawArc(
                                color = trackColor,
                                startAngle = 0f,
                                sweepAngle = 360f,
                                useCenter = false,
                                style = Stroke(width = 14.dp.toPx(), cap = StrokeCap.Round)
                            )
                            drawArc(
                                color = ringColor,
                                startAngle = -90f,
                                sweepAngle = 360f * progress,
                                useCenter = false,
                                style = Stroke(width = 14.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Spa, contentDescription = null, tint = ringColor, modifier = Modifier.size(24.dp))
                            Text(
                                text = count.toBn(),
                                style = MaterialTheme.typography.displayLarge,
                                color = noor.textPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Text("ট্যাপ করুন", style = MaterialTheme.typography.labelSmall, color = noor.textSecondary)
                        }
                    }

                    Spacer(Modifier.height(18.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onReset,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("রিসেট")
                        }
                        Button(
                            onClick = onSave,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (noor.isDark) noor.accentGold else noor.emeraldPrimary,
                                contentColor = if (noor.isDark) Color(0xFF10271F) else Color.White
                            )
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("সংরক্ষণ")
                        }
                    }
                }
            }
        }

        if (logs.isNotEmpty()) {
            item {
                GlassCard {
                    NoorSectionHeader(title = "যিকিরের ইতিহাস", icon = Icons.Default.Spa)
                    Spacer(Modifier.height(6.dp))
                    logs.take(10).forEach { log ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("${log.dhikrTitle} (${log.dateKey})", style = MaterialTheme.typography.bodySmall, color = noor.textPrimary)
                            Text("${log.count.toBn()} / ${log.targetCount.toBn()}", style = MaterialTheme.typography.labelMedium, color = noor.accentGold)
                        }
                    }
                }
            }
        }
    }

    if (showCustomDialog) {
        var title by remember { mutableStateOf("") }
        var targetText by remember { mutableStateOf("100") }
        AlertDialog(
            onDismissRequest = { showCustomDialog = false },
            title = { Text("নিজের যিকির যোগ করুন") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("যিকিরের নাম") }, singleLine = true)
                    OutlinedTextField(value = targetText, onValueChange = { targetText = it.filter { c -> c.isDigit() } }, label = { Text("দৈনিক লক্ষ্য") }, singleLine = true)
                }
            },
            confirmButton = {
                Button(onClick = {
                    val t = targetText.toLongOrNull()?.coerceIn(1L, 10000L) ?: 33L
                    if (title.isNotBlank()) {
                        onSetCustom(title, t)
                        showCustomDialog = false
                    }
                }) { Text("শুরু করুন") }
            },
            dismissButton = {
                TextButton(onClick = { showCustomDialog = false }) { Text("বাতিল") }
            }
        )
    }
}

@Composable
private fun CommunityAndQuizTab(
    profile: UserProfile?,
    postsState: UiState<List<CommunityPost>>,
    quizResults: List<QuizResult>,
    onCreatePost: (String, CommunityCategory, String) -> Unit,
    onLikePost: (String) -> Unit,
    onReportPost: (String, String) -> Unit,
    onBlockAuthor: (String) -> Unit,
    onSubmitQuiz: (String, Int, Int) -> Unit
) {
    val noor = LocalNoorGlassColors.current
    var activeQuiz by remember { mutableStateOf<IslamicQuiz?>(null) }
    var showCreatePost by remember { mutableStateOf(false) }

    val blockedIds = profile?.blockedUserIds?.toSet() ?: emptySet()
    val posts = ((postsState as? UiState.Success)?.data ?: emptyList())
        .filter { it.authorId !in blockedIds }

    if (activeQuiz != null) {
        QuizRunnerCard(
            quiz = activeQuiz!!,
            onFinish = { score, total ->
                onSubmitQuiz(activeQuiz!!.titleBn, score, total)
                activeQuiz = null
            },
            onCancel = { activeQuiz = null }
        )
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Quizzes Section
        item {
            GlassCard(elevated = true) {
                NoorSectionHeader(
                    title = "ইসলামিক কুইজ ও জ্ঞান প্রতিযোগিতা",
                    subtitle = "জ্ঞান অর্জন করুন ও গেম XP পয়েন্ট জিতুন",
                    icon = Icons.Default.Quiz
                )
                Spacer(Modifier.height(10.dp))
                BengaliIslamicData.quizzes.forEach { quiz ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(quiz.titleBn, style = MaterialTheme.typography.titleSmall, color = noor.textPrimary, fontWeight = FontWeight.Bold)
                            Text("${quiz.questions.size.toBn()}টি প্রশ্ন • ${quiz.categoryBn}", style = MaterialTheme.typography.bodySmall, color = noor.textSecondary)
                        }
                        Button(
                            onClick = { activeQuiz = quiz },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("অংশ নিন")
                        }
                    }
                }
                if (quizResults.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "সর্বশেষ স্কোর: ${quizResults.first().quizTitle} — ${quizResults.first().score.toBn()}/${quizResults.first().totalQuestions.toBn()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = noor.success
                    )
                }
            }
        }

        // Community Sharing Section
        item {
            GlassCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        NoorSectionHeader(
                            title = "অনামিকা মোটিভেশন ও ভালো কাজের অভিজ্ঞতা",
                            subtitle = "কারো ব্যক্তিগত গুনাহ বা গোপন আমল এখানে প্রকাশ করা হয় না।",
                            icon = Icons.Default.Forum
                        )
                    }
                    IconButton(onClick = { showCreatePost = true }) {
                        Icon(Icons.Default.Add, contentDescription = "পোস্ট করুন", tint = noor.accentGold)
                    }
                }
            }
        }

        if (posts.isEmpty()) {
            item {
                GlassCard {
                    Text("প্রথম কল্যাণকর মোটিভেশন বা ভালো কাজের অভিজ্ঞতা শেয়ার করুন।", color = noor.textSecondary)
                }
            }
        } else {
            items(posts, key = { it.id }) { post ->
                GlassCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(post.authorAlias, style = MaterialTheme.typography.titleSmall, color = if (noor.isDark) noor.accentGold else noor.emeraldPrimary, fontWeight = FontWeight.Bold)
                        NoorBadgeChip(text = CommunityCategory.fromRaw(post.category).labelBn)
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(post.content, style = MaterialTheme.typography.bodyMedium, color = noor.textPrimary)
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { onLikePost(post.id) }) {
                            Icon(Icons.Default.Favorite, contentDescription = "পছন্দ", modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("উৎসাহ (${post.likesCount.toBn()})")
                        }
                        Row {
                            IconButton(onClick = { onReportPost(post.id, "অনুপযুক্ত কন্টেন্ট") }) {
                                Icon(Icons.Default.Flag, contentDescription = "রিপোর্ট", tint = noor.warning, modifier = Modifier.size(18.dp))
                            }
                            IconButton(onClick = { onBlockAuthor(post.authorId) }) {
                                Icon(Icons.Default.Block, contentDescription = "ব্লক করুন", tint = noor.error, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreatePost) {
        var alias by remember { mutableStateOf(profile?.displayName?.take(20) ?: "অনামিকা মুসাফির") }
        var category by remember { mutableStateOf(CommunityCategory.MOTIVATION) }
        var content by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreatePost = false },
            title = { Text("ইতিবাচক অভিজ্ঞতা শেয়ার করুন") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = alias, onValueChange = { alias = it }, label = { Text("ছদ্মনাম বা নাম") }, singleLine = true)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(CommunityCategory.entries) { cat ->
                            FilterChip(selected = category == cat, onClick = { category = cat }, label = { Text(cat.labelBn) })
                        }
                    }
                    OutlinedTextField(value = content, onValueChange = { content = it }, label = { Text("মোটিভেশন বা ভালো কাজের বার্তা") }, minLines = 3)
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (content.trim().length >= 3) {
                        onCreatePost(alias, category, content)
                        showCreatePost = false
                    }
                }) { Text("প্রকাশ করুন") }
            },
            dismissButton = {
                TextButton(onClick = { showCreatePost = false }) { Text("বাতিল") }
            }
        )
    }
}

@Composable
private fun QuizRunnerCard(
    quiz: IslamicQuiz,
    onFinish: (Int, Int) -> Unit,
    onCancel: () -> Unit
) {
    val noor = LocalNoorGlassColors.current
    var qIndex by remember { mutableIntStateOf(0) }
    var score by remember { mutableIntStateOf(0) }
    var selectedOpt by remember { mutableStateOf<Int?>(null) }

    val question = quiz.questions[qIndex]

    Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
        GlassCard(elevated = true, modifier = Modifier.widthIn(max = 580.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(quiz.titleBn, style = MaterialTheme.typography.titleMedium, color = noor.textPrimary, fontWeight = FontWeight.Bold)
                TextButton(onClick = onCancel) { Text("বন্ধ করুন") }
            }
            Text("প্রশ্ন ${(qIndex + 1).toBn()} / ${quiz.questions.size.toBn()}", style = MaterialTheme.typography.labelSmall, color = noor.accentGold)
            Spacer(Modifier.height(10.dp))
            Text(question.questionBn, style = MaterialTheme.typography.titleLarge, color = noor.textPrimary)
            Spacer(Modifier.height(12.dp))

            question.optionsBn.forEachIndexed { idx, opt ->
                val isSelected = selectedOpt == idx
                val isCorrect = idx == question.correctIndex
                val borderColor = when {
                    selectedOpt == null -> noor.glassBorder
                    isCorrect -> noor.success
                    isSelected -> noor.error
                    else -> noor.glassBorder
                }
                OutlinedButton(
                    onClick = {
                        if (selectedOpt == null) {
                            selectedOpt = idx
                            if (idx == question.correctIndex) score++
                        }
                    },
                    border = BorderStroke(1.5.dp, borderColor),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Text(opt, color = noor.textPrimary, modifier = Modifier.fillMaxWidth())
                }
            }

            if (selectedOpt != null) {
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "${question.explanationBn} (${question.referenceBn})",
                    style = MaterialTheme.typography.bodySmall,
                    color = noor.textSecondary
                )
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = {
                        if (qIndex < quiz.questions.lastIndex) {
                            qIndex++
                            selectedOpt = null
                        } else {
                            onFinish(score, quiz.questions.size)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (qIndex < quiz.questions.lastIndex) "পরবর্তী প্রশ্ন" else "ফলাফল সংরক্ষণ করুন")
                }
            }
        }
    }
}
