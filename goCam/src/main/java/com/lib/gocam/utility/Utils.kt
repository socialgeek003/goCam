package com.lib.gocam.utility

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.view.View
import androidx.annotation.RequiresApi
import com.lib.gocam.BuildConfig
import java.io.*
import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Pattern

class Utils {

    companion object{

        fun printMessage(Message: String) {
            if (BuildConfig.DEBUG) {
                println("@@@goCam@@@ : $Message")
            }
        }


        fun getBitmapFromPath(path: String?): Bitmap? {
            return try {
                var bitmap: Bitmap? = null
                val f = File(path)
                val options = BitmapFactory.Options()
                options.inPreferredConfig = Bitmap.Config.ARGB_8888
                bitmap = BitmapFactory.decodeStream(FileInputStream(f), null, options)
                bitmap
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }

        fun getUri(context: Context, bitmap: Bitmap): Uri? {
            val bytes = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytes)
            val path =
                MediaStore.Images.Media.insertImage(context.contentResolver, bitmap, "Title", null)
            return Uri.parse(path)
        }

        @Throws(IOException::class)
        fun getBitmap(context: Context, uri: Uri?): Bitmap? {
            return MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
        }

        fun isValidPanNumber(pan_number: String?): Boolean {
            val pattern = Pattern.compile("[A-Z]{5}[0-9]{4}[A-Z]{1}")
            val matcher = pattern.matcher(pan_number)
            return matcher.matches()
        }

        fun isValidPanDOB(pan_number: String?): Boolean {
            val pattern = Pattern.compile("^([0-2][0-9]||3[0-1])/(0[0-9]||1[0-2])/([0-9][0-9])?[0-9][0-9]\$")
            val matcher = pattern.matcher(pan_number)
            return matcher.matches()
        }

        fun isValidPanName(pan_number: String?): Boolean {
            val pattern = Pattern.compile("([^#]*Name)")
            val matcher = pattern.matcher(pan_number)
            return matcher.matches()
        }

        @RequiresApi(api = Build.VERSION_CODES.KITKAT)
        fun saveBitmap(bitmap: Bitmap, path: String?): String? {
            try {
                FileOutputStream(path).use { out ->
                    bitmap.compress(
                        Bitmap.CompressFormat.JPEG,
                        70,
                        out
                    ) // bmp is your Bitmap instance
                    // PNG is a lossless format, the compression factor (100) is ignored
                    return path
                }
            } catch (e: IOException) {
                e.printStackTrace()
                return ""
            }
        }

        @RequiresApi(api = Build.VERSION_CODES.KITKAT)
        fun saveBitmapCustom(bitmap: Bitmap, path: String?, quality: Int): String? {
            try {
                FileOutputStream(path).use { out ->
                    bitmap.compress(
                        Bitmap.CompressFormat.JPEG,
                        quality,
                        out
                    ) // bmp is your Bitmap instance
                    // PNG is a lossless format, the compression factor (100) is ignored
                    return path
                }
            } catch (e: IOException) {
                e.printStackTrace()
                return ""
            }
        }

        fun deleteFile(uri: Uri){
            val fileDelete = File(uri.path)
            if (fileDelete.exists()) {
                if (fileDelete.delete()) {
                    println("file Deleted :" + uri.path)
                } else {
                    println("file not Deleted :" + uri.path)
                }
            }
        }

        fun getDateTime(): String? {
            val dateFormat = SimpleDateFormat(
                "dd/MM/yyyy HH:mm:ss", Locale.getDefault())
            val date = Date()
            return dateFormat.format(date)
        }

        open fun getBitmapFromView(view: View): Bitmap? {
            view.setWillNotCacheDrawing(false)
            view.destroyDrawingCache()
            view.buildDrawingCache()

            val cachedImage: Bitmap = Bitmap.createBitmap(view.getDrawingCache())

            val bmap = Bitmap.createBitmap(
                view.getWidth(), view.getHeight(),
                Bitmap.Config.ARGB_8888
            )
            val offscreencanvas = Canvas(bmap)
            offscreencanvas.drawBitmap(cachedImage, 0f, 0f, null)


            /*var bitmap =
                Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            var canvas = Canvas(bitmap)
            view.draw(canvas)*/
            return bmap
        }

    }


}