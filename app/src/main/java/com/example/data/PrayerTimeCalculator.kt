package com.example.data

import com.example.model.CityCoordinates
import com.example.model.PrayerSchedule
import com.example.ui.components.toBn
import java.util.Calendar
import java.util.GregorianCalendar
import kotlin.math.PI
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.tan

object PrayerTimeCalculator {

    val presetCities = listOf(
        CityCoordinates("ঢাকা", 23.8103, 90.4125, 6.0),
        CityCoordinates("চট্টগ্রাম", 22.3569, 91.7832, 6.0),
        CityCoordinates("সিলেট", 24.8949, 91.8687, 6.0),
        CityCoordinates("রাজশাহী", 24.3745, 88.6042, 6.0),
        CityCoordinates("খুলনা", 22.8456, 89.5403, 6.0),
        CityCoordinates("বরিশাল", 22.7010, 90.3535, 6.0),
        CityCoordinates("রংপুর", 25.7439, 89.2752, 6.0),
        CityCoordinates("ময়মনসিংহ", 24.7471, 90.4203, 6.0),
        CityCoordinates("মক্কা", 21.3891, 39.8579, 3.0),
        CityCoordinates("মদিনা", 24.5247, 39.5692, 3.0),
        CityCoordinates("লন্ডন", 51.5072, -0.1276, 0.0)
    )

    private val banglaMonths = listOf(
        "জানুয়ারি", "ফেব্রুয়ারি", "মার্চ", "এপ্রিল", "মে", "জুন",
        "জুলাই", "আগস্ট", "সেপ্টেম্বর", "অক্টোবর", "নভেম্বর", "ডিসেম্বর"
    )

    private val banglaWeekdays = listOf(
        "রবিবার", "সোমবার", "মঙ্গলবার", "বুধবার", "বৃহস্পতিবার", "শুক্রবার", "শনিবার"
    )

    private val hijriMonthsBn = listOf(
        "মুহাররম", "সফর", "রবিউল আউয়াল", "রবিউস সানি",
        "জমাদিউল আউয়াল", "জমাদিউস সানি", "রজব", "শাবান",
        "রমজান", "শাওয়াল", "জিলকদ", "জিলহজ"
    )

    fun todayDateKey(): String {
        val cal = Calendar.getInstance()
        val y = cal.get(Calendar.YEAR)
        val m = cal.get(Calendar.MONTH) + 1
        val d = cal.get(Calendar.DAY_OF_MONTH)
        return "%04d-%02d-%02d".format(y, m, d)
    }

    fun formatDateBn(cal: Calendar = Calendar.getInstance()): String {
        val dayName = banglaWeekdays[(cal.get(Calendar.DAY_OF_WEEK) - 1).coerceIn(0, 6)]
        val day = cal.get(Calendar.DAY_OF_MONTH).toBn()
        val month = banglaMonths[cal.get(Calendar.MONTH).coerceIn(0, 11)]
        val year = cal.get(Calendar.YEAR).toBn()
        return "$dayName, $day $month $year"
    }

    fun hijriDateBn(cal: Calendar = Calendar.getInstance()): Triple<Int, Int, Int> {
        val y = cal.get(Calendar.YEAR)
        val m = cal.get(Calendar.MONTH) + 1
        val d = cal.get(Calendar.DAY_OF_MONTH)
        val jd = gregorianToJulianDay(y, m, d)
        return julianDayToHijri(jd)
    }

    fun formatHijriDateBn(cal: Calendar = Calendar.getInstance()): String {
        val (hYear, hMonth, hDay) = hijriDateBn(cal)
        val mName = hijriMonthsBn[(hMonth - 1).coerceIn(0, 11)]
        return "${hDay.toBn()} $mName ${hYear.toBn()} হিজরি"
    }

    fun hijriMonthName(month: Int): String = hijriMonthsBn[(month - 1).coerceIn(0, 11)]

    private fun gregorianToJulianDay(year: Int, month: Int, day: Int): Int {
        var y = year
        var m = month
        if (m <= 2) {
            y -= 1
            m += 12
        }
        val a = y / 100
        val b = 2 - a + (a / 4)
        return floor(365.25 * (y + 4716)).toInt() + floor(30.6001 * (m + 1)).toInt() + day + b - 1524
    }

    private fun julianDayToHijri(jd: Int): Triple<Int, Int, Int> {
        val l = jd - 1948440 + 10632
        val n = (l - 1) / 10631
        val l2 = l - 10631 * n + 354
        val j = ((10985 - l2) / 5316) * ((50 * l2) / 17719) + (l2 / 5670) * ((43 * l2) / 15238)
        val l3 = l2 - ((30 - j) / 15) * ((17719 * j) / 50) - (j / 16) * ((15238 * j) / 43) + 29
        val month = (24 * l3) / 709
        val day = l3 - (709 * month) / 24
        val year = 30 * n + j - 30
        return Triple(year, month, day)
    }

    fun findCity(locationName: String): CityCoordinates {
        return presetCities.firstOrNull { it.nameBn == locationName } ?: presetCities.first()
    }

    fun calculateForCity(
        city: CityCoordinates,
        cal: Calendar = GregorianCalendar.getInstance()
    ): PrayerSchedule {
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH) + 1
        val day = cal.get(Calendar.DAY_OF_MONTH)

        val jd = julianDate(year, month, day) - city.longitude / (15.0 * 24.0)
        val d = jd - 2451545.0

        val g = fixAngle(357.529 + 0.98560028 * d)
        val q = fixAngle(280.459 + 0.98564736 * d)
        val l = fixAngle(q + 1.915 * dSin(g) + 0.020 * dSin(2 * g))
        val e = 23.439 - 0.00000036 * d
        val ra = dAtan2(dCos(e) * dSin(l), dCos(l)) / 15.0
        val eqTime = q / 15.0 - fixHour(ra)
        val decl = dAsin(dSin(e) * dSin(l))

        val dhuhrHour = fixHour(12.0 + city.utcOffsetHours - city.longitude / 15.0 - eqTime)
        val sunriseHour = dhuhrHour - sunAngleTime(0.833, decl, city.latitude)
        val sunsetHour = dhuhrHour + sunAngleTime(0.833, decl, city.latitude)
        // Karachi / Islamic Foundation Bangladesh standard: Fajr 18 deg, Isha 18 deg, Hanafi Asr shadow = 2
        val fajrHour = dhuhrHour - sunAngleTime(18.0, decl, city.latitude)
        val asrHour = dhuhrHour + asrTime(2.0, decl, city.latitude)
        val maghribHour = sunsetHour + (2.0 / 60.0)
        val ishaHour = dhuhrHour + sunAngleTime(18.0, decl, city.latitude)
        val sehriEndHour = fajrHour - (3.0 / 60.0)

        val nowMinutes = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        val slots = listOf(
            "ফজর" to toMinutes(fajrHour),
            "যোহর" to toMinutes(dhuhrHour),
            "আসর" to toMinutes(asrHour),
            "মাগরিব" to toMinutes(maghribHour),
            "এশা" to toMinutes(ishaHour)
        )
        val nextSlot = slots.firstOrNull { it.second > nowMinutes }
        val (nextName, nextMins, diffMins) = if (nextSlot != null) {
            Triple(nextSlot.first, nextSlot.second, nextSlot.second - nowMinutes)
        } else {
            val fajrMins = slots.first().second
            Triple("ফজর (আগামীকাল)", fajrMins, (24 * 60 - nowMinutes) + fajrMins)
        }

        val remH = diffMins / 60
        val remM = diffMins % 60
        val remText = if (remH > 0) "${remH.toBn()} ঘণ্টা ${remM.toBn()} মিনিট বাকি" else "${remM.toBn()} মিনিট বাকি"

        return PrayerSchedule(
            locationName = city.nameBn,
            dateTextBn = formatDateBn(cal),
            hijriDateTextBn = formatHijriDateBn(cal),
            fajr = formatHourBn(fajrHour),
            sunrise = formatHourBn(sunriseHour),
            dhuhr = formatHourBn(dhuhrHour),
            asr = formatHourBn(asrHour),
            maghrib = formatHourBn(maghribHour),
            isha = formatHourBn(ishaHour),
            sehriEnd = formatHourBn(sehriEndHour),
            iftarTime = formatHourBn(maghribHour),
            nextPrayerNameBn = nextName,
            nextPrayerTime = formatMinutesBn(nextMins),
            remainingTimeBn = remText
        )
    }

    private fun julianDate(year: Int, month: Int, day: Int): Double {
        var y = year
        var m = month
        if (m <= 2) {
            y -= 1
            m += 12
        }
        val a = floor(y / 100.0)
        val b = 2.0 - a + floor(a / 4.0)
        return floor(365.25 * (y + 4716)) + floor(30.6001 * (m + 1)) + day + b - 1524.5
    }

    private fun sunAngleTime(angle: Double, decl: Double, lat: Double): Double {
        val num = -dSin(angle) - dSin(decl) * dSin(lat)
        val den = dCos(decl) * dCos(lat)
        val ratio = (num / den).coerceIn(-1.0, 1.0)
        return dAcos(ratio) / 15.0
    }

    private fun asrTime(factor: Double, decl: Double, lat: Double): Double {
        val angle = -dAcot(factor + dTan(kotlin.math.abs(lat - decl)))
        return sunAngleTime(angle, decl, lat)
    }

    private fun toMinutes(hours: Double): Int {
        val h = fixHour(hours)
        return (h * 60.0).roundToInt() % (24 * 60)
    }

    private fun formatHourBn(hours: Double): String = formatMinutesBn(toMinutes(hours))

    private fun formatMinutesBn(totalMinutes: Int): String {
        val m = ((totalMinutes % (24 * 60)) + (24 * 60)) % (24 * 60)
        val h24 = m / 60
        val min = m % 60
        val period = if (h24 < 12) "পূর্বাহ্ণ" else "অপরাহ্ণ"
        val h12 = when {
            h24 == 0 -> 12
            h24 > 12 -> h24 - 12
            else -> h24
        }
        return "${h12.toBn()}:${"%02d".format(min).let { com.example.ui.components.toBanglaDigits(it) }} $period"
    }

    private fun fixAngle(a: Double): Double {
        var res = a - 360.0 * floor(a / 360.0)
        if (res < 0) res += 360.0
        return res
    }

    private fun fixHour(a: Double): Double {
        var res = a - 24.0 * floor(a / 24.0)
        if (res < 0) res += 24.0
        return res
    }

    private fun dSin(d: Double) = sin(d * PI / 180.0)
    private fun dCos(d: Double) = cos(d * PI / 180.0)
    private fun dTan(d: Double) = tan(d * PI / 180.0)
    private fun dAsin(x: Double) = asin(x.coerceIn(-1.0, 1.0)) * 180.0 / PI
    private fun dAcos(x: Double) = acos(x.coerceIn(-1.0, 1.0)) * 180.0 / PI
    private fun dAtan2(y: Double, x: Double) = atan2(y, x) * 180.0 / PI
    private fun dAcot(x: Double) = atan(1.0 / x) * 180.0 / PI
}
