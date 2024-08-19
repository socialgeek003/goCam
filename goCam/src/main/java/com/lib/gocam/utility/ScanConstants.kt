package com.lib.gocam.utility

interface ScanConstants {
    companion object {
        const val SCANNED_RESULT = "scannedResult"
        const val CAMERA_REQUEST = 100
        const val RESULT_FRAGMENT = "resultFragment"
        const val CODE = "code"
        const val MESSAGE = "message"
        const val IMAGE_PROCESS = "imageProcess"
        const val REQUEST = "request"
        const val AUTO_CROP_FRAGMENT = "cropFragment"
        const val BRIGHTNESS_FRAGMENT = "brightnessFragment"
        const val LAT = "lat"
        const val LNG = "lng"
        const val ACCURACY = "accuracy"
        const val MASKING_RESULT = "maskingResult"
        const val REQUEST_AADHAR_MASKING = 111
        const val ADD = "add"
        const val REPLACE = "replace"
        const val REMOVE = "remove"
        const val IMAGE = "image"
        const val PDF = "pdf"

        //OCR
        var MASKING_DONE = false
        var PAN_OCR_COUNT = 500

        //Aadhar Data
        const val AADHAAR_DATA_TAG = "PrintLetterBarcodeData"
        const val AADHAR_UID_ATTR = "uid"
        const val AADHAR_NAME_ATTR = "name"
        const val AADHAR_GENDER_ATTR = "gender"
        const val AADHAR_YOB_ATTR = "yob"
        const val AADHAR_CO_ATTR = "co"
        const val AADHAR_VTC_ATTR = "vtc"
        const val AADHAR_PO_ATTR = "po"
        const val AADHAR_DIST_ATTR = "dist"
        const val AADHAR_STATE_ATTR = "state"
        const val AADHAR_PC_ATTR = "pc"
        const val AADHAR_STREET_ATTR = "street"
        const val AADHAR_LM_ATTR = "lm"
        const val AADHAR_LOC_ATTR = "loc"
        const val AADHAR_SUDIST_ATTR = "subdist"
        const val AADHAR_DOB_ATTR = "dob"
        const val AADHAR_HOUSE_ATTR = "house"
        const val AADHAR_MOTHER_ATTR = "gname"
        const val AADHAAR = "aadhaar"
        const val PAN = "pan"
        const val OTHER = "other"
        const val OCR_TYPE = "ocr_type"
        const val IMAGEPATH = "imagePath"
        const val SUCCESS = 1
        const val ERROR = 0
        const val SCAN_RESULT = "result"
        const val FLIP_VERTICAL = 1
        const val FLIP_HORIZONTAL = 2

        //Capture Sound
        const val CLICK_PHOTO = "click_photo"
        const val START_VIDEO = "start_video"
        const val STOP_VIDEO = "stop_video"

        //KEY'S For Intent
        const val FILE_TYPE = "fileType"
        const val SCAN_TYPE = "scan_type"
        const val CUSTOM_MESSAGE = "custom_message"
        const val ADDITIONAL_INFO = "additionalInfo"
        const val CAMERA_TYPE = "cameraType" // 1 for back 2 for front
        const val CAPTURE_TYPE = "captureType"
        const val VIDEO_MAX_TIME = "videoMaxTime"
        const val LOCATION = "location"
        const val PAN_OCR = "panOcr"
        const val PDF_NAME = "pdfName"
        const val IMAGE_COUNT = "imageCount"
        const val IMAGE_PATH = "imagePath"
        const val IMAGE_DATA = "imageData"
        const val IMAGE_NAME = "imageName"
        const val PDF_CREATE = "pdfCreate"
        const val IS_UPLOAD_AADHAR = "isUploadAadhar"
        const val PDF_PATH = "pdfPath"
        const val IS_DOC = "isDoc"
        const val IS_BLANK_DOC = "isBlankDoc"
        const val ADD_LOCATION = "addLocation"
        const val IS_AADHAR_MASKING = "isAadharMasking"
        const val GUIDELINES_ENABLE = "guidelinesEnable"
        const val GUIDELINES_ORIENTATION = "guidelinesOrientation"
        const val GUIDELINES_MESSAGE = "guidelinesMessage"

        //Watermark Keys
        const val WATERMARK_MESSAGE = "watermarkMessage"
        const val ADDITIONAL_WATERMARK = "additionalWatermark"
        const val WATERMARK_FONT_SIZE = "watermarkFontSize"
        const val WATERMARK_ALIGN_X = "watermarkAlignX"
        const val WATERMARK_ALIGN_Y = "watermarkAlignY"
        const val WATERMARK_ROTATION = "watermarkRotation"

        //Liveness Photo Copy
        const val IMAGE_COPY_BEFORE_LOCATION = "imageCopyBeforeLocation"
    }
}