package com.example.testapp

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.testapp.ui.theme.TestAppTheme
import com.example.testapp.R
import android.content.pm.PackageManager

class MainActivity : ComponentActivity() {

    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "悬浮窗权限已授予", Toast.LENGTH_SHORT).show()
            checkAccessibilityService()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TestAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    PermissionRequestScreen()
                }
            }
        }

        // 检查权限
        checkOverlayPermission()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1)
            }
        }
    }

    @Composable
    fun PermissionRequestScreen() {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "需要以下权限才能正常使用：",
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )

            Button(
                onClick = { checkOverlayPermission() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "1. 授予悬浮窗权限",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Button(
                onClick = { checkAccessibilityService() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "2. 开启辅助功能服务",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Text(
                text = """
                    使用说明：
                    1. 请先授予上述权限
                    2. 在任意应用中选择文本
                    3. 点击出现的"朗读"按钮即可朗读文本
                """.trimIndent(),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth()
            )

            Divider(
                modifier = Modifier.padding(vertical = 16.dp),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
            )

            // 测试区域标题
            Text(
                text = "测试区域",
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            // 可选择的测试文本
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                shape = MaterialTheme.shapes.medium
            ) {
                AndroidView(
                    factory = { context ->
                        TextView(context).apply {
                            text = """
                                这是一段测试文本，您可以长按选择这些文字。
                                
                                选择文本后，屏幕上方会出现"朗读"按钮。
                                
                                点击"朗读"按钮后，系统会朗读选中的内容。
                                
                                您也可以尝试选择部分文字来测试朗读功能。
                            """.trimIndent()
                            textSize = 16f
                            setPadding(32, 32, 32, 32)
                            setTextIsSelectable(true)
                            customSelectionActionModeCallback = TextSelectionActionModeCallback(this)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    private fun checkOverlayPermission() {
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "请授予悬浮窗权限", Toast.LENGTH_SHORT).show()
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            overlayPermissionLauncher.launch(intent)
        } else {
            Toast.makeText(this, "已获得悬浮窗权限", Toast.LENGTH_SHORT).show()
            checkAccessibilityService()
        }
    }

    private fun checkAccessibilityService() {
        if (!isAccessibilityServiceEnabled()) {
            Log.d("Accessibility", "Service not enabled, requesting...")
            Toast.makeText(this, "请开启辅助功能服务", Toast.LENGTH_SHORT).show()
            try {
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
            } catch (e: Exception) {
                Log.e("Accessibility", "Error opening accessibility settings", e)
                Toast.makeText(this, "无法打开辅助功能设置", Toast.LENGTH_SHORT).show()
            }
        } else {
            Log.d("Accessibility", "Service is enabled")
            Toast.makeText(this, "辅助功能服务已启用", Toast.LENGTH_SHORT).show()
        }
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        try {
            val serviceName = "$packageName/${AccessibilityTTSService::class.java.canonicalName}"
            val accessibilityEnabled = Settings.Secure.getInt(
                contentResolver,
                Settings.Secure.ACCESSIBILITY_ENABLED
            )
            
            Log.d("Accessibility", "Accessibility enabled: $accessibilityEnabled")
            
            if (accessibilityEnabled == 1) {
                val settingValue = Settings.Secure.getString(
                    contentResolver,
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
                ) ?: return false
                
                Log.d("Accessibility", "Enabled services: $settingValue")
                Log.d("Accessibility", "Looking for service: $serviceName")
                
                return settingValue.split(':').any { 
                    val matches = it.equals(serviceName, ignoreCase = true)
                    Log.d("Accessibility", "Service $it matches: $matches")
                    matches
                }
            }
            return false
        } catch (e: Exception) {
            Log.e("Accessibility", "Error checking service status", e)
            return false
        }
    }
}
