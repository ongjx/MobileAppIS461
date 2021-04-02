package com.example.gitrich

import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.MediaStore.Images.Thumbnails.getThumbnail
import android.speech.RecognizerIntent
import android.util.Log
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.example.gitrich.databinding.ActivityMainBinding
import org.json.JSONObject
import java.io.*
import java.util.*


private const val PERMISSION_CODE = 1000
private const val IMAGE_CAPTURE_CODE = 1001
private const val IMAGE_PICK_CODE=1002
private const val LOGIN_USER_CODE = 1003
private const val RECEIPT_SUBMIT_CODE = 1004

private const val VOICE_CODE = 1005
private const val OCR_CODE = 1006
private const val OCR_RESULT_CODE = 1007
class MainActivity : AppCompatActivity() {
    private lateinit var username: String
    private lateinit var binding: ActivityMainBinding
    private var image_uri: Uri? = null

    private val rotateOpen: Animation by lazy { AnimationUtils.loadAnimation(this, R.anim.rotate_open_anim) }
    private val rotateClose: Animation by lazy { AnimationUtils.loadAnimation(this, R.anim.rotate_close_anim) }
    private val fromBottom: Animation by lazy { AnimationUtils.loadAnimation(this, R.anim.from_bottom_anim) }
    private val toBottom: Animation by lazy { AnimationUtils.loadAnimation(this, R.anim.to_bottom_anim) }
    private var clicked = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        try{
            val scan = Scanner(openFileInput("userProfile.txt"))

            // Load username to global context
            username = scan.nextLine().toString()
            MySingleton.setUsername(username)
        } catch(e: FileNotFoundException) {
            loginPage()
        }


        binding.drawer.setOnClickListener {
            onDrawerBtnClicked()
        }

        binding.manual.setOnClickListener {
            Toast.makeText(this, "Manual", Toast.LENGTH_SHORT).show()
            intent = Intent(this, CreateReceipt::class.java)
            startActivityForResult(intent, RECEIPT_SUBMIT_CODE)
        }

        binding.ocr.setOnClickListener {
            Toast.makeText(this, "OCR", Toast.LENGTH_SHORT).show()
//            intent = Intent(this, OCRScannerActivity::class.java)
//            startActivityForResult(intent, OCR_CODE)

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

        binding.voice.setOnClickListener {
            Toast.makeText(this, "Voice", Toast.LENGTH_SHORT).show()
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            intent.putExtra(RecognizerIntent.EXTRA_PROMPT,"Say Something")
            startActivityForResult(intent, VOICE_CODE)
//            val intent = Intent(this, Voice::class.java)
//            startActivity(intent)
        }

        binding.qr.setOnClickListener {
            Toast.makeText(this, "QR", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, QRScannerActivity::class.java)
            startActivity(intent)
        }

        val transactionsFragment = receipts_summary()
        val analyticsFragment = AnalyticsFragment()

        makeCurrentFragment(transactionsFragment)

        binding.bottomNav.setOnNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.ic_transaction -> makeCurrentFragment(transactionsFragment)
                R.id.ic_analytics -> makeCurrentFragment(analyticsFragment)

            }
            true
        }


    }

    private fun onDrawerBtnClicked() {
        setVisibility(clicked)
        setAnimation(clicked)
        setClickable(clicked)
        clicked = !clicked
    }

    private fun setAnimation(clicked: Boolean) {
        if (!clicked) {
            binding.manual.startAnimation(fromBottom)
            binding.ocr.startAnimation(fromBottom)
            binding.qr.startAnimation(fromBottom)
            binding.voice.startAnimation(fromBottom)
            binding.drawer.startAnimation(rotateOpen)
        } else {
            binding.manual.startAnimation(toBottom)
            binding.ocr.startAnimation(toBottom)
            binding.qr.startAnimation(toBottom)
            binding.voice.startAnimation(toBottom)
            binding.drawer.startAnimation(rotateClose)
        }
    }

    private fun setClickable(clicked: Boolean){
        if (!clicked) {
            binding.manual.isClickable = true
            binding.ocr.isClickable = true
            binding.qr.isClickable = true
            binding.voice.isClickable = true
        }else{
            binding.manual.isClickable = false
            binding.ocr.isClickable = false
            binding.qr.isClickable = false
            binding.voice.isClickable = false
        }
    }

    private fun setVisibility(clicked: Boolean) {
        if(!clicked){
            binding.manual.visibility = View.VISIBLE
            binding.qr.visibility = View.VISIBLE
            binding.ocr.visibility = View.VISIBLE
            binding.voice.visibility = View.VISIBLE
        }else{
            binding.manual.visibility = View.INVISIBLE
            binding.qr.visibility = View.INVISIBLE
            binding.ocr.visibility = View.INVISIBLE
            binding.voice.visibility = View.INVISIBLE
        }
    }


    fun launchOCR(view: View) {

    }


    private fun loginPage() {
        val it = Intent(this, LoginActivity::class.java)
        startActivityForResult(it, LOGIN_USER_CODE)
    }

    private fun makeCurrentFragment(fragment: Fragment) =
        supportFragmentManager.beginTransaction().apply{
            replace(R.id.fl_wrapper, fragment)
            commit()
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
            if(requestCode == IMAGE_CAPTURE_CODE) {
                val intent = Intent(this, OCRScannerActivity::class.java )
                // change to bitmap and store
                val imageStream: InputStream? = contentResolver.openInputStream(image_uri!!)
                val selectedImage = BitmapFactory.decodeStream(imageStream)
                val bitmap = Bitmap.createScaledBitmap(selectedImage, 768, 1024, true)

                val root = getExternalFilesDir(username)
//                val currentFiles = root?.listFiles()
                val newFileName = UUID.randomUUID().toString() + ".jpg"

//                if (currentFiles != null && currentFiles.size > 0) {
//                    newFileName = (currentFiles.first().toString()
//                                            .split(username + "/")[1]
//                                            .split(".jpg")[0]
//                                            .toInt()+1).toString() + ".jpg"
//                } else {
//                    newFileName = "1.jpg"
//                }

                // Saving files
                println("new file name: " + newFileName)
                val imageFile = File(root, newFileName)
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, FileOutputStream(imageFile))

                // Show all files
                val files = root?.listFiles()
                if (files != null) {
                    for (file in files) {
                        println(file.toString())
                    }
                }

                intent.putExtra("image_uri", image_uri)
                intent.putExtra("filepath", newFileName)
                startActivityForResult(intent, OCR_CODE)
            }
            else if (requestCode == IMAGE_PICK_CODE) {
                val intent = Intent(this, OCRScannerActivity::class.java )
                image_uri = data?.data
                if (image_uri != null) {
                    intent.putExtra("image_uri", image_uri)
                    startActivityForResult(intent, OCR_CODE)
                }
            }
            else if (requestCode == LOGIN_USER_CODE) {
                val output = PrintStream(openFileOutput("userProfile.txt", MODE_PRIVATE))
                val username = data?.getStringExtra("username")
                output.println("${username}")
                output.close()
            }
            else if (requestCode == VOICE_CODE) {
                if (data == null) {
                    // make toast say error for voice cant process try again
                } else {
                    val res : ArrayList<String> = data!!.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS) as ArrayList<String>
                    post_dialoflow_api(res[0])
                    finish()
                    startActivity(getIntent())
                }

            }
            else if (requestCode == OCR_CODE) {
                // ocr finished
                val id = data!!.getStringExtra("id")
                val intent = Intent(this, OCRScannerResultActivity::class.java )
                intent.putExtra("id", id)
                println("starting result ocr")
                startActivityForResult(intent, OCR_RESULT_CODE)
            }
        }
    }

    fun post_dialoflow_api(text: String) {
        val username = MySingleton.getUsername()
        // val url = "http://192.168.10.115:8000/users/" + username + "/dialogflow"
        val url = "http://192.168.10.115:8000/users/" + username + "/dialogflow"
        val payload = JSONObject()
        payload.put("text", text)
        payload.put("name", "Adhoc Receipt Mobile")

        val jsonObjectRequest = JsonObjectRequest(
                Request.Method.POST, url, payload,
                { response ->
                    val res = response.getInt("code")

                    if (res == 201){
                        println("success")
                        Toast.makeText(this, "Success! Receipt Created!", Toast.LENGTH_SHORT).show()
                    } else {
                        println("failure")
                        Toast.makeText(this, "Failure! Receipt Not Created!", Toast.LENGTH_SHORT).show()
                    }
                },
                { error ->
                    // TODO: Handle error
                    Log.e("Error", error.toString())
                }
        )
        MySingleton.getInstance(this).addToRequestQueue(jsonObjectRequest)
    }


}