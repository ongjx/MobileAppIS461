package com.example.gitrich

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.example.gitrich.models.Receipt
import com.google.android.material.textfield.TextInputEditText
import com.google.gson.JsonObject
import org.json.JSONObject
import java.util.*

class OCRScannerResultActivity : AppCompatActivity() {
    private val username = MySingleton.getUsername()
    private lateinit var categories: Array<String>;
    private var json = JSONObject();
    private var amount = ""
    private var date = ""
    private var store = ""
    private var desc = ""
    private var category = "Food & Drinks"
    private var id = ""
    private var image = ""
    private lateinit var spinner: AutoCompleteTextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_receipt)

        supportActionBar!!.title = "Confirm Receipt";
        categories = resources.getStringArray(R.array.Categories)
        spinner = findViewById(R.id.receipt_category)
        val arrayAdapter = ArrayAdapter(this, R.layout.dropdown_items, categories)
        spinner.setAdapter(arrayAdapter)

        // Get receipt data from API
        if (intent != null) {
            if (intent.extras != null) {
                println("in extras")
                store = intent.extras?.getString("name").toString()
                amount = intent.extras?.getString("amount").toString()
                date = intent.extras?.getString("date").toString()
                image = intent.extras?.getString("image").toString()


                var amountElement = findViewById<TextInputEditText>(R.id.totalAmount)
                var dateElement = findViewById<TextInputEditText>(R.id.receipt_date)
                var storeElement = findViewById<TextInputEditText>(R.id.receipt_name)
                var descElement = findViewById<TextInputEditText>(R.id.create_desc)
                var catElement = findViewById<AutoCompleteTextView>(R.id.receipt_category)

                val items = JSONObject(intent.extras?.getString("items").toString())
                if (items.length() != 0) {
                    for (i in 0 until items.names().length()) {
                        val key = items.names()[i] as String
                        val value = items.getString(key)
                        desc += "${key},$${value}\n"
                    }
                }

                category = catElement.text.toString()

                amountElement.setText(amount)
                dateElement.setText(date.split(" ")[0])
                storeElement.setText(store)
                descElement.setText(desc)
                catElement.setText(category)

            }

        }
    }


    fun save(view: View) {
        val url = "http://ec2-18-136-119-32.ap-southeast-1.compute.amazonaws.com:8000/users/${username}/qr-receipts"

        var amountElement = findViewById<TextInputEditText>(R.id.totalAmount)
        var dateElement = findViewById<TextInputEditText>(R.id.receipt_date)
        var storeElement = findViewById<TextInputEditText>(R.id.receipt_name)
        var descElement = findViewById<TextInputEditText>(R.id.create_desc)
        var catElement = findViewById<AutoCompleteTextView>(R.id.receipt_category)

        json.put("amount", amountElement.text.toString())
        // parse off the 00:00:00
        if (dateElement.text!!.contains(" ")) {
            date = dateElement.text.toString().split(" ")[0]
        } else {
            date = dateElement.text.toString()
        }
        json.put("date", dateElement.text.toString())
        json.put("name", storeElement.text.toString())

        val items = JSONObject()
        val scanner = Scanner(descElement.text.toString())
        while (scanner.hasNextLine()) {
            val item = scanner.nextLine()
            val itemList = item.split(",")
            items.put(itemList[0], itemList[1])
        }

        json.put("category", catElement.text.toString())
        json.put("image", image)
        json.put("items", items)

        val jsonObjectRequest = JsonObjectRequest(
                Request.Method.POST, url, json,
                { response ->
                    val res = response.getInt("code")

                    if (res == 201) {
                        val goBack = Intent()
                        setResult(RESULT_OK)
                        finish()
                    }
                },
                { error ->
                    // TODO: Handle error
//                    Log.e("Error", error.toString())
                }
        )

        MySingleton.getInstance(this).addToRequestQueue(jsonObjectRequest)
    }
}