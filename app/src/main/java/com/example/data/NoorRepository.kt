package com.example.data

import android.content.Context
import com.example.R
import com.example.firebase.OperationType
import com.example.firebase.handleFirestoreError
import com.example.model.AiConversation
import com.example.model.AiMessage
import com.example.model.CommunityCategory
import com.example.model.CommunityPost
import com.example.model.CommunityReport
import com.example.model.CuratedItem
import com.example.model.MissionCategory
import com.example.model.MissionHistory
import com.example.model.MissionSchedule
import com.example.model.PrayerLog
import com.example.model.PrivateJournal
import com.example.model.QuranBookmark
import com.example.model.QuranProgress
import com.example.model.QuizResult
import com.example.model.TasbihLog
import com.example.model.UserMission
import com.example.model.UserNotification
import com.example.model.UserProfile
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.security.MessageDigest

class NoorRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("ডাটা অ্যাক্সেস করার আগে গুগল অ্যাকাউন্ট দিয়ে সাইন-ইন করুন।")
    }

    private fun <T> Flow<T>.wrapFirestoreErrors(
        uid: String,
        operationType: OperationType,
        path: String
    ): Flow<T> = catch { error ->
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && auth.currentUser?.uid == uid) {
            handleFirestoreError(exceptionToLog, operationType, path)
        }
        throw error
    }

    // --- User Profile & Bootstrap ---

    suspend fun ensureUserProfileAndDefaults(firebaseUser: FirebaseUser): Result<Unit> {
        val uid = firebaseUser.uid
        val userDocRef = db.collection("users").document(uid)
        return try {
            val snap = userDocRef.get().await()
            if (!snap.exists()) {
                val safeDisplayName = (firebaseUser.displayName?.takeIf { it.isNotBlank() } ?: "নতুন মুসলিম")
                    .take(80)
                val email = (firebaseUser.email?.takeIf { it.isNotBlank() } ?: "$uid@users.noor")
                    .take(120)
                val rawUsername = email.substringBefore("@").replace(Regex("[^a-zA-Z0-9_]"), "")
                    .ifBlank { "noor_user" }
                    .take(36)
                val username = if (rawUsername.length < 2) "noor_$rawUsername" else rawUsername
                val payload = mapOf(
                    "userId" to uid,
                    "displayName" to safeDisplayName,
                    "username" to username,
                    "email" to email,
                    "photoUrl" to (firebaseUser.photoUrl?.toString()?.take(500) ?: ""),
                    "country" to "বাংলাদেশ",
                    "timezone" to "Asia/Dhaka",
                    "locationName" to "ঢাকা",
                    "themeMode" to "AUTO",
                    "role" to "user",
                    "accountStatus" to "ACTIVE",
                    "xp" to 0L,
                    "level" to 1L,
                    "streakDays" to 1L,
                    "activeDaysCount" to 1L,
                    "dailyQuranGoalPages" to 4L,
                    "dailyTasbihGoal" to 100L,
                    "dailyScreenGoalMinutes" to 120L,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                userDocRef.set(payload).await()
            }

            // Seed default missions once if empty
            val missionsSnap = userDocRef.collection("missions").limit(1).get().await()
            if (missionsSnap.isEmpty) {
                val batch = db.batch()
                BengaliIslamicData.defaultMissionSeeds.forEachIndexed { idx, (title, cat, schedXp) ->
                    val docRef = userDocRef.collection("missions").document("seed_${idx + 1}")
                    batch.set(
                        docRef,
                        mapOf(
                            "userId" to uid,
                            "title" to title,
                            "category" to cat.name,
                            "schedule" to schedXp.first.name,
                            "xpReward" to schedXp.second,
                            "custom" to false,
                            "completed" to false,
                            "completedCount" to 0L,
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
                batch.commit().await()
            }

            // Seed Quran progress doc if absent
            val progressDoc = userDocRef.collection("quran_progress").document("current")
            if (!progressDoc.get().await().exists()) {
                progressDoc.set(
                    mapOf(
                        "userId" to uid,
                        "lastSurahNumber" to 1L,
                        "lastSurahNameBn" to "আল-ফাতিহা",
                        "lastAyahNumber" to 1L,
                        "lastParaNumber" to 1L,
                        "lastAudioQariId" to "ar.alafasy",
                        "totalAyahsRead" to 0L,
                        "dailyAyahsRead" to 0L,
                        "dailyGoalAyahs" to 15L,
                        "lastReadDateKey" to PrayerTimeCalculator.todayDateKey(),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }

            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, userDocRef.path)
            Result.failure(e)
        }
    }

    fun observeUserProfile(userId: String): Flow<UserProfile?> = flow {
        val path = "users/$userId"
        emitAll(
            db.collection("users").document(userId)
                .snapshots()
                .map { it.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .wrapFirestoreErrors(userId, OperationType.GET, path)
        )
    }

    suspend fun checkIsAdmin(userId: String): Boolean {
        val email = auth.currentUser?.email
        val verified = auth.currentUser?.isEmailVerified == true
        if (verified && email == "tamimryl@gmail.com") return true
        return try {
            db.collection("admins").document(userId).get().await().exists()
        } catch (_: Exception) {
            false
        }
    }

    suspend fun updateProfileFields(
        displayName: String,
        username: String,
        country: String,
        locationName: String,
        dailyQuranGoalPages: Long,
        dailyTasbihGoal: Long,
        dailyScreenGoalMinutes: Long,
        photoUrl: String? = null
    ): Result<Unit> {
        var path = "users"
        return try {
            val uid = requireUserId()
            val cleanName = displayName.trim().take(80)
            val cleanUsername = username.trim().take(40)
            require(cleanName.isNotEmpty()) { "নাম খালি রাখা যাবে না।" }
            require(cleanUsername.length >= 2) { "ইউজারনেম কমপক্ষে ২ অক্ষরের হতে হবে।" }
            val ref = db.collection("users").document(uid)
            path = ref.path
            val updates = mutableMapOf<String, Any>(
                "displayName" to cleanName,
                "username" to cleanUsername,
                "country" to country.trim().ifBlank { "বাংলাদেশ" }.take(60),
                "locationName" to locationName.trim().ifBlank { "ঢাকা" }.take(80),
                "dailyQuranGoalPages" to dailyQuranGoalPages.coerceIn(1L, 100L),
                "dailyTasbihGoal" to dailyTasbihGoal.coerceIn(10L, 10000L),
                "dailyScreenGoalMinutes" to dailyScreenGoalMinutes.coerceIn(15L, 720L),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (photoUrl != null) {
                updates["photoUrl"] = photoUrl.take(500)
            }
            ref.update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateThemeMode(themeModeName: String): Result<Unit> {
        var path = "users"
        return try {
            val uid = requireUserId()
            val ref = db.collection("users").document(uid)
            path = ref.path
            ref.update(
                mapOf(
                    "themeMode" to themeModeName,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateJournalPin(pinPlain: String): Result<Unit> {
        var path = "users"
        return try {
            val uid = requireUserId()
            val hash = if (pinPlain.isBlank()) "" else sha256(pinPlain)
            val ref = db.collection("users").document(uid)
            path = ref.path
            ref.update(
                mapOf(
                    "journalPinHash" to hash,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun blockCommunityAuthor(authorId: String): Result<Unit> {
        var path = "users"
        return try {
            val uid = requireUserId()
            val ref = db.collection("users").document(uid)
            path = ref.path
            ref.update(
                mapOf(
                    "blockedUserIds" to FieldValue.arrayUnion(authorId),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun awardXp(xpGain: Long): Result<Unit> {
        if (xpGain <= 0L) return Result.success(Unit)
        var path = "users"
        return try {
            val uid = requireUserId()
            val ref = db.collection("users").document(uid)
            path = ref.path
            val snap = ref.get().await()
            val currentXp = (snap.getLong("xp") ?: 0L).coerceAtLeast(0L)
            val newXp = currentXp + xpGain
            val newLevel = (1L + (newXp / 100L)).coerceIn(1L, 100L)
            ref.update(
                mapOf(
                    "xp" to newXp,
                    "level" to newLevel,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    // --- Missions & History ---

    fun observeMissions(userId: String): Flow<List<UserMission>> = flow {
        val path = "users/$userId/missions"
        emitAll(
            db.collection("users").document(userId).collection("missions")
                .snapshots()
                .map { snap ->
                    snap.toObjects(UserMission::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.completed }
                }
                .wrapFirestoreErrors(userId, OperationType.LIST, path)
        )
    }

    fun observeMissionHistory(userId: String): Flow<List<MissionHistory>> = flow {
        val path = "users/$userId/mission_history"
        emitAll(
            db.collection("users").document(userId).collection("mission_history")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(50)
                .snapshots()
                .map { snap ->
                    snap.toObjects(MissionHistory::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .wrapFirestoreErrors(userId, OperationType.LIST, path)
        )
    }

    suspend fun addCustomMission(
        title: String,
        category: MissionCategory,
        schedule: MissionSchedule,
        xpReward: Long
    ): Result<Unit> {
        var path = "users"
        return try {
            val uid = requireUserId()
            val cleanTitle = title.trim().take(120)
            require(cleanTitle.isNotEmpty()) { "মিশনের নাম লিখুন।" }
            val docRef = db.collection("users").document(uid).collection("missions").document()
            path = docRef.path
            docRef.set(
                mapOf(
                    "userId" to uid,
                    "title" to cleanTitle,
                    "category" to category.name,
                    "schedule" to schedule.name,
                    "xpReward" to xpReward.coerceIn(5L, 500L),
                    "custom" to true,
                    "completed" to false,
                    "completedCount" to 0L,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun toggleMissionComplete(mission: UserMission): Result<Unit> {
        var path = "users"
        return try {
            val uid = requireUserId()
            val docRef = db.collection("users").document(uid).collection("missions").document(mission.id)
            path = docRef.path
            val today = PrayerTimeCalculator.todayDateKey()
            val nowCompleted = !mission.completed
            val newCount = if (nowCompleted) mission.completedCount + 1L else mission.completedCount
            val updates = mutableMapOf<String, Any>(
                "completed" to nowCompleted,
                "completedCount" to newCount,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (nowCompleted) {
                updates["lastCompletedDate"] = today
            }
            docRef.update(updates).await()

            if (nowCompleted) {
                db.collection("users").document(uid).collection("mission_history").document().set(
                    mapOf(
                        "userId" to uid,
                        "missionId" to mission.id,
                        "title" to mission.title.take(120),
                        "category" to mission.category,
                        "xpEarned" to mission.xpReward.coerceIn(0L, 500L),
                        "dateKey" to today,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                ).await()
                awardXp(mission.xpReward)
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun resetMissionsFreshStart(missions: List<UserMission>): Result<Unit> {
        var path = "users"
        return try {
            val uid = requireUserId()
            val col = db.collection("users").document(uid).collection("missions")
            path = col.path
            val batch = db.batch()
            missions.filter { it.completed }.forEach { m ->
                batch.update(
                    col.document(m.id),
                    mapOf(
                        "completed" to false,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    // --- Prayer Logs ---

    fun observePrayerLogs(userId: String): Flow<List<PrayerLog>> = flow {
        val path = "users/$userId/prayer_logs"
        emitAll(
            db.collection("users").document(userId).collection("prayer_logs")
                .orderBy("dateKey", Query.Direction.DESCENDING)
                .limit(31)
                .snapshots()
                .map { snap ->
                    snap.toObjects(PrayerLog::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .wrapFirestoreErrors(userId, OperationType.LIST, path)
        )
    }

    suspend fun savePrayerLog(log: PrayerLog): Result<Unit> {
        var path = "users"
        return try {
            val uid = requireUserId()
            val dateKey = log.dateKey.ifBlank { PrayerTimeCalculator.todayDateKey() }
            val docRef = db.collection("users").document(uid).collection("prayer_logs").document(dateKey)
            path = docRef.path
            val existing = docRef.get().await()
            if (!existing.exists()) {
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "dateKey" to dateKey,
                        "fajr" to log.fajr,
                        "dhuhr" to log.dhuhr,
                        "asr" to log.asr,
                        "maghrib" to log.maghrib,
                        "isha" to log.isha,
                        "tahajjud" to log.tahajjud,
                        "naflCount" to log.naflCount.coerceIn(0L, 50L),
                        "locationName" to log.locationName.take(80),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.update(
                    mapOf(
                        "fajr" to log.fajr,
                        "dhuhr" to log.dhuhr,
                        "asr" to log.asr,
                        "maghrib" to log.maghrib,
                        "isha" to log.isha,
                        "tahajjud" to log.tahajjud,
                        "naflCount" to log.naflCount.coerceIn(0L, 50L),
                        "locationName" to log.locationName.take(80),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    // --- Quran Bookmarks & Reading Progress ---

    fun observeQuranBookmarks(userId: String): Flow<List<QuranBookmark>> = flow {
        val path = "users/$userId/quran_bookmarks"
        emitAll(
            db.collection("users").document(userId).collection("quran_bookmarks")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .snapshots()
                .map { snap ->
                    snap.toObjects(QuranBookmark::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .wrapFirestoreErrors(userId, OperationType.LIST, path)
        )
    }

    fun observeQuranProgress(userId: String): Flow<QuranProgress?> = flow {
        val path = "users/$userId/quran_progress/current"
        emitAll(
            db.collection("users").document(userId).collection("quran_progress").document("current")
                .snapshots()
                .map { it.toObject(QuranProgress::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .wrapFirestoreErrors(userId, OperationType.GET, path)
        )
    }

    suspend fun toggleQuranBookmark(
        surahNumber: Int,
        surahNameBn: String,
        ayahNumber: Int,
        paraNumber: Int,
        arabicText: String,
        banglaText: String,
        existingBookmark: QuranBookmark?
    ): Result<Unit> {
        var path = "users"
        return try {
            val uid = requireUserId()
            val col = db.collection("users").document(uid).collection("quran_bookmarks")
            if (existingBookmark != null && existingBookmark.id.isNotBlank()) {
                val ref = col.document(existingBookmark.id)
                path = ref.path
                ref.delete().await()
            } else {
                val docId = "s${surahNumber}_a${ayahNumber}"
                val ref = col.document(docId)
                path = ref.path
                ref.set(
                    mapOf(
                        "userId" to uid,
                        "surahNumber" to surahNumber.toLong().coerceIn(1L, 114L),
                        "surahNameBn" to surahNameBn.take(80),
                        "ayahNumber" to ayahNumber.toLong().coerceIn(1L, 286L),
                        "paraNumber" to paraNumber.toLong().coerceIn(1L, 30L),
                        "arabicText" to arabicText.take(3000),
                        "banglaText" to banglaText.take(3000),
                        "favorite" to true,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun updateQuranProgress(
        surahNumber: Int,
        surahNameBn: String,
        ayahNumber: Int,
        paraNumber: Int,
        qariId: String,
        ayahsIncrement: Long = 1L
    ): Result<Unit> {
        var path = "users"
        return try {
            val uid = requireUserId()
            val ref = db.collection("users").document(uid).collection("quran_progress").document("current")
            path = ref.path
            val snap = ref.get().await()
            val today = PrayerTimeCalculator.todayDateKey()
            if (!snap.exists()) {
                ref.set(
                    mapOf(
                        "userId" to uid,
                        "lastSurahNumber" to surahNumber.toLong().coerceIn(1L, 114L),
                        "lastSurahNameBn" to surahNameBn.take(80),
                        "lastAyahNumber" to ayahNumber.toLong().coerceIn(1L, 286L),
                        "lastParaNumber" to paraNumber.toLong().coerceIn(1L, 30L),
                        "lastAudioQariId" to qariId.take(60),
                        "totalAyahsRead" to ayahsIncrement.coerceAtLeast(0L),
                        "dailyAyahsRead" to ayahsIncrement.coerceAtLeast(0L),
                        "dailyGoalAyahs" to 15L,
                        "lastReadDateKey" to today,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                val prevDate = snap.getString("lastReadDateKey") ?: ""
                val prevDaily = snap.getLong("dailyAyahsRead") ?: 0L
                val prevTotal = snap.getLong("totalAyahsRead") ?: 0L
                val newDaily = if (prevDate == today) prevDaily + ayahsIncrement else ayahsIncrement
                ref.update(
                    mapOf(
                        "lastSurahNumber" to surahNumber.toLong().coerceIn(1L, 114L),
                        "lastSurahNameBn" to surahNameBn.take(80),
                        "lastAyahNumber" to ayahNumber.toLong().coerceIn(1L, 286L),
                        "lastParaNumber" to paraNumber.toLong().coerceIn(1L, 30L),
                        "lastAudioQariId" to qariId.take(60),
                        "totalAyahsRead" to (prevTotal + ayahsIncrement),
                        "dailyAyahsRead" to newDaily,
                        "lastReadDateKey" to today,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    // --- AI Conversations & Messages ---

    fun observeAiConversations(userId: String): Flow<List<AiConversation>> = flow {
        val path = "users/$userId/ai_conversations"
        emitAll(
            db.collection("users").document(userId).collection("ai_conversations")
                .orderBy("updatedAt", Query.Direction.DESCENDING)
                .snapshots()
                .map { snap ->
                    snap.toObjects(AiConversation::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .wrapFirestoreErrors(userId, OperationType.LIST, path)
        )
    }

    fun observeAiMessages(userId: String, conversationId: String): Flow<List<AiMessage>> = flow {
        val path = "users/$userId/ai_conversations/$conversationId/messages"
        emitAll(
            db.collection("users").document(userId)
                .collection("ai_conversations").document(conversationId)
                .collection("messages")
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .snapshots()
                .map { snap ->
                    snap.toObjects(AiMessage::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .wrapFirestoreErrors(userId, OperationType.LIST, path)
        )
    }

    suspend fun createAiConversation(title: String): Result<String> {
        var path = "users"
        return try {
            val uid = requireUserId()
            val docRef = db.collection("users").document(uid).collection("ai_conversations").document()
            path = docRef.path
            docRef.set(
                mapOf(
                    "userId" to uid,
                    "title" to title.trim().ifBlank { "নতুন কথোপকথন" }.take(120),
                    "lastMessagePreview" to "",
                    "messageCount" to 0L,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun saveAiMessage(conversationId: String, role: String, content: String): Result<Unit> {
        var path = "users"
        return try {
            val uid = requireUserId()
            val convRef = db.collection("users").document(uid).collection("ai_conversations").document(conversationId)
            val msgRef = convRef.collection("messages").document()
            path = msgRef.path
            msgRef.set(
                mapOf(
                    "userId" to uid,
                    "conversationId" to conversationId,
                    "role" to role,
                    "content" to content.take(8000),
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
            convRef.update(
                mapOf(
                    "lastMessagePreview" to content.take(280),
                    "messageCount" to FieldValue.increment(1L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteAiConversation(conversationId: String): Result<Unit> {
        var path = "users"
        return try {
            val uid = requireUserId()
            val convRef = db.collection("users").document(uid).collection("ai_conversations").document(conversationId)
            path = convRef.path
            val msgs = convRef.collection("messages").get().await()
            val batch = db.batch()
            msgs.documents.forEach { batch.delete(it.reference) }
            batch.delete(convRef)
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    // --- Private Journals ---

    fun observePrivateJournals(userId: String): Flow<List<PrivateJournal>> = flow {
        val path = "users/$userId/private_journals"
        emitAll(
            db.collection("users").document(userId).collection("private_journals")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .snapshots()
                .map { snap ->
                    snap.toObjects(PrivateJournal::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .wrapFirestoreErrors(userId, OperationType.LIST, path)
        )
    }

    suspend fun addPrivateJournal(
        title: String,
        content: String,
        gratitudeNote: String,
        habitReflection: String,
        moodTag: String
    ): Result<Unit> {
        var path = "users"
        return try {
            val uid = requireUserId()
            val ref = db.collection("users").document(uid).collection("private_journals").document()
            path = ref.path
            val payload = mutableMapOf<String, Any?>(
                "userId" to uid,
                "title" to title.trim().ifBlank { "দৈনিক আত্মোপলব্ধি" }.take(120),
                "content" to content.trim().take(5000),
                "gratitudeNote" to gratitudeNote.trim().take(1000).ifBlank { null },
                "habitReflection" to habitReflection.trim().take(1000).ifBlank { null },
                "moodTag" to moodTag.trim().take(40).ifBlank { null },
                "dateKey" to PrayerTimeCalculator.todayDateKey(),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }
            ref.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun deletePrivateJournal(journalId: String): Result<Unit> {
        var path = "users"
        return try {
            val uid = requireUserId()
            val ref = db.collection("users").document(uid).collection("private_journals").document(journalId)
            path = ref.path
            ref.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    // --- Tasbih Logs ---

    fun observeTasbihLogs(userId: String): Flow<List<TasbihLog>> = flow {
        val path = "users/$userId/tasbih_logs"
        emitAll(
            db.collection("users").document(userId).collection("tasbih_logs")
                .orderBy("updatedAt", Query.Direction.DESCENDING)
                .limit(30)
                .snapshots()
                .map { snap ->
                    snap.toObjects(TasbihLog::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .wrapFirestoreErrors(userId, OperationType.LIST, path)
        )
    }

    suspend fun saveTasbihSession(
        dhikrTitle: String,
        arabicText: String,
        count: Long,
        targetCount: Long
    ): Result<Unit> {
        var path = "users"
        return try {
            val uid = requireUserId()
            val dateKey = PrayerTimeCalculator.todayDateKey()
            val docId = "${dateKey}_${dhikrTitle.hashCode().toString().replace("-", "n")}".take(64)
            val ref = db.collection("users").document(uid).collection("tasbih_logs").document(docId)
            path = ref.path
            val existing = ref.get().await()
            if (!existing.exists()) {
                val payload = mutableMapOf<String, Any?>(
                    "userId" to uid,
                    "dhikrTitle" to dhikrTitle.take(120),
                    "arabicText" to arabicText.take(300).ifBlank { null },
                    "count" to count.coerceAtLeast(0L),
                    "targetCount" to targetCount.coerceIn(1L, 10000L),
                    "dateKey" to dateKey,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null }
                ref.set(payload).await()
            } else {
                val updates = mutableMapOf<String, Any?>(
                    "dhikrTitle" to dhikrTitle.take(120),
                    "arabicText" to arabicText.take(300).ifBlank { null },
                    "count" to count.coerceAtLeast(0L),
                    "targetCount" to targetCount.coerceIn(1L, 10000L),
                    "dateKey" to dateKey,
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null }
                ref.update(updates as Map<String, Any>).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    // --- Community, Quizzes & Reports ---

    fun observeCommunityPosts(userId: String): Flow<List<CommunityPost>> = flow {
        val path = "community_posts"
        emitAll(
            db.collection(path)
                .whereEqualTo("moderatedHidden", false)
                .snapshots()
                .map { snap ->
                    snap.toObjects(CommunityPost::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .wrapFirestoreErrors(userId, OperationType.LIST, path)
        )
    }

    suspend fun createCommunityPost(
        alias: String,
        category: CommunityCategory,
        content: String
    ): Result<Unit> {
        val path = "community_posts"
        return try {
            val uid = requireUserId()
            val cleanContent = content.trim().take(1000)
            require(cleanContent.length >= 3) { "পোস্ট কমপক্ষে ৩ অক্ষরের হতে হবে।" }
            val ref = db.collection(path).document()
            ref.set(
                mapOf(
                    "authorId" to uid,
                    "authorAlias" to alias.trim().ifBlank { "অনামিকা মুসাফির" }.take(50),
                    "category" to category.name,
                    "content" to cleanContent,
                    "likesCount" to 0L,
                    "reportedCount" to 0L,
                    "moderatedHidden" to false,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun likeCommunityPost(postId: String): Result<Unit> {
        val path = "community_posts/$postId"
        return try {
            requireUserId()
            db.collection("community_posts").document(postId)
                .update(
                    mapOf(
                        "likesCount" to FieldValue.increment(1L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun reportCommunityPost(postId: String, reason: String): Result<Unit> {
        val reportPath = "community_reports"
        return try {
            val uid = requireUserId()
            val cleanReason = reason.trim().ifBlank { "অনুপযুক্ত কন্টেন্ট" }.take(300)
            db.collection(reportPath).document().set(
                mapOf(
                    "reporterId" to uid,
                    "postId" to postId,
                    "reason" to cleanReason,
                    "status" to "OPEN",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            db.collection("community_posts").document(postId).update(
                mapOf(
                    "reportedCount" to FieldValue.increment(1L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, reportPath)
            Result.failure(e)
        }
    }

    fun observeQuizResults(userId: String): Flow<List<QuizResult>> = flow {
        val path = "quiz_results"
        emitAll(
            db.collection(path)
                .whereEqualTo("userId", userId)
                .snapshots()
                .map { snap ->
                    snap.toObjects(QuizResult::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .wrapFirestoreErrors(userId, OperationType.LIST, path)
        )
    }

    suspend fun saveQuizResult(quizTitle: String, score: Int, total: Int, xpEarned: Long): Result<Unit> {
        val path = "quiz_results"
        return try {
            val uid = requireUserId()
            db.collection(path).document().set(
                mapOf(
                    "userId" to uid,
                    "quizTitle" to quizTitle.take(120),
                    "score" to score.toLong(),
                    "totalQuestions" to total.toLong().coerceIn(1L, 50L),
                    "xpEarned" to xpEarned.coerceIn(0L, 500L),
                    "dateKey" to PrayerTimeCalculator.todayDateKey(),
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
            awardXp(xpEarned)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    // --- Curated & Admin Operations ---

    fun observeCuratedCollection(collectionName: String, userId: String): Flow<List<CuratedItem>> = flow {
        emitAll(
            db.collection(collectionName)
                .snapshots()
                .map { snap ->
                    snap.toObjects(CuratedItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .wrapFirestoreErrors(userId, OperationType.LIST, collectionName)
        )
    }

    fun observeAllUsersForAdmin(adminUid: String): Flow<List<UserProfile>> = flow {
        emitAll(
            db.collection("users")
                .limit(100)
                .snapshots()
                .map { snap ->
                    snap.toObjects(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .wrapFirestoreErrors(adminUid, OperationType.LIST, "users")
        )
    }

    fun observeOpenReportsForAdmin(adminUid: String): Flow<List<CommunityReport>> = flow {
        emitAll(
            db.collection("community_reports")
                .limit(100)
                .snapshots()
                .map { snap ->
                    snap.toObjects(CommunityReport::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .wrapFirestoreErrors(adminUid, OperationType.LIST, "community_reports")
        )
    }

    suspend fun adminSaveCuratedItem(
        collectionName: String,
        title: String,
        body: String,
        reference: String,
        category: String
    ): Result<Unit> {
        return try {
            requireUserId()
            val ref = db.collection(collectionName).document()
            val payload = mutableMapOf<String, Any?>(
                "title" to title.trim().take(160),
                "body" to body.trim().take(5000),
                "reference" to reference.trim().take(200).ifBlank { null },
                "category" to category.trim().ifBlank { "সাধারণ" }.take(60),
                "active" to true,
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }
            ref.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, collectionName)
            Result.failure(e)
        }
    }

    suspend fun adminDeleteCuratedItem(collectionName: String, docId: String): Result<Unit> {
        return try {
            requireUserId()
            db.collection(collectionName).document(docId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "$collectionName/$docId")
            Result.failure(e)
        }
    }

    suspend fun adminSetUserStatus(targetUid: String, status: String): Result<Unit> {
        return try {
            requireUserId()
            db.collection("users").document(targetUid).update(
                mapOf(
                    "accountStatus" to status,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "users/$targetUid")
            Result.failure(e)
        }
    }

    suspend fun adminResolveReport(reportId: String, postId: String, hidePost: Boolean): Result<Unit> {
        return try {
            requireUserId()
            db.collection("community_reports").document(reportId).update(
                mapOf(
                    "status" to if (hidePost) "RESOLVED" else "DISMISSED",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            if (hidePost) {
                db.collection("community_posts").document(postId).update(
                    mapOf(
                        "moderatedHidden" to true,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "community_reports/$reportId")
            Result.failure(e)
        }
    }

    suspend fun deleteMyAccountData(): Result<Unit> {
        var path = "users"
        return try {
            val uid = requireUserId()
            val userRef = db.collection("users").document(uid)
            path = userRef.path
            val subcollections = listOf(
                "missions", "mission_history", "prayer_logs", "quran_bookmarks",
                "quran_progress", "private_journals", "tasbih_logs", "notifications"
            )
            for (sub in subcollections) {
                val docs = userRef.collection(sub).get().await()
                val batch = db.batch()
                docs.documents.forEach { batch.delete(it.reference) }
                batch.commit().await()
            }
            val convs = userRef.collection("ai_conversations").get().await()
            for (c in convs.documents) {
                deleteAiConversation(c.id)
            }
            userRef.delete().await()
            auth.currentUser?.delete()?.await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    companion object {
        fun sha256(input: String): String {
            val digest = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
            return digest.joinToString("") { "%02x".format(it) }
        }
    }
}
