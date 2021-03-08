package com.example.gitrich

import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.webkit.WebView
import android.widget.TextView
import android.widget.Toast
import com.github.barteksc.pdfviewer.PDFView
import com.github.barteksc.pdfviewer.util.FitPolicy
import org.json.JSONObject

class QRScannerResultActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_q_r_scanner_result_activity)

        val intent = intent.extras
        if (intent != null) {
            val url = intent.getString("url")
            Toast.makeText(this, url, Toast.LENGTH_LONG).show()
            val webview = findViewById<WebView>(R.id.pdfView)
            webview.getSettings().setJavaScriptEnabled(true);
            webview.loadUrl("https://drive.google.com/viewerng/viewer?embedded=true&url=" + url);
        }
    }

    fun submitReceipt(view: View) {
        //TODO: Handle receipt submission to backend
    }

    fun cancel(view: View) {
        finish()
    }
}

