package com.lib.gocam.activity

/*import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.graphics.image.JPEGFactory
import com.tom_roush.pdfbox.pdmodel.graphics.image.PDImageXObject*/

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.pdf.PdfRenderer
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import android.text.TextUtils
import android.util.Log
import android.util.SparseArray
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.google.android.gms.vision.Frame
import com.google.android.gms.vision.text.TextBlock
import com.google.android.gms.vision.text.TextRecognizer
import com.itextpdf.text.*
import com.itextpdf.text.pdf.*
import com.lib.gocam.R
import com.lib.gocam.databinding.PickerActivityBinding
import com.lib.gocam.latlng.LatLngModule
import com.lib.gocam.latlng.LatLngRead
import com.lib.gocam.model.ImageData
import com.lib.gocam.utility.PdfUtility
import com.lib.gocam.utility.ScanConstants
import com.lib.gocam.utility.ScanConstants.Companion.ACCURACY
import com.lib.gocam.utility.ScanConstants.Companion.CODE
import com.lib.gocam.utility.ScanConstants.Companion.FILE_TYPE
import com.lib.gocam.utility.ScanConstants.Companion.IMAGE
import com.lib.gocam.utility.ScanConstants.Companion.IMAGE_COUNT
import com.lib.gocam.utility.ScanConstants.Companion.IMAGE_DATA
import com.lib.gocam.utility.ScanConstants.Companion.IMAGE_NAME
import com.lib.gocam.utility.ScanConstants.Companion.IMAGE_PATH
import com.lib.gocam.utility.ScanConstants.Companion.IS_UPLOAD_AADHAR
import com.lib.gocam.utility.ScanConstants.Companion.LAT
import com.lib.gocam.utility.ScanConstants.Companion.LNG
import com.lib.gocam.utility.ScanConstants.Companion.MESSAGE
import com.lib.gocam.utility.ScanConstants.Companion.PDF
import com.lib.gocam.utility.ScanConstants.Companion.PDF_CREATE
import com.lib.gocam.utility.ScanConstants.Companion.PDF_NAME
import com.lib.gocam.utility.ScanConstants.Companion.PDF_PATH
import com.lib.gocam.utility.ScanConstants.Companion.SCANNED_RESULT
import com.lib.gocam.utility.Utils
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.*
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors
import kotlin.collections.ArrayList
import kotlin.math.roundToInt


class PickerActivity : AppCompatActivity(), LatLngRead {

    lateinit var pickerActivityBinding: PickerActivityBinding

    lateinit var latLngModule: LatLngModule
    lateinit var location: Location

    var PICK_IMAGE_MULTIPLE = 1
    var imageEncoded: String = ""
    var imagesEncodedList: ArrayList<String> = ArrayList()
    var imagePathList: ArrayList<ImageData> = ArrayList()
    var isPdfCreate = false
    var isUploadAadhar = false
    var requestData: String = ""
    var pdfName: String = ""
    var pdfPath: String = ""
    var imageName: String = ""
    var imagePath: String = ""
    var imageCount = 1
    var fileType = ""
    lateinit var pdfFile: File

    //Watermark
    var watermarkMessage = ""
    var additionalWatermark = ""
    var watermarkFontSize = 0f
    var watermarkAlignX = 0f
    var watermarkAlignY = 0f
    var watermarkRotation = 0f

    var tempBitmap: Bitmap? = null
    var tempBitmap_list: ArrayList<Bitmap> = ArrayList()

    override fun getLatLng(location: Location?) {
        this.location = Location("ImagesLocation")
        if (location != null) {
            this.location.latitude = location.latitude
            this.location.accuracy = location.accuracy
            this.location.longitude = location.longitude
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pickerActivityBinding = PickerActivityBinding.inflate(layoutInflater)
        setContentView(pickerActivityBinding.root)


        if (supportActionBar != null) {
            supportActionBar!!.hide()
        }
        pickerActivityBinding.progressBarPicker.setIndicatorColor(
            ContextCompat.getColor(
                baseContext,
                R.color.black
            )
        )
        latLngModule = LatLngModule(this@PickerActivity, this)
        val temp = latLngModule.startLocation()
        Utils.printMessage("Location is $temp")

        val intent = intent
        if (intent != null) {
            requestData = intent.extras!!.getString(ScanConstants.REQUEST)!!
            try {
                val jsonObject = JSONObject(requestData)
                Utils.printMessage("My Directory:- $jsonObject")

                if (jsonObject.has(PDF_CREATE)) {
                    isPdfCreate = jsonObject.getBoolean(PDF_CREATE)
                }

                if (jsonObject.has(IS_UPLOAD_AADHAR)) {
                    isUploadAadhar = jsonObject.getBoolean(IS_UPLOAD_AADHAR)
                }

                if (jsonObject.has(FILE_TYPE)) {
                    fileType = jsonObject.getString(FILE_TYPE)
                } else {
                    createJSONDataONERROR(
                        101,
                        baseContext.resources.getString(R.string.file_type_error)
                    )
                }

                if (jsonObject.has(ScanConstants.WATERMARK_MESSAGE)) {
                    watermarkMessage = jsonObject.getString(ScanConstants.WATERMARK_MESSAGE)
                }
                if (jsonObject.has(ScanConstants.ADDITIONAL_WATERMARK)) {
                    additionalWatermark = jsonObject.getString(ScanConstants.ADDITIONAL_WATERMARK)
                }
                if (jsonObject.has(ScanConstants.WATERMARK_FONT_SIZE)) {
                    watermarkFontSize = try {
                        jsonObject.optDouble(ScanConstants.WATERMARK_FONT_SIZE).toFloat()
                    } catch (e: Exception) {
                        0f
                    }
                }
                if (jsonObject.has(ScanConstants.WATERMARK_ALIGN_X)) {
                    watermarkAlignX = try {
                        jsonObject.optDouble(ScanConstants.WATERMARK_ALIGN_X).toFloat()
                    } catch (e: Exception) {
                        0f
                    }
                }
                if (jsonObject.has(ScanConstants.WATERMARK_ALIGN_Y)) {
                    watermarkAlignY = try {
                        jsonObject.optDouble(ScanConstants.WATERMARK_ALIGN_Y).toFloat()
                    } catch (e: Exception) {
                        0f
                    }
                }
                if (jsonObject.has(ScanConstants.WATERMARK_ROTATION)) {
                    watermarkRotation = try {
                        jsonObject.optDouble(ScanConstants.WATERMARK_ROTATION).toFloat()
                    } catch (e: Exception) {
                        0f
                    }
                }

                when (fileType) {
                    IMAGE -> {
                        if (isPdfCreate) {
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

                        if (jsonObject.has(IMAGE_PATH)) {
                            imagePath = jsonObject.getString(IMAGE_PATH)
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


                    }

                    PDF -> {

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
                }


                if (isPdfCreate) {
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

                if (jsonObject.has(IMAGE_COUNT)) {
                    imageCount = jsonObject.getInt(IMAGE_COUNT)
                } else {
                    createJSONDataONERROR(
                        101,
                        baseContext.resources.getString(R.string.image_count_error)
                    )
                }

            } catch (e: Exception) {
                e.printStackTrace()

            }
        }

        startGallery(fileType)

    }

    private fun startGallery(fileType: String) {
        var chooserIntent: Intent? = null
        if (fileType.equals("image", true)) {
            val pickIntent =
                Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            pickIntent.type = "image/*"
            pickIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            pickIntent.action = Intent.ACTION_GET_CONTENT
            val mimeTypes = arrayOf("image/jpeg", "image/png")
            chooserIntent = Intent.createChooser(pickIntent, "Select Image")
            //chooserIntent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes)
        } else {
            val pdfIntent = Intent(Intent.ACTION_GET_CONTENT)
            pdfIntent.type = "application/pdf"
            pdfIntent.addCategory(Intent.CATEGORY_OPENABLE)
            chooserIntent = Intent.createChooser(pdfIntent, "Select Document")
        }
        resultGalleryLauncher.launch(chooserIntent)
    }

    @SuppressLint("Range")
    private var resultGalleryLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                // There are no request codes
                if (fileType.equals("image", true)) {
                    val data: Intent? = result.data
                    val dataUri = result.data!!.clipData
                    Utils.printMessage("My Response: $dataUri")
                    try {
                        if (null != data) {
                            val filePathColumn = arrayOf(MediaStore.Images.Media.DATA)
                            if (data.data != null) {
                                val mImageUri: Uri = data.data!!
                                val mOutputDirectory: File =
                                    ScanActivity.getOutputDirectory(baseContext)
                                val fileName =
                                    SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US)
                                        .format(System.currentTimeMillis()) + ".jpg"
                                val inputStream: InputStream? =
                                    contentResolver.openInputStream(mImageUri)
                                val b = BitmapFactory.decodeStream(inputStream)
                                /*val bitmap = Bitmap.createScaledBitmap(b, 540, 960, false)
                            inputStream!!.close()*/
                                val nh: Int = ((b.height * (768.0 / b.width)).roundToInt())
                                val scaled = Bitmap.createScaledBitmap(b, 768, nh, true)
                                Utils.saveBitmap(scaled, "$imagePath/$imageName")

                                imageEncoded = "$imagePath/$imageName"
                                val imageData = ImageData()
                                imageData.imagePath = imageEncoded

                                if (location != null) {
                                    imageData.lat = location.latitude.toString()
                                    imageData.lng = location.longitude.toString()
                                    imageData.accuracy = location.accuracy.toString()
                                }
                                imagePathList.add(imageData)
                                Log.d("LOG_TAG", "Selected Images :- $imageEncoded")
                                callSave()
                            } else {
                                if (data.clipData != null) {
                                    val mClipData: ClipData = data.clipData!!
                                    val mArrayUri: ArrayList<Uri> = ArrayList<Uri>()
                                    if (mClipData.itemCount <= imageCount) {
                                        for (i in 0 until mClipData.itemCount) {
                                            val item = mClipData.getItemAt(i)
                                            Utils.printMessage("My Response: $item")
                                            val uri: Uri = item.uri
                                            mArrayUri.add(uri)

                                            val mOutputDirectory: File =
                                                ScanActivity.getOutputDirectory(baseContext)
                                            val fileName =
                                                SimpleDateFormat(
                                                    "yyyy-MM-dd-HH-mm-ss-SSS$i",
                                                    Locale.US
                                                )
                                                    .format(System.currentTimeMillis()) + ".jpg"

                                            val inputStream: InputStream? =
                                                contentResolver.openInputStream(uri)
                                            val b = BitmapFactory.decodeStream(inputStream)
                                            //val bitmap = Bitmap.createScaledBitmap(b, 540, 960, false)
                                            val bitmap = Bitmap.createScaledBitmap(b, b.width, b.height, false)

                                            val rotatedBitmap =
                                                fixOrientation(bitmap)

                                            inputStream!!.close()
                                            if (imageName.contains(".")) {
                                                imageName = imageName.replace(".jpg", "")
                                            }
                                            imageName = "$imageName-$i.jpg"
                                            if (rotatedBitmap != null) {
                                                val scaledBitmap = Bitmap.createScaledBitmap(rotatedBitmap, 540, 960, false)
                                                Utils.saveBitmap(scaledBitmap, "$imagePath/$imageName")
                                            } else {
                                                val scaledBitmap = Bitmap.createScaledBitmap(bitmap, 540, 960, false)
                                                Utils.saveBitmap(scaledBitmap, "$imagePath/$imageName")
                                            }

                                            imageEncoded = "$imagePath/$imageName"
                                            Log.d("LOG_TAG", "Selected Images :- $imageEncoded")
                                            imagesEncodedList.add(imageEncoded)
                                            val imageData = ImageData()
                                            imageData.imagePath = imageEncoded

                                            if (location != null) {
                                                imageData.lat = location.latitude.toString()
                                                imageData.lng = location.longitude.toString()
                                                imageData.accuracy = location.accuracy.toString()
                                            }
                                            imagePathList.add(imageData)
                                        }
                                        Log.d(
                                            "LOG_TAG",
                                            "Selected Images Count:- $imagesEncodedList"
                                        )
                                        callSave()
                                    } else {
                                        val tempPath = File(pdfPath, pdfName)
                                        Utils.deleteFile(tempPath.toString().toUri())
                                        Toast.makeText(
                                            this,
                                            "Please select only $imageCount images",
                                            Toast.LENGTH_LONG
                                        ).show()
                                        finish()
                                    }
                                }
                            }
                        } else {
                            Toast.makeText(this, "You haven't picked Image", Toast.LENGTH_LONG)
                                .show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(this, "Something went wrong", Toast.LENGTH_LONG).show()
                        e.printStackTrace()
                    }

                } else {
                    try {
                        val data: Intent? = result.data
                        if (data != null) {
                            val uri: Uri = data.data!!
                            var file = File(uri.path)
                            Utils.printMessage("Pdf Path :--" + file.absolutePath)
                            copyFileFromIntent(
                                this@PickerActivity,
                                data,
                                file.absolutePath,
                                file.absolutePath
                            )
                            callSave()
                            //executorWatermark(pdfFile.absolutePath, pdfFile.absolutePath, watermarkMessage, additionalWatermark, watermarkFontSize, watermarkAlignX, watermarkAlignY, watermarkRotation)
                        } else {
                            Toast.makeText(this, "Something went wrong", Toast.LENGTH_LONG).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(this, "Something went wrong", Toast.LENGTH_LONG).show()
                    }
                }

            } else {
                finish()
            }
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

    fun copyFileFromIntent(
        context: Context,
        intent: Intent,
        outputFileName: String?,
        outputSubFolder: String?
    ): Boolean {
        var outputFileName = outputFileName
        var outputSubFolder = outputSubFolder
        val extStorageDirectory = context.getExternalFilesDir("").toString()
        outputSubFolder = extStorageDirectory
        val outputFileName1: String
        outputFileName1 = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US)
            .format(System.currentTimeMillis()) + ".pdf"
        outputFileName = outputFileName1
        pdfFile = File(pdfPath, pdfName)
        Utils.printMessage("Pdf Path :--" + pdfFile.absolutePath)
        /*imagePath = outputFile.absolutePath
        file = outputFile*/
        val uri = intent.data
        try {
            context.contentResolver.openInputStream(uri!!).use { inputStream ->
                FileOutputStream(pdfFile).use { outputStream ->
                    val buffer = ByteArray(4 * 1024)
                    var read: Int
                    while (inputStream!!.read(buffer).also { read = it } != -1) {
                        outputStream.write(buffer, 0, read)
                    }
                    outputStream.flush()
                    return true
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
            return false
        }
    }


    private fun executorWatermark(
        inputPath: String,
        outputPath: String,
        watermarkText: String,
        additionalInfo: String,
        fontSize: Float,
        alignX: Float,
        alignY: Float,
        rotation: Float
    ) {
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
            addWatermarkToPdf(
                inputPath,
                outputPath,
                watermarkText,
                additionalInfo,
                fontSize,
                alignX,
                alignY,
                rotation
            )
            //compressPDF(inputPath, outputPath, 150)


            handler.post {
                /*
                * You can perform any operation that
                * requires UI Thread here.
                *
                * its like onPostExecute()
                * */
                runOnUiThread {
                    createJSONDataONSuccess(
                        100,
                        "Success",
                        "",
                        imagePathList,
                        outputPath
                    )

                }

            }
        }

    }

    private fun addWatermarkToPdf(
        inputPath: String,
        outputPath: String,
        watermarkText: String,
        additionalInfo: String,
        fontSize: Float,
        alignX: Float,
        alignY: Float,
        rotation: Float
    ): Boolean {
        try {
            Utils.printMessage("Watermark Pdf Path :--$inputPath\n$outputPath")
            val reader = PdfReader(FileInputStream(inputPath))
            val stamper = PdfStamper(reader, FileOutputStream(outputPath))

            val font = FontFactory.getFont(FontFactory.HELVETICA, 52f, Font.BOLD, GrayColor(0.85f))
            val font2 = Font(Font.FontFamily.TIMES_ROMAN, 52f, Font.BOLD, BaseColor.RED)

            val total: Int = reader.numberOfPages + 1
            for (i in 1 until total) {
                if (watermarkText.isNotBlank()) {
                    val over = stamper.getOverContent(i) // Get the first page's content
                    reader.setPageContent(i + 1, reader.getPageContent(i + 1))
                    over.saveState()
                    over.beginText()
                    over.setFontAndSize(font.baseFont, fontSize)
                    over.setTextMatrix(30f, 30f)
                    val gs1 = PdfGState()
                    gs1.setFillOpacity(0.1f)
                    over.setGState(gs1)
                    over.showTextAligned(
                        Element.ALIGN_CENTER,
                        watermarkText,
                        alignX,
                        alignY,
                        rotation
                    )
                    over.endText()
                    over.restoreState()
                }

                if (additionalWatermark.isNotBlank()) {
                    val over2 = stamper.getOverContent(i) // Get the first page's content
                    over2.saveState()
                    over2.beginText()
                    over2.setFontAndSize(font.baseFont, 14f)
                    over2.setTextMatrix(30f, 30f)
                    over2.showTextAligned(Element.ALIGN_BOTTOM, additionalWatermark, 10f, 10f, 0f)
                    over2.endText()
                    over2.restoreState()
                }

            }

            stamper.setFullCompression()
            stamper.close()
            reader.close()

            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }


    private fun callSave() {
        if (fileType.equals("pdf", true)) {

            if (isUploadAadhar) {
                val extractedImages: List<Bitmap> = extractImagesFromPDF(
                    pdfFile.toString()
                )!!

                val executor = Executors.newSingleThreadExecutor()
                val handler = Handler(Looper.getMainLooper())
                var pdfPath2 = ""
                executor.execute {
                    for (image in extractedImages.indices) {
                        maskImage(extractedImages[image], image)
                    }
                    handler.post {
                        if (tempBitmap_list.isNotEmpty()) {
                            pdfPath2 =
                                PdfUtility.createPdf(this, tempBitmap_list, pdfFile.absolutePath)!!
                            Utils.printMessage("Pdf path 123 $pdfPath2")
                        }
                    }
                }
            }

            createJSONDataONSuccess(
                100,
                "Success",
                "",
                imagePathList,
                pdfFile.toString()
            )
        } else if (isPdfCreate) {
            val compressPdfFile = PdfUtility.createPdf(
                imagePathList,
                pdfPath,
                pdfName
            )

            if (isUploadAadhar) {
                val extractedImages: List<Bitmap> = extractImagesFromPDF(
                    compressPdfFile.toString()
                )!!

                val executor = Executors.newSingleThreadExecutor()
                val handler = Handler(Looper.getMainLooper())
                var pdfPath2 = ""
                executor.execute {
                    for (image in extractedImages.indices) {
                        maskImage(extractedImages[image], image)
                        Utils.printMessage("indices $image")
                    }
                    handler.post {
                        if (tempBitmap_list.isNotEmpty()) {
                            pdfPath2 =
                                PdfUtility.createPdf(this, tempBitmap_list, compressPdfFile!!.absolutePath)!!
                            Utils.printMessage("Pdf path 123 $pdfPath2")
                        }
                    }
                }
            }

            Utils.printMessage("Pdf Path :$compressPdfFile")
            for (item in imagePathList) {
                Utils.printMessage("Delete File :- ${item.imagePath.toUri()}")
                Utils.deleteFile(item.imagePath.toUri())
            }
            //executorWatermark(compressPdfFile!!.absolutePath, compressPdfFile!!.absolutePath, "CREDILITY", "Additional Info", 45f, 280f, 400f, 40f)
            if (watermarkMessage.isNotBlank() || additionalWatermark.isNotBlank()) {
                executorWatermark(
                    compressPdfFile!!.absolutePath,
                    compressPdfFile!!.absolutePath,
                    watermarkMessage,
                    additionalWatermark,
                    watermarkFontSize,
                    watermarkAlignX,
                    watermarkAlignY,
                    watermarkRotation
                )
            } else {
                createJSONDataONSuccess(
                    100,
                    "Success",
                    "",
                    imagePathList,
                    compressPdfFile!!.absolutePath
                )
            }
        } else {

            /*if (isUploadAadhar) {
                val executor = Executors.newSingleThreadExecutor()
                val handler = Handler(Looper.getMainLooper())
                var pdfPath2 = ""
                executor.execute {
                    for (image in imagePathList.indices) {
                        maskImage(Utils.getBitmapFromPath(imagePathList[image].imagePath), image)
                    }
                    handler.post {
                        if (tempBitmap_list.isNotEmpty()) {
                            pdfPath2 =
                                PdfUtility.createPdf(this, tempBitmap_list, compressPdfFile!!.absolutePath)!!
                            Utils.printMessage("Pdf path 123 $pdfPath2")
                        }
                    }
                }
            }*/

            createJSONDataONSuccess(
                100,
                "Success",
                "",
                imagePathList,
                ""
            )
        }
    }

    /*private fun compressPDF(): File{
        val document = PDDocument()

        if (imagePathList != null) {
            for (i in imagePathList.indices) {
                val page = PDPage(PDRectangle.A4)
                document.addPage(page)
                var contentStream = PDPageContentStream(document, page)

                val bimapCompress: Bitmap = BitmapFactory.decodeFile(imagePathList[i].imagePath)

                val ximage: PDImageXObject = JPEGFactory.createFromImage(document, bimapCompress, 0.50f, 72)
                contentStream.drawImage(ximage, 0f, 0f)

                contentStream.close()
            }
        }else{
            val page = PDPage(PDRectangle.A4)
            document.addPage(page)

            var contentStream = PDPageContentStream(document, page)

            val bimapCompress: Bitmap = BitmapFactory.decodeFile(imageEncoded)
            val ximage: PDImageXObject = JPEGFactory.createFromImage(document, bimapCompress, 0.50f, 72)
            contentStream.drawImage(ximage, 0f, 0f)

            contentStream.close()
        }


        var pdfFile = File(pdfPath, pdfName)
        document.save(pdfFile.path)
        document.close()

        return pdfFile
    }*/


    fun extractImagesFromPDF(
        pdfFilePath: String?
    ): List<Bitmap>? {
        val bitmaps: MutableList<Bitmap> = ArrayList()
        try {
            val parcelFileDescriptor =
                ParcelFileDescriptor.open(File(pdfFilePath), ParcelFileDescriptor.MODE_READ_ONLY)
            val pdfRenderer = PdfRenderer(parcelFileDescriptor)
            for (i in 0 until pdfRenderer.pageCount) {
                val page = pdfRenderer.openPage(i)
                val bitmap = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                bitmaps.add(bitmap)
                page.close()
            }
            /*for (pageNumber in pageNumbers) {
                if (pageNumber < 0 || pageNumber >= pdfRenderer.pageCount) {
                    continue
                }
                val page = pdfRenderer.openPage(pageNumber)
                val bitmap = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                bitmaps.add(bitmap)
                page.close()
            }*/
            pdfRenderer.close()
            parcelFileDescriptor.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return bitmaps
    }

    private fun maskImage(bitmap: Bitmap?, pos: Int) {
        val textRecognizer = TextRecognizer.Builder(this).build()
        if (!textRecognizer.isOperational) {
            Log.e("Main Activity", "Dependencies not available")

            // Check android for low storage so dependencies can be loaded, DEPRICATED CHANGE LATER
            val intentLowStorage = IntentFilter(Intent.ACTION_DEVICE_STORAGE_LOW)
            val hasLowStorage = registerReceiver(null, intentLowStorage) != null
            if (hasLowStorage) {
                Toast.makeText(this, "Low Memory On Disk", Toast.LENGTH_LONG).show()
                Log.e("Main Activity", "Low Memory On Disk")
            }
        } else if (bitmap != null) {
            val frame = Frame.Builder().setBitmap(bitmap).build()
            val items: SparseArray<*> = textRecognizer.detect(frame)
            val blocks: MutableList<TextBlock?> = ArrayList()
            var myItem: TextBlock? = null
            for (i in 0 until items.size()) {
                myItem = items.valueAt(i) as TextBlock
                blocks.add(myItem)
            }
            val rectPaint = Paint()
            rectPaint.color = Color.WHITE
            rectPaint.style = Paint.Style.FILL
            rectPaint.strokeWidth = 4.0f

            tempBitmap = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.RGB_565)
            val canvas = Canvas(tempBitmap!!).apply {
                drawBitmap(bitmap, 0f, 0f, null)
            }
            val rectFS = ArrayList<Rect>()
            for (textBlock in blocks) {
                run {
                    val textLines =
                        textBlock!!.components

                    for (currentLine in textLines) {
                        val words: MutableList<out com.google.android.gms.vision.text.Text?>? =
                            currentLine.components
                        println("currentword.getValue() " + words?.size)
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
                                val rect: android.graphics.Rect =
                                    android.graphics.Rect(currentword!!.boundingBox)
                                rectPaint.color = android.graphics.Color.RED
                                println("currentword.getValue() " + currentword.boundingBox + " " + currentword.value)
                                println("currentword.getValue() " + currentword.boundingBox.left + " " + currentword.boundingBox.top + " " + currentword.boundingBox.right + " " + currentword.boundingBox.bottom)
                                var check: kotlin.Boolean = false
                                if (old != null) {
                                    check = true
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
                                        rectFS.add(rectMask)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if (rectFS.size > 0) {
                for (rect in rectFS) {
                    canvas.drawRect(rect, rectPaint)
                    if (tempBitmap_list.size <= pos) {
                        tempBitmap_list.add(tempBitmap!!)
                    } else {
                        tempBitmap_list.remove(tempBitmap!!)
                        tempBitmap_list.add(tempBitmap!!)
                    }
                }
            } else {
                if (tempBitmap_list.size <= pos) {
                    tempBitmap_list.add(tempBitmap!!)
                } else {
                    tempBitmap_list.remove(tempBitmap!!)
                    tempBitmap_list.add(tempBitmap!!)
                }
            }

        }
    }


    private fun createJSONDataONERROR(code: Int, message: String?) {
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

    private fun createJSONDataONSuccess(
        code: Int,
        message: String?,
        imageProcessPath: String?,
        imageDataList: ArrayList<ImageData>?,
        pdfPath: String?
    ) {
        val jsonObject = JSONObject()
        try {
            jsonObject.put(CODE, code)
            jsonObject.put(MESSAGE, message)
            val jsonArray = JSONArray()
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
            jsonObject.put(IMAGE_DATA, jsonArray)
            jsonObject.put(PDF_PATH, pdfPath)
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        val result = Intent()
        result.putExtra(SCANNED_RESULT, jsonObject.toString())
        setResult(RESULT_OK, result)
        finish()
    }


}