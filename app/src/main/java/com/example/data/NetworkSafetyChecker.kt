package com.example.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import com.example.model.DnsCheckResult
import com.example.model.DnsProtectionStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

object NetworkSafetyChecker {

    private val familyDnsHosts = setOf(
        "family.cloudflare-dns.com",
        "family.adguard-dns.com",
        "family-filter-dns.cleanbrowsing.org",
        "adult-filter-dns.cleanbrowsing.org",
        "dns.adguard.com",
        "dns-family.adguard.com"
    )

    private val client = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(6, TimeUnit.SECONDS)
        .build()

    suspend fun checkPrivateDnsAndDoh(context: Context): DnsCheckResult = withContext(Dispatchers.IO) {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return@withContext DnsCheckResult(
                status = DnsProtectionStatus.UNAVAILABLE,
                activeHostname = null,
                dohReachable = false,
                detailsBn = "নেটওয়ার্ক সার্ভিস পাওয়া যায়নি।"
            )

        val network = cm.activeNetwork
        val caps = if (network != null) cm.getNetworkCapabilities(network) else null
        val isOnline = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true

        if (!isOnline || network == null) {
            return@withContext DnsCheckResult(
                status = DnsProtectionStatus.UNAVAILABLE,
                activeHostname = null,
                dohReachable = false,
                detailsBn = "ইন্টারনেট সংযোগ সক্রিয় নেই। সংযুক্ত হয়ে আবার পরীক্ষা করুন।"
            )
        }

        val linkProps = cm.getLinkProperties(network)
        val privateDnsActive = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            linkProps?.isPrivateDnsActive == true
        } else {
            false
        }
        val dnsHost = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            linkProps?.privateDnsServerName
        } else {
            null
        }

        val dohOk = testCloudflareFamilyDoh()

        if (!privateDnsActive) {
            return@withContext DnsCheckResult(
                status = DnsProtectionStatus.INACTIVE,
                activeHostname = null,
                dohReachable = dohOk,
                detailsBn = "ডিভাইসে Private DNS চালু নেই। নিরাপদ ব্রাউজিংয়ের জন্য নিচের নির্দেশনা অনুসরণ করে ফ্যামিলি-সেফ Private DNS যুক্ত করুন।"
            )
        }

        val normalizedHost = dnsHost?.trim()?.lowercase()
        val isKnownFamilyHost = normalizedHost != null && familyDnsHosts.any {
            normalizedHost == it || normalizedHost.endsWith(".$it")
        }

        if (isKnownFamilyHost) {
            DnsCheckResult(
                status = DnsProtectionStatus.ACTIVE_FAMILY,
                activeHostname = dnsHost,
                dohReachable = dohOk,
                detailsBn = "ডিভাইসের সিস্টেম Private DNS ($dnsHost) সক্রিয়ভাবে কাজ করছে।"
            )
        } else {
            DnsCheckResult(
                status = DnsProtectionStatus.CONFIGURED_STANDARD,
                activeHostname = dnsHost,
                dohReachable = dohOk,
                detailsBn = "Private DNS চালু আছে${if (!dnsHost.isNullOrBlank()) " ($dnsHost)" else ""}, তবে এটি পরিচিত ফ্যামিলি-ফিল্টার হোস্ট হিসেবে নিশ্চিত নয়।"
            )
        }
    }

    private fun testCloudflareFamilyDoh(): Boolean {
        return try {
            val req = Request.Builder()
                .url("https://family.cloudflare-dns.com/dns-query?name=quran.com&type=A")
                .header("Accept", "application/dns-json")
                .build()
            client.newCall(req).execute().use { it.isSuccessful }
        } catch (_: Exception) {
            false
        }
    }
}
