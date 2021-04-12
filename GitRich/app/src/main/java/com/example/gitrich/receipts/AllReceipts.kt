package com.example.gitrich.receipts

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ListView
import com.example.gitrich.R
import com.example.gitrich.models.Receipt

class AllReceipts : AppCompatActivity() {
    private var receipts: ArrayList<Receipt> = ArrayList()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_all_receipts)
        supportActionBar!!.title = "All Receipts"
        receipts = intent.getSerializableExtra("receipts") as ArrayList<Receipt>

        val listView = findViewById<ListView>(R.id.receipt_all_list)
        listView.adapter = receipts_summary.CustomAdapter(this, receipts)

        listView.setOnItemClickListener { parent, view, position, id ->
            val receipt = receipts[position]
            val intent = Intent(this, ReceiptDetails::class.java)
            intent.putExtra("receipt", receipt)
            startActivity(intent)
        }
    }


}