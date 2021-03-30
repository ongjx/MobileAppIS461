package com.example.gitrich

import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
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
            api_call(res[0])
        }
    }

    fun api_call(text: String) {
        val jsonObject = JSONObject()

        val username = MySingleton.getUsername()
        val url = "http://10.0.2.2:8000/users/" + username + "/dialogflow"
        val payload = JSONObject()
        payload.put("text", text)
        payload.put("name", "Adhoc Voice Receipt")

        val jsonObjectRequest = JsonObjectRequest(
            Request.Method.POST, url, payload,
            { response ->
                val res = response.getInt("code")

                if (res == 201){
                    println("success")
                    val goBack = Intent()
                    goBack.putExtra("message", "Receipt Created")
                    setResult(RESULT_OK, goBack)
                    finish()
                } else {
                    println("failure")
                }
            },
            { error ->
                // TODO: Handle error
                Log.e("Error", error.toString())
            }
        )

        MySingleton.getInstance(this).addToRequestQueue(jsonObjectRequest)

    }
}