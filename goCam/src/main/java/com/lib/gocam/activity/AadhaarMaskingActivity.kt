package com.lib.gocam.activity

import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Intent
import android.content.IntentFilter
import android.graphics.*
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.util.Log
import android.util.SparseArray
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.vision.Frame
import com.google.android.gms.vision.text.TextBlock
import com.google.android.gms.vision.text.TextRecognizer
import com.lib.gocam.R
import com.lib.gocam.databinding.AadharMaskingLayoutBinding
import com.lib.gocam.fragment.ResultFragment
import com.lib.gocam.utility.ScanConstants.Companion.MASKING_DONE
import com.lib.gocam.utility.Utils
import java.io.File
import java.io.FileOutputStream
import java.util.*
import java.util.concurrent.Executors

class AadhaarMaskingActivity: AppCompatActivity() {
    

    var bitmap: Bitmap? = null
    var imagePath: String? = null
    var progressDialog: ProgressDialog? = null
    var tempBitmap: Bitmap? = null
    var manual = false
    var autoMasking = false

    lateinit var aadharMaskingLayoutBinding: AadharMaskingLayoutBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        aadharMaskingLayoutBinding = AadharMaskingLayoutBinding.inflate(layoutInflater)
        setContentView(aadharMaskingLayoutBinding.root)
        if (supportActionBar != null) {
            supportActionBar?.hide()
        }
        
        progressDialog = ProgressDialog(this)
        val intent: Intent = intent
        imagePath = intent.extras!!.getString("imagePath")
        Utils.getBitmapFromPath(imagePath)?.let { aadharMaskingLayoutBinding.ivCustom.setBitmap(it) }
        aadhaarMasking(Utils.getBitmapFromPath(imagePath))
        /*val adharMAsking = AdharMAsking(this)
        adharMAsking.execute(Utils.getBitmapFromPath(imagePath))*/
        aadharMaskingLayoutBinding.saveButton.setOnClickListener(View.OnClickListener {
            startProgress(true)
            if (manual) {
                bitmap = aadharMaskingLayoutBinding.ivCustom.getBitmap()
                bitmap?.let { saveImage(it) }
            } else {
                tempBitmap?.let { it1 -> saveImage(it1) }
            }
        })
        aadharMaskingLayoutBinding.btnClose.setOnClickListener {
            MASKING_DONE = true
            finish()
        }
    }

    private fun startProgress(boolean: Boolean){
        if (boolean){
            aadharMaskingLayoutBinding.aadhaarProgressLayout.visibility = View.VISIBLE
            aadharMaskingLayoutBinding.aadhaarProgressBar.visibility = View.VISIBLE
            window.setFlags(
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
        }else{
            aadharMaskingLayoutBinding.aadhaarProgressLayout.visibility = View.GONE
            aadharMaskingLayoutBinding.aadhaarProgressBar.visibility = View.GONE
            window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
        }
    }

    fun loadBitmapFromView(v: View, width: Int, height: Int): Bitmap? {
        val b = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
        val c = Canvas(b)
        v.layout(0, 0, v.layoutParams.width, v.layoutParams.height)
        v.draw(c)
        return b
    }

    open fun manualMasking() {
        manual = true
        aadharMaskingLayoutBinding.ivAutomatic.visibility = View.GONE
        aadharMaskingLayoutBinding.ivCustom.visibility = View.VISIBLE
    }

    private fun aadhaarMasking(bitmap: Bitmap?){
        val executor = Executors.newSingleThreadExecutor()
        val handler = Handler(Looper.getMainLooper())


        executor.execute {
            /*
            * Your task will be executed here
            * you can execute anything here that
            * you cannot execute in UI thread
            * for example a network operation
            * This is a background thread and you cannot
            * access view elements here
            *
            * its like doInBackground()
            * */
            generateOcr(bitmap)


            handler.post {
                /*
                * You can perform any operation that
                * requires UI Thread here.
                *
                * its like onPostExecute()
                * */
                runOnUiThread {
                    if (!autoMasking) {
                        Toast.makeText(this, "Aadhaar number not found", Toast.LENGTH_LONG).show()
                        val builder1 = AlertDialog.Builder(
                            this
                        )
                        builder1.setMessage(this.resources.getString(R.string.manual_mask))
                        builder1.setCancelable(true)
                        builder1.setPositiveButton(
                            "Yes"
                        ) { dialog, id ->
                            dialog.cancel()
                            System.gc()
                            runOnUiThread(Runnable { manualMasking() })
                        }
                        builder1.setNegativeButton(
                            "No"
                        ) { dialog, id ->
                            dialog.cancel()
                            aadharMaskingLayoutBinding.ivAutomatic.visibility = View.VISIBLE
                            aadharMaskingLayoutBinding.ivCustom.visibility = View.GONE
                            tempBitmap = Utils.getBitmapFromPath(imagePath)
                            aadharMaskingLayoutBinding.ivAutomatic.setImageBitmap(Utils.getBitmapFromPath(imagePath))
                        }
                        val alert11 = builder1.create()
                        alert11.show()
                    } else {
                        aadharMaskingLayoutBinding.ivAutomatic.visibility = View.VISIBLE
                        aadharMaskingLayoutBinding.ivCustom.visibility = View.GONE
                        aadharMaskingLayoutBinding.ivAutomatic.setImageBitmap(tempBitmap)
                        if (ResultFragment.isDocument){
                            aadharMaskingLayoutBinding.saveButton.performClick()
                        }
                    }
                }

            }
        }

    }

    private fun generateOcr(bitmap: Bitmap?) {
        manual = false
        val textRecognizer = TextRecognizer.Builder(this).build()
        if (!textRecognizer.isOperational) {
            Log.e("Main Activity", "Dependencies not available")

            // Check android for low storage so dependencies can be loaded, DEPRICATED CHANGE LATER
            val intentLowStorage = IntentFilter(Intent.ACTION_DEVICE_STORAGE_LOW)
            val hasLowStorage = registerReceiver(null, intentLowStorage) != null
            if (hasLowStorage) {
                Toast.makeText(this, "Low Memory On Disk", Toast.LENGTH_LONG)
                Log.e("Main Activity", "Low Memory On Disk")
            }
        } else if (bitmap != null) {
            val frame = Frame.Builder().setBitmap(bitmap).build()
            val items: SparseArray<*> = textRecognizer.detect(frame)
            val blocks: MutableList<TextBlock?> = ArrayList()
            var myItem: TextBlock? = null
            for (i in 0 until items.size()) {
                myItem = items.valueAt(i) as TextBlock

                //Add All TextBlocks to the `blocks` List
                blocks.add(myItem)
            }
            //END OF DETECTING TEXT

            //The Color of the Rectangle to Draw on top of Text
            val rectPaint = Paint()
            rectPaint.color = Color.WHITE
            rectPaint.style = Paint.Style.FILL
            rectPaint.strokeWidth = 4.0f

            //Create the Canvas object,
            //Which ever way you do image that is ScreenShot for example, you
            //need the views Height and Width to draw recatngles
            //because the API detects the position of Text on the View
            //So Dimesnions are important for Draw method to draw at that Text
            //Location
            tempBitmap = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.RGB_565)
            val canvas = Canvas(tempBitmap!!).apply {
                drawBitmap(bitmap, 0f, 0f, null)
            }

            //Loop through each `Block`
            val rectFS = ArrayList<Rect>()
            for (textBlock in blocks) {
                run {
                    val textLines =
                        textBlock!!.components

                    //loop Through each `Line`
                    for (currentLine in textLines) {
                        val words: MutableList<out com.google.android.gms.vision.text.Text?>? =
                            currentLine.components
                        println("currentword.getValue() " + words?.size)
                        //Loop through each `Word`
                        var old: android.graphics.Rect? = null
                        var cont: Int = 0
                        var add: kotlin.Boolean = false
                        for (currentWord in words!!) {
                            if (currentWord!!.value.length == 4 && TextUtils.isDigitsOnly(
                                    currentWord.value
                                        .trim { it <= ' ' }) && words.size == 3
                            ) {
                                cont++
                            }
                        }
                        if (cont >= 2) {
                            var check_2: Int = 0
                            for (currentword in words) {
                                if (currentword!!.value.length == 4) {
                                    check_2++
                                }
                            }
                            if (check_2 == 3) {
                                add = true
                                cont = 0
                            } else {
                                add = false
                            }
                        }
                        if (add) {
                            for (currentword in words) {
                                //Get the Rectangle/boundingBox of the word
                                val rect: android.graphics.Rect =
                                    android.graphics.Rect(currentword!!.boundingBox)
                                rectPaint.color = android.graphics.Color.RED
                                //                                     rectFS.add(rect);
                                println("currentword.getValue() " + currentword.boundingBox + " " + currentword.value)
                                //                                    if (/*currentword.getValue().length() == 4 && */TextUtils.isDigitsOnly(currentword.getValue().trim()) && words.size() == 3) {
                                println("currentword.getValue() " + currentword.boundingBox.left + " " + currentword.boundingBox.top + " " + currentword.boundingBox.right + " " + currentword.boundingBox.bottom)
                                var check: kotlin.Boolean = false
                                if (old != null) {
                                    //if (rect.top < old.top+10 && rect.top> old.top-10) {
                                    check = true
                                    // }
                                } else {
                                    check = true
                                    old = rect
                                }
                                if (cont < 2 && check) {
                                    cont += 1
                                    if (cont == 2) {
                                        var bottom: Int = 0
                                        if (rect.bottom > old.bottom) {
                                            bottom = rect.bottom
                                        } else {
                                            bottom = old.bottom
                                        }
                                        var top: Int = 0
                                        if (rect.top < old.top) {
                                            top = rect.top
                                        } else {
                                            top = old.top
                                        }
                                        val rectMask: android.graphics.Rect =
                                            android.graphics.Rect(
                                                old.left,
                                                top,
                                                rect.right,
                                                bottom
                                            )
                                        //  rectFS.add(old);
                                        rectFS.add(rectMask)
                                    }
                                }
                            }
                            //Finally Draw Rectangle/boundingBox around word

//                                }
                        }
                    }
                }
            }
            if (rectFS.size > 0) {
                autoMasking = true
                for (rect in rectFS) {
                    canvas.drawRect(rect, rectPaint)
                }
            } else {
                autoMasking = false
            }

            //Set image to the `View`
//            aadharMaskingLayoutBinding.ivAutomaticetImageBitmap(tempBitmap);
            //  imgView.setImageDrawable(new BitmapDrawable(getResources(), tempBitmap));
        }
    }

    open fun saveImage(finalBitmap: Bitmap) {
        Utils.printMessage("PATH:::$imagePath")
        val file = File(imagePath)
        if (file.exists()) file.delete()
        try {
            val out = FileOutputStream(file)
            finalBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
            out.flush()
            out.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }

//        Intent result = new Intent();
//        result.putExtra(MASKING_RESULT, imagePath);
//        setResult(RESULT_OK, result);
        MASKING_DONE = true
        /*val jsonObject = JSONObject()
        try {
            jsonObject.put("ImagePath", )
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        val result = Intent()
        result.putExtra(SCANNED_RESULT, jsonObject.toString())
        setResult(RESULT_OK, result)*/
        startProgress(false)
        finish()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        MASKING_DONE = true
        finish()
    }
}