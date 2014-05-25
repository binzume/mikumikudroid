package jp.gauzau.MikuMikuDroid.util;

public class MatrixUtil {
	
	/**
	 * 
	 * @param mat 4x4 OpenGL matrix
	 */
	public static String stringfy(float[] mat) {
		String s = "";
		for (int i=0;i<4;i++) {
			s += "| " + mat[i*4 + 0] + "," + mat[i*4 + 1] + "," + mat[i*4 + 2] + "," + mat[i*4 + 3] + " ";
		}
		return s + "|";
	}

}
