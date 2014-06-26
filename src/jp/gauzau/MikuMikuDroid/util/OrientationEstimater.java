package jp.gauzau.MikuMikuDroid.util;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorManager;
import android.opengl.Matrix;
import android.util.Log;

public class OrientationEstimater {

	private final static float G = SensorManager.GRAVITY_EARTH * 1.023f;
	private final static float PI = (float) Math.PI;

	private final float[] outputRotationMatrix = new float[16];
	public float[] rotationMatrix = new float[16];
	public final float[] rotationMatrix_t1 = new float[16];
	public float[] rotationMatrix_t2 = new float[16];

	public float[] rotationMatrix_d = new float[16];

	private boolean landscape = true; // swapXY

	private float[] acc = new float[3];
	private float[] mag = new float[3];
	private long lastGyroTime = 0;
	private long lastAccelTime = 0;
	private long lastMagneTime = 0;
	private long resetTime = 0;

	private final float[] groundI = new float[] { 0, 1, 0, 1 };
	private final Vector3f groundVec = new Vector3f();
	private final Vector3f accVec = new Vector3f();
	private final Vector3f accVecN = new Vector3f();
	private final Vector3f vVec = new Vector3f();
	private final Vector3f posVec = new Vector3f();
	private final Vector3f gyroVec = new Vector3f();

	//private float[] outputPosition = new float[3];
	private float[] orientation = new float[3]; // [yaw roll pitch] (rad)
	private float[] orientationCurrent = new float[3];
	private float[] position = new float[3]; // beta

	private final float[] accHistory = new float[8];
	private int accHistoryCount = 0;

	private int eventCount = 0;

	public OrientationEstimater() {
		reset();
	}

	public void reset() {
		Log.d("OrientationEstimater", "reset");
		resetTime = System.currentTimeMillis();
		Matrix.setIdentityM(rotationMatrix, 0);
		Matrix.setIdentityM(rotationMatrix_d, 0);
		Matrix.setIdentityM(outputRotationMatrix, 0);
		position[0] = 0;
		position[1] = 0;
		position[2] = 0;
		posVec.set(0, 0, 0);
	}

	/**
	 * current orientation array If require matrix of OpenGL, it is necessary to
	 * rotate in the following order: 1. roll 2. pitch 3. yaw
	 * 
	 * @return float array [x,y,z]
	 */
	public float[] getCurrentOrientation() {
		SensorManager.getOrientation(rotationMatrix, orientationCurrent);
		// swap x,y
		orientation[0] = -orientationCurrent[1];
		orientation[1] = orientationCurrent[0];
		orientation[2] = -orientationCurrent[2];
		return orientation;
	}

	/**
	 * Current rotation matrix.
	 * 
	 * @return
	 */
	public float[] getRotationMatrix() {
		Matrix.multiplyMM(outputRotationMatrix, 0, rotationMatrix, 0, rotationMatrix_d, 0);
		return outputRotationMatrix;
	}

	/**
	 * BUGGY!
	 * 
	 * @return float array [x,y,z] unit:mm
	 */
	public float[] getPosition() {
		//outputPosition[0] = posVec.values[0] + position[0];
		//outputPosition[1] = posVec.values[1] + position[1];
		//outputPosition[2] = posVec.values[2] + position[2];
		return position;
	}

	public void rotateInDisplay(float dx, float dy) {

		float l = (float) Math.sqrt(dx * dx + dy * dy) * 0.002f;
		if (l > 0.001f) {
			//  OutRot = a * Rot * D = Rot * b * D
			//  b * D = Rot^-1 * a * OutRot
			Matrix.invertM(rotationMatrix_t1, 0, rotationMatrix, 0);
			Matrix.rotateM(rotationMatrix_t1, 0, l * 180 / PI, dy, dx, 0);
			Matrix.multiplyMM(rotationMatrix_t2, 0, rotationMatrix_t1, 0, outputRotationMatrix, 0);

			// swap(d,t2)
			float tm[] = rotationMatrix_t2;
			rotationMatrix_t2 = rotationMatrix_d;
			rotationMatrix_d = tm;
		}
	}

	public void translateInDisplay(float[] pos, float dx, float dy, float dz) {
		float scale = 0.1f;

		float l = (float) Math.sqrt(dx * dx + dy * dy + dz * dz) * scale;
		if (l > 0.001f) {
			float[] a = new float[] { -dx * scale, dy * scale, -dz * scale, 1 };
			float[] b = new float[4];
			Matrix.invertM(rotationMatrix_t1, 0, outputRotationMatrix, 0);
			Matrix.multiplyMV(b, 0, rotationMatrix_t1, 0, a, 0);

			pos[0] += b[0];
			pos[1] += b[1];
			pos[2] += b[2];
		}
	}

	public void rotate(float x, float y) {
		Matrix.rotateM(rotationMatrix_d, 0, x, 1, 0, 0);
		Matrix.rotateM(rotationMatrix_d, 0, y, 0, 1, 0);
	}

	public boolean isReady() {
		return lastAccelTime != 0 && lastMagneTime != 0;
	}

	public void onSensorEvent(SensorEvent event) {

		if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
			System.arraycopy(event.values, 0, acc, 0, 3);

			if (landscape) {
				accVecN.set(-acc[1], acc[0], -acc[2]);
			} else {
				accVecN.set(-acc[0], -acc[1], -acc[2]);
			}

			float dt = (event.timestamp - lastAccelTime) * 0.000000001f; // dt(sec)
			Matrix.multiplyMV(groundVec.values, 0, rotationMatrix, 0, groundI, 0); // rotMatrix * groundA
			if (lastAccelTime > 0 && dt < 0.5f) {
				accVec.set(accVecN.values);

				// m/s^2
				accVec.values[0] -= groundVec.values[0] * G;
				accVec.values[1] -= groundVec.values[1] * G;
				accVec.values[2] -= groundVec.values[2] * G;

				// mm/s
				vVec.values[0] += accVec.values[0] * dt * 1000;
				vVec.values[1] += accVec.values[1] * dt * 1000;
				vVec.values[2] += accVec.values[2] * dt * 1000;

				// mm
				posVec.values[0] += vVec.values[0] * dt;
				posVec.values[1] += vVec.values[1] * dt;
				posVec.values[2] += vVec.values[2] * dt;

				// ...
				vVec.scale(0.99f);
				if (vVec.length() < 20 || vVec.length() > 1000) {
					vVec.scale(0.95f);
				}

				accHistory[(accHistoryCount++) % accHistory.length] = accVec.length();
				if (accHistoryCount > accHistory.length) {
					float sum = 0;
					for (int i = 0; i < accHistory.length; i++) {
						sum += accHistory[i];
					}
					if (sum < 1.0f && vVec.length() > 10f && accVec.length() < 0.5f) {
						vVec.scale(0.8f);
						//posVec.scale(0.995f);
					}
					if (sum < 2.0f && accVec.length() < 0.5f) {
						vVec.scale(0.95f);
					}
				}

				float l = posVec.length();
				for (int i = 0; i < 3; i++) {
					if (posVec.values[i] > 10) {
						if (vVec.values[i] < -200 || accVec.values[i] < -1f)
							posVec.values[i] -= 2f;
					} else if (posVec.values[i] < -10) {
						if (vVec.values[i] > 200 || accVec.values[i] > 1f)
							posVec.values[i] += 2f;
					}
				}
				if (l > 1000.0f) {
					posVec.scale(0.98f);
				}
				if (l < 10) {
					posVec.scale(0.8f);
				}
				//posVec.values[2] *= 0.95f;
				if (posVec.values[0] > 150) {
					posVec.values[0] -= 1f;
				} else if (posVec.values[0] < -1500) {
					posVec.values[0] += 1f;
				}

				if (posVec.values[2] < -200) {
					posVec.values[2] += 1f;
				} else if (posVec.values[2] > 500) {
					posVec.values[2] -= 1f;
				}

				eventCount++;
				if (eventCount % 20 == 0) {
					Log.d("OrientationEstimater", "P=" + posVec + " V=" + vVec + " A=" + accVec + ":" + accVec.length() + " (G:" + groundVec);
				}

			} else {
				//vVec.scale(0.5f);
				// vVec.set(0, 0, 0);
			}

			accVec.set(accVecN.values);
			accVec.normalize();
			lastAccelTime = event.timestamp;
		} else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
			//Log.d("Sensor","TYPE_MAGNETIC_FIELD " + event.values[0] + "," + event.values[1] + "," + event.values[2]+ " ("+  event.timestamp);
			if (event.accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE) {
				//return;
			}
			System.arraycopy(event.values, 0, mag, 0, 3);
			if (landscape) {
				mag[0] = -event.values[1];
				mag[1] = event.values[0];
			}
			lastMagneTime = event.timestamp;
		} else if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
			// event.values : [x,y,z] -> [yaw pitch roll]  landscape mode
			//Log.d("Sensor","TYPE_GYROSCOPE " + event.values[0] + "," + event.values[1] + "," + event.values[2]+ " ("+  event.timestamp);
			if (Math.abs(event.values[0]) > 3.0f) {
				posVec.values[1] *= 0.95f;
			}
			if (lastGyroTime > 0) {
				float dt = (event.timestamp - lastGyroTime) * 0.000000001f;
				if (landscape) {
					gyroVec.set(-event.values[1], event.values[0], -event.values[2]);
				} else {
					gyroVec.set(event.values[0], event.values[1], event.values[2]);
				}
				// Matrix.rotateM(rotationMatrix, 0, gyroVec.length() * dt * 180 / PI,  gyroVec.array()[0],  gyroVec.array()[1],  gyroVec.array()[2]);
				Matrix.setRotateM(rotationMatrix_t1, 0, gyroVec.length() * dt * 180 / PI, gyroVec.array()[0], gyroVec.array()[1], gyroVec.array()[2]);
				Matrix.multiplyMM(rotationMatrix_t2, 0, rotationMatrix_t1, 0, rotationMatrix, 0);
				float tm[] = rotationMatrix_t2;
				rotationMatrix_t2 = rotationMatrix;
				rotationMatrix = tm;
			}
			lastGyroTime = event.timestamp;
		}

		if (lastAccelTime == 0 || lastMagneTime == 0)
			return; // wait for initialize

		// probably stopping. adjust ground vector.
		if (gyroVec.length() < 0.2f && Math.abs(accVecN.length() - G) < 0.5f) {
			// estimated ground vec.
			Matrix.multiplyMV(groundVec.array(), 0, rotationMatrix, 0, groundI, 0); // rotMatrix * groundA
			float theta = (float) Math.acos(groundVec.dot(accVec));
			if (theta > 0) {
				float[] cross = groundVec.cross(accVec).normalize().array();
				float factor = (System.currentTimeMillis() - resetTime < 300) ? 0.8f : 0.001f;
				Matrix.setRotateM(rotationMatrix_t1, 0, theta * 180 / PI * factor, cross[0], cross[1], cross[2]);
				Matrix.multiplyMM(rotationMatrix_t2, 0, rotationMatrix_t1, 0, rotationMatrix, 0);
				float tm[] = rotationMatrix_t2;
				rotationMatrix_t2 = rotationMatrix;
				rotationMatrix = tm;
			}
		}

	}

}
