package be.csmmi.zombiegame.app;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import be.csmmi.zombiegame.rendering.CameraViewRenderer;

import com.google.vrtoolkit.cardboard.CardboardView;

public class CameraView extends CardboardView implements Callback{
	private final static String TAG = CameraView.class.getSimpleName();
	
	private CameraViewRenderer renderer;
	
	public CameraView(Context context) {
		this(context,null);
	}
	
	public CameraView(Context context, AttributeSet attrs) {
		super(context, attrs);
		renderer = new CameraViewRenderer(this);
		setEGLContextClientVersion(2);
		setRenderer(renderer);
		
		setVRModeEnabled(true);
		setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
		setDistortionCorrectionEnabled(true);
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		super.surfaceDestroyed(holder);
	}
	
	/**
	 * Set cameraPreview in surfaceHolder after surface was created.
	 */
	@Override
	public void surfaceCreated(SurfaceHolder holder) {
//		renderer.getArRenderer().startCamera();
		super.surfaceCreated(holder);
	}
	
	public CameraViewRenderer getRenderer() {
		return renderer;
	}
	
	/**
	 * When surface has change we can determine the best preview size,
	 * set the camera parameters to this size and start the preview.
	 */
	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int w,
			int h) {
		super.surfaceChanged(holder, format, w, h);
		if(AppConfig.DEBUG_LOGGING) Log.d(TAG, "Width: "+w+",Height: "+h);
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if(AppConfig.DEBUG_LOGGING) Log.d(TAG, "Touch noticed");
		AppConfig.TOUCH_EVENT = true;
		return super.onTouchEvent(event);
	}

}
