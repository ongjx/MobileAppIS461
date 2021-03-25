package com.example.gitrich

import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import okhttp3.*
import java.io.IOException
import java.util.*
import com.example.gitrich.models.Receipt
import com.google.gson.GsonBuilder
import org.json.JSONObject



class Voice : AppCompatActivity() {
    lateinit var btn : Button
    lateinit var txt : TextView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_voice)
        btn = findViewById(R.id.btnn)
        txt = findViewById(R.id.text)
        btn.setOnClickListener {

            speechToText()
        }
    }
    fun speechToText(){
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT,"Say Something")
        startActivityForResult(intent,100)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode == 100 || data != null){
            val res : ArrayList<String> = data!!.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS) as ArrayList<String>
            txt.text = res[0]
            Log.v("printing", "calling")
            api_call(res[0])
        }
    }

    fun api_call(text: String) {
        val client = OkHttpClient();
        val jsonObject = JSONObject()
        jsonObject.put("text", text)
        jsonObject.put("name", "Adhoc Receipt Mobile")
        val JSON = MediaType.parse("application/json; charset=utf-8")
        val body = RequestBody.create(JSON, jsonObject.toString());

        val url = "http://192.168.10.145:8000/users/kelvinngsl/dialogflow"
        val request = Request.Builder()
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
                    } else {
                        println("failure")
                    }
                }
            }
        })
    }
}