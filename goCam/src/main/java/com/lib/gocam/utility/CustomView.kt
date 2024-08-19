package com.lib.gocam.utility

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.ImageView
import androidx.core.widget.ImageViewCompat

class CustomView : androidx.appcompat.widget.AppCompatImageView {
    private var paint: Paint? = null
    private lateinit var mBitmap: Bitmap
    private var mCanvas: Canvas? = null
    private var mPath: Path? = null
    //var context: Context? = null
    var left = 0f
    var top = 0f
    var right = 0f
    var bottom = 0f
    private var mX = 0f
    private var mY = 0f
    private var newX = 0f
    private var newY = 0f



    constructor(context: Context) : super(context) {

        init()
    }

    fun init() {
        paint = Paint()
        paint!!.isAntiAlias = true
        paint!!.isDither = true
        paint!!.color = Color.RED
        paint!!.strokeJoin = Paint.Join.ROUND
        paint!!.strokeWidth = 2f
        mPath = Path()
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {

        init()
    }

    fun setBitmap(mBitmap: Bitmap) {
        this.mBitmap = mBitmap
    }


    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        mCanvas = canvas
        mBitmap = Bitmap.createScaledBitmap(mBitmap!!, canvas.width, canvas.height, false)
        canvas.drawBitmap(mBitmap, 0f, 0f, null)
        //        canvas.drawPath(mPath, paint);
        if (mX > newX) {
            left = newX
            right = mX
        } else {
            left = mX
            right = newX
        }
        if (mY > newY) {
            top = newY
            bottom = mY
        } else {
            top = mY
            bottom = newY
        }
        //        RectF myRectum = new RectF(mX, mY, newX, newY);
        val myRectum = RectF(left, top, right, bottom)
        canvas.drawRect(myRectum, paint!!)
    }

    private fun touch_start(x: Float, y: Float) {
        paint!!.style = Paint.Style.STROKE
        mPath!!.reset()
        mPath!!.moveTo(x, y)
        mX = x
        mY = y
    }

    private fun touch_move(x: Float, y: Float) {
        var y = y
        val slope = slope(mX, mY, x, y)
        if (slope == 0f) {
            val dx = Math.abs(x - mX)
            val dy = Math.abs(y - mY)
            if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
                mPath!!.quadTo(mX, mY, (x + mX) / 2, (y + mY) / 2)
                mX = x
                mY = y
            }
        } else if (slope < 0.3 && slope > -0.3) {
            y = mY
            val dx = Math.abs(x - mX)
            val dy = Math.abs(y - mY)
            if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
                mPath!!.quadTo(mX, mY, (x + mX) / 2, (y + mY) / 2)
                mX = x
                mY = y
            }
        }
    }

    private fun touch_up() {
        paint!!.style = Paint.Style.FILL
        //        mPath.lineTo(mX, mY);
//        // commit the path to our offscreen
//        mCanvas.drawPath(mPath, paint);
//        // kill this so we don't double draw
//        mPath.reset();
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        //        bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
//        mCanvas = new Canvas(bitmap);
    }

    fun getBitmap(): Bitmap {
        val b = Bitmap.createBitmap(mBitmap!!.width, mBitmap!!.height, Bitmap.Config.RGB_565)
        val c = Canvas(b)
        layout(0, 0, this.layoutParams.width, this.layoutParams.height)
        draw(c)
        return b
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                touch_start(x, y)
                invalidate()
            }
            MotionEvent.ACTION_MOVE -> {
                newX = x
                newY = y
                //                touch_move(x, y);
                invalidate()
            }
            MotionEvent.ACTION_UP -> {
                touch_up()
                invalidate()
            }
        }
        return true
    }

    companion object {
        private const val TOUCH_TOLERANCE = 4f
        fun slope(
            x1: Float, y1: Float,
            x2: Float, y2: Float
        ): Float {
            return (y2 - y1) / (x2 - x1)
        }
    }
}
