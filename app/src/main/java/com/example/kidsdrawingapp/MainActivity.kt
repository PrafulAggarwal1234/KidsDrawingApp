package com.example.kidsdrawingapp

import android.Manifest
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.media.MediaScannerConnection
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.get
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

//To draw some thing you need 4 basic things
//1. A bitmap to hold the pixels
//2. A Canvas to host the draw calls (writing into Bitmap)
//3. A Drawing primitive (e.g Rect,Path,text,Bitmap)
//The Path class encapsulates compound (multiple contour) geometric paths
// consisting of straight line segments, quadratic curves, and cubic curves
//4. A Paint  (to describe the colors ans styles for the drawing)

class MainActivity : AppCompatActivity() {

    //CREATING A VARIABLE FOR OUR DRAWING VIEW
    private var drawingView:DrawingView?=null
    private var mImageCurrentPaint:ImageButton?=null
    private var progressDialog:Dialog?=null
    //creating a variable to request storage permission
    val openGalleryLauncher:ActivityResultLauncher<Intent> = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ){
        //RESULT_OK is result code is operation succeeded
        result->
        //making sure operation succeeded and data is not null
        if(result.resultCode == RESULT_OK && result.data!=null){
            //getting the image view where we need to set the image
            val imageBackground:ImageView = findViewById(R.id.iv_background)
            imageBackground.setImageURI(result.data?.data)
        }
    }
    private var storageResultLauncher:ActivityResultLauncher<Array<String> > = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ){
        permissions->
        permissions.entries.forEach{
            val permissionName = it.key
            val isGranted = it.value
            if(isGranted){
                Toast.makeText(this@MainActivity,"Permission for storage is granted",Toast.LENGTH_SHORT).show()

                //getting the intent
                val pickIntent = Intent(Intent.ACTION_PICK,MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                openGalleryLauncher.launch(pickIntent)

            }
            else{
                if(permissionName==Manifest.permission.READ_EXTERNAL_STORAGE){
                    Toast.makeText(this@MainActivity,"Storage's permission denied",Toast.LENGTH_SHORT).show()
                }
            }
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        drawingView=findViewById(R.id.drawing_view)
        //setting up the brush size
        drawingView!!.setBrushSize(20.toFloat())

        //creating the ibBrush (the brush size selector) Button
        val ibBrush: ImageButton=findViewById(R.id.ib_brush_size_selector)
        ibBrush.setOnClickListener(View.OnClickListener {
            showBrushSizeChooserDialog()
        }
        )



        //taking the linear layout containing all the paints
        val linearLayoutPaintColors = findViewById<LinearLayout>(R.id.ll_paint_colors)
        mImageCurrentPaint=linearLayoutPaintColors[7] as ImageButton
        mImageCurrentPaint?.setImageDrawable(
            //change the drawable of curr Image button to pallet pressed
            ContextCompat.getDrawable(this,R.drawable.pallet_pressed)
        )

        //setting up import image button
        val importImage = findViewById<ImageButton>(R.id.ib_gallery)
        importImage.setOnClickListener(View.OnClickListener {
            requestStoragePermission()
        }
        )

        //setting up undo button functionality
         val undoButton: ImageButton = findViewById(R.id.ib_undo)
        undoButton.setOnClickListener(View.OnClickListener {
            drawingView?.onClickUndo()
        }
        )

        //setting up the redo button functionality
        val redoButton:ImageButton = findViewById(R.id.ib_redo)
        redoButton.setOnClickListener(View.OnClickListener {
            drawingView?.onClickRedo()
        })

//        setting up the save image button
        val saveButton:ImageButton = findViewById(R.id.ib_save)
        saveButton.setOnClickListener{
            if(isReadStorageAllowed()) {
                showProgressDialog()
                lifecycleScope.launch {
                    val flDrawingView: FrameLayout = findViewById(R.id.fl_drawing_view_container)

                    saveBitmapFile(getBitmapFromView(flDrawingView))
                }
            }

        }

    }

    private fun showBrushSizeChooserDialog(){
        val brushDialog=Dialog(this)
        brushDialog.setContentView(R.layout.dialog_brush_size)
        brushDialog.setTitle("Brush Size: ")
        val smallBtn: ImageButton= brushDialog.findViewById(R.id.ib_small_brush)
        val mediumBtn: ImageButton= brushDialog.findViewById(R.id.ib_medium_brush)
        val largeBtn: ImageButton = brushDialog.findViewById(R.id.ib_large_brush)

        smallBtn.setOnClickListener(View.OnClickListener {
            drawingView?.setBrushSize(10.toFloat())
            brushDialog.dismiss()
        }
        )

        mediumBtn.setOnClickListener(View.OnClickListener{
            drawingView?.setBrushSize(20.toFloat())
            brushDialog.dismiss()
        }
        )

        largeBtn.setOnClickListener(View.OnClickListener {
            brushDialog.dismiss()
            drawingView?.setBrushSize(30.toFloat())
        }
        )

        //without using show the dialog won't even appear
        brushDialog.show()
    }

    //since one is being outside the MainActivity.kt (i.e in activity_main.xml) this function should not be private
    fun paintClicked(view:View){
        //if clicked on already selected paint nothing needs to be done
        // so we run function in case only if new color is pressed
        if (mImageCurrentPaint != view) {
            //setting previously pressed button's drawable back to normal
            mImageCurrentPaint!!.setImageDrawable(
                ContextCompat.getDrawable(this,R.drawable.pallet_normal)
            )

        //now  we need to change the drawable from normal to pressed so we can know which one is pressed
            val imageButton = view as ImageButton
            mImageCurrentPaint=imageButton
            imageButton.setImageDrawable(
                ContextCompat.getDrawable(this, R.drawable.pallet_pressed)
            )
            //setting the color
            drawingView?.setColor(imageButton.tag.toString())
        }
    }

    private fun requestStoragePermission(){
        if(ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.READ_EXTERNAL_STORAGE)){
            showRationalDialog("Kids drawing app","App requires Storage permission to import image")
        }
        else{
            storageResultLauncher.launch(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
            )
        }

    }
    //fun to check if he user have given the read storage permission
    private fun isReadStorageAllowed():Boolean{
        var result = ContextCompat.checkSelfPermission(this,Manifest.permission.READ_EXTERNAL_STORAGE)

        return result == PackageManager.PERMISSION_GRANTED
    }
    private fun showRationalDialog(title:String,message:String){
        val builder = AlertDialog.Builder(this)
        builder.setTitle(title)
        builder.setMessage(message)
        builder.setPositiveButton("Cancel"){dialog, _->
            dialog.dismiss()

        }
        builder.create().show()

    }

    //creating the bitmap from to view to export as image
    private fun getBitmapFromView(view:View): Bitmap {
        val returnedBitmap = Bitmap.createBitmap(view.width,view.height,Bitmap.Config.ARGB_8888)
        //getting the canvas which has all the colours
        val canvas = Canvas(returnedBitmap)
        //getting background
        val bgDrawable = view.background

        //drawing the back ground on canvas
        if(bgDrawable != null){
            //since back ground is not empty draw it on canvas
            bgDrawable.draw(canvas)
        }
        else{
            canvas.drawColor(Color.WHITE)
        }

        //draw canvas onto the view

        view.draw(canvas)

        return returnedBitmap
    }

    //function saving the bitmap
    private suspend fun saveBitmapFile(mBitmap:Bitmap?):String{
        var result = ""
        withContext(Dispatchers.IO){
            if (mBitmap != null){
                try {
                    val bytes = ByteArrayOutputStream()
                    mBitmap.compress(Bitmap.CompressFormat.PNG,90,bytes)

                    //up to toSting() it will give us the location of our application and after that with + we can add the file name after a separator
                    val f =File(externalCacheDir?.absoluteFile.toString()+File.separator+"KidsDrawingApp_"+System.currentTimeMillis()/1000+".png")

                    //open the created file f and write into it then close it and release any other system resources we may be using
                    val fo =FileOutputStream(f)
                    fo.write(bytes.toByteArray())
                    fo.close()

                    result = f.absolutePath

                    runOnUiThread(){
                            cancelProgressDialog()
                        if (result.isNotEmpty()){
                            Toast.makeText(this@MainActivity,"File saved successfully :$result",Toast.LENGTH_LONG).show()
                            shareImage(result)
                        }
                        else{
                            Toast.makeText(this@MainActivity,"Unable to save the file",Toast.LENGTH_LONG)
                        }
                    }

                }
                //catch block if try fails
                catch (e:Exception){
                    result=""
                    e.printStackTrace()
                }
            }
        }
        return result
    }

    private fun showProgressDialog(){
        progressDialog = Dialog(this@MainActivity)
        progressDialog?.setContentView(R.layout.progress_dialog)
        progressDialog?.show()
    }
    private fun cancelProgressDialog(){
        if(progressDialog!=null){
            progressDialog?.dismiss()
            progressDialog=null
        }
    }

    private fun shareImage(result: String){
        MediaScannerConnection.scanFile(this, arrayOf(result),null){
            path, uri ->
            val shareIntent = Intent()
            shareIntent.action = Intent.ACTION_SEND
            shareIntent.putExtra(Intent.EXTRA_STREAM,uri)
            shareIntent.type = "image/png"
            startActivity(Intent.createChooser(shareIntent,"Share"))
        }

    }
}