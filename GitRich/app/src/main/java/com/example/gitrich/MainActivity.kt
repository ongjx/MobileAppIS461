package com.example.gitrich

import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.speech.RecognizerIntent
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.gitrich.databinding.ActivityMainBinding
import java.io.*
import java.util.*


private const val PERMISSION_CODE = 1000
private const val IMAGE_CAPTURE_CODE = 1001
private const val IMAGE_PICK_CODE=1002
private const val LOGIN_USER_CODE = 1003
private const val RECEIPT_SUBMIT_CODE = 1004

private const val VOICE_CODE = 1005
private const val VOICE_CONFIRM_CODE = 1006
private const val OCR_CODE = 1007
private const val OCR_RESULT_CODE = 1008
private const val QR_RESULT_CODE = 1009
private const val VIEW_RECEIPT = 1010

class MainActivity : AppCompatActivity() {
    private lateinit var username: String
    private lateinit var binding: ActivityMainBinding
    private var image_uri: Uri? = null

    private val rotateOpen: Animation by lazy { AnimationUtils.loadAnimation(this, R.anim.rotate_open_anim) }
    private val rotateClose: Animation by lazy { AnimationUtils.loadAnimation(this, R.anim.rotate_close_anim) }
    private val fromBottom: Animation by lazy { AnimationUtils.loadAnimation(this, R.anim.from_bottom_anim) }
    private val toBottom: Animation by lazy { AnimationUtils.loadAnimation(this, R.anim.to_bottom_anim) }
    var clicked = false

    private lateinit var transactionsFragment: receipts_summary;
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        actionBar?.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM)
//        actionBar?.setCustomView(R.layout.actionbar_center)
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
            startActivityForResult(intent, QR_RESULT_CODE)
        }

        transactionsFragment = receipts_summary()
        val analyticsFragment = AnalyticsFragment()

        makeCurrentFragment(transactionsFragment, "transaction")

        binding.bottomNav.setOnNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.ic_transaction -> makeCurrentFragment(transactionsFragment, "transaction")
                R.id.ic_analytics -> makeCurrentFragment(analyticsFragment, "analytics")

            }
            true
        }


    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.logout_menu, menu)
        return super.onCreateOptionsMenu(menu);
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.logout -> {
                val file = File("userProfile.txt").delete()

            }

        }

        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        val fragManager = supportFragmentManager
        if(fragManager.backStackEntryCount > 1){
            fragManager.popBackStackImmediate()
        }else{
            super.onBackPressed()
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
            binding.flWrapper.isClickable = false
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                binding.flWrapper.focusable = FrameLayout.NOT_FOCUSABLE
            }
        }else{
            binding.manual.isClickable = false
            binding.ocr.isClickable = false
            binding.qr.isClickable = false
            binding.voice.isClickable = false
            binding.flWrapper.isClickable = true
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                binding.flWrapper.focusable = FrameLayout.FOCUSABLE
            }
        }
    }

    private fun setVisibility(clicked: Boolean) {
        if(!clicked){
            binding.manualGroup.visibility = View.VISIBLE
            binding.qrGroup.visibility = View.VISIBLE
            binding.ocrGroup.visibility = View.VISIBLE
            binding.voiceGroup.visibility = View.VISIBLE
            binding.overlay.visibility = View.VISIBLE
        }else{
            binding.manualGroup.visibility = View.INVISIBLE
            binding.qrGroup.visibility = View.INVISIBLE
            binding.ocrGroup.visibility = View.INVISIBLE
            binding.voiceGroup.visibility = View.INVISIBLE
            binding.overlay.visibility = View.INVISIBLE
        }
    }


    fun launchOCR(view: View) {

    }


    private fun loginPage() {
        val it = Intent(this, LoginActivity::class.java)
        startActivityForResult(it, LOGIN_USER_CODE)
    }

    fun makeCurrentFragment(fragment: Fragment, tag: String) {
        if(tag == "transaction" || tag =="analytics"){
            binding.drawer.visibility = View.VISIBLE
            binding.drawer.isClickable = true
        }else{
            binding.drawer.visibility = View.INVISIBLE
            binding.drawer.isClickable = false
        }

        supportFragmentManager.beginTransaction().apply{
            replace(R.id.fl_wrapper, fragment)
            addToBackStack(tag)
            commit()
        }
    }

    fun enableFAB(isShow: Boolean){
        if(isShow){
            binding.drawer.visibility = View.VISIBLE
            binding.drawer.isClickable = true
        }else{
            binding.drawer.visibility = View.INVISIBLE
            binding.drawer.isClickable = false
        }
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

                // Rotate back 90degree becasue by default its landscape
                val matrix = Matrix()
                matrix.preRotate(90F)
                val scaledBitmap = Bitmap.createScaledBitmap(bitmap, 768, 1024, true)
                val rotatedBitmap = Bitmap.createBitmap(scaledBitmap, 0, 0, scaledBitmap.width, scaledBitmap.height, matrix, true)

                val root = getExternalFilesDir(username)
                val newFileName = UUID.randomUUID().toString() + ".jpg"

                // Saving files
                println("new file name: " + newFileName)
                val imageFile = File(root, newFileName)
                rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 90, FileOutputStream(imageFile))

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
                username = data?.getStringExtra("username").toString()
                MySingleton.setUsername(username)
                output.println("${username}")
                output.close()
                refresh()
            }
            else if (requestCode == VOICE_CODE) {
                if (data == null) {
                    // Cant process voice
                    Toast.makeText(this, "Can't process your voice, please try again or remember to open your virtual mic", Toast.LENGTH_SHORT)
                } else {
                    val res : ArrayList<String> = data!!.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS) as ArrayList<String>
                    // Go to voice class to confirm
                    val intent = Intent(this, Voice::class.java )
                    intent.putExtra("speech", res[0])
                    startActivityForResult(intent, VOICE_CONFIRM_CODE)
                }

            }
            else if (requestCode == VOICE_CONFIRM_CODE) {
                Toast.makeText(this, "Success! Adhoc Voice Receipt Created!", Toast.LENGTH_SHORT).show()
                refresh()

            }
            else if (requestCode == OCR_CODE) {
                // ocr finished
                val intent = Intent(this, OCRScannerResultActivity::class.java )
                // name
                intent.putExtra("name", data!!.getStringExtra("name"))
                // amount
                intent.putExtra("amount", data!!.getStringExtra("amount"))
                // date
                intent.putExtra("date", data!!.getStringExtra("date"))
                // items
                intent.putExtra("items", data!!.getStringExtra("items"))
                // image
                intent.putExtra("image", data!!.getStringExtra("image"))
                // category
                intent.putExtra("category", data!!.getStringExtra("category"))

                startActivityForResult(intent, OCR_RESULT_CODE)
            }
            else if (requestCode == OCR_RESULT_CODE) {
                Toast.makeText(this, "Success! OCR Receipt Created!", Toast.LENGTH_SHORT).show()
                refresh()
            }
            else if (requestCode == RECEIPT_SUBMIT_CODE || requestCode == QR_RESULT_CODE) {
                refreshFragment()
            }
        }
        else if (resultCode == VIEW_RECEIPT) {
            refreshFragment()
        }
    }

    fun refresh() {
        finish()
        overridePendingTransition( 0, 0)
        startActivity(getIntent())
        overridePendingTransition( 0, 0)
    }

    fun refreshFragment() {
        val fragTransaction = supportFragmentManager.beginTransaction()
        fragTransaction.detach(transactionsFragment)
        fragTransaction.attach(transactionsFragment)
        fragTransaction.commit()
    }


}