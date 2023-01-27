package `in`.tutorial.drawapp

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import java.lang.reflect.Type

class DrawingView(context:Context, attrs:AttributeSet) : View(context, attrs){

    private var mDrawPath: CustomPath? = null
    private var mCanvasBitmap: Bitmap? = null
    private var mDrawPaint: Paint? = null
    private var mCanvasPaint: Paint? = null
    private var mBrushSize: Float = 0.0F
    private var mColor = Color.BLACK
    private var canvas:Canvas? = null
    private val mPaths = ArrayList<CustomPath>()
    private val undoPaths = ArrayList<CustomPath>()

    init {
        setupDrawing()
    }
    private fun setupDrawing(){
        mDrawPaint = Paint()
        mDrawPath = CustomPath(mColor, mBrushSize)
        mDrawPaint!!.color = mColor
        mDrawPaint!!.style = Paint.Style.STROKE
        mDrawPaint!!.strokeJoin = Paint.Join.ROUND
        mDrawPaint!!.strokeCap = Paint.Cap.ROUND
        mCanvasPaint = Paint(Paint.DITHER_FLAG)
        //mBrushSize = 20.toFloat()

    }
    fun setSizeForBrush(newSize:Float){
        mBrushSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, newSize,
            resources.displayMetrics)
        mDrawPaint!!.strokeWidth = mBrushSize
    }
    internal inner class CustomPath(var color:Int,
                                    var brushThisckness:Float) : Path(){

    }

    fun setColor(newColor:String){
        mColor = Color.parseColor(newColor)
        mDrawPaint!!.color = mColor
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        mCanvasBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        canvas = Canvas(mCanvasBitmap!!)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawBitmap(mCanvasBitmap!!, 0f, 0f, mCanvasPaint)
        for(m in mPaths){
            mDrawPaint!!.strokeWidth = m!!.brushThisckness
            mDrawPaint!!.color = m!!.color
            canvas.drawPath(m!!, mDrawPaint!!)
        }
        if(!mDrawPath!!.isEmpty){
            mDrawPaint!!.strokeWidth = mDrawPath!!.brushThisckness
            mDrawPaint!!.color = mDrawPath!!.color
            canvas.drawPath(mDrawPath!!, mDrawPaint!!)
        }
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {

        val touchX = event?.x;
        val touchY = event?.y
        when(event?.action){
            MotionEvent.ACTION_DOWN->{
                mDrawPath!!.color = mColor
                mDrawPath!!.brushThisckness = mBrushSize
                mDrawPath!!.reset()
                if(touchX != null && touchY!=null) {
                    mDrawPath!!.moveTo(touchX, touchY)
                }
            }
            MotionEvent.ACTION_MOVE ->{
                if(touchX != null && touchY!=null) {
                    mDrawPath!!.lineTo(touchX, touchY)
                }
            }
            MotionEvent.ACTION_UP ->{
                if(touchX != null && touchY!=null) {
                    mDrawPath = CustomPath(mColor, mBrushSize)
                    mPaths.add(mDrawPath!!)
                }
            }
            else -> return false
        }
        invalidate()
        return true

    }

    fun onClickUndo(){
        if(mPaths.size > 0){
            undoPaths.add(mPaths.removeAt(mPaths.size-1))
            invalidate()
        }
    }
}