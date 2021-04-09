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
import okhttp3.OkHttpClient
import org.json.JSONObject

class QRScannerResultActivity : AppCompatActivity() {
    private lateinit var categories: Array<String>;
    private var json = JSONObject();
    private var amount = ""
    private var date = ""
    private var store = ""
    private var desc = ""
    private var category = ""
    private lateinit var spinner: Spinner

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_receipt)
        supportActionBar!!.title = "GitRich - Confirm Receipt";
        categories = resources.getStringArray(R.array.Categories)
        spinner = findViewById(R.id.create_category)

        var amountElement = findViewById<EditText>(R.id.create_amount)
        var dateElement = findViewById<EditText>(R.id.create_date)
        var storeElement = findViewById<EditText>(R.id.create_store)
        var descElement = findViewById<EditText>(R.id.create_desc)

        val intent = intent.extras
        if (intent != null) {
            val data = intent.getString("data")!!
            json = JSONObject(data)
            amount = json.get("amount") as String
            date = json.get("date") as String
            store = json.get("name") as String
            category = json.get("category") as String
            var items = json.get("items") as JSONObject
            for (i in 0 until items.names().length()) {
                val key = items.names()[i] as String
                val value = items.getString(key)
                desc += "${key},$${value}\n"
            }

            amountElement.setText(amount)
            dateElement.setText(date)
            storeElement.setText(store)
            descElement.setText(desc)
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
        //TODO: Handle receipt submission to backend
        val client = OkHttpClient();
        val user = MySingleton.getUsername()
        val url = "http://ec2-18-136-119-32.ap-southeast-1.compute.amazonaws.com:8000/users/${user}/qr-receipts"

        // Check if value on form has been edited
        amount = findViewById<EditText>(R.id.create_amount).text.toString()
        date = findViewById<EditText>(R.id.create_date).text.toString()
        store = findViewById<EditText>(R.id.create_store).text.toString()
        desc = findViewById<EditText>(R.id.create_desc).text.toString()
        val items = desc.splitToSequence("\n")
        val itemsObject = JSONObject()
        for (item: String in items) {
            if (item != "") {
                val l = item.split(',')
                val name = l[0].trim()
                val amount = if ("$" in l[1]) l[1].trim() else ("$${l[1].trim()}")
                itemsObject.put(l[0].trim() as String, l[1])
            }
        }
        json.put("name", store)
        json.put("amount", amount)
        json.put("items", itemsObject)
        json.put("date", date)

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
                    Log.e("Error", error.toString())
                }
        )

        MySingleton.getInstance(this).addToRequestQueue(jsonObjectRequest)
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

