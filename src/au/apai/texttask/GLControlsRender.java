package au.apai.texttask;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.widget.Toast;
import au.apai.texttask.GLButton.OnClickListener;
import au.apai.texttask.GLList.RowElem;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;
/****
 * Renderer of our scene with label , button and list
 * @author Apai
 */

public class GLControlsRender implements GLSurfaceView.Renderer {
	public final static int MARGINY = 5;// pixels between two elements
    public  Context mContext;
    
    List<Control> elements = new ArrayList<Control>();

    private static final String vertexShaderCode =
            "uniform mat4 u_MVMatrix;"+
            "uniform float u_z;"+
            "attribute vec2 a_vertex;" +
            "attribute vec2 a_texture;" +
            "varying vec2 v_texcoord;"+
            "void main() {" +
            "  v_texcoord = a_texture ; "+
            "  gl_Position = u_MVMatrix*vec4(a_vertex,u_z,1.0);" +
            "}";

    private static final String fragmentShaderCode =
            "precision mediump float;\n" +
            "varying vec2 v_texcoord;\n"+
            "uniform float u_state; \n"+		
            "uniform sampler2D u_texture_unpressed;\n"+
            "uniform sampler2D u_texture_pressed;\n"+
            "void main() {\n" +
            " if (u_state==0.0){\n"+
            "  	gl_FragColor = texture2D(u_texture_unpressed, v_texcoord);\n" +
            " }else {\n "+
            "	if(u_state==1.0){\n" +
            "		gl_FragColor = texture2D(u_texture_pressed, v_texcoord); \n"+
            "	}else {\n"+
            "		vec4 color = texture2D(u_texture_unpressed,v_texcoord)*(1.0-u_state);\n"+
           "		color=color+texture2D(u_texture_pressed,v_texcoord)*u_state;"+
            "		gl_FragColor = color;\n "+            	
			" 	}\n"+
            " }\n"+ 
            "}";
    
    

    int mProgramHandle =0;
    
    public GLControlsRender(Context context){
        this.mContext = context;
        addLabel("На горе стоит избушка, не избушка - а пивнушка!");
        addButton("Далее",R.drawable.ic_launcher);
        addList();
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {

       GLES20.glEnable(GLES20.GL_BLEND);
       GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);

        GLES20.glClearColor(0f,0f,0f,1f);

        mProgramHandle =  loadProgramme(vertexShaderCode,fragmentShaderCode);
        for(Control contr:elements)
        	contr.onCreate(mProgramHandle);

    }
    /**
     * add simple label with standard dialog background
     * @param text
     */
    private void addLabel(String text){
    	GLLabel label = new GLLabel(mContext , text, 0, 0);
    	label.setDrawableBackground(
    			mContext.getResources().getDrawable(android.R.drawable.dialog_frame));
    	elements.add(label);
    }
    /**
     * add button with callback on onClick method
     * @param text - caption of button
     * @param image
     */
    private void addButton(String text , int image){
    	GLButton button = new GLButton(mContext, text, 10, 100);
    	button.setDrawableBackground(mContext.getResources().getDrawable(
    			R.drawable.button_background));
    	button.setDrawableImage(mContext.getResources().getDrawable(image));
    	button.mPaint.setTextSize(32);
    	button.mPaint.setColor(Color.BLACK);
    	button.setOnClickListener(onClick);
    	elements.add(button);
    }
    /***
     * add list with 20 simple elements
     */
    private void addList(){
    	GLList list = new GLList(mContext, 0, 0);
    	List<RowElem> data = new ArrayList<GLList.RowElem>();
    	Drawable icon = mContext.getResources().getDrawable(R.drawable.ic_launcher);
    	for(int i=0;i<20;i++)
    		data.add(new RowElem(String.valueOf(i)+" element ",icon ));
    	list.setData(data);
    	list.z = -0.5f;
    	elements.add(list);
    }
    
    /***
     * calls when surface options changed 
     * need to set viewport , and init all controls 
     */
    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0,0,width,height);
        GLES20.glUseProgram(mProgramHandle);
        if (elements.size()>0){
        	elements.get(0).onSurfaceChanged(width, height);
	        for(int i=1;i<elements.size();i++){
	        	int previousBottom = elements.get(i-1).getBounds().bottom;
	        	elements.get(i).setPosition(0, previousBottom+MARGINY);
	        	elements.get(i).onSurfaceChanged(width, height);
	        }
        }
        
    }
    /***
     * onDraw callback 
     *  just send it to all controls 
     */

    @Override
    public void onDrawFrame(GL10 gl) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT|GLES20.GL_DEPTH_BUFFER_BIT);
        for (int i=elements.size()-1;i>=0;i--)
        	elements.get(i).onDraw(0);
    }
    /***
     * load and compile shader 
     * @param type - type of shader vertex or fragment
     * @param shaderCode
     * @return
     */

    public static int loadShaders(int type,String shaderCode){
        int shaderHandler = GLES20.glCreateShader(type) ;
        if (shaderHandler==0) {
            Log.e("SHADER ERROR " , "in creating  shader ");
            return 0;
        }
        GLES20.glShaderSource(shaderHandler , shaderCode);
        GLES20.glCompileShader(shaderHandler);
        final int[] compileStatus = new int[1];
        GLES20.glGetShaderiv(shaderHandler,GLES20.GL_COMPILE_STATUS,compileStatus,0);
        if (compileStatus[0]==0){
            Log.e("SHADER ERROR " , "in compile  shader ");
            GLES20.glGetShaderInfoLog(shaderHandler);
            GLES20.glDeleteShader(shaderHandler);
            return 0;
        }
        return shaderHandler;
    }
    /**
     * Build and compile shader program
     * @param vertexCode
     * @param framgentCode
     * @return
     */
    public static int  loadProgramme(String vertexCode , String framgentCode){

        int vertexShader = loadShaders(GLES20.GL_VERTEX_SHADER , vertexCode);
        int fragmentshader =loadShaders(GLES20.GL_FRAGMENT_SHADER , framgentCode);

        int programe = GLES20.glCreateProgram();
        if (programe==0){
            Log.e("SHADER ERROR","Eror create program");
            return 0;
        }
        GLES20.glAttachShader(programe,vertexShader);
        GLES20.glAttachShader(programe,fragmentshader);

        GLES20.glLinkProgram(programe);
        // Get the link status.
        final int[] linkStatus = new int[1];
        GLES20.glGetProgramiv(programe, GLES20.GL_LINK_STATUS, linkStatus, 0);
        // If the link failed, delete the program.
        if (linkStatus[0] == 0){
            GLES20.glDeleteProgram(programe);
            Log.e("PROGRAM ERROR","Eror link shaders");
            return 0;
        }
        return programe;
    }

    /**
     * delegate touch event to all controls
     * @param event
     */
    public void onTouchEvent(MotionEvent event) {
    	for(Control contr:elements)
        	contr.onTouch(event);    	
    }
    
    /***
     * on click button callback 
     * just show Simple toast message
     */
    private OnClickListener onClick= new OnClickListener() {
		
		@Override
		public void onClick() {
			((Activity)mContext).runOnUiThread(new Runnable() {
				
				@Override
				public void run() {
					Toast msg = Toast.makeText(mContext, "Ok , see below )", Toast.LENGTH_LONG);
					msg.setGravity(Gravity.CENTER, 0, 0);
					msg.show();
				}
			});
			
		}
	};

}
