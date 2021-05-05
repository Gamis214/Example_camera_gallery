package com.example.photogallerytest

import android.Manifest
import android.R.attr
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.theartofdev.edmodo.cropper.CropImage
import com.theartofdev.edmodo.cropper.CropImageView
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException


class MainActivity : AppCompatActivity() {

    private lateinit var btn: Button
    private lateinit var btnGallery: Button
    private lateinit var imageView: ImageView

    val APP_TAG = "MyCustomApp"
    val CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE = 1034
    val PICK_GALLERY_IMAGE_CODE = 1046
    var photoFileName = "photo.jpg"
    var photoCropName = "photo"
    var photoFile: File? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        btn = findViewById(R.id.btnPhoto)
        btnGallery = findViewById(R.id.btnGallery)
        imageView = findViewById(R.id.imageView)

        btn.setOnClickListener{
            Dexter.withContext(this)
                .withPermissions(Manifest.permission.CAMERA)
                .withListener(object : MultiplePermissionsListener {
                    override fun onPermissionsChecked(multiplePermissionsReport: MultiplePermissionsReport) {
                        if (multiplePermissionsReport.areAllPermissionsGranted())
                            onLaunchCamera()
                    }

                    override fun onPermissionRationaleShouldBeShown(list: List<PermissionRequest>, permissionToken: PermissionToken) {
                        permissionToken.continuePermissionRequest()
                    }
                }).check()
        }

        btnGallery.setOnClickListener{
            Dexter.withContext(this)
                .withPermissions(Manifest.permission.READ_EXTERNAL_STORAGE)
                .withListener(object : MultiplePermissionsListener {
                    override fun onPermissionsChecked(multiplePermissionsReport: MultiplePermissionsReport) {
                        if (multiplePermissionsReport.areAllPermissionsGranted())
                            onPickPhoto()
                    }

                    override fun onPermissionRationaleShouldBeShown(list: List<PermissionRequest>, permissionToken: PermissionToken) {
                        permissionToken.continuePermissionRequest()
                    }
                }).check()
        }

    }

    fun onLaunchCamera() {
        // create Intent to take a picture and return control to the calling application
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        // Create a File reference for future access
        photoFile = getPhotoFileUri(photoFileName)

        // wrap File object into a content provider
        // required for API >= 24
        // See https://guides.codepath.com/android/Sharing-Content-with-Intents#sharing-files-with-api-24-or-higher
        val fileProvider: Uri = FileProvider.getUriForFile(this, "com.example.photogallerytest.fileprovider", photoFile!!)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, fileProvider)

        // If you call startActivityForResult() using an intent that no app can handle, your app will crash.
        // So as long as the result is not null, it's safe to use the intent.
        if (intent.resolveActivity(packageManager) != null) {
            // Start the image capture intent to take photo
            startActivityForResult(intent, CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE)
        }
    }

    // Trigger gallery selection for a photo
    fun onPickPhoto() {
        // Create intent for picking a photo from the gallery
        val intent = Intent(
            Intent.ACTION_PICK,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        )
        // If you call startActivityForResult() using an intent that no app can handle, your app will crash.
        // So as long as the result is not null, it's safe to use the intent.
        if (intent.resolveActivity(packageManager) != null) {
            // Bring up gallery to select a photo
            startActivityForResult(intent, PICK_GALLERY_IMAGE_CODE)
        }
    }

    fun loadFromUri(photoUri: Uri): Bitmap? {
        var image: Bitmap? = null
        try {
            // check version of Android on device
            image = if (Build.VERSION.SDK_INT > 27) {
                // on newer versions of Android, use the new decodeBitmap method
                val source: ImageDecoder.Source = ImageDecoder.createSource(this.contentResolver, photoUri)
                ImageDecoder.decodeBitmap(source)
            } else {
                // support older versions of Android by using getBitmap
                MediaStore.Images.Media.getBitmap(this.contentResolver, photoUri)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return image
    }

    // Returns the File for a photo stored on disk given the fileName
    fun getPhotoFileUri(fileName: String): File? {
        // Get safe storage directory for photos
        // Use `getExternalFilesDir` on Context to access package-specific directories.
        // This way, we don't need to request external read/write runtime permissions.
        val mediaStorageDir = File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), APP_TAG)

        // Create the storage directory if it does not exist
        if (!mediaStorageDir.exists() && !mediaStorageDir.mkdirs()) {
            Toast.makeText(this, "failed to create directory", Toast.LENGTH_SHORT).show()
        }

        // Return the file target for the photo based on filename
        return File(mediaStorageDir.path + File.separator + fileName)
    }

    fun reziseBitmap(takenImage: Bitmap): Bitmap? {
        val resizedBitmap: Bitmap = BitmapScaler.scaleToFill(takenImage, 800,800)
        // Configure byte output stream
        val bytes = ByteArrayOutputStream()
        // Compress the image further
        resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 40, bytes)
        // Create a new file for the resized bitmap (`getPhotoFileUri` defined above)
        val resizedFile = getPhotoFileUri(photoFileName + "_resized.jpg")
        resizedFile!!.createNewFile()
        val fos = FileOutputStream(resizedFile)
        // Write the bytes of the bitmap to file
        fos.write(bytes.toByteArray())
        fos.close()
        return BitmapFactory.decodeFile(resizedFile.absolutePath)
    }

    fun saveCropImage(cropImage: Bitmap) : Bitmap{
        val resizedBitmap: Bitmap = BitmapScaler.scaleToFill(cropImage, 800,800)
        // Configure byte output stream
        val bytes = ByteArrayOutputStream()
        // Compress the image further
        resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 40, bytes)
        // Create a new file for the resized bitmap (`getPhotoFileUri` defined above)
        val resizedFile = getPhotoFileUri(photoCropName + "_crop.jpg")
        resizedFile!!.createNewFile()
        val fos = FileOutputStream(resizedFile)
        // Write the bytes of the bitmap to file
        fos.write(bytes.toByteArray())
        fos.close()
        return BitmapFactory.decodeFile(resizedFile.absolutePath)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(resultCode == RESULT_OK){
            when(requestCode){
                CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE -> {
                    val takenPhotoUri = Uri.fromFile(getPhotoFileUri(photoFileName))
                    CropImage.activity(takenPhotoUri)
                        .setGuidelines(CropImageView.Guidelines.ON)
                        .setBackgroundColor(R.color.black)
                        .start(this)
                }
                PICK_GALLERY_IMAGE_CODE -> {
                    val photoUri: Uri = data!!.data!!
                    CropImage.activity(photoUri)
                        .setGuidelines(CropImageView.Guidelines.ON)
                        .start(this);
                }
                CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE-> {
                    val result = CropImage.getActivityResult(data)
                    val resultUri = result.uri
                    val imageCrop = BitmapFactory.decodeFile(resultUri.path)
                    imageView.setImageBitmap(saveCropImage(imageCrop))
                }
            }
        } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
            Toast.makeText(this,"ERROR CORTE",Toast.LENGTH_LONG).show()
        }

    /*override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(resultCode == RESULT_OK){
            when(requestCode){
                CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE -> {
                    // by this point we have the camera photo on disk **** 2 formas
                    val takenPhotoUri = Uri.fromFile(getPhotoFileUri(photoFileName))
                    //val takenImage = BitmapFactory.decodeFile(photoFile!!.absolutePath)
                    val rawTakenImage = BitmapFactory.decodeFile(takenPhotoUri.path)
                    // ****************************************************
                    // RESIZE BITMAP, see section below
                    var imageRezize = reziseBitmap(rawTakenImage)
                    // Load the taken image into a preview
                    imageView.setImageBitmap(imageRezize)
                }
                PICK_GALLERY_IMAGE_CODE -> {
                    val photoUri: Uri = data!!.data!!
                    // Load the image located at photoUri into selectedImage
                    val selectedImage = loadFromUri(photoUri)
                    // Load the selected image into a preview
                    imageView.setImageBitmap(selectedImage)
                }
            }
        }*/
    }


}