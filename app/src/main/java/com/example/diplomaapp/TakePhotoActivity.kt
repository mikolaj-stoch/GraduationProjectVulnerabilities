// ---------- BASED ON ----------
// https://medium.com/better-programming/how-to-upload-an-image-file-to-your-server-using-volley-in-kotlin-a-step-by-step-tutorial-23f3c0603ec2
// -----------------------------

package com.example.diplomaapp

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException

class TakePhotoActivity : AppCompatActivity() {
    // Set variables
    // Lateinit to avoid null check
    private lateinit var imageView: ImageView
    private var imageData: ByteArray? = null
    private val postURL: String = "http://192.168.0.142/send-image"
    private val postMessageURL : String = "http://192.168.0.142/send-message"

    // IMAGE_PICK_CODE to open gallery
    private val IMAGE_PICK_CODE = 999

    // CAMERA_REQUEST_CODE to open camera
    private val CAMERA_REQUEST_CODE = 0

    private var sender = ""
    private var recipient = ""
    private var message = ""


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_take_photo)

        // Assign imageView and buttons to the layout file.
        imageView = findViewById(R.id.imageView)

        sender = intent.getStringExtra("Sender")
        recipient = intent.getStringExtra("Recipient")
        message = intent.getStringExtra("Message")


        val imageButton = findViewById<Button>(R.id.imageButton)
        imageButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            // Set intent type to open gallery.
            intent.type = "image/*"
            startActivityForResult(intent, IMAGE_PICK_CODE)
        }
        val sendButton = findViewById<Button>(R.id.sendButton)
        sendButton.setOnClickListener {
            uploadImage()
        }

        val takePhotoButton = findViewById<Button>(R.id.takePhotoButton)
        takePhotoButton.setOnClickListener {
            val callCameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            if (callCameraIntent.resolveActivity(packageManager) != null){
                startActivityForResult(callCameraIntent, CAMERA_REQUEST_CODE)
            }
        }

    }

    var imageName = getRandomString(10)

    private fun uploadImage() {
        imageData?: return
        val request = object : VolleyFileUploadRequest(
            Method.POST,
            postURL,
            Response.Listener {
                Log.d("UPLOAD_IMAGE_RESPONSE","Response is: $it")
            },
            Response.ErrorListener {
                Log.d("UPLOAD_IMAGE_ERROR","Error is: $it")
            }
        ) {
            // Insert parameters for request - must match in Flask app.
            override fun getByteData(): MutableMap<String, FileDataPart> {
                var params = HashMap<String, FileDataPart>()
                params["image"] = FileDataPart(imageName , imageData!!, "jpeg")
                return params
            }
        }

        var messageData: JSONObject = JSONObject()
        messageData.put("sender",sender)
        messageData.put("recipient",recipient)
        messageData.put("message",message)
        messageData.put("imageName",imageName)

        val requestMessage =
            JsonObjectRequest(
                Request.Method.POST, postMessageURL, messageData,
                Response.Listener { response ->
                    try {
                        val jsonOutput = response.getString("status")
                        if (jsonOutput == "Passed.")
                            Log.d("PHOTO STATUS:", "SUCCESS")
                    } catch (e: JSONException) {
                        e.printStackTrace()
                        Log.d("VOLLEY",e.printStackTrace().toString())
                    }
                }, Response.ErrorListener { error -> error.printStackTrace() })

        // Add to the Queue

        Volley.newRequestQueue(this).add(request)
        Volley.newRequestQueue(this).add(requestMessage)
    }

    @Throws(IOException::class)
    private fun createImageData(uri: Uri) {
        val inputStream = contentResolver.openInputStream(uri)
        inputStream?.buffered()?.use {
            imageData = it.readBytes()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK){
            when (requestCode) {
                IMAGE_PICK_CODE -> {
                    if (resultCode == Activity.RESULT_OK) {
                        val uri = data?.data
                        if (uri != null) {
                            imageView.setImageURI(uri)
                            createImageData(uri)
                        }
                    }
                }
                CAMERA_REQUEST_CODE -> {
                    if (data != null) {
                        imageView.setImageBitmap(data.extras  ?.get("data") as Bitmap)
                    }
                }
            }
        }
        //if (resultCode == Activity.RESULT_OK && requestCode == IMAGE_PICK_CODE) {
        //    val uri = data?.data
        //    if (uri != null) {
        //        imageView.setImageURI(uri)
        //        createImageData(uri)
        //    }
        //}

    }

    fun getRandomString(length: Int): String {
        val charset = ('a'..'z') + ('A'..'Z') + ('0'..'9')
        return List(length) { charset.random() }.joinToString("")
    }
}