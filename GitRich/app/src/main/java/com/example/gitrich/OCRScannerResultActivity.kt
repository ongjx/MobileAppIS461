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
import org.json.JSONObject

class OCRScannerResultActivity : AppCompatActivity() {
    private val username = MySingleton.getUsername()
    private lateinit var categories: Array<String>;
    private var json = JSONObject();
    private var amount = 0.0
    private var date = ""
    private var store = ""
    private var desc = ""
    private var category = ""
    private var id = ""
    private lateinit var spinner: Spinner

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_receipt)

        println("IM ASDHIUSADHAAAAAAAKJBHASKDHASKDJAHSDKJASDHKASJDHKASJDHADKJHASKJHASDJKAHDSKJDASHDKAJSHDASKJHADSKJDAHDKASJHASDKJASDHKJH")
        supportActionBar!!.title = "GitRich - Confirm Receipt";
        categories = resources.getStringArray(R.array.Categories)
        spinner = findViewById(R.id.create_category)


        // Get receipt data from API
        if (intent != null) {
            id = intent.extras?.getString("id").toString()
            populateForm(id)
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

    fun populateForm(id: String) {
        val url = "http://10.0.2.2:8000/users/${username}/receipts/${id}"

        val payload = JSONObject()

        val jsonObjectRequest = JsonObjectRequest(
                Request.Method.GET, url, payload,
                { response ->
                    val res = response.getInt("code")

                    if (res == 200) {
                        json = response.getJSONObject("data")
//                        val amount_raw = json.get("amount")
//                        if (amount_raw is Int) {
//                            amount = amount_raw.toDouble()
//                        } else if (amount_raw is Double) {
//                            amount = amount_raw
//                        }
                        val amount_raw = json.get("amount") as String
                        if (amount_raw.equals("")) {
                            amount = 0.0
                        } else {
                            amount = amount_raw.toDouble()
                        }
                        date = json.get("date") as String
                        store = json.get("name") as String
                        category = json.get("category") as String

                        var items = json.get("items") as JSONObject
                        if (items.length() != 0) {
                            for (i in 0 until items.names().length()) {
                                val key = items.names()[i] as String
                                val value = items.getString(key)
                                desc += "${key},$${value}\n"
                            }
                        }

                        var amountElement = findViewById<EditText>(R.id.create_amount)
                        var dateElement = findViewById<EditText>(R.id.create_date)
                        var storeElement = findViewById<EditText>(R.id.create_store)
                        var descElement = findViewById<EditText>(R.id.create_desc)

                        amountElement.setText(amount.toString())
                        dateElement.setText(date)
                        storeElement.setText(store)
                        descElement.setText(desc)
                    }
                },
                { error ->
                    // TODO: Handle error
                    Log.e("Error", error.toString())
                }
        )

        MySingleton.getInstance(this).addToRequestQueue(jsonObjectRequest)

    }

    fun save(view: View) {
        //TODO: update receipt submission to backend
        val url = "http://10.0.2.2:8000/users/${username}/receipts/${id}"
        val payload = JSONObject()
        amount = findViewById<EditText>(R.id.create_amount).text.toString().toDouble()
        store = findViewById<EditText>(R.id.create_store).text.toString()
        date = findViewById<EditText>(R.id.create_date).text.toString()

        payload.put("amount", amount)
        payload.put("name", store)
        payload.put("category", category)

        // TODO: Add items
//        payload.put("name", store)

        val jsonObjectRequest = JsonObjectRequest(
                Request.Method.PUT, url, payload,
                { response ->
                    val res = response.getInt("code")

                    if (res == 200) {
                        val goBack = Intent()
//                        goBack.putExtra("message", "OCR Receipt Created")
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