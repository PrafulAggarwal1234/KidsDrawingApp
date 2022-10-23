package com.example.kidsdrawingapp

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import androidx.constraintlayout.widget.ConstraintSet.Motion

//To draw some thing you need 4 basic things
//1. A bitmap to hold the pixels
//2. A Canvas to host the draw calls (writing into Bitmap)
//3. A Drawing primitive (e.g Rect,Path,text,Bitmap)
//The Path class encapsulates compound (multiple contour) geometric paths
// consisting of straight line segments, quadratic curves, and cubic curves
//4. A Paint  (to describe the colors ans styles for the drawing)



//The Path class encapsulates compound (multiple contour) geometric paths consisting of straight
// line segments, quadratic curves, and cubic curves.
// It can be drawn with canvas.drawPath(path, paint), either filled or stroked (based on the paint's Style),
// or it can be used for clipping or to draw text on a path.


class DrawingView(context: Context,attrs: AttributeSet): View(context,attrs) {
    private var mDrawPath: CustomPath? = null //3. Drawing primitive
    private var mCanvasBitmap:Bitmap? = null  //1.
    private var mDrawPaint:Paint?=null //4
    private var mCanvasPaint: Paint? = null //kinda 4 as well
    //below two variables are to input to mDraw path
    private var mBrushSize: Float = 0 .toFloat()//input
    private var color = Color.BLACK //input
    private var canvas: Canvas? = null //2.
    //to make the line not disappear when to lift back our finger we need to make an arraylist(resizable array) of all
    // path we drew and draw them thus

    private val mPaths = ArrayList<CustomPath>()
    private val mUndoPaths = ArrayList<CustomPath>()



    init {
        setUpDrawing()
    }

    //setting up Paint and little bit of Path
    private fun setUpDrawing(){
        mDrawPath= CustomPath(color,mBrushSize)
        //describing colors and styles for drawing
        mDrawPaint=Paint()
        mDrawPaint!!.color = color
        mDrawPaint!!.style=Paint.Style.STROKE
        mDrawPaint!!.strokeJoin=Paint.Join.ROUND
        mDrawPaint!!.strokeCap=Paint.Cap.ROUND
        //colors and styles describing ends
        mCanvasPaint=Paint(Paint.DITHER_FLAG)
//        mBrushSize=20.toFloat()
    }

    //editing the preexisting function onSizeChanged (inside the View)
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        //preexisting command inside the function onSizeChanged()
        //This is called during layout when the size of this view has changed.
        // If you were just added to the view hierarchy, you're called with the old values of 0.
        super.onSizeChanged(w, h, oldw, oldh)

        //Let's Construct the Bitmap
        //bitmap configuration describe how the pixels are stored.this affect quality of colors
        //(its recommended to use ARGB_8888 for good quality)
        mCanvasBitmap=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888)


        //lets construct a Canvas with specified bitmap (here our own mCanvasBitmap) to draw into
        //and put this into our already defined variable canvas
        canvas = Canvas(mCanvasBitmap!!)
    }



   // editing the preexisting function onDraw (from View class)
    //parameters:Canvas- the Canvas on which background will be drawn
    override fun onDraw(canvas: Canvas) {
       //preexisting command
        super.onDraw(canvas)

       //drawing the specified Bitmap(mCanvasBitmap) with its top left corner at (x,y) with mCanvasPaint
        canvas.drawBitmap(mCanvasBitmap!!,0f,0f,mCanvasPaint)

       //(round 2) // Drawing all the previous Paths(this is required to keep the drawn content on screen
       // otherwise once the finger is lift off everything will will be vanished)
       for(path in mPaths){
           //setting the paint to draw
           mDrawPaint!!.strokeWidth=path!!.brushThickness
           mDrawPaint!!.color = path!!.color

           //Drawing path on canvas(using the paint we just describe above)
           canvas.drawPath(path!!,mDrawPaint!!)

       }


       //making sure mDrawPath is not empty
        if(!mDrawPath!!.isEmpty){
            //now since the mDrawPath is not empty we can provide the color and brushThickness to the paint from mDrawPath
            mDrawPaint!!.strokeWidth=mDrawPath!!.brushThickness
            mDrawPaint!!.color=mDrawPath!!.color

            //Draw the specified path(mDrawPath) using specified Paint (mDrawPaint)
            canvas.drawPath(mDrawPath!!,mDrawPaint!!)
        }

    }


    //on touching the screen
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        //getting the position where the screen has been touch
        val touchX = event?.x
        val touchY = event?.y

        //defining what should happen when user do various action
        //there are three basic actions 1. ActionDown 2. Drag 3. Action Up
        //1. ActionDown:- when the user put finger on the screen
        //2.:-ActionMove:- When user move or drag the finger on the screen after touching
        //3. ActionUp:- when the user remove his finger back
        when(event?.action){
            //when we put finger on screen
            MotionEvent.ACTION_DOWN ->{
                mDrawPath!!.color=color
                mDrawPath!!.brushThickness=mBrushSize

                mDrawPath!!.reset() //making path empty and removing any lines or curves if present(this does not change the fill type setting)

                mDrawPath!!.moveTo(touchX!!,touchY!!) //moving the path to where we touched

            }

            MotionEvent.ACTION_MOVE->{
                //i want my mDraw path to be a line
                //draw a line from last point to specified point here: (touchX,touchY)
                mDrawPath!!.lineTo(touchX!!,touchY!!)
            }

            MotionEvent.ACTION_UP->{
                //adding the current path
                mPaths.add(mDrawPath!!)
                //since we have have store previous path in mPaths. Now,
                //clearing the mPath variable so it can used to store further Paths
                mDrawPath = CustomPath(color,mBrushSize)
            }

            else-> return false

        }
        //it will invalidate the whole view. IF view is visible (and thus call the onDraw method)
        invalidate()


        return true
    }


    //creating a function to set/change the brush size
    fun setBrushSize(newSize: Float){
        //mBrushSize=newSize
        // we can't do this because we also want to adjust the brush size according to screen size so
        // we have to adjust it according to screen size

        mBrushSize= TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,newSize,resources.displayMetrics)
        //this will set the mBrushSize equal to newSize but at the same time it will adjust it to the displayMetrics
        //(OR Adjust it to the size of out display)

        //nw putting the the brush size just set to the stroke width of paint
        mDrawPaint!!.strokeWidth=mBrushSize
    }

    //color code such as #FF99999 is considered as string so input to the function will be string
    fun setColor(newColor: String){
        //saving the entered color in variable named color
                    color=Color.parseColor(newColor)
        //setting the brush color to new value inside the color
        mDrawPaint!!.color=color
    }

    fun onClickUndo(){
        if(mPaths.isNotEmpty()){
        mUndoPaths.add(mPaths.removeLast())
        invalidate()// will invalidate the whole view and call the onDraw method somewhere in future
        }

    }

    fun onClickRedo(){
        if (mUndoPaths.isNotEmpty()){
        mPaths.add(mUndoPaths.removeLast())
        invalidate()
        }
    }





    internal inner class CustomPath(var color:Int,var brushThickness:Float) : Path(){

    }
}