package com.dsk.myexpense.expense_module.util

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

class LineChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paintLine = Paint().apply {
        color = Color.parseColor("#3CB371") // Line color
        strokeWidth = 8f
        style = Paint.Style.STROKE
        isAntiAlias = true
    }

    private val paintFill = Paint().apply {
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val paintPoint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val paintPointOutline = Paint().apply {
        color = Color.parseColor("#3CB371")
        style = Paint.Style.STROKE
        strokeWidth = 4f
        isAntiAlias = true
    }

    private val paintGrid = Paint().apply {
        color = Color.LTGRAY
        strokeWidth = 2f
        style = Paint.Style.STROKE
        isAntiAlias = true
    }

    private val paintText = Paint().apply {
        color = Color.GRAY
        textSize = 32f
        isAntiAlias = true
    }

    private val tooltipPaint = Paint().apply {
        color = Color.BLACK
        textSize = 40f
        isAntiAlias = true
    }

    private var dataPoints: List<Pair<String, Int>> = emptyList()
    private var selectedIndex = -1

    fun setData(newDataPoints: List<Pair<String, Int>>) {
        dataPoints = newDataPoints
        invalidate() // Redraw the chart
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (dataPoints.isEmpty()) return

        val padding = 100f
        val chartWidth = width - 2 * padding
        val chartHeight = height - 2 * padding
        val stepX = chartWidth / (dataPoints.size - 1).coerceAtLeast(1)
        val maxValue = dataPoints.maxOfOrNull { it.second } ?: 1

        drawGrid(canvas, padding, chartWidth, chartHeight)

        val path = Path()
        val fillPath = Path()

        for (i in dataPoints.indices) {
            val x = padding + i * stepX
            val y = height - padding - (dataPoints[i].second / maxValue.toFloat() * chartHeight)

            if (i == 0) {
                path.moveTo(x, y)
                fillPath.moveTo(x, y)
            } else {
                val prevX = padding + (i - 1) * stepX
                val prevY =
                    height - padding - (dataPoints[i - 1].second / maxValue.toFloat() * chartHeight)
                val midX = (prevX + x) / 2

                // Use cubic Bézier curves for smooth transitions
                path.cubicTo(midX, prevY, midX, y, x, y)
                fillPath.cubicTo(midX, prevY, midX, y, x, y)
            }

            if (i == dataPoints.size - 1) {
                fillPath.lineTo(x, height - padding)
                fillPath.lineTo(padding, height - padding)
                fillPath.close()
            }
        }

        // Draw gradient below the line
        val gradient = LinearGradient(
            0f, padding, 0f, height.toFloat(),
            Color.parseColor("#80C9E3"), Color.TRANSPARENT,
            Shader.TileMode.CLAMP
        )
        paintFill.shader = gradient
        canvas.drawPath(fillPath, paintFill)

        // Draw smooth line
        canvas.drawPath(path, paintLine)

        // Draw points and tooltips
        dataPoints.forEachIndexed { index, (label, value) ->
            val x = padding + index * stepX
            val y = height - padding - (value / maxValue.toFloat() * chartHeight)

            // Draw data points
            canvas.drawCircle(x, y, 10f, paintPoint)
            canvas.drawCircle(x, y, 10f, paintPointOutline)

            // Auto-trim X-axis labels
            val showLabel = dataPoints.size < 8 || index % (dataPoints.size / 7) == 0
            if (showLabel) {
                canvas.drawText(label, x - 30f, height - 20f, paintText)
            }

            // Highlight tooltip for selected point
            if (selectedIndex == index) {
                // Tooltip background
                val tooltipText = "${value}"
                val textWidth = tooltipPaint.measureText(tooltipText)
                val rect = RectF(x - textWidth / 2 - 10, y - 90, x + textWidth / 2 + 10, y - 50)
                val tooltipBackground = Paint().apply {
                    color = Color.WHITE
                    style = Paint.Style.FILL
                    isAntiAlias = true
                }
                val tooltipOutline = Paint().apply {
                    color = Color.BLACK
                    style = Paint.Style.STROKE
                    strokeWidth = 2f
                    isAntiAlias = true
                }
                canvas.drawRoundRect(rect, 10f, 10f, tooltipBackground)
                canvas.drawRoundRect(rect, 10f, 10f, tooltipOutline)

                // Tooltip text
                canvas.drawText(tooltipText, x - textWidth / 2, y - 60, tooltipPaint)
            }
        }
    }

    private fun drawGrid(canvas: Canvas, padding: Float, chartWidth: Float, chartHeight: Float) {
        val gridCount = 5
        val stepY = chartHeight / gridCount

        for (i in 0..gridCount) {
            val y = height - padding - i * stepY
            canvas.drawLine(padding, y, width - padding, y, paintGrid)
            val label = (i * (dataPoints.maxOfOrNull { it.second } ?: 1) / gridCount).toString()
            canvas.drawText(label, 10f, y, paintText)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (dataPoints.isEmpty()) return super.onTouchEvent(event)

        val padding = 100f
        val chartWidth = width - 2 * padding
        val stepX = chartWidth / (dataPoints.size - 1).coerceAtLeast(1)

        when (event.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                val touchX = event.x
                selectedIndex = ((touchX - padding) / stepX).toInt().coerceIn(0, dataPoints.size - 1)
                invalidate()
            }
            MotionEvent.ACTION_UP -> {
                selectedIndex = -1
                invalidate()
            }
        }
        return true
    }
}
