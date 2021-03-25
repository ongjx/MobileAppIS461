package com.example.gitrich

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.ListView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.example.gitrich.models.Receipt
import com.google.gson.Gson


// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [receipts_summary.newInstance] factory method to
 * create an instance of this fragment.
 */
var receipts = ArrayList<Receipt>();

class receipts_summary : Fragment() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        retainInstance = true
//        if (savedInstanceState != null) {
//            receipts = savedInstanceState.getParcelableArrayList<Receipt>("receipts") as ArrayList<Receipt>
//        }
    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_receipts_summary, container, false)

        val seeAll = view.findViewById<TextView>(R.id.see_all_receipts_btn)
        seeAll.setOnClickListener {
            val intent = Intent(activity!!, AllReceipts::class.java)
            intent.putExtra("receipts", receipts)
            startActivity(intent)
        }

        return view
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        if (savedInstanceState != null) {
            //Restore the fragment's state here
            receipts = savedInstanceState.getParcelableArrayList<Receipt>("receipts") as ArrayList<Receipt>
        } else {
            getReceipts()
        }


    }
    private fun getReceipts () {
        val gson = Gson()
        val listView = activity!!.findViewById<ListView>(R.id.receipt_summary_list)

        val url = "https://leojk9.deta.dev/users/kelvinngsl/receipts"
        // For development local
//        val url = "http://10.0.2.2:8000/users/kelvinngsl/receipts"
        listView.adapter = CustomAdapter(activity!!, receipts)

        val jsonObjectRequest = JsonObjectRequest(Request.Method.GET, url, null,
            { response ->
                val res = response.getJSONArray("data")
                for (i in 0 until res.length()) {
                    val receipt = gson.fromJson(res[i].toString(), Receipt::class.java)
                    receipts.add(receipt)
                }
                listView.adapter = CustomAdapter(activity!!, receipts)
            },
            { error ->
                // TODO: Handle error
                Log.e("Error", error.toString())
            }
        )

        startListListener(listView)

        MySingleton.getInstance(activity!!).addToRequestQueue(jsonObjectRequest)
    }
    class CustomAdapter(context: Context, receipts: ArrayList<Receipt>): BaseAdapter() {

        private val mContext: Context = context
        private val mReceipts: ArrayList<Receipt> = receipts

        override fun getCount(): Int {
            return mReceipts.size
        }

        override fun getItem(position: Int): Any {
            return "Test String"
        }

        override fun getItemId(position: Int): Long {
            return position.toLong()
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val layoutInflater = LayoutInflater.from(mContext)
            val row = layoutInflater.inflate(R.layout.main_row, parent, false)
            
            val thumbnail = row.findViewById<ImageView>(R.id.thumbnail_display)
            val title = row.findViewById<TextView>(R.id.row_title)
            val amount = row.findViewById<TextView>(R.id.row_amount)
            val category = row.findViewById<TextView>(R.id.category)
            val date = row.findViewById<TextView>(R.id.date)
            if (mReceipts.isNotEmpty()) {
                val receipt = mReceipts[position]
                try {
                    var receiptBytes = receipt.image
                    if (receipt.image.contains("data:image")) {
                        receiptBytes = receipt.image.substringAfter(',')
                    }
                    val decodedString = Base64.decode(receiptBytes, Base64.DEFAULT);
                    val decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)
                    thumbnail.setImageBitmap(decodedByte)
                } catch (error: Exception){
                }
                title.text = receipt.id
                amount.text = "$${receipt.amount}"
                category.text = receipt.category
                date.text = receipt.date
                return row
            }
            return row

        }
    }
    class MySingleton constructor(context: Context) {
        companion object {
            @Volatile
            private var INSTANCE: MySingleton? = null
            fun getInstance(context: Context) =
                INSTANCE ?: synchronized(this) {
                    INSTANCE ?: MySingleton(context).also {
                        INSTANCE = it
                    }
                }
        }

        val requestQueue: RequestQueue by lazy {
            // applicationContext is key, it keeps you from leaking the
            // Activity or BroadcastReceiver if someone passes one in.
            Volley.newRequestQueue(context.applicationContext)
        }

        fun <T> addToRequestQueue(req: Request<T>) {
            requestQueue.add(req)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelableArrayList("receipts", receipts)
    }


    fun startListListener(listView: ListView) {
        listView.setOnItemClickListener { _, _, position, id ->
            val receipt = receipts[position];
            Log.e("Receipt", receipt.toString())
            val intent = Intent(activity!!, ReceiptDetails::class.java)
            intent.putExtra("receipt", receipt)
            startActivity(intent)
        }
    }


}