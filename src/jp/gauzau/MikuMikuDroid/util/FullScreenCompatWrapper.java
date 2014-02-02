package jp.gauzau.MikuMikuDroid.util;

import android.annotation.TargetApi;
import android.os.Build;
import android.view.View;

public class FullScreenCompatWrapper {
	
	@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
	private static class ICS {
		public static void fullScreen(View v, boolean enable) {
			if (enable) {
				v.setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
			} else {
				v.setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
			}
		}
	}
	
	@TargetApi(Build.VERSION_CODES.KITKAT)
	private static class KitKat {
		public static void fullScreen(View v, boolean enable) {
			if (enable) {
				v.setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | 0x00001000); // .SYSTEM_UI_FLAG_IMMERSIVE_STICKY
			} else {
				v.setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
			}
		}
	}
	
	public static void fullScreen(View v, boolean enable) {
		if (Build.VERSION.SDK_INT >= 19) { // KitKat
			KitKat.fullScreen(v, enable);
		} else if (Build.VERSION.SDK_INT >= 14) {
			ICS.fullScreen(v, enable);
		} else {
			
		}
	}
}
