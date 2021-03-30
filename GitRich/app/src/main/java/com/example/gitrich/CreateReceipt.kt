package com.example.gitrich

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
import java.text.SimpleDateFormat
import java.time.format.DateTimeFormatter
import java.util.*

class CreateReceipt : AppCompatActivity() {
    private lateinit var categories: Array<String>;
    private var amount = "";
    private var date = Date();
    private var store = "";
    private var desc = "";
    private var category = "";

    private lateinit var spinner: Spinner
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_receipt)
        categories = resources.getStringArray(R.array.Categories)
        spinner = findViewById(R.id.create_category)
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

    @RequiresApi(Build.VERSION_CODES.O)
    fun save(view: View) {
        amount = findViewById<TextView>(R.id.create_amount).toString()
        date = findViewById(R.id.create_date)
        val format = SimpleDateFormat("dd/MM/yyyy")
        var dateStr = format.format(date)
        store = findViewById(R.id.create_shop)
        desc = findViewById(R.id.create_desc)
        category = findViewById(R.id.category)
        val image = ""
        Log.e("err", "$amount    $dateStr $store $desc $category")
        val client = OkHttpClient();
        val jsonObject = JSONObject()

        jsonObject.put("name", store)
        jsonObject.put("amount", amount)
        val itemsObject = JSONObject()
        itemsObject.put(desc, amount)
        jsonObject.put("items", itemsObject)
        jsonObject.put("image", image)
        jsonObject.put("date", dateStr)

        val JSON = MediaType.parse("application/json; charset=utf-8")
        val body = RequestBody.create(JSON, jsonObject.toString());

        val url = "http://10.0.2.2:8000/users/yongwk1/qr-receipts"
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

