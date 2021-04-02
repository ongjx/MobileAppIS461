package com.example.gitrich

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.example.gitrich.databinding.ActivityOCRScannerBinding
import okhttp3.MediaType
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.InputStream
import okhttp3.*
import java.io.IOException

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
            val bitmap = Bitmap.createScaledBitmap(selectedImage, 768, 1024, true)
            binding.imageView.setImageBitmap(bitmap)
            encodedImage_BASE64 = "data:image/jpg;base64," + encodeImage(bitmap)
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
        // TODO: Send base64 image string to backend ocr service
        // TODO: After that show the receipt in a view
        // TODO: Go back main screen and refresh
        println("processing")
        post_ocr_receipt(encodedImage_BASE64)


//        setResult(RESULT_OK, goBack)
//        finish()
    }

    fun post_ocr_receipt(base64_image: String) {
        val username = MySingleton.getUsername()
        val client = OkHttpClient()

        // val url = "https://gitrich-backend.herokuapp.com/users/" + username + "/ocr-receipts"
        val url = "http://10.0.2.2:8000/users/" + username + "/ocr-receipts"
        val payload = JSONObject()
        payload.put("image", base64_image)

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
                val body = response?.body()?.string()
                if (body != null) {
                    val status = JSONObject(body).getInt("code")
                    if (status == 201) {
                        println("success")
                        // go to ScannerResult
//                        val intent = Intent(this@OCRScannerActivity, OCRScannerResultActivity::class.java)
//                        intent.putExtra("data", JSONObject(body).getJSONObject("data").toString())
//                        startActivityForResult(intent, 777)

                        val goBack = Intent()
                        // Get ID
                        goBack.putExtra("id", JSONObject(body).getJSONObject("data").getString("id"))
                        setResult(RESULT_OK, goBack)
                        finish()

                    } else {
                        println("failure")
                    }
                }
            }
        })

//        val jsonObjectRequest = JsonObjectRequest(
//                Request.Method.POST, url, payload,
//                { response ->
//                    val res = response.getInt("code")
//
//                    if (res == 201){
//                        println("success")
//                        val result = response.getJSONObject("data").toString()
//                        println(result)
//                        Toast.makeText(this, "Success! Receipt Created!", Toast.LENGTH_SHORT).show()
////                        val it = Intent(this, OCRScannerResultActivity::class.java)
////                        it.putExtra("data", result)
////                        startActivityForResult(it, 888)
//
//                    } else {
//                        println("failure")
//                        Toast.makeText(this, "Failure! Receipt Not Created!", Toast.LENGTH_SHORT).show()
//                    }
//                },
//                { error ->
//                    // TODO: Handle error
//                    Log.e("Error", error.toString())
//                }
//        )
//        MySingleton.getInstance(this).addToRequestQueue(jsonObjectRequest)
    }
}