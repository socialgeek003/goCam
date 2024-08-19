package com.lib.gocam.fragment

/*import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.graphics.image.JPEGFactory
import com.tom_roush.pdfbox.pdmodel.graphics.image.PDImageXObject*/

import android.annotation.SuppressLint
import android.app.Activity.RESULT_OK
import android.app.AlertDialog
import android.content.Intent
import android.content.IntentFilter
import android.graphics.*
import android.location.Address
import android.location.Geocoder
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.util.Log
import android.util.SparseArray
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.RotateAnimation
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.GoogleMap.SnapshotReadyCallback
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.vision.Frame
import com.google.android.gms.vision.text.TextBlock
import com.google.android.gms.vision.text.TextRecognizer
import com.itextpdf.text.Element
import com.itextpdf.text.Font
import com.itextpdf.text.FontFactory
import com.itextpdf.text.pdf.GrayColor
import com.itextpdf.text.pdf.PdfGState
import com.itextpdf.text.pdf.PdfReader
import com.itextpdf.text.pdf.PdfStamper
import com.lib.gocam.R
import com.lib.gocam.activity.AadhaarMaskingActivity
import com.lib.gocam.activity.ScanActivity
import com.lib.gocam.databinding.FragmentResultBinding
import com.lib.gocam.model.ImageData
import com.lib.gocam.utility.PdfUtility.createPdf
import com.lib.gocam.utility.ScanConstants
import com.lib.gocam.utility.Utils
import com.theartofdev.edmodo.cropper.CropImage
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.URI
import java.util.*
import java.util.concurrent.Executors


class ResultFragment: Fragment(), OnMapReadyCallback {

    companion object {
        lateinit var scanActivity: ScanActivity
        var isDocument = false
    }
    lateinit var resultFragment: ResultFragment
    var bitmap: Bitmap? = null
    var rotation = 0
    var isMaskAadhaar = false
    var tempBitmap: Bitmap? = null
    private var mMap: GoogleMap? = null
    var pass_degree: Double = 0.0
    private var currentDegree = 0f
    lateinit var resultBinding: FragmentResultBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        scanActivity = context as ScanActivity
        resultFragment = this
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bindView()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        resultBinding = FragmentResultBinding.inflate(inflater, container, false)

        resultBinding.map.onCreate(savedInstanceState)
        resultBinding.map.getMapAsync(this)
        resultBinding.map.isDrawingCacheEnabled = true;


        return resultBinding.root
    }

    private fun bindView(){

        resultBinding.resultProgressBar.setIndicatorColor(ContextCompat.getColor(requireContext(), R.color.white))
        resultBinding.btnClose.setOnClickListener { scanActivity.onBackPressed() }

        resultBinding.brightness.setOnClickListener { scanActivity.changeBrightness() }

        bitmap = scanActivity.getTransformed()
        if (bitmap == null) {
            bitmap = scanActivity.getScanImage()
        }
        setScannedImage(bitmap)


        if (arguments != null) {
            pass_degree = requireArguments().getDouble("pass_degree")
        }

        resultBinding.crop.setOnClickListener { cropPhoto() }
        resultBinding.saveButton.setOnClickListener{
            if (resultBinding.locationLayout.isVisible){
                mMap!!.snapshot(SnapshotReadyCallback { snapshot ->
                    resultBinding.map1Img.setImageBitmap(snapshot)
                    saveButtonClick()
                })
            }else{
                saveButtonClick()
            }
        }
        resultBinding.retake.setOnClickListener { retakePhoto() }
        resultBinding.original.setOnClickListener { setOriginalPhoto() }
        resultBinding.maskAadhaar.setOnClickListener { maskAadhaar() }
        resultBinding.autoCropButton.setOnClickListener {
            /*scanActivity.setCheckDoc(false)*/
            autoCrop()
            documentOcrExecutor(scanActivity.getTransformed())
        }
        if (scanActivity.count > 1){
            resultBinding.resultImageCount.visibility = View.VISIBLE
            resultBinding.resultImageCount.text = (scanActivity.captureCount+1).toString() +"/"+ scanActivity.count + "Images"
        }else{
            resultBinding.resultImageCount.visibility = View.GONE
        }
        if(scanActivity.captureCount+1 < scanActivity.count){
            resultBinding.saveButton.text = resources.getString(R.string.next)
        }else{
            resultBinding.saveButton.text = resources.getString(R.string.save)
        }
        if (/*scanActivity.count == 1 &&*/ scanActivity.isAadharMasking() && scanActivity.isCheckDoc()) {
            autoMaskAadhaar()
        }else if (/*scanActivity.count == 1 && */scanActivity.isAadharMasking()) {
            maskAadhaar()
        }
        if (scanActivity.isCheckDoc()){
            resultBinding.saveButton.visibility = View.GONE
            resultBinding.autoCropButton.visibility = View.VISIBLE
            resultBinding.ivCrop.visibility = View.VISIBLE
            resultBinding.ivCrop.setImageToCrop(bitmap)
            isDocument = true
            autoCrop()
            documentOcrExecutor(scanActivity.getTransformed())
        }

        if (scanActivity.isCheckBlankDoc()){
            documentOcrExecutor(scanActivity.getScanImage())
        }


        if (scanActivity.isAddLocation()){
            resultBinding.locationLayout.visibility = View.VISIBLE
            getCompleteAddressString(scanActivity.location.latitude, scanActivity.location.longitude)
        }else{
            resultBinding.locationLayout.visibility = View.GONE
        }
        /*  val mapFragment = childFragmentManager
              .findFragmentById(R.id.map) as SupportMapFragment?
          mapFragment!!.getMapAsync(resultFragment)*/
        setcompassss()
    }

    private fun maskAadhaar(){
        val intent = Intent(activity, AadhaarMaskingActivity::class.java)
        bitmap = scanActivity.getTransformed()
        if (bitmap == null) {
            bitmap = scanActivity.getScanImage()
        }
        Utils.saveBitmap(bitmap!!, scanActivity.imagePath)
        intent.putExtra("imagePath", scanActivity.imagePath)
        resultLauncher.launch(intent)
    }

    private fun autoMaskAadhaar(){
        try {
            resultBinding.resultProgressLayout.visibility = View.VISIBLE
            resultBinding.resultProgressBar.visibility = View.VISIBLE
            autoAadhaarMasking(bitmap)
        }catch (e: Exception){
            e.printStackTrace()
        }
    }

    private fun autoCrop(){
        val crop: Bitmap = resultBinding.ivCrop.crop()
        scanActivity.setTransformed(crop)
        resultBinding.autoCropButton.visibility = View.GONE
        resultBinding.saveButton.visibility = View.VISIBLE
        resultBinding.ivCrop.visibility = View.GONE
        //scanActivity.setCheckDoc(false)
        isDocument = false
        setScannedImage(crop)
    }

    private var resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val maskBitmap = Utils.getBitmapFromPath(scanActivity.imagePath)
        scanActivity.setTransformed(maskBitmap)
        setScannedImage(maskBitmap)
        //scanActivity.setAadharMasking(false)
        isMaskAadhaar = true
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            val result = CropImage.getActivityResult(data)
            if (resultCode == RESULT_OK) {
                val resultUri: Uri = result.uri
                Utils.printMessage("Crop Image: $resultUri")
                val cropFile2 = File(URI(resultUri.toString()))
                val tempPath = File(scanActivity.getPath(), scanActivity.imageName)
                Utils.deleteFile(tempPath.toString().toUri())
                val cropFileCopy = cropFile2.copyTo(tempPath)
                val cropBitmap = Utils.getBitmapFromPath(scanActivity.imagePath)
                scanActivity.setTransformed(cropBitmap)
                setScannedImage(cropBitmap)
            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                val error = result.error
            }
        }
    }

    private fun retakePhoto(){
        Utils.printMessage("My Data ${scanActivity.imagePathList.size}")
        /*if (scanActivity.imagePathList.size != 0){
            Utils.deleteFile(Uri.parse(scanActivity.imagePathList[scanActivity.imagePathList.size - 1].imagePath))
        }else{
            Utils.deleteFile(Uri.parse(scanActivity.imagePath))
        }*/
        scanActivity.isRetake = true
        scanActivity.onBackPressed()
    }

    private fun setOriginalPhoto(){
        val tempOriginalPath = File(scanActivity.getPath(), "ORIGINAL-${scanActivity.imageName}")
        val tempPath = File(scanActivity.imagePath)
        Utils.deleteFile(tempPath.toString().toUri())
        val tempStorePath = tempOriginalPath.copyTo(tempPath)
        scanActivity.imagePath = tempStorePath.toString()
        val originalBitmap = Utils.getBitmapFromPath(scanActivity.imagePath)
        resultBinding.scannedImage.setImageBitmap(originalBitmap)
    }

    private fun cropPhoto(){
        try{
            bitmap = scanActivity.getTransformed()
            if (bitmap == null) {
                bitmap = scanActivity.getScanImage()
            }
            Utils.saveBitmap(bitmap!!, scanActivity.imagePath)
            val cropFile = File(scanActivity.imagePath)
            CropImage.activity(cropFile.toUri())
                .start(requireContext(), this)
        }catch (e: Exception){
            e.printStackTrace()
        }
    }




    /*private fun compressPDF(): File{
        val document = PDDocument()

        if (scanActivity.imagePathList != null) {
            for (i in scanActivity.imagePathList.indices) {
                val page = PDPage(PDRectangle.A4)
                document.addPage(page)
                var contentStream = PDPageContentStream(document, page)

                val bimapCompress: Bitmap = BitmapFactory.decodeFile(scanActivity.imagePathList[i].imagePath)
                val bitmap = Bitmap.createScaledBitmap(bimapCompress, 540, 960, false)

                val ximage: PDImageXObject = JPEGFactory.createFromImage(document, bitmap, 0.50f, 72)
                contentStream.drawImage(ximage, 0f, 0f)

                contentStream.close()
            }
        }else{
            val page = PDPage(PDRectangle.A4)
            document.addPage(page)

            var contentStream = PDPageContentStream(document, page)

            val bimapCompress: Bitmap = BitmapFactory.decodeFile(scanActivity.imagePath)
            val bitmap = Bitmap.createScaledBitmap(bimapCompress, 540, 960, false)
            val ximage: PDImageXObject = JPEGFactory.createFromImage(document, bitmap, 0.50f, 72)
            contentStream.drawImage(ximage, 0f, 0f)

            contentStream.close()
        }


        var pdfFile = File(scanActivity.pdfPath, scanActivity.pdfName)
        document.save(pdfFile.path)
        document.close()

        return pdfFile
    }*/

    private fun setScannedImage(scannedBitmap: Bitmap?) {
        resultBinding.scannedImage.setImageBitmap(scannedBitmap)
    }

    private fun saveButtonClick(){
        try {
            var bitmap: Bitmap? = scanActivity.getTransformed()
            if (bitmap == null) {
                bitmap = scanActivity.getScanImage()!!
            }
            var copyBitmap: Bitmap = bitmap
            if(scanActivity.isAddLocation()){
                resultBinding.topBar.visibility = View.GONE
                resultBinding.topViewResult.visibility = View.GONE
                bitmap = Utils.getBitmapFromView(resultBinding.mainLayout)!!
            }
            Utils.printMessage("CountImagesCaptured:" + scanActivity.remainingCount)
            if (scanActivity.isImageProcess()) {
                val imagePath: File = File(scanActivity.imageProcessPath)
                val path: String? = Utils.saveBitmap(bitmap, imagePath.toString())
                scanActivity.createJSONDataONSuccess(100, "Success", path, null, "")
            } else {
                var imagePath: File? = null
                if (scanActivity.count == 1) {
                    if (scanActivity.imageName.contains(".")) {
                        imagePath = File(scanActivity.getPath(), scanActivity.imageName)
                    } else {
                        imagePath = File(
                            scanActivity.getPath(),
                            scanActivity.imageName + ".jpg"
                        )
                    }
                } else {
                    if (scanActivity.imageName.contains(".")) {
                        val imageName: String =
                            scanActivity.imageName.replace(".jpg", "")
                        imagePath = File(
                            scanActivity.getPath(),
                            imageName + (scanActivity.imagePathList.size + 1).toString() + ".jpg"
                        )
                    } else {
                        imagePath = File(
                            scanActivity.getPath(),
                            scanActivity.imageName + (scanActivity.imagePathList.size + 1).toString() + ".jpg"
                        )
                    }
                }
                val path: String? = Utils.saveBitmap(bitmap, imagePath.toString())
                val imageData = ImageData()
                if (path != null) {
                    imageData.imagePath = path
                }
                if (scanActivity.location != null) {
                    imageData.lat = scanActivity.location.latitude.toString()
                    imageData.lng = scanActivity.location.longitude.toString()
                    imageData.accuracy = scanActivity.location.accuracy.toString()
                }
                scanActivity.imagePathList.add(imageData)
                /*if (scanActivity.count == 1 && isMaskAadhaar) {
                    *//*if (scanActivity.isLocationEnabled) {
                        scanActivity.latLngModule.startLocation()
                    }*//*
                    scanActivity.createJSONDataONSuccess(
                        100,
                        "Success",
                        "",
                        scanActivity.imagePathList,
                        ""
                    )
                } else */if (scanActivity.count == scanActivity.imagePathList.size) {
                    if (scanActivity.isCreatePdf()) {
                        val pdfFile = createPdf(
                            scanActivity.imagePathList,
                            scanActivity.pdfPath,
                            scanActivity.pdfName
                        )
                        if (scanActivity.watermarkMessage.isNotBlank() || scanActivity.additionalWatermark.isNotBlank()) {
                            executorWatermark(
                                pdfFile!!.absolutePath,
                                pdfFile!!.absolutePath,
                                scanActivity.watermarkMessage,
                                scanActivity.additionalWatermark,
                                scanActivity.watermarkFontSize,
                                scanActivity.watermarkAlignX,
                                scanActivity.watermarkAlignY,
                                scanActivity.watermarkRotation
                            )
                        }else{
                            Utils.printMessage("Pdf Path :$pdfFile")
                            for (item in scanActivity.imagePathList){
                                Utils.printMessage("Delete File :- ${item.imagePath.toUri()}")
                                Utils.deleteFile(item.imagePath.toUri())
                            }
                            val tempPath = File(scanActivity.getPath(), "${scanActivity.imageName}")
                            Utils.deleteFile(tempPath.toString().toUri())
                            scanActivity.imagePathList.clear()
                            scanActivity.createJSONDataONSuccess(
                                100,
                                "Success",
                                "",
                                scanActivity.imagePathList,
                                pdfFile.toString()
                            )

                        }
                    }else {
                        if (scanActivity.imagePathList.isNotEmpty()) {
                            var pathCopy: String? = null
                            pathCopy = ""
                            if (scanActivity.getImageCopy()) {
                                try {
                                    var imagePath: File? = null
                                    if (scanActivity.count == 1) {
                                        if (scanActivity.imageName.contains(".")) {
                                            imagePath = File(
                                                scanActivity.getPath(),
                                                "copy-" + scanActivity.imageName
                                            )
                                        } else {
                                            imagePath = File(
                                                scanActivity.getPath(),
                                                "copy-" + scanActivity.imageName + ".jpg"
                                            )
                                        }
                                    }
                                    pathCopy = Utils.saveBitmap(copyBitmap, imagePath.toString())

                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                            if (scanActivity.imagePathList.size > 1) {
                                val tempPath =
                                    File(scanActivity.getPath(), "${scanActivity.imageName}")
                                Utils.deleteFile(tempPath.toString().toUri())
                            }
                            scanActivity.createJSONDataONSuccess(
                                100,
                                "Success",
                                pathCopy,
                                scanActivity.imagePathList,
                                ""
                            )
                        }
                    }
                    //scanActivity.dismissDialog()
                } else {
                    scanActivity.captureCount++
                    Utils.printMessage("My Count :- "+scanActivity.captureCount)
                    scanActivity.onBackPressed()
                    if (scanActivity.isLocationEnabled) {
                        scanActivity.latLngModule.startLocation()
                    }
                    /*getActivity().runOnUiThread(Runnable {
                        scanActivity.dismissDialog()
                        scanActivity.onBackPressed()
                        if (scanActivity.isLocationEnabled) {
                            scanActivity.latLngModule.startLocation()
                        }
                    })*/
                }
            }
            if (!isMaskAadhaar) {
                val tempPath = File(scanActivity.getPath(), "ORIGINAL-${scanActivity.imageName}")
                Utils.deleteFile(tempPath.toString().toUri())
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun executorWatermark(inputPath: String, outputPath: String, watermarkText: String, additionalInfo: String, fontSize: Float, alignX: Float, alignY: Float, rotation: Float){
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
            addWatermarkToPdf(inputPath, outputPath, watermarkText, additionalInfo, fontSize, alignX, alignY, rotation)
            //compressPDF(inputPath, outputPath, 150)


            handler.post {
                /*
                * You can perform any operation that
                * requires UI Thread here.
                *
                * its like onPostExecute()
                * */

                Utils.printMessage("Pdf Path :$outputPath")
                for (item in scanActivity.imagePathList){
                    Utils.printMessage("Delete File :- ${item.imagePath.toUri()}")
                    Utils.deleteFile(item.imagePath.toUri())
                }
                val tempPath = File(scanActivity.getPath(), "${scanActivity.imageName}")
                Utils.deleteFile(tempPath.toString().toUri())
                scanActivity.imagePathList.clear()
                scanActivity.createJSONDataONSuccess(
                    100,
                    "Success",
                    "",
                    scanActivity.imagePathList,
                    outputPath.toString()
                )
            }
        }

    }

    private fun addWatermarkToPdf(inputPath: String, outputPath: String, watermarkText: String, additionalInfo: String, fontSize: Float, alignX: Float, alignY: Float, rotation: Float): Boolean{
        try {
            Utils.printMessage("Watermark Pdf Path :--$inputPath\n$outputPath")
            val reader = PdfReader(FileInputStream(inputPath))
            val stamper = PdfStamper(reader, FileOutputStream(outputPath))

            val font = FontFactory.getFont(FontFactory.HELVETICA, 52f, Font.BOLD, GrayColor(0.85f))

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

                if (additionalInfo.isNotBlank()) {
                    val over2 = stamper.getOverContent(i) // Get the first page's content
                    over2.saveState()
                    over2.beginText()
                    over2.setFontAndSize(font.baseFont, 14f)
                    over2.setTextMatrix(30f, 30f)
                    over2.showTextAligned(Element.ALIGN_BOTTOM, additionalInfo, 10f, 10f, 0f)
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

    private fun autoAadhaarMasking(bitmap: Bitmap?){
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
                resultBinding.ivCrop.setImageBitmap(tempBitmap)
                isMaskAadhaar = true
                resultBinding.resultProgressLayout.visibility = View.GONE
                resultBinding.resultProgressBar.visibility = View.GONE

            }
        }

    }


    private fun generateOcr(bitmap: Bitmap?) {
        val textRecognizer = TextRecognizer.Builder(requireContext()).build()
        if (!textRecognizer.isOperational) {
            Log.e("Main Activity", "Dependencies not available")

            // Check android for low storage so dependencies can be loaded, DEPRICATED CHANGE LATER
            val intentLowStorage = IntentFilter(Intent.ACTION_DEVICE_STORAGE_LOW)
            val hasLowStorage = activity?.registerReceiver(null, intentLowStorage) != null
            if (hasLowStorage) {
                Toast.makeText(requireContext(), "Low Memory On Disk", Toast.LENGTH_LONG).show()
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
                        val words: MutableList<out com.google.android.gms.vision.text.Text?> =
                            currentLine.components
                        println("currentword.getValue() " + words?.size)
                        for (currentWord in words) {
                            println("currentWord " + currentWord!!.value)
                        }
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
                //autoMasking = true
                for (rect in rectFS) {
                    canvas.drawRect(rect, rectPaint)
                }
            } else {
                //autoMasking = false
            }

            //Set image to the `View`
//            iv_automatic.setImageBitmap(tempBitmap);
            //  imgView.setImageDrawable(new BitmapDrawable(getResources(), tempBitmap));
        }
    }


    private fun generateDocumentOcr(bitmap: Bitmap?): String? {
        val textRecognizer = TextRecognizer.Builder(requireContext()).build()
        var pan = ""
        val jsonObject = JSONObject()
        if (!textRecognizer.isOperational) {
            Log.e("Main Activity", "Dependencies not available")

            // Check android for low storage so dependencies can be loaded, DEPRICATED CHANGE LATER
            val intentLowStorage = IntentFilter(Intent.ACTION_DEVICE_STORAGE_LOW)
            val hasLowStorage = requireActivity().registerReceiver(null, intentLowStorage) != null
            if (hasLowStorage) {
                ResultFragment.scanActivity.createJSONData(ScanConstants.ERROR, 102, "Low Memory On Disk")
                Log.e("Main Activity", "Low Memory On Disk")
            }
        } else if (bitmap != null) {
            val frame = Frame.Builder().setBitmap(bitmap).build()
            val items: SparseArray<*> = textRecognizer.detect(frame)
            val blocks: MutableList<TextBlock?> = java.util.ArrayList()
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
                            pan += currentWord.value
                        }
                    }
                }
            }
        }
        jsonObject.put("data", pan)
        return jsonObject.toString()
    }

    private fun documentOcrExecutor(bitmap: Bitmap?){
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
            var resultPanOcr = generateDocumentOcr(bitmap)
            Utils.printMessage("DOCUMENT OCR: $resultPanOcr")


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
                if (jsonObject.has("data")){
                    tempStr = jsonObject.getString("data")
                }
                if (tempStr?.length!! > 0) {
                    //scanActivity.createJSONData(ScanConstants.SUCCESS, 100, resultPanOcr)
                } else {
                    //Toast.makeText(requireContext(),"Blank Document", Toast.LENGTH_LONG).show()
                    scanActivity.createJSONData(ScanConstants.ERROR, 101, "Blank Document")
                }

            }
        }

    }


    @SuppressLint("LongLogTag")
    private fun getCompleteAddressString(LATITUDE: Double, LONGITUDE: Double): String? {

        var strAdd = ""
        //var showAddress = "${ if (scanActivity.additionalInfo.isNotEmpty()) "${scanActivity.additionalInfo}\n" else "" }${if (scanActivity.createPdf) "${scanActivity.pdfName.replace(".pdf", "")}\n" else "${scanActivity.imageName.replace(".jpg", "")}\n"}"
        var showAddress = if (scanActivity.additionalInfo.isNotEmpty()) "${scanActivity.additionalInfo}\n" else ""
        val geocoder = Geocoder(requireContext(), Locale.getDefault())
        try {
            val addresses: List<Address>? = geocoder.getFromLocation(LATITUDE, LONGITUDE, 1)
            if (addresses != null && addresses.isNotEmpty()) {
                val returnedAddress: Address = addresses[0]
                val strReturnedAddress = StringBuilder("")
                for (i in 0..returnedAddress.maxAddressLineIndex) {
                    strReturnedAddress.append(returnedAddress.getAddressLine(i)).append("\n")
                }
                strAdd = strReturnedAddress.toString()
                showAddress = "${showAddress}${strReturnedAddress.trim()}\nLat: $LATITUDE \tLong: $LONGITUDE\n${Utils.getDateTime()}"
                resultBinding.locationField.text = showAddress

                Utils.printMessage("Current Address: $strReturnedAddress")
            } else {
                Utils.printMessage("Current Address: No Address returned!")
                if (!showAddress.isNullOrBlank()) {
                    resultBinding.locationField.text = showAddress
                }else{
                    noLocationDialogue()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            showAddress = "${showAddress}Lat: $LATITUDE \tLong: $LONGITUDE \n${Utils.getDateTime()}"
            resultBinding.locationField.text = showAddress
            Utils.printMessage("Current Address: Can not get address!")
        }
        return strAdd
    }

    private fun noLocationDialogue(){
        val builder1 = AlertDialog.Builder(requireContext())
        builder1.setMessage(this.resources.getString(R.string.no_location))
        builder1.setCancelable(true)
        builder1.setPositiveButton(
            "Yes"
        ) { dialog, id ->
            dialog.cancel()
            resultBinding.locationLayout.visibility = View.GONE
        }
        builder1.setNegativeButton(
            "No"
        ) { dialog, id ->
            scanActivity.createJSONData(ScanConstants.ERROR, 101, "Location not found")
            dialog.cancel()
        }
        val alert11 = builder1.create()
        alert11.show()
    }


    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // Add a marker in Sydney and move the camera
        val location = LatLng(scanActivity.location.latitude, scanActivity.location.longitude)
        mMap!!.addMarker(
            MarkerOptions()
                .position(location)
                .title("Location")
        )
        mMap!!.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 10f))
        val cameraPosition = CameraPosition.Builder()
            .target(location)
            .zoom(15f)
            .build()
        mMap!!.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
    }

    private fun setcompassss(){

        val ra = RotateAnimation(
            currentDegree,
            (-pass_degree).toFloat(),
            Animation.RELATIVE_TO_SELF, 0.5f,
            Animation.RELATIVE_TO_SELF,
            0.5f
        )

        ra.duration = 210

        ra.fillAfter = true
        if(scanActivity.isAddLocation()){
            resultBinding.imgCompassss.startAnimation(ra)
            resultBinding.imgCompassss.visibility = VISIBLE
        }else{
            resultBinding.imgCompassss.visibility = GONE
        }

        currentDegree = (-pass_degree).toFloat()
    }

}