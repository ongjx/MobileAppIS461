package com.example.gitrich.receipts

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.gitrich.databinding.ActivityOCRScannerBinding
import com.example.gitrich.request.RequestQueueSingleton
import okhttp3.MediaType
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import okhttp3.*
import java.io.File
import java.io.IOException

private const val OCR_RESULT_CODE = 1007

class OCRScannerActivity : AppCompatActivity() {
    private lateinit var binding: ActivityOCRScannerBinding
    private var encodedImage_BASE64 = ""
    private var filepath = ""
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOCRScannerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val intent = intent.extras
        if (intent != null) {
            filepath = intent.getString("filepath").toString()

            val root = getExternalFilesDir(RequestQueueSingleton.getUsername())
            val imgFile = File(root, filepath)

            if (imgFile.exists()) {
                val decodedByte = BitmapFactory.decodeFile(imgFile.absolutePath)
                binding.imageView.setImageBitmap(decodedByte)

                encodedImage_BASE64 = "data:image/jpg;base64," + encodeImage(decodedByte)
            }
        }
    }

    fun openGallery(view: View){
        val intent = Intent("android.intent.action.GET_CONTENT")
        intent.type = "image/*"
        startActivityForResult(intent, 999)
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
        post_ocr_receipt()

    }

    fun post_ocr_receipt() {
        val username = RequestQueueSingleton.getUsername()
        val client = OkHttpClient()

        // val url = "http://ec2-18-136-119-32.ap-southeast-1.compute.amazonaws.com:8000/users/" + username + "/ocr-receipts"
        val url = "http://ec2-18-136-119-32.ap-southeast-1.compute.amazonaws.com:8000/users/" + username + "/ocr-receipts"
        val payload = JSONObject()
        payload.put("image", encodedImage_BASE64)
        payload.put("filepath", filepath)

        val JSON = MediaType.parse("application/json; charset=utf-8")
        val body = RequestBody.create(JSON, payload.toString())

        val request = okhttp3.Request.Builder()
                .url(url)
                .post(body)
                .build()

        client.newCall(request).enqueue(object: Callback {
            override fun onFailure(call: Call, e: IOException) {
                println("failed to execute request")

            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body()?.string()
                if (body != null) {
                    val status = JSONObject(body).getInt("code")
                    if (status == 200) {
                        val data = JSONObject(body).getJSONObject("data")
                        val goBack = Intent()

                        // name
                        goBack.putExtra("name", data.getString("name"))
                        // amount
                        goBack.putExtra("amount", data.getString("amount"))
                        // date
                        goBack.putExtra("date", data.getString("date"))
                        // items
                        goBack.putExtra("items", data.getJSONObject("items").toString())
                        // image
                        goBack.putExtra("image", data.getString("image"))
                        // category
                        goBack.putExtra("category", data.getString("category"))

                        setResult(RESULT_OK, goBack)
                        finish()

                    } else {
                        println("failure")
                    }
                }
            }
        })
    }
}