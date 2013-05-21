package au.apai.texttask;



import android.opengl.GLSurfaceView;
import android.os.Bundle;

import android.widget.Toast;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ConfigurationInfo;
import android.graphics.PixelFormat;

/***
 * Simple Activity 
 * create our GlSurfaceView and add it to content
 * redirect change of activity's lifecycle to the view
 * @author Apai
 *
 */

public class MainActivity extends Activity {
	GLContrlosSurfaceView mGLView;
	GLControlsRender mRender;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setupGLSurface();

	}
	
	private void setupGLSurface(){
		

        // Check if the system supports OpenGL ES 2.0.
        final ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        final ConfigurationInfo configurationInfo = activityManager.getDeviceConfigurationInfo();
        final boolean supportsEs2 = configurationInfo.reqGlEsVersion >= 0x20000;

        if (supportsEs2) {
        	mGLView = new GLContrlosSurfaceView(this);
        	mGLView.setEGLContextClientVersion(2);
            // Request an OpenGL ES 2.0 compatible context.
        	mRender = new GLControlsRender(this);
        	mGLView.setRenderer(mRender);
        	
        	mGLView.getHolder().setFormat(PixelFormat.RGBA_8888);
        	mGLView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
        	
        }else {
        	Toast.makeText(this, "Device doesn't support OpenGL ES 2.0 !", Toast.LENGTH_LONG).show();
            return;
        }

        setContentView(mGLView);
	}
	
    @Override
    protected void onResume() {
        // The activity must call the GL surface view's onResume() on activity onResume().
        super.onResume();
        mGLView.onResume();
    }

    @Override
    protected void onPause() {
        // The activity must call the GL surface view's onPause() on activity onPause().
        super.onPause();
        mGLView.onPause();
    }


    
    

}
