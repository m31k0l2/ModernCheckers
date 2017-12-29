package melcor.neurocheckers.objects

import android.graphics.Color
import android.graphics.Paint

object paints {
    val desk = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#fef7da") }
    val field = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#6c3a13") }
    val white = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#fef7da") }
    val black = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#36180c") }
    val green = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#a4a102") }
    val blue = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#a4a102") }
    val yellow = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#ecab7c") }
    val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        strokeWidth = 2f
        style = Paint.Style.STROKE
    }
}