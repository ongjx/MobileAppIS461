package com.example.gitrich.receipts

import android.content.Context
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.example.gitrich.MainActivity
import com.example.gitrich.R
import com.example.gitrich.models.Receipt
import com.example.gitrich.request.RequestQueueSingleton
import com.google.android.material.textfield.TextInputEditText
import okhttp3.OkHttpClient
import org.json.JSONObject
import java.io.File
import java.util.regex.Pattern

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [receipt_details.newInstance] factory method to
 * create an instance of this fragment.
 */
class receipt_details : Fragment() {
    // TODO: Rename and change types of parameters
    private lateinit var receipt: Receipt
    private lateinit var receiptImage: ImageView
    private lateinit var title: TextInputEditText
    private lateinit var amount: TextInputEditText
    private lateinit var category: AutoCompleteTextView
    private lateinit var date: TextInputEditText
    private lateinit var itemList: ListView
    private lateinit var desc: TextInputEditText
    private lateinit var categories: Array<String>
    private lateinit var saveBtn: Button
    private lateinit var deleteBtn: Button
    private var username = RequestQueueSingleton.getUsername()
    private var height = 0
    private var width = 0
    private var amountNew = ""
    private var dateNew = ""
    private var storeNew = ""
    private var descNew = ""
    private var categoryNew = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            receipt = it.getParcelable<Receipt>("receipt")!!
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        var view = inflater.inflate(R.layout.fragment_receipt_details, container, false)
        saveBtn = view.findViewById<View>(R.id.create_save) as Button
        deleteBtn = view.findViewById<View>(R.id.create_delete) as Button
        saveBtn.setOnClickListener {
            save()
        }
        deleteBtn.setOnClickListener {
            delete()
        }
        return view
    }

    private fun delete() {
        val client = OkHttpClient()
        val user = RequestQueueSingleton.getUsername()
        val url = "http://ec2-18-136-119-32.ap-southeast-1.compute.amazonaws.com:8000/users/${user}/receipts/${receipt.id}"
        val jsonObjectRequest = JsonObjectRequest(
            Request.Method.DELETE, url, JSONObject(),
            { response ->
                val res = response.getInt("code")

                if (res == 200) {
                    (context as MainActivity).fragManager.popBackStack()
                }
            },
            { error ->
                // TODO: Handle error
//                Log.e("Error", error.toString())
            }
        )

        RequestQueueSingleton.getInstance(requireContext()).addToRequestQueue(jsonObjectRequest)
    }


    override fun onResume() {
        super.onResume()
        categories = resources.getStringArray(R.array.Categories)
        val arrayAdapter = ArrayAdapter(requireContext(), R.layout.dropdown_items, categories)
        category.setAdapter(arrayAdapter)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        val act = activity
        if(act != null){
            receiptImage = act.findViewById(R.id.ReceiptImage)
            title = act.findViewById(R.id.receipt_name)
            amount = act.findViewById(R.id.totalAmount)
            category = act.findViewById(R.id.receipt_category)
            date = act.findViewById(R.id.receipt_date)
            //itemList = view.findViewById(R.id.breakdown_list)
            desc = act.findViewById(R.id.create_desc)
            if(receipt != null){
                try {
                    var receiptBytes = receipt.image
                    if (receipt.image == "null") {
                        receiptImage.setImageResource(R.drawable.logo)
                    } else {
                        if (receipt.image.contains("data:image")) {
                            receiptBytes = receipt.image.substringAfter(',')
                            val decodedString = Base64.decode(receiptBytes, Base64.DEFAULT)
                            val decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)
                            receiptImage.setImageBitmap(decodedByte)
                        } else if (receipt.image.equals("")) {
                            receiptImage.setImageResource(R.drawable.logo)
                        } else {
                            // .jpg
                            val root = activity?.getExternalFilesDir(username)
                            val imgFile = File(root, receipt.image)

                            if (imgFile.exists()) {
                                val decodedByte = BitmapFactory.decodeFile(imgFile.absolutePath)
                                receiptImage.setImageBitmap(decodedByte)
                            } else {
                                receiptImage.setImageResource(R.drawable.logo)
                            }
                        }
                    }
                    height = receiptImage.layoutParams.height
                    width = receiptImage.layoutParams.width
                } catch (error: Exception){

                }


                title.setText(receipt.name)
                amount.setText("$${receipt.amount}")
                category.setText(receipt.category)
                date.setText(receipt.date)

                var x = ""
                receipt.items.mapKeys { item ->
                    x += "${item.key}, ${item.value}\n"
                }
                desc.setText(x)
                modifyReceiptImageSize()
            }else{
                category.isClickable = true
            }
        }

    }

    fun save(){
        val amountNew = amount.text.toString()
        val dateNew = date.text.toString()
        val storeNew = title.text.toString()
        val descNew = desc.text?.splitToSequence("\n")
        val categoryNew = category.text.toString()

        val client = OkHttpClient()
        val user = RequestQueueSingleton.getUsername()
        val url = "http://ec2-18-136-119-32.ap-southeast-1.compute.amazonaws.com:8000/users/${user}/receipts/${receipt.id}"

        // Check if value on form has been edited
        val itemsObject = JSONObject()
        if (descNew != null) {
            for (item: String in descNew) {
                if (item != "") {
                    val l = item.split(',')
                    val name = l[0].trim()
                    val amount = if ("$" in l[1]) l[1].trim() else ("$${l[1].trim()}")
                    itemsObject.put(l[0].trim(), l[1])
                }
            }
        }
        val json = JSONObject()
        json.put("name", storeNew)
        json.put("amount", amountNew.removePrefix("$"))
        json.put("items", itemsObject)
        json.put("date", dateNew)
        json.put("category", categoryNew)

        val dateRegex = "^([0-2][0-9]||3[0-1])/(0[0-9]||1[0-2])/([0-9][0-9])?[0-9][0-9]$"
        if (!Pattern.matches(dateRegex, dateNew)){
            Toast.makeText(requireContext(), "Invalid date format", Toast.LENGTH_SHORT).show()
        }
        else if (amountNew == "" || dateNew == "" || storeNew == "") {
            Toast.makeText(requireContext(), "Please ensure that there are no empty fields!", Toast.LENGTH_SHORT).show()
        } else {
            val jsonObjectRequest = JsonObjectRequest(
                Request.Method.PUT, url, json,
                { response ->
                    val res = response.getInt("code")

                    if (res == 200) {
                        Toast.makeText(requireContext(), "Receipt saved", Toast.LENGTH_SHORT).show()
                        (context as MainActivity).refresh()
                    }
                },
                { error ->
                    // TODO: Handle error
                    //Log.e("Error", error.toString())
                }
            )

            RequestQueueSingleton.getInstance(requireContext()).addToRequestQueue(jsonObjectRequest)

        }

    }


    private fun modifyReceiptImageSize() {
        var isImageFitToScreen = false

        receiptImage.setOnClickListener{
            if (isImageFitToScreen) {
                isImageFitToScreen = false
                receiptImage.layoutParams = LinearLayout.LayoutParams(
                        width,
                        height
                )
                receiptImage.adjustViewBounds = true
            } else {
                isImageFitToScreen = true
                receiptImage.layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.MATCH_PARENT
                )
                receiptImage.scaleType = ImageView.ScaleType.FIT_XY
            }
        }
    }

    class CustomAdapter(context: Context, items: HashMap<String, String>): BaseAdapter() {

        private val mContext: Context = context
        private val mItems: HashMap<String, String> = items
        private val mKeys: Array<String> = mItems.keys.toTypedArray()

        override fun getCount(): Int {
            return mItems.size
        }

        override fun getItem(position: Int): Any {
            return mItems.getValue(mKeys[position])
        }

        override fun getItemId(position: Int): Long {
            return position.toLong()
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val layoutInflater = LayoutInflater.from(mContext)
            val row = layoutInflater.inflate(R.layout.receipt_item_row, parent, false)

            val item = row.findViewById<TextView>(R.id.itemname)
            val cost = row.findViewById<TextView>(R.id.itemcost)

            if (mItems.isNotEmpty()) {

                item.text = mKeys[position]
                cost.text = mItems.getValue(mKeys[position])
            }
            return row
        }
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment receipt_details.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(receipt: Receipt?) =
            receipt_details().apply {
                arguments = Bundle().apply {
                    putParcelable("receipt", receipt)
                }
            }
    }
}