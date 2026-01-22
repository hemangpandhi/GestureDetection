package com.example.gesturecontrol.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import java.util.LinkedList

class LineGraphView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val dataPoints = LinkedList<Float>()
    private val maxPoints = 200 // Number of points visible
    private val paint = Paint().apply {
        color = Color.parseColor("#00FF99") // Neon Green
        strokeWidth = 5f
        style = Paint.Style.STROKE
        isAntiAlias = true
    }
    private val path = Path()

    fun addPoint(value: Float) {
        dataPoints.add(value)
        if (dataPoints.size > maxPoints) {
            dataPoints.removeFirst()
        }
        invalidate() // Trigger redraw
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (dataPoints.isEmpty()) return

        val width = width.toFloat()
        val height = height.toFloat()
        
        // Dynamic Scaling
        val maxVal = dataPoints.maxOrNull() ?: 255f
        val minVal = dataPoints.minOrNull() ?: 0f
        val range = (maxVal - minVal).coerceAtLeast(10f) // Avoid divide by zero
        
        val stepX = width / (maxPoints - 1)

        path.reset()
        
        dataPoints.forEachIndexed { index, value ->
            // Normalize Y: Flip coordinate system (0 is top)
            // value - minVal -> 0 to range
            // / range -> 0 to 1
            // * height -> 0 to height (from bottom)
            // height - (...) -> actual y
            
            val normalizedY = ((value - minVal) / range) * (height * 0.8f) // Use 80% height
            val x = index * stepX
            val y = height - (normalizedY + (height * 0.1f)) // Center vertically

            if (index == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }

        canvas.drawPath(path, paint)
    }
}
