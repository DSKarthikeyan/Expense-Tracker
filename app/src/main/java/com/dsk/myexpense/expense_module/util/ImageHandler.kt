package com.dsk.myexpense.expense_module.util

import android.net.Uri

class ImageHandler {

    fun processImage(uri: Uri): String {
        // Example: Return the file path or image as a bitmap
        return uri.path ?: "Invalid Path"
    }

    fun uploadToServer(filePath: String) {
        // Logic to upload the image to a server
    }
}