package com.example.gitrich

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.example.gitrich.models.Receipt
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
    private lateinit var categories: Array<String>;
    private var username = MySingleton.getUsername()
    private var height = 0
    private var width = 0
    private var amountCreated = "";
    private var dateCreated = "";
    private var storeCreated = "";
    private var descCreated = "";
    private var categoryCreated = "";

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

        receiptImage = view.findViewById(R.id.ReceiptImage)
        title = view.findViewById(R.id.receipt_name)
        amount = view.findViewById(R.id.totalAmount)
        category = view.findViewById(R.id.receipt_category)
        date = view.findViewById(R.id.receipt_date)
        itemList = view.findViewById(R.id.breakdown_list)
        desc = view.findViewById(R.id.create_desc)


        return view
    }

    override fun onResume() {
        super.onResume()
        categories = resources.getStringArray(R.array.Categories)
        val arrayAdapter = ArrayAdapter(requireContext(), R.layout.dropdown_items, categories)
        category.setAdapter(arrayAdapter)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
            if(receipt != null){
                try {
                    var receiptBytes = receipt?.image
                    if (receipt?.image == "null") {
                        receiptImage.setImageResource(R.drawable.empty)
                    } else {
                        if (receipt?.image!!.contains("data:image")) {
                            receiptBytes = receipt?.image!!.substringAfter(',')
                            val decodedString = Base64.decode(receiptBytes, Base64.DEFAULT);
                            val decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)
                            receiptImage.setImageBitmap(decodedByte)
                        } else if (receipt?.image.equals("")) {
                            receiptImage.setImageResource(R.drawable.empty)
                        } else {
                            // .jpg
                            val root = activity?.getExternalFilesDir(username)
                            val imgFile = File(root, receipt!!.image)

                            if (imgFile.exists()) {
                                val decodedByte = BitmapFactory.decodeFile(imgFile.getAbsolutePath())
                                receiptImage.setImageBitmap(decodedByte)
                            } else {
                                receiptImage.setImageResource(R.drawable.empty)
                            }
                        }
                    }
                    height = receiptImage.layoutParams.height
                    width = receiptImage.layoutParams.width
                } catch (error: Exception){

                }
                title.setText(receipt?.name)
                amount.setText("$${receipt?.amount}")
                category.setText(receipt?.category)
                date.setText(receipt?.date)
                itemList.adapter = ReceiptDetails.CustomAdapter(requireContext(), receipt.items)
                modifyReceiptImageSize()
            }else{
                category.isClickable = true
                itemList.visibility = View.VISIBLE

            }
    }

    fun save(view: View){
        amountCreated = amount.text.toString()
        dateCreated = date.text.toString()
        storeCreated = title.text.toString()
        descCreated = desc.text.toString()
        categoryCreated = category.text.toString()

        var itemsObject = JSONObject()

        if (descCreated.equals("")) {
            // do nothing
        } else {
            try {
                val items = descCreated.splitToSequence("\n")

                for (item: String in items) {
                    val l = item.split(',')
                    val name = l[0].trim()
                    val amount = if ("$" in l[1]) l[1].trim() else ("$${l[1].trim()}")

                    itemsObject.put(l[0].trim() as String, l[1])
                }
            } catch (e: java.lang.Exception) {
                // We can choose to stop processing unless its empty or appropriate desc is entered
                println("Inappropriate Description")
                itemsObject = JSONObject()
            }
        }


        val dateRegex = "^([0-2][0-9]||3[0-1])/(0[0-9]||1[0-2])/([0-9][0-9])?[0-9][0-9]$"
        if (!Pattern.matches(dateRegex, dateCreated)){
            Toast.makeText(requireContext(), "Invalid date format", Toast.LENGTH_SHORT).show()
        }
        else if (amountCreated == "" || dateCreated == "" || storeCreated == "") {
            Toast.makeText(requireContext(), "Please ensure that there are no empty fields!", Toast.LENGTH_SHORT).show()
        }
        else {
            val client = OkHttpClient();
            val jsonObject = JSONObject()

            jsonObject.put("name", storeCreated)
            jsonObject.put("amount", amountCreated)
            jsonObject.put("items", itemsObject)
            jsonObject.put("image", "")
            jsonObject.put("date", dateCreated)
            jsonObject.put("category", categoryCreated)

            val user = MySingleton.getUsername()
            val url = "http://ec2-18-136-119-32.ap-southeast-1.compute.amazonaws.com:8000/users/${user}/qr-receipts"
            val jsonObjectRequest = JsonObjectRequest(
                Request.Method.POST, url, jsonObject,
                { response ->
                    val res = response.getInt("code")
                    Log.e("response", response.toString())
                    if (res == 201){

                        val goBack = Intent()
                        requireActivity().setResult(AppCompatActivity.RESULT_OK)
                        requireActivity().finish()
                    }
                },
                { error ->
                    Log.e("Error", error.toString())
                }
            )
            MySingleton.getInstance(requireContext()).addToRequestQueue(jsonObjectRequest)
        }

    }

    private fun modifyReceiptImageSize() {
        var isImageFitToScreen = false

        receiptImage.setOnClickListener{
            if (isImageFitToScreen) {
                isImageFitToScreen = false
                receiptImage.setLayoutParams(
                    LinearLayout.LayoutParams(
                        width,
                        height
                    )
                )
                receiptImage.setAdjustViewBounds(true)
            } else {
                isImageFitToScreen = true
                receiptImage.setLayoutParams(
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.MATCH_PARENT
                    )
                )
                receiptImage.setScaleType(ImageView.ScaleType.FIT_XY)
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
            return mItems.getValue(mKeys[position]);
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