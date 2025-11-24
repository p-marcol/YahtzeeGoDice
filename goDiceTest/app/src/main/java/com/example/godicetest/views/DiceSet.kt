package com.example.godicetest.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import com.example.godicetest.R

class DiceSet @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : LinearLayout(context, attrs, defStyle) {

    private val titleView: TextView

    init {
        LayoutInflater.from(context).inflate(R.layout.dice_set, this, true)
        titleView = findViewById(R.id.title)

        context.theme.obtainStyledAttributes(attrs, R.styleable.TitleBox, 0, 0)
            .apply {
                try {
                    val text = getString(R.styleable.TitleBox_title)
                    if (text != null) titleView.text = text
                } finally {
                    recycle()
                }
            }
    }

    fun setTitle(text: String) {
        titleView.text = text
    }
}