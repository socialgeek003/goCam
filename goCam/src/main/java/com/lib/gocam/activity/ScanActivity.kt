package com.lib.gocam.activity


import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.*
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraMetadata
import android.hardware.display.DisplayManager
import android.location.Location
import android.media.AudioManager
import android.media.MediaActionSound
import android.net.Uri
import android.os.*
import android.provider.MediaStore
import android.util.*
import android.view.*
import android.view.animation.Animation
import android.view.animation.RotateAnimation
import android.widget.Chronometer.GONE
import android.widget.Chronometer.OnChronometerTickListener
import android.widget.Chronometer.VISIBLE
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.core.*
import androidx.camera.core.Camera
import androidx.camera.core.VideoCapture
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.camera.video.VideoRecordEvent.Finalize
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.net.toUri
import androidx.core.util.Consumer
import androidx.exifinterface.media.ExifInterface
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import com.google.android.gms.vision.Frame
import com.google.android.gms.vision.text.TextBlock
import com.google.android.gms.vision.text.TextRecognizer
import com.lib.gocam.R
import com.lib.gocam.databinding.ScanActivityBinding
import com.lib.gocam.fragment.BrightnessFragment
import com.lib.gocam.fragment.ResultFragment
import com.lib.gocam.latlng.LatLngModule
import com.lib.gocam.latlng.LatLngRead
import com.lib.gocam.model.ImageData
import com.lib.gocam.timer.CustomCountTimer
import com.lib.gocam.utility.ScanConstants.Companion.ACCURACY
import com.lib.gocam.utility.ScanConstants.Companion.ADD
import com.lib.gocam.utility.ScanConstants.Companion.ADDITIONAL_INFO
import com.lib.gocam.utility.ScanConstants.Companion.ADDITIONAL_WATERMARK
import com.lib.gocam.utility.ScanConstants.Companion.ADD_LOCATION
import com.lib.gocam.utility.ScanConstants.Companion.BRIGHTNESS_FRAGMENT
import com.lib.gocam.utility.ScanConstants.Companion.CAMERA_TYPE
import com.lib.gocam.utility.ScanConstants.Companion.CAPTURE_TYPE
import com.lib.gocam.utility.ScanConstants.Companion.CLICK_PHOTO
import com.lib.gocam.utility.ScanConstants.Companion.CODE
import com.lib.gocam.utility.ScanConstants.Companion.ERROR
import com.lib.gocam.utility.ScanConstants.Companion.FLIP_HORIZONTAL
import com.lib.gocam.utility.ScanConstants.Companion.FLIP_VERTICAL
import com.lib.gocam.utility.ScanConstants.Companion.GUIDELINES_ENABLE
import com.lib.gocam.utility.ScanConstants.Companion.GUIDELINES_MESSAGE
import com.lib.gocam.utility.ScanConstants.Companion.GUIDELINES_ORIENTATION
import com.lib.gocam.utility.ScanConstants.Companion.IMAGE_COPY_BEFORE_LOCATION
import com.lib.gocam.utility.ScanConstants.Companion.IMAGE_COUNT
import com.lib.gocam.utility.ScanConstants.Companion.IMAGE_DATA
import com.lib.gocam.utility.ScanConstants.Companion.IMAGE_NAME
import com.lib.gocam.utility.ScanConstants.Companion.IMAGE_PATH
import com.lib.gocam.utility.ScanConstants.Companion.IMAGE_PROCESS
import com.lib.gocam.utility.ScanConstants.Companion.IS_AADHAR_MASKING
import com.lib.gocam.utility.ScanConstants.Companion.IS_BLANK_DOC
import com.lib.gocam.utility.ScanConstants.Companion.IS_DOC
import com.lib.gocam.utility.ScanConstants.Companion.LAT
import com.lib.gocam.utility.ScanConstants.Companion.LNG
import com.lib.gocam.utility.ScanConstants.Companion.LOCATION
import com.lib.gocam.utility.ScanConstants.Companion.MESSAGE
import com.lib.gocam.utility.ScanConstants.Companion.PAN_OCR
import com.lib.gocam.utility.ScanConstants.Companion.PDF_CREATE
import com.lib.gocam.utility.ScanConstants.Companion.PDF_NAME
import com.lib.gocam.utility.ScanConstants.Companion.PDF_PATH
import com.lib.gocam.utility.ScanConstants.Companion.REPLACE
import com.lib.gocam.utility.ScanConstants.Companion.REQUEST
import com.lib.gocam.utility.ScanConstants.Companion.RESULT_FRAGMENT
import com.lib.gocam.utility.ScanConstants.Companion.SCANNED_RESULT
import com.lib.gocam.utility.ScanConstants.Companion.START_VIDEO
import com.lib.gocam.utility.ScanConstants.Companion.STOP_VIDEO
import com.lib.gocam.utility.ScanConstants.Companion.SUCCESS
import com.lib.gocam.utility.ScanConstants.Companion.VIDEO_MAX_TIME
import com.lib.gocam.utility.ScanConstants.Companion.WATERMARK_ALIGN_X
import com.lib.gocam.utility.ScanConstants.Companion.WATERMARK_ALIGN_Y
import com.lib.gocam.utility.ScanConstants.Companion.WATERMARK_FONT_SIZE
import com.lib.gocam.utility.ScanConstants.Companion.WATERMARK_MESSAGE
import com.lib.gocam.utility.ScanConstants.Companion.WATERMARK_ROTATION
import com.lib.gocam.utility.Utils
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.*
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.*
import kotlin.math.*


class ScanActivity : AppCompatActivity(), LatLngRead, Consumer<VideoRecordEvent>,
    SensorEventListener {
    private val mDisplayManager by lazy {
        getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
    }

    lateinit var scanActivityBinding: ScanActivityBinding

    private var startHTime = 1L
    private var customHandler = Handler(Looper.getMainLooper())
    private var timeInMilliseconds = 0L

    private lateinit var countDownTimerWithPause: CustomCountTimer

    private lateinit var mOutputDirectory: File
    private lateinit var mCameraExecutor: Executor
    private lateinit var mImageAnalyzerExecutor: ExecutorService

    private var isTakingVideo = false
    private var isVideoPause = false
    private var mPreview: Preview? = null
    private var mImageAnalyzer: ImageAnalysis? = null
    private var mImageCapture: ImageCapture? = null
    private var mVideoCapture: VideoCapture? = null
    var newVideoCapture: androidx.camera.video.VideoCapture<Recorder>? = null
    var recording: Recording? = null
    private var mCamera: Camera? = null
    private var mCameraProvider: ProcessCameraProvider? = null
    private var mDisplayId: Int = -1
    private var mLensFacing: Int = CameraSelector.LENS_FACING_BACK
    private var mFlashMode: Int = ImageCapture.FLASH_MODE_OFF

    var scanImage: Bitmap? = null
    var count = 0
    private var camera_type = 1
    var captureCount = 0
    var remainingCount = 0
    var pass_degree: Double = 0.0

    var path: String = ""
    var createPdf = false
    var addLocation = false
    var checkDoc = false
    var checkBlankDoc = false
    var imageProcess = false
    var isLocationEnabled: Boolean = false
    var isPanOcr: Boolean = false
    var imagePathList: ArrayList<ImageData> = ArrayList()
    var aadharMasking = false
    var imageName: String = ""
    private var transformed: Bitmap? = null
    var requestData: String = ""
    var imagePath: String = ""
    var imageProcessPath: String = ""
    var captureType: String = ""
    var pdfPath: String = ""
    var pdfName: String = ""
    lateinit var latLngModule: LatLngModule
    lateinit var location: Location
    var originalImagePath = ""
    var isRetake = false
    var imageCopyBefore = false

    var FM: FragmentManager? = null
    var ft: FragmentTransaction? = null
    var ReplacingFragment: Fragment? = null
    var current_tag = "ScanActivity"
    var last_tag = "ScanActivity"
    lateinit var resultFragment: ResultFragment
    lateinit var brightnessFragment: BrightnessFragment

    var MAX_VIDEO_TIME = 0

    var additionalInfo = ""
    var guidelinesEnable = false
    var guidelinesOrientation = "horizontal"

    //Watermark
    var watermarkMessage = ""
    var additionalWatermark = ""
    var watermarkFontSize = 0f
    var watermarkAlignX = 0f
    var watermarkAlignY = 0f
    var watermarkRotation = 0f

    var pauseTime = 0L
    var resumeTime = 0L
    var preResumeTime = 0L
    var startTime = 0L

    @JvmName("getCaptureType1")
    fun getCaptureType(): String {
        return captureType
    }

    @JvmName("setImageProcess1")
    fun setCaptureType(capture: String) {
        this.captureType = capture
    }

    @JvmName("getImageCopy1")
    fun getImageCopy(): Boolean {
        return imageCopyBefore
    }

    @JvmName("setImageCopy1")
    fun setImageCopy(copy: Boolean) {
        this.imageCopyBefore = copy
    }

    fun isImageProcess(): Boolean {
        return imageProcess
    }

    @JvmName("setImageProcess1")
    fun setImageProcess(imageProcess: Boolean) {
        this.imageProcess = imageProcess
    }

    fun isAadharMasking(): Boolean {
        return aadharMasking
    }

    @JvmName("setAadharMasking1")
    fun setAadharMasking(aadharMasking: Boolean) {
        this.aadharMasking = aadharMasking
    }

    @JvmName("isPanOcr1")
    fun isPanOcr(): Boolean {
        return isPanOcr
    }

    @JvmName("setPanOcr1")
    fun setPanOcr(panOcr: Boolean) {
        isPanOcr = panOcr
    }

    fun getTransformed(): Bitmap? {
        return transformed
    }

    fun setTransformed(transformed: Bitmap?) {
        this.transformed = transformed
    }

    fun isCreatePdf(): Boolean {
        return createPdf
    }

    @JvmName("setCreatePdf1")
    fun setCreatePdf(createPdf: Boolean) {
        this.createPdf = createPdf
    }

    fun isAddLocation(): Boolean {
        return addLocation
    }

    @JvmName("setAddLocation1")
    fun setAddLocation(addLoc: Boolean) {
        this.addLocation = addLoc
    }

    @JvmName("getPath1")
    fun getPath(): String {
        return path
    }

    @JvmName("setPath1")
    fun setPath(path: String?) {
        this.path = path!!
    }

    @JvmName("getScanImage1")
    fun getScanImage(): Bitmap? {
        return scanImage
    }

    @JvmName("setScanImage1")
    fun setScanImage(scanImage: Bitmap?) {
        this.scanImage = scanImage
    }

    fun isCheckDoc(): Boolean {
        return checkDoc
    }

    @JvmName("setCheckDoc1")
    fun setCheckDoc(checkDoc: Boolean) {
        this.checkDoc = checkDoc
    }
    fun isCheckBlankDoc(): Boolean {
        return checkBlankDoc
    }

    @JvmName("setCheckBlankDoc1")
    fun setCheckBlankDoc(checkBlankDoc: Boolean) {
        this.checkBlankDoc = checkBlankDoc
    }

    private var currentDegree = 0f
    private var mSensorManager: SensorManager? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        scanActivityBinding = ScanActivityBinding.inflate(layoutInflater)
        setContentView(scanActivityBinding.root)


        if (supportActionBar != null) {
            supportActionBar!!.hide()
        }
        scanActivityBinding.progressBar.setIndicatorColor(ContextCompat.getColor(baseContext, R.color.white))
        scanActivityBinding.closeButton.setOnClickListener { onBackPressed() }
        allPermissionsGranted()
        this.location = Location("ImagesLocation")
        imagePathList = ArrayList<ImageData>()
        scanActivityBinding.content.visibility = View.GONE
        val intent = intent
        if (intent != null) {
            requestData = intent.extras!!.getString(REQUEST)!!
            try {
                val jsonObject = JSONObject(requestData)
                Utils.printMessage("My Directory:- $jsonObject")
                if (jsonObject.has(IMAGE_PROCESS)) {
                    //setImageProcess(jsonObject.getBoolean(IMAGE_PROCESS))
                    setImageProcess(true)
                }
                if (jsonObject.has(ADDITIONAL_INFO)) {
                    additionalInfo = jsonObject.getString(ADDITIONAL_INFO)
                }
                if (jsonObject.has(GUIDELINES_ENABLE)) {
                    guidelinesEnable = jsonObject.optBoolean(GUIDELINES_ENABLE)
                }
                if (jsonObject.has(GUIDELINES_ORIENTATION)) {
                    guidelinesOrientation = jsonObject.getString(GUIDELINES_ORIENTATION)
                }
                if (jsonObject.has(GUIDELINES_MESSAGE)) {
                    var message = ""
                    message = jsonObject.getString(GUIDELINES_MESSAGE)
                    if (message.isNotBlank()) {
                        scanActivityBinding.tvCustomMessage.text = message
                        scanActivityBinding.tvCustomMessage.visibility = View.VISIBLE
                    }
                }
                if (isImageProcess()) {
                    if (jsonObject.has(IMAGE_PATH)) {
                        imageProcessPath = jsonObject.getString(IMAGE_PATH)
                        //sendImageToProcess()
                    } else {
                        createJSONDataONERROR(
                            101,
                            baseContext.resources.getString(R.string.image_path_error)
                        )
                    }
                } else {
                    if (jsonObject.has(IMAGE_COUNT)) {
                        count = jsonObject.getInt(IMAGE_COUNT)
                    } else {
                        createJSONDataONERROR(
                            101,
                            baseContext.resources.getString(R.string.image_count_error)
                        )
                    }
                    if (jsonObject.has(IMAGE_PATH)) {
                        imagePath = jsonObject.getString(IMAGE_PATH)
                        setPath(imagePath)
                    } else {
                        createJSONDataONERROR(
                            101,
                            baseContext.resources.getString(R.string.image_path_error)
                        )
                    }
                    if (jsonObject.has(IMAGE_NAME)) {
                        imageName = jsonObject.getString(IMAGE_NAME)
                    } else {
                        createJSONDataONERROR(
                            101,
                            baseContext.resources.getString(R.string.image_name_error)
                        )
                    }
                    if (jsonObject.has(PDF_CREATE)) {
                        setCreatePdf(jsonObject.getBoolean(PDF_CREATE))
                    }
                    if (isCreatePdf()) {
                        if (jsonObject.has(PDF_PATH)) {
                            pdfPath = jsonObject.getString(PDF_PATH)
                        } else {
                            createJSONDataONERROR(
                                101,
                                baseContext.resources.getString(R.string.pdf_path_error)
                            )
                        }
                        if (jsonObject.has(PDF_NAME)) {
                            pdfName = jsonObject.getString(PDF_NAME)
                        } else {
                            createJSONDataONERROR(
                                101,
                                baseContext.resources.getString(R.string.pdf_name_error)
                            )
                        }
                    }
                    if (jsonObject.has(IMAGE_COPY_BEFORE_LOCATION)){
                        setImageCopy(jsonObject.getBoolean(IMAGE_COPY_BEFORE_LOCATION))
                    }
                    if (jsonObject.has(WATERMARK_MESSAGE)){
                        watermarkMessage = jsonObject.getString(WATERMARK_MESSAGE)
                    }
                    if (jsonObject.has(ADDITIONAL_WATERMARK)){
                        additionalWatermark = jsonObject.getString(ADDITIONAL_WATERMARK)
                    }
                    if (jsonObject.has(WATERMARK_FONT_SIZE)){
                        watermarkFontSize = try {
                            jsonObject.optDouble(WATERMARK_FONT_SIZE).toFloat()
                        } catch (e: Exception) {
                            0f
                        }
                    }
                    if (jsonObject.has(WATERMARK_ALIGN_X)){
                        watermarkAlignX = try {
                            jsonObject.optDouble(WATERMARK_ALIGN_X).toFloat()
                        } catch (e: Exception) {
                            0f
                        }
                    }
                    if (jsonObject.has(WATERMARK_ALIGN_Y)){
                        watermarkAlignY = try {
                            jsonObject.optDouble(WATERMARK_ALIGN_Y).toFloat()
                        } catch (e: Exception) {
                            0f
                        }
                    }
                    if (jsonObject.has(WATERMARK_ROTATION)){
                        watermarkRotation = try {
                            jsonObject.optDouble(WATERMARK_ROTATION).toFloat()
                        } catch (e: Exception) {
                            0f
                        }
                    }
                    if (jsonObject.has(IS_DOC)) {
                        setCheckDoc(jsonObject.getBoolean(IS_DOC))
                    }
                    if (jsonObject.has(IS_BLANK_DOC)) {
                        setCheckBlankDoc(jsonObject.getBoolean(IS_BLANK_DOC))
                    }
                    if (jsonObject.has(LOCATION)) {
                        isLocationEnabled = true
                        //isLocationEnabled = jsonObject.getString(LOCATION).equals("1", ignoreCase = true)
                    }
                    if (jsonObject.has(ADD_LOCATION)){
                        addLocation = jsonObject.getBoolean(ADD_LOCATION)
                    }
                    if (jsonObject.has(CAMERA_TYPE)) {
                        if (jsonObject.optInt(CAMERA_TYPE) == 1) {
                            camera_type = 1
                        } else if (jsonObject.optInt(CAMERA_TYPE) == 2) {
                            camera_type = 2
                        }
                    } else {
                        camera_type = 1
                    }
                    if (jsonObject.has(IS_AADHAR_MASKING)) {
                        if (jsonObject.getBoolean(IS_AADHAR_MASKING)) {
                            //if (jsonObject.optInt(IMAGE_COUNT) == 1) {
                            setAadharMasking(jsonObject.getBoolean(IS_AADHAR_MASKING))
                            /*} else {
                                createJSONDataONERROR(
                                    101,
                                    baseContext.resources.getString(R.string.image_masking)
                                )
                            }*/
                        }
                    }
                }
                if (jsonObject.has(CAPTURE_TYPE)){
                    setCaptureType(jsonObject.getString(CAPTURE_TYPE))
                    if (getCaptureType() == "Video"){
                        if (jsonObject.has(VIDEO_MAX_TIME)){
                            MAX_VIDEO_TIME = jsonObject.optInt(VIDEO_MAX_TIME)
                            if (MAX_VIDEO_TIME == 0){
                                createJSONDataONERROR(
                                    101,
                                    "Video Max Limit not found"
                                )
                            }
                        }else{
                            createJSONDataONERROR(
                                101,
                                "Video Max Limit not found"
                            )
                        }
                    }
                }
                if (jsonObject.has(PAN_OCR)){
                    val value = jsonObject.getBoolean(PAN_OCR)
                    if (value){
                        setPanOcr(true)
                        scanActivityBinding.layoutGuidelines.visibility = View.VISIBLE
                    }else{
                        setPanOcr(false)
                        if (!guidelinesEnable) {
                            scanActivityBinding.layoutGuidelines.visibility = View.GONE
                        }
                    }
                }
            } catch (e: JSONException) {
                e.printStackTrace()
            }
            Utils.printMessage("My Count IS:- $captureCount")
            if (count > 1){
                scanActivityBinding.scanImageLayout.visibility = View.VISIBLE
                scanActivityBinding.scanImageCount.text = (captureCount+1).toString() +"/"+ count + "Images"
            }else{
                scanActivityBinding.scanImageLayout.visibility = View.GONE
            }
        }

        //if (isLocationEnabled) {
        latLngModule = LatLngModule(this@ScanActivity, this)
        val temp = latLngModule.startLocation()
        Utils.printMessage("Location is $temp")
        //}

        if (guidelinesEnable) {
            scanActivityBinding.layoutGuidelines.visibility = View.VISIBLE
            when(guidelinesOrientation){
                "horizontal" -> {
                    val scale: Float = applicationContext.resources.displayMetrics.density
                    val pixels = (250 * scale + 0.5f).toInt()

                    var layoutParameters = scanActivityBinding.cameraPreview.layoutParams
                    layoutParameters.height = pixels
                    scanActivityBinding.cameraPreview.layoutParams = layoutParameters
                }
                "vertical" -> {
                    val scale: Float = applicationContext.resources.displayMetrics.density
                    val pixelsWidth = (350 * scale + 0.5f).toInt()
                    val pixelsHeight = (500 * scale + 0.5f).toInt()

                    var layoutParameters = scanActivityBinding.cameraPreview.layoutParams
                    layoutParameters.width = pixelsWidth
                    layoutParameters.height = pixelsHeight
                    scanActivityBinding.cameraPreview.layoutParams = layoutParameters

                    var viewParameters = scanActivityBinding.viewOcr.layoutParams
                    viewParameters.width = pixelsWidth
                    viewParameters.height = pixelsHeight
                    scanActivityBinding.viewOcr.layoutParams = viewParameters
                }
                "square" -> {
                    val scale: Float = applicationContext.resources.displayMetrics.density
                    val pixelsWidth = (350 * scale + 0.5f).toInt()
                    val pixelsHeight = (350 * scale + 0.5f).toInt()

                    var layoutParameters = scanActivityBinding.cameraPreview.layoutParams
                    layoutParameters.width = pixelsWidth
                    layoutParameters.height = pixelsHeight
                    scanActivityBinding.cameraPreview.layoutParams = layoutParameters

                    var viewParameters = scanActivityBinding.viewOcr.layoutParams
                    viewParameters.width = pixelsWidth
                    viewParameters.height = pixelsHeight
                    scanActivityBinding.viewOcr.layoutParams = viewParameters
                }
            }
        }

        if (getCaptureType() == "Video"){
            scanActivityBinding.captureImage.visibility = View.GONE
            scanActivityBinding.captureVideo.visibility = View.VISIBLE
        }else{
            scanActivityBinding.captureVideo.visibility = View.GONE
            scanActivityBinding.captureImage.visibility = View.VISIBLE
        }
        addListeners()
        mOutputDirectory = getOutputDirectory(baseContext)
        mCameraExecutor = ContextCompat.getMainExecutor(baseContext)
        mImageAnalyzerExecutor = Executors.newSingleThreadExecutor()
        mDisplayManager.registerDisplayListener(mDisplayListener, null)
        scanActivityBinding.cameraPreview.post {
            mDisplayId = scanActivityBinding.cameraPreview.display.displayId
            setUpCamera()
        }
        mSensorManager = getSystemService(SENSOR_SERVICE) as SensorManager?;

    }

    @SuppressLint("RestrictedApi")
    private val mDisplayListener = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId: Int) = Unit
        override fun onDisplayRemoved(displayId: Int) = Unit
        override fun onDisplayChanged(displayId: Int) {
            try {
                if (displayId == mDisplayId) {
                    mImageAnalyzer?.targetRotation = scanActivityBinding.cameraPreview.display.rotation
                    mImageCapture?.targetRotation = scanActivityBinding.cameraPreview.display.rotation
                    //mVideoCapture?.setTargetRotation(scanActivityBinding.cameraPreview.display.rotation)
                }
            } catch (e: Exception) {

            }
        }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun onDestroy() {
        super.onDestroy()
        mDisplayManager.unregisterDisplayListener(mDisplayListener)
    }

    private fun toggleCamera() {
        if (isTakingVideo) return
        mLensFacing = if (CameraSelector.LENS_FACING_FRONT == mLensFacing) {
            CameraSelector.LENS_FACING_BACK
        } else {
            CameraSelector.LENS_FACING_FRONT
        }
        bindCameraUseCases()
    }

    private fun toggleFlash() {
        //if (isTakingVideo) return
        when (mFlashMode) {
            ImageCapture.FLASH_MODE_OFF -> {
                mFlashMode = ImageCapture.FLASH_MODE_ON
                scanActivityBinding.flashToggle.setImageResource(R.drawable.ic_flash_on_white_20dp)
            }
            ImageCapture.FLASH_MODE_ON -> {
                mFlashMode = ImageCapture.FLASH_MODE_AUTO
                scanActivityBinding.flashToggle.setImageResource(R.drawable.ic_flash_auto_white_20dp)
            }
            ImageCapture.FLASH_MODE_AUTO -> {
                mFlashMode = ImageCapture.FLASH_MODE_OFF
                scanActivityBinding.flashToggle.setImageResource(R.drawable.ic_flash_off_white_20dp)
            }
        }
        // Re-bind use cases to include changes
        bindCameraUseCases()
    }

    private fun addListeners() {


        scanActivityBinding.captureImage.setOnClickListener { takePhoto() }
        scanActivityBinding.flashToggle.setOnClickListener { toggleFlash() }
        scanActivityBinding.rotateCamera.setOnClickListener { toggleCamera() }
        scanActivityBinding.captureVideo.setOnClickListener {
            if(isTakingVideo){
                stopVideo()
            }else{
                startVideo()
            }
        }
        scanActivityBinding.capturePause.setOnClickListener {
            if (countDownTimerWithPause.isTimerRunning()){
                if (countDownTimerWithPause.isTimerPaused()) {
                    countDownTimerWithPause.resumeCountDownTimer()
                    scanActivityBinding.capturePause.setImageDrawable(ResourcesCompat.getDrawable(resources, R.drawable.ic_round_pause_circle, null))
                    scanActivityBinding.capturePause.visibility = View.VISIBLE
                    recording?.resume()
                } else {
                    countDownTimerWithPause.pauseCountDownTimer()
                    scanActivityBinding.capturePause.setImageDrawable(ResourcesCompat.getDrawable(resources, R.drawable.ic_round_play_circle, null))
                    scanActivityBinding.capturePause.visibility = View.VISIBLE
                    recording?.pause()
                }
            }
            scanActivityBinding.capturePause.isEnabled = false

            Handler(Looper.getMainLooper()).postDelayed({ // This method will be executed once the timer is over
                scanActivityBinding.capturePause.isEnabled = true
            }, 1500)
        }
        initCountdownTimer()

    }

    @SuppressLint("SetTextI18n")
    private fun initCountdownTimer() {
        var mCount: Int

        countDownTimerWithPause =
            object : CustomCountTimer((MAX_VIDEO_TIME * 1000).toLong(), 1000) {

                override fun onTimerTick(timeRemaining: Long) {
                    mCount = MAX_VIDEO_TIME - (timeRemaining / 1000).toInt()
                    if (mCount % 2 == 0) {
                        scanActivityBinding.ivRecordImage.visibility = View.VISIBLE
                    } else {
                        scanActivityBinding.ivRecordImage.visibility = View.INVISIBLE
                    }
                    val newCount: Int = MAX_VIDEO_TIME - mCount
                    val asText = (String.format("%02d", newCount / 60) + ":"
                            + String.format("%02d", newCount % 60))
                    scanActivityBinding.textChrono.text = asText

                }

                override fun onTimerFinish() {
                    stopVideo()
                }


            }


    }

    private fun hasBackCamera(): Boolean {
        return mCameraProvider?.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA) ?: false
    }

    private fun hasFrontCamera(): Boolean {
        return mCameraProvider?.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA) ?: false
    }

    private fun hasFlashMode(): Boolean {
        return this.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)
    }

    private fun aspectRatio(width: Int, height: Int): Int {
        val previewRatio = max(width, height).toDouble() / min(width, height)
        if (abs(previewRatio - RATIO_4_3_VALUE) <= abs(previewRatio - RATIO_16_9_VALUE)) {
            return AspectRatio.RATIO_4_3
        }
        return AspectRatio.RATIO_16_9
    }

    private fun setUpCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this@ScanActivity)
        cameraProviderFuture.addListener(Runnable {
            // CameraProvider
            mCameraProvider = cameraProviderFuture.get()

            // Select lensFacing depending on the available cameras
            mLensFacing = when (camera_type) {
                2 -> when{
                    hasFrontCamera() -> CameraSelector.LENS_FACING_FRONT
                    hasBackCamera() -> CameraSelector.LENS_FACING_BACK
                    else -> throw IllegalStateException("Back and front camera are unavailable")
                }
                else -> when{
                    hasBackCamera() -> CameraSelector.LENS_FACING_BACK
                    hasFrontCamera() -> CameraSelector.LENS_FACING_FRONT
                    else -> throw IllegalStateException("Back and front camera are unavailable")
                }
            }
            mLensFacing = if (camera_type == 2){
                if (hasFrontCamera()){
                    CameraSelector.LENS_FACING_FRONT
                }else {
                    CameraSelector.LENS_FACING_BACK
                }
            }else{
                if (hasBackCamera()){
                    CameraSelector.LENS_FACING_BACK
                }else if (hasFrontCamera()){
                    CameraSelector.LENS_FACING_FRONT
                }else{
                    throw IllegalStateException("Back and front camera are unavailable")
                }
            }
            /*mLensFacing = when {
                hasBackCamera() -> CameraSelector.LENS_FACING_BACK
                hasFrontCamera() -> CameraSelector.LENS_FACING_FRONT
                else -> throw IllegalStateException("Back and front camera are unavailable")
            }*/

            mFlashMode = when {
                hasFlashMode() -> ImageCapture.FLASH_MODE_OFF
                else -> NO_FLASH
            }

            // Build and bind the camera use cases
            bindCameraUseCases()
            buildUi()
        }, mCameraExecutor)
    }

    @SuppressLint("RestrictedApi", "UnsafeOptInUsageError")
    private fun bindCameraUseCases() {
        val metrics = DisplayMetrics().also { scanActivityBinding.cameraPreview.display.getRealMetrics(it) }
        Log.d(TAG, "Screen metrics: ${metrics.widthPixels} x ${metrics.heightPixels}")

        val screenAspectRatio = aspectRatio(metrics.widthPixels, metrics.heightPixels)
        Log.d(TAG, "Preview aspect ratio: $screenAspectRatio")

        val rotation = scanActivityBinding.cameraPreview.display.rotation
        Log.d(TAG, "Rotation: $rotation")

        // CameraProvider
        val cameraProvider =
            mCameraProvider ?: throw IllegalStateException("Camera initialization failed.")

        // CameraSelector
        val cameraSelector = CameraSelector.Builder().apply {
            requireLensFacing(mLensFacing)
        }.build()

        // Preview
        mPreview = Preview.Builder().apply {
            setTargetAspectRatio(screenAspectRatio)
            setTargetRotation(rotation)
        }.build()

        // ImageAnalysis
        /*mImageAnalyzer = ImageAnalysis.Builder().apply {
            setTargetAspectRatio(screenAspectRatio)
            setTargetRotation(rotation)
        }.build().also {
            try {
                it.setAnalyzer(mImageAnalyzerExecutor, LuminosityAnalyzer { luma ->
                    //Log.d(TAG, "Average luminosity: $luma")
                })
            } catch (e: Exception) {
                it.setAnalyzer(ContextCompat.getMainExecutor(applicationContext),  LuminosityAnalyzer { luma ->
                    //Log.d(TAG, "Average luminosity: $luma")
                })
            }
        }*/

        // ImageCapture
        mImageCapture = ImageCapture.Builder().apply {
            setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            setTargetAspectRatio(screenAspectRatio)
            setTargetRotation(rotation)
            if (mFlashMode != NO_FLASH)
                setFlashMode(mFlashMode)
        }.build()

        //Video Capture
        val videoAspectRatio = aspectRatio(640 , 480)
        val videoResolution = Size(640 , 480)
        /*mVideoCapture = VideoCapture.Builder().apply {
            setTargetRotation(scanActivityBinding.cameraPreview.display.rotation)
            setTargetAspectRatio(screenAspectRatio)
            setBitRate(3000000)
        }.build()*/

        val cameraInfo = cameraProvider.availableCameraInfos.filter {
            Camera2CameraInfo
                .from(it)
                .getCameraCharacteristic(CameraCharacteristics.LENS_FACING) == CameraMetadata.LENS_FACING_BACK
        }

        val qualitySelector = QualitySelector.from(Quality.HD)


        val recorder = Recorder.Builder()
            .setExecutor(mCameraExecutor).setQualitySelector(qualitySelector)
            .build()
        newVideoCapture = androidx.camera.video.VideoCapture.withOutput(recorder)

        // Create MediaStoreOutputOptions for our recorder
        val name = "CameraX-recording-" +
                SimpleDateFormat(FILENAME_FORMAT, Locale.US)
                    .format(System.currentTimeMillis()) + ".mp4"
        val contentValues = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, imageName)
        }
        val mediaStoreOutput = MediaStoreOutputOptions.Builder(this.contentResolver,
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
            .setContentValues(contentValues)
            .build()

        val outputFile = File(imagePath, imageName)
        val option2 = FileOutputOptions.Builder(outputFile).build()

        // 2. Configure Recorder and Start recording to the mediaStoreOutput.
        val recording = if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            newVideoCapture!!.output
                .prepareRecording(this@ScanActivity, option2)
                .withAudioEnabled()
                .start(ContextCompat.getMainExecutor(this), this@ScanActivity)
        } else {
            null
        }

        // Must unbind the use-cases before rebinding them
        cameraProvider.unbindAll()

        try {
            if (guidelinesEnable) {
                val viewPort: ViewPort = ViewPort.Builder(
                    Rational(
                        scanActivityBinding.cameraPreview.width,
                        scanActivityBinding.cameraPreview.height
                    ),
                    scanActivityBinding.cameraPreview.display.rotation
                ).setScaleType(ViewPort.FILL_CENTER).build()

                val useCaseGroupBuilder: UseCaseGroup.Builder = UseCaseGroup.Builder().setViewPort(
                    viewPort
                )

                useCaseGroupBuilder.addUseCase(mPreview!!)
                useCaseGroupBuilder.addUseCase(mImageCapture!!)
                //useCaseGroupBuilder.addUseCase(mImageAnalyzer!!)

                mCamera = cameraProvider.bindToLifecycle(
                    this,
                    cameraSelector,
                    useCaseGroupBuilder.build()
                )
            }else {
                mCamera = if (getCaptureType() == "Video") {
                    cameraProvider.bindToLifecycle(
                        this,
                        cameraSelector,
                        mPreview,
                        newVideoCapture
                    )
                } else {
                    cameraProvider.bindToLifecycle(
                        this,
                        cameraSelector,
                        mPreview,
                        mImageCapture
                    )
                }
            }
            if (getCaptureType() == "Video") {
                when (mFlashMode) {
                    ImageCapture.FLASH_MODE_OFF -> {
                        mCamera?.cameraControl?.enableTorch(false)
                    }
                    ImageCapture.FLASH_MODE_ON -> {
                        mCamera?.cameraControl?.enableTorch(true)
                    }
                    ImageCapture.FLASH_MODE_AUTO -> {
                        mCamera?.cameraControl?.enableTorch(true)
                    }
                }
            }
            // Attach the viewfinder's surface provider to preview use case
            mPreview?.setSurfaceProvider(scanActivityBinding.cameraPreview.surfaceProvider)
        } catch (exc: Exception) {
            Log.e(TAG, "Use case binding failed", exc)
        }

        try{
            scanActivityBinding.cameraPreview.afterMeasured {
                scanActivityBinding.cameraPreview.setOnTouchListener { _, event ->
                    return@setOnTouchListener when (event.action) {
                        MotionEvent.ACTION_DOWN -> {
                            true
                        }
                        MotionEvent.ACTION_UP -> {
                            val factory: MeteringPointFactory = SurfaceOrientedMeteringPointFactory(
                                scanActivityBinding.cameraPreview.width.toFloat(), scanActivityBinding.cameraPreview.height.toFloat()
                            )
                            val autoFocusPoint = factory.createPoint(event.x, event.y)
                            try {
                                mCamera?.cameraControl?.startFocusAndMetering(
                                    FocusMeteringAction.Builder(
                                        autoFocusPoint,
                                        FocusMeteringAction.FLAG_AF
                                    ).apply {
                                        //focus only when the user tap the preview
                                        disableAutoCancel()
                                    }.build()
                                )
                            } catch (e: CameraInfoUnavailableException) {
                                Log.d("ERROR", "cannot access camera", e)
                            }
                            true
                        }
                        else -> false // Unhandled event.
                    }
                }
            }
        }catch (e: Exception){
            e.printStackTrace()
        }
    }

    inline fun View.afterMeasured(crossinline block: () -> Unit) {
        if (measuredWidth > 0 && measuredHeight > 0) {
            block()
        } else {
            viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    if (measuredWidth > 0 && measuredHeight > 0) {
                        viewTreeObserver.removeOnGlobalLayoutListener(this)
                        block()
                    }
                }
            })
        }
    }

    private fun buildUi() {
        if (!hasFlashMode())
            scanActivityBinding.flashToggle.visibility = View.INVISIBLE
        if (!hasFrontCamera())
            scanActivityBinding.rotateCamera.visibility = View.INVISIBLE
    }

    private fun shootSound(what: String){
        val audio = getSystemService(AUDIO_SERVICE) as AudioManager
        when (audio.ringerMode) {
            AudioManager.RINGER_MODE_NORMAL -> {
                val sound = MediaActionSound()
                when (what) {
                    START_VIDEO -> sound.play(MediaActionSound.START_VIDEO_RECORDING)
                    STOP_VIDEO -> sound.play(MediaActionSound.STOP_VIDEO_RECORDING)
                    else -> sound.play(MediaActionSound.SHUTTER_CLICK)
                }
            }
            AudioManager.RINGER_MODE_SILENT -> {
            }
            AudioManager.RINGER_MODE_VIBRATE -> {
            }
        }
    }

    private fun startProgress(boolean: Boolean){
        if (boolean){
            scanActivityBinding.progressLayout.visibility = View.VISIBLE
            scanActivityBinding.progressBar.visibility = View.VISIBLE
            window.setFlags(
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
        }else{
            scanActivityBinding.progressLayout.visibility = View.GONE
            scanActivityBinding.progressBar.visibility = View.GONE
            window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
        }
    }

    private fun takePhoto() {
        startProgress(true)
        shootSound(CLICK_PHOTO)
        if (isTakingVideo) return
        // Get a stable reference of the modifiable image capture use case
        val imageCapture = mImageCapture ?: return

        // Create timestamped output file to hold the image
        val fileName = SimpleDateFormat(FILENAME_FORMAT, Locale.US)
            .format(System.currentTimeMillis()) + ".jpg"
        //val photoFile = File(mOutputDirectory, fileName)
        Utils.printMessage("My Directory:- $imagePath")
        val photoFile = if (!imagePath.contains(".jpg")) {
            File(imagePath, imageName)
        }else{
            File(imagePath)
        }
        imagePath = photoFile.toString()
        Utils.printMessage("My Directory:- $imagePath")

        // Setup image capture metadata
        val metadata = ImageCapture.Metadata().apply {
            // Mirror image when using the front camera
            isReversedHorizontal = mLensFacing == CameraSelector.LENS_FACING_FRONT
        }

        // Create output options object which contains file + metadata
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile)
            .apply {
                setMetadata(metadata)
            }.build()

        // Setup image capture listener which is triggered after photo has been taken
        imageCapture.takePicture(
            outputOptions,
            mCameraExecutor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    val msg = "Photo capture failed: ${exc.message}"
                    Toast.makeText(this@ScanActivity, msg, Toast.LENGTH_SHORT).show()
                    Log.e(TAG, msg, exc)
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    saveBitmapToFile(photoFile)

                    //Flip Image Fix
                    if (mLensFacing == CameraSelector.LENS_FACING_FRONT) {
                        val flipBitmap = flipImage(Utils.getBitmapFromPath(photoFile.absolutePath)!!, FLIP_HORIZONTAL)
                        if (flipBitmap != null) {
                            Utils.saveBitmap(flipBitmap, photoFile.absolutePath)
                        }
                    }

                    //Rotation Fix
                    if (guidelinesEnable) {
                        /*val rotatedBitmap =
                            fixOrientationGuideline(Utils.getBitmapFromPath(photoFile.absolutePath)!!)
                        if (rotatedBitmap != null) {
                            Utils.saveBitmap(rotatedBitmap, photoFile.absolutePath)
                        }*/
                    } else {
                        val rotatedBitmap =
                            fixOrientation(Utils.getBitmapFromPath(photoFile.absolutePath)!!)
                        if (rotatedBitmap != null) {
                            Utils.saveBitmap(rotatedBitmap, photoFile.absolutePath)
                        }
                    }

                    /*if (isImageBlurry(Utils.getBitmapFromPath(photoFile.absolutePath)!!)){
                        Toast.makeText(this@ScanActivity, "Image is blurry", Toast.LENGTH_SHORT).show()
                    }*/

                    val savedUri = Uri.fromFile(photoFile)
                    val tempPath = File(getPath(), "ORIGINAL-$imageName")
                    Utils.deleteFile(tempPath.toString().toUri())
                    val originalCopy = photoFile.copyTo(tempPath)
                    Utils.printMessage("My PATH $originalCopy")
                    originalImagePath = originalCopy.toString()
                    val msg = "Photo capture succeeded: $savedUri"
                    Log.d(TAG, msg)
                    scanActivityBinding.content.visibility = View.VISIBLE
                    scanActivityBinding.controlLayout.visibility = View.GONE //hide Linearlayout

                    sendImageToProcess()
                    startProgress(false)
                }
            })
    }

    private fun fixOrientation(mBitmap: Bitmap): Bitmap? {
        Utils.printMessage("Image Rotation:- ${mBitmap.width}, ${mBitmap.height}")
        return if (mBitmap.width > mBitmap.height) {
            val matrix = Matrix()
            matrix.postRotate(90f)
            Bitmap.createBitmap(
                mBitmap,
                0,
                0,
                mBitmap.width,
                mBitmap.height,
                matrix,
                true
            )
        }else{
            null
        }
    }

    @Throws(IOException::class)
    fun rotateImageIfRequired(img: Bitmap, selectedImage: Uri): Bitmap? {
        val ei = ExifInterface(selectedImage.path!!)
        Utils.printMessage("Rotation Is ${ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)}")
        return when (ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
            ExifInterface.ORIENTATION_ROTATE_90 -> rotateImage(img, 90)
            ExifInterface.ORIENTATION_ROTATE_180 -> rotateImage(img, 180)
            ExifInterface.ORIENTATION_ROTATE_270 -> rotateImage(img, 270)
            ExifInterface.ORIENTATION_TRANSVERSE -> rotateImage(img, 270)
            else -> img
        }
    }
    private fun rotateImage(img: Bitmap, degree: Int): Bitmap? {
        val matrix = Matrix()
        matrix.postRotate(degree.toFloat())
        val rotatedImg = Bitmap.createBitmap(img, 0, 0, img.width, img.height, matrix, true)
        img.recycle()
        return rotatedImg
    }

    private fun fixOrientationGuideline(mBitmap: Bitmap): Bitmap? {
        Utils.printMessage("Image Rotation:- ${mBitmap.width}, ${mBitmap.height}")
        return if (mBitmap.width < mBitmap.height) {
            val matrix = Matrix()
            matrix.postRotate(90f)
            Bitmap.createBitmap(
                mBitmap,
                0,
                0,
                mBitmap.width,
                mBitmap.height,
                matrix,
                true
            )
        }else{
            null
        }
    }

    private fun flipImage(src: Bitmap, type: Int): Bitmap? {
        // create new matrix for transformation
        val matrix = Matrix()
        // if vertical
        when (type) {
            FLIP_VERTICAL -> {
                matrix.preScale(1.0f, -1.0f)
            }
            FLIP_HORIZONTAL -> {
                matrix.preScale(-1.0f, 1.0f)
            }
            else -> {
                return null
            }
        }

        return Bitmap.createBitmap(src, 0, 0, src.width, src.height, matrix, true)
    }

    private fun isImageBlurry(imageBitmap: Bitmap): Boolean {
        startProgress(true)

        val grayBitmap = toGrayscale(imageBitmap)

        val laplacian = getLaplacian(grayBitmap)

        val variance = getVariance(laplacian)

        Utils.printMessage("variance =-= $variance")

        val threshold = 18.0

        startProgress(false)
        return variance < threshold
    }

    private fun toGrayscale(imageBitmap: Bitmap): Bitmap {
        val width = imageBitmap.width
        val height = imageBitmap.height

        val grayBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)

        val canvas = Canvas(grayBitmap)
        val paint = Paint().apply {
            colorFilter = ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(0f) })
        }

        canvas.drawBitmap(imageBitmap, 0f, 0f, paint)

        return grayBitmap
    }

    private fun getLaplacian(imageBitmap: Bitmap): Array<IntArray> {
        val width = imageBitmap.width
        val height = imageBitmap.height

        val laplacian = Array(height) { IntArray(width) }

        val grayscale = Array(height) { IntArray(width) }
        for (y in 0 until height) {
            for (x in 0 until width) {
                grayscale[y][x] = Color.red(imageBitmap.getPixel(x, y))
            }
        }

        for (y in 1 until height - 1) {
            for (x in 1 until width - 1) {
                laplacian[y][x] = grayscale[y - 1][x] + grayscale[y + 1][x] +
                        grayscale[y][x - 1] + grayscale[y][x + 1] -
                        4 * grayscale[y][x]
            }
        }

        return laplacian
    }

    private fun getVariance(matrix: Array<IntArray>): Double {
        val height = matrix.size
        val width = matrix[0].size

        var sum = 0
        for (y in 0 until height) {
            for (x in 0 until width) {
                sum += matrix[y][x]
            }
        }

        val mean = sum.toDouble() / (height * width)

        var variance = 0.0
        for (y in 0 until height) {
            for (x in 0 until width) {
                val diff = matrix[y][x] - mean
                variance += diff.pow(2)
            }
        }
        variance /= (height * width)

        return sqrt(variance)
    }


    fun saveBitmapToFile(file: File): File? {
        return try {

            // BitmapFactory options to downsize the image
            val o = BitmapFactory.Options()
            o.inJustDecodeBounds = true
            o.inSampleSize = 6
            // factor of downsizing the image
            var inputStream = FileInputStream(file)
            //Bitmap selectedBitmap = null;
            BitmapFactory.decodeStream(inputStream, null, o)
            inputStream.close()

            // The new size we want to scale to
            val REQUIRED_SIZE = 75

            // Find the correct scale value. It should be the power of 2.
            var scale = 1
            while (o.outWidth / scale / 2 >= REQUIRED_SIZE && o.outHeight / scale / 2 >= REQUIRED_SIZE) {
                scale *= 2
            }
            val o2 = BitmapFactory.Options()
            o2.inSampleSize = scale
            inputStream = FileInputStream(file)
            var selectedBitmap = BitmapFactory.decodeStream(inputStream, null, o2)
            inputStream.close()

            // rotate image if required
            try {
                selectedBitmap = rotateImageIfRequired(selectedBitmap!!, Uri.parse(imagePath))
            } catch (e: Exception) {
                Utils.printMessage("Rotate Exception : ${e.message}")
            }

            // here i override the original image file
            file.createNewFile()
            val outputStream = FileOutputStream(file)
            selectedBitmap!!.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    @SuppressLint("RestrictedApi")
    private fun startVideo() {
        if (isTakingVideo) return
        shootSound(START_VIDEO)
        // Get a stable reference of the modifiable video capture use case
        val videoCapture = newVideoCapture ?: return

        // Create timestamped output file to hold the image

        val recording1: Recording? = recording
        if (recording1 != null) {
            recording1.stop()
            recording = null
            return
        }
        /*val name = SimpleDateFormat(
            "yyyy-MM-dd-HH-mm-ss-SSS",
            Locale.getDefault()
        ).format(System.currentTimeMillis())*/
        val contentValues = ContentValues()
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, imageName)
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
        contentValues.put(MediaStore.Video.Media.RELATIVE_PATH, imagePath)

        val options = MediaStoreOutputOptions.Builder(
            contentResolver,
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        )
            .setContentValues(contentValues).build()

        val outputFile = File(imagePath, imageName)
        val option2 = FileOutputOptions.Builder(outputFile).build()

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        recording =
            videoCapture.output.prepareRecording(this@ScanActivity, option2).withAudioEnabled()
                .start(ContextCompat.getMainExecutor(this@ScanActivity),
                    Consumer<VideoRecordEvent> { videoRecordEvent: VideoRecordEvent? ->
                        /*if (videoRecordEvent is VideoRecordEvent.Start) {
                            //capture.setEnabled(true)
                        } else */
                        if (videoRecordEvent is Finalize) {
                            if (!videoRecordEvent.hasError()) {

                                try {
                                    val savedUri = videoRecordEvent.outputResults.outputUri
                                    val msg = "Video capture succeeded: $savedUri"
                                    val imageData = ImageData()
                                    if (savedUri.path != null) {
                                        imageData.imagePath = savedUri.path!!
                                    }
                                    if (location != null) {
                                        imageData.lat = location.latitude.toString()
                                        imageData.lng = location.longitude.toString()
                                        imageData.accuracy = location.accuracy.toString()
                                    }
                                    imagePathList.add(imageData)
                                    createJSONDataONSuccess(100, "Success", "", imagePathList, "")
                                    //Toast.makeText(this@ScanActivity, msg, Toast.LENGTH_SHORT).show()
                                    Utils.printMessage("Video: $msg")
                                    //Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
                                    //Log.d(TAG, msg)
                                    finish()
                                } catch (e: Exception) {
                                    Utils.printMessage("Recording Error 1 ${e.message}")
                                }

                            } else {
                                try {
                                    recording!!.close()
                                    recording = null
                                    val msg = "Video capture failed: " + videoRecordEvent.error
                                    createJSONDataONERROR(
                                        101,
                                        baseContext.resources.getString(R.string.video_error)
                                    )
                                    //Toast.makeText(this@ScanActivity, msg, Toast.LENGTH_SHORT).show()
                                    Log.d(TAG, msg)
                                } catch (e: Exception) {
                                    Utils.printMessage("Recording Error 2 ${e.message}")
                                }
                            }
                        }
                    })


        /* val fileName = SimpleDateFormat(FILENAME_FORMAT, Locale.US)
             .format(System.currentTimeMillis()) + ".mp4"
         val videoFile = File(imagePath, imageName)

         val videOutputOptions = VideoCapture.OutputFileOptions.Builder(videoFile)
             .apply {
                 //setMetadata(metadata)
             }.build()

         if (ActivityCompat.checkSelfPermission(
                 this,
                 Manifest.permission.RECORD_AUDIO
             ) != PackageManager.PERMISSION_GRANTED
         ) {
             // TODO: Consider calling
             //    ActivityCompat#requestPermissions
             // here to request the missing permissions, and then overriding
             //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
             //                                          int[] grantResults)
             // to handle the case where the user grants the permission. See the documentation
             // for ActivityCompat#requestPermissions for more details.
             return
         }
         videoCapture.startRecording(videOutputOptions,
             mCameraExecutor,
             object : VideoCapture.OnVideoSavedCallback {

                 override fun onVideoSaved(outputFileResults: VideoCapture.OutputFileResults) {
                     val savedUri = Uri.fromFile(videoFile)
                     val msg = "Video capture succeeded: $savedUri"
                     val imageData = ImageData()
                     if (savedUri.path != null) {
                         imageData.imagePath = savedUri.path!!
                     }
                     if (location != null) {
                         imageData.lat = location.latitude.toString()
                         imageData.lng = location.longitude.toString()
                         imageData.accuracy = location.accuracy.toString()
                     }
                     imagePathList.add(imageData)
                     createJSONDataONSuccess(100, "Success", "", imagePathList, "")
                     //Toast.makeText(this@ScanActivity, msg, Toast.LENGTH_SHORT).show()
                     Log.d(TAG, msg)
                     finish()
                 }

                 override fun onError(videoCaptureError: Int, message: String, cause: Throwable?) {
                     val msg = "Video capture failed: ${cause?.message}"
                     createJSONDataONERROR(
                         101,
                         baseContext.resources.getString(R.string.video_error)
                     )
                     //Toast.makeText(this@ScanActivity, msg, Toast.LENGTH_SHORT).show()
                     Log.e(TAG, msg, cause)
                 }
             })*/
        isTakingVideo = true
        recordingStarted()
        startTimer()
    }

    @SuppressLint("RestrictedApi")
    private fun stopVideo() {
        if (!isTakingVideo) return
        shootSound(STOP_VIDEO)
        // Get a stable reference of the modifiable video capture use case
        val videoCapture = newVideoCapture ?: return
        recording!!.stop()
        recordingStop()
        //stopTimer()
        stopChronometer()
        isTakingVideo = false
    }

    private fun recordingStarted() {
        //resetTimer()
        scanActivityBinding.captureVideo.setImageDrawable(ResourcesCompat.getDrawable(resources, R.drawable.ic_outline_stop_circle, null))
        scanActivityBinding.capturePause.setImageDrawable(ResourcesCompat.getDrawable(resources, R.drawable.ic_round_pause_circle, null))
        scanActivityBinding.capturePause.visibility = View.VISIBLE
        scanActivityBinding.timer.visibility = View.GONE
        scanActivityBinding.flashToggle.visibility = View.INVISIBLE
        scanActivityBinding.rotateCamera.visibility = View.INVISIBLE
        //tipText.visibility = View.INVISIBLE
    }

    private fun recordingStop() {
        scanActivityBinding.flashToggle.visibility = View.VISIBLE
        scanActivityBinding.captureVideo.setImageDrawable(ResourcesCompat.getDrawable(resources, R.drawable.ic_circle_red_white_24dp, null))
        scanActivityBinding.capturePause.setImageDrawable(ResourcesCompat.getDrawable(resources, R.drawable.ic_round_pause_circle, null))
        scanActivityBinding.capturePause.visibility = View.GONE
        scanActivityBinding.rotateCamera.visibility = View.VISIBLE
        //tipText.visibility = View.VISIBLE
        scanActivityBinding.timer.visibility = View.GONE
        //dot_text.clearAnimation()
    }

    fun resetTimer() {
        timeInMilliseconds = 0L
        startHTime = SystemClock.uptimeMillis()
        scanActivityBinding.timerText.text = String.format("%02d", 0).plus(":" + String.format("%02d", 0))
    }

    var value: Long = 0L
    private val updateTimerThread = object : Runnable {

        override fun run() {
            //Utils.printMessage("Timer is: $isVideoPause")
            if (!isVideoPause) {
                timeInMilliseconds = if (value == 0L) SystemClock.uptimeMillis() else value - startHTime
                scanActivityBinding.timerText.text = timeInMilliseconds.calculateDuration()
                value = SystemClock.uptimeMillis()
            }
            customHandler.postDelayed(this, 0)
        }
    }

    private fun Long.calculateDuration(): String {
        return String.format("%02d", TimeUnit.MILLISECONDS.toMinutes(this)).plus(
            ":" + String.format(
                "%02d", TimeUnit.MILLISECONDS.toSeconds(this) - TimeUnit.MINUTES.toSeconds(
                    TimeUnit.MILLISECONDS.toMinutes(this)
                )
            )
        )
    }

    fun pauseVideo() {
        customHandler.removeCallbacks(updateTimerThread)
    }

    private fun startTimer() {
        //customHandler.postDelayed(updateTimerThread, 0)
        //startChronometer()
        /*val timer = object: CountDownTimer(20000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                Utils.printMessage("seconds remaining: " + millisUntilFinished / 1000)
            }

            override fun onFinish() {
                if(isTakingVideo){
                    stopVideo()
                }
            }
        }
        timer.start()*/
        if (countDownTimerWithPause.isTimerRunning()) countDownTimerWithPause.stopCountDownTimer()

        countDownTimerWithPause.startCountDownTimer()
        scanActivityBinding.layoutTimer.visibility = View.VISIBLE
    }

    private fun stopTimer() {
        customHandler.removeCallbacks(updateTimerThread)
    }


    private fun startChronometer() {
        scanActivityBinding.layoutTimer.visibility = View.VISIBLE
        val startTime = SystemClock.elapsedRealtime()
        scanActivityBinding.textChrono.onChronometerTickListener = OnChronometerTickListener {
            var countUp = (SystemClock.elapsedRealtime() - startTime) / 1000
            if (countUp % 2 == 0L) {
                scanActivityBinding.ivRecordImage.visibility = View.VISIBLE
            } else {
                scanActivityBinding.ivRecordImage.visibility = View.INVISIBLE
            }
            val min: Int = (countUp / 60).toInt()
            val sec: Int = (countUp % 60).toInt()
            Utils.printMessage("$min:$sec")
            val newCount: Int = MAX_VIDEO_TIME - countUp.toInt()
            val asText = (String.format("%02d", newCount / 60) + ":"
                    + String.format("%02d", newCount % 60))
            scanActivityBinding.textChrono.text = asText
            if (countUp > MAX_VIDEO_TIME) {
                stopVideo()
            }
        }
        scanActivityBinding.textChrono.start()
    }

    private fun stopChronometer() {
        if (countDownTimerWithPause.isTimerRunning()) countDownTimerWithPause.stopCountDownTimer()
        scanActivityBinding.textChrono.stop()
        scanActivityBinding.layoutTimer.visibility = View.INVISIBLE
    }




    companion object {
        private const val TAG = "CameraFragment"
        private const val RATIO_4_3_VALUE = 4.0 / 3.0
        private const val RATIO_16_9_VALUE = 16.0 / 9.0
        private const val DELAY_MILLIS = 2000L
        private const val SCALE_UP = 1.5f
        private const val SCALE_DOWN = 1.0f
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val NO_FLASH = -1111
        private val REQUIRED_PERMISSIONS = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
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
        private const val REQUEST_CODE_PERMISSIONS = 10
        fun getOutputDirectory(context: Context): File {
            val appContext = context.applicationContext
            val mediaDir = context.externalMediaDirs.firstOrNull()?.let {
                File(it, appContext.resources.getString(R.string.app_name)).apply { mkdirs() } }
            return if (mediaDir != null && mediaDir.exists())
                mediaDir else appContext.filesDir
        }
    }

    fun createJSONData(status: Int, code: Int, result: String?) {
        val jsonObject = JSONObject()
        when (status) {
            SUCCESS -> try {
                jsonObject.put(CODE, code)
                jsonObject.put(MESSAGE, result)
            } catch (e: JSONException) {
                e.printStackTrace()
            }
            ERROR -> try {
                jsonObject.put(CODE, code)
                jsonObject.put(MESSAGE, result)
            } catch (e: JSONException) {
                e.printStackTrace()
            }
        }
        scanActivityBinding.progressLayout.visibility = View.GONE
        scanActivityBinding.progressBar.visibility = View.GONE
        val intent = Intent()
        intent.putExtra(SCANNED_RESULT, jsonObject.toString())
        setResult(RESULT_OK, intent)
        finish()
    }

    fun createJSONDataONERROR(code: Int, message: String?) {
        val jsonObject = JSONObject()
        try {
            jsonObject.put(CODE, code)
            jsonObject.put(MESSAGE, message)
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        val result = Intent()
        result.putExtra(SCANNED_RESULT, jsonObject.toString())
        setResult(RESULT_OK, result)
        finish()
    }

    fun createJSONDataONSuccess(code: Int, message: String?, imageProcessPath: String?, imageDataList: ArrayList<ImageData>?, pdfPath: String?) {
        val jsonObject = JSONObject()
        try {
            jsonObject.put(CODE, code)
            jsonObject.put(MESSAGE, message)
            val jsonArray = JSONArray()
            if (isImageProcess()) {
                jsonObject.put(IMAGE_PATH, imageProcessPath)
            } else {
                if (imageDataList != null) {
                    for (i in imageDataList.indices) {
                        val jsonObject1 = JSONObject()
                        jsonObject1.put(IMAGE_PATH, imageDataList[i].imagePath)
                        jsonObject1.put(LAT, imageDataList[i].lat)
                        jsonObject1.put(LNG, imageDataList[i].lng)
                        jsonObject1.put(ACCURACY, imageDataList[i].accuracy)
                        jsonArray.put(jsonObject1)
                    }
                }
                if (getImageCopy()){
                    jsonObject.put(IMAGE_PATH, imageProcessPath)
                }
                jsonObject.put(IMAGE_DATA, jsonArray)
                jsonObject.put(PDF_PATH, pdfPath)
            }
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        val result = Intent()
        result.putExtra(SCANNED_RESULT, jsonObject.toString())
        setResult(RESULT_OK, result)
        finish()
    }

    override fun getLatLng(location: Location?) {
        this.location = Location("ImagesLocation")
        if (location != null) {
            this.location.latitude = location.latitude
            this.location.accuracy = location.accuracy
            this.location.longitude = location.longitude
        }
    }

    private fun sendImageToProcess() {
        //closeCamera()
        setScanImage(Utils.getBitmapFromPath(imagePath))
        if (isPanOcr()){
            panOcrExecutor(getScanImage())
        }else {
            onScanFinish()
        }
    }

    private fun generatePanOcr(bitmap: Bitmap?): String {
        val textRecognizer = TextRecognizer.Builder(this).build()
        var pan = ""
        val jsonObject = JSONObject()
        if (!textRecognizer.isOperational) {
            Log.e("Main Activity", "Dependencies not available")

            // Check android for low storage so dependencies can be loaded, DEPRICATED CHANGE LATER
            val intentLowStorage = IntentFilter(Intent.ACTION_DEVICE_STORAGE_LOW)
            val hasLowStorage = registerReceiver(null, intentLowStorage) != null
            if (hasLowStorage) {
                ResultFragment.scanActivity.createJSONData(ERROR, 102, "Low Memory On Disk")
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
            for (textBlock in blocks) {
                run {
                    val textLines =
                        textBlock!!.components

                    //loop Through each `Line`
                    for (currentLine in textLines) {
                        val words: MutableList<out com.google.android.gms.vision.text.Text?>? = currentLine.components
                        println("currentword.getValue() " + words!!.size)
                        //Loop through each `Word`
                        for (currentWord in words) {
                            println("currentWord " + currentWord!!.value)
                            //if (ocr_type.equals("pan", ignoreCase = true)) {
                            if (currentWord.value.length == 10) {
                                if (Utils.isValidPanNumber(currentWord.value)) {
                                    val str: String = currentWord.value
                                    println("currentWord 10 " + currentWord.value)
                                    pan = currentWord.value
                                    jsonObject.put("PanNo",pan)
                                }else if(Utils.isValidPanDOB(currentWord.value)){
                                    val str: String = currentWord.value
                                    println("currentWord 10 " + currentWord.value)
                                    jsonObject.put("PanDOB",str)
                                } else if(Utils.isValidPanName(currentWord.value)){
                                    val str: String = currentWord.value
                                    println("currentWord 10 " + currentWord.value)
                                    jsonObject.put("PanName",str)
                                }
                                jsonObject.put("image_path", imagePath)
                                jsonObject.put(LAT, location.latitude.toString())
                                jsonObject.put(LNG, location.longitude.toString())
                                jsonObject.put(ACCURACY, location.accuracy.toString())
                            }
                            //}
                        }
                    }
                }
            }
        }
        return jsonObject.toString()
    }

    private fun panOcrExecutor(bitmap: Bitmap?){
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
            var resultPanOcr = generatePanOcr(bitmap)
            Utils.printMessage("PAN OCR: $resultPanOcr")


            handler.post {
                /*
                * You can perform any operation that
                * requires UI Thread here.
                *
                * its like onPostExecute()
                * */
                //return the ocrresult
                Utils.printMessage("PAN OCR: $resultPanOcr")
                val jsonObject = JSONObject(resultPanOcr)
                var tempStr = ""
                if (jsonObject.has("PanNo")){
                    tempStr = jsonObject.getString("PanNo")
                }
                if (tempStr.length == 10) {
                    createJSONData(SUCCESS, 100, resultPanOcr)
                } else {
                    createJSONData(ERROR, 101, "Unable to Scan")
                }

            }
        }

    }

    fun onBrightnessFinish(bitmap: Bitmap?) {
        try {
            setTransformed(bitmap)
            Utils.printMessage("Brightness Changed:- ${bitmap.toString()}, $imagePath")
            Utils.saveBitmap(bitmap!!, imagePath)
            //saveBitmapToFile(File(imagePath))
            if (isAadharMasking()){
                setAadharMasking(false)
            }
            fragmentManagement(RESULT_FRAGMENT, REPLACE, true, null)
        }catch (e: Exception){
            e.printStackTrace()
        }
    }


    private fun onScanFinish() {
        val bundle = Bundle()
        bundle.putDouble("pass_degree", pass_degree)
        fragmentManagement(RESULT_FRAGMENT, REPLACE, true, bundle)
    }

    fun changeBrightness() {
        fragmentManagement(BRIGHTNESS_FRAGMENT, REPLACE, true, null)
    }

    fun generateOcr(bitmap: Bitmap?): String {
        val textRecognizer = TextRecognizer.Builder(this).build()
        var pan = ""
        if (!textRecognizer.isOperational) {
            Log.e("Main Activity", "Dependencies not available")

            // Check android for low storage so dependencies can be loaded, DEPRICATED CHANGE LATER
            val intentLowStorage = IntentFilter(Intent.ACTION_DEVICE_STORAGE_LOW)
            val hasLowStorage = registerReceiver(null, intentLowStorage) != null
            if (hasLowStorage) {
                createJSONData(ERROR, 102, "Low Memory On Disk")
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
                        for (currentword in words!!) {
                            println("currentword " + currentword!!.value)
                            //if (ocr_type.equals("pan", ignoreCase = true)) {
                            if (currentword.value.length == 10) {
                                if (Utils.isValidPanNumber(currentword.value)) {
                                    val str: String = currentword.value
                                    println("currentword 10 " + currentword.value)
                                    pan = currentword.value
                                }
                            }
                            //}
                        }
                    }
                }
            }
        }
        return pan
    }



    private fun fragmentManagement(Tag: String, addReplace: String, addToBackstack: Boolean, bundle: Bundle?) {
        FM = supportFragmentManager
        ft = FM!!.beginTransaction()
        last_tag = current_tag
        current_tag = Tag
        if (FM!!.findFragmentByTag(Tag) == null) {
            Utils.printMessage("My Fragment:- $Tag")
            when (Tag) {
                /*AUTO_CROP_FRAGMENT -> {
                    autoCropFragment = AutoCropFragment()
                    ReplacingFragment = autoCropFragment
                }*/
                RESULT_FRAGMENT -> {
                    resultFragment = ResultFragment()
                    ReplacingFragment = resultFragment
                }
                BRIGHTNESS_FRAGMENT -> {
                    brightnessFragment = BrightnessFragment()
                    ReplacingFragment = brightnessFragment
                }
            }

            val ft = FM!!.beginTransaction()
            if (bundle != null) {
                ReplacingFragment!!.arguments = bundle
            }
            if (addReplace == ADD) {
                ft.add(R.id.content, ReplacingFragment!!, current_tag)
            } else {
                ft.replace(R.id.content, ReplacingFragment!!, current_tag)
            }

            if (addToBackstack) {
                ft.addToBackStack(current_tag)
            }
            try {
                ft.commitAllowingStateLoss()
            } catch (e: Exception) {
                e.printStackTrace()
                try {
                    ft.commitAllowingStateLoss()
                } catch (ex: Exception) {
                    ex.printStackTrace()
                }
            }
        } else {
            current_tag = Tag
            FM!!.popBackStack(Tag, 0)
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        Utils.printMessage("Current Tag:- $current_tag")
        if (current_tag == RESULT_FRAGMENT) {
            if (count > 1) {
                scanActivityBinding.scanImageLayout.visibility = View.VISIBLE
                scanActivityBinding.scanImageCount.text = (captureCount + 1).toString() + "/" + count + "Images"
            } else {
                scanActivityBinding.scanImageLayout.visibility = View.GONE
            }
            setTransformed(null)

            if(count < 2) {
                for (item in ResultFragment.scanActivity.imagePathList) {
                    Utils.printMessage("Delete File :- ${item.imagePath.toUri()}")
                    Utils.deleteFile(item.imagePath.toUri())
                }
                val tempPath = File(
                    ResultFragment.scanActivity.getPath(),
                    "${ResultFragment.scanActivity.imageName}"
                )
                Utils.deleteFile(tempPath.toString().toUri())
                val tempPath2 = File(
                    ResultFragment.scanActivity.getPath(),
                    "ORIGINAL-${ResultFragment.scanActivity.imageName}"
                )
                Utils.deleteFile(tempPath2.toString().toUri())
            }

        }
        current_tag = last_tag
        val checkFragment = supportFragmentManager.findFragmentById(R.id.content)
        if(checkFragment != null){
            scanActivityBinding.controlLayout.visibility = View.GONE //hide Linearlayout
        }else{
            scanActivityBinding.controlLayout.visibility = View.VISIBLE
        }

    }

    override fun accept(t: VideoRecordEvent?) {

    }


    override fun onResume() {
        super.onResume()

        mSensorManager!!.registerListener(
            this, mSensorManager!!.getDefaultSensor(Sensor.TYPE_ORIENTATION),
            SensorManager.SENSOR_DELAY_GAME
        )
    }

    override fun onPause() {
        super.onPause()

        mSensorManager!!.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {

        val degree = Math.round(event.values[0]).toFloat()
        pass_degree = degree.toDouble()
//        scanActivityBinding.tvDegree.setText("Heading: " + java.lang.Float.toString(degree) + " degrees")

        val ra = RotateAnimation(
            currentDegree,
            -degree,
            Animation.RELATIVE_TO_SELF, 0.5f,
            Animation.RELATIVE_TO_SELF,
            0.5f
        )

        ra.duration = 210

        ra.fillAfter = true

        if(isAddLocation()){
            scanActivityBinding.imgCompassss.startAnimation(ra)
            scanActivityBinding.imgCompassss.visibility = VISIBLE
        }else{
            scanActivityBinding.imgCompassss.visibility = GONE
        }

        currentDegree = -degree
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
    }

}