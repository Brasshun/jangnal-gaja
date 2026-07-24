package com.jangnal.gaja.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.*
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.google.android.gms.location.LocationServices
import com.jangnal.gaja.data.local.entity.Market
import com.kakao.vectormap.KakaoMap
import com.kakao.vectormap.KakaoMapReadyCallback
import com.kakao.vectormap.LatLng
import com.kakao.vectormap.MapLifeCycleCallback
import com.kakao.vectormap.MapView
import com.kakao.vectormap.camera.CameraUpdateFactory
import com.kakao.vectormap.label.LabelOptions
import com.kakao.vectormap.label.LabelStyle
import com.kakao.vectormap.label.LabelStyles
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DisplayMode
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi

private const val TAG = "KakaoMapScreen"

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun KakaoMapScreen(
    markets: List<Market>,
    onMarketClick: (Market) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    var kakaoMap by remember { mutableStateOf<KakaoMap?>(null) }
    var mapView by remember { mutableStateOf<MapView?>(null) }
    var isMapReady by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var userLocation by remember { mutableStateOf<LatLng?>(null) }
    
    // Filter States
    var showFilterSheet by remember { mutableStateOf(false) }
    var filterShowPermanent by remember { mutableStateOf(true) }
    var filterShowPeriodic by remember { mutableStateOf(true) }
    
    // Date Filter State
    var selectedDateMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    var showDatePicker by remember { mutableStateOf(false) }

    // Search Query State
    var searchQuery by remember { mutableStateOf("") }
    
    // Detailed Filter States
    var selectedRegions by remember { mutableStateOf<Set<String>>(emptySet()) }
    var selectedMarketDays by remember { mutableStateOf<Set<String>>(emptySet()) } // "1일", "2일"... "주말", "상설"

    val regions = listOf("서울", "인천,경기", "강원", "대전,충남", "충북", "대구,경북", "부산,경남", "광주,전남", "전북", "제주")
    val marketDayOptions = (1..10).map { "${it}일" } + listOf("주말", "상설")

    // Filter Logic Calculation
    val filteredMarkets = remember(markets, filterShowPermanent, filterShowPeriodic, selectedDateMillis, searchQuery, selectedRegions, selectedMarketDays) {
        markets.filter { market ->
            // 1. 검색어 필터 (이름 또는 주소)
            val matchesSearch = searchQuery.isBlank() || 
                market.marketName.contains(searchQuery, ignoreCase = true) || 
                market.addressRoad.contains(searchQuery, ignoreCase = true) ||
                market.addressJibun.contains(searchQuery, ignoreCase = true)

            if (!matchesSearch) return@filter false
            
            // 2. 지역 필터
            if (selectedRegions.isNotEmpty()) {
                val matchesRegion = selectedRegions.any { region ->
                    val addr = market.addressRoad + " " + market.addressJibun
                    when (region) {
                        "인천,경기" -> addr.contains("인천") || addr.contains("경기")
                        "대전,충남" -> addr.contains("대전") || addr.contains("충남") || addr.contains("세종")
                        "대구,경북" -> addr.contains("대구") || addr.contains("경북")
                        "부산,경남" -> addr.contains("부산") || addr.contains("경남") || addr.contains("울산")
                        "광주,전남" -> addr.contains("광주") || addr.contains("전남")
                        else -> addr.contains(region)
                    }
                }
                if (!matchesRegion) return@filter false
            }

            // 3. 장날 유형 필터 (1-10일, 주말, 상설)
            if (selectedMarketDays.isNotEmpty()) {
                val matchesDay = selectedMarketDays.any { opt ->
                    when (opt) {
                        "상설" -> market.isPermanent()
                        "주말" -> {
                            val cal = java.util.Calendar.getInstance()
                            cal.timeInMillis = selectedDateMillis
                            val dayOfWeek = cal.get(java.util.Calendar.DAY_OF_WEEK)
                            val isWeekend = (dayOfWeek == java.util.Calendar.SATURDAY || dayOfWeek == java.util.Calendar.SUNDAY)
                            // 선택된 날짜가 주말이고, 그날 열리는 시장인 경우
                            isWeekend && (market.isPermanent() || market.isOpenOn(selectedDateMillis))
                        }
                        else -> {
                            val digitStr = opt.replace("일", "")
                            val digit = digitStr.toIntOrNull()?.let { it % 10 } ?: -1
                            if (digit == -1) return@any false
                            
                            val targetDigits = market.openingCycle.filter { it.isDigit() }.map { it.toString().toInt() }
                            targetDigits.any { it % 10 == digit }
                        }
                    }
                }
                if (!matchesDay) return@filter false
            }

            // 4. 기본 유형 필터 (상설/정기)
            val isPerm = market.isPermanent()
            if (isPerm) {
                filterShowPermanent
            } else {
                filterShowPeriodic
            }
        }
    }
    
    // 위치 권한 요청
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true) {
            getCurrentLocation(context) { location ->
                userLocation = location
                kakaoMap?.moveCamera(
                    CameraUpdateFactory.newCenterPosition(location, 10)
                )
            }
        }
    }
    
    // 위치 권한 확인 및 요청
    LaunchedEffect(Unit) {
        when {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                getCurrentLocation(context) { location ->
                    userLocation = location
                }
            }
            else -> {
                locationPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
        }
    }
    
    // Lifecycle management
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    mapView?.resume()
                    Log.d(TAG, "MapView resumed")
                }
                Lifecycle.Event.ON_PAUSE -> {
                    mapView?.pause()
                    Log.d(TAG, "MapView paused")
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView?.pause()
        }
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        // MapView
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                MapView(ctx).apply {
                    mapView = this
                    
                    start(object : MapLifeCycleCallback() {
                        override fun onMapDestroy() {
                            Log.d(TAG, "Map destroyed")
                            isMapReady = false
                        }

                        override fun onMapError(error: Exception) {
                            Log.e(TAG, "Map error: ${error.message}", error)
                            errorMessage = "지도 로드 실패: ${error.message}"
                        }
                    }, object : KakaoMapReadyCallback() {
                        override fun onMapReady(map: KakaoMap) {
                            Log.d(TAG, "Map ready!")
                            kakaoMap = map
                            isMapReady = true
                            
                            // 한반도 전체가 보이도록 초기 위치 및 축척 설정
                            val centerPosition = LatLng.from(36.1, 127.8) // 한반도 중심 미세 조정
                            // 축척: 대한민국 전체가 보이게 -> 줌 레벨 8 (이미지 기준 최적화)
                            val zoomLevel = 8
                            map.moveCamera(
                                CameraUpdateFactory.newCenterPosition(centerPosition, zoomLevel)
                            )
                            
                            Log.d(TAG, "Camera moved to default (Korea View): $centerPosition, zoom: $zoomLevel")
                        }
                    })
                }
            }
        )

        // 검색창 (지도 위에 띄움)
        androidx.compose.foundation.layout.Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .align(Alignment.TopCenter)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface, androidx.compose.foundation.shape.RoundedCornerShape(8.dp)),
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
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )
        }
        
        // Loading indicator
        if (!isMapReady && errorMessage == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
        
        // Error message
        if (errorMessage != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = errorMessage ?: "",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
        
        // 범례 (Legend) 및 컨트롤
        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 필터 버튼
            FloatingActionButton(
                onClick = { showFilterSheet = true },
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = "필터 (상세조건)"
                    )
                    Text(
                        text = "상세조건",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
            
            // 현재 위치 버튼
            FloatingActionButton(
                onClick = {
                    userLocation?.let { location ->
                        kakaoMap?.moveCamera(
                            CameraUpdateFactory.newCenterPosition(location, 14) // 줌 레벨 14로 변경
                        )
                    } ?: run {
                        locationPermissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    }
                },
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Icon(
                    imageVector = Icons.Default.MyLocation,
                    contentDescription = "내 위치"
                )
            }
        }
        
        // 하단 정보 카드 (범례 포함)
        Card(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 상단: 제목 및 통계
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        val dateFormat = java.text.SimpleDateFormat("M월 d일 (E)", java.util.Locale.KOREA)
                        val dateText = if (android.text.format.DateUtils.isToday(selectedDateMillis)) "오늘" else dateFormat.format(java.util.Date(selectedDateMillis))
                        
                        Text(
                            text = "시장 지도 ($dateText)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                        )
                        Text(
                            text = "${filteredMarkets.size}개 시장 표시 중",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                // 구분선
                Divider(color = MaterialTheme.colorScheme.outlineVariant)
                
                // 하단: 범례 (Legend)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LegendItem(color = android.graphics.Color.parseColor("#FF6B35"), label = "장날 (선택일)")
                    LegendItem(color = android.graphics.Color.parseColor("#4CAF50"), label = "상설 시장")
                    LegendItem(color = android.graphics.Color.parseColor("#AAAAAA"), label = "쉬는 날")
                }
            }
        }
    }

    // Filter Sheet
    if (showFilterSheet) {
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(
            onDismissRequest = { showFilterSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "상세조건 설정",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                )
                
                Divider()

                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // 1. 날짜 선택
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "날짜", style = MaterialTheme.typography.titleMedium, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                        TextButton(onClick = { selectedDateMillis = System.currentTimeMillis() }) {
                            Text("오늘", color = com.jangnal.gaja.ui.theme.JangnalOrange)
                        }
                    }
                    
                    // Simple Calendar Style View (Represented by Button for now, or DatePicker UI)
                    // The user screenshot shows a calendar, but we have a DatePicker already.
                    // For premium feel, let's keep the date display nice.
                    val dateFormat = java.text.SimpleDateFormat("yyyy년 M월 d일 (E)", java.util.Locale.KOREA)
                    OutlinedButton(
                        onClick = { showDatePicker = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(text = dateFormat.format(java.util.Date(selectedDateMillis)))
                    }

                    // 2. 지역 선택
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "지역", style = MaterialTheme.typography.titleMedium, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                        TextButton(onClick = { selectedRegions = emptySet() }) {
                            Text("선택취소", color = com.jangnal.gaja.ui.theme.JangnalOrange)
                        }
                    }
                    
                    // Grid-like layout for regions
                    androidx.compose.foundation.layout.FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedRegions.isEmpty(),
                            onClick = { selectedRegions = emptySet() },
                            label = { Text("전국") }
                        )
                        regions.forEach { region ->
                            FilterChip(
                                selected = selectedRegions.contains(region),
                                onClick = {
                                    selectedRegions = if (selectedRegions.contains(region)) {
                                        selectedRegions - region
                                    } else {
                                        selectedRegions + region
                                    }
                                },
                                label = { Text(region) }
                            )
                        }
                    }

                    // 3. 장날 선택
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "장날", style = MaterialTheme.typography.titleMedium, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                        TextButton(onClick = { selectedMarketDays = emptySet() }) {
                            Text("선택취소", color = com.jangnal.gaja.ui.theme.JangnalOrange)
                        }
                    }

                    androidx.compose.foundation.layout.FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        marketDayOptions.forEach { opt ->
                            FilterChip(
                                selected = selectedMarketDays.contains(opt),
                                onClick = {
                                    selectedMarketDays = if (selectedMarketDays.contains(opt)) {
                                        selectedMarketDays - opt
                                    } else {
                                        selectedMarketDays + opt
                                    }
                                },
                                label = { Text(opt) }
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { showFilterSheet = false },
                            modifier = Modifier.weight(1f),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                        ) {
                            Text("적용하기", fontSize = 16.sp)
                        }
                        
                        OutlinedButton(
                            onClick = { 
                                selectedRegions = emptySet()
                                selectedMarketDays = emptySet()
                                selectedDateMillis = System.currentTimeMillis()
                                filterShowPermanent = true
                                filterShowPeriodic = true
                            },
                            modifier = Modifier.weight(1f),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                        ) {
                            Text("전체 선택취소", fontSize = 16.sp)
                        }
                    }
                }
            }
        }
    }
    
    // Move Camera to Nearest Search Result
    LaunchedEffect(filteredMarkets, searchQuery) {
        if (!isMapReady) return@LaunchedEffect
        val map = kakaoMap ?: return@LaunchedEffect
        
        // 검색어가 있고 결과가 있을 때만 자동 이동
        if (searchQuery.isNotBlank() && filteredMarkets.isNotEmpty()) {
            val targetMarket = if (userLocation != null) {
                filteredMarkets.minByOrNull { market ->
                    val results = FloatArray(1)
                    android.location.Location.distanceBetween(
                        userLocation!!.latitude, userLocation!!.longitude,
                        market.latitude, market.longitude,
                        results
                    )
                    results[0]
                }
            } else {
                filteredMarkets.firstOrNull()
            }
            
            if (targetMarket != null) {
                 // 너무 잦은 이동 방지를 위해 약간의 딜레이가 있으면 좋지만, 여기선 즉시 이동
                map.moveCamera(
                    CameraUpdateFactory.newCenterPosition(LatLng.from(targetMarket.latitude, targetMarket.longitude), 14)
                )
            }
        }
    }
    
    // Date Picker Dialog (Existing code...)
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDateMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { selectedDateMillis = it }
                    showDatePicker = false
                }) {
                    Text("확인")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("취소")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // Update Markers when map is ready or filtered list changes
    // filteredMarkets 가 변경되면 자동으로 recomposition 및 LaunchedEffect 트리거됨
    // selectedDateMillis가 변경되어도 마커 색상(Open/Closed)을 갱신해야 하므로 키에 추가 필수!
    LaunchedEffect(isMapReady, filteredMarkets, selectedDateMillis) {
        if (!isMapReady) return@LaunchedEffect
        
        val map = kakaoMap ?: return@LaunchedEffect
        val labelManager = map.labelManager ?: return@LaunchedEffect
        val layer = labelManager.layer ?: return@LaunchedEffect

        try {
            // Log for debug
            val debugDate = java.util.Date(selectedDateMillis)
            val openPeriodicCount = filteredMarkets.count { !it.isPermanent() && it.isOpenOn(selectedDateMillis) }
            Log.d(TAG, "Updating markers. Date: $debugDate, Total: ${filteredMarkets.size}, Open Periodic: $openPeriodicCount")
            
            // 디버깅: '2'가 포함된 주기 시장 하나 찾아서 상태 로그 찍기
            val debugMarket = filteredMarkets.find { !it.isPermanent() && it.openingCycle.contains("2") && it.openingCycle.contains("7") }
            if (debugMarket != null) {
                Log.d(TAG, "DEBUG MARKET: ${debugMarket.marketName}, Cycle: ${debugMarket.openingCycle}")
                Log.d(TAG, "IsOpenOn($debugDate) => ${debugMarket.isOpenOn(selectedDateMillis)}")
                val (cycle, startDays) = debugMarket.parseCyclePublic()
                Log.d(TAG, "Parsed: Cycle=$cycle, StartDays=$startDays")
                val days = debugMarket.getMarketDaysInMonth(31)
                Log.d(TAG, "OpenDays: $days")
            } else {
                Log.d(TAG, "No 2+7 market found for debug.")
            }
            
            layer.removeAll()
            
            // 사용자가 선택한 필터에 따라 걸러진 목록만 마커로 표시
            // (filterShowPeriodic=false 면 5일장 아예 안나옴)
            
            // 마커 생성 코드 재사용...
            val todayStyle = try { LabelStyles.from(LabelStyle.from(com.jangnal.gaja.R.drawable.ic_marker_today)) } catch (e: Exception) { null }
            val normalStyle = try { LabelStyles.from(LabelStyle.from(com.jangnal.gaja.R.drawable.ic_marker_normal)) } catch (e: Exception) { null }
            val permanentStyle = try { LabelStyles.from(LabelStyle.from(com.jangnal.gaja.R.drawable.ic_marker_permanent)) } catch (e: Exception) { null }

            // filteredMarkets에는 이미 사용자가 "보고 싶은" 시장만 들어있음.
            // 단, "5일장이지만 오늘 안 열리는 애들"은 filtering 로직에서 isOpenOn 체크에 의해 걸려졌는지 확인 필요.
            // Logic:
            // if (isPerm) filterShowPermanent
            // else filterShowPeriodic && market.isOpenOn(selectedDateMillis)
            // 즉, " Periodic 체크 " AND " Open on Date "
            // -> 이 경우 "Closed Periodic Markets"는 아예 안 나옴.
            // 요구사항: "오늘 안열리는 장도 최초 지도에 같이 회색으로 나온다".
            // 그러려면 필터 로직이 "Show Periodic" 이면 다 보여주되, Open 여부에 따라 색상만 달라야 함.
            // 그런데 위 getFilteredList 로직은 isOpenOn 인 것만 필터링함.
            // 수정 필요: "5일장 보고 싶음" -> 5일장이면 다 보여주고, 오늘 열리면 주황, 닫히면 회색.
            // 그래야 "회색" 마커가 의미가 있음.
            // 수정된 로직 반영 필요.
            
            // 하지만 remember 블록은 이미 compose scope 위쪽에 정의됨.
            // LaunchedEffect 안에서는 filteredMarkets를 그대로 사용.
            // 위쪽 'filteredMarkets' 로직을 수정해야 함. (직접 수정 불가, 이 tool call 범위 밖)
            // 따라서 여기서 로직을 다시 짜는 게 아니라, 위쪽 'filteredMarkets' 로직 수정 tool call을 하나 더 보내야 함.
            // 일단 이 tool call은 UI 렌더링 부분임.
           
            filteredMarkets.forEach { market ->
                if (market.latitude != 0.0 && market.longitude != 0.0) {
                    try {
                        val isPermanent = market.isPermanent()
                        val isFavorite = market.isFavorite
                        // 선택된 날짜에 열리는지 확인
                        val isOpenOnSelectedDate = market.isOpenOn(selectedDateMillis)
                        
                        // 즐겨찾기인 경우 다른 색상이나 스타일을 적용할 수 있으면 좋음
                        // 여기서는 우선 순위를 가장 높게 둠 (Rank)
                        val style = when {
                            isPermanent -> permanentStyle ?: todayStyle
                            isOpenOnSelectedDate -> todayStyle
                            else -> normalStyle
                        }
                        
                        val position = LatLng.from(market.latitude, market.longitude)
                        val options = LabelOptions.from(position).apply {
                            if (style != null) setStyles(style)
                            
                            // 즐겨찾기면 텍스트 라벨을 추가하거나 랭크를 더 높임
                            val rank = when {
                                isFavorite -> 10L // 즐겨찾기 최우선
                                !isPermanent && isOpenOnSelectedDate -> 2L
                                isPermanent -> 1L
                                else -> 0L
                            }
                            setRank(rank)
                            
                            // setTexts는 스타일 설정이 복잡하므로 우선 순위(rank) 조정으로 만족
                            // if (isFavorite) { setTexts("★ ${market.marketName}") }

                            setTag(market.id)
                            setClickable(true)
                        }
                        layer.addLabel(options)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to add marker", e)
                    }
                }
            }
            
            // Listener
            map.setOnLabelClickListener { _, _, label ->
                val marketId = label.tag as? Long
                if (marketId != null) {
                    val market = markets.find { it.id == marketId }
                    if (market != null) onMarketClick(market)
                }
                true
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error updating markers", e)
        }
    }
}

// 범례 아이템 컴포넌트
@Composable
fun LegendItem(color: Int, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        // 원형 마커 아이콘 흉내
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(androidx.compose.ui.graphics.Color(color), androidx.compose.foundation.shape.CircleShape)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

// 현재 위치 가져오기
private fun getCurrentLocation(
    context: android.content.Context,
    onLocationReceived: (LatLng) -> Unit
) {
    try {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    val latLng = LatLng.from(it.latitude, it.longitude)
                    onLocationReceived(latLng)
                    Log.d(TAG, "Current location: ${it.latitude}, ${it.longitude}")
                }
            }
        }
    } catch (e: Exception) {
        Log.e(TAG, "Failed to get current location", e)
    }
}
