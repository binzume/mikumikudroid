package jp.gauzau.MikuMikuDroid.util;

import android.hardware.Sensor;
import android.hardware.SensorEvent;

public class MotionDetector {
	private float[] gravHistory = new float[10]; // Gravity (G)
	private int gravHistoryPos = 0;
	float[] mAxV = new float[3];

	public boolean jump = false;

	public void onSensorEvent(SensorEvent event) {
		if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
			System.arraycopy(event.values, 0, mAxV, 0, 3);
			gravHistoryPos = (gravHistoryPos + 1) % gravHistory.length;
			gravHistory[gravHistoryPos] = (float) Math.sqrt(mAxV[0] * mAxV[0] + mAxV[1] * mAxV[1] + mAxV[2] * mAxV[2]) / 9.8f;
		}

		jump = false;
		if (gravHistory[gravHistoryPos] < 0.4) { // 0.3G
			if (gravHistory[(gravHistoryPos + 5) % gravHistory.length] > 1.5) {
				// jump!
				jump = true;
			}
			return;
		}

	}

}
