package au.apai.texttask;


import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.opengl.GLUtils;
/***
 * Class for create texture from Bitmap
 * number of created texture can be given by getNumber()
 * @author Apai
 *
 */
public class GLTexture {
    //number texture created
    private int mTextureNumber;
    public GLTexture( Bitmap bitmap ) {
        int []numbers = new int[1];        
        GLES20.glGenTextures(1, numbers, 0);
        
        mTextureNumber = numbers[0];
        
        GLES20.glPixelStorei(GLES20.GL_UNPACK_ALIGNMENT, 1);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureNumber);
 
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MIN_FILTER,
                GLES20.GL_LINEAR_MIPMAP_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MAG_FILTER,
                GLES20.GL_LINEAR);

        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_WRAP_S,
                GLES20.GL_REPEAT);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_WRAP_T,
                GLES20.GL_REPEAT);


        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
        GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_2D);
    }

    public int getNumber() {
        return mTextureNumber;
    }
    
    public void delete(){
		int[] texturesID = {mTextureNumber};
		GLES20.glDeleteTextures(1, texturesID, 0);
    }

}
