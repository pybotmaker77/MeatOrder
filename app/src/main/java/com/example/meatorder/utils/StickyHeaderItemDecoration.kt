package com.example.meatorder.utils

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class StickyHeaderItemDecoration(
    private val headerHeight: Int = 120,  // подгоните под ваш дизайн
    private val backgroundColor: Int = 0xFFF0F0F0.toInt(),
    private val textColor: Int = 0xFF333333.toInt(),
    private val textSize: Float = 16f
) : RecyclerView.ItemDecoration() {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = backgroundColor
        style = Paint.Style.FILL
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = textColor
        textSize = this@StickyHeaderItemDecoration.textSize
        isFakeBoldText = true
    }

    override fun onDrawOver(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        super.onDrawOver(c, parent, state)

        val adapter = parent.adapter ?: return
        val layoutManager = parent.layoutManager as? LinearLayoutManager ?: return
        val firstVisiblePos = layoutManager.findFirstVisibleItemPosition()
        if (firstVisiblePos == RecyclerView.NO_POSITION) return

        // Найти ближайший заголовок (с типом 0)
        var headerPos = firstVisiblePos
        while (headerPos >= 0 && adapter.getItemViewType(headerPos) != 0) {
            headerPos--
        }
        if (headerPos < 0) return

        val headerItem = adapter.getItem(headerPos) ?: return
        if (headerItem !is String) return
        val title = headerItem

        // Вычислить вертикальное смещение заголовка, чтобы он "выталкивался" следующим заголовком
        var offsetY = 0
        val nextHeaderPos = findNextHeader(adapter, headerPos)
        if (nextHeaderPos != -1) {
            val nextHeaderView = layoutManager.findViewByPosition(nextHeaderPos)
            if (nextHeaderView != null) {
                val top = nextHeaderView.top
                if (top < headerHeight) {
                    offsetY = top - headerHeight
                }
            }
        }

        // Нарисовать фон заголовка
        c.drawRect(0f, offsetY.toFloat(), parent.width.toFloat(), (offsetY + headerHeight).toFloat(), paint)

        // Нарисовать текст заголовка
        val textX = 32f  // отступ слева
        val textY = offsetY + headerHeight / 2f - (textPaint.descent() + textPaint.ascent()) / 2f
        c.drawText(title, textX, textY, textPaint)
    }

    private fun findNextHeader(adapter: RecyclerView.Adapter<*>, currentPos: Int): Int {
        for (i in currentPos + 1 until adapter.itemCount) {
            if (adapter.getItemViewType(i) == 0) return i
        }
        return -1
    }

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        // Никаких дополнительных отступов не нужно, оставляем как есть
    }
}
