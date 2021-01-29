package io.ysq.widget

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.LinearLayout

@SuppressLint("Recycle")
class ClearEditTextKt @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : LinearLayout(context, attrs, defStyleAttr) {

    private val mEt by lazy {
        findViewById<EditText>(R.id.et)
    }

    private val mClearIv by lazy {
        findViewById<EditText>(R.id.clear_iv)
    }

    var text: String? = ""

    init {
        LayoutInflater.from(context).inflate(R.layout.small_edittext_clear, this, true)
        val a = context.obtainStyledAttributes(attrs, R.styleable.ClearEditTextKt, defStyleAttr, 0)
        val count = a.indexCount
        var index: Int
        for (i in 0..count) {
            index = a.getIndex(i)
            when (index) {
                R.styleable.ClearEditTextKt_text -> {
                    text = a.getString(index)
                }
            }
        }

        mEt.setText(text)
    }

    fun getEditText() = mEt

    fun getClearIv() = mClearIv

}