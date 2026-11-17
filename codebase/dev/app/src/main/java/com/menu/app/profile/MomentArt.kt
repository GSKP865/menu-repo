package com.menu.app.profile

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.View

/** Small bundled illustrations: no remote image or decoding dependency. */
class MomentArt(context: Context, private val variant: Int) : View(context) {
    private val brush = Paint(Paint.ANTI_ALIAS_FLAG)
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.save()
        canvas.scale(width / 160f, height / 100f)
        fun rect(x: Float, y: Float, w: Float, h: Float, color: String) {
            brush.color = Color.parseColor(color)
            canvas.drawRoundRect(x,y,x+w,y+h,6f,6f,brush)
        }
        brush.color = Color.parseColor("#F8F4EB")
        canvas.drawCircle(113f,25f,19f,brush)
        if (variant % 2 == 0) {
            rect(20f,12f,48f,58f,"#FBF7F0")
            rect(25f,17f,38f,43f,"#B4CCC9")
            rect(42f,17f,3f,43f,"#FBF7F0")
            rect(25f,37f,38f,3f,"#FBF7F0")
            rect(35f,70f,98f,8f,"#987C6A")
            rect(40f,77f,5f,23f,"#987C6A")
            rect(120f,77f,5f,23f,"#987C6A")
            rect(88f,54f,20f,17f,"#FEF8EF")
            brush.color=Color.parseColor("#6B937C")
            canvas.drawOval(112f,32f,127f,60f,brush)
            canvas.drawOval(126f,25f,141f,54f,brush)
            rect(120f,54f,19f,20f,"#CA9E88")
        } else {
            brush.color=Color.parseColor("#ADC3BB")
            val hills=android.graphics.Path().apply {
                moveTo(0f,75f); cubicTo(40f,5f,60f,80f,95f,53f)
                cubicTo(125f,32f,150f,54f,160f,62f); lineTo(160f,100f); lineTo(0f,100f); close()
            }
            canvas.drawPath(hills,brush)
            brush.color=Color.parseColor("#6F9293")
            canvas.drawOval(-20f,77f,190f,145f,brush)
        }
        canvas.restore()
    }
}
