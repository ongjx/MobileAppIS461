package com.example.gitrich.receipts

import android.content.Context
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.ListView
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.example.gitrich.MainActivity
import com.example.gitrich.R
import com.example.gitrich.models.Receipt
import com.example.gitrich.request.RequestQueueSingleton
import com.google.gson.Gson
import java.io.File
import kotlin.collections.ArrayList


// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [receipts_summary.newInstance] factory method to
 * create an instance of this fragment.
 */

var receipts = ArrayList<Receipt>()

class receipts_summary : Fragment() {
    private lateinit var username:String

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_receipts_summary, container, false)

        (context as MainActivity).enableFAB(true)
        return view
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        username = RequestQueueSingleton.getUsername()
        val user = requireActivity().findViewById<TextView>(R.id.greetings)
        user.text = "Hi, ${username}"

    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        if (savedInstanceState != null) {
            //Restore the fragment's state here
                if(receipts != null){
                    if (savedInstanceState.getParcelableArrayList<Receipt>("receipts") != null) {
                        receipts = savedInstanceState.getParcelableArrayList<Receipt>("receipts") as ArrayList<Receipt>
                    }
                }
        } else {
            receipts.clear()
            getReceipts()

        }
    }
    @RequiresApi(Build.VERSION_CODES.O)
    private fun getReceipts () {
        val gson = Gson()
        val listView = requireActivity().findViewById<ListView>(R.id.receipt_summary_list)

        val url = "http://ec2-18-136-119-32.ap-southeast-1.compute.amazonaws.com:8000/users/${username}/receipts"

        val jsonObjectRequest = JsonObjectRequest(Request.Method.GET, url, null,
            { response ->
                val res = response.getJSONArray("data")
                for (i in 0 until res.length()) {
                    val receipt = gson.fromJson(res[i].toString(), Receipt::class.java)
                    receipts.add(receipt)
                }
                listView.adapter = CustomAdapter(requireActivity(), receipts)
            },

            { error ->
                // TODO: Handle error
            }
        )

        startListListener(listView)

        RequestQueueSingleton.getInstance(requireActivity()).addToRequestQueue(jsonObjectRequest)
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

        @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val layoutInflater = LayoutInflater.from(mContext)
            val row = layoutInflater.inflate(R.layout.main_row, parent, false)

            row.background = mContext.getDrawable(R.drawable.listview_border)

            val thumbnail = row.findViewById<ImageView>(R.id.thumbnail_display)
            val title = row.findViewById<TextView>(R.id.row_title)
            val amount = row.findViewById<TextView>(R.id.row_amount)
            val category = row.findViewById<TextView>(R.id.category)
            val date = row.findViewById<TextView>(R.id.date)

            if (receipts.isNotEmpty()) {
                val receipt = receipts[position]
                try {
                    var receiptBytes = receipt.image
                    if (receipt.image == null) {
                        thumbnail.setImageResource(R.drawable.logo)
                    } else {
                        if (receipt.image.contains("data:image")) {
                            receiptBytes = receipt.image.substringAfter(',')
                            val decodedString = Base64.decode(receiptBytes, Base64.DEFAULT)
                            val decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)
                            thumbnail.setImageBitmap(decodedByte)
                        } else if (receipt.image.equals("")) {
                            thumbnail.setImageResource(R.drawable.logo)
                        } else {
                            // .jpg
                            val root = mContext.getExternalFilesDir(RequestQueueSingleton.getUsername())
                            val imgFile = File(root, receipt.image)

                            if (imgFile.exists()) {
                                val decodedByte = BitmapFactory.decodeFile(imgFile.absolutePath)
                                thumbnail.setImageBitmap(decodedByte)
                            } else {
                                thumbnail.setImageResource(R.drawable.logo)
                            }
                        }
                    }

                } catch (error: Exception){
                    println(error)
                }
                title.text = receipt.name
                amount.text = "$${receipt.amount}"
                category.text = receipt.category
                date.text = receipt.date
                return row
            }

            return row

        }
    }


    fun startListListener(listView: ListView) {
        listView.setOnItemClickListener { _, _, position, id ->
            if ((context as MainActivity).clicked == false) {
                val receipt = receipts[position]
                (context as MainActivity).makeCurrentFragment(receipt_details.newInstance(receipt), "receipt_details")
            }
        }
    }
}
