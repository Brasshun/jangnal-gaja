package com.jangnal.gaja

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jangnal.gaja.ui.screens.MarketListScreen
import com.jangnal.gaja.ui.theme.JangnalGajaTheme
import com.jangnal.gaja.ui.viewmodel.MarketViewModel
import com.jangnal.gaja.ui.viewmodel.MarketViewModelFactory
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 애플리케이션 클래스에서 의존성 가져오기
        val app = application as MarketApplication
        val repository = app.repository
        val viewModelFactory = MarketViewModelFactory(repository)

        // 키 해시 출력 (카카오맵 연동용)
        try {
            @Suppress("DEPRECATION")
            val info = packageManager.getPackageInfo(packageName, android.content.pm.PackageManager.GET_SIGNATURES)
            info.signatures?.forEach { signature ->
                val md = java.security.MessageDigest.getInstance("SHA")
                md.update(signature.toByteArray())
                val keyHash = android.util.Base64.encodeToString(md.digest(), android.util.Base64.NO_WRAP)
                android.util.Log.d("KeyHash", "KeyHash: $keyHash")
            }
        } catch (e: Exception) {
            android.util.Log.e("KeyHash", "해시 키를 가져올 수 없습니다.", e)
        }

        setContent {
            val context = androidx.compose.ui.platform.LocalContext.current
            val sharedPref = androidx.compose.runtime.remember { 
                context.getSharedPreferences("settings", android.content.Context.MODE_PRIVATE) 
            }
            var textScale by androidx.compose.runtime.remember { 
                androidx.compose.runtime.mutableFloatStateOf(sharedPref.getFloat("text_scale", 1.0f)) 
            }
            
            val onScaleChange: (Float) -> Unit = { newScale ->
                textScale = newScale
                sharedPref.edit().putFloat("text_scale", newScale).apply()
            }

            JangnalGajaTheme(textScale = textScale) {
                var showExitDialog by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }

                if (showExitDialog) {
                    androidx.compose.material3.AlertDialog(
                        onDismissRequest = { showExitDialog = false },
                        title = { androidx.compose.material3.Text("종료 확인") },
                        text = { androidx.compose.material3.Text("앱을 종료하시겠습니까?") },
                        confirmButton = {
                            androidx.compose.material3.TextButton(onClick = { finish() }) {
                                androidx.compose.material3.Text("종료")
                            }
                        },
                        dismissButton = {
                            androidx.compose.material3.TextButton(onClick = { showExitDialog = false }) {
                                androidx.compose.material3.Text("취소")
                            }
                        }
                    )
                }

                androidx.activity.compose.BackHandler(enabled = true) {
                    showExitDialog = true
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // ViewModel 생성
                    val viewModel: MarketViewModel = viewModel(factory = viewModelFactory)
                    
                    // 초기 데이터 로딩 체크
                    // LaunchedEffect를 사용하여 컴포지션 진입 시 1회 실행
                    androidx.compose.runtime.LaunchedEffect(Unit) {
                        viewModel.checkAndLoadInitialData(this@MainActivity)
                    }
                    
                    // 메인 화면 표시
                    MarketListScreen(
                        viewModel = viewModel,
                        currentTextScale = textScale,
                        onTextScaleChange = onScaleChange,
                        onMarketClick = { market ->
                            // 클릭 시 동작 (추후 상세 페이지로 이동)
                            Toast.makeText(
                                this@MainActivity,
                                "${market.marketName} 선택됨",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    )
                }
            }
        }
    }
}
