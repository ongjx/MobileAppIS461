package com.example.gitrich.accounts

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.view.Window
import android.widget.Toast
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.example.gitrich.databinding.ActivitySignupBinding
import com.example.gitrich.request.RequestQueueSingleton
import org.json.JSONObject

class SignupActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySignupBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        supportActionBar?.hide()
        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)

    }

    fun back(view: View) {
        val goBack = Intent()
        goBack.putExtra("back", "1")
        setResult(Activity.RESULT_CANCELED, goBack)
        finish()
    }

    fun signUp(view: View) {
        //Call API and return to main page.

        val username = binding.editTextUsername.text.toString()
        val password = binding.editTextPassword.text.toString()

        if (username == "" || password == "") {
            Toast.makeText(this, "Please do not leave any inputs blank", Toast.LENGTH_SHORT).show()
            return
        }

        val url = "http://ec2-18-136-119-32.ap-southeast-1.compute.amazonaws.com:8000/users/${binding.editTextUsername.text}/signup"
        val payload = JSONObject()
        payload.put("password", password)

// Formulate the request and handle the response.
        val jsonObjectRequest = JsonObjectRequest(
            Request.Method.POST, url, payload,
            { response ->
                val res = response.getInt("code")

                if (res == 201) {
                    val data = response.getJSONObject("data")
                    val username = data.optString("username")
                    val goBack = Intent()
                    goBack.putExtra("username", username)
                    setResult(RESULT_OK, goBack)

                    // Set username in global context
                    RequestQueueSingleton.setUsername(username)
                    finish()
                }

                if (res == 400){
                    binding.textView.text = "Error signing up"
                }
            },
            { error ->
                // TODO: Handle error
                //Log.e("Error", error.toString())
                binding.textView.text = "Error signing up"
            }
        )

        RequestQueueSingleton.getInstance(this).addToRequestQueue(jsonObjectRequest)
    }


}