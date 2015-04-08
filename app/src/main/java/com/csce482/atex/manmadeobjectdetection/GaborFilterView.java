package com.csce482.atex.manmadeobjectdetection;

import android.content.Context;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.opengl.GLES31;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Size;
import android.view.Surface;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;


public class GaborFilterView extends GLSurfaceView implements GLSurfaceView.Renderer,
        SurfaceTexture.OnFrameAvailableListener {

    private SurfaceTexture mSurfaceTexture;
    private final ShaderHelper mOesShader = new ShaderHelper();
    private final ShaderHelper mGaborShader = new ShaderHelper();
    private ByteBuffer mFullQuadVertices;
    private final FrameBufferHolder mExternalFrameBufferHolder = new FrameBufferHolder();
    private final FrameBufferHolder mOffscreenFrameBufferHolder = new FrameBufferHolder();
    private int mWidth;
    private int mHeight;
    private float[] mAspectRatio = new float[2];
    public final float mAspectRatioPreview[] = new float[2];
    private SurfaceTextureListener mSurfaceTextureListener;
    private boolean mSurfaceTextureUpdate;
    // SurfaceTexture transform matrix.
    private final float[] mTransformMat = new float[16];
    // Camera orientation matrix.
    public final float[] mOrientationMat = new float[16];

    public GaborFilterView(Context context) {
        super(context);
        init();
    }

    public GaborFilterView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        // Create full scene quad buffer.
        final byte FULL_QUAD_COORDS[] = { -1, 1, -1, -1, 1, 1, 1, -1 };
        mFullQuadVertices = ByteBuffer.allocateDirect(4 * 2);
        mFullQuadVertices.put(FULL_QUAD_COORDS).position(0);

        setPreserveEGLContextOnPause(true);
        setEGLContextClientVersion(2);
        setRenderer(this);
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
    }


    // TODO: make this get called in PreviewFragment on its GaborFilterView Object probably in openCamera()
    public void setCameraOrientationAngle(int angle) {
        Matrix.setRotateM(mOrientationMat, 0, angle, 0f, 0f, 1f);
    }

    public void setPreviewAspectRatio(Size size) {
        int height = size.getHeight();
        int width = size.getWidth();
        mAspectRatioPreview[0] = (float) Math.min(width, height) / width;
        mAspectRatioPreview[1] = (float) Math.min(width, height) / height;

    }

    @Override
    public synchronized void onDrawFrame(GL10 unused) {

        // Clear view.
        GLES31.glClearColor(0.5f, 1f, 0f, 1f);
        GLES31.glClear(GLES31.GL_COLOR_BUFFER_BIT);


        // if we have a new frame then we copy frame to our offscreen buffer
        if (mSurfaceTextureUpdate) {
            // Update surface texture.
            mSurfaceTexture.updateTexImage();
            // Update texture transform matrix.
            mSurfaceTexture.getTransformMatrix(mTransformMat);
            mSurfaceTextureUpdate = false;


            // Set the offscreen buffer as output texture/FBO
            mOffscreenFrameBufferHolder.bind();
            mOffscreenFrameBufferHolder.bindTexture(0);


            // Set the OES shader to be in use
            mOesShader.useProgram();

            // Get shader variable handles so that
            // we can pass data to the shader program
            int uOrientationMat = mOesShader.getHandle("uOrientationM");
            int uTransformMat = mOesShader.getHandle("uTransformM");

            // Set the variables in the shader program
            GLES31.glUniformMatrix4fv(uOrientationMat, 1, false, mOrientationMat, 0);
            GLES31.glUniformMatrix4fv(uTransformMat, 1, false, mTransformMat, 0);

            // Set external texture as source for render
            GLES31.glActiveTexture(GLES31.GL_TEXTURE0);
            GLES31.glBindTexture(GLES31.GL_TEXTURE_2D, mExternalFrameBufferHolder.getTexture(0));

            //render to offscreen buffer
            int aPosition = mOesShader.getHandle("aPosition");
            GLES31.glVertexAttribPointer(aPosition, 2, GLES31.GL_BYTE, false, 0, mFullQuadVertices);
            GLES31.glEnableVertexAttribArray(aPosition);
            GLES31.glDrawArrays(GLES31.GL_TRIANGLE_STRIP, 0, 4);

        }

        // Now we render from our offsceen buffer/texture to the screen buffer/texture

        // Bind screen buffer to be output.
        GLES31.glBindFramebuffer(GLES31.GL_FRAMEBUFFER, 0);
        GLES31.glViewport(0, 0, mWidth, mHeight);

        // Pass the pixel size to the gabor shader
        int uPixelSize = mGaborShader.getHandle("uPixelSize");
        GLES31.glUniform2f(uPixelSize, 1.0f / mWidth, 1.0f / mHeight);

        // Put gabor filter shader into use
        mGaborShader.useProgram();

        int uAspectRatio = mGaborShader.getHandle("uAspectRatio");
        int uAspectRatioPreview = mGaborShader.getHandle("uAspectRatioPreview");

        // TODO: get aspect ratio of preview from camera/PreviewFragment (something like camera.getPreviewSize() and then calculate it)
        GLES31.glUniform2fv(uAspectRatio, 1, mAspectRatio, 0);
        GLES31.glUniform2fv(uAspectRatioPreview, 1, mAspectRatioPreview, 0);

        // Use offscreen texture as source.
        GLES31.glActiveTexture(GLES31.GL_TEXTURE0);
        GLES31.glBindTexture(GLES31.GL_TEXTURE_2D, mOffscreenFrameBufferHolder.getTexture(0));

        // Render from offscreen buffer to  screen buffer/texture
        int aPosition = mOesShader.getHandle("aPosition");
        GLES31.glVertexAttribPointer(aPosition, 2, GLES31.GL_BYTE, false, 0, mFullQuadVertices);
        GLES31.glEnableVertexAttribArray(aPosition);
        GLES31.glDrawArrays(GLES31.GL_TRIANGLE_STRIP, 0, 4);
    }

    @Override
    public void onSurfaceCreated(GL10 unused, EGLConfig config) {

        // Load Oes Shader
        try {
            String vertexSource = loadRawString(R.raw.filter_oes_vs);
            String fragmentSource = loadRawString(R.raw.filter_oes_fs);
            mOesShader.setProgram(vertexSource, fragmentSource);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Load Gabor Shader
        try {
            String vertexSource = loadRawString(R.raw.filter_gabor_vs);
            String fragmentSource = loadRawString(R.raw.filter_gabor_fs);
            mGaborShader.setProgram(vertexSource, fragmentSource);
        } catch (Exception e) {
            e.printStackTrace();
        }
        mExternalFrameBufferHolder.reset();
        mOffscreenFrameBufferHolder.reset();

        Rect surfaceFrame = this.getHolder().getSurfaceFrame();
        Surface surface = this.getHolder().getSurface();
        //mSurfaceTextureListener.onSurfaceTextureAvailable(surface, surfaceFrame.width(), surfaceFrame.height());
    }

    @Override
    public void onSurfaceChanged(GL10 unused, int width, int height){

        // Store width and height.
        mWidth = width;
        mHeight = height;
        Log.d("onSurfaceChanged", "width: "+ mWidth + " height: "+mHeight);
        // Calculate view aspect ratio.
        mAspectRatio[0] = (float) Math.min(mWidth, mHeight) / mWidth;
        mAspectRatio[1] = (float) Math.min(mWidth, mHeight) / mHeight;

        // Initialize textures.
        if (mExternalFrameBufferHolder.getWidth() != mWidth
                || mExternalFrameBufferHolder.getHeight() != mHeight) {
            mExternalFrameBufferHolder.init(mWidth, mHeight, 1, true);
        }
        if (mOffscreenFrameBufferHolder.getWidth() != mWidth
                || mOffscreenFrameBufferHolder.getHeight() != mHeight) {
            mOffscreenFrameBufferHolder.init(mWidth, mHeight, 1, false);
        }

        // Allocate new SurfaceTexture.
        SurfaceTexture oldSurfaceTexture = mSurfaceTexture;
        mSurfaceTexture = new SurfaceTexture(mExternalFrameBufferHolder.getTexture(0));
        mSurfaceTexture.setOnFrameAvailableListener(this);
        if (mSurfaceTextureListener != null) {
            mSurfaceTextureListener.onSurfaceTextureAvailable(mSurfaceTexture, width, height);
        }
        if (oldSurfaceTexture != null) {
            oldSurfaceTexture.release();
        }

        requestRender();
    }

    public void onFrameAvailable(SurfaceTexture surfaceTexture){
        mSurfaceTextureUpdate = true;
        requestRender();
    }

    private String loadRawString(int rawId) throws Exception {
        InputStream is = getContext().getResources().openRawResource(rawId);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buf = new byte[1024];
        int len;
        while ((len = is.read(buf)) != -1) {
            baos.write(buf, 0, len);
        }
        return baos.toString();
    }

    public interface SurfaceTextureListener {
        public void onSurfaceTextureChanged(SurfaceTexture surfaceTexture, int width, int height);
        public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height);
    }

    public SurfaceTexture getSurfaceTexture() { return mSurfaceTexture; }

    public boolean isAvailable(){
        Log.d("isAvailable", "retval: " + (mSurfaceTexture != null)); return mSurfaceTexture != null; }

    public void setSurfaceTextureListener(SurfaceTextureListener l) {
        mSurfaceTextureListener = l;
    }
}
