package com.example.data

import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Content
import com.google.firebase.ai.type.content
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout

class NoorAiRepository {

    private val systemPrompt = content {
        text(
            """
            তুমি 'নূর সহকারী' (DCFBD NOOR অ্যাপের ইসলামিক জীবনযাপন ও আত্মউন্নয়ন সহায়ক AI)।
            তোমার অবশ্য পালনীয় নীতিমালা:
            ১. ব্যবহারকারী বাংলায় বা ইংরেজিতে প্রশ্ন করলে সেই ভাষায় (প্রাধান্যত প্রাঞ্জল ও ভদ্র বাংলায়) উত্তর দেবে।
            ২. কখনোই নিজের মনগড়া কোনো কুরআনের আয়াত বা হাদীস বানিয়ে বলবে না।
            ৩. কুরআনের আয়াত উল্লেখ করলে অবশ্যই সঠিক সূরার নাম ও আয়াত নম্বর (যেমন: সূরা আল-বাকারা ২:১৫৩) দেবে।
            ৪. হাদীস উল্লেখ করলে অবশ্যই হাদীসগ্রন্থের নাম ও রেফারেন্স দেবে এবং সহিহ, হাসান, দুর্বল (যয়ীফ) বা আলেমদের মতভেদ স্পষ্টভাবে আলাদা করে বলবে। যদি কোনো রেফারেন্স নিয়ে নিশ্চিত না হও, তবে অনুমান না করে স্পষ্ট বলবে এবং নির্ভরযোগ্য আলেমদের কাছ থেকে যাচাই করার পরামর্শ দেবে।
            ৫. তুমি কখনো নিজেকে মুফতি বা আলেম দাবি করবে না। জটিল ফিকহ বা ব্যক্তিগত সংবেদনশীল মাসআলার ক্ষেত্রে সর্বদা যোগ্য আলেম বা মুফতির পরামর্শ নিতে উৎসাহিত করবে।
            ৬. হতাশ, অনুতপ্ত বা মানসিকভাবে বিপর্যস্ত ব্যবহারকারীর সাথে গভীর সহানুভূতি, ভালোবাসা এবং আল্লাহর রহমতের আশা জাগানো ভাষায় কথা বলবে—কখনোই কাউকে বিচার, তিরস্কার বা অপমান করবে না।
            ৭. গুরুতর মানসিক সংকট বা নিজের ক্ষতির ঝুঁকি বুঝলে সহানুভূতির সাথে বিশ্বস্ত অভিভাবক, পরিবার, বন্ধু বা পেশাদার মানসিক স্বাস্থ্য সহায়তাকারীর সাহায্য নিতে উৎসাহিত করবে।
            ৮. উত্তর পরিষ্কার, সুশৃঙ্খল ও প্রয়োজনীয় পয়েন্ট আকারে দেবে।
            """.trimIndent()
        )
    }

    private val model by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            systemInstruction = systemPrompt
        )
    }

    fun streamIslamicReply(
        historyPairs: List<Pair<String, String>>,
        userMessage: String
    ): Flow<String> = flow {
        val chatHistory: List<Content> = historyPairs.takeLast(12).map { (role, text) ->
            val mappedRole = if (role == "USER") "user" else "model"
            content(mappedRole) { text(text) }
        }
        val chat = model.startChat(history = chatHistory)
        val accumulated = StringBuilder()
        try {
            withTimeout(60_000L) {
                chat.sendMessageStream(userMessage).collect { chunk ->
                    val piece = chunk.text.orEmpty()
                    if (piece.isNotEmpty()) {
                        accumulated.append(piece)
                        emit(accumulated.toString())
                    }
                }
            }
            if (accumulated.isBlank()) {
                throw IllegalStateException("AI সহকারী থেকে কোনো উত্তর পাওয়া যায়নি।")
            }
        } catch (e: TimeoutCancellationException) {
            throw IllegalStateException("উত্তর তৈরি করতে সময়সীমা (৬০ সেকেন্ড) অতিক্রম করেছে। আবার চেষ্টা করুন।", e)
        } catch (e: CancellationException) {
            throw e
        }
    }
}
