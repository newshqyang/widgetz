package io.ysq.widget.small

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.LinearLayout
import io.ysq.widget.R

class ClearEditText @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : LinearLayout(context, attrs, defStyleAttr) {

    private val mEt by lazy {
        findViewById<EditText>(R.id.et)
    }

    private val mClearIv by lazy {
        findViewById<EditText>(R.id.clear_iv)
    }

    init {
        LayoutInflater.from(context).inflate(R.layout.small_edittext_clear, this, true)
    }

    fun getEditText() = mEt

    fun getClearIv() = mClearIv

}