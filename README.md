# GoCam SDK

Welcome to the **GoCam SDK** GitHub repository! This open-source Android SDK offers a suite of powerful image and video processing features, tailored for developers who need to capture and process media efficiently within their applications. The SDK comes with out-of-the-box features such as OCR, QR scanning, document detection, and more, to empower your Android applications with advanced media handling capabilities.

## Features

### 1. Video Capture
Capture high-quality videos directly from the camera. This feature allows seamless video recording, with easy integration into your Android app.

### 2. Image Capture
Capture images with enhanced accuracy and clarity. Perfect for applications requiring quick photo snaps or advanced image processing.

### 3. Optical Character Recognition (OCR)
Extract text from images with our integrated OCR technology. This feature supports multiple languages and character sets, making it easy to digitize physical documents.

### 4. QR Scanner
Easily scan and decode QR codes using your device's camera. This feature can be used in a variety of applications, from payments to information retrieval.

### 5. Blank Document Detection
Automatically detect and discard blank or irrelevant documents during image capture, ensuring that only meaningful content is saved.

### 6. Document Detection
Accurately detect and outline documents within the camera frame, making it easier to scan and save important papers.

### 7. Generate PDF using Images
Convert captured images into a high-quality PDF document. This feature allows you to bundle multiple images into a single PDF, with options for customization.

### 8. Location Adding on Images
Embed location information (latitude and longitude) directly into the captured images. This is useful for applications that need to track where photos were taken.

### 9. Lat/Long Adding on Images
In addition to the location, add specific latitude and longitude coordinates onto your images for precise geotagging.

### 10. Customized Watermark Adding in PDF
Add custom watermarks to your PDFs generated using the SDK. This feature supports text, images, and more, giving you control over the branding and security of your documents.

### 11. Aadhar Number Masking
Automatically detect and mask Aadhar numbers in images, ensuring privacy and compliance with data protection regulations.

## Getting Started

### Prerequisites

- **Android Studio**: Ensure you have the latest version of Android Studio installed.
- **Minimum SDK Version**: 21 (Android 5.0 Lollipop)
- **Permissions**: The SDK requires camera and storage permissions. Ensure that these are handled in your app.

### Installation

1. **Clone the Repository**

   ```sh
   git clone https://github.com/yourusername/gocam-sdk.git
   ```

2. **Add the SDK to your project**
   
   - Navigate to your Android project directory.
   - Open the `build.gradle` file for your app module.
   - Add the following line to include the GoCam SDK:

   ```groovy
   implementation project(path: ':gocam-sdk')
   ```

3. **Sync your project** to ensure that all dependencies are correctly added.

### Usage

Detailed usage examples for each feature are described above.

Here's a quick example of how to integrate the image capture feature:

```java
import com.gocam.sdk.GoCam;

GoCam goCam = new GoCam(this);
goCam.captureImage(new GoCam.ImageCaptureCallback() {
    @Override
    public void onImageCaptured(Bitmap image) {
        // Handle the captured image
    }

    @Override
    public void onError(String error) {
        // Handle any errors
    }
});
```

### Sample App

A sample app demonstrating the usage of the SDK is available in the `examples` directory. You can run this app to see the SDK features in action.

## Contributing

We welcome contributions to the GoCam SDK! If you'd like to contribute, please follow these steps:

1. **Fork the repository** to your own GitHub account.
2. **Create a branch** for your feature or bug fix.
3. **Commit your changes** and push your branch to your fork.
4. **Submit a pull request** to this repository with a clear description of your changes.

### Bug Reports & Feature Requests

If you encounter any issues or have a feature request.

## License

This project is licensed under the MIT License, its a opensource so you can change the code and distribute it further.

## Support

If you have any questions or need support, please feel free to reach out to us via email-id "SOCIAL9741atGMAILdotCOM".

---

Thank you for using the GoCam SDK! We look forward to your feedback and contributions.
