package com.example.testapp

import android.speech.tts.TextToSpeech
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import java.util.*

class TextSelectionActionModeCallback(private val textView: TextView) : ActionMode.Callback {
    private var tts: TextToSpeech? = null

    init {
        tts = TextToSpeech(textView.context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.CHINESE
            }
        }
    }

    override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
        menu?.add(Menu.NONE, 1, Menu.FIRST, textView.context.getString(R.string.speak))
        return true
    }

    override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean = false

    override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
        if (item?.itemId == 1) {
            val start = textView.selectionStart
            val end = textView.selectionEnd
            val selectedText = textView.text.substring(start, end)
            tts?.speak(selectedText, TextToSpeech.QUEUE_FLUSH, null, null)
            mode?.finish()
            return true
        }
        return false
    }

    override fun onDestroyActionMode(mode: ActionMode?) {
        tts?.stop()
        tts?.shutdown()
    }
} 