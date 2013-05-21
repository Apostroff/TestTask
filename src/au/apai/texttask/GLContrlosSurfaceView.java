package au.apai.texttask;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.os.Handler;
import android.view.MotionEvent;


public class GLContrlosSurfaceView extends GLSurfaceView {
	private static final int FPS = 30; // 30 frame per second
	private volatile boolean pause = false; // if on pause 
	private Handler mHandler = new Handler();
	
    public GLContrlosSurfaceView(Context context) {
        super(context);
    }

	void reqRend(){
        mHandler.removeCallbacks(mDrawRa); // remove all callbacks mDrawRa
        if(!pause){
        	mHandler.postDelayed(mDrawRa, 1000 / FPS); // request for draw new frame
        	requestRender(); // render frame
        }
    }
	
	private final Runnable mDrawRa = new Runnable() {
        public void run() {
            reqRend();
        }
	};
	@Override
	public void onResume() {
	    super.onResume();
	    pause = false;
	    reqRend(); // start render
	}
    @Override
    public void onPause() {
        super.onPause();
        pause = true; // stop render
    }
	@Override
	public boolean onTouchEvent(final MotionEvent event) {
		this.queueEvent(new Runnable() {
			public void run() {
				((MainActivity)getContext()).mRender.onTouchEvent(event);
			}});
		return true;
	}
	
	
	    
}
