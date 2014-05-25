package jp.gauzau.MikuMikuDroid;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import jp.gauzau.MikuMikuDroid.util.FullScreenCompatWrapper;
import jp.gauzau.MikuMikuDroid.util.OrientationEstimater;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Toast;

@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
public class MikuMikuDroid extends Activity implements SensorEventListener {
	// View
	private MMGLSurfaceView mMMGLSurfaceView;
	private RelativeLayout mRelativeLayout;
	private SeekBar mSeekBar;
	private Button mPlayPauseButton;
	private Button mRewindButton;
	private ScaleGestureDetector mScaleGestureDetector;
	private OrientationEstimater orientationEstimater = new OrientationEstimater();
	
	// Model
	private CoreLogic mCoreLogic;
	
	// Sensor
	SensorManager	mSM = null;
	Sensor			mAx = null;
	Sensor			mMg = null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		mSM = (SensorManager)getSystemService(SENSOR_SERVICE);
		mAx = mSM.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		mMg = mSM.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

		mCoreLogic = new CoreLogic(this) {
			@Override
			public void onInitialize() {
				MikuMikuDroid.this.runOnUiThread(new Runnable() {
					@Override
					public void run() {
						AsyncExec<CoreLogic> ae = new AsyncExec<CoreLogic>(MikuMikuDroid.this) {
							@Override
							protected boolean exec(CoreLogic target) {
								try {
									mCoreLogic.restoreState();
									final int max = target.getDulation();
									mSeekBar.post(new Runnable() {
										@Override
										public void run() {
											mSeekBar.setMax(max);
										}
									});
								} catch (OutOfMemoryError e) {
									return false;
								}

								return true;
							}
							
							@Override
							public void post() {
								if(mFail.size() != 0) {
									Toast.makeText(MikuMikuDroid.this, "Out of Memory. Abort.", Toast.LENGTH_LONG).show();										
								}
								
							}
						};
						ae.setMax(1);
						ae.setMessage("Restoring Previous state...");
						ae.execute(mCoreLogic);
					}
				});
			}
			
			@Override
			public void onDraw(final int pos) {
				MikuMikuDroid.this.mSeekBar.post(new Runnable() {
					@Override
					public void run() {
						MikuMikuDroid.this.mSeekBar.setProgress(pos);
					}
				});
			}
		};
		mCoreLogic.setScreenAngle(0);
		if (mCoreLogic.getCameraMode() == CoreLogic.CAMERA_MODE_SENSOR2) {
			cameraPos[2] = 0;
		}
		mCoreLogic.setCameraPosition(cameraPos);

		mRelativeLayout = new RelativeLayout(this);
		mRelativeLayout.setVerticalGravity(Gravity.BOTTOM);
		mMMGLSurfaceView = new MMGLSurfaceView(this, mCoreLogic);

	
		LayoutParams p = new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
		p.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
		mSeekBar = new SeekBar(this);
		mSeekBar.setLayoutParams(p);
		mSeekBar.setId(1024);
		mSeekBar.setVisibility(SeekBar.INVISIBLE);
		mSeekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
			private boolean mIsPlaying = false;

			@Override
			public void onProgressChanged(SeekBar seekBar, final int progress, boolean fromUser) {
				if(fromUser) {
					mCoreLogic.seekTo(progress);
				}
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
				if(mCoreLogic.isPlaying()) {
					mCoreLogic.pause();
					mIsPlaying = true;
				}
			}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				if(mIsPlaying) {
					mCoreLogic.toggleStartStop();
					mIsPlaying = false;
				}
			}
			
		});
		
		p = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		p.addRule(RelativeLayout.CENTER_HORIZONTAL);
		p.addRule(RelativeLayout.ABOVE, mSeekBar.getId());
		p.setMargins(5, 5, 5, 60);
		mPlayPauseButton = new Button(this);
		mPlayPauseButton.setLayoutParams(p);
		mPlayPauseButton.setVisibility(Button.INVISIBLE);
		mPlayPauseButton.setBackgroundResource(R.drawable.ic_media_play);
		mPlayPauseButton.setId(mSeekBar.getId() + 1);
		mPlayPauseButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if(mCoreLogic.toggleStartStop()) {
					mPlayPauseButton.setBackgroundResource(R.drawable.ic_media_pause);
				} else {
					mPlayPauseButton.setBackgroundResource(R.drawable.ic_media_play);					
				}
			}
		});

		p = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		p.addRule(RelativeLayout.ABOVE, mSeekBar.getId());
		p.addRule(RelativeLayout.LEFT_OF, mPlayPauseButton.getId());
		p.setMargins(5, 5, 60, 60);
		mRewindButton = new Button(this);
		mRewindButton.setLayoutParams(p);
		mRewindButton.setVisibility(Button.INVISIBLE);
		mRewindButton.setBackgroundResource(R.drawable.ic_media_previous);
		mRewindButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				mCoreLogic.rewind();
			}
		});

		
		mRelativeLayout.addView(mMMGLSurfaceView);
		mRelativeLayout.addView(mSeekBar);
		mRelativeLayout.addView(mPlayPauseButton);
		mRelativeLayout.addView(mRewindButton);
		setContentView(mRelativeLayout);

		if (mCoreLogic.checkFileIsPrepared() == false) {
			/*
			AlertDialog.Builder ad;
			ad = new AlertDialog.Builder(this);
			ad.setTitle(R.string.setup_alert_title);
			ad.setMessage(R.string.setup_alert_text);
			ad.setPositiveButton(R.string.select_ok, null);
			ad.show();
			*/
		}
		
		mScaleGestureDetector = new ScaleGestureDetector(this,
				new ScaleGestureDetector.SimpleOnScaleGestureListener() {
					@Override
					public boolean onScaleBegin(ScaleGestureDetector detector) {
						Log.d("", "onScaleBegin : " + detector.getScaleFactor());
						return super.onScaleBegin(detector);
					}

					@Override
					public void onScaleEnd(ScaleGestureDetector detector) {
						Log.d("", "onScaleEnd : " + detector.getScaleFactor());
						super.onScaleEnd(detector);
					}

					@Override
					public boolean onScale(ScaleGestureDetector detector) {
						float d = detector.getCurrentSpan() - detector.getPreviousSpan();
						float cameraPosition[] = mCoreLogic.getCameraPositionAsRef();
						float mCameraOrientation[] = mCoreLogic.getCameraOrientationAsRef();
						
						if (mCoreLogic.getCameraMode() == CoreLogic.CAMERA_MODE_SENSOR2) {
							if (d < 0 || mCoreLogic.cameraDistance > 0) {
								mCoreLogic.cameraDistance -= d * 0.1f;
							}
						} else {
							cameraPosition[0] += Math.sin(mCameraOrientation[0]) * d * 0.1f;
							cameraPosition[2] += Math.cos(mCameraOrientation[0]) * d * 0.1f;
						}
						return true;
					};
				});
		
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		FullScreenCompatWrapper.fullScreen(mRelativeLayout, true);
	}

	@Override
	protected void onResume() {
		super.onResume();
		this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
		mRelativeLayout.setKeepScreenOn(true);
		mMMGLSurfaceView.onResume();
		if(mAx != null && mMg != null) {
			mSM.registerListener(this, mAx, SensorManager.SENSOR_DELAY_GAME);
			mSM.registerListener(this, mMg, SensorManager.SENSOR_DELAY_GAME);
			Sensor gs = mSM.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
			if (gs != null) {
				mSM.registerListener(this, gs, SensorManager.SENSOR_DELAY_FASTEST);
			}
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		mCoreLogic.pause();
		mMMGLSurfaceView.onPause();
		if(mAx != null || mMg != null) {
			mSM.unregisterListener(this);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		boolean ret = super.onCreateOptionsMenu(menu);

		menu.add(0, Menu.FIRST,     Menu.NONE, R.string.menu_load_model);
		menu.add(0, Menu.FIRST + 1, Menu.NONE, R.string.menu_load_camera);
		menu.add(0, Menu.FIRST + 2, Menu.NONE, R.string.menu_load_music);
		menu.add(0, Menu.FIRST + 3, Menu.NONE, R.string.menu_toggle_oculus);
		menu.add(0, Menu.FIRST + 4, Menu.NONE, R.string.menu_toggle_physics);
		menu.add(0, Menu.FIRST + 5, Menu.NONE, R.string.menu_initialize);

		return ret;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {

		case (Menu.FIRST + 0):
			final File[] sc0 = mCoreLogic.getModelSelector();
			openSelectDialog(sc0, R.string.menu_load_model, R.string.setup_alert_pmd, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					final String model = sc0[which].getPath();
					
					// read as background if not .pmd
					if(!model.endsWith(".pmd") && !model.endsWith(".pmx")) {
						if(model.endsWith(".x")) { // accessory
							AsyncExec<CoreLogic> ae = new AsyncExec<CoreLogic>(MikuMikuDroid.this) {
								@Override
								protected boolean exec(CoreLogic target) {
									try {
										mCoreLogic.loadAccessory(model);
										mCoreLogic.storeState();
									} catch (OutOfMemoryError e) {
										return false;
									} catch (IOException e) {
										// TODO Auto-generated catch block
										e.printStackTrace();
									}
									return true;
								}
								
								@Override
								public void post() {
									if(mFail.size() != 0) {
										Toast.makeText(MikuMikuDroid.this, "Out of Memory. Abort.", Toast.LENGTH_LONG).show();										
									}
								}
							};
							ae.setMax(1);
							ae.setMessage("Loading Model/Motion...");
							ae.execute(mCoreLogic);
						} else {
							mMMGLSurfaceView.deleteTexture(mCoreLogic.loadBG(model));							
						}
						return ;
					}
					
					final File[] sc = mCoreLogic.getMotionSelector();
					openMotionSelectDialog(sc, R.string.menu_load_motion, R.string.setup_alert_vmd, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, final int which) {
							final String motion = which == 0 ? null : sc[which-1].getPath();
							AsyncExec<CoreLogic> ae = new AsyncExec<CoreLogic>(MikuMikuDroid.this) {
								@Override
								protected boolean exec(CoreLogic target) {
									try {
										if(which == 0) {
											MikuModel m = target.loadStage(model);
											if(m != null) {
												ArrayList<MikuModel> mm = new ArrayList<MikuModel>(1);
												mm.add(m);
												mMMGLSurfaceView.deleteTextures(mm);
											}
										} else {
											target.loadModelMotion(model, motion);
											final int max = target.getDulation();
											mSeekBar.post(new Runnable() {
												@Override
												public void run() {
													mSeekBar.setMax(max);
												}
											});
										}
										mCoreLogic.storeState();
									} catch (OutOfMemoryError e) {
										return false;
									} catch (IOException e) {
										// TODO Auto-generated catch block
										e.printStackTrace();
									}
									return true;
								}
								
								@Override
								public void post() {
									if(mFail.size() != 0) {
										Toast.makeText(MikuMikuDroid.this, "Out of Memory. Abort.", Toast.LENGTH_LONG).show();										
									}
								}
							};
							ae.setMax(1);
							ae.setMessage("Loading Model/Motion...");
							ae.execute(mCoreLogic);
						}
					});
				}
			});
			break;

		case (Menu.FIRST + 1):
			final File[] sc1 = mCoreLogic.getCameraSelector();
			openSelectDialog(sc1, R.string.menu_load_camera, R.string.setup_alert_vmd, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					final String camera = sc1[which].getPath();
					new AsyncTask <Void, Void, Void>() {
						@Override
						protected Void doInBackground(Void... params) {
							try {
								mCoreLogic.loadCamera(camera);
								mCoreLogic.storeState();
								final int max = mCoreLogic.getDulation();
								mSeekBar.post(new Runnable() {
									@Override
									public void run() {
										mSeekBar.setMax(max);
									}
								});
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							return null;
						}
					}.execute();
				}
			});
			break;

		case (Menu.FIRST + 2):
			final File[] sc2 = mCoreLogic.getMediaSelector();
			openSelectDialog(sc2, R.string.menu_load_music, R.string.setup_alert_music, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					final String media = "file://" + sc2[which].getPath();
					new AsyncTask <Void, Void, Void>() {
						@Override
						protected Void doInBackground(Void... params) {
							mCoreLogic.loadMedia(media);
							mCoreLogic.storeState();
							final int max = mCoreLogic.getDulation();
							mSeekBar.post(new Runnable() {
								@Override
								public void run() {
									mSeekBar.setMax(max);
								}
							});
							return null;
						}
					}.execute();
				}
			});
			break;
			
		case (Menu.FIRST + 3):
			// mCoreLogic.toggleStartStop();
			mCoreLogic.enableOculusMode = !mCoreLogic.enableOculusMode;
			break;
			
		case (Menu.FIRST + 4):
			mCoreLogic.togglePhysics();
			break;

		case (Menu.FIRST + 5):
			mMMGLSurfaceView.deleteTextures(mCoreLogic.clear());
			break;

		default:
			;
		}

		return super.onOptionsItemSelected(item);
	}
	
	private void openSelectDialog(File[] item, int title, int alert, DialogInterface.OnClickListener task) {
		Builder ad = new AlertDialog.Builder(this);
		if (item == null) {
			ad.setTitle(R.string.setup_alert_title);
			ad.setMessage(alert);
			ad.setPositiveButton(R.string.select_ok, null);
		} else {
			ad.setTitle(title);
			String[] is = new String[item.length];
			for(int i = 0; i < item.length; i++) {
				is[i] = item[i].getName();
				int idx = is[i].lastIndexOf(".");
				is[i] = is[i].substring(0, idx);
			}
			ad.setItems(is, task);
		}
		ad.show();
	}

	private void openMotionSelectDialog(File[] item, int title, int alert, DialogInterface.OnClickListener task) {
		Builder ad = new AlertDialog.Builder(this);
		if (item == null) {
			ad.setTitle(R.string.setup_alert_title);
			ad.setMessage(alert);
			ad.setPositiveButton(R.string.select_ok, null);
		} else {
			ad.setTitle(title);
			String[] is = new String[item.length+1];
			is[0] = "Load as Background";
			for(int i = 1; i < is.length; i++) {
				is[i] = item[i-1].getName();
				int idx = is[i].lastIndexOf(".");
				is[i] = is[i].substring(0, idx);
			}
			ad.setItems(is, task);
		}
		ad.show();
	}

	float touchX = 0f;
	float touchY = 0f;
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if(event.getAction() == MotionEvent.ACTION_UP) {
			if(mCoreLogic.isPlaying()) {
				mPlayPauseButton.setBackgroundResource(R.drawable.ic_media_pause);
			} else {
				mPlayPauseButton.setBackgroundResource(R.drawable.ic_media_play);					
			}
			if (mPlayPauseButton.getVisibility() == View.VISIBLE) {
				FullScreenCompatWrapper.fullScreen(mRelativeLayout, true);
			}
			
			mSeekBar.setVisibility(mSeekBar.getVisibility() == SeekBar.VISIBLE ? SeekBar.INVISIBLE : SeekBar.VISIBLE);
			mPlayPauseButton.setVisibility(mPlayPauseButton.getVisibility() == Button.VISIBLE ? Button.INVISIBLE : Button.VISIBLE);
			mRewindButton.setVisibility(mRewindButton.getVisibility() == Button.VISIBLE ? Button.INVISIBLE : Button.VISIBLE);
			mRelativeLayout.requestLayout();

		}

		if (event.getActionMasked() == MotionEvent.ACTION_POINTER_DOWN) {
			if (event.getPointerCount() == 2) {
				touchX = (event.getX(0) + event.getX(1))/2f;
				touchY = (event.getY(0) + event.getY(1))/2f;
			}
		}
		if (event.getActionMasked() == MotionEvent.ACTION_POINTER_UP) {
			touchX = 0f;
		}

		if (event.getAction() == MotionEvent.ACTION_DOWN) {
			touchX = event.getX();
			touchY = event.getY();
		}

		if (event.getAction() == MotionEvent.ACTION_MOVE && event.getPointerCount() == 1) {
			if (touchX == 0f) {
				touchX = event.getX();
				touchY = event.getY();
			} else {
				float dx = event.getX() - touchX;
				float dy = event.getY() - touchY;
				touchX = event.getX();
				touchY = event.getY();
				if (mCoreLogic.getCameraMode() == CoreLogic.CAMERA_MODE_SENSOR2) {
					dx = -dx;
					dy = -dy;
				}

				orientationEstimater.rotateInDisplay(dx, dy);

	
				// mCoreLogic.setCameraOrientation(orientationEstimater.getCurrentOrientation());
				mCoreLogic.setSensorRotationMatrix(orientationEstimater.getRotationMatrix());
			}
		}
		if (event.getAction() == MotionEvent.ACTION_MOVE && event.getPointerCount() == 2) {
			float dy = (event.getY(0) + event.getY(1))/2f - touchY;
			float dx = (event.getX(0) + event.getX(1))/2f - touchX;

			touchX = (event.getX(0) + event.getX(1))/2f;
			touchY = (event.getY(0) + event.getY(1))/2f;

			float cameraPosition[] = mCoreLogic.getCameraPositionAsRef();
			float mCameraOrientation[] = mCoreLogic.getCameraOrientationAsRef();
			cameraPosition[0] -= Math.cos(mCameraOrientation[0]) * dx * 0.1f;
			cameraPosition[2] += Math.sin(mCameraOrientation[0]) * dx * 0.1f;
			cameraPosition[1] += dy * 0.1f;

		}

		final boolean isInProgres = mScaleGestureDetector.isInProgress();
		mScaleGestureDetector.onTouchEvent(event);
		return isInProgres || mScaleGestureDetector.isInProgress();
	}
	
	@Override
	public void onSaveInstanceState(Bundle bundle) {
		mCoreLogic.storeState();
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		mCoreLogic.storeState();
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub
		
	}

	private float[] gravHistory = new float[10]; // Gravity (G)
	private int gravHistoryPos = 0;
	float[]			mAxV = new float[3];
	@Override
	public void onSensorChanged(SensorEvent event) {
		orientationEstimater.onSensorEvent(event);
	
		if(event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
			System.arraycopy(event.values, 0, mAxV, 0, 3);
			gravHistoryPos = (gravHistoryPos + 1) % gravHistory.length;
			gravHistory[gravHistoryPos] = (float) Math.sqrt(mAxV[0] * mAxV[0] + mAxV[1]*mAxV[1] + mAxV[2]*mAxV[2]) / 9.8f;
		}
		
		if (gravHistory[gravHistoryPos] < 0.4) { // 0.3G
			if (gravHistory[(gravHistoryPos + 5)%gravHistory.length] > 1.5) {
				// jump!
				Log.d("","JUMP!");
				mCoreLogic.cameraJump();
			}
			return;
		}
		
		if (!orientationEstimater.isReady()) return;
		
		
		if (Math.abs(mCoreLogic.analogInput[0]) > 0.1) {
			orientationEstimater.rotate(0, -mCoreLogic.analogInput[0] * 0.005f);
		}
		if (Math.abs(mCoreLogic.analogInput[4]) > 0.1) {
			orientationEstimater.rotate(-mCoreLogic.analogInput[4] * 0.005f, 0);
		}
		if (Math.abs(mCoreLogic.analogInput[5]) > 0.1) {
			orientationEstimater.rotate(mCoreLogic.analogInput[5] * 0.005f, 0);
		}
		
		if (mCoreLogic.keyState[KeyEvent.KEYCODE_A] || mCoreLogic.keyState[KeyEvent.KEYCODE_PAGE_UP]) {
			orientationEstimater.rotate(0, 0.01f);
		}
		if (mCoreLogic.keyState[KeyEvent.KEYCODE_D] || mCoreLogic.keyState[KeyEvent.KEYCODE_PAGE_DOWN]) {
			orientationEstimater.rotate(0, -0.01f);
		}
		
		if (mCoreLogic.keyState[KeyEvent.KEYCODE_F]) {
			orientationEstimater.rotate(-0.01f, 0);
		}
		if (mCoreLogic.keyState[KeyEvent.KEYCODE_R]) {
			orientationEstimater.rotate(0.01f, 0);
		}
		
		//Log.d("Sensor","Orientation " + orientation[0] + "," + orientation[1] + "," + orientation[2]);
		// mCoreLogic.setCameraOrientation(orientationEstimater.getCurrentOrientation());
		mCoreLogic.setSensorRotationMatrix(orientationEstimater.getRotationMatrix());
	}
	
	float cameraPos[] = new float[]{0,17,-11};
	

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode >= 0 && keyCode < mCoreLogic.keyState.length) {
			mCoreLogic.keyState[keyCode] = true;
		}
		switch(keyCode) {
		case KeyEvent.KEYCODE_DPAD_UP:
		case KeyEvent.KEYCODE_DPAD_DOWN:
		case KeyEvent.KEYCODE_DPAD_LEFT:
		case KeyEvent.KEYCODE_DPAD_RIGHT:
		case KeyEvent.KEYCODE_W:
		case KeyEvent.KEYCODE_S:
			mCoreLogic.analogInput[0] = 0;
			mCoreLogic.analogInput[1] = 0;
			break;
		case KeyEvent.KEYCODE_M:
			openOptionsMenu();
			break;
		case KeyEvent.KEYCODE_BUTTON_R1:
		//case KeyEvent.KEYCODE_D:
			orientationEstimater.rotate(0, -0.05f);
			break;
		case KeyEvent.KEYCODE_BUTTON_L1:
		//case KeyEvent.KEYCODE_A:
			orientationEstimater.rotate(0, 0.05f);
			break;
		case KeyEvent.KEYCODE_V:
		case KeyEvent.KEYCODE_BUTTON_Y: // ^
			mCoreLogic.toggleViewMode();
			break;
		case KeyEvent.KEYCODE_C:
		case KeyEvent.KEYCODE_BUTTON_X: // []
			mCoreLogic.toggleCameraView();
			break;
		case KeyEvent.KEYCODE_X:
		case KeyEvent.KEYCODE_BUTTON_B: // O
			mCoreLogic.cameraJump();
			break;
		case KeyEvent.KEYCODE_BUTTON_A: // X
		case KeyEvent.KEYCODE_SPACE:
			FullScreenCompatWrapper.fullScreen(mRelativeLayout, true);
			mSeekBar.setVisibility(View.INVISIBLE);
			mPlayPauseButton.setVisibility(View.INVISIBLE);
			mRewindButton.setVisibility(View.INVISIBLE);
			mCoreLogic.toggleStartStop();
			break;
		default:
			return super.onKeyDown(keyCode, event);
		}
		//mCoreLogic.setCameraPosition(cameraPos);
		return true;
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		if (keyCode >= 0 && keyCode < mCoreLogic.keyState.length) {
			mCoreLogic.keyState[keyCode] = false;
		}
		return super.onKeyUp(keyCode, event);
	}

	@Override
	public boolean onGenericMotionEvent(MotionEvent event) {
		if (event.getAction() == MotionEvent.ACTION_MOVE) {
			Log.d("MotionEvent" ,"Action: " + event.getAction() + " s:" + event.getSource());
			float h1 = event.getAxisValue(MotionEvent.AXIS_X);
			float v1 = event.getAxisValue(MotionEvent.AXIS_Y);
			float h2 = event.getAxisValue(MotionEvent.AXIS_Z);
			float v2 = event.getAxisValue(MotionEvent.AXIS_RZ);
			mCoreLogic.analogInput[0] = h1;
			mCoreLogic.analogInput[1] = v1;
			mCoreLogic.analogInput[2] = h2;
			mCoreLogic.analogInput[3] = v2;
			mCoreLogic.analogInput[4] = event.getAxisValue(MotionEvent.AXIS_LTRIGGER);
			mCoreLogic.analogInput[5] = event.getAxisValue(MotionEvent.AXIS_RTRIGGER);
			return true;
		}
		return super.onGenericMotionEvent(event);
	}
	
	
	
}