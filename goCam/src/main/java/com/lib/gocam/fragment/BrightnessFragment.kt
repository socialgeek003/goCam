package com.lib.gocam.fragment

import android.content.Context
import android.graphics.*
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.TextView
import androidx.appcompat.widget.AppCompatSeekBar
import androidx.fragment.app.Fragment
import com.lib.gocam.R
import com.lib.gocam.activity.ScanActivity
import com.lib.gocam.databinding.FragmentBrightnessBinding
import com.lib.gocam.utility.Utils

class BrightnessFragment : Fragment() {
    
    lateinit var scanActivity: ScanActivity
    lateinit var brightnessBinding: FragmentBrightnessBinding
    
    var edited: Bitmap? = null

    private var original: Bitmap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        scanActivity = context as ScanActivity
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        brightnessBinding = FragmentBrightnessBinding.inflate(inflater, container, false)
        return brightnessBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        init()
    }

    private fun init() {
        brightnessBinding.sbBrightness.max = 200
        brightnessBinding.sbBrightness.progress = 100
        brightnessBinding.closeBtn.setOnClickListener(View.OnClickListener { scanActivity.onBackPressed() })
        brightnessBinding.sbBrightness.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                Thread(Runnable {
                    Utils.printMessage("ProgressBrightness$progress")
                    activity?.runOnUiThread(Runnable {
                        val scaledBitmap =
                            scaledBitmap(original, brightnessBinding.sourceFrame.width, brightnessBinding.sourceFrame.height)
                        edited = enhanceImage(scaledBitmap, 1f, (progress - 100).toFloat())
                        brightnessBinding.scannedImage.setImageBitmap(edited)
                    })
                }).start()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })
        brightnessBinding.sourceFrame.post(Runnable {
            var bitmap = scanActivity.getTransformed()
            if (bitmap == null) {
                bitmap = scanActivity.getScanImage()
            }
            original = bitmap
            if (original != null) {
                setBitmap(original)
            }
        })
        brightnessBinding.brightDone.setOnClickListener(View.OnClickListener {
            if (edited != null) {
                scanActivity.onBrightnessFinish(edited)
            } else {
                Utils.printMessage("Bitmap Brightness is not done")
            }
        })
    }

    private fun setBitmap(original: Bitmap?) {
        val scaledBitmap = scaledBitmap(original, brightnessBinding.sourceFrame!!.width, brightnessBinding.sourceFrame!!.height)
        brightnessBinding.scannedImage!!.setImageBitmap(scaledBitmap)
    }

    private fun scaledBitmap(bitmap: Bitmap?, width: Int, height: Int): Bitmap {
        val m = Matrix()
        m.setRectToRect(
            RectF(0F, 0F, bitmap!!.width.toFloat(), bitmap.height.toFloat()), RectF(
                0F, 0F,
                width.toFloat(),
                height.toFloat()
            ), Matrix.ScaleToFit.CENTER
        )
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, m, true)
    }

    companion object {
        fun enhanceImage(mBitmap: Bitmap, contrast: Float, brightness: Float): Bitmap {
            val cm = ColorMatrix(
                floatArrayOf(
                    contrast,
                    0f,
                    0f,
                    0f,
                    brightness,
                    0f,
                    contrast,
                    0f,
                    0f,
                    brightness,
                    0f,
                    0f,
                    contrast,
                    0f,
                    brightness,
                    0f,
                    0f,
                    0f,
                    1f,
                    0f
                )
            )
            val mEnhancedBitmap = Bitmap.createBitmap(
                mBitmap.width, mBitmap.height, mBitmap
                    .config
            )
            val canvas = Canvas(mEnhancedBitmap)
            val paint = Paint()
            paint.colorFilter = ColorMatrixColorFilter(cm)
            canvas.drawBitmap(mBitmap, 0f, 0f, paint)
            return mEnhancedBitmap
        }
    }
}
