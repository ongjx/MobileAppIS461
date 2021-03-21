package com.example.gitrich

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.ListView
import com.example.gitrich.models.Receipt

class AllReceipts : AppCompatActivity() {
    private var receipts: ArrayList<Receipt> = ArrayList();
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_all_receipts)
        getSupportActionBar()!!.setTitle("All Receipts");
        receipts = intent.getSerializableExtra("receipts") as ArrayList<Receipt>
        Log.e("receipts", receipts.toString())

        val listView = findViewById<ListView>(R.id.receipt_all_list)
        listView.adapter = receipts_summary.CustomAdapter(this, receipts)
    }


}