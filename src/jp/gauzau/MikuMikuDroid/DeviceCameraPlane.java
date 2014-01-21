package jp.gauzau.MikuMikuDroid;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.List;

import jp.gauzau.MikuMikuDroid.MikuRendererGLES20.GLSL;
import android.annotation.TargetApi;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.opengl.GLES20;
import android.os.Build;
import android.util.Log;

@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
public class DeviceCameraPlane {
	private FloatBuffer vertex;
	public Camera camera;
	private SurfaceTexture cameraTexture;
	private boolean cameraTextureUpdated;
	public int cameraTextureName = -1;
	
	public DeviceCameraPlane() {
		try {
			camera = Camera.open();
			cameraTextureUpdated = false;
			cameraTexture = createSurfaceTexture();
			camera.setPreviewTexture(cameraTexture);
			// camera.setPreviewDisplay(null);
		} catch (RuntimeException e) {
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		ByteBuffer bb = ByteBuffer.allocateDirect(4 * 4 * 4);
		bb.order(ByteOrder.nativeOrder());
		vertex = bb.asFloatBuffer();
		
		vertex.position(0);

		float bottom = 0.0f;
		float left = -0.3f;
		float sz = 0.2f;
		
		vertex.put(left);
		vertex.put(bottom);
		vertex.put(0);
		vertex.put(1);

		vertex.put(left);
		vertex.put(bottom + sz);
		vertex.put(0);
		vertex.put(0);

		vertex.put(left + sz);
		vertex.put(bottom + sz);
		vertex.put(1);
		vertex.put(0);

		vertex.put(left + sz);
		vertex.put(bottom);
		vertex.put(1);
		vertex.put(1);

		vertex.position(0);

	}
	
	
	public void setPreviewSize(Camera.Parameters params, int width, int height) {
		List<Camera.Size> supported = params.getSupportedPreviewSizes();
		if (supported != null) {
			for (Camera.Size size : supported) {
				if (size.width <= width && size.height <= height) {
					params.setPreviewSize(size.width, size.height);
					Log.d("CameraView","PreviewSIze: "+size.width + "x" + size.height);
					camera.setParameters(params);
					break;
				}
			}
		}
	}
	
	private static final int GL_TEXTURE_EXTERNAL_OES = 0x8D65;
	private SurfaceTexture createSurfaceTexture() {
		int tex[] = new int[1];
		GLES20.glGenTextures(1, tex, 0);
		GLES20.glBindTexture(GL_TEXTURE_EXTERNAL_OES, tex[0]);
		GLES20.glTexParameteri(GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
		GLES20.glTexParameteri(GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
		GLES20.glTexParameteri(GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
		GLES20.glTexParameteri(GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
		cameraTextureName = tex[0];
		final SurfaceTexture t = new SurfaceTexture(tex[0]);
		t.setOnFrameAvailableListener(new SurfaceTexture.OnFrameAvailableListener() {
			
			@Override
			public void onFrameAvailable(SurfaceTexture surfaceTexture) {
				cameraTextureUpdated = true;
			}
		});
		return t;
	}
	
	public void update() {
		if (cameraTextureUpdated) {
			cameraTextureUpdated = false;
			cameraTexture.updateTexImage();
			// cameraTexture.getTransformMatrix(mtx);
		}
	}
	
	
	public void bindBuffer(GLSL glsl) {
		vertex.position(0);

		GLES20.glEnableVertexAttribArray(glsl.maPositionHandle);
		vertex.position(0);
		GLES20.glVertexAttribPointer(glsl.maPositionHandle, 4, GLES20.GL_FLOAT, false, 0, vertex);
//		checkGlError("drawGLES20 VertexAttribPointer bg vertex");
		
		vertex.position(0);

		GLES20.glBindTexture(GL_TEXTURE_EXTERNAL_OES, cameraTextureName);
	
	}
	
	public void release() {
		if (camera != null) {
			camera.setPreviewCallback(null);
			camera.stopPreview();
			camera.release();
			camera = null;
		}
		if (cameraTexture != null) {
			cameraTexture.release();
			cameraTexture = null;
		}
	}

}
