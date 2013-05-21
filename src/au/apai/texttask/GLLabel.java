package au.apai.texttask;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.view.MotionEvent;

public class GLLabel implements Control{
	/* 			constants 		**/
	protected static final int BYTES_FOR_FLOAT = 4; 
    public static final String SHADER_A_VERTEX	=	"a_vertex";
    public static final String SHADER_A_TEXTURE	=	"a_texture";
    public static final String SHADER_U_MVMATRIX=	"u_MVMatrix";
    public static final String SHADER_U_TEXTURE_UNPRESSED =	"u_texture_unpressed";
    public static final String SHADER_U_TEXTURE_PRESSED =	"u_texture_pressed";
    public static final String SHADER_U_STATE = "u_state";
    public static final String SHADER_U_Z =	"u_z";
    
    
	
    private static final int STATE_NEW = 0;
    private static final int STATE_INITIALIZED = 1;
    
    int mState;
    String mText;
	Paint mPaint = new Paint();
	boolean mMultiLine= true;
	private int mPaddingLeft,mPaddingRight,mPaddingTop,mPaddingBottom;
	protected Drawable mDrawableBackground;
	protected Drawable mDrawableImage;

	protected int mWidth;
	protected int mHeight;
	protected int x , y , z;
	

	private int mScreenWidth;
	private int mScreenHeight;
	/* texture parameters */
    private GLTexture mTexture;
    protected float mU;
    protected float mV;
    /* 		shaders parameters*/
    /* handles for shaders arguments */
    protected int mAttributeVertexHandle;
    protected int mAttributeTextureCoordHandle;
    protected int mMVMatrixHandler;
    protected int mUniformTexturePressedHandler;
    protected int mUniformTextureUnPressedHandler;
    protected int mUniformState;
    protected int mUniformZCoordinateHandle;
    
    protected int mProgrameHandler;
    protected FloatBuffer mVertexBuffer;
    protected FloatBuffer mTextureCoordsBuffer ;
    
    protected float[] mModelMatrix = new float[16];
    
	public GLLabel(Context context , int x , int y ){
		mScreenWidth = context.getResources().getDisplayMetrics().widthPixels;
		mScreenHeight = context.getResources().getDisplayMetrics().heightPixels;
		mWidth = mScreenWidth;	
		this.x = x;
		this.y = y;
		mPaint.setTextSize(25);
		mPaint.setColor(Color.WHITE);
	}
	public GLLabel(Context context , String text, int x , int y ){
		this(context, x ,  y);
		setText(text);
		mState = STATE_NEW;
	}
	/*	 		static methods 	**/
	/***
	 *  convert float array to FloatBuffer with native order .<br>
	 *  buffers are used for vertex attribute for shaders
	 * @param floatAray
	 * @return
	 */
    public static FloatBuffer convertToFloatBuffer(float[] floatAray){
        ByteBuffer bb = ByteBuffer.allocateDirect(floatAray.length * BYTES_FOR_FLOAT);
        bb.order(ByteOrder.nativeOrder());
        FloatBuffer res = bb.asFloatBuffer();
        res.put(floatAray);
        res.position(0);
        return res;
    }
    /**
     *  convert screen coordinates(0:scrWidth) to openGL coordinates (-1 :1)
     * @param x  x coordinate
     * @param scrWidth total  screen width
     * @return open gl coordinate
     */
    public static float convertXtoXGL(int x , int scrWidth){
        return 2*(float)x/(float)scrWidth-1;
    }

    /***
     *  convert screen coordinates(0:scrHeight) to openGL coordinates (1 :-1)
     * @param y coordinate
     * @param scrHeigth  total screen height
     * @return open gl coordinate
     */
    public static float convertYtoYGL(int y , int scrHeigth){
        return 1- 2*(float)y/(float)scrHeigth;
    }
    /***
     * return smallest power of two greater or equal then a
     * @param a
     * @return
     */
    public static int apprPow2(int a){
    	int log2a = (int) Math.ceil( (Math.log(a)/Math.log(2)));
		return  (int) Math.pow(2, log2a);
    }
	/* 		getters & setters 		**/
	public int getPaddingLeft() {
		return mPaddingLeft;
	}
	public int getPaddingRight() {
		return mPaddingRight;
	}
	public int getPaddingTop() {
		return mPaddingTop;
	}
	public int getPaddingBottom() {
		return mPaddingBottom;
	}
	public Drawable getDrawableBackground() {
		return mDrawableBackground;
	}
	public void setDrawableBackground(Drawable drawableBackground) {
		this.mDrawableBackground = drawableBackground;	
	}
	public int getZ() {
		return z;
	}
	public void setZ(int z) {
		this.z = z;
	}
	public int getWidth() {
		return mWidth;
	}
	public int getHeight(){
		return mHeight;
	}
	public void setWidth(int mWidth) {
		this.mWidth = mWidth;
	}
	public String getText(){
		return mText;
	}
	public void setText(String text){
		if (text!=null)
			mText= text;
	}
	public void setPadding(int left, int top, int right, int bottom){
		mPaddingLeft = left;
		mPaddingBottom = bottom;
		mPaddingRight =right;
		mPaddingTop = top;
	}
	public int getX() {
		return x;
	}
	public int getY() {
		return y;
	}
	
	public Drawable getDrawableImage() {
		return mDrawableImage;
	}
	public void setDrawableImage(Drawable drawableImage) {
		this.mDrawableImage = drawableImage;
	}
	
	/***
	 * make Bitmap with text and drawable background
	 * in order to OpenGL requrements 
	 * width and height of bitmap is power of two
	 * @return
	 */
	public Bitmap getTextureBitmap(){		
		Rect drawablePadding = new Rect();
		if (mDrawableBackground!=null){
			mDrawableBackground.getPadding(drawablePadding);
            mWidth = Math.max(mWidth, mDrawableBackground.getMinimumWidth());
            mHeight =  mDrawableBackground.getMinimumHeight();
		}
		int imageWidth =0 , imageHeight =0;
		if (mDrawableImage!=null){
			imageWidth = mDrawableImage.getIntrinsicWidth();
			if (imageWidth==-1) imageWidth =0;
			imageHeight = mDrawableImage.getIntrinsicHeight();
			if (imageHeight==-1) imageHeight =0;
		}
		
		int widthPadding  = mPaddingLeft+mPaddingRight+imageWidth
				+drawablePadding.left+drawablePadding.right;
		int heightPadding  = mPaddingTop+mPaddingBottom
				+drawablePadding.top+drawablePadding.bottom;
		
		Rect textRect = new Rect();
		List<String> lines = measureText(mWidth-widthPadding, textRect);
		// calc height of label max of three values - 
		// background mnHeight , text height+padding , imageHeight+padding
	
		mWidth = Math.min(mWidth, textRect.width()+widthPadding);
		
		mHeight = Math.max( mHeight , textRect.height()+heightPadding);
		mHeight = Math.max( mHeight , imageHeight+heightPadding);
		
		
		int bitmapHeight= apprPow2(mHeight);		
		int bitmapWidth	= apprPow2(mWidth);
		
		Bitmap holst = Bitmap.createBitmap(bitmapWidth, bitmapHeight, Config.ARGB_8888);
		Canvas canvas = new Canvas(holst);
		
		if (mDrawableBackground!=null){
			mDrawableBackground.setBounds(0,0,mWidth ,mHeight);
			mDrawableBackground.draw(canvas);
		}
		if (mDrawableImage!=null){
			int x = drawablePadding.left ;
			int y = mHeight/2-imageHeight/2;
			mDrawableImage.setBounds(x ,y,x+imageWidth,y+imageHeight);
			mDrawableImage.draw(canvas);
		}

		int ascent = (int) Math.ceil(-mPaint.ascent());
        int descent = (int) Math.ceil(mPaint.descent());
        if (mMultiLine)
	        for(int i=0;i<lines.size();i++){
	        	int x = drawablePadding.left+imageWidth+mPaddingLeft;
	        	int y = mPaddingTop+drawablePadding.top+i*(ascent+descent)+ascent;
	        	canvas.drawText(lines.get(i), x, y, mPaint);
	        }        
        else {
        	int x = drawablePadding.left+imageWidth+mPaddingLeft;
        	int y = (mHeight-heightPadding)/2 -(ascent+descent)/2+ascent
        				+mPaddingTop+drawablePadding.top;
        	canvas.drawText(lines.get(0), x, y, mPaint);
        }
		return holst;
	}
	/***
	 * Measure text in rect , also divide text on line in multiline case.
	 * 
	 * mText - String , which is needed to measure
	 * @param rect - result of measure , must been created before method call
	 * @return List of divided lines
	 */
	public List<String> measureText(int width  , Rect rect ){

		if (mText==null) mText = "";
		List<String> lines = new ArrayList<String>();
		rect.left =rect.top=0;
		int ascent = 0;
        int descent = 0;
        int measuredTextWidth = 0;
   
        // Paint.ascent is negative, so negate it.
        ascent = (int) Math.ceil(-mPaint.ascent());
        descent = (int) Math.ceil(mPaint.descent());
        measuredTextWidth = (int) Math.ceil(mPaint.measureText(mText));
        
        int textHeight = ascent + descent;
        int textWidth = measuredTextWidth;
		
		if (mMultiLine&&textWidth>width){			  
			int startLine =0;
			
			for(int i=0;i<mText.length();i++){
				float lineWidth =  mPaint.measureText(mText, startLine, i+1);
				if(lineWidth>width){
					lines.add(mText.substring(startLine, i));
					startLine=i;
					lineWidth=0;
				}
			}
			lines.add(mText.substring(startLine, mText.length()));
			rect.right = width;
			rect.bottom = lines.size()*textHeight;
		}else{
			rect.bottom = textHeight;
			rect.right = Math.min(textWidth, width);
			lines.add(mText);
		}

		return lines;
	}
	/*****
	 *  create OpenGL texture  , copy bitmap with text and background to GPU memory
	 *  calc texel coordinates
	 */
	public void prepareTexture(){
		Bitmap bmp = getTextureBitmap();
		mTexture = new GLTexture(bmp);
		mU = (float)mWidth / (float)bmp.getWidth();
		mV = (float)mHeight / (float)bmp.getHeight();
		bmp.recycle();
	}
	/**
	 * make vertexes FloatBuffer for shaders
	 * two dimens
	 */
	public void prepareVertexBuffer(){
		
		float glLeft = convertXtoXGL(x, mScreenWidth);
		float glRight = convertXtoXGL(x+mWidth, mScreenWidth);
		
		float glTop = convertYtoYGL(y, mScreenHeight);
		float glBottom = convertYtoYGL(y+mHeight, mScreenHeight);
		
		float[] vertexes = getVertextArray(glLeft,glTop,glRight,glBottom);
		mVertexBuffer = convertToFloatBuffer(vertexes);
	}
	/***
	 * prepare texture coordinate buffer<br>
	 * for shader attribute<br>
	 * as texture in OpenGL must be power of two  - <br>
	 * thats why texture usually more than bitmap size
	 * 
	 * so we need to create texture coordinate array 
	 * for define our bitmap in U V dimens<br>
	 * 
	 * by default array looks like:
	 * {0f,1f,1f,1f,1f,0f,0f,0f}
	 */
	public void prepareTextureCoordBuffer(){
		float[] texels = {0,mV,mU,mV,mU,0,0,0};
		mTextureCoordsBuffer = convertToFloatBuffer(texels);
	}
    /***
     *   fill vertex's array.Please, note coordinates have to be from -1 to 1
     * @param left
     * @param top
     * @param right
     * @param bottom
     */
    public float[] getVertextArray(float left , float top , float right,float bottom){
        float[] mVertextCoords = new float[4*2];
        mVertextCoords[0] = left;
        mVertextCoords[1] = bottom;

        mVertextCoords[2] = right;
        mVertextCoords[3] = bottom;

        mVertextCoords[4] = right;
        mVertextCoords[5] = top;

        mVertextCoords[6] = left;
        mVertextCoords[7] = top;
        return mVertextCoords;
    }
    
	public void initialize(int programmeHandle){
		mProgrameHandler = programmeHandle;
        Matrix.setIdentityM(mModelMatrix,0);
        
        prepareTexture();
        prepareVertexBuffer();
        prepareTextureCoordBuffer();
        mAttributeVertexHandle = GLES20.glGetAttribLocation(mProgrameHandler, SHADER_A_VERTEX);
        mAttributeTextureCoordHandle = GLES20.glGetAttribLocation(mProgrameHandler , SHADER_A_TEXTURE);
        mMVMatrixHandler = GLES20.glGetUniformLocation(mProgrameHandler,SHADER_U_MVMATRIX);
        mUniformTexturePressedHandler= GLES20.glGetUniformLocation(mProgrameHandler, 
        				SHADER_U_TEXTURE_PRESSED);
        mUniformTextureUnPressedHandler= GLES20.glGetUniformLocation(mProgrameHandler, 
				SHADER_U_TEXTURE_UNPRESSED);
        mUniformState = GLES20.glGetUniformLocation(mProgrameHandler, SHADER_U_STATE);
        mUniformZCoordinateHandle= GLES20.glGetUniformLocation(mProgrameHandler, SHADER_U_Z);
        mState = STATE_INITIALIZED;
	}
	/***
	 *  translate Model Matrix by toX-X toY-Y
	 * @param toX
	 * @param toY
	 */
    public void moveTo(int toX , int toY){

        float newX = convertXtoXGL(toX,mScreenWidth);
        float newY = convertYtoYGL(toY,mScreenHeight);

        float deltaX = newX-convertXtoXGL(x,mScreenWidth);
        float deltaY = newY-convertYtoYGL(y, mScreenHeight);

        Matrix.translateM(mModelMatrix,0,deltaX,deltaY,0f);
    }
    /**
     * Set Label new postion
     * @param x
     * @param y
     */
    @Override
    public void setPosition(int x , int y){
    	mState = STATE_NEW;
    	this.x =x;
    	this.y=y;
    	prepareVertexBuffer();
    	mState = STATE_INITIALIZED;
    }
	
	public void draw(int textureSlot){
	/*	if (mState!=STATE_INITIALIZED) {
			return ;
		}
		*/
	    GLES20.glEnableVertexAttribArray(mAttributeVertexHandle);
        GLES20.glVertexAttribPointer(mAttributeVertexHandle,2 , 
        		GLES20.GL_FLOAT, false,2*BYTES_FOR_FLOAT, mVertexBuffer);
       
        
        // атрибут текстурные координаты
        GLES20.glEnableVertexAttribArray(mAttributeTextureCoordHandle);
        GLES20.glVertexAttribPointer(mAttributeTextureCoordHandle,2,
        		GLES20.GL_FLOAT,false,2*BYTES_FOR_FLOAT, mTextureCoordsBuffer );

        // матрица вида модели
        GLES20.glUniformMatrix4fv(mMVMatrixHandler, 1, false, mModelMatrix, 0);
        // z координата спрайта 
        GLES20.glUniform1f(mUniformZCoordinateHandle, z);
        // текстура
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0+textureSlot);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,mTexture.getNumber());
        GLES20.glUniform1i(mUniformTextureUnPressedHandler,textureSlot);
        
        GLES20.glUniform1f(mUniformState, 0);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 0, 4);

        // Disable vertex array
     //   GLES20.glDisableVertexAttribArray(mAttributeVertexHandle);
    //    GLES20.glDisableVertexAttribArray(mAttributeTextureCoordHandle);
 	}
	public void onScreenSizeChange(int newWidth, int newHeight){
		this.mScreenWidth = newWidth;
		this.mScreenHeight = newHeight;
	}
	

	/* Control interface implementation */
	@Override
	public void onCreate(int programmeHandle) {
		mProgrameHandler = programmeHandle;
		
	}
	@Override
	public void onSurfaceChanged(int width, int height) {
		onScreenSizeChange(width, height);
		initialize(mProgrameHandler);
		
	}
	@Override
	public boolean onTouch(MotionEvent event) {

		return false;
	}
	@Override
	public void onDraw(int textureSlot) {
		draw(textureSlot);
		
	}
	@Override
	public void onDestroy() {
		if(mTexture!=null) mTexture.delete(); 
	}
	@Override
	public Rect getBounds() {		
		return new Rect(x,y,x+mWidth , y+mHeight);
	}
}
