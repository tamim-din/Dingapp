package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.R
import com.example.firebase.onGoogleSignInClicked
import com.example.ui.components.GlassCard
import com.example.ui.components.IslamicBackdrop
import com.example.ui.components.toBn
import com.example.ui.theme.LocalNoorGlassColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun NoorSplashScreen(onFinished: () -> Unit) {
    val transition = rememberInfiniteTransition(label = "splashGlow")
    val pulse by transition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    LaunchedEffect(Unit) {
        delay(1700L)
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF10271F))
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_splash_art),
            contentDescription = "DCFBD NOOR ইসলামিক ব্যাকগ্রাউন্ড",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .alpha(0.45f)
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0x8810271F),
                            Color(0xCC10271F),
                            Color(0xF010271F)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                shape = CircleShape,
                color = Color(0xFF174B38),
                border = BorderStroke(2.dp, Color(0xFFE6D3A1).copy(alpha = 0.7f)),
                modifier = Modifier
                    .size(118.dp)
                    .scale(pulse)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.noor_icon_fg),
                    contentDescription = "DCFBD NOOR লোগো",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().clip(CircleShape)
                )
            }

            Spacer(Modifier.height(24.dp))

            Text(
                text = "DCFBD NOOR",
                style = MaterialTheme.typography.displayLarge,
                color = Color(0xFFE6D3A1),
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "একটি সুন্দর জীবনের পথে",
                style = MaterialTheme.typography.titleLarge,
                color = Color(0xFFF5F1E8)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "A Journey Towards Hidayah",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFE6D3A1).copy(alpha = 0.85f)
            )

            Spacer(Modifier.height(40.dp))

            Surface(
                shape = RoundedCornerShape(50),
                color = Color.White.copy(alpha = 0.1f),
                border = BorderStroke(1.dp, Color(0xFFE6D3A1).copy(alpha = 0.3f))
            ) {
                Text(
                    text = "পরিচালনায়: DarkNet Cyber Force BD",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFFF5F1E8),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }
    }
}

private data class OnboardingPage(
    val title: String,
    val subtitle: String,
    val icon: ImageVector
)

@Composable
fun OnboardingScreen(onComplete: () -> Unit) {
    val noor = LocalNoorGlassColors.current
    val scope = rememberCoroutineScope()
    val pages = remember {
        listOf(
            OnboardingPage(
                "DCFBD NOOR-এ স্বাগতম",
                "একটি প্রশান্ত ও সুশৃঙ্খল ইসলামিক জীবনযাপনের বিশ্বস্ত ডিজিটাল সঙ্গী—DarkNet Cyber Force BD-এর পক্ষ থেকে উপহার।",
                Icons.Default.AutoAwesome
            ),
            OnboardingPage(
                "ভালো অভ্যাস গড়ে তুলুন",
                "দৈনিক ইবাদত, সুন্দর চরিত্র, আত্মসংযম ও মানবকল্যাণমূলক মিশনের মাধ্যমে ধাপে ধাপে নিজেকে উন্নত করুন।",
                Icons.Default.CheckCircle
            ),
            OnboardingPage(
                "আল্লাহর সঙ্গে সম্পর্ক দৃঢ় করুন",
                "পাঁচ ওয়াক্ত নামাজের সময়সূচি, নামাজ ট্র্যাকার, ডিজিটাল তাসবিহ ও দৈনিক দোয়ার মাধ্যমে হৃদয়ে প্রশান্তি আনুন।",
                Icons.Default.Favorite
            ),
            OnboardingPage(
                "কুরআন পড়ুন ও শুনুন",
                "সম্পূর্ণ ৩০ পারা ও ১১৪টি সূরা আরবি আয়াত, বাংলা অনুবাদ, উচ্চারণ এবং প্রিয় ক্বারীদের তিলাওয়াতসহ উপভোগ করুন।",
                Icons.Default.MenuBook
            ),
            OnboardingPage(
                "অনুপযুক্ত অনলাইন কন্টেন্ট থেকে নিরাপদ থাকুন",
                "ফ্যামিলি-সেফ Private DNS গাইড, ডিজিটাল ওয়েলবিইং এবং AI সহায়তার মাধ্যমে পবিত্র ও নিরাপদ ডিজিটাল জীবন গড়ুন।",
                Icons.Default.Shield
            )
        )
    }
    val pagerState = rememberPagerState(pageCount = { pages.size })

    IslamicBackdrop(showMosqueHeaderArt = true) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(20.dp)
                .widthIn(max = 560.dp)
                .align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${(pagerState.currentPage + 1).toBn()} / ${pages.size.toBn()}",
                    style = MaterialTheme.typography.labelLarge,
                    color = noor.textSecondary
                )
                TextButton(
                    onClick = onComplete,
                    modifier = Modifier.testTag("onboarding_skip_button")
                ) {
                    Text(
                        text = "এড়িয়ে যান",
                        color = if (noor.isDark) noor.accentGold else noor.emeraldPrimary
                    )
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) { idx ->
                val page = pages[idx]
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    GlassCard(
                        elevated = true,
                        contentPadding = 28.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = noor.accentGold.copy(alpha = 0.2f),
                                border = BorderStroke(1.5.dp, noor.accentGold),
                                modifier = Modifier.size(84.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = page.icon,
                                        contentDescription = page.title,
                                        tint = if (noor.isDark) noor.accentGold else noor.emeraldPrimary,
                                        modifier = Modifier.size(40.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.height(24.dp))
                            Text(
                                text = page.title,
                                style = MaterialTheme.typography.headlineSmall,
                                color = noor.textPrimary,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(14.dp))
                            Text(
                                text = page.subtitle,
                                style = MaterialTheme.typography.bodyLarge,
                                color = noor.textSecondary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(pages.size) { index ->
                    val active = pagerState.currentPage == index
                    Box(
                        modifier = Modifier
                            .height(8.dp)
                            .width(if (active) 24.dp else 8.dp)
                            .clip(CircleShape)
                            .background(
                                if (active) noor.accentGold
                                else noor.textSecondary.copy(alpha = 0.35f)
                            )
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            Button(
                onClick = {
                    if (pagerState.currentPage < pages.lastIndex) {
                        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                    } else {
                        onComplete()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("onboarding_next_button"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (noor.isDark) noor.accentGold else noor.emeraldPrimary,
                    contentColor = if (noor.isDark) Color(0xFF10271F) else Color.White
                )
            ) {
                Text(
                    text = if (pagerState.currentPage < pages.lastIndex) "পরবর্তী" else "শুরু করুন",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.width(8.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
            }
        }
    }
}

@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit,
    onShowOnboarding: () -> Unit
) {
    val noor = LocalNoorGlassColors.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    IslamicBackdrop(showMosqueHeaderArt = true) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(24.dp)
                .widthIn(max = 520.dp)
                .align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                shape = CircleShape,
                color = Color(0xFF174B38),
                border = BorderStroke(2.dp, noor.accentGold),
                modifier = Modifier.size(92.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.noor_icon_fg),
                    contentDescription = "DCFBD NOOR",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().clip(CircleShape)
                )
            }

            Spacer(Modifier.height(18.dp))

            Text(
                text = "DCFBD NOOR",
                style = MaterialTheme.typography.headlineMedium,
                color = noor.textPrimary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "একটি সুন্দর জীবনের পথে",
                style = MaterialTheme.typography.titleMedium,
                color = if (noor.isDark) noor.accentGold else noor.emeraldPrimary
            )

            Spacer(Modifier.height(24.dp))

            GlassCard(
                elevated = true,
                contentPadding = 22.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "নিরাপদ অ্যাকাউন্টে প্রবেশ করুন",
                    style = MaterialTheme.typography.titleLarge,
                    color = noor.textPrimary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "আপনার নামাজ, কুরআন তিলাওয়াত, দৈনিক মিশন, তাসবিহ এবং ব্যক্তিগত জার্নাল নিরাপদ ক্লাউড ডেটাবেজে সংরক্ষিত থাকবে।",
                    style = MaterialTheme.typography.bodyMedium,
                    color = noor.textSecondary
                )

                Spacer(Modifier.height(18.dp))

                Button(
                    onClick = {
                        errorMessage = null
                        isLoading = true
                        onGoogleSignInClicked(
                            context = context,
                            credentialManager = credentialManager,
                            onAuthSuccess = {
                                isLoading = false
                                onAuthSuccess()
                            },
                            onAuthError = { msg ->
                                isLoading = false
                                errorMessage = msg
                            },
                            scope = scope,
                            onAuthCancelled = {
                                isLoading = false
                            }
                        )
                    },
                    enabled = !isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("google_sign_in_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (noor.isDark) noor.accentGold else noor.emeraldPrimary,
                        contentColor = if (noor.isDark) Color(0xFF10271F) else Color.White
                    )
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.5.dp,
                            color = if (noor.isDark) Color(0xFF10271F) else Color.White
                        )
                        Spacer(Modifier.width(10.dp))
                        Text("প্রবেশ করা হচ্ছে...", style = MaterialTheme.typography.labelLarge)
                    } else {
                        Icon(Icons.Default.VerifiedUser, contentDescription = null)
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "গুগল অ্যাকাউন্ট দিয়ে প্রবেশ করুন",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                AnimatedVisibility(
                    visible = !errorMessage.isNullOrBlank(),
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Column {
                        Spacer(Modifier.height(12.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = noor.error.copy(alpha = 0.14f),
                            border = BorderStroke(1.dp, noor.error.copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = errorMessage.orEmpty(),
                                style = MaterialTheme.typography.bodySmall,
                                color = noor.error,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))
                Text(
                    text = "গোপনীয়তার নিশ্চয়তা: আপনার ব্যক্তিগত নোট, পাপ-পুণ্যের হিসাব বা গুনাহের তথ্য কখনো অন্য কারো সাথে শেয়ার করা হয় না।",
                    style = MaterialTheme.typography.bodySmall,
                    color = noor.textSecondary
                )
            }

            Spacer(Modifier.height(14.dp))
            OutlinedButton(
                onClick = onShowOnboarding,
                shape = RoundedCornerShape(50),
                border = BorderStroke(1.dp, noor.glassBorder)
            ) {
                Text("পরিচিতি স্লাইড আবার দেখুন", color = noor.textPrimary)
            }
        }
    }
}
