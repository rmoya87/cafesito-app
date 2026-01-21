package com.example.cafesito.ui.access

import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.cafesito.ui.theme.*
import com.example.cafesito.ui.components.*
import kotlinx.coroutines.launch

data class OnboardingPage(
    val title: String,
    val description: String,
    val icon: String 
)

val onboardingPages = listOf(
    OnboardingPage(
        "¡Bienvenido a Cafesito!",
        "La comunidad para los amantes del café de especialidad.",
        "☕"
    ),
    OnboardingPage(
        "Comparte tu Pasión",
        "Publica tus momentos cafeteros, sigue a otros baristas y descubre otros tipos de cafes",
        "📸"
    ),
    OnboardingPage(
        "Tu Diario de Cata",
        "Valora cada café, guarda tus favoritos y crea tu propio perfil sensorial personalizado.",
        "📊"
    )
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    onFinished: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { onboardingPages.size })
    val scope = rememberCoroutineScope()

    Scaffold(containerColor = SoftOffWhite) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (pagerState.currentPage < onboardingPages.size - 1) {
                TextButton(
                    onClick = onFinished,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(24.dp)
                        .statusBarsPadding()
                        .zIndex(1f) 
                ) {
                    Text(
                        "OMITIR", 
                        style = MaterialTheme.typography.labelLarge, 
                        color = CaramelAccent,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Column(modifier = Modifier.fillMaxSize()) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) { page ->
                    OnboardingPageContentPremium(onboardingPages[page])
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp)
                        .padding(bottom = 64.dp), 
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.height(40.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(onboardingPages.size) { iteration ->
                            val isSelected = pagerState.currentPage == iteration
                            Box(
                                modifier = Modifier
                                    .padding(4.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) EspressoDeep else BorderLight)
                                    .size(if (isSelected) 10.dp else 6.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    Button(
                        onClick = {
                            if (pagerState.currentPage < onboardingPages.size - 1) {
                                scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                            } else {
                                onFinished()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EspressoDeep),
                        shape = RoundedCornerShape(30.dp)
                    ) {
                        Text(
                            text = if (pagerState.currentPage == onboardingPages.size - 1) "EMPEZAR" else "SIGUIENTE",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun OnboardingPageContentPremium(page: OnboardingPage) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        PremiumCard(
            modifier = Modifier.size(200.dp),
            shape = RoundedCornerShape(40.dp)
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = page.icon, fontSize = 80.sp)
            }
        }
        
        Spacer(modifier = Modifier.height(48.dp))
        
        Text(
            text = page.title.uppercase(),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            color = EspressoDeep,
            letterSpacing = 1.sp
        )
        
        Spacer(modifier = Modifier.height(20.dp))
        
        Text(
            text = page.description,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = Color.Gray,
            lineHeight = 26.sp
        )
    }
}
