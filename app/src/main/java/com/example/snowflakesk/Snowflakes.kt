package com.example.snowflakesk

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.AsyncTask
import android.util.Log
import android.view.MotionEvent
import android.view.View
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

data class Snowflake(
    var x: Float,
    var y: Float,
    val velocity: Float,
    val radius: Float,
    val color: Int,
    var baseX: Float,
    val amplitude: Float,  
    val phaseSpeed: Float,
    var phase: Float
)

@Suppress("DEPRECATION")
open class Snowflakes(ctx: Context) : View(ctx) {

    companion object {
        const val USE_ASYNC = true
        const val COUNT = 40
        const val DELAY_MS = 30
    }

    @Volatile
    private var snow: Array<Snowflake> = emptyArray()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var h = 1000
    private var w = 1000
    private var moveTask: MoveTask? = null

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(Color.BLUE)

        for (s in snow) {
            paint.color = s.color
            canvas.drawCircle(s.x, s.y, s.radius, paint)
        }
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        val newH = bottom - top
        val newW = right - left
        if (snow.isNotEmpty() && newH == h && newW == w) return
        h = newH; w = newW

        snow = Array(COUNT) { createSnowflake() }
        Log.d("mytag", "snow: " + snow.contentToString())
    }

    private fun createSnowflake(): Snowflake {
        val baseX = Random.nextFloat() * w
        val shade = 170 + Random.nextInt(86)
        val color = Color.rgb(shade, shade, 255)
        return Snowflake(
            x = baseX,
            y = Random.nextFloat() * h,
            velocity = 10 + 8 * Random.nextFloat(),
            radius = 8 + 12 * Random.nextFloat(),
            color = color,
            baseX = baseX,
            amplitude = 20 + 40 * Random.nextFloat(),
            phaseSpeed = 0.03f + 0.05f * Random.nextFloat(),
            phase = Random.nextFloat() * 2 * PI.toFloat()
        )
    }

    private fun respawn(s: Snowflake) {
        s.y = 0f
        s.baseX = Random.nextFloat() * w
        s.phase = Random.nextFloat() * 2 * PI.toFloat()
    }

    fun moveSnowflakes() {
        for (s in snow) {
            val k = 1f - s.y / h
            s.y += s.velocity * k

            s.phase += s.phaseSpeed
            s.x = s.baseX + s.amplitude * sin(s.phase)

            if (k < 0.05f) respawn(s)
        }
        postInvalidate()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (USE_ASYNC && moveTask == null) {
            moveTask = MoveTask(this).also { it.execute(DELAY_MS) }
        }
    }

    override fun onDetachedFromWindow() {
        moveTask?.cancel(true)
        moveTask = null
        super.onDetachedFromWindow()
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (!USE_ASYNC && event?.action == MotionEvent.ACTION_DOWN) {
            moveSnowflakes()
        }
        return false
    }

    class MoveTask(private val s: Snowflakes) : AsyncTask<Int, Unit, Unit>() {
        override fun doInBackground(vararg params: Int?) {
            val delay = (params[0] ?: 200).toLong()
            while (!isCancelled) {
                try {
                    Thread.sleep(delay)
                } catch (e: InterruptedException) {
                    break
                }
                s.moveSnowflakes()
            }
        }
    }
}
