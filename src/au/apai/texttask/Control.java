package au.apai.texttask;

import android.graphics.Rect;
import android.view.MotionEvent;
/**
 * Interface provides access to common of view  , 
 * which renders on scene
 */
public interface Control {
	void onCreate(int programmeHandle);
	void onSurfaceChanged(int width, int height);
	boolean onTouch(MotionEvent event);
	void onDraw(int textureSlot);
	void onDestroy();
	Rect getBounds();
	void setPosition(int left , int top);
}
