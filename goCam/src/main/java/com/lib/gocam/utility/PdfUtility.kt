package com.lib.gocam.utility

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfDocument
import android.util.Log
import com.itextpdf.text.Document
import com.itextpdf.text.DocumentException
import com.itextpdf.text.Image
import com.itextpdf.text.PageSize
import com.itextpdf.text.Rectangle
import com.itextpdf.text.pdf.PdfWriter
import com.lib.gocam.model.ImageData
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

object PdfUtility {
    fun createPdf(bitmap: ArrayList<ImageData>, pdfPath: String?, pdfName: String): File? {
        Log.d("","Pdf name and path : - $pdfPath $pdfName")
        var myBitmap: Bitmap
        val docsFolder = File(pdfPath)
        if (!docsFolder.exists()) {
            docsFolder.mkdir()
            Log.i("", "Created a new directory for PDF")
        }
        val doc = Document(PageSize.LETTER)
        val documentRect: Rectangle = doc.pageSize
        var pdfNameExtension = if (!pdfName.contains(".pdf")){
            "$pdfName.pdf"
        }else{
            pdfName
        }
        val exportDir = File(docsFolder.absolutePath, pdfNameExtension)
        try {
            PdfWriter.getInstance(doc, FileOutputStream(exportDir))
            doc.open()
            for (i in bitmap.indices) {

                val stream3 = ByteArrayOutputStream()
                val file: File = File(bitmap[i].imagePath)
                if (file.exists()) {
                    myBitmap = BitmapFactory.decodeFile(file.absolutePath)
                    myBitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream3)
                    val image: Image = Image.getInstance(stream3.toByteArray())
                    doc.pageSize = image
                    doc.newPage()
                    /*if (myBitmap.width > documentRect.width || myBitmap.height > documentRect.height) {

                        //bitmap is larger than page,so set bitmap's size similar to the whole page
                        image.scaleAbsolute(documentRect.width, documentRect.height)
                    } else {
                        //bitmap is smaller than page, so add bitmap simply.
                        //[note: if you want to fill page by stretching image,
                        // you may set size similar to page as above]
                        image.scaleAbsolute(myBitmap.width.toFloat(), myBitmap.height.toFloat())
                    }
                    image.setAbsolutePosition(
                        (documentRect.getWidth() - image.getScaledWidth()) / 2,
                        (documentRect.getHeight() - image.getScaledHeight()) / 2
                    )*/
                    image.setAbsolutePosition(0f, 0f)
                    image.border = Image.BOX
                    image.borderWidth = 15f
                    doc.add(image)
                }
            }
        } catch (e: DocumentException) {
            e.printStackTrace()
            return null
        } catch (e: IOException) {
            e.printStackTrace()
            return null
        } finally {
            doc.close()
        }
        return exportDir
    }

    fun createPdf(context: Context, bitmaps: List<Bitmap>, pdfPath: String): String? {
        val pdfDocument = PdfDocument()
        //val pdfPath = getOutputMediaFile(context)?.absolutePath

        if (pdfPath == null) {
            return null
        }

        try {
            for (bitmap in bitmaps) {
                val pageInfo = PdfDocument.PageInfo.Builder(bitmap.width, bitmap.height, 1).create()
                val page = pdfDocument.startPage(pageInfo)
                val canvas = page.canvas
                canvas.drawBitmap(bitmap, 0f, 0f, null)
                pdfDocument.finishPage(page)
            }

            val outputStream = FileOutputStream(pdfPath)
            pdfDocument.writeTo(outputStream)
            pdfDocument.close()
            outputStream.close()
        } catch (e: IOException) {
            e.printStackTrace()
            return null
        }
        return pdfPath
    }

    private fun getOutputMediaFile(context: Context): File? {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir =  context.getExternalFilesDir("")
        if (storageDir != null && !storageDir.exists()) {
            if (!storageDir.mkdirs()) {
                return null
            }
        }
        return File(storageDir, "PDF_$timeStamp.pdf")
    }



}
