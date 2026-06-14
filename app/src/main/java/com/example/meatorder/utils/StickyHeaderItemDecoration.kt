package com.example.meatorder.utils

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class StickyHeaderItemDecoration(
    private val getItems: () -> List<Any>,
    private val headerHeight: Int = 120,
    private val backgroundColor: Int = 0xFFF0F0F0.toInt(),
    private val textColor: Int = 0xFF333333.toInt(),
    private val getTextSize: () -> Float,
    private val textSizeOffset: Float = 40f  // Добавка к базовому размеру
) : RecyclerView.ItemDecoration() {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = backgroundColor
        style = Paint.Style.FILL
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = textColor
        isFakeBoldText = true
    }

    override fun onDrawOver(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        super.onDrawOver(c, parent, state)

        val items = getItems()
        if (items.isEmpty()) return

        // Применяем размер шрифта с добавкой
        textPaint.textSize = getTextSize() + textSizeOffset

        val layoutManager = parent.layoutManager as? LinearLayoutManager ?: return
        val firstVisiblePos = layoutManager.findFirstVisibleItemPosition()
        if (firstVisiblePos == RecyclerView.NO_POSITION) return

        var headerPos = firstVisiblePos
        while (headerPos >= 0 && items[headerPos] !is String) {
            headerPos--
        }
        if (headerPos < 0) return

        val title = items[headerPos] as String

        var offsetY = 0
        val nextHeaderPos = findNextHeader(items, headerPos)
        if (nextHeaderPos != -1) {
            val nextHeaderView = layoutManager.findViewByPosition(nextHeaderPos)
            if (nextHeaderView != null) {
                val top = nextHeaderView.top
                if (top < headerHeight) {
                    offsetY = top - headerHeight
                }
            }
        }

        c.drawRect(0f, offsetY.toFloat(), parent.width.toFloat(), (offsetY + headerHeight).toFloat(), paint)

        val textX = 32f
        val textY = offsetY + headerHeight / 2f - (textPaint.descent() + textPaint.ascent()) / 2f
        c.drawText(title, textX, textY, textPaint)
    }

    private fun findNextHeader(items: List<Any>, currentPos: Int): Int {
        for (i in currentPos + 1 until items.size) {
            if (items[i] is String) return i
        }
        return -1
    }

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        // без изменений
    }
}
