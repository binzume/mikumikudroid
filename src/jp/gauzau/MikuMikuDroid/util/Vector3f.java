package jp.gauzau.MikuMikuDroid.util;

public class Vector3f {
	public final float values[] = new float[4];

	public Vector3f() {
		values[3] = 1;
	}

	public Vector3f(float x, float y, float z) {
		values[0] = x;
		values[1] = y;
		values[2] = z;
		values[3] = 1;
	}

	public Vector3f(float a[]) {
		values[0] = a[0];
		values[1] = a[1];
		values[2] = a[2];
		values[3] = 1;
	}

	public Vector3f(Vector3f v) {
		float a[] = v.array();
		values[0] = a[0];
		values[1] = a[1];
		values[2] = a[2];
		values[3] = 1;
	}

	public float[] array() {
		return values;
	}

	public void set(float[] a) {
		values[0] = a[0];
		values[1] = a[1];
		values[2] = a[2];
		values[3] = 1;
	}

	public void set(float x, float y, float z) {
		values[0] = x;
		values[1] = y;
		values[2] = z;
		values[3] = 1;
	}

	public float length() {
		return (float) Math.sqrt(values[0] * values[0] + values[1] * values[1] + values[2] * values[2]);
	}

	public float length2() {
		return values[0] * values[0] + values[1] * values[1] + values[2] * values[2];
	}

	public float dot(Vector3f v) {
		return values[0] * v.values[0] + values[1] * v.values[1] + values[2] * v.values[2];
	}

	public Vector3f cross(Vector3f v, Vector3f dst) {
		dst.values[0] = values[1] * v.values[2] - v.values[1] * values[2];
		dst.values[1] = values[2] * v.values[0] - v.values[2] * values[0];
		dst.values[2] = values[0] * v.values[1] - v.values[0] * values[1];
		return dst;
	}

	public Vector3f normalize() {
		float l = length();
		if (l != 0) {
			values[0] /= l;
			values[1] /= l;
			values[2] /= l;
		} else {
			values[0] = 1;
			values[1] = 0;
			values[2] = 0;
		}
		return this;
	}

	public Vector3f cross(Vector3f v) {
		return cross(v, new Vector3f());
	}

	public void scale(float s) {
		values[0] *= s;
		values[1] *= s;
		values[2] *= s;
	}

	public String toString() {
		return "( " + values[0] + ", " + values[1] + ", " + values[2] + " )";
	}

	public static Vector3f add(Vector3f result, Vector3f a, Vector3f b) {
		result.values[0] = a.values[0] + b.values[0];
		result.values[1] = a.values[1] + b.values[1];
		result.values[2] = a.values[2] + b.values[2];
		return result;
	}

	public Vector3f add(Vector3f b) {
		return add(this, this, b);
	}

	public static Vector3f sub(Vector3f result, Vector3f a, Vector3f b) {
		result.values[0] = a.values[0] - b.values[0];
		result.values[1] = a.values[1] - b.values[1];
		result.values[2] = a.values[2] - b.values[2];
		return result;
	}

	public Vector3f sub(Vector3f b) {
		return sub(this, this, b);
	}
}
