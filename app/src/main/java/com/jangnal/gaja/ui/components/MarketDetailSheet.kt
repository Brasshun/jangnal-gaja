package com.jangnal.gaja.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import com.jangnal.gaja.data.local.entity.Market
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketDetailSheet(
    market: Market,
    onFavoriteToggle: (Market) -> Unit = {},
    onVoteClick: (Long, Boolean) -> Unit = { _, _ -> },
    onDismissRequest: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        val specialtyParts = market.specialty.split("\n\n💡 특징: ")
        val displaySpecialty = specialtyParts[0].replace("+", ", ")
        val displayFeature = if (specialtyParts.size > 1) specialtyParts[1] else ""

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, bottom = 48.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header - 시장 이름 및 즐겨찾기
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = market.marketName,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                
                androidx.compose.material3.IconButton(
                    onClick = { onFavoriteToggle(market) }
                ) {
                    Icon(
                        imageVector = if (market.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "즐겨찾기",
                        tint = if (market.isFavorite) Color.Red else MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            
            // 시장 유형 뱃지
            Badge(
                text = market.getSimpleTypeText(),
                bgColor = MaterialTheme.colorScheme.primaryContainer,
                textColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            Divider()
            Spacer(modifier = Modifier.height(24.dp))

            // 개장 정보 섹션 (헬스케어/달력 스타일 반영)
            Text(
                text = "📅 개장 정보 및 달력",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            // 캘린더 형태의 시각적 요소 제공
            MarketCalendarView(market = market)
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (!market.isPermanent()) {
                InfoItem(label = "시장 유형", value = market.getMarketTypeName())
                Spacer(modifier = Modifier.height(8.dp))
                
                val (cycle, _) = market.parseCyclePublic()
                if (cycle != null) {
                    InfoItem(label = "개장 주기", value = "${cycle}일 주기")
                    Spacer(modifier = Modifier.height(8.dp))
                }
                
                InfoItem(
                    label = "다음 개장일", 
                    value = market.getNextMarketText(),
                    highlight = market.isOpenToday()
                )
            } else {
                InfoItem(label = "운영 주기", value = "매일 상설 운영")
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            Divider()
            Spacer(modifier = Modifier.height(24.dp))
            
            // 편의 시설 섹션 (카드/아이콘화)
            Text(
                text = "🏗 편의 시설",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AmenityCard(
                    label = "공중화장실",
                    icon = "🚻",
                    hasAmenity = market.hasToilet == "Y",
                    modifier = Modifier.weight(1f)
                )
                AmenityCard(
                    label = "주차 공간",
                    icon = "🅿️",
                    hasAmenity = market.hasParking == "Y",
                    modifier = Modifier.weight(1f)
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            Divider()
            Spacer(modifier = Modifier.height(24.dp))

            // 실시간 제보 및 투표 (크라우드소싱)
            Text(
                text = "💬 실시간 장날 제보",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "오늘 시장이 열렸는지 현장의 소식을 실시간으로 공유해 주세요!",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { onVoteClick(market.id, true) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Text("👍 오늘 열렸어요", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text("${market.voteOpenTodayCount}명 제보", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                
                OutlinedButton(
                    onClick = { onVoteClick(market.id, false) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Text("👎 닫혔어요/안열려요", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text("${market.voteClosedTodayCount}명 제보", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Divider()
            Spacer(modifier = Modifier.height(24.dp))
            
            // 위치 및 주요 품목 정보
            DetailRow(Icons.Default.LocationOn, market.addressRoad.ifEmpty { market.addressJibun })
            Spacer(modifier = Modifier.height(16.dp))
            
            DetailRow(Icons.Default.ShoppingBag, "주요 품목: $displaySpecialty")
            Spacer(modifier = Modifier.height(16.dp))
            
            if (displayFeature.isNotEmpty()) {
                Text(
                    text = "💡 특징",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = displayFeature,
                    style = MaterialTheme.typography.bodyLarge,
                    lineHeight = 24.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            if (market.phoneNumber.isNotEmpty()) {
                Row(
                   modifier = Modifier
                       .fillMaxWidth()
                       .clickable {
                           val intent = Intent(Intent.ACTION_DIAL).apply {
                               data = "tel:${market.phoneNumber}".toUri()
                           }
                           try {
                               context.startActivity(intent)
                           } catch (_: Exception) {
                               // Ignore
                           }
                       }
                ) {
                    DetailRow(Icons.Default.Phone, market.phoneNumber)
                }
                Spacer(modifier = Modifier.height(24.dp))
            } else {
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { shareMarket(context, market) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("공유하기")
                }
                
                Button(
                    onClick = { openMap(context, market) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(imageVector = Icons.Default.Directions, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("길찾기")
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Close Button
            OutlinedButton(
                onClick = onDismissRequest,
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Text("닫기", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun MarketCalendarView(market: Market) {
    val calendar = Calendar.getInstance()
    val currentDay = calendar.get(Calendar.DAY_OF_MONTH)
    val maxDay = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    val year = calendar.get(Calendar.YEAR)
    val month = calendar.get(Calendar.MONTH) + 1
    
    // 이번 달 장날 목록 계산
    val marketDays = market.getMarketDaysInMonth(maxDay).toSet()
    
    // 달력 행렬 계산을 위해 이번 달 1일의 요일 계산
    val firstDayCal = Calendar.getInstance()
    firstDayCal.set(Calendar.DAY_OF_MONTH, 1)
    val startDayOfWeek = firstDayCal.get(Calendar.DAY_OF_WEEK) - 1 // 0 (일) ~ 6 (토)
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Text(
            text = "${year}년 ${month}월 장날 예측 달력",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        // 요일 헤더
        val weekDays = listOf("일", "월", "화", "수", "목", "금", "토")
        Row(modifier = Modifier.fillMaxWidth()) {
            weekDays.forEachIndexed { idx, day ->
                Text(
                    text = day,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = if (idx == 0) Color.Red.copy(alpha = 0.7f) else if (idx == 6) Color.Blue.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // 날짜 렌더링
        var dayCounter = 1
        val rowsCount = (maxDay + startDayOfWeek + 6) / 7
        
        for (r in 0 until rowsCount) {
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                for (c in 0..6) {
                    val cellIdx = r * 7 + c
                    if (cellIdx < startDayOfWeek || dayCounter > maxDay) {
                        Spacer(modifier = Modifier.weight(1f))
                    } else {
                        val day = dayCounter
                        val isMarketDay = marketDays.contains(day)
                        val isToday = day == currentDay
                        val isPermanent = market.isPermanent()
                        val isOpened = isMarketDay || isPermanent
                        
                        val cellBg = when {
                            isToday && isOpened -> MaterialTheme.colorScheme.primary
                            isOpened -> MaterialTheme.colorScheme.primaryContainer
                            isToday -> MaterialTheme.colorScheme.surfaceVariant
                            else -> Color.Transparent
                        }
                        
                        val cellTextCol = when {
                            isToday && isOpened -> MaterialTheme.colorScheme.onPrimary
                            isOpened -> MaterialTheme.colorScheme.onPrimaryContainer
                            isToday -> MaterialTheme.colorScheme.onSurfaceVariant
                            else -> MaterialTheme.colorScheme.onSurface
                        }
                        
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(cellBg)
                                .padding(4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = day.toString(),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isToday || isOpened) FontWeight.Bold else FontWeight.Normal,
                                color = cellTextCol,
                                textAlign = TextAlign.Center
                            )
                        }
                        dayCounter++
                    }
                }
            }
        }
    }
}

@Composable
fun AmenityCard(
    label: String,
    icon: String,
    hasAmenity: Boolean,
    modifier: Modifier = Modifier
) {
    val bg = if (hasAmenity) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
    val borderCol = if (hasAmenity) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
    val textCol = if (hasAmenity) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
    
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = bg,
        border = BorderStroke(1.dp, borderCol)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(text = icon, fontSize = 20.sp, modifier = Modifier.padding(end = 8.dp))
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = textCol
                )
                Text(
                    text = if (hasAmenity) "이용 가능" else "정보 없음",
                    style = MaterialTheme.typography.labelSmall,
                    color = textCol
                )
            }
        }
    }
}

@Composable
private fun InfoItem(
    label: String,
    value: String,
    highlight: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (highlight) FontWeight.Bold else FontWeight.Normal,
            color = if (highlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun DetailRow(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

private fun openMap(context: Context, market: Market) {
    val uri = if (market.latitude != 0.0 && market.longitude != 0.0) {
        "geo:${market.latitude},${market.longitude}?q=${Uri.encode(market.marketName)}".toUri()
    } else {
        "geo:0,0?q=${Uri.encode(market.addressRoad)}".toUri()
    }
    
    val intent = Intent(Intent.ACTION_VIEW, uri)
    try {
        context.startActivity(intent)
    } catch (_: Exception) {
        // Ignore
    }
}

private fun shareMarket(context: Context, market: Market) {
    val shareText = buildString {
        appendLine("[장날가자] ${market.marketName}")
        appendLine()
        if (market.isOpenToday()) {
            appendLine("🎉 오늘 장 열리는 날!")
        }
        appendLine("📍 주소: ${market.addressRoad.ifEmpty { market.addressJibun }}")
        appendLine("📅 일정: ${if(market.isPermanent()) "매일 운영" else market.openingCycle}")
        appendLine("🍎 주요 품목: ${market.getCleanSpecialty()}")
        appendLine()
        appendLine("더 자세한 정보는 '장날가자' 앱에서 확인하세요!")
    }
    
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, shareText)
    }
    
    val chooser = Intent.createChooser(intent, "시장 정보 공유하기")
    try {
        context.startActivity(chooser)
    } catch (_: Exception) {
        // Ignore
    }
}
