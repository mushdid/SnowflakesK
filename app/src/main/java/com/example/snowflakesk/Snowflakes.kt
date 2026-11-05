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

/**
 * Снежинка.
 * x вычисляется из baseX + amplitude * sin(phase) — это и есть блуждание по горизонтали.
 */
data class Snowflake(
    var x: Float,
    var y: Float,
    val velocity: Float,     // максимальная скорость (в верхней части экрана)
    val radius: Float,
    val color: Int,          // оттенок, создаётся через Color.rgb()
    var baseX: Float,        // "ось", вокруг которой качается снежинка
    val amplitude: Float,    // размах качания по горизонтали
    val phaseSpeed: Float,   // как быстро меняется фаза синуса
    var phase: Float
)

@Suppress("DEPRECATION") // AsyncTask устарел с API 30, но в задании требуется именно он
open class Snowflakes(ctx: Context) : View(ctx) {

    companion object {
        /** false — часть 1 (шаг по касанию), true — часть 2 (фоновый поток через AsyncTask) */
        const val USE_ASYNC = true
        const val COUNT = 40
        const val DELAY_MS = 30
    }

    // @Volatile: массив подменяется в UI-потоке (onLayout), а читается ещё и из фонового
    @Volatile
    private var snow: Array<Snowflake> = emptyArray()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var h = 1000
    private var w = 1000 // значения по умолчанию, будут заменены в onLayout()
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
        if (snow.isNotEmpty() && newH == h && newW == w) return // размеры не менялись
        h = newH; w = newW

        // случайные координаты, радиусы, скорости и оттенки
        snow = Array(COUNT) { createSnowflake() }
        Log.d("mytag", "snow: " + snow.contentToString())
    }

    private fun createSnowflake(): Snowflake {
        val baseX = Random.nextFloat() * w
        // оттенок: от бело-голубого до почти белого. Color.rgb(int, int, int), канал 0..255
        val shade = 170 + Random.nextInt(86)          // 170..255
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

    /** Снежинка долетела до низа — возвращаем её наверх с новой позицией. */
    private fun respawn(s: Snowflake) {
        s.y = 0f
        s.baseX = Random.nextFloat() * w
        s.phase = Random.nextFloat() * 2 * PI.toFloat()
    }

    fun moveSnowflakes() {
        for (s in snow) {
            // k = 1 вверху экрана и плавно уменьшается до 0 внизу → падение с замедлением
            val k = 1f - s.y / h
            s.y += s.velocity * k

            // блуждание по горизонтали: синус от меняющейся фазы
            s.phase += s.phaseSpeed
            s.x = s.baseX + s.amplitude * sin(s.phase)

            // скорость почти дошла до нуля — снежинка "приземлилась", запускаем новую сверху
            if (k < 0.05f) respawn(s)
        }
        // из фонового потока нельзя вызывать invalidate(), только postInvalidate()
        postInvalidate()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (USE_ASYNC && moveTask == null) {
            moveTask = MoveTask(this).also { it.execute(DELAY_MS) }
        }
    }

    override fun onDetachedFromWindow() {
        // останавливаем поток, иначе он продолжит работать после закрытия экрана
        moveTask?.cancel(true)
        moveTask = null
        super.onDetachedFromWindow()
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        // Часть 1: без фонового потока снежинки двигаются шагом по каждому касанию
        if (!USE_ASYNC && event?.action == MotionEvent.ACTION_DOWN) {
            moveSnowflakes()
        }
        return false // защита от множественных срабатываний
    }

    class MoveTask(private val s: Snowflakes) : AsyncTask<Int, Unit, Unit>() {
        // https://developer.alexanderklimov.ru/android/theory/asynctask.php
        override fun doInBackground(vararg params: Int?) {
            val delay = (params[0] ?: 200).toLong() // если задержка не задана
            while (!isCancelled) {
                try {
                    Thread.sleep(delay)
                } catch (e: InterruptedException) {
                    break // cancel(true) прервал сон
                }
                s.moveSnowflakes()
            }
        }
    }
}