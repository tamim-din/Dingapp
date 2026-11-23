package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.example.data.BengaliIslamicData
import com.example.data.PrayerTimeCalculator
import com.example.ui.components.toBanglaDigits
import com.example.ui.screens.OnboardingScreen
import com.example.ui.theme.DcfbdNoorTheme
import com.example.ui.theme.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.GregorianCalendar

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `app name matches DCFBD NOOR`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("DCFBD NOOR", appName)
    }

    @Test
    fun `quran catalog includes all 114 surahs and 30 paras`() {
        assertEquals(114, BengaliIslamicData.allSurahs.size)
        assertEquals(30, BengaliIslamicData.allParas.size)
        assertEquals("আল-ফাতিহা", BengaliIslamicData.allSurahs.first().nameBn)
        assertEquals("আন-নাস", BengaliIslamicData.allSurahs.last().nameBn)
        assertTrue(BengaliIslamicData.allSurahs.sumOf { it.ayahCount } == 6236)
    }

    @Test
    fun `prayer calculator computes valid times and hijri date for Dhaka`() {
        val dhaka = PrayerTimeCalculator.findCity("ঢাকা")
        val sampleDate = GregorianCalendar(2025, 2, 15)
        val schedule = PrayerTimeCalculator.calculateForCity(dhaka, sampleDate)

        assertEquals("ঢাকা", schedule.locationName)
        assertTrue(schedule.fajr.isNotBlank())
        assertTrue(schedule.dhuhr.isNotBlank())
        assertTrue(schedule.asr.isNotBlank())
        assertTrue(schedule.maghrib.isNotBlank())
        assertTrue(schedule.isha.isNotBlank())

        val hijri = PrayerTimeCalculator.formatHijriDateBn(sampleDate)
        assertTrue(hijri.contains("হিজরি"))
    }

    @Test
    fun `bangla digit converter converts numerals accurately`() {
        assertEquals("০১:২৩", toBanglaDigits("01:23"))
        assertEquals("১১৪ সূরা", toBanglaDigits("114 সূরা"))
    }

    @Test
    fun `onboarding screen renders Bengali welcome and completes on skip`() {
        var completed = false
        composeTestRule.setContent {
            DcfbdNoorTheme(themeMode = ThemeMode.DAY) {
                OnboardingScreen(onComplete = { completed = true })
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("DCFBD NOOR-এ স্বাগতম").assertIsDisplayed()
        composeTestRule.onNodeWithTag("onboarding_skip_button").performClick()
        composeTestRule.waitForIdle()
        assertTrue(completed)
    }
}
