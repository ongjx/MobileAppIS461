package com.example.gitrich.receipts

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.gitrich.R
import com.example.gitrich.request.RequestQueueSingleton
import okhttp3.*
import org.json.JSONObject
import java.io.IOException


class Voice : AppCompatActivity() {
    private lateinit var btnCancel : Button
    private lateinit var btnConfirm : Button
    private lateinit var speechTv : TextView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_voice)
        btnCancel = findViewById(R.id.btnCancel)
        btnConfirm = findViewById(R.id.btnConfirm)
        speechTv = findViewById(R.id.speech)

        val extras = intent.extras
        if (extras != null) {
            speechTv.text = extras.getString("speech")
        }
    }

    fun postVoiceReceipt() {
        val speech = speechTv.text.toString()
        val username = RequestQueueSingleton.getUsername()
        val client = OkHttpClient()

        // Get editTextName
        var name = findViewById<EditText>(R.id.editTextName).text.toString()
        if (name == "") {
            name = "Adhoc Voice Receipt"
        }

        val url = "http://ec2-18-136-119-32.ap-southeast-1.compute.amazonaws.com:8000/users/" + username + "/dialogflow"
        val payload = JSONObject()
        payload.put("text", speech)
        payload.put("name", name)
        payload.put("image", "")
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
                    if (status == 201) {
                        println("success voice dialogflow call")
                        setResult(RESULT_OK)
                        finish()
                    } else {
                        println("failure voice dialogflow call")
                        setResult(RESULT_CANCELED)
                        finish()
                    }
                }
            }
        })
    }

    fun confirm(view: View) {
        // api call
        postVoiceReceipt()
    }
    fun cancel(view: View) {
        // return
        setResult(RESULT_CANCELED)
        finish()
    }
}