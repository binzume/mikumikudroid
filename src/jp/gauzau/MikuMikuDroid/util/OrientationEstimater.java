package jp.gauzau.MikuMikuDroid.util;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorManager;

public class OrientationEstimater {

	private float[] orientation = new float[] { 0, 0, -3.14159f / 2 }; // [yaw roll pitch] rad
	private float[] orientationCurrent = new float[3];
	private float yRotationBase = 8; // yaw
	private float xRotationBase = 0; // pitch
	
	
	private boolean swapXY = false;
	
	private float[] acc = new float[3];
	private float[] mag = new float[3];
	private long lastGyroTime = 0;
	private long lastAccelTime = 0;
	private long lastMagneTime = 0;
	private float[] inR = new float[16];

	/**
	 * current orientation array If require matrix of OpenGL, it is necessary to
	 * rotate in the following order: 1. roll 2. pitch 3. yaw
	 * 
	 * @return float array [x,y,z]
	 */
	public float[] getCurrentOrientation() {
		return orientation;
	}

	public void rotateInDisplay(float dx, float dy) {
		float scale = 0.005f;
		double degx = (dx * Math.cos(orientation[1]) + dy * Math.sin(orientation[1])) * scale;
		double degy = (dy * Math.cos(orientation[1]) - dx * Math.sin(orientation[1])) * scale;

		yRotationBase -= -degx;
		orientation[0] += -degx;

		xRotationBase += degy;
		orientation[2] -= degy;
	}
	
	public void rotate(float x, float y) {
		xRotationBase += x;
		orientation[2] -= x;

		yRotationBase += y;
		orientation[0] -= y;
	}
	
	public boolean isReady() {
		return lastAccelTime != 0 && lastMagneTime != 0;
	}

	public void onSensorEvent(SensorEvent event) {

		if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
			System.arraycopy(event.values, 0, acc, 0, 3);
			if (swapXY) {
				acc[0] = -event.values[1];
				acc[1] = event.values[0];
			}
			lastAccelTime = event.timestamp;
		} else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
			//Log.d("Sensor","TYPE_MAGNETIC_FIELD " + event.values[0] + "," + event.values[1] + "," + event.values[2]+ " ("+  event.timestamp);
			if (event.accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE) {
				//return;
			}
			System.arraycopy(event.values, 0, mag, 0, 3);
			if (swapXY) {
				mag[0] = -event.values[1];
				mag[1] = event.values[0];
			}
			lastMagneTime = event.timestamp;
		} else if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
			// event.values : [x,y,z] -> [yaw pitch roll]  landscape mode
			//Log.d("Sensor","TYPE_GYROSCOPE " + event.values[0] + "," + event.values[1] + "," + event.values[2]+ " ("+  event.timestamp);
			if (lastGyroTime > 0) {
				float dt = (event.timestamp - lastGyroTime) * 0.000000001f * 1.1f;
				// yaw rotation
				orientation[0] -= -Math.sin(orientation[2]) * Math.cos(orientation[1]) * event.values[0] * dt; // virtual Y => Y
				//orientation[2] += Math.sin(orientation[1]) * event.values[0] * d; // virtual Y => X
				orientation[2] += (Math.sin(orientation[1]) * event.values[0] - Math.cos(orientation[2]) * Math.abs(event.values[0])) * dt; // virtual Y => X
				orientation[1] += -(Math.cos(orientation[2])) * event.values[0] * dt; // virtual Y => Z
				// pitch rotation
				orientation[2] += Math.cos(orientation[1]) * event.values[1] * dt; // virtual X
				orientation[0] += Math.sin(orientation[1]) * event.values[1] * dt; // virtual X => Y
				// roll rotation
				orientation[1] += event.values[2] * dt; // virtual Z roll
			}
			lastGyroTime = event.timestamp;
		}

		if (lastAccelTime == 0 || lastMagneTime == 0)
			return; // wait for initialize

		SensorManager.getRotationMatrix(inR, null, acc, mag);
		SensorManager.getOrientation(inR, orientationCurrent);
		if (yRotationBase > 7) {
			yRotationBase = orientationCurrent[0];
			orientation[0] = yRotationBase;
		}
		orientationCurrent[0] -= yRotationBase;
		orientationCurrent[2] -= xRotationBase;
		for (int i = 0; i < 3; i++) {
			if (orientationCurrent[i] > orientation[i] + Math.PI) {
				orientation[i] += Math.PI * 2;
			}
			if (orientationCurrent[i] < orientation[i] - Math.PI) {
				orientation[i] -= Math.PI * 2;
			}

			orientation[i] = (orientation[i] * 199 + orientationCurrent[i]) / 200;
		}

	}

}
