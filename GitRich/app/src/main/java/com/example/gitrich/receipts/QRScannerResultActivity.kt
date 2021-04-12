package com.example.gitrich.receipts

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.example.gitrich.R
import com.example.gitrich.request.RequestQueueSingleton
import com.google.android.material.textfield.TextInputEditText
import okhttp3.OkHttpClient
import org.json.JSONObject

class QRScannerResultActivity : AppCompatActivity() {
    private lateinit var categories: Array<String>
    private var json = JSONObject()
    private var amount = ""
    private var date = ""
    private var store = ""
    private var desc = ""
    private var category = ""
    private lateinit var spinner: AutoCompleteTextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_receipt)
        supportActionBar!!.title = "Confirm Receipt"
        categories = resources.getStringArray(R.array.Categories)
        spinner = findViewById(R.id.receipt_category)
        val arrayAdapter = ArrayAdapter(this, R.layout.dropdown_items, categories)
        spinner.setAdapter(arrayAdapter)

        var amountElement = findViewById<TextInputEditText>(R.id.totalAmount)
        var dateElement = findViewById<TextInputEditText>(R.id.receipt_date)
        var storeElement = findViewById<TextInputEditText>(R.id.receipt_name)
        var descElement = findViewById<TextInputEditText>(R.id.create_desc)
        var catElement = findViewById<AutoCompleteTextView>(R.id.receipt_category)

        val intent = intent.extras
        if (intent != null) {
            val data = intent.getString("data")!!
            json = JSONObject(data)
            amount = json.get("amount") as String
            date = json.get("date") as String
            store = json.get("name") as String
            category = json.get("category") as String
            var items = json.get("items") as JSONObject
            if (items.length() > 0) {
                for (i in 0 until items.names().length()) {
                    val key = items.names()[i] as String
                    val value = items.getString(key)
                    desc += "${key},$${value}\n"
                }
            }


            amountElement.setText(amount)
            dateElement.setText(date)
            storeElement.setText(store)
            descElement.setText(desc)
            catElement.setText(category)
        }
    }

    fun save(view: View) {
        //TODO: Handle receipt submission to backend
        val client = OkHttpClient()
        val user = RequestQueueSingleton.getUsername()
        val url = "http://ec2-18-136-119-32.ap-southeast-1.compute.amazonaws.com:8000/users/${user}/qr-receipts"

        // Check if value on form has been edited
        amount = findViewById<TextInputEditText>(R.id.create_amount).text.toString()
        date = findViewById<TextInputEditText>(R.id.create_date).text.toString()
        store = findViewById<TextInputEditText>(R.id.create_store).text.toString()
        desc = findViewById<TextInputEditText>(R.id.create_desc).text.toString()
        category = findViewById<AutoCompleteTextView>(R.id.receipt_category).text.toString()
        val items = desc.splitToSequence("\n")
        val itemsObject = JSONObject()
        for (item: String in items) {
            if (item != "") {
                val l = item.split(',')
                val name = l[0].trim()
                val amount = if ("$" in l[1]) l[1].trim() else ("$${l[1].trim()}")
                itemsObject.put(l[0].trim(), l[1])
            }
        }
        json.put("name", store)
        json.put("amount", amount)
        json.put("items", itemsObject)
        json.put("date", date)
        json.put("category", category)

        val jsonObjectRequest = JsonObjectRequest(
                Request.Method.POST, url, json,
                { response ->
                    val res = response.getInt("code")

                    if (res == 201) {
                        val goBack = Intent()
                        goBack.putExtra("message", "Receipt Created")
                        setResult(RESULT_OK, goBack)
                        finish()
                    }
                },
                { error ->
                    // TODO: Handle error
//                    Log.e("Error", error.toString())
                }
        )

        RequestQueueSingleton.getInstance(this).addToRequestQueue(jsonObjectRequest)
        setResult(RESULT_OK)
        finish()
    }

    override fun onBackPressed() {
        super.onBackPressed()
//        Toast.makeText(this,"Back Pressed", Toast.LENGTH_SHORT).show()
        setResult(RESULT_CANCELED)
        finish()
    }
}

