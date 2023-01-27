package `in`.tutorial.drawapp

import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.icu.text.CaseMap.Title
import android.media.MediaScannerConnection
import android.os.Binder
import android.os.Build
import androidx.lifecycle.lifecycleScope
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Message
import android.provider.MediaStore
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.get
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {
    private var drawingView:DrawingView? = null
    private var mImageBtnCurrPaint: ImageButton? = null
    private val openGalleryLauncher:ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
            result->
            if(result.resultCode == RESULT_OK && result.data!=null) {
                val image:ImageView = findViewById(R.id.iv_background)
                image.setImageURI(result.data?.data)
            }
        }
    private val requestPermission:ActivityResultLauncher<Array<String>> = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()){
        perms ->
        perms.entries.forEach{
            val permName = it.key
            val status = it.value
            if(status){
                Toast.makeText(this, "Permission Granted", Toast.LENGTH_SHORT).show()
                val pickIntent = Intent(Intent.ACTION_PICK,
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                openGalleryLauncher.launch(pickIntent)
            }else{
                if(permName == android.Manifest.permission.READ_EXTERNAL_STORAGE){
                    Toast.makeText(this, "Permission Denied for external storage",
                        Toast.LENGTH_SHORT).show()
                }
                Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show()
            }
        }
    }
    var customProgressDialog:Dialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        drawingView = findViewById(R.id.drawing_view)
        drawingView?.setSizeForBrush(20.toFloat())
        val linearLayoutPaintColors = findViewById<LinearLayout>(R.id.ll_paint_colors)
        mImageBtnCurrPaint = linearLayoutPaintColors[2] as ImageButton
        mImageBtnCurrPaint!!.setImageDrawable(
            ContextCompat.getDrawable(this, R.drawable.pallet_pressed))

        val ib_brush:ImageButton = findViewById(R.id.ib_brush)
        ib_brush.setOnClickListener{showBrushSizeChooserDialog()}

        val ib_file:ImageButton = findViewById(R.id.ib_file)
        ib_file.setOnClickListener { checkForPermissions() }

        val ib_undo:ImageButton = findViewById(R.id.ib_undo)
        ib_undo.setOnClickListener { drawingView?.onClickUndo() }

        val ib_save:ImageButton = findViewById(R.id.ib_save)
        ib_save.setOnClickListener {
            if(isReadStorageAllowed()){
                customProgressDialogFunction()
                lifecycleScope.launch{
                    val fl:FrameLayout = findViewById(R.id.fl_draw_view_container)
                    saveBitmapFile(getBitmapFromView(fl))
                }
            }
        }
    }

    private fun isReadStorageAllowed():Boolean{
        var result = ContextCompat.checkSelfPermission(this,
            android.Manifest.permission.READ_EXTERNAL_STORAGE)
        return result == PackageManager.PERMISSION_GRANTED
    }

    fun checkForPermissions(){
        if(ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.READ_EXTERNAL_STORAGE)){
            showRationaleDialog("Draw App", "For image loading")
        }else{
            requestPermission.launch(arrayOf( android.Manifest.permission.READ_EXTERNAL_STORAGE,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE))
        }
    }

    fun showRationaleDialog(title: String, message: String){
        val builder: androidx.appcompat.app.AlertDialog.Builder = androidx.appcompat.app.AlertDialog.Builder(this)
        builder.setTitle(title)
            .setMessage(message)
            .setPositiveButton("Submit"){dialog, _->
                dialog.dismiss()
            }
        builder.create().show()
    }

    fun paintClicked(view: View){
        if(view !== mImageBtnCurrPaint){
            val imageButton = view as ImageButton
            val colorTag = imageButton.tag.toString()
            drawingView?.setColor(colorTag)
            mImageBtnCurrPaint!!.setImageDrawable(
                ContextCompat.getDrawable(this, R.drawable.pallet_normal))
            imageButton!!.setImageDrawable(
                ContextCompat.getDrawable(this, R.drawable.pallet_pressed))
            mImageBtnCurrPaint = imageButton
        }
    }

    private fun showBrushSizeChooserDialog(){
        val brushDialog = Dialog(this)
        brushDialog.setContentView(R.layout.dialog_brush_size)
        brushDialog.setTitle("Size Selector : ")
        val smallBtn = brushDialog.findViewById<ImageButton>(R.id.ib_small_brush)
        smallBtn.setOnClickListener{
            drawingView?.setSizeForBrush(10.toFloat())
            brushDialog.dismiss()
        }
        val medBtn = brushDialog.findViewById<ImageButton>(R.id.ib_medium_brush)
        medBtn.setOnClickListener{
            drawingView?.setSizeForBrush(30.toFloat())
            brushDialog.dismiss()
        }
        val largeBtn = brushDialog.findViewById<ImageButton>(R.id.ib_large_brush)
        largeBtn.setOnClickListener{
            drawingView?.setSizeForBrush(60.toFloat())
            brushDialog.dismiss()
        }
        brushDialog.show()
    }

    private fun getBitmapFromView(view:View):Bitmap{
        val returnedBitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(returnedBitmap)
        val bgDrawable = view.background
        if(bgDrawable!=null){
            bgDrawable.draw(canvas)
        }else{
            canvas.drawColor(Color.WHITE)
        }
        view.draw(canvas)
        return returnedBitmap
    }

    private suspend fun saveBitmapFile(mBitmap:Bitmap?):String{
        var result = ""
        withContext(Dispatchers.IO){
            mBitmap?.let {
                try {
                    val hbyte = ByteArrayOutputStream()
                    mBitmap.compress(Bitmap.CompressFormat.PNG, 90, hbyte)
                    val f = File(externalCacheDir?.absoluteFile.toString()
                            + File.separator + "DrawApp_"+System.currentTimeMillis()/1000+".png")
                    val fo = FileOutputStream(f)
                    fo.write(hbyte.toByteArray())
                    fo.close()
                    result = f.absolutePath
                    runOnUiThread{
                        if(result.isNotEmpty()){
                            Toast.makeText(this@MainActivity,
                                "File saved $result", Toast.LENGTH_SHORT).show()
                            shareImage(result)
                        }else{
                            Toast.makeText(this@MainActivity,
                                "File not saved ", Toast.LENGTH_SHORT).show()
                        }
                        cancelProgressDialog()
                    }
                }catch (e: Exception){
                        result = "";
                        e.printStackTrace()
                }
            }
        }
        return result
    }

    private fun cancelProgressDialog(){
        if(customProgressDialog != null){
            customProgressDialog?.hide()
            customProgressDialog = null
        }
    }

    private fun customProgressDialogFunction(){
        customProgressDialog = Dialog(this@MainActivity)
        customProgressDialog?.setContentView(R.layout.dialog_custom_progress)
        customProgressDialog?.show()
    }

    private fun shareImage(result:String){
        MediaScannerConnection.scanFile(this, arrayOf(result), null){
            path, uri ->
            val shareIntent = Intent()
            shareIntent.action = Intent.ACTION_SEND
            shareIntent.putExtra(Intent.EXTRA_STREAM, uri)
            shareIntent.type = "image/png"
            startActivity(Intent.createChooser(shareIntent, "Share"))
        }
    }
}
