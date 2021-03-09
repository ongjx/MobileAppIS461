package com.example.gitrich

import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.opengl.ETC1.encodeImage
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.gitrich.databinding.ActivityMainBinding
import java.io.InputStream


private const val PERMISSION_CODE = 1000
private const val IMAGE_CAPTURE_CODE = 1001
private const val IMAGE_PICK_CODE=1002

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var image_uri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.OCRBtn.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED ||
                        checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED){
                    val permission = arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    requestPermissions(permission, PERMISSION_CODE)
                }
                else{
                    openCamera()
                }
            }
            else{
                openCamera()
            }
        }
    }

    fun launchQR(view: View) {
        val intent = Intent(this, QRScannerActivity::class.java)
        startActivity(intent)
    }

    fun launchOCR(view: View) {

    }


    private fun openCamera() {
        val contents = ContentValues()
        contents.put(MediaStore.Images.Media.TITLE, "receipt")
        image_uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contents)
        val takePhotoIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        takePhotoIntent.putExtra(MediaStore.EXTRA_OUTPUT, image_uri)
        startActivityForResult(takePhotoIntent, IMAGE_CAPTURE_CODE)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {
            PERMISSION_CODE -> {
                if (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "You need camera permission to use this app", Toast.LENGTH_SHORT)
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        //called when image was captured from camera intent
        if (resultCode == Activity.RESULT_OK) {
            val intent = Intent(this, OCRScannerActivity::class.java )

            if(requestCode == IMAGE_CAPTURE_CODE) {
                intent.putExtra("image_uri", image_uri)
                startActivity(intent)
            }
            else if (requestCode == IMAGE_PICK_CODE) {
                image_uri = data?.data
                if (image_uri != null) {
                    intent.putExtra("image_uri", image_uri)
                    startActivity(intent)
                }
            }
        }

    }
}