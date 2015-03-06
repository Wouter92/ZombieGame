package be.csmmi.zombiegame.rendering;

import java.io.IOException;
import java.net.URI;
import java.nio.Buffer;
import java.nio.IntBuffer;
import java.util.ArrayList;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;
import javax.xml.ws.Response;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.android.volley.RequestQueue;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ImageFormat;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.graphics.SurfaceTexture.OnFrameAvailableListener;
import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnPreparedListener;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLSurfaceView.Renderer;
import android.opengl.Matrix;
import android.os.AsyncTask;
import android.util.Log;
import android.view.Surface;
import be.csmmi.zombiegame.app.AppConfig;
import be.csmmi.zombiegame.rendering.meshes.FullSquadMesh;
import be.csmmi.zombiegame.utilities.RenderingUtils;

public class ArRenderer implements Renderer, PreviewCallback, OnFrameAvailableListener, OnPreparedListener {
	
	private final static String TAG = ArRenderer.class.getSimpleName();
	private GLSurfaceView view;
	
	private Camera camera;
	private int[] tex;
	private SurfaceTexture st;
	private Buffer pTexCoordVBG;
	private Buffer pVertexVBG;
	private Context context;
	
    private float[] mvp = new float[16];
    
    // RENDER TO TEXTURE VARIABLES
 	int[] fb, depthRb, renderTex; // the framebuffer, the renderbuffer and the texture to render
 	int texW = AppConfig.PREVIEW_RESOLUTION[0];           // the texture's width
 	int texH = AppConfig.PREVIEW_RESOLUTION[1];           // the texture's height
 	IntBuffer texBuffer;          //  Buffer to store the texture
 	
 	// RENDERING HANDLERS
 	private int[] programId;
 	private int vTexCoordHandler;
	private int sTextureHandler;
	private int[] vPositionHandler;
    private int[] mvpHandler;
    
    private MediaPlayer mp;
    private boolean updateSurface = false;
    
    private ArrayList<String> urls = new ArrayList<String>();
    private int currentCamIndex = 0;
	
    // CAMERA SHADERS
	private final String vssCamera =
		"attribute vec2 vPosition;\n" +
		"attribute vec2 vTexCoord;\n" +
		"uniform mat4 u_MVP;\n" +
		"varying vec2 texCoord;\n" +
		"void main() {\n" +
		"  texCoord = vTexCoord;\n" +
		"  gl_Position = u_MVP * vec4( vPosition.x, vPosition.y, 0.0, 1.0 );\n" +
		"}";
 
	private final String fssCamera =
		"#extension GL_OES_EGL_image_external : require\n" +
		"precision mediump float;\n" +
		"uniform samplerExternalOES sTexture;\n" +
		"varying vec2 texCoord;\n" +
		"void main() {\n" +
		"  gl_FragColor = texture2D(sTexture,texCoord);\n" +
		"}";
	
	public ArRenderer(GLSurfaceView view, Context context) {
		this.view = view;
		this.context = context;
	}
	
	/**
	 ***********
	 * GETTERS *
	 ***********
	 */
	
	public Camera getCamera() {
		return camera;
	}
	
	public int getTextureId() {
		return renderTex[0];
	}

	/**
	 ****************************
	 * CAMERA CONTROL FUNCTIONS *
	 ****************************
	 */
	
	public void startCamera() {
		if(AppConfig.DEBUG_LOGGING) Log.d(TAG, "Starting camera...");
		
		if(camera == null) {
			camera = Camera.open();
		}
//		try {
////			camera.setPreviewTexture(st);
//		} catch (IOException e) {
//		}
		
//		try {
//	        MediaPlayer player = new MediaPlayer(this.context);
//	        Surface surface = new Surface(st);
//
//	        player.setSurface(surface);
//
//	        player.setDataSource(this.context, Uri.parse("http://10.43.99.13:8080/ms"));
//
//	        player.prepare();
//
//	        player.setOnPreparedListener(new OnPreparedListener() {
//
//	            @Override
//	            public void onPrepared(MediaPlayer mp) {
//	                Log.d("SimpleVideoPlayer", "Starting player");
//	                mp.start();
//	            }
//	        });
//	        
//	        player.setOnErrorListener(new OnErrorListener() {
//				
//				@Override
//				public boolean onError(MediaPlayer mp, int what, int extra) {
//					Log.d("SimpleVideoPlayer", "error with code: " + what);
//					return false;
//				}
//			});
//	        
//	        player.start();
//
//	    }catch(Exception e) {
//	        e.printStackTrace();
//	    }
	}

	public void stopCamera() {
		if(AppConfig.DEBUG_LOGGING) Log.d(TAG, "Stopping camera...");
		
		if (camera != null) {
			camera.stopPreview();
			camera.setPreviewCallback(null);
			camera.release();
			camera = null;
		}
	}

	/**
	 **************************
	 * SURFACE INIT FUNCTIONS *
	 **************************
	 */
	private MjpegInputStream source = null;
	private MjpegViewThread thread;
	public final static int SIZE_STANDARD   = 1; 
    public final static int SIZE_BEST_FIT   = 4;
    public final static int SIZE_FULLSCREEN = 8;
    public int IMG_WIDTH=640;
	public int IMG_HEIGHT=480;
	private boolean mRun = false;
    private boolean surfaceDone = false;  
    private Bitmap bmp = null;
    private Surface s;
	
	public class MjpegViewThread extends Thread {
        private Surface surface;

         
        public MjpegViewThread(Surface surface, Context context) { 
            this.surface = surface; 
        }

        public void run() {
        	long start = System.currentTimeMillis();
        	
            Log.d("MJPEG","Reading begins ... ");
            Paint p = new Paint();
            while(true) {
            	Log.d("MJPEG", "Running: "+mRun);
	            while (mRun) {
	            	if((System.currentTimeMillis() - start)/1000.0 > 10) {
	            		start = System.currentTimeMillis();
	            		currentCamIndex = (currentCamIndex+1) % urls.size();
	            		Log.d("CamIDX", "Camidx: "+currentCamIndex);
	            		new DoRead().execute(urls.get(currentCamIndex));
            			mRun = false;
            			break;
	            	}
	            	Log.d("CamIDX", "CAM: "+currentCamIndex);
	            	
	                Canvas c = null;
	
	                if(surfaceDone) {   
	                	try {
	                		if(bmp==null){
	                			bmp = Bitmap.createBitmap(IMG_WIDTH, IMG_HEIGHT, Bitmap.Config.ARGB_8888);
	                		}
	                		int ret = source.readMjpegFrame(bmp);
	
	                		if(ret == -1)
	                		{
	                			Log.d("MJPEG", "Error while reading frame");
	                			new DoRead().execute(urls.get(currentCamIndex));
	                			mRun = false;
	                			break;
	                		}
	                        
	                        c = surface.lockCanvas(null);
	                        synchronized (surface) {
	                               	c.drawBitmap(bmp, new Rect(0, 0, bmp.getWidth(), bmp.getHeight()), new Rect(0, 0, c.getWidth(), c.getHeight()), p);
	                        }
	
	                    }catch (IOException e){ 
	                }finally { 
	                    	if (c != null) {
	                    		surface.unlockCanvasAndPost(c); 
	                    		Log.d("MJPEG", "Canvas unlocked!");
	                    		view.requestRender();
	                    	}
	                    }
	                }
	            }
            }
        }
    }
	
	public class DoRead extends AsyncTask<String, Void, MjpegInputStream> {
        protected MjpegInputStream doInBackground(String... url) {
            HttpResponse res = null;         
            DefaultHttpClient httpclient = new DefaultHttpClient(); 
            HttpParams httpParams = httpclient.getParams();
            HttpConnectionParams.setConnectionTimeout(httpParams, 5*1000);
            HttpConnectionParams.setSoTimeout(httpParams, 5*1000);
            try {
                res = httpclient.execute(new HttpGet(URI.create(url[0])));
                if(res.getStatusLine().getStatusCode()==401){
                    //You must turn off camera User Access Control before this will work
                    return null;
                }
                return new MjpegInputStream(res.getEntity().getContent());  
            } catch (ClientProtocolException e) {
	                e.printStackTrace();
	                Log.d(TAG, "Request failed-ClientProtocolException", e);
                //Error connecting to camera
            } catch (IOException e) {
	                e.printStackTrace();
	                Log.d(TAG, "Request failed-IOException", e);
                //Error connecting to camera
            }
            return null;
        }

        protected void onPostExecute(MjpegInputStream result) {
        	
        	Log.d("CONN", "Source was set to: "+source);
        	if(result == null) {
        		Log.d("CONNECTION", "Trying to connect");
        		new DoRead().execute(urls.get(currentCamIndex));
        		return;
        	}
        	
            source = result;
            if(result!=null){
            	result.setSkip(1);
            }
            
            if(thread==null){
            	thread = new MjpegViewThread(s, context);
            }
            Log.d("CONN", "Tread starts ... ");
            
            mRun = true;  
            surfaceDone = true;
            
            if(!thread.isAlive()) {
            	thread.start(); 
            }
        }
    }
	
	@Override
	public void onSurfaceCreated(GL10 unused, EGLConfig config) {
		initFrameBuffer();
		
		setupCameraTex();
		
		GLES20.glClearColor(1.0f, 1.0f, 0.0f, 1.0f);
		GLES20.glDisable(GLES20.GL_DEPTH_TEST);
		
		setupRenderHandlers();
		
		
		// Instantiate the RequestQueue.
		RequestQueue queue = Volley.newRequestQueue(this.context);
		String urlJsonArry = "http://192.168.137.1:8082/getcams";

		JsonArrayRequest req = new JsonArrayRequest(urlJsonArry,
	            new com.android.volley.Response.Listener<JSONArray>() {
	                @Override
	                public void onResponse(JSONArray response) {
	                    Log.d(TAG, response.toString());
	 
	                    try {
	                        // Parsing json array response
	                        // loop through each json object
//	                        jsonResponse = "";
	                        for (int i = 0; i < response.length(); i++) {
	 
	                            JSONObject cams = (JSONObject) response.get(i);
	 
	                            String name = cams.getString("name");
	                            String address = cams.getString("address");
	                            
	                            Log.d("JSON", "Name: "+name+", Address: "+address);
	                            
	                            urls.add("http://"+address);
	                        }
	                        currentCamIndex = 0;
	                        
	                        s = new Surface(st);
	                        new DoRead().execute(urls.get(currentCamIndex));
	 
//	                        txtResponse.setText(jsonResponse);
	 
	                    } catch (JSONException e) {
	                    	Log.d("JSON", "JSON ERROR!");
	                        e.printStackTrace();
//	                        Toast.makeText(getApplicationContext(),
//	                                "Error: " + e.getMessage(),
//	                                Toast.LENGTH_LONG).show();
	                    }
	 
//	                    hidepDialog();
	                }
	            }, new com.android.volley.Response.ErrorListener() {
	                @Override
	                public void onErrorResponse(VolleyError error) {
	                    VolleyLog.d(TAG, "Error: " + error.getMessage());
//	                    Toast.makeText(getApplicationContext(),
//	                            error.getMessage(), Toast.LENGTH_SHORT).show();
//	                    hidepDialog();
	                }
	            });
		
		// Add the request to the RequestQueue.
		queue.add(req);
		
		
		/*
         * Create the SurfaceTexture that will feed this textureID,
         * and pass it to the MediaPlayer
         */

		Log.d(TAG, "Attach mediaplayer to surface!");
		
        
    }
	
	public void onPrepared(MediaPlayer player) {
		mp.start();
    }

	@Override
	public void onSurfaceChanged(GL10 unused, int width, int height) {
		if(AppConfig.DEBUG_LOGGING) Log.d(TAG, "Starting vertices, texcoords init...");	
		
		FullSquadMesh squad = new FullSquadMesh();
		
		pTexCoordVBG = squad.getTexCoords();
		pVertexVBG = squad.getVertices();
		
		if(AppConfig.DEBUG_LOGGING) Log.d(TAG, "Vertices, texcoords init done...");
		
		if(AppConfig.DEBUG_LOGGING) Log.d(TAG, "Starting camera setup...");
		
//	    setupCamera();
		
	    if(AppConfig.DEBUG_LOGGING) Log.d(TAG, "Camera setup done...");
	}

	/**
	 ******************************
	 * RENDERER CONTROL FUNCTIONS *
	 ******************************
	 */
	
	@Override
	public void onDrawFrame(GL10 unused) {
//		synchronized(this) {
//            if (updateSurface) {
//                st.updateTexImage();
////                st.getTransformMatrix(mSTMatrix);
//                updateSurface = false;
//            }
//        }
		Log.d("MJPEG", "Drawing...");
		
		st.updateTexImage();
		
		GLES20.glViewport(0, 0, this.texW, this.texH);
		
		GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fb[0]);
     	GLES20.glBindRenderbuffer(GLES20.GL_RENDERBUFFER, depthRb[0]);
     	GLES20.glClear( GLES20.GL_DEPTH_BUFFER_BIT | GLES20.GL_COLOR_BUFFER_BIT);
		GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
		
		renderCamera();
	    
	    GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
		GLES20.glBindRenderbuffer(GLES20.GL_RENDERBUFFER, 0);
	}

	private void renderCamera() {
		if(AppConfig.DEBUG_LOGGING) Log.d(TAG, "Starting rendering camera to FrameBuffer...");
		
		// Render camera to texture
		GLES20.glUseProgram(programId[0]);
		
		GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
	    GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, tex[0]);
	    GLES20.glUniform1i(sTextureHandler, 0);
	    
	    GLES20.glVertexAttribPointer(vPositionHandler[0], 2, GLES20.GL_FLOAT, false, 4*2, pVertexVBG);
	    GLES20.glEnableVertexAttribArray(vPositionHandler[0]);
	    GLES20.glVertexAttribPointer(vTexCoordHandler, 2, GLES20.GL_FLOAT, false, 4*2, pTexCoordVBG );
	    GLES20.glEnableVertexAttribArray(vTexCoordHandler);
	    
	    Matrix.setIdentityM(mvp, 0);
	    Matrix.scaleM(mvp, 0, 1, -1, 1);
	    
	    GLES20.glUniformMatrix4fv(mvpHandler[0], 1, false, mvp, 0);
	
	    GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
	    GLES20.glFlush();
	    
	    GLES20.glDisableVertexAttribArray(vPositionHandler[0]);
	    GLES20.glDisableVertexAttribArray(vTexCoordHandler);
	    
	    if(AppConfig.DEBUG_LOGGING) Log.d(TAG, "Rendering camera to FrameBuffer done...");
	}

	/**
	 **************************
	 *     OnPreviewFrame     *
	 **************************
	 */
	
	@Override
	public void onPreviewFrame(byte[] frameData, Camera camera) {
		
	}
	
	/**
	 **************************
	 * SETUP HELPER FUNCTIONS *
	 **************************
	 */

	private void setupCamera() {
		// Setup Camera Parameters
	    Camera.Parameters params = camera.getParameters();
	    params.setPreviewFormat(ImageFormat.NV21);
		params.setPreviewFpsRange(AppConfig.FPS_RANGE[0], AppConfig.FPS_RANGE[1]);
		params.setPreviewSize(AppConfig.PREVIEW_RESOLUTION[0], AppConfig.PREVIEW_RESOLUTION[1]);
		params.set("orientation", "landscape");
//		params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
		camera.setParameters(params);
		camera.startPreview();
		
		// Add callback buffers to camera for frame handling
		float bytesPerPix = ImageFormat.getBitsPerPixel(params.getPreviewFormat()) / 8.0f;
		int frame_byteSize = (int) ((AppConfig.PREVIEW_RESOLUTION[0] * AppConfig.PREVIEW_RESOLUTION[1]) * bytesPerPix);
		
		for(int i = 0; i< AppConfig.AMOUNT_PREVIEW_BUFFERS ; i++) {
			camera.addCallbackBuffer(new byte[frame_byteSize]);
		}
		
		camera.setPreviewCallbackWithBuffer(this);
	}

	private void setupRenderHandlers() {
		if(AppConfig.DEBUG_LOGGING) Log.d(TAG, "Starting renderhandlers init...");
		
		programId = new int[1];
		vPositionHandler = new int[1];
		mvpHandler = new int[1];
		
		programId[0] = RenderingUtils.createProgramFromShaderSrc(vssCamera,fssCamera);
		
		// Camera handlers
		vPositionHandler[0] = GLES20.glGetAttribLocation(programId[0], "vPosition");
	    vTexCoordHandler = GLES20.glGetAttribLocation ( programId[0], "vTexCoord" );
	    sTextureHandler = GLES20.glGetUniformLocation ( programId[0], "sTexture" );
	    mvpHandler[0] = GLES20.glGetUniformLocation(programId[0], "u_MVP");
	    
	    if(AppConfig.DEBUG_LOGGING) Log.d(TAG, "Renderhandlers init done...");
	}

	private void setupCameraTex() {
		if(AppConfig.DEBUG_LOGGING) Log.d(TAG, "Starting camera texture setup...");
		
		tex = new int[2];
		GLES20.glGenTextures(1, tex, 0);
	    GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, tex[0]);
	    GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
	    GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
	    GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
	    GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);
		st = new SurfaceTexture(tex[0]);
		st.setDefaultBufferSize(1920, 1080);
		st.setOnFrameAvailableListener(this);
		
//		startCamera();
		
		if(AppConfig.DEBUG_LOGGING) Log.d(TAG, "Camera texture setup done...");
	}

	private void initFrameBuffer() {
		if(AppConfig.DEBUG_LOGGING) Log.d(TAG, "Starting FrameBuffer init...");
		
		// create the ints for the framebuffer, depth render buffer and texture
        fb = new int[1];
        depthRb = new int[1];
        renderTex = new int[1];
         
        // generate
        GLES20.glGenFramebuffers(1, fb, 0);
        GLES20.glGenRenderbuffers(1, depthRb, 0); // the depth buffer
        GLES20.glGenTextures(1, renderTex, 0);
         
        // generate texture
        GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, renderTex[0]);
         
        // parameters - we have to make sure we clamp the textures to the edges
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S,
                GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T,
                GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER,
                GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER,
                GLES20.GL_LINEAR);
        
        // generate the textures
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, texW, texH, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);
         
        // create render buffer and bind 16-bit depth buffer
        GLES20.glBindRenderbuffer(GLES20.GL_RENDERBUFFER, depthRb[0]);
        GLES20.glRenderbufferStorage(GLES20.GL_RENDERBUFFER, GLES20.GL_DEPTH_COMPONENT16, texW, texH);
        
        // Bind the framebuffer
		GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fb[0]);
		 
		// specify texture as color attachment
		GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D, renderTex[0], 0);
		 
		// attach render buffer as depth buffer
		GLES20.glFramebufferRenderbuffer(GLES20.GL_FRAMEBUFFER, GLES20.GL_DEPTH_ATTACHMENT, GLES20.GL_RENDERBUFFER, depthRb[0]);
		 
		// check status
		int status = GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER);
		
		if (status != GLES20.GL_FRAMEBUFFER_COMPLETE) {
		    Log.e(TAG, "FRAMEBUFFER INCOMPLETE"); 
		    System.exit(1);
		}
		
		// Bind screen as framebuffer
		GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
		GLES20.glBindRenderbuffer(GLES20.GL_RENDERBUFFER, 0);
		
		if(AppConfig.DEBUG_LOGGING) Log.d(TAG, "FrameBuffer init successful...");
	}

	@Override
	public void onFrameAvailable(SurfaceTexture surfaceTexture) {
//		updateSurface = true;
//		st.updateTexImage();
//		Log.d("MJPEG", "NEW FRAME AVAILABLE!");
	}
}
