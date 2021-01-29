//package io.ysq.widget;
//
//import android.annotation.SuppressLint;
//import android.content.Context;
//import android.content.res.TypedArray;
//import android.util.AttributeSet;
//import android.view.LayoutInflater;
//import android.widget.EditText;
//import android.widget.ImageView;
//import android.widget.LinearLayout;
//
//import androidx.annotation.Nullable;
//
//public class ClearEditText extends LinearLayout {
//    public ClearEditText(Context context) {
//        super(context);
//    }
//
//    public ClearEditText(Context context, @Nullable AttributeSet attrs) {
//        super(context, attrs);
//        init(context, attrs, 0);
//    }
//
//    public ClearEditText(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
//        super(context, attrs, defStyleAttr);
//        init(context, attrs, defStyleAttr);
//    }
//
//    private String text;
//
//    public String getText() {
//        return text;
//    }
//
//    public void setText(String text) {
//        this.text = text;
//    }
//
//    private EditText mEt;
//    private ImageView mClearIv;
//
//    private void init(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
//        LayoutInflater.from(context).inflate(R.layout.small_edittext_clear, this, true);
//        mEt = findViewById(R.id.et);
//        mClearIv = findViewById(R.id.clear_iv);
//        @SuppressLint("Recycle") TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.ClearEditText, defStyleAttr, 0);
//        int count = a.getIndexCount();
//        int index = 0;
//        for (int i = 0;i < count;i++) {
//            index = a.getIndex(i);
//            if (index == R.styleable.ClearEditText_text) {
//                text = a.getString(index);
//            }
//        }
//
//        mEt.setText(text);
//    }
//}
