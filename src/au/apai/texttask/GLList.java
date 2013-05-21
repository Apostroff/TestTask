package au.apai.texttask;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.support.v4.view.VelocityTrackerCompat;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.animation.DecelerateInterpolator;

/***
 * implementation List of GLLabels for rendering 
 * @author Apai
 *
 */

public class GLList implements Control {
	private final float[] DIVIDER_COLOR = {1,1,1}; // color of divider lines
	// duration auto scrolling after touch up
	private final static int ANIMATION_DURATION_MILIS =1000; 
	
	private int mPaddingLeft=10;
	private int mPaddingRight=10;
	private int mProgrameHandler ;
	private int mProgrameSolidColorHandler;
	private List<GLLabel> elems = new ArrayList<GLLabel>();
	private int mScreenWidth , mScreenHeight;
	private int x , y , offset  ; // in screen dimens - pixels
	private Context mContext;
	public float z; // in openGL dimens
	private boolean mScrolling;
	/* shader attribute/uniforms */
	private int mStartScrollingPosition;
	private int mUniformMVMatrixHandle;
	private int mAttributeVertexHandler;
	private int mUniformColorHandler;
	/* vertexes buffers */
	private FloatBuffer mLinesVertexesBuffer;
	private FloatBuffer mHideSquareVertexBuffer;
	
	public float[] mModelMatrix = new float[16];
	private int mListHeight;
	private int maxOffset, minOffset , startAnimOffset ;
	private float mVelocity;
	private VelocityTracker mVelocityTracker = null;
	private DecelerateInterpolator mInterpolator = new DecelerateInterpolator();
	private long mStartAnimationTime ;
	
	private static final String vertexShaderCodeLines =
	        // This matrix member variable provides a hook to manipulate
	        // the coordinates of the objects that use this vertex shader
	        "uniform mat4 uMVMatrix;" +
	        "attribute vec4 a_vertex;" +
	        "void main() {" +
	        // the matrix must be included as a modifier of gl_Position
	        "  gl_Position = uMVMatrix * a_vertex;" +
	        "}";

	private static final String fragmentShaderCodeLines =
	        "precision mediump float;" +
	        "uniform vec3 u_color;" +
	        "void main() {" +
	        "  gl_FragColor = vec4(u_color,1.0);" +
	        "}";
	/**
	 * class for single data row in list
	 * @author Apai
	 *
	 */
	public static class RowElem{
		public String text;
		public Drawable image;
		public RowElem(String t , Drawable i){
			text = t;
			image =i;		
		}
	}
	
	public GLList(Context context, int x , int y){
		mContext = context;
		this.x = x;
		this.y = y;
	}
	
	@Override
	public void setPosition(int left , int top){
		this.x = left;
		this.y = top;
	}	
	/* Setters & getters */
	public void setPaddingLeft(int left){
		mPaddingLeft = left;
	}
	public void setPaddingRight(int right){
		mPaddingRight = right;
	}
	/***
	 *  set data of list , 
	 *  by default data labels are not multiline
	 * @param data
	 */
	public void setData(List<RowElem> data){
		clearElems();
		for (int i=0;i<data.size();i++){
			GLLabel label = new GLLabel(mContext,data.get(i).text, x, y);
			label.setDrawableImage(data.get(i).image);
			label.mMultiLine = false;
			elems.add(label);
		}
			
	}
	private void clearElems(){
		for(GLLabel label:elems){
			label.onDestroy();
		}
		elems.clear();
	}
	
	/* implementation of Control */
	@Override
	public void onCreate(int programmeHandle) {
		
		mProgrameHandler = programmeHandle;
		for(GLLabel label : elems){
			label.onCreate(programmeHandle);
		}

		mProgrameSolidColorHandler = GLControlsRender.loadProgramme(
				vertexShaderCodeLines, fragmentShaderCodeLines);
		mUniformMVMatrixHandle = GLES20.glGetUniformLocation(mProgrameSolidColorHandler, 
				"uMVMatrix");
		mUniformColorHandler =  GLES20.glGetUniformLocation(mProgrameSolidColorHandler, 
				"u_color");
		mAttributeVertexHandler =  GLES20.glGetAttribLocation(mProgrameSolidColorHandler, 
				"a_vertex");
		
		Matrix.setIdentityM(mModelMatrix, 0);
	}
	/* here place labels each after other in line by Y
	 * 
	 */
	@Override
	public void onSurfaceChanged(int width, int height) {
		mScreenWidth  = width;
		mScreenHeight = height;
		if (elems.size()>0){
			float[] line_coord= new float[(elems.size()-1)*6];
			elems.get(0).setPosition(x, y);
			elems.get(0).onScreenSizeChange(width, height);
			elems.get(0).initialize(mProgrameHandler);			
			int sumheight = elems.get(0).getHeight()+y;
			int index = 0;
			for (int i=1;i<elems.size();i++){
				elems.get(i).setPosition(x, sumheight+1);
				elems.get(i).onScreenSizeChange(width, height);
				elems.get(i).initialize(mProgrameHandler);				
				
				line_coord[index++] = GLLabel.convertXtoXGL(mPaddingLeft,mScreenWidth);
				line_coord[index++] = GLLabel.convertYtoYGL(sumheight,mScreenHeight);
				line_coord[index++] = z;

				line_coord[index++] = GLLabel.convertXtoXGL(mScreenWidth-mPaddingRight,mScreenWidth);
				line_coord[index++] = GLLabel.convertYtoYGL(sumheight,mScreenHeight);
				line_coord[index++] = z;
				
				sumheight+=elems.get(i).getHeight()+1;
			}
			// calc min and max offset for scrolling
			mListHeight = sumheight - y-1;
			maxOffset  =0;
			minOffset = mListHeight-(mScreenHeight-y);
			
			// set lines coordinates into float buffer for further drawing 
			mLinesVertexesBuffer = GLLabel.convertToFloatBuffer(line_coord);
			float[] vertexes = getVertextArray(
					GLLabel.convertXtoXGL(0, mScreenWidth),
					GLLabel.convertYtoYGL(0, mScreenHeight),
					GLLabel.convertXtoXGL(mScreenWidth, mScreenWidth),
					GLLabel.convertYtoYGL(y, mScreenHeight) ,
					z+0.1f);
			mHideSquareVertexBuffer = GLLabel.convertToFloatBuffer(vertexes);
		}
	}

	private void checkVelocityTracker(boolean clear){
		if(mVelocityTracker == null) {
            // Retrieve a new VelocityTracker object to watch the velocity of a motion.
            mVelocityTracker = VelocityTracker.obtain();
        }
        else {
            // Reset the velocity tracker back to its initial state.
            if (clear) mVelocityTracker.clear();
        }
	}
	@Override
	public boolean onTouch(MotionEvent event) {
		int ex = (int) event.getRawX();
		int ey = (int) event.getRawY();			
		Rect listRect  = getBounds();
		boolean inside = listRect.contains(ex, ey);
		
        // need for velocity
		int index = event.getActionIndex();
        int pointerId = event.getPointerId(index);
        
		switch(event.getAction()){
			case MotionEvent.ACTION_DOWN : 
				checkVelocityTracker(true);
                // Add a user's movement to the tracker.
                mVelocityTracker.addMovement(event);
                // fix user start point for draging
				if (inside)	{
					mStartAnimationTime = 0;
					mScrolling=true;
					mStartScrollingPosition = ey;
					return true;
				}
				break;	
			case MotionEvent.ACTION_MOVE: 
				checkVelocityTracker(false);
				mVelocityTracker.addMovement(event);
				mVelocityTracker.computeCurrentVelocity(1000);
				mVelocity =- VelocityTrackerCompat.getYVelocity(mVelocityTracker,pointerId)/5.0f;
				if (inside&&mScrolling){
					offset +=  mStartScrollingPosition-ey;
					offset = Math.min(offset, minOffset);
					offset = Math.max(offset, maxOffset);
					mStartScrollingPosition = ey;
					return true;
				}else 
					mScrolling = false;
				break;
				
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                // Return a VelocityTracker object back to be re-used by others.
                mVelocityTracker.recycle();
				mScrolling = false;
				
				mStartAnimationTime = System.currentTimeMillis();
				startAnimOffset = offset;
		}

		return false;
	}

	@Override
	public void onDraw(int textureSlot) {
		// if still animated after scrolling
		long currentTime  = System.currentTimeMillis();
		if (!mScrolling&&currentTime<mStartAnimationTime+ANIMATION_DURATION_MILIS){
			float part = (float)(currentTime-mStartAnimationTime)/(float)ANIMATION_DURATION_MILIS;
			float power = mInterpolator.getInterpolation(part);
			offset =(int) (startAnimOffset+mVelocity*power);
			offset = Math.min(offset, minOffset);
			offset = Math.max(offset, maxOffset);
		}
		Matrix.setIdentityM(mModelMatrix, 0);
		Matrix.translateM(mModelMatrix, 0, 0, (float)offset*2/(float)mScreenHeight, 0);
		
		for(GLLabel label:elems ){
			label.mModelMatrix = mModelMatrix;
			label.onDraw(textureSlot);
		}
		GLES20.glUseProgram(mProgrameSolidColorHandler);
		
		drawLines();
        drawHideSq();
        GLES20.glUseProgram(mProgrameHandler);
	}
	private void drawHideSq() {
		GLES20.glEnableVertexAttribArray(mAttributeVertexHandler);
        GLES20.glVertexAttribPointer(mAttributeVertexHandler,3 , 
        		GLES20.GL_FLOAT, false,3*GLLabel.BYTES_FOR_FLOAT, mHideSquareVertexBuffer);
        // clear Model matrix  , for deny square scrolling
        Matrix.setIdentityM(mModelMatrix, 0);
        GLES20.glUniformMatrix4fv(mUniformMVMatrixHandle, 1, false, mModelMatrix, 0);
        
        GLES20.glUniform3f(mUniformColorHandler, 0.5f, 0.5f, 0.5f);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 0, 4);
		
	}

	/***
	 * draw separate line between list rows
	 */
	private void drawLines(){
		GLES20.glEnableVertexAttribArray(mAttributeVertexHandler);
        GLES20.glVertexAttribPointer(mAttributeVertexHandler,3 , 
        		GLES20.GL_FLOAT, false,3*GLLabel.BYTES_FOR_FLOAT, mLinesVertexesBuffer);

        GLES20.glUniformMatrix4fv(mUniformMVMatrixHandle, 1, false, mModelMatrix, 0);
        
        GLES20.glUniform3f(mUniformColorHandler, DIVIDER_COLOR[0], DIVIDER_COLOR[1], DIVIDER_COLOR[2]);

        GLES20.glDrawArrays(GLES20.GL_LINES, 0, (elems.size()-1)*2);
	}
	
    public float[] getVertextArray(float left , float top , float right,float bottom,float zc){
        float[] mVertextCoords = new float[4*3];
        mVertextCoords[0] = left;
        mVertextCoords[1] = bottom;
        mVertextCoords[2] = zc;

        mVertextCoords[3] = right;
        mVertextCoords[4] = bottom;
        mVertextCoords[5] = zc;

        mVertextCoords[6] = right;
        mVertextCoords[7] = top;
        mVertextCoords[8] = zc;
        
        mVertextCoords[9] = left;
        mVertextCoords[10] = top;
        mVertextCoords[11] = zc;
        return mVertextCoords;
    }

	@Override
	public void onDestroy() {
		clearElems();
	}


	@Override
	public Rect getBounds() {	
		return new Rect(x,y,mScreenWidth , mScreenHeight);
	}

}
