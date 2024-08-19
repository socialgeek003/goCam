package com.app.gocam.example

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.RadioButton
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.app.gocam.example.databinding.ActivityMainBinding
import com.lib.gocam.activity.PickerActivity
import com.lib.gocam.activity.ScanActivity
import com.lib.gocam.activity.ScannerActivity
import com.lib.gocam.utility.ManagePermissions
import com.lib.gocam.utility.ScanConstants
import com.lib.gocam.utility.ScanConstants.Companion.AADHAAR
import com.lib.gocam.utility.ScanConstants.Companion.ADDITIONAL_WATERMARK
import com.lib.gocam.utility.ScanConstants.Companion.ADD_LOCATION
import com.lib.gocam.utility.ScanConstants.Companion.CAMERA_TYPE
import com.lib.gocam.utility.ScanConstants.Companion.CAPTURE_TYPE
import com.lib.gocam.utility.ScanConstants.Companion.FILE_TYPE
import com.lib.gocam.utility.ScanConstants.Companion.GUIDELINES_ENABLE
import com.lib.gocam.utility.ScanConstants.Companion.GUIDELINES_ORIENTATION
import com.lib.gocam.utility.ScanConstants.Companion.IMAGE
import com.lib.gocam.utility.ScanConstants.Companion.IMAGE_COPY_BEFORE_LOCATION
import com.lib.gocam.utility.ScanConstants.Companion.IMAGE_COUNT
import com.lib.gocam.utility.ScanConstants.Companion.IMAGE_NAME
import com.lib.gocam.utility.ScanConstants.Companion.IMAGE_PATH
import com.lib.gocam.utility.ScanConstants.Companion.IS_AADHAR_MASKING
import com.lib.gocam.utility.ScanConstants.Companion.IS_BLANK_DOC
import com.lib.gocam.utility.ScanConstants.Companion.IS_DOC
import com.lib.gocam.utility.ScanConstants.Companion.IS_UPLOAD_AADHAR
import com.lib.gocam.utility.ScanConstants.Companion.LOCATION
import com.lib.gocam.utility.ScanConstants.Companion.OTHER
import com.lib.gocam.utility.ScanConstants.Companion.PAN
import com.lib.gocam.utility.ScanConstants.Companion.PAN_OCR
import com.lib.gocam.utility.ScanConstants.Companion.PDF
import com.lib.gocam.utility.ScanConstants.Companion.PDF_CREATE
import com.lib.gocam.utility.ScanConstants.Companion.PDF_NAME
import com.lib.gocam.utility.ScanConstants.Companion.PDF_PATH
import com.lib.gocam.utility.ScanConstants.Companion.REQUEST
import com.lib.gocam.utility.ScanConstants.Companion.VIDEO_MAX_TIME
import com.lib.gocam.utility.ScanConstants.Companion.WATERMARK_ALIGN_X
import com.lib.gocam.utility.ScanConstants.Companion.WATERMARK_ALIGN_Y
import com.lib.gocam.utility.ScanConstants.Companion.WATERMARK_FONT_SIZE
import com.lib.gocam.utility.ScanConstants.Companion.WATERMARK_MESSAGE
import com.lib.gocam.utility.ScanConstants.Companion.WATERMARK_ROTATION
import com.lib.gocam.utility.Utils
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList


class MainActivity : AppCompatActivity() {

    private lateinit var docsFolder: File
    private val PermissionsRequestCode = 123
    private lateinit var managePermissions: ManagePermissions
    private var count = 1
    var PICK_IMAGE_MULTIPLE = 1
    var imageEncoded: String = ""
    var imagesEncodedList: ArrayList<String> = ArrayList()
    lateinit var radioButton: RadioButton
    lateinit var rbFile: RadioButton
    lateinit var rbOrientation: RadioButton

    lateinit var mainBinding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mainBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(mainBinding.root)

        docsFolder = File(applicationContext.getExternalFilesDir("").toString() + "/ScannerImages")
        if (!docsFolder.exists()) {
            docsFolder.mkdir()
            Log.i("", "Created a new directory for Images")
        }

        if (allPermissionsGranted()) {
            //startCamera()
        }else{
            managePermissions = ManagePermissions(this, PERMISSIONS_LIST, PermissionsRequestCode)

            managePermissions.checkPermissions()
        }



        mainBinding.tvCamera.setOnClickListener{ startCamera("Photo", false) }
        mainBinding.tvVideo.setOnClickListener{ startCamera("Video", false) }
        mainBinding.tvPanOcr.setOnClickListener{ startCamera("Photo", true) }
        mainBinding.tvScanner.setOnClickListener{ startScanner() }
        mainBinding.tvPickGallery.setOnClickListener{ startPicker() }
        mainBinding.ibCountMinus.setOnClickListener{
            if (mainBinding.tvCount.text.toString().toInt() != 1){
                var tempMinus = mainBinding.tvCount.text.toString().toInt() - 1
                mainBinding.tvCount.setText(tempMinus.toString())
            }
        }

        mainBinding.ibCountPlus.setOnClickListener{
            if (mainBinding.tvCount.text.toString().toInt() != 100){
                var tempPlus = mainBinding.tvCount.text.toString().toInt() + 1
                mainBinding.tvCount.setText(tempPlus.toString())
            }
        }
    }


    private fun startScanner(){
        val scannerIntent = Intent(this, ScannerActivity::class.java)
        var selectedId = mainBinding.radioGroup.checkedRadioButtonId
        radioButton = findViewById<View>(selectedId) as RadioButton
        var scanType = when (radioButton.text) {
            "Pan" -> {
                PAN
            }
            "Aadhaar" -> {
                AADHAAR
            }
            else -> {
                OTHER
            }
        }
        scannerIntent.putExtra(ScanConstants.SCAN_TYPE, scanType)
        scannerIntent.putExtra(ScanConstants.CUSTOM_MESSAGE, "Please use original Aadhaar Card.\nDo not use Smart Card, PVC Card & Xerox")
        resultLauncher.launch(scannerIntent)
    }

    private fun startPicker(){
        val mOutputDirectory: File = ScanActivity.getOutputDirectory(baseContext)
        val fileName = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US)
            .format(System.currentTimeMillis()) + ".jpg"

        val intent = Intent(this, PickerActivity::class.java)
        val jsonObject = JSONObject()
        try {
            if (mainBinding.tvCount.text.toString().toInt() != 0 && mainBinding.tvCount.text.toString().toInt() <= 10 ){
                count = mainBinding.tvCount.text.toString().toInt()
            }
            jsonObject.put(IMAGE_COUNT, count)
            if (mainBinding.swGeneratePdf.isChecked) {
                jsonObject.put(PDF_CREATE, true)
                Utils.printMessage("Pdf" + true)
            } else {
                jsonObject.put(PDF_CREATE, false)
                Utils.printMessage("Pdf" + false)
            }
            var selectedId = mainBinding.radioGroupFile.checkedRadioButtonId
            rbFile = findViewById<View>(selectedId) as RadioButton
            var fileType = when (rbFile.text) {
                "Image" -> {
                    IMAGE
                }
                "PDF" -> {
                    PDF
                }
                else -> {
                    IMAGE
                }
            }
            if (mainBinding.swAddWatermark.isChecked) {
                jsonObject.put(WATERMARK_MESSAGE, "CREDILITY")
                jsonObject.put(ADDITIONAL_WATERMARK, "i-XL Technologies")
                jsonObject.put(WATERMARK_FONT_SIZE, 45)
                jsonObject.put(WATERMARK_ALIGN_X, 280)
                jsonObject.put(WATERMARK_ALIGN_Y, 400)
                jsonObject.put(WATERMARK_ROTATION, 40)
                Utils.printMessage("WATERMARK_ENABLE" + true)
            }
            jsonObject.put(FILE_TYPE, fileType)
            if (mainBinding.swMaskAadhar.isChecked) {
                jsonObject.put(IS_UPLOAD_AADHAR, true)
                Utils.printMessage("Mask" + true)
            } else {
                jsonObject.put(IS_UPLOAD_AADHAR, false)
                Utils.printMessage("Mask" + false)
            }
            jsonObject.put(PDF_NAME, "pdfName.pdf")
            jsonObject.put(PDF_PATH, mOutputDirectory.toString())
            jsonObject.put(IMAGE_PATH, mOutputDirectory.toString())
            jsonObject.put(IMAGE_NAME, fileName)
        }catch (e: Exception){
            e.printStackTrace()
        }
        intent.putExtra(REQUEST, jsonObject.toString())
        resultLauncher.launch(intent)
    }

    private fun startCamera(what: String, panOCR: Boolean){
        val mOutputDirectory: File = ScanActivity.getOutputDirectory(baseContext)
        val fileName = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US)
            .format(System.currentTimeMillis()) + ".jpg"

        val videoFileName = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US)
            .format(System.currentTimeMillis()) + ".mp4"

        val intent = Intent(this, ScanActivity::class.java)

        try {
            if (mainBinding.tvCount.text.toString().toInt() != 0 && mainBinding.tvCount.text.toString().toInt() <= 100 ){
                count = mainBinding.tvCount.text.toString().toInt()
            }
            val jsonObject = JSONObject()
            if (what == "Photo") {
                jsonObject.put(IMAGE_COUNT, count)
                Utils.printMessage("My Directory:- $mOutputDirectory -- $fileName")
                jsonObject.put(IMAGE_PATH, mOutputDirectory.toString())
                jsonObject.put(IMAGE_NAME, fileName)
                jsonObject.put(PDF_NAME, "pdfName.pdf")
                jsonObject.put(PDF_PATH, mOutputDirectory.toString())
                jsonObject.put(LOCATION, 1)
                jsonObject.put(IS_DOC, true)
                jsonObject.put(PAN_OCR, panOCR)
                jsonObject.put(CAMERA_TYPE, 1)
                if (mainBinding.swEnableGuidelines.isChecked) {
                    jsonObject.put(GUIDELINES_ENABLE, true)
                    Utils.printMessage("GUIDELINES_ENABLE" + true)
                } else {
                    jsonObject.put(GUIDELINES_ENABLE, false)
                    Utils.printMessage("GUIDELINES_ENABLE" + false)
                }

                if (mainBinding.swAddWatermark.isChecked) {
                    jsonObject.put(WATERMARK_MESSAGE, "CREDILITY")
                    jsonObject.put(ADDITIONAL_WATERMARK, "i-XL Technologies")
                    jsonObject.put(WATERMARK_FONT_SIZE, 45)
                    jsonObject.put(WATERMARK_ALIGN_X, 600)
                    jsonObject.put(WATERMARK_ALIGN_Y, 800)
                    jsonObject.put(WATERMARK_ROTATION, 40)
                    Utils.printMessage("WATERMARK_ENABLE" + true)
                }

                var selectedId = mainBinding.radioOrientation.checkedRadioButtonId
                radioButton = findViewById<View>(selectedId) as RadioButton
                var orientation = when (radioButton.text) {
                    "Horizontal" -> {
                        "horizontal"
                    }
                    "Vertical" -> {
                        "vertical"
                    }
                    "Square" -> {
                        "square"
                    }
                    else -> {
                        "horizontal"
                    }
                }
                if (orientation.equals("square", true)){
                    jsonObject.put(IMAGE_COPY_BEFORE_LOCATION, true)
                }
                jsonObject.put(GUIDELINES_ORIENTATION, orientation)
                if (mainBinding.swMaskAadhar.isChecked) {
                    jsonObject.put(IS_AADHAR_MASKING, true)
                    Utils.printMessage("Mask" + true)
                } else {
                    jsonObject.put(IS_AADHAR_MASKING, false)
                    Utils.printMessage("Mask" + false)
                }
                if (mainBinding.swIsDocument.isChecked) {
                    jsonObject.put(IS_DOC, true)
                    Utils.printMessage("Dock" + true)
                } else {
                    jsonObject.put(IS_DOC, false)
                    Utils.printMessage("Dock" + false)
                }
                if (mainBinding.swIsBlankDocument.isChecked) {
                    jsonObject.put(IS_BLANK_DOC, true)
                    Utils.printMessage("IS_BLANK_DOC" + true)
                } else {
                    jsonObject.put(IS_BLANK_DOC, false)
                    Utils.printMessage("IS_BLANK_DOC" + false)
                }
                if (mainBinding.swGeneratePdf.isChecked) {
                    jsonObject.put(PDF_CREATE, true)
                    Utils.printMessage("Pdf" + true)
                } else {
                    jsonObject.put(PDF_CREATE, false)
                    Utils.printMessage("Pdf" + false)
                }
                if (mainBinding.swAddLocation.isChecked) {
                    jsonObject.put(ADD_LOCATION, true)
                    Utils.printMessage("Add Location" + true)
                } else {
                    jsonObject.put(ADD_LOCATION, false)
                    Utils.printMessage("Add Location" + false)
                }
                jsonObject.put(CAPTURE_TYPE, what)
            }else{
                jsonObject.put(IMAGE_COUNT, 1)
                Utils.printMessage("My Directory:- $mOutputDirectory -- $videoFileName")
                jsonObject.put(IMAGE_PATH, mOutputDirectory.toString())
                jsonObject.put(IMAGE_NAME, videoFileName)
                jsonObject.put(PDF_NAME, "")
                jsonObject.put(PDF_PATH, "")
                jsonObject.put(LOCATION, 1)
                jsonObject.put(IS_AADHAR_MASKING, false)
                jsonObject.put(CAMERA_TYPE, 1)
                jsonObject.put(PDF_CREATE, false)
                jsonObject.put(CAPTURE_TYPE, what)
                jsonObject.put(VIDEO_MAX_TIME, 30)
            }
            /*if (radioButtonDoc.getText() == "Yes") {
                jsonObject.put(IS_DOC, true)
                Utils.printMessage("isDoc" + true)
            } else {
                jsonObject.put(IS_DOC, false)
                Utils.printMessage("isDoc" + false)
            }*/


            intent.putExtra(REQUEST, jsonObject.toString())
        } catch (e: JSONException) {
            e.printStackTrace()
        }

        resultLauncher.launch(intent)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults:
        IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(allPermissionsGranted()){
            //startCamera()
        }else {
            managePermissions.checkPermissions()
        }
    }



    private var resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // There are no request codes
            val data: Intent? = result.data
            val response = data?.extras?.get(ScanConstants.SCANNED_RESULT)
            Utils.printMessage("My Response: $response")
            Toast.makeText(this@MainActivity,"My Response: $response", Toast.LENGTH_LONG).show()

        }
    }


    private fun allPermissionsGranted() = permissionLists.all {
        ContextCompat.checkSelfPermission(
            this, it
        ) == PackageManager.PERMISSION_GRANTED
    }


    val PERMISSIONS_LIST = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        listOf<String>(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    } else {
        listOf<String>(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    }

    val permissionLists = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    } else {
        arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    }

}

