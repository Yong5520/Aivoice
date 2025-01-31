package com.example.testapp

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.graphics.PixelFormat
import android.graphics.Rect
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.*
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Button
import android.widget.Toast
import java.util.*

class AccessibilityTTSService : AccessibilityService() {
    private var tts: TextToSpeech? = null
    private var windowManager: WindowManager? = null
    private var floatingButton: Button? = null
    private var lastSelectedText: String = ""

    override fun onCreate() {
        super.onCreate()
        Log.d("AccessibilityService", "onCreate")
        try {
            initTTS()
            windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        } catch (e: Exception) {
            Log.e("AccessibilityService", "Error in onCreate", e)
        }
    }

    private fun initTTS() {
        try {
            if (tts == null) {
                tts = TextToSpeech(this) { status ->
                    if (status == TextToSpeech.SUCCESS) {
                        val result = tts?.setLanguage(Locale.CHINESE)
                        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                            Log.e("TTS", "Language not supported")
                        } else {
                            tts?.setSpeechRate(1.0f)
                            tts?.setPitch(1.0f)
                            Log.d("TTS", "TTS initialized successfully")
                        }
                    } else {
                        Log.e("TTS", "TTS initialization failed")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("TTS", "Error initializing TTS", e)
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d("AccessibilityService", "onServiceConnected")
        try {
            val info = AccessibilityServiceInfo()
            info.apply {
                eventTypes = AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED
                feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
                flags = AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS or
                        AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
                notificationTimeout = 100
            }
            serviceInfo = info
            Log.d("AccessibilityService", "Service info set successfully")
        } catch (e: Exception) {
            Log.e("AccessibilityService", "Error in onServiceConnected", e)
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        try {
            event?.let { evt ->
                when (evt.eventType) {
                    AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED,
                    AccessibilityEvent.TYPE_VIEW_FOCUSED,
                    AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                        handleTextSelection(evt)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("AccessibilityService", "Error in onAccessibilityEvent", e)
        }
    }

    private fun handleTextSelection(event: AccessibilityEvent) {
        try {
            val source = event.source
            if (source != null) {
                try {
                    val start = source.textSelectionStart
                    val end = source.textSelectionEnd
                    if (start >= 0 && end >= 0 && start != end) {
                        val text = source.text?.toString() ?: ""
                        if (text.isNotEmpty() && start < text.length && end <= text.length) {
                            lastSelectedText = text.substring(start, end)
                            val bounds = Rect()
                            source.getBoundsInScreen(bounds)
                            showFloatingButton(bounds)
                            Log.d("AccessibilityService", "Selected text from range: $lastSelectedText")
                            return
                        }
                    }

                    if (source.isSelected) {
                        val text = source.text?.toString() ?: ""
                        if (text.isNotEmpty()) {
                            lastSelectedText = text
                            val bounds = Rect()
                            source.getBoundsInScreen(bounds)
                            showFloatingButton(bounds)
                            Log.d("AccessibilityService", "Selected text from selection: $lastSelectedText")
                            return
                        }
                    }

                    findSelectedTextInNode(source)
                } finally {
                    source.recycle()
                }
            }

            if (lastSelectedText.isEmpty()) {
                val rootNode = rootInActiveWindow
                if (rootNode != null) {
                    try {
                        findSelectedTextInNode(rootNode)
                    } finally {
                        rootNode.recycle()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("AccessibilityService", "Error handling text selection", e)
        }
    }

    private fun findSelectedTextInNode(node: AccessibilityNodeInfo) {
        try {
            if (node.isSelected || node.isTextSelectable) {
                val text = node.text?.toString() ?: ""
                if (text.isNotEmpty()) {
                    lastSelectedText = text
                    val bounds = Rect()
                    node.getBoundsInScreen(bounds)
                    showFloatingButton(bounds)
                    Log.d("AccessibilityService", "Found selected text in node: $lastSelectedText")
                    return
                }
            }

            for (i in 0 until node.childCount) {
                node.getChild(i)?.let { child ->
                    try {
                        findSelectedTextInNode(child)
                    } finally {
                        child.recycle()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("AccessibilityService", "Error finding selected text in node", e)
        }
    }

    private fun showFloatingButton(textBounds: Rect) {
        try {
            removeFloatingButton()

            floatingButton = Button(this).apply {
                text = "朗读"
                textSize = 14f
                minimumWidth = 0
                minimumHeight = 0
                setPadding(20, 10, 20, 10)
                setBackgroundResource(android.R.drawable.btn_default)
                setOnClickListener {
                    speakText(lastSelectedText)
                }
            }

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = textBounds.left
                y = textBounds.bottom + 10
            }

            windowManager?.addView(floatingButton, params)
            Log.d("AccessibilityService", "Floating button shown")
        } catch (e: Exception) {
            Log.e("AccessibilityService", "Error showing floating button", e)
        }
    }

    private fun speakText(text: String) {
        try {
            if (text.isNotEmpty()) {
                tts?.let { t ->
                    t.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
                    Log.d("AccessibilityService", "Speaking text: $text")
                } ?: run {
                    Log.d("AccessibilityService", "TTS not initialized, reinitializing...")
                    initTTS()
                }
            }
            removeFloatingButton()
        } catch (e: Exception) {
            Log.e("AccessibilityService", "Error in speakText", e)
        }
    }

    private fun removeFloatingButton() {
        try {
            floatingButton?.let {
                windowManager?.removeView(it)
                floatingButton = null
            }
        } catch (e: Exception) {
            Log.e("AccessibilityService", "Error removing floating button", e)
        }
    }

    override fun onInterrupt() {
        Log.d("AccessibilityService", "onInterrupt")
        try {
            tts?.stop()
            removeFloatingButton()
        } catch (e: Exception) {
            Log.e("AccessibilityService", "Error in onInterrupt", e)
        }
    }

    override fun onDestroy() {
        Log.d("AccessibilityService", "onDestroy")
        try {
            tts?.stop()
            tts?.shutdown()
            removeFloatingButton()
            super.onDestroy()
        } catch (e: Exception) {
            Log.e("AccessibilityService", "Error in onDestroy", e)
        }
    }
} 