package com.example.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

enum class MissionCategory(val labelBn: String) {
    IBADAT("ইবাদত"),
    CHARACTER("চরিত্র"),
    SELF_CONTROL("আত্মসংযম"),
    GOOD_DEEDS("ভালো কাজ");

    companion object {
        fun fromRaw(raw: String?): MissionCategory =
            entries.firstOrNull { it.name == raw } ?: IBADAT
    }
}

enum class MissionSchedule(val labelBn: String) {
    DAILY("দৈনিক"),
    WEEKLY("সাপ্তাহিক"),
    MONTHLY("মাসিক");

    companion object {
        fun fromRaw(raw: String?): MissionSchedule =
            entries.firstOrNull { it.name == raw } ?: DAILY
    }
}

enum class CommunityCategory(val labelBn: String) {
    MOTIVATION("মোটিভেশন"),
    GOOD_DEED("ভালো কাজের অভিজ্ঞতা"),
    REFLECTION("আত্মউন্নয়ন ভাবনা");

    companion object {
        fun fromRaw(raw: String?): CommunityCategory =
            entries.firstOrNull { it.name == raw } ?: MOTIVATION
    }
}

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val username: String = "",
    val email: String = "",
    val photoUrl: String = "",
    val country: String = "বাংলাদেশ",
    val timezone: String = "Asia/Dhaka",
    val locationName: String = "ঢাকা",
    val themeMode: String = "AUTO",
    val role: String = "user",
    val accountStatus: String = "ACTIVE",
    val xp: Long = 0L,
    val level: Long = 1L,
    val streakDays: Long = 0L,
    val activeDaysCount: Long = 1L,
    val dailyQuranGoalPages: Long = 4L,
    val dailyTasbihGoal: Long = 100L,
    val dailyScreenGoalMinutes: Long = 120L,
    val journalPinHash: String = "",
    val blockedUserIds: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class UserMission(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val category: String = MissionCategory.IBADAT.name,
    val schedule: String = MissionSchedule.DAILY.name,
    val xpReward: Long = 20L,
    val custom: Boolean = false,
    val completed: Boolean = false,
    val completedCount: Long = 0L,
    val lastCompletedDate: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class MissionHistory(
    @DocumentId val id: String = "",
    val userId: String = "",
    val missionId: String = "",
    val title: String = "",
    val category: String = "",
    val xpEarned: Long = 0L,
    val dateKey: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class PrayerLog(
    @DocumentId val id: String = "",
    val userId: String = "",
    val dateKey: String = "",
    val fajr: Boolean = false,
    val dhuhr: Boolean = false,
    val asr: Boolean = false,
    val maghrib: Boolean = false,
    val isha: Boolean = false,
    val tahajjud: Boolean = false,
    val naflCount: Long = 0L,
    val locationName: String = "ঢাকা",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class QuranBookmark(
    @DocumentId val id: String = "",
    val userId: String = "",
    val surahNumber: Long = 1L,
    val surahNameBn: String = "",
    val ayahNumber: Long = 1L,
    val paraNumber: Long = 1L,
    val arabicText: String = "",
    val banglaText: String = "",
    val note: String = "",
    val favorite: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class QuranProgress(
    @DocumentId val id: String = "",
    val userId: String = "",
    val lastSurahNumber: Long = 1L,
    val lastSurahNameBn: String = "আল-ফাতিহা",
    val lastAyahNumber: Long = 1L,
    val lastParaNumber: Long = 1L,
    val lastAudioQariId: String = "ar.alafasy",
    val totalAyahsRead: Long = 0L,
    val dailyAyahsRead: Long = 0L,
    val dailyGoalAyahs: Long = 15L,
    val lastReadDateKey: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class AiConversation(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val lastMessagePreview: String = "",
    val messageCount: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class AiMessage(
    @DocumentId val id: String = "",
    val userId: String = "",
    val conversationId: String = "",
    val role: String = "USER",
    val content: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class PrivateJournal(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val content: String = "",
    val gratitudeNote: String = "",
    val habitReflection: String = "",
    val moodTag: String = "শান্ত",
    val dateKey: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class TasbihLog(
    @DocumentId val id: String = "",
    val userId: String = "",
    val dhikrTitle: String = "",
    val arabicText: String = "",
    val count: Long = 0L,
    val targetCount: Long = 33L,
    val dateKey: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class UserNotification(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val body: String = "",
    val category: String = "সাধারণ",
    val read: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CommunityPost(
    @DocumentId val id: String = "",
    val authorId: String = "",
    val authorAlias: String = "",
    val category: String = CommunityCategory.MOTIVATION.name,
    val content: String = "",
    val likesCount: Long = 0L,
    val reportedCount: Long = 0L,
    val moderatedHidden: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CommunityReport(
    @DocumentId val id: String = "",
    val reporterId: String = "",
    val postId: String = "",
    val reason: String = "",
    val status: String = "OPEN",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class QuizResult(
    @DocumentId val id: String = "",
    val userId: String = "",
    val quizTitle: String = "",
    val score: Long = 0L,
    val totalQuestions: Long = 0L,
    val xpEarned: Long = 0L,
    val dateKey: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CuratedItem(
    @DocumentId val id: String = "",
    val title: String = "",
    val body: String = "",
    val reference: String = "",
    val category: String = "",
    val extra: String = "",
    val active: Boolean = true,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

// Domain Models for Quran, Prayer, Knowledge & Quizzes

data class SurahMeta(
    val number: Int,
    val nameAr: String,
    val nameBn: String,
    val meaningBn: String,
    val revelationBn: String,
    val ayahCount: Int,
    val startPara: Int
)

data class ParaMeta(
    val number: Int,
    val nameBn: String,
    val startSurah: Int,
    val startAyah: Int,
    val descriptionBn: String
)

data class QuranAyah(
    val surahNumber: Int,
    val ayahNumber: Int,
    val globalNumber: Int,
    val paraNumber: Int,
    val arabicText: String,
    val banglaTranslation: String,
    val banglaPronunciation: String,
    val audioUrl: String
)

data class QariInfo(
    val id: String,
    val nameBn: String,
    val styleBn: String,
    val cdnBitratePath: String
)

data class HadithItem(
    val id: String,
    val titleBn: String,
    val arabicText: String,
    val banglaText: String,
    val referenceBn: String,
    val gradeBn: String,
    val categoryBn: String
)

data class DuaItem(
    val id: String,
    val titleBn: String,
    val categoryBn: String,
    val arabicText: String,
    val pronunciationBn: String,
    val translationBn: String,
    val referenceBn: String
)

data class KnowledgeArticle(
    val id: String,
    val titleBn: String,
    val categoryBn: String,
    val summaryBn: String,
    val contentBn: String,
    val referenceBn: String
)

data class QuizQuestion(
    val questionBn: String,
    val optionsBn: List<String>,
    val correctIndex: Int,
    val explanationBn: String,
    val referenceBn: String
)

data class IslamicQuiz(
    val id: String,
    val titleBn: String,
    val categoryBn: String,
    val questions: List<QuizQuestion>
)

data class DnsProviderInfo(
    val nameBn: String,
    val hostname: String,
    val ipv4Primary: String,
    val ipv4Secondary: String,
    val descriptionBn: String
)

data class PrayerSchedule(
    val locationName: String,
    val dateTextBn: String,
    val hijriDateTextBn: String,
    val fajr: String,
    val sunrise: String,
    val dhuhr: String,
    val asr: String,
    val maghrib: String,
    val isha: String,
    val sehriEnd: String,
    val iftarTime: String,
    val nextPrayerNameBn: String,
    val nextPrayerTime: String,
    val remainingTimeBn: String
)

data class CityCoordinates(
    val nameBn: String,
    val latitude: Double,
    val longitude: Double,
    val utcOffsetHours: Double
)

data class AchievementBadge(
    val id: String,
    val titleBn: String,
    val descriptionBn: String,
    val requiredXp: Long,
    val unlocked: Boolean
)

enum class DnsProtectionStatus(val labelBn: String) {
    ACTIVE_FAMILY("সক্রিয় (ফ্যামিলি-সেফ DNS)"),
    CONFIGURED_STANDARD("কনফিগার করা হয়েছে (সাধারণ DNS)"),
    INACTIVE("নিষ্ক্রিয়"),
    UNAVAILABLE("উপলব্ধ নয়")
}

data class DnsCheckResult(
    val status: DnsProtectionStatus,
    val activeHostname: String?,
    val dohReachable: Boolean,
    val detailsBn: String
)
