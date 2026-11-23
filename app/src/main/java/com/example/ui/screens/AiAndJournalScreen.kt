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
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Share
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.data.NoorRepository
import com.example.model.AiConversation
import com.example.model.AiMessage
import com.example.model.PrivateJournal
import com.example.model.UiState
import com.example.model.UserProfile
import com.example.ui.components.GlassCard
import com.example.ui.components.NoorBadgeChip
import com.example.ui.components.NoorSectionHeader
import com.example.ui.theme.LocalNoorGlassColors
import com.example.viewmodel.AiChatUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiAndJournalScreen(
    profileState: UiState<UserProfile?>,
    conversationsState: UiState<List<AiConversation>>,
    selectedConversationId: String?,
    activeMessagesState: UiState<List<AiMessage>>,
    aiChatUiState: AiChatUiState,
    journalsState: UiState<List<PrivateJournal>>,
    onSelectConversation: (String?) -> Unit,
    onSendAiPrompt: (String, List<AiMessage>) -> Unit,
    onDeleteConversation: (String) -> Unit,
    onSetJournalPin: (String) -> Unit,
    onAddJournal: (String, String, String, String, String) -> Unit,
    onDeleteJournal: (String) -> Unit
) {
    val noor = LocalNoorGlassColors.current
    var subTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("কৃত্রিম বুদ্ধিমত্তার সহকারী", "ব্যক্তিগত জার্নাল")

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

            if (subTab == 0) {
                AiAssistantTab(
                    conversationsState = conversationsState,
                    selectedConversationId = selectedConversationId,
                    activeMessagesState = activeMessagesState,
                    aiChatUiState = aiChatUiState,
                    onSelectConversation = onSelectConversation,
                    onSendAiPrompt = onSendAiPrompt,
                    onDeleteConversation = onDeleteConversation
                )
            } else {
                PrivateJournalTab(
                    profile = (profileState as? UiState.Success)?.data,
                    journalsState = journalsState,
                    onSetJournalPin = onSetJournalPin,
                    onAddJournal = onAddJournal,
                    onDeleteJournal = onDeleteJournal
                )
            }
        }
    }
}

@Composable
private fun AiAssistantTab(
    conversationsState: UiState<List<AiConversation>>,
    selectedConversationId: String?,
    activeMessagesState: UiState<List<AiMessage>>,
    aiChatUiState: AiChatUiState,
    onSelectConversation: (String?) -> Unit,
    onSendAiPrompt: (String, List<AiMessage>) -> Unit,
    onDeleteConversation: (String) -> Unit
) {
    val noor = LocalNoorGlassColors.current
    var promptText by remember { mutableStateOf("") }
    val conversations = (conversationsState as? UiState.Success)?.data ?: emptyList()
    val messages = (activeMessagesState as? UiState.Success)?.data ?: emptyList()

    val quickPrompts = remember {
        listOf(
            "তওবা করার সঠিক নিয়ম কী?",
            "হতাশার সময় আল্লাহর ওপর ভরসা রাখার উপায় বলুন",
            "দৃষ্টি হেফাজতের জন্য কুরআন ও হাদীসের পরামর্শ দিন",
            "সূরা আল-আসরের তাফসীর ও শিক্ষা ব্যাখ্যা করুন"
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Conversation chips bar
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            item {
                FilterChip(
                    selected = selectedConversationId == null,
                    onClick = { onSelectConversation(null) },
                    label = { Text("+ নতুন চ্যাট") }
                )
            }
            items(conversations, key = { it.id }) { conv ->
                FilterChip(
                    selected = selectedConversationId == conv.id,
                    onClick = { onSelectConversation(conv.id) },
                    label = { Text(conv.title.take(18)) },
                    trailingIcon = {
                        IconButton(
                            onClick = { onDeleteConversation(conv.id) },
                            modifier = Modifier.size(18.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "চ্যাট মুছুন", modifier = Modifier.size(14.dp))
                        }
                    }
                )
            }
        }

        // Messages area
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                GlassCard {
                    NoorSectionHeader(
                        title = "নূর ইসলামিক AI সহকারী",
                        subtitle = "সতর্কতা: AI কোনো মুফতি বা আলেম নয়—জটিল মাসআলায় যোগ্য আলেমের পরামর্শ নিন।",
                        icon = Icons.Default.AutoAwesome
                    )
                    if (messages.isEmpty()) {
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = "ইসলামিক প্রশ্ন, কুরআনের আয়াত বা হাদীস ব্যাখ্যা, তওবা, আত্মউন্নয়ন কিংবা মনের কথা শেয়ার করুন:",
                            style = MaterialTheme.typography.bodySmall,
                            color = noor.textSecondary
                        )
                        Spacer(Modifier.height(8.dp))
                        quickPrompts.forEach { qp ->
                            OutlinedButton(
                                onClick = { onSendAiPrompt(qp, messages) },
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, noor.glassBorder),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp)
                            ) {
                                Text(qp, color = noor.textPrimary, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }

            items(messages, key = { it.id }) { msg ->
                val isUser = msg.role == "USER"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                ) {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = if (isUser) {
                            if (noor.isDark) noor.emeraldPrimary else noor.emeraldPrimary
                        } else {
                            noor.glassCardElevated
                        },
                        border = BorderStroke(1.dp, noor.glassBorder),
                        modifier = Modifier.widthIn(max = 560.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = if (isUser) "আপনি" else "নূর সহকারী",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isUser) Color(0xFFE6D3A1) else noor.accentGold,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = msg.content,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isUser) Color(0xFFF5F1E8) else noor.textPrimary
                            )
                        }
                    }
                }
            }

            when (aiChatUiState) {
                is AiChatUiState.Loading -> {
                    item {
                        GlassCard {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                Spacer(Modifier.width(10.dp))
                                Text("রেফারেন্সসহ উত্তর প্রস্তুত হচ্ছে...", color = noor.textPrimary)
                            }
                        }
                    }
                }
                is AiChatUiState.Streaming -> {
                    item {
                        GlassCard(elevated = true) {
                            Text(
                                text = "নূর সহকারী",
                                style = MaterialTheme.typography.labelSmall,
                                color = noor.accentGold,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(aiChatUiState.text, style = MaterialTheme.typography.bodyMedium, color = noor.textPrimary)
                        }
                    }
                }
                is AiChatUiState.Error -> {
                    item {
                        GlassCard {
                            Text(aiChatUiState.message, color = noor.error, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                AiChatUiState.Idle -> Unit
            }
        }

        // Input Bar
        Surface(
            color = noor.glassCardElevated,
            border = BorderStroke(1.dp, noor.glassBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = promptText,
                    onValueChange = { promptText = it },
                    placeholder = { Text("আপনার প্রশ্ন বা অনুভূতি লিখুন...") },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("ai_prompt_input"),
                    maxLines = 3,
                    shape = RoundedCornerShape(16.dp)
                )
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = {
                        val toSend = promptText
                        promptText = ""
                        onSendAiPrompt(toSend, messages)
                    },
                    enabled = promptText.isNotBlank() && aiChatUiState !is AiChatUiState.Loading && aiChatUiState !is AiChatUiState.Streaming,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .height(52.dp)
                        .testTag("ai_send_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (noor.isDark) noor.accentGold else noor.emeraldPrimary,
                        contentColor = if (noor.isDark) Color(0xFF10271F) else Color.White
                    )
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "পাঠান")
                }
            }
        }
    }
}

@Composable
private fun PrivateJournalTab(
    profile: UserProfile?,
    journalsState: UiState<List<PrivateJournal>>,
    onSetJournalPin: (String) -> Unit,
    onAddJournal: (String, String, String, String, String) -> Unit,
    onDeleteJournal: (String) -> Unit
) {
    val noor = LocalNoorGlassColors.current
    val context = LocalContext.current
    val pinHash = profile?.journalPinHash.orEmpty()
    var unlocked by remember(pinHash) { mutableStateOf(pinHash.isBlank()) }
    var pinEntry by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf<String?>(null) }
    var showPinSetupDialog by remember { mutableStateOf(false) }
    var showNewEntryDialog by remember { mutableStateOf(false) }

    if (!unlocked && pinHash.isNotBlank()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            GlassCard(
                elevated = true,
                modifier = Modifier
                    .padding(24.dp)
                    .widthIn(max = 420.dp)
            ) {
                NoorSectionHeader(
                    title = "ব্যক্তিগত জার্নাল লক করা আছে",
                    subtitle = "আপনার ব্যক্তিগত নোট দেখতে ৪–৬ সংখ্যার PIN দিন।",
                    icon = Icons.Default.Lock
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = pinEntry,
                    onValueChange = { pinEntry = it.filter { c -> c.isDigit() }.take(6) },
                    label = { Text("গোপন PIN") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (!pinError.isNullOrBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text(pinError.orEmpty(), color = noor.error, style = MaterialTheme.typography.bodySmall)
                }
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = {
                        if (NoorRepository.sha256(pinEntry) == pinHash) {
                            pinError = null
                            unlocked = true
                        } else {
                            pinError = "ভুল PIN দেওয়া হয়েছে।"
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.LockOpen, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("আনলক করুন")
                }
            }
        }
        return
    }

    val journals = (journalsState as? UiState.Success)?.data ?: emptyList()

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
                            "ব্যক্তিগত জার্নাল ও কৃতজ্ঞতা",
                            style = MaterialTheme.typography.titleLarge,
                            color = noor.textPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "সম্পূর্ণ ব্যক্তিগত—শুধুমাত্র আপনার অ্যাকাউন্টেই সংরক্ষিত থাকে।",
                            style = MaterialTheme.typography.bodySmall,
                            color = noor.textSecondary
                        )
                    }
                    Row {
                        IconButton(onClick = { showPinSetupDialog = true }) {
                            Icon(
                                imageVector = if (pinHash.isNotBlank()) Icons.Default.Lock else Icons.Default.LockOpen,
                                contentDescription = "PIN লক সেটিংস",
                                tint = noor.accentGold
                            )
                        }
                        IconButton(onClick = {
                            val exportText = journals.joinToString("\n\n---\n\n") { j ->
                                "${j.dateKey} — ${j.title}\n${j.content}\nকৃতজ্ঞতা: ${j.gratitudeNote}\nঅভ্যাস ভাবনা: ${j.habitReflection}"
                            }
                            val share = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, exportText.ifBlank { "কোনো জার্নাল নোট নেই।" })
                            }
                            context.startActivity(Intent.createChooser(share, "জার্নাল এক্সপোর্ট করুন"))
                        }) {
                            Icon(Icons.Default.Share, contentDescription = "ডেটা এক্সপোর্ট", tint = noor.textPrimary)
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = { showNewEntryDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (noor.isDark) noor.accentGold else noor.emeraldPrimary,
                        contentColor = if (noor.isDark) Color(0xFF10271F) else Color.White
                    )
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("নতুন জার্নাল লিখুন")
                }
            }
        }

        if (journals.isEmpty()) {
            item {
                GlassCard {
                    Text(
                        "আজকের কৃতজ্ঞতা, মনের কথা বা আত্মউন্নয়নের ভাবনা লিখে রাখুন।",
                        color = noor.textSecondary
                    )
                }
            }
        } else {
            items(journals, key = { it.id }) { item ->
                GlassCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.title, style = MaterialTheme.typography.titleMedium, color = noor.textPrimary, fontWeight = FontWeight.Bold)
                            Text("${item.dateKey} • ${item.moodTag}", style = MaterialTheme.typography.labelSmall, color = noor.accentGold)
                        }
                        IconButton(onClick = { onDeleteJournal(item.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = "নোট মুছুন", tint = noor.error)
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(item.content, style = MaterialTheme.typography.bodyMedium, color = noor.textPrimary)
                    if (item.gratitudeNote.isNotBlank()) {
                        Spacer(Modifier.height(6.dp))
                        NoorBadgeChip(text = "কৃতজ্ঞতা: ${item.gratitudeNote}", color = noor.success)
                    }
                    if (item.habitReflection.isNotBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Text("আত্মউন্নয়ন: ${item.habitReflection}", style = MaterialTheme.typography.bodySmall, color = noor.textSecondary)
                    }
                }
            }
        }
    }

    if (showNewEntryDialog) {
        var title by remember { mutableStateOf("") }
        var content by remember { mutableStateOf("") }
        var gratitude by remember { mutableStateOf("") }
        var habit by remember { mutableStateOf("") }
        var mood by remember { mutableStateOf("প্রশান্ত") }
        AlertDialog(
            onDismissRequest = { showNewEntryDialog = false },
            title = { Text("নতুন ব্যক্তিগত নোট") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("শিরোনাম") }, singleLine = true)
                    OutlinedTextField(value = content, onValueChange = { content = it }, label = { Text("মনের কথা") }, minLines = 3)
                    OutlinedTextField(value = gratitude, onValueChange = { gratitude = it }, label = { Text("আজকের কৃতজ্ঞতা (আলহামদুলিল্লাহ)") })
                    OutlinedTextField(value = habit, onValueChange = { habit = it }, label = { Text("অভ্যাস ও লক্ষ্য") })
                    OutlinedTextField(value = mood, onValueChange = { mood = it }, label = { Text("অনুভূতি (শান্ত/কৃতজ্ঞ/আশাবাদী)") }, singleLine = true)
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (content.isNotBlank()) {
                        onAddJournal(title, content, gratitude, habit, mood)
                        showNewEntryDialog = false
                    }
                }) { Text("সংরক্ষণ") }
            },
            dismissButton = {
                TextButton(onClick = { showNewEntryDialog = false }) { Text("বাতিল") }
            }
        )
    }

    if (showPinSetupDialog) {
        var newPin by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showPinSetupDialog = false },
            title = { Text("জার্নাল PIN লক সেটিংস") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("নতুন ৪–৬ সংখ্যার PIN দিন অথবা লক বন্ধ করতে খালি রেখে সংরক্ষণ করুন।", style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(
                        value = newPin,
                        onValueChange = { newPin = it.filter { c -> c.isDigit() }.take(6) },
                        label = { Text("PIN (৪–৬ সংখ্যা)") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    onSetJournalPin(newPin)
                    showPinSetupDialog = false
                }) { Text("সংরক্ষণ") }
            },
            dismissButton = {
                TextButton(onClick = { showPinSetupDialog = false }) { Text("বাতিল") }
            }
        )
    }
}
