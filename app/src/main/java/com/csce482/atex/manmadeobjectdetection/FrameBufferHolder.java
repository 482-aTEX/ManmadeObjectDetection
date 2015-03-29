package com.csce482.atex.manmadeobjectdetection;

import android.opengl.GLES11Ext;
import android.opengl.GLES31;
import android.util.Log;


/**
 * Created by cloudburst on 3/21/15.
 */
public class FrameBufferHolder {

    // FBO handle.
    private int mFrameBufferHandle = -1;
    // Generated texture handles.
    private int[] mTextureHandles = {};
    // FBO textures and depth buffer size.
    private int mWidth, mHeight;

    /**
     * Binds this FBO into use and adjusts viewport to FBO size.
     */
    public void bind() {
        GLES31.glBindFramebuffer(GLES31.GL_FRAMEBUFFER, mFrameBufferHandle);
        GLES31.glViewport(0, 0, mWidth, mHeight);
    }

    /**
     * Bind certain texture into target texture. This method should be called
     * only after call to bind().
     *
     * @param index
     *            Index of texture to bind.
     */
    public void bindTexture(int index) {
        GLES31.glFramebufferTexture2D(GLES31.GL_FRAMEBUFFER,
                GLES31.GL_COLOR_ATTACHMENT0, GLES31.GL_TEXTURE_2D,
                mTextureHandles[index], 0);
    }

    /**
     * Getter for FBO height.
     *
     * @return FBO height in pixels.
     */
    public int getHeight() {
        return mHeight;
    }

    /**
     * Getter for texture ids.
     *
     * @param index
     *            Index of texture.
     * @return Texture id.
     */
    public int getTexture(int index) {
        return mTextureHandles[index];
    }

    /**
     * Getter for FBO width.
     *
     * @return FBO width in pixels.
     */
    public int getWidth() {
        return mWidth;
    }

    public void init(int width, int height, int textureCount,
                     boolean textureExternalOES) {

        // Just in case.
        reset();

        // Store FBO size.
        mWidth = width;
        mHeight = height;

        // Genereta FBO.
        int handle[] = { 0 };
        GLES31.glGenFramebuffers(1, handle, 0);
        mFrameBufferHandle = handle[0];
        GLES31.glBindFramebuffer(GLES31.GL_FRAMEBUFFER, mFrameBufferHandle);

        // Generate textures.
        mTextureHandles = new int[textureCount];
        GLES31.glGenTextures(textureCount, mTextureHandles, 0);
        int target = textureExternalOES ? GLES11Ext.GL_TEXTURE_EXTERNAL_OES
                : GLES31.GL_TEXTURE_2D;
        for (int texture : mTextureHandles) {
            GLES31.glBindTexture(target, texture);
            GLES31.glTexParameteri(GLES31.GL_TEXTURE_2D,
                    GLES31.GL_TEXTURE_WRAP_S, GLES31.GL_CLAMP_TO_EDGE);
            GLES31.glTexParameteri(target, GLES31.GL_TEXTURE_WRAP_T,
                    GLES31.GL_CLAMP_TO_EDGE);
            GLES31.glTexParameteri(target, GLES31.GL_TEXTURE_MIN_FILTER,
                    GLES31.GL_NEAREST);
            GLES31.glTexParameteri(target, GLES31.GL_TEXTURE_MAG_FILTER,
                    GLES31.GL_LINEAR);
            if (target == GLES31.GL_TEXTURE_2D) {
                GLES31.glTexImage2D(GLES31.GL_TEXTURE_2D, 0, GLES31.GL_RGBA,
                        mWidth, mHeight, 0, GLES31.GL_RGBA,
                        GLES31.GL_UNSIGNED_BYTE, null);
            }
        }

    }

    /**
     * Resets this FBO into its initial state, releasing all resources that were
     * allocated during a call to init.
     */
    public void reset() {
        int[] handle = { mFrameBufferHandle };
        GLES31.glDeleteFramebuffers(1, handle, 0);
        GLES31.glDeleteTextures(mTextureHandles.length, mTextureHandles, 0);
        mFrameBufferHandle = -1;
        mTextureHandles = new int[0];
        mWidth = mHeight = 0;
    }
}
