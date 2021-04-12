package com.example.gitrich.receipts

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.view.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.gitrich.R
import com.example.gitrich.models.Receipt
import com.example.gitrich.request.RequestQueueSingleton
import java.io.File

private const val EDIT_DELETE_STATUS_CODE = 999
private const val EDIT_STATUS_CODE = 998
private const val DELETE_STATUS_CODE = 999
class ReceiptDetails : AppCompatActivity() {
    private var height = 0
    private var width = 0
    private lateinit var receiptImage: ImageView
    private lateinit var title: TextView
    private lateinit var amount: TextView
    private lateinit var category: TextView
    private lateinit var date: TextView
    private lateinit var itemList: ListView
    private lateinit var receipt: Receipt
    private var username = RequestQueueSingleton.getUsername()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_receipt_details)
        receipt = intent.extras?.getParcelable<Receipt>("receipt")!!

        setReceipt(receipt)
    }

    private fun setReceipt(receipt: Receipt) {
        receiptImage = findViewById(R.id.ReceiptImage)
        title = findViewById(R.id.receipt_name)
        amount = findViewById(R.id.totalAmount)
        category = findViewById(R.id.receipt_category)
        date = findViewById(R.id.receipt_date)
        itemList = findViewById(R.id.breakdown_list)

        if (receipt != null) {
            try {
                var receiptBytes = receipt.image
                if (receipt.image == "null") {
                    receiptImage.setImageResource(R.drawable.logo)
                } else {
                    if (receipt.image.contains("data:image")) {
                        receiptBytes = receipt.image.substringAfter(',')
                        val decodedString = Base64.decode(receiptBytes, Base64.DEFAULT);
                        val decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)
                        receiptImage.setImageBitmap(decodedByte)
                    } else if (receipt.image.equals("")) {
                        receiptImage.setImageResource(R.drawable.logo)
                    } else {
                        // .jpg
                        val root = getExternalFilesDir(username)
                        val imgFile = File(root, receipt.image)

                        if (imgFile.exists()) {
                            val decodedByte = BitmapFactory.decodeFile(imgFile.getAbsolutePath())
                            receiptImage.setImageBitmap(decodedByte)
                        } else {
                            receiptImage.setImageResource(R.drawable.logo)
                        }
                    }
                }
                height = receiptImage.layoutParams.height
                width = receiptImage.layoutParams.width
            } catch (error: Exception) {
            }
            title.text = receipt.name
            amount.text = "$${receipt.amount}"
            category.text = receipt.category
            date.text = receipt.date
            itemList.adapter = CustomAdapter(this, receipt.items)
            modifyReceiptImageSize()
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == EDIT_DELETE_STATUS_CODE) {
            if (resultCode == DELETE_STATUS_CODE) {
                setResult(1010)
                finish()
            }
            else if (resultCode == EDIT_STATUS_CODE) {
                receipt = data!!.extras!!.get("receipt") as Receipt
                setReceipt(receipt)
            }
        }
    }
}
