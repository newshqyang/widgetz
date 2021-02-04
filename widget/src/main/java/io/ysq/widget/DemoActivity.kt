package io.ysq.widget

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

class DemoActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.demo_activity)

        ClearEditText(this)
        SwipeMenuLayout(this)
    }
}