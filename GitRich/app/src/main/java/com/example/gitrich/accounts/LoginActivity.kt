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
import com.example.gitrich.databinding.ActivityLoginBinding
import com.example.gitrich.request.RequestQueueSingleton
import org.json.JSONObject

private const val SIGNUP_CODE = 1004

class LoginActivity : AppCompatActivity() {
    private lateinit var binding:ActivityLoginBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        supportActionBar?.hide()
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    fun signUpAccount(view: View) {
        val it = Intent(this, SignupActivity::class.java)
        startActivityForResult(it, SIGNUP_CODE)
    }

    fun login(view: View) {
        //Call API to login
        val username = binding.editTextUsername.text.toString()
        val password = binding.editTextPassword.text.toString()

        val goBack = Intent()
        if (username == "" || password == "") {
            Toast.makeText(this, "Please do not leave any inputs blank", Toast.LENGTH_SHORT).show()
            return
        }

        // val url = "http://ec2-18-136-119-32.ap-southeast-1.compute.amazonaws.com:8000/users/${binding.editTextUsername.text}/login"
        val url = "http://ec2-18-136-119-32.ap-southeast-1.compute.amazonaws.com:8000/users/${binding.editTextUsername.text}/login"
        //val url = "http://10.0.2.2:8000/users/${binding.editTextUsername.text}/login"
        val payload = JSONObject()
        payload.put("password", password)

// Formulate the request and handle the response.
        val jsonObjectRequest = JsonObjectRequest(Request.Method.POST, url, payload,
            { response ->
                val res = response.getInt("code")

                if (res == 200){
                    val data = response.getJSONObject("data")
                    val username = data.optString("username")
                    goBack.putExtra("username", username)
                    println("this is the username before passing: " + username)
                    setResult(RESULT_OK, goBack)
                    finish()
                }

                if (res == 400){
                    binding.textView.text = "Wrong Username/Password"
                }

            },
            { error ->
                // TODO: Handle error
//                Log.e("Error", error.toString())
                binding.textView.text = "Wrong Username/Password"
            }
        )

        RequestQueueSingleton.getInstance(this).addToRequestQueue(jsonObjectRequest)

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {

            if(requestCode == SIGNUP_CODE) {
                val username = data?.getStringExtra("username")
                val goBack = Intent()
                goBack.putExtra("username", username)
                setResult(RESULT_OK, goBack)
                finish()
            }

        }
    }


}