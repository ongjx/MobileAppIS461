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
    private lateinit var spinner: Spinner

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_receipt)

        supportActionBar!!.title = "GitRich - Confirm Receipt";
        categories = resources.getStringArray(R.array.Categories)
        spinner = findViewById(R.id.create_category)

        // Get receipt data from API
        if (intent != null) {
            if (intent.extras != null) {
                println("in extras")
                store = intent.extras?.getString("name").toString()
                amount = intent.extras?.getString("amount").toString()
                date = intent.extras?.getString("date").toString()
                image = intent.extras?.getString("image").toString()

                var amountElement = findViewById<EditText>(R.id.create_amount)
                var dateElement = findViewById<EditText>(R.id.create_date)
                var storeElement = findViewById<EditText>(R.id.create_store)
                var descElement = findViewById<EditText>(R.id.create_desc)

                val items = JSONObject(intent.extras?.getString("items").toString())
                if (items.length() != 0) {
                    for (i in 0 until items.names().length()) {
                        val key = items.names()[i] as String
                        val value = items.getString(key)
                        desc += "${key},$${value}\n"
                    }
                }

                amountElement.setText(amount)
                dateElement.setText(date.split(" ")[0])
                storeElement.setText(store)
                descElement.setText(desc)

            }

        }

        if (spinner != null) {
            val adapter = object: ArrayAdapter<String>(
                    this,
                    android.R.layout.simple_spinner_item, categories
            ) {
                override fun getDropDownView(
                        position: Int,
                        convertView: View?,
                        parent: ViewGroup
                ): View {
                    val view: TextView = super.getDropDownView(
                            position,
                            convertView,
                            parent
                    ) as TextView

                    // set item text bold and sans serif font
                    view.setTypeface(Typeface.SANS_SERIF, Typeface.BOLD)

                    if (position == 0){
                        // set the spinner disabled item text color
                        view.setTextColor(Color.LTGRAY)
                    }

                    // set selected item style
                    if (position == spinner.selectedItemPosition){
                        view.background = ColorDrawable(Color.parseColor("#F5F5F5"))
                    }

                    return view
                }

                override fun isEnabled(position: Int): Boolean {
                    // disable the third item of spinner
                    return position != 0
                }

            }
            spinner.adapter = adapter
            spinner.setSelection(adapter.getPosition(category))

        }

        spinner.onItemSelectedListener = object :
                AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>,
                                        view: View, position: Int, id: Long) {
                if (position > 0) {
                    category = categories[position];
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
            }
        }
    }


    fun save(view: View) {
        val url = "http://ec2-18-136-119-32.ap-southeast-1.compute.amazonaws.com:8000/users/${username}/qr-receipts"

        var amountElement = findViewById<EditText>(R.id.create_amount)
        var dateElement = findViewById<EditText>(R.id.create_date)
        var storeElement = findViewById<EditText>(R.id.create_store)
        var descElement = findViewById<EditText>(R.id.create_desc)

        json.put("amount", amountElement.text.toString())
        // parse off the 00:00:00
        if (dateElement.text.contains(" ")) {
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

        json.put("category", category)
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
                    Log.e("Error", error.toString())
                }
        )

        MySingleton.getInstance(this).addToRequestQueue(jsonObjectRequest)
    }
}