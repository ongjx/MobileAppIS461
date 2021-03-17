package com.example.gitrich

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.gitrich.databinding.ActivityOCRScannerBinding
import java.io.ByteArrayOutputStream
import java.io.InputStream

class OCRScannerActivity : AppCompatActivity() {
    private lateinit var binding: ActivityOCRScannerBinding
    private var encodedImage_BASE64 = ""
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOCRScannerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val intent = intent.extras

        if (intent != null) {
            val image_uri = intent.getParcelable<Uri>("image_uri")
            val imageStream: InputStream? = contentResolver.openInputStream(image_uri!!)
            val selectedImage = BitmapFactory.decodeStream(imageStream)
            binding.imageView.setImageBitmap(selectedImage)
            encodedImage_BASE64 = encodeImage(selectedImage)

        }
    }

    private fun encodeImage(bm: Bitmap): String {
        val baos = ByteArrayOutputStream()
        bm.compress(Bitmap.CompressFormat.JPEG, 100, baos)
        val b = baos.toByteArray()
        val encImage = Base64.encodeToString(b, Base64.DEFAULT)
        return encImage
    }

    fun retry(view: View) {
        finish()
    }

    fun processReceipt(view: View) {
        //TODO: Send base64 image string to backend ocr service
        Toast.makeText(this, encodedImage_BASE64, Toast.LENGTH_SHORT).show()

        Toast.makeText(this, "Sent to process", Toast.LENGTH_SHORT).show()
        finish()
    }
}