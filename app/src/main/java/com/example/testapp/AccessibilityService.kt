package com.example.testapp

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent

class AccessibilityService : AccessibilityService() {
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // 处理辅助功能事件
    }

    override fun onInterrupt() {
        // 服务中断时的处理
    }
} 