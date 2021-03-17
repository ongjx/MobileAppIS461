package com.example.gitrich

import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.util.LruCache
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.ImageLoader
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
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

class receipts_summary : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_receipts_summary, container, false)
    }
    class Receipt(
        val id: String,
        val date: String,
        val amount: Double,
        val category: String
    ) {
        override fun toString(): String {
            return "Receipt [id:$id, date:$date, amount:$amount, category:$category]"
        }
    }
     override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        val gson = Gson()
        val listView = activity!!.findViewById<ListView>(R.id.receipt_summary_list)

         val url = "https://leojk9.deta.dev/users/kelvinngsl/receipts"
         val receipts = ArrayList<Receipt>();
         listView.adapter = CustomAdapter(activity!!, receipts)

         val jsonObjectRequest = JsonObjectRequest(Request.Method.GET, url, null,
             Response.Listener { response ->
                 Toast.makeText(activity!!, response.toString(), Toast.LENGTH_LONG)
                 val res = response.getJSONArray("data")
                 for (i in 0 until res.length()) {
                     val receipt = gson.fromJson(res[i].toString(), Receipt::class.java)
                     receipts.add(receipt)
                 }
                 Log.e("receipt", receipts.size.toString())
                 listView.adapter = CustomAdapter(activity!!, receipts)

             },
             Response.ErrorListener { error ->
                 // TODO: Handle error
                 Log.e("Error", error.toString())
             }
         )

         MySingleton.getInstance(activity!!).addToRequestQueue(jsonObjectRequest)

     }

    private class CustomAdapter(context: Context, receipts: ArrayList<Receipt>): BaseAdapter() {

        private val mContext: Context
        private val mReceipts: ArrayList<Receipt>

        init {
            mContext = context
            mReceipts = receipts
        }

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

            val title = row.findViewById<TextView>(R.id.row_title)
            val amount = row.findViewById<TextView>(R.id.row_amount)
            val category = row.findViewById<TextView>(R.id.category)
            val date = row.findViewById<TextView>(R.id.date)
            if (!mReceipts.isEmpty()) {
                val receipt = mReceipts[position]
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


}
