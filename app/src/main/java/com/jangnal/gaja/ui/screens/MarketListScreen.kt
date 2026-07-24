package com.jangnal.gaja.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jangnal.gaja.data.local.entity.Market
import com.jangnal.gaja.ui.components.MarketItem
import com.jangnal.gaja.ui.viewmodel.MarketViewModel
import kotlinx.coroutines.launch

import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.runtime.mutableStateOf
import androidx.compose.foundation.background
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Clear
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.fillMaxWidth

import com.google.android.gms.location.LocationServices
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import android.location.Location
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MarketListScreen(
    viewModel: MarketViewModel,
    onMarketClick: (Market) -> Unit,
    currentTextScale: Float,
    onTextScaleChange: (Float) -> Unit
) {
    val context = LocalContext.current
    var userLocation by remember { mutableStateOf<Location?>(null) }
    
    // 위치 권한 요청 Launcher
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true) {
            try {
                LocationServices.getFusedLocationProviderClient(context).lastLocation.addOnSuccessListener { 
                    userLocation = it
                }
            } catch (e: SecurityException) {
                // Ignore
            }
        }
    }
    
    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            LocationServices.getFusedLocationProviderClient(context).lastLocation.addOnSuccessListener { 
                userLocation = it
            }
        } else {
            // 권한 없으면 요청 (선택 사항, 지도 탭에서 이미 했을 수도 있음)
            locationPermissionLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            )
        }
    }

    val todayMarkets by viewModel.todayMarkets.collectAsState()
    val permanentMarkets by viewModel.permanentMarkets.collectAsState()
    val allMarkets by viewModel.allMarkets.collectAsState()
    val todayDate by viewModel.today.collectAsState()
    
    // 날짜 포맷팅
    val dateFormat = remember { SimpleDateFormat("M월 d일 (E)", Locale.KOREA) }
    val formattedDate = dateFormat.format(Date(todayDate))

    val tabs = listOf("지도", "오늘 장 ($formattedDate)", "상설 시장", "전체 목록/검색")
    val pagerState = rememberPagerState(
        initialPage = 0, // 지도 탭을 기본으로
        pageCount = { tabs.size }
    )
    val scope = rememberCoroutineScope()
    
    // State for Detail BottomSheet
    var selectedMarket by remember { androidx.compose.runtime.mutableStateOf<Market?>(null) }
    
    // State for About Screen
    var showAbout by remember { mutableStateOf(false) }

    if (selectedMarket != null) {
        com.jangnal.gaja.ui.components.MarketDetailSheet(
            market = selectedMarket!!,
            onFavoriteToggle = { viewModel.toggleFavorite(it) },
            onVoteClick = { marketId, isOpen -> viewModel.voteMarketStatus(marketId, isOpen) },
            onDismissRequest = { selectedMarket = null }
        )
    }
    
    if (showAbout) {
        AboutScreen(
            onDismiss = { showAbout = false },
            currentScale = currentTextScale,
            onScaleChange = onTextScaleChange
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    androidx.compose.foundation.Image(
                        painter = androidx.compose.ui.res.painterResource(id = com.jangnal.gaja.R.drawable.header_logo),
                        contentDescription = "장날 가자",
                        modifier = Modifier.height(40.dp),
                        contentScale = androidx.compose.ui.layout.ContentScale.Fit
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = com.jangnal.gaja.ui.theme.JangnalYellow,
                    titleContentColor = com.jangnal.gaja.ui.theme.JangnalBrown,
                    actionIconContentColor = com.jangnal.gaja.ui.theme.JangnalBrown
                ),
                actions = {
                    IconButton(onClick = { showAbout = true }) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "앱 정보"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).background(com.jangnal.gaja.ui.theme.BackgroundCream)) {
            ScrollableTabRow(
                selectedTabIndex = pagerState.currentPage,
                containerColor = com.jangnal.gaja.ui.theme.JangnalYellow,
                contentColor = com.jangnal.gaja.ui.theme.JangnalBrown,
                edgePadding = 0.dp,
                indicator = { tabPositions ->
                    TabRowDefaults.Indicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[pagerState.currentPage]),
                        color = com.jangnal.gaja.ui.theme.JangnalBrown,
                        height = 4.dp
                    )
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        text = { 
                            Text(
                                title, 
                                fontWeight = if(pagerState.currentPage == index) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Normal,
                                fontSize = 18.sp
                            ) 
                        },
                        selected = pagerState.currentPage == index,
                        onClick = {
                            scope.launch { pagerState.animateScrollToPage(index) }
                        },
                        selectedContentColor = com.jangnal.gaja.ui.theme.JangnalBrown,
                        unselectedContentColor = com.jangnal.gaja.ui.theme.JangnalBrown.copy(alpha = 0.6f)
                    )
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                userScrollEnabled = pagerState.currentPage != 0 // 지도 탭(0번)에서는 스와이프 비활성화
            ) { page ->
                when (page) {
                    0 -> { // Map (맨 앞으로 이동)
                        val mapMarkets = allMarkets
                        if (mapMarkets.isEmpty()) {
                            EmptyState("지도에 표시할 시장이 없습니다.")
                        } else {
                            // Map does not need MarketList's sorting UI yet, but user asked for search nearest logic in Map.
                            // That is handled in KakaoMapScreen separately.
                            KakaoMapScreen(markets = mapMarkets, onMarketClick = { selectedMarket = it })
                        }
                    }
                    1 -> { // Today 5-day Markets
                        if (todayMarkets.isEmpty()) {
                            EmptyState("오늘 ($formattedDate) 열리는 5일장이 없습니다.")
                        } else {
                            MarketList(markets = todayMarkets, dateLabel = formattedDate, userLocation = userLocation, onMarketClick = { selectedMarket = it })
                        }
                    }
                    2 -> { // Permanent Markets
                        if (permanentMarkets.isEmpty()) {
                            EmptyState("등록된 상설시장이 없습니다.")
                        } else {
                             MarketList(markets = permanentMarkets, dateLabel = "상설시장 (매일)", userLocation = userLocation, onMarketClick = { selectedMarket = it })
                        }
                    }
                    3 -> { // All List
                        MarketList(markets = allMarkets, dateLabel = "전체 목록", userLocation = userLocation, onMarketClick = { selectedMarket = it })
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketList(
    markets: List<Market>, 
    dateLabel: String?, 
    userLocation: Location?,
    onMarketClick: (Market) -> Unit
) {
    if (markets.isEmpty()) {
         EmptyState("데이터가 없습니다.")
    } else {
        var searchQuery by remember { mutableStateOf("") }
        // 0: 가나다순, 1: 거리순
        var sortType by remember { mutableIntStateOf(0) }
        
        val filteredMarkets = remember(markets, searchQuery, sortType, userLocation) {
            val filtered = if (searchQuery.isBlank()) markets
            else markets.filter { 
                it.marketName.contains(searchQuery, ignoreCase = true) ||
                it.addressRoad.contains(searchQuery, ignoreCase = true) ||
                it.addressJibun.contains(searchQuery, ignoreCase = true)
            }
            
            when (sortType) {
                1 -> { // 거리순
                    if (userLocation != null) {
                         filtered.sortedBy { market ->
                             val results = FloatArray(1)
                             Location.distanceBetween(
                                 userLocation.latitude, userLocation.longitude,
                                 market.latitude, market.longitude,
                                 results
                             )
                             results[0]
                         }
                    } else {
                        filtered.sortedBy { it.marketName }
                    }
                }
                else -> filtered.sortedBy { it.marketName } // 가나다순
            }
        }

        Column(modifier = Modifier.fillMaxSize()) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { Text("시장 이름 또는 주소 검색") },
                leadingIcon = { 
                    Icon(imageVector = Icons.Default.Search, contentDescription = "검색") 
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(imageVector = Icons.Default.Clear, contentDescription = "지우기")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )
            
            // 정렬 옵션
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = sortType == 0,
                    onClick = { sortType = 0 },
                    label = { Text("가나다순") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = com.jangnal.gaja.ui.theme.JangnalYellow,
                        selectedLabelColor = com.jangnal.gaja.ui.theme.JangnalBrown
                    )
                )
                FilterChip(
                    selected = sortType == 1,
                    onClick = { sortType = 1 },
                    label = { Text("거리순") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = com.jangnal.gaja.ui.theme.JangnalYellow,
                        selectedLabelColor = com.jangnal.gaja.ui.theme.JangnalBrown
                    ),
                    enabled = userLocation != null
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))

            if (filteredMarkets.isEmpty()) {
                EmptyState("검색 결과가 없습니다.")
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    if (dateLabel != null) {
                        item {
                            Text(
                                text = "오늘 날짜: $dateLabel",
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                    }
                    items(filteredMarkets) { market ->
                        MarketItem(market = market, onItemClick = onMarketClick)
                    }
                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }
            }
        }
    }
}

@Composable
fun EmptyState(message: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
