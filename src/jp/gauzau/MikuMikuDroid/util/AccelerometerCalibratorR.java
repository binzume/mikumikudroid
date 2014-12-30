package jp.gauzau.MikuMikuDroid.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.os.Environment;

public class AccelerometerCalibratorR {

	private final Vector3f offset = new Vector3f();
	private final Vector3f scale = new Vector3f(1, 1, 1);

	public void load() {
		try {
			load(Environment.getExternalStorageDirectory() + "/sensor_calib.txt");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void load(String configPath) throws IOException {
		File f = new File(configPath);
		BufferedReader reader = new BufferedReader(new FileReader(f));
		String line;
		while ((line = reader.readLine()) != null) {
			String kv[] = line.split("=", 2);
			if (kv.length < 2)
				continue;
			if (kv[0].equals("accel_offset")) {
				offset.set(parseVec(kv[1]).array());
			}
			if (kv[0].equals("accel_scale")) {
				scale.set(parseVec(kv[1]).array());
			}
		}
		reader.close();
	}

	public void save(String configPath) throws IOException {
		File f = new File(configPath);
		BufferedWriter writer = new BufferedWriter(new FileWriter(f));
		writer.write("accel_offset=" + offset + "\n");
		writer.write("accel_scale=" + scale + "\n");
		writer.close();
	}

	private Vector3f parseVec(String v) {
		String a[] = v.replaceAll("[\\(\\)\\s]+", "").split(",");
		if (a.length == 3) {
			return new Vector3f(Float.parseFloat(a[0]), Float.parseFloat(a[1]), Float.parseFloat(a[2]));
		}
		return null;
	}

	public void onSensorEvent(SensorEvent event) {
		if (event.sensor.getType() != Sensor.TYPE_ACCELEROMETER) {
			return;
		}

		event.values[0] = (event.values[0] + offset.values[0]) * scale.values[0];
		event.values[1] = (event.values[1] + offset.values[1]) * scale.values[1];
		event.values[2] = (event.values[2] + offset.values[2]) * scale.values[2];
	}
}
