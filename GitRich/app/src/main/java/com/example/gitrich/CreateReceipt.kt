package com.example.gitrich

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.annotation.RequiresApi
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.google.android.material.textfield.TextInputEditText
import com.google.gson.JsonObject
import java.lang.Exception
import java.util.regex.Pattern

class CreateReceipt : AppCompatActivity() {
    private lateinit var categories: Array<String>;
    private var amount = "";
    private var date = "";
    private var store = "";
    private var desc = "";
    private var category = "Food & Drinks";

    private lateinit var spinner: AutoCompleteTextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_receipt)
        supportActionBar!!.title = "Add New Receipt";
        categories = resources.getStringArray(R.array.Categories)
        spinner = findViewById(R.id.receipt_category)
        val arrayAdapter = ArrayAdapter(this, R.layout.dropdown_items, categories)
        spinner.setAdapter(arrayAdapter)

    }

    fun save(view: View) {

        amount = findViewById<TextInputEditText>(R.id.totalAmount).text.toString()
        date = findViewById<TextInputEditText>(R.id.receipt_date).text.toString()
        store = findViewById<TextInputEditText>(R.id.receipt_name).text.toString()
        desc = findViewById<TextInputEditText>(R.id.create_desc).text.toString()
        category = spinner.text.toString()

        var itemsObject = JSONObject()

        if (desc.equals("")) {
            // do nothing
        } else {
            try {
                val items = desc.splitToSequence("\n")

                for (item: String in items) {
                    val l = item.split(',')
                    val name = l[0].trim()
                    val amount = if ("$" in l[1]) l[1].trim() else ("$${l[1].trim()}")

                    itemsObject.put(l[0].trim() as String, l[1])
                }
            } catch (e: Exception) {
                // We can choose to stop processing unless its empty or appropriate desc is entered
                println("Inappropriate Description")
                itemsObject = JSONObject()
            }
        }


        val dateRegex = "^([0-2][0-9]||3[0-1])/(0[0-9]||1[0-2])/([0-9][0-9])?[0-9][0-9]$"
        if (!Pattern.matches(dateRegex, date)){
            Toast.makeText(this, "Invalid date format", Toast.LENGTH_SHORT).show()
        }
        else if (amount == "" || date == "" || store == "") {
            Toast.makeText(this, "Please ensure that there are no empty fields!", Toast.LENGTH_SHORT).show()
        }
        else {
            val client = OkHttpClient();
            val jsonObject = JSONObject()

            jsonObject.put("name", store)
            jsonObject.put("amount", amount)
            jsonObject.put("items", itemsObject)
            jsonObject.put("image", "")
            jsonObject.put("date", date)
            jsonObject.put("category", category)

            val user = MySingleton.getUsername()
            val url = "http://ec2-18-136-119-32.ap-southeast-1.compute.amazonaws.com:8000/users/${user}/qr-receipts"
            val jsonObjectRequest = JsonObjectRequest(
                    Request.Method.POST, url, jsonObject,
                    { response ->
                        val res = response.getInt("code")
                        if (res == 201){
                            val goBack = Intent()
                            setResult(RESULT_OK)
                            finish()
                        }
                    },
                    { error ->
                        Log.e("Error", error.toString())
                    }
            )
            MySingleton.getInstance(this).addToRequestQueue(jsonObjectRequest)
        }


    }
}

