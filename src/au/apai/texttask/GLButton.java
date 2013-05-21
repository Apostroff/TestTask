package au.apai.texttask;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.opengl.GLES20;

import android.view.MotionEvent;

/**
 * Implementation of Button
 * 
 * @author Apai
 *
 */
public class GLButton extends GLLabel {
	public static final int ANIM_DURATION_MILIS = 500;
	public static final int[] PRESSED_STATE = {android.R.attr.state_pressed};
	private volatile boolean mPressed;
	private GLTexture mTexturePressed , mTextureUnPressed;
	private volatile long startAnimation;
	private OnClickListener listener;
	
	
	public static interface OnClickListener{
		public void onClick();
	}

	public GLButton(Context context, String text, int x, int y) {
		super(context, text, x, y);
		mMultiLine= false;
	}
	public void setOnClickListener(OnClickListener onClick){
		listener = onClick;
	}
 
	@Override
	public void prepareTexture(){
		{	// unpressed state
			Bitmap bmp = getTextureBitmap();
			mTextureUnPressed = new GLTexture(bmp);
			mU = (float)mWidth / (float)bmp.getWidth();
			mV = (float)mHeight / (float)bmp.getHeight();
			bmp.recycle();
		}		
		
		if (mDrawableBackground!=null)
			mDrawableBackground.setState(PRESSED_STATE);
		if (mDrawableImage!=null)
			mDrawableImage.setState(PRESSED_STATE);
		
		Bitmap bmp = getTextureBitmap();
		mTexturePressed = new GLTexture(bmp);
		bmp.recycle();
	}

	@Override
	public void onDraw(int textureSlot) {
		// attribute vertexes
	    GLES20.glEnableVertexAttribArray(mAttributeVertexHandle);
        GLES20.glVertexAttribPointer(mAttributeVertexHandle,2 , 
        		GLES20.GL_FLOAT, false,2*BYTES_FOR_FLOAT, mVertexBuffer);
        
        // attribute texels
        GLES20.glEnableVertexAttribArray(mAttributeTextureCoordHandle);
        GLES20.glVertexAttribPointer(mAttributeTextureCoordHandle,2,
        		GLES20.GL_FLOAT,false,2*BYTES_FOR_FLOAT, mTextureCoordsBuffer );

        // uniform ModelViewMarix
        GLES20.glUniformMatrix4fv(mMVMatrixHandler, 1, false, mModelMatrix, 0);
        // uniform z coordinate for label
        GLES20.glUniform1f(mUniformZCoordinateHandle, z);
        // uniforms textures
        long currentTime = System.currentTimeMillis();
        // if animation atm ?
        if (currentTime<startAnimation+ANIM_DURATION_MILIS){
        	float phase = (float)(currentTime - startAnimation) / (float)ANIM_DURATION_MILIS;
        	if (!mPressed) phase=1-phase;
        	GLES20.glUniform1f(mUniformState, phase);
        	
        	GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,mTextureUnPressed.getNumber());
            GLES20.glUniform1i(mUniformTextureUnPressedHandler,1);
            
            GLES20.glActiveTexture(GLES20.GL_TEXTURE2);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,mTexturePressed.getNumber());
            GLES20.glUniform1i(mUniformTexturePressedHandler,2);
        
        }else {
        	
        	if (mPressed){
        		GLES20.glUniform1f(mUniformState, 1);
        		GLES20.glActiveTexture(GLES20.GL_TEXTURE0+textureSlot);
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,mTexturePressed.getNumber());
                GLES20.glUniform1i(mUniformTexturePressedHandler,textureSlot);
        	}else {
        		GLES20.glUniform1f(mUniformState, 0);
            	GLES20.glActiveTexture(GLES20.GL_TEXTURE0+textureSlot);
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,mTextureUnPressed.getNumber());
                GLES20.glUniform1i(mUniformTextureUnPressedHandler,textureSlot);        		
        	}
        }
        
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 0, 4);

	}

	@Override
	public void onDestroy() {
		if (mTexturePressed!=null) mTexturePressed.delete();		 
		if (mTextureUnPressed!=null) mTextureUnPressed.delete();
	}

	@Override
	public boolean onTouch(MotionEvent event) {
		int ex = (int) event.getRawX();
		int ey = (int) event.getRawY();	
		
		Rect buttonRect  = new Rect(x, y, x+mWidth, y+mHeight) ;
		boolean inside = buttonRect.contains(ex, ey);
		switch(event.getAction()){
			case MotionEvent.ACTION_DOWN : 
				if (inside){
					startAnimation= System.currentTimeMillis();
					mPressed = true;
					return true;
				}				
				break;
			case  MotionEvent.ACTION_MOVE: 
				if (inside!=mPressed){
					startAnimation= System.currentTimeMillis();
					mPressed = !mPressed;					
				}
				return true;
			case MotionEvent.ACTION_UP : 
				if (inside){
					startAnimation= System.currentTimeMillis();
					mPressed = false;				
					onClick();
					return true;
				}
				break;
		}
		return false;
	}

	
	
	public void onClick(){
		if(listener!=null)
			listener.onClick();
	}


}
