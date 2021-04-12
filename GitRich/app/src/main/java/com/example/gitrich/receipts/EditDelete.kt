package com.example.gitrich.receipts

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.example.gitrich.R
import com.example.gitrich.models.Receipt
import com.example.gitrich.request.RequestQueueSingleton
import com.google.gson.Gson
import okhttp3.OkHttpClient
import org.json.JSONObject
import java.util.regex.Pattern

private const val EDIT_CODE = 998
private const val DELETE_CODE = 999
class EditDelete : AppCompatActivity() {
    private lateinit var receipt: Receipt
    private lateinit var title: EditText
    private lateinit var amount: EditText
    private lateinit var spinner: Spinner
    private lateinit var date: EditText
    private lateinit var itemList: EditText
    private var category = ""
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_delete)
        var categories = resources.getStringArray(R.array.Categories)

        receipt = intent.extras?.getParcelable<Receipt>("receipt")!!
        title = findViewById(R.id.create_store)
        amount = findViewById(R.id.create_amount)
        spinner = findViewById(R.id.create_category)
        date = findViewById(R.id.create_date)
        itemList = findViewById(R.id.create_desc)

        title.setText(receipt.name)
        amount.setText("${receipt.amount}")
        date.setText(receipt.date)

        var desc = ""
        receipt.items.mapKeys { item ->
            desc += "${item.key},${item.value}\n"
        }

        itemList.setText(desc)

        category = receipt.category

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
        val client = OkHttpClient();
        val user = RequestQueueSingleton.getUsername()
        val url = "http://ec2-18-136-119-32.ap-southeast-1.compute.amazonaws.com:8000/users/${user}/receipts/${receipt.id}"

        // Check if value on form has been edited
        val amount = amount.text.toString()
        val date = findViewById<EditText>(R.id.create_date).text.toString()
        val title = findViewById<EditText>(R.id.create_store).text.toString()
        val items = itemList.text.splitToSequence("\n")
        val itemsObject = JSONObject()
        for (item: String in items) {
            if (item != "") {
                val l = item.split(',')
                val name = l[0].trim()
                val amount = if ("$" in l[1]) l[1].trim() else ("$${l[1].trim()}")
                itemsObject.put(l[0].trim() as String, l[1])
            }
        }
        val json = JSONObject()
        json.put("name", title)
        json.put("amount", amount)
        json.put("items", itemsObject)
        json.put("date", date)
        json.put("category", category)

        val dateRegex = "^([0-2][0-9]||3[0-1])/(0[0-9]||1[0-2])/([0-9][0-9])?[0-9][0-9]$"
        if (!Pattern.matches(dateRegex, date)){
            Toast.makeText(this, "Invalid date format", Toast.LENGTH_SHORT).show()
        }
        else if (amount == "" || date == "" || title == "") {
            Toast.makeText(this, "Please ensure that there are no empty fields!", Toast.LENGTH_SHORT).show()
        } else {
            val jsonObjectRequest = JsonObjectRequest(
                    Request.Method.PUT, url, json,
                    { response ->
                        val res = response.getInt("code")

                        if (res == 200) {
                            val goBack = Intent()
                            goBack.putExtra("message", "Receipt Updated")
                            json.put("id", receipt.id)
                            val gson = Gson()
                            goBack.putExtra("receipt", gson.fromJson(json.toString(), Receipt::class.java))
                            setResult(EDIT_CODE, goBack)
                            finish()
                        }
                    },
                    { error ->
                        // TODO: Handle error
//                        Log.e("Error", error.toString())
                    }
            )

            RequestQueueSingleton.getInstance(this).addToRequestQueue(jsonObjectRequest)

        }
    }
    fun delete(view: View) {
        val client = OkHttpClient();
        val user = RequestQueueSingleton.getUsername()
        val url = "http://ec2-18-136-119-32.ap-southeast-1.compute.amazonaws.com:8000/users/${user}/receipts/${receipt.id}"
        val jsonObjectRequest = JsonObjectRequest(
                Request.Method.DELETE, url, JSONObject(),
                { response ->
                    val res = response.getInt("code")

                    if (res == 200) {
                        val goBack = Intent()
                        goBack.putExtra("message", "Receipt Deleted")
                        setResult(DELETE_CODE, goBack)
                        finish()
                    }
                },
                { error ->
                    // TODO: Handle error
//                    Log.e("Error", error.toString())
                }
        )

        RequestQueueSingleton.getInstance(this).addToRequestQueue(jsonObjectRequest)
    }
}