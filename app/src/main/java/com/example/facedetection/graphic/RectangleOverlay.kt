package com.example.facedetection.graphic

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import com.example.facedetection.utils.CameraUtils
import com.google.mlkit.vision.face.Face

class RectangleOverlay(
    private val graphicOverlay: GraphicOverlay<*>,
    private val face: Face,
    private val rect: Rect
) : GraphicOverlay.Graphic(graphicOverlay) {

    private val boxPaint: Paint = Paint()

    init {
        boxPaint.color = Color.GREEN
        boxPaint.style = Paint.Style.STROKE
        boxPaint.strokeWidth = 3.0f
    }

    override fun draw(canvas: Canvas) {
        val rect = CameraUtils.calculateRect(
            graphicOverlay,
            rect.height().toFloat(),
            rect.width().toFloat(),
            face.boundingBox
        )
        canvas.drawRect(rect, boxPaint)
    }

}