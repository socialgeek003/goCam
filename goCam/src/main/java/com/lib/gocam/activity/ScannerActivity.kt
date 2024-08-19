package com.lib.gocam.activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.budiyev.android.codescanner.*
import com.google.android.material.textview.MaterialTextView
import com.lib.gocam.R
import com.lib.gocam.utility.PanData
import com.lib.gocam.utility.ScanConstants.Companion.AADHAAR_DATA_TAG
import com.lib.gocam.utility.ScanConstants.Companion.AADHAR_CO_ATTR
import com.lib.gocam.utility.ScanConstants.Companion.AADHAR_DIST_ATTR
import com.lib.gocam.utility.ScanConstants.Companion.AADHAR_DOB_ATTR
import com.lib.gocam.utility.ScanConstants.Companion.AADHAR_GENDER_ATTR
import com.lib.gocam.utility.ScanConstants.Companion.AADHAR_HOUSE_ATTR
import com.lib.gocam.utility.ScanConstants.Companion.AADHAR_LM_ATTR
import com.lib.gocam.utility.ScanConstants.Companion.AADHAR_LOC_ATTR
import com.lib.gocam.utility.ScanConstants.Companion.AADHAR_MOTHER_ATTR
import com.lib.gocam.utility.ScanConstants.Companion.AADHAR_NAME_ATTR
import com.lib.gocam.utility.ScanConstants.Companion.AADHAR_PC_ATTR
import com.lib.gocam.utility.ScanConstants.Companion.AADHAR_PO_ATTR
import com.lib.gocam.utility.ScanConstants.Companion.AADHAR_STATE_ATTR
import com.lib.gocam.utility.ScanConstants.Companion.AADHAR_STREET_ATTR
import com.lib.gocam.utility.ScanConstants.Companion.AADHAR_SUDIST_ATTR
import com.lib.gocam.utility.ScanConstants.Companion.AADHAR_UID_ATTR
import com.lib.gocam.utility.ScanConstants.Companion.AADHAR_VTC_ATTR
import com.lib.gocam.utility.ScanConstants.Companion.AADHAR_YOB_ATTR
import com.lib.gocam.utility.ScanConstants.Companion.AADHAAR
import com.lib.gocam.utility.ScanConstants.Companion.CODE
import com.lib.gocam.utility.ScanConstants.Companion.CUSTOM_MESSAGE
import com.lib.gocam.utility.ScanConstants.Companion.ERROR
import com.lib.gocam.utility.ScanConstants.Companion.MESSAGE
import com.lib.gocam.utility.ScanConstants.Companion.OTHER
import com.lib.gocam.utility.ScanConstants.Companion.PAN
import com.lib.gocam.utility.ScanConstants.Companion.SCANNED_RESULT
import com.lib.gocam.utility.ScanConstants.Companion.SCAN_RESULT
import com.lib.gocam.utility.ScanConstants.Companion.SCAN_TYPE
import com.lib.gocam.utility.ScanConstants.Companion.SUCCESS
import com.lib.gocam.utility.Utils
import org.json.JSONException
import org.json.JSONObject
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import org.xmlpull.v1.XmlPullParserFactory
import java.io.IOException
import java.io.StringReader
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

class ScannerActivity : AppCompatActivity() {
    private lateinit var codeScanner: CodeScanner
    var uid = ""
    var name = ""
    var gender = ""
    var careOf = ""
    var villageTehsile = ""
    var postOffice = ""
    var yearOfBirth = ""
    var dob = ""
    var district = ""
    var state = ""
    var postCode = ""
    var house = ""
    var location = ""
    var subDist = ""
    var street = ""
    var landmark = ""
    var motherName = ""
    var panNO = ""
    var panName = ""
    var fatherName = ""
    var dobOfPan = ""
    var scanType = ""
    var customMessage = ""
    private var panDataArrayList: ArrayList<PanData>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.scanner_activity)

        if (supportActionBar != null) {
            supportActionBar!!.hide()
        }
        val intent = intent
        scanType = intent.getStringExtra(SCAN_TYPE)!!
        if (intent.getStringExtra(CUSTOM_MESSAGE) != null) {
            customMessage = intent.getStringExtra(CUSTOM_MESSAGE)!!
        }
        val scannerView = findViewById<CodeScannerView>(R.id.scanner_view)
        val tvCustomMessage = findViewById<MaterialTextView>(R.id.tv_custom_message)

        codeScanner = CodeScanner(this, scannerView)

        // Parameters (default values)
        codeScanner.camera = CodeScanner.CAMERA_BACK // or CAMERA_FRONT or specific camera id
        codeScanner.formats = CodeScanner.ALL_FORMATS // list of type BarcodeFormat,
        // ex. listOf(BarcodeFormat.QR_CODE)
        codeScanner.autoFocusMode = AutoFocusMode.SAFE // or CONTINUOUS
        codeScanner.scanMode = ScanMode.CONTINUOUS // or CONTINUOUS or PREVIEW
        codeScanner.isAutoFocusEnabled = true // Whether to enable auto focus or not
        codeScanner.isFlashEnabled = false // Whether to enable flash or not
        scannerView.isFlashButtonVisible = false // flash was not working
        Utils.printMessage("Scan Type:- $scanType")

        // Callbacks
        codeScanner.decodeCallback = DecodeCallback {
            runOnUiThread {
                //                Toast.makeText(this, result.getContents(), Toast.LENGTH_LONG).show();
                if (scanType == AADHAAR) {
                    if (checkIfXMLIsWellFormed(it.text)) {
                        Utils.printMessage("My Data: ${it.text}")
                        processScannedAadhaarData(it.text.replace("<?xml version=\"1.0\" encoding=\"UTF-8\"?>", "").trim())
                    } else {
                        createJSONData(ERROR, 102, "Unable to scan Aadhaar Card")
                    }
                } else if (scanType == PAN) {
                    panDataArrayList = ArrayList<PanData>()
                    processPanData(it.text)
                } else if (scanType == OTHER) {
                    processScannedOtherData(it.text)
                } else {
                    createJSONData(
                        ERROR,
                        109,
                        "This is Aadhaar Card Please Select Aadhaar option to scan it"
                    )
                }
                //Toast.makeText(this, "Scan result: ${it.text}", Toast.LENGTH_LONG).show()
            }
        }
        codeScanner.errorCallback = ErrorCallback { // or ErrorCallback.SUPPRESS
            runOnUiThread {
                Toast.makeText(this, "Camera initialization error: ${it.message}",
                    Toast.LENGTH_LONG).show()
            }
        }

        if (customMessage.isNotEmpty()){
            tvCustomMessage.text = customMessage
            tvCustomMessage.visibility = View.VISIBLE
        }else{
            tvCustomMessage.visibility = View.GONE
        }

        scannerView.setOnClickListener {
            codeScanner.startPreview()
        }
    }

    override fun onResume() {
        super.onResume()
        codeScanner.startPreview()
    }

    override fun onPause() {
        codeScanner.releaseResources()
        super.onPause()
    }

    private fun processPanData(scanData: String) {
        if (scanData.contains("PAN")) {
            Utils.printMessage("Scan Process:- $scanData")
            val names = scanData.split("\n").toTypedArray()
            var data: Array<String?>
            for (i in names.indices) {
                data = names[i].split(":").toTypedArray()
                Utils.printMessage("Name!" + names[i])
                val panData = PanData()
                panData.name = data[0].toString()
                panData.value = data[1].toString()
                panDataArrayList!!.add(panData)
            }
            for (i in panDataArrayList!!.indices) {
                if (panDataArrayList!![i].name.lowercase().contains("father")) {
                    fatherName = panDataArrayList!![i].value
                } else if (panDataArrayList!![i].name.lowercase().contains("name")) {
                    panName = panDataArrayList!![i].value
                } else if (panDataArrayList!![i].name.lowercase()
                        .contains("date of birth")
                ) {
                    dobOfPan = panDataArrayList!![i].value
                } else if (panDataArrayList!![i].name.lowercase().contains("pan")) {
                    panNO = panDataArrayList!![i].value
                }
            }
            try {
                dobOfPan = formatDate(dobOfPan).toString()
            } catch (e: ParseException) {
                createJSONData(
                    ERROR, 104,
                    "Expected dob to be in dd/mm/yyyy or yyyy-mm-dd format, got $dobOfPan"
                )
                System.err.println("Expected dob to be in dd/mm/yyyy or yyyy-mm-dd format, got $dobOfPan")
            }
            if (panNO != null) {
                if (Utils.isValidPanNumber(panNO.trim { it <= ' ' })) {
                    createJSONData(SUCCESS, 100, "Data get Successfully")
                } else {
                    createJSONData(ERROR, 101, "Not a Pan Card")
                }
            } else {
                createJSONData(ERROR, 101, "Not a Pan Card")
            }
        } else {
            createJSONData(ERROR, 101, "Not a Pan Card")
        }
    }

    fun checkIfXMLIsWellFormed(aXml: String?): Boolean {
        return aXml != null && aXml.trim { it <= ' ' }.isNotEmpty() && aXml.trim { it <= ' ' }
            .startsWith("<") && aXml.trim { it <= ' ' }
            .endsWith(">")
    }

    fun createJSONData(status: Int, code: Int, message: String?) {
        val jsonObject = JSONObject()
        when (status) {
            SUCCESS -> try {
                jsonObject.put(CODE, code)
                jsonObject.put(MESSAGE, message)
                when (scanType) {
                    AADHAAR -> {
                        jsonObject.put("UID", uid)
                        jsonObject.put("Name", name)
                        jsonObject.put("Gender", gender)
                        jsonObject.put("YOB", yearOfBirth)
                        jsonObject.put("CO", careOf)
                        jsonObject.put("MN", motherName)
                        jsonObject.put("VTC", villageTehsile)
                        jsonObject.put("PO", postOffice)
                        jsonObject.put("Dist", district)
                        jsonObject.put("State", state)
                        jsonObject.put("PC", postCode)
                        jsonObject.put("House", house)
                        jsonObject.put("Street", street)
                        jsonObject.put("LM", landmark)
                        jsonObject.put("LC", location)
                        jsonObject.put("Subdist", subDist)
                        jsonObject.put("DOB", dob)
                    }
                    PAN -> {
                        jsonObject.put("name", panName.trim { it <= ' ' })
                        jsonObject.put("fatherName", fatherName.trim { it <= ' ' })
                        jsonObject.put("dob", dobOfPan.trim { it <= ' ' })
                        jsonObject.put("panNo", panNO.trim { it <= ' ' })
                    }
                }
            } catch (e: JSONException) {
                e.printStackTrace()
            }
            ERROR -> try {
                jsonObject.put(CODE, code)
                jsonObject.put(MESSAGE, message)
            } catch (e: JSONException) {
                e.printStackTrace()
            }
        }
        val intent = Intent()
        intent.putExtra(SCANNED_RESULT, jsonObject.toString())
        setResult(RESULT_OK, intent)
        finish()
    }

    private fun getAttributeValueEmptyString(
        parser: XmlPullParser,
        attributeName: String
    ): String? {
        val value = parser.getAttributeValue(null, attributeName)
        return value ?: ""
    }


    @Throws(ParseException::class)
    private fun formatDate(rawDateString: String?): String? {
        return if (rawDateString != null) {
            if (rawDateString == "") {
                return ""
            }
            val toFormat = SimpleDateFormat("yyyy-MM-dd")
            val possibleFormats = arrayOf(
                SimpleDateFormat("dd/MM/yyyy"),
                SimpleDateFormat("yyyy-MM-dd")
            )
            var date: Date? = null
            var parseException: ParseException? = null
            for (fromFormat in possibleFormats) {
                try {
                    date = fromFormat.parse(rawDateString)
                    break
                } catch (e: ParseException) {
                    parseException = e
                }
            }
            if (date != null) {
                toFormat.format(date)
            } else if (parseException != null) {
                throw parseException
            } else {
                throw AssertionError("This code is unreachable")
            }
        } else {
            ""
        }
    }


    private fun processScannedAadhaarData(scanData: String?) {
        val pullParserFactory: XmlPullParserFactory
        try {
            Utils.printMessage("My Data Parse: $scanData")
            pullParserFactory = XmlPullParserFactory.newInstance()
            val parser = pullParserFactory.newPullParser()
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            parser.setInput(StringReader(scanData))
            var eventType = parser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                var tagName = parser.name
                println("AdharUID= $tagName")
                when (eventType) {
                    XmlPullParser.START_DOCUMENT -> {
                    }
                    XmlPullParser.START_TAG -> {
                        tagName = parser.name
                        if (tagName == AADHAAR_DATA_TAG) {
                            uid = getAttributeValueEmptyString(parser, AADHAR_UID_ATTR).toString()
                            name = getAttributeValueEmptyString(parser, AADHAR_NAME_ATTR).toString()
                            var rawGender: String = getAttributeValueEmptyString(parser, AADHAR_GENDER_ATTR).toString()
                            try {
                                rawGender = formatGender(rawGender).toString()
                            } catch (e: ParseException) {
                                createJSONData(
                                    ERROR, 103,
                                    "Expected gender to be one of m, f, male, female; got $rawGender"
                                )
                            }
                            gender = rawGender
                            yearOfBirth = getAttributeValueEmptyString(parser, AADHAR_YOB_ATTR).toString()
                            careOf = getAttributeValueEmptyString(parser, AADHAR_CO_ATTR).toString()
                            villageTehsile = getAttributeValueEmptyString(parser, AADHAR_VTC_ATTR).toString()
                            postOffice = getAttributeValueEmptyString(parser, AADHAR_PO_ATTR).toString()
                            district = getAttributeValueEmptyString(parser, AADHAR_DIST_ATTR).toString()
                            state = getAttributeValueEmptyString(parser, AADHAR_STATE_ATTR).toString()
                            postCode = getAttributeValueEmptyString(parser, AADHAR_PC_ATTR).toString()
                            house = getAttributeValueEmptyString(parser, AADHAR_HOUSE_ATTR).toString()
                            street = getAttributeValueEmptyString(parser, AADHAR_STREET_ATTR).toString()
                            landmark = getAttributeValueEmptyString(parser, AADHAR_LM_ATTR).toString()
                            location = getAttributeValueEmptyString(parser, AADHAR_LOC_ATTR).toString()
                            subDist = getAttributeValueEmptyString(parser, AADHAR_SUDIST_ATTR).toString()
                            motherName = getAttributeValueEmptyString(parser, AADHAR_MOTHER_ATTR).toString()
                            var rawDob: String = getAttributeValueEmptyString(parser, AADHAR_DOB_ATTR).toString()
                            try {
                                rawDob = formatDate(rawDob).toString()
                            } catch (e: ParseException) {
                                createJSONData(
                                    ERROR, 104,
                                    "Expected dob to be in dd/mm/yyyy or yyyy-mm-dd format, got $rawDob"
                                )
                                System.err.println("Expected dob to be in dd/mm/yyyy or yyyy-mm-dd format, got $rawDob")
                            }
                            dob = rawDob
                        } else {
                            createJSONData(ERROR, 105, "Not an Aadhar Card")
                            println("Not an Aadhar Card")
                        }
                    }
                    XmlPullParser.END_TAG -> {
                    }
                }
                eventType = parser.next()
            }
            createJSONData(SUCCESS, 100, "Data get Successfully")
        } catch (e: XmlPullParserException) {
            e.printStackTrace()
            createJSONData(ERROR, 102, "NOt a Proper Data")
        } catch (e: IOException) {
            e.printStackTrace()
            createJSONData(ERROR, 102, "NOt a Proper Data")
        }
    }

    @Throws(ParseException::class)
    private fun formatGender(gender: String): String? {
        val lowercaseGender = gender.lowercase()
        return if (lowercaseGender == "male" || lowercaseGender == "m") {
            "M"
        } else if (lowercaseGender == "female" || lowercaseGender == "f") {
            "F"
        } else if (lowercaseGender == "other" || lowercaseGender == "o") {
            "O"
        } else {
            throw ParseException("404 gender not found", 0)
        }
    }

    private fun processScannedOtherData(scanData: String?) {
        createJSONData(SUCCESS, 100, scanData)
    }
}