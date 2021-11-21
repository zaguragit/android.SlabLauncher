package io.posidon.android.slablauncher.util.view.recycler

import android.annotation.SuppressLint
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.ViewConfiguration
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.abs

object RecyclerViewLongPressHelper {

    @SuppressLint("ClickableViewAccessibility")
    fun setOnLongPressListener(recyclerView: RecyclerView, listener: (recyclerView: RecyclerView, x: Float, y: Float) -> Unit) {
        var popupX = 0f
        var popupY = 0f
        val onLongPress = Runnable {
            recyclerView.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            listener(recyclerView, popupX, popupY)
        }
        var lastRecyclerViewDownTouchEvent: MotionEvent? = null
        recyclerView.setOnTouchListener { v, event ->
            when (event.action and MotionEvent.ACTION_MASK) {
                MotionEvent.ACTION_DOWN -> {
                    popupX = event.rawX
                    popupY = event.rawY
                    if (recyclerView.findChildViewUnder(event.x, event.y) == null) {
                        v.handler.removeCallbacks(onLongPress)
                        lastRecyclerViewDownTouchEvent = event
                        v.handler.postDelayed(onLongPress, ViewConfiguration.getLongPressTimeout().toLong())
                    }
                }
                MotionEvent.ACTION_MOVE -> if (lastRecyclerViewDownTouchEvent != null) {
                    val xDelta = abs(popupX - event.rawX)
                    val yDelta = abs(popupY - event.rawY)
                    if (xDelta >= 10 || yDelta >= 10) {
                        v.handler.removeCallbacks(onLongPress)
                        lastRecyclerViewDownTouchEvent = null
                    }
                }
                MotionEvent.ACTION_CANCEL,
                MotionEvent.ACTION_UP -> {
                    v.handler.removeCallbacks(onLongPress)
                    lastRecyclerViewDownTouchEvent = null
                }
            }
            false
        }
    }
}