package com.example.rth.pulldownswitchview.view;

/**
 * Created by rth on 16/11/5.
 * 这部分源码来自 http://blog.csdn.net/yanzi1225627/article/details/33339965/
 */
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLSurfaceView.Renderer;
import android.opengl.Matrix;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.AttributeSet;
import android.util.Log;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

public class CameraGLSurfaceView extends GLSurfaceView implements Renderer, SurfaceTexture.OnFrameAvailableListener {
    private static final String TAG = "yanzi";
    Context mContext;
    SurfaceTexture mSurface;
    int mTextureID = -1;
    DirectDrawer mDirectDrawer;
    HandlerThread handlerThread;
    Handler handler;
    public CameraGLSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        // TODO Auto-generated constructor stub
        mContext = context;
        setEGLContextClientVersion(2);
        setRenderer(this);
        setRenderMode(RENDERMODE_WHEN_DIRTY);
        handlerThread = new HandlerThread("camera");
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());
    }

    @Override
    public void onPause() {
        super.onPause();
        handler.post(new Runnable() {
            @Override
            public void run() {
                CameraInterface.getInstance().doStopCamera();
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        handler.post(new Runnable() {
            @Override
            public void run() {
                CameraInterface.getInstance().doStartPreview(mSurface);
            }
        });
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        // TODO Auto-generated method stub
        Log.i(TAG, "onSurfaceCreated...");
        if (mTextureID == -1) mTextureID = createTextureID();
        mSurface = new SurfaceTexture(mTextureID);
        mSurface.setOnFrameAvailableListener(this);
        mDirectDrawer = new DirectDrawer(mTextureID);
        handler.post(new Runnable() {
            @Override
            public void run() {
                CameraInterface.getInstance().doStopCamera();
            }
        });
    }
    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        // TODO Auto-generated method stub
        Log.i(TAG, "onSurfaceChanged...");
        GLES20.glViewport(0, 0, width, height);
        handler.post(new Runnable() {
            @Override
            public void run() {
                CameraInterface.getInstance().doStartPreview(mSurface);
            }
        });

    }
    @Override
    public void onDrawFrame(GL10 gl) {
        // TODO Auto-generated method stub
        Log.i(TAG, "onDrawFrame...");
        GLES20.glClearColor(1.0f, 1.0f, 1.0f, 1.0f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        mSurface.updateTexImage();
        float[] mtx = new float[16];
        mSurface.getTransformMatrix(mtx);
        mDirectDrawer.draw(mtx);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        handlerThread.quit();
    }

    private int createTextureID()
    {
        int[] texture = new int[1];

        GLES20.glGenTextures(1, texture, 0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, texture[0]);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GL10.GL_TEXTURE_MIN_FILTER,GL10.GL_LINEAR);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GL10.GL_TEXTURE_WRAP_S, GL10.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GL10.GL_TEXTURE_WRAP_T, GL10.GL_CLAMP_TO_EDGE);

        return texture[0];
    }

    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        // TODO Auto-generated method stub
        Log.i(TAG, "onFrameAvailable...");
        this.requestRender();
    }

    static class DirectDrawer {
        private final String vertexShaderCode =
                "attribute vec4 vPosition;" +
                        "attribute vec2 inputTextureCoordinate;" +
                        "varying vec2 textureCoordinate;" +
                        "void main()" +
                        "{"+
                        "gl_Position = vPosition;"+
                        "textureCoordinate = inputTextureCoordinate;" +
                        "}";

        private final String fragmentShaderCode =
                "#extension GL_OES_EGL_image_external : require\n"+
                        "precision mediump float;" +
                        "varying vec2 textureCoordinate;\n" +
                        "uniform samplerExternalOES s_texture;\n" +
                        "void main() {" +
                        "  gl_FragColor = texture2D( s_texture, textureCoordinate );\n" +
                        "}";

        private FloatBuffer vertexBuffer, textureVerticesBuffer;
        private ShortBuffer drawListBuffer;
        private final int mProgram;
        private int mPositionHandle;
        private int mTextureCoordHandle;

        private short drawOrder[] = { 0, 1, 2, 0, 2, 3 }; // order to draw vertices

        // number of coordinates per vertex in this array
        private static final int COORDS_PER_VERTEX = 2;

        private final int vertexStride = COORDS_PER_VERTEX * 4; // 4 bytes per vertex

        static float squareCoords[] = {
                -1.0f,  1.0f,
                -1.0f, -1.0f,
                1.0f, -1.0f,
                1.0f,  1.0f,
        };

        static float textureVertices[] = {
                0.0f, 1.0f,
                1.0f, 1.0f,
                1.0f, 0.0f,
                0.0f, 0.0f,
        };

        private int texture;

        public DirectDrawer(int texture)
        {
            this.texture = texture;
            // initialize vertex byte buffer for shape coordinates
            ByteBuffer bb = ByteBuffer.allocateDirect(squareCoords.length * 4);
            bb.order(ByteOrder.nativeOrder());
            vertexBuffer = bb.asFloatBuffer();
            vertexBuffer.put(squareCoords);
            vertexBuffer.position(0);

            // initialize byte buffer for the draw list
            ByteBuffer dlb = ByteBuffer.allocateDirect(drawOrder.length * 2);
            dlb.order(ByteOrder.nativeOrder());
            drawListBuffer = dlb.asShortBuffer();
            drawListBuffer.put(drawOrder);
            drawListBuffer.position(0);

            ByteBuffer bb2 = ByteBuffer.allocateDirect(textureVertices.length * 4);
            bb2.order(ByteOrder.nativeOrder());
            textureVerticesBuffer = bb2.asFloatBuffer();
            textureVerticesBuffer.put(textureVertices);
            textureVerticesBuffer.position(0);

            int vertexShader    = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
            int fragmentShader  = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);

            mProgram = GLES20.glCreateProgram();             // create empty OpenGL ES Program
            GLES20.glAttachShader(mProgram, vertexShader);   // add the vertex shader to program
            GLES20.glAttachShader(mProgram, fragmentShader); // add the fragment shader to program
            GLES20.glLinkProgram(mProgram);                  // creates OpenGL ES program executables
        }

        public void draw(float[] mtx)
        {
            GLES20.glUseProgram(mProgram);

            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, texture);

            // get handle to vertex shader's vPosition member
            mPositionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition");

            // Enable a handle to the triangle vertices
            GLES20.glEnableVertexAttribArray(mPositionHandle);

            // Prepare the <insert shape here> coordinate data
            GLES20.glVertexAttribPointer(mPositionHandle, COORDS_PER_VERTEX, GLES20.GL_FLOAT, false, vertexStride, vertexBuffer);

            mTextureCoordHandle = GLES20.glGetAttribLocation(mProgram, "inputTextureCoordinate");
            GLES20.glEnableVertexAttribArray(mTextureCoordHandle);

//        textureVerticesBuffer.clear();
//        textureVerticesBuffer.put( transformTextureCoordinates( textureVertices, mtx ));
//        textureVerticesBuffer.position(0);
            GLES20.glVertexAttribPointer(mTextureCoordHandle, COORDS_PER_VERTEX, GLES20.GL_FLOAT, false, vertexStride, textureVerticesBuffer);

            GLES20.glDrawElements(GLES20.GL_TRIANGLES, drawOrder.length, GLES20.GL_UNSIGNED_SHORT, drawListBuffer);

            // Disable vertex array
            GLES20.glDisableVertexAttribArray(mPositionHandle);
            GLES20.glDisableVertexAttribArray(mTextureCoordHandle);
        }

        private  int loadShader(int type, String shaderCode){

            // create a vertex shader type (GLES20.GL_VERTEX_SHADER)
            // or a fragment shader type (GLES20.GL_FRAGMENT_SHADER)
            int shader = GLES20.glCreateShader(type);

            // add the source code to the shader and compile it
            GLES20.glShaderSource(shader, shaderCode);
            GLES20.glCompileShader(shader);

            return shader;
        }

        private float[] transformTextureCoordinates( float[] coords, float[] matrix)
        {
            float[] result = new float[ coords.length ];
            float[] vt = new float[4];

            for ( int i = 0 ; i < coords.length ; i += 2 ) {
                float[] v = { coords[i], coords[i+1], 0 , 1  };
                Matrix.multiplyMV(vt, 0, matrix, 0, v, 0);
                result[i] = vt[0];
                result[i+1] = vt[1];
            }
            return result;
        }
    }

    static class CameraInterface {

        private static CameraInterface cameraInterface;

        private android.hardware.Camera camera;

        private boolean isPreviewing;

        private CameraInterface() {
        }

        public static synchronized CameraInterface getInstance() {
            if (cameraInterface == null) {
                cameraInterface = new CameraInterface();
            }
            return cameraInterface;
        }

        public void doStartPreview(SurfaceTexture mSurface) {
            if ( mSurface == null) return;
            if (isPreviewing()) {
                return;
            }
            try {
                camera = Camera.open();
                camera.setPreviewTexture(mSurface);
                camera.startPreview();
                setPreviewing(true);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public synchronized boolean isPreviewing() {
            return isPreviewing;
        }

        public void doStopCamera() {
            if (isPreviewing()) {
                camera.stopPreview();
                camera.release();
                camera = null;
                setPreviewing(false);
            }
        }

        public synchronized void setPreviewing(boolean previewing) {
            isPreviewing = previewing;
        }

    }

}