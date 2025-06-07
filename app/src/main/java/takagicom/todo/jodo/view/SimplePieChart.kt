package takagicom.todo.jodo.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import takagicom.todo.jodo.R
import kotlin.math.min

class SimplePieChart @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()
    private var centerX = 0f
    private var centerY = 0f
    private var radius = 0f
    
    // 数据和颜色
    private val slices = mutableListOf<Slice>()
    
    // 图例相关
    private var showLegend = true
    private val legendPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val legendTextPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val legendMargin = context.resources.getDimensionPixelSize(R.dimen.pie_legend_margin)
    private val legendIndicatorSize = context.resources.getDimensionPixelSize(R.dimen.pie_legend_indicator_size)
    private val legendTextSize = context.resources.getDimensionPixelSize(R.dimen.pie_legend_text_size)
    private val legendPadding = context.resources.getDimensionPixelSize(R.dimen.pie_legend_padding)
    
    init {
        legendTextPaint.textSize = legendTextSize.toFloat()
        legendTextPaint.color = context.getColor(R.color.text_primary)
    }
    
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        updateDimensions(w, h)
    }
    
    private fun updateDimensions(width: Int, height: Int) {
        centerX = width / 2f
        centerY = height / 3f  // 上方1/3位置，留出下方空间显示图例
        radius = min(width, height / 2) / 2f * 0.8f  // 80%的空间用于饼图
        
        rect.set(
            centerX - radius,
            centerY - radius,
            centerX + radius,
            centerY + radius
        )
    }
    
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        // 绘制饼图
        drawPieChart(canvas)
        
        // 绘制图例
        if (showLegend && slices.isNotEmpty()) {
            drawLegend(canvas)
        }
    }
    
    private fun drawPieChart(canvas: Canvas) {
        if (slices.isEmpty()) return
        
        var startAngle = 0f
        
        for (slice in slices) {
            val sweepAngle = slice.percentage * 360f / 100f
            
            paint.color = slice.color
            canvas.drawArc(rect, startAngle, sweepAngle, true, paint)
            
            startAngle += sweepAngle
        }
    }
    
    private fun drawLegend(canvas: Canvas) {
        var legendY = centerY + radius + legendMargin * 2
        
        for (slice in slices) {
            // 绘制颜色指示器
            legendPaint.color = slice.color
            canvas.drawRect(
                centerX - radius,
                legendY,
                centerX - radius + legendIndicatorSize,
                legendY + legendIndicatorSize,
                legendPaint
            )
            
            // 绘制文本
            val legendText = "${slice.label}: ${slice.percentage.toInt()}%"
            canvas.drawText(
                legendText,
                centerX - radius + legendIndicatorSize + legendPadding,
                legendY + legendIndicatorSize / 2 + legendTextPaint.textSize / 3,
                legendTextPaint
            )
            
            legendY += legendIndicatorSize + legendPadding
        }
    }
    
    fun setData(newSlices: List<Slice>) {
        slices.clear()
        slices.addAll(newSlices)
        invalidate()
    }
    
    fun setShowLegend(show: Boolean) {
        this.showLegend = show
        invalidate()
    }
    
    data class Slice(val label: String, val percentage: Float, val color: Int)
}
