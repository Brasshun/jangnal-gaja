package com.jangnal.gaja.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onDismiss: () -> Unit,
    currentScale: Float,
    onScaleChange: (Float) -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false) // 전체 화면 사용
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("앱 정보 및 도움말") },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "뒤로 가기")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = com.jangnal.gaja.ui.theme.JangnalYellow,
                        titleContentColor = com.jangnal.gaja.ui.theme.JangnalBrown,
                        navigationIconContentColor = com.jangnal.gaja.ui.theme.JangnalBrown
                    )
                )
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // 0. 글자 크기 설정
                TextSizeSettingCard(currentScale, onScaleChange)

                // 1. 5일장이란?
                InfoCard(
                    title = "🍎 5일장이란 무엇인가요?",
                    content = "5일장은 조선시대부터 이어져 온 우리의 전통 시장입니다. \n\n" +
                            "5일 간격으로 장이 열린다는 뜻으로, 예를 들어 '1, 6일장'이라면 매달 날짜의 끝자리가 1일과 6인 날에 열립니다.\n\n" +
                            "예) 1일, 6일, 11일, 16일, 21일, 26일, 31일"
                )

                // 2. 서비스 소개
                InfoCard(
                    title = "📱 '장날가자' 서비스 소개",
                    content = "복잡한 장날 계산, 이제 '장날가자'가 대신 해드립니다!\n\n" +
                            "'장날가자'는 전국 방방곡곡의 5일장과 정기시장, 상설시장 정보를 쉽고 편리하게 제공하는 전통시장 지도 서비스입니다.\n\n" +
                            "✅ 오늘 바로 열리는 시장 확인\n" +
                            "✅ 내 위치 기준 가까운 시장 찾기\n" +
                            "✅ 맛있는 먹거리와 특산물 정보\n" +
                            "✅ 간편한 길찾기와 전화 연결\n\n" +
                            "우리 동네 장날, 이제 잊지 말고 '장날가자'와 함께하세요."
                )

                // 3. 사용 팁
                InfoCard(
                    title = "💡 알뜰 사용 팁",
                    content = "• [목록] 탭에서 전국의 모든 시장을 찾아볼 수 있습니다.\n" +
                            "• [지도] 탭에서 내 주변의 '오늘 열리는' 시장을 지도로 확인하세요.\n" +
                            "• 검색창을 통해 원하는 시장이나 지역을 빠르게 찾을 수 있습니다.\n" +
                            "• 거리순 정렬을 이용해 가장 가까운 시장부터 확인해보세요."
                )

                // 4. 개인정보처리방침
                InfoCard(
                    title = "🔒 개인정보 처리방침",
                    content = "1. 개인정보의 처리 목적\n" +
                            "'장날가자'는 사용자의 개인정보를 별도로 수집, 저장, 전송하지 않습니다.\n\n" +
                            "2. 위치 정보의 사용\n" +
                            "가까운 시장 찾기 및 지도 표시 기능을 제공하기 위해 위치 정보를 사용합니다. 이 정보는 기기 내부에서만 일시적으로 처리되며 외부 서버로 전송되거나 저장되지 않습니다.\n\n" +
                            "3. 앱 권한 안내\n" +
                            "- 위치: 내 주변 시장 찾기 및 지도 이동\n" +
                            "- 전화: 시장 관리 사무소 전화 연결\n\n" +
                            "본 앱은 사용자의 프라이버시를 최우선으로 보호합니다."
                )
                
                Spacer(modifier = Modifier.height(20.dp))
                
                Text(
                    text = "버전 1.1.0 | 만든이: 콜코(COLLCO)",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@Composable
fun InfoCard(title: String, content: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = content,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun TextSizeSettingCard(currentScale: Float, onScaleChange: (Float) -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "🔎 글자 크기 조절",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SizeButton("기본", 1.0f, currentScale, onScaleChange, Modifier.weight(1f))
                SizeButton("크게", 1.25f, currentScale, onScaleChange, Modifier.weight(1f))
                SizeButton("왕크게", 1.5f, currentScale, onScaleChange, Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun SizeButton(text: String, scale: Float, currentScale: Float, onScaleChange: (Float) -> Unit, modifier: Modifier = Modifier) {
    val isSelected = (Math.abs(currentScale - scale) < 0.01f)
    Button(
        onClick = { onScaleChange(scale) },
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceVariant,
            contentColor = if (isSelected) Color.White else Color.Black
        ),
        border = if (!isSelected) androidx.compose.foundation.BorderStroke(1.dp, Color.Gray) else null
    ) {
        Text(text, fontSize = 14.sp * scale, maxLines = 1) 
    }
}
