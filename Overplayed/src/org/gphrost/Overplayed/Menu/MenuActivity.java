package org.gphrost.Overplayed.Menu;

import java.io.File;
import java.io.FilenameFilter;

import org.gphrost.Overplayed.MainService;
import org.gphrost.Overplayed.Overplayed;
import org.gphrost.Overplayed.Preferences;
import org.gphrost.Overplayed.Controller.ButtonControl;
import org.gphrost.Overplayed.Controller.ControlView;
import org.gphrost.Overplayed.Controller.Controller;
import org.gphrost.Overplayed.Controller.DPad;
import org.gphrost.Overplayed.Controller.Joystick;

import com.gphrost.Overplayed.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewManager;
import android.view.WindowManager;
import android.view.ViewGroup.LayoutParams;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

public class MenuActivity extends Activity implements OnCancelListener {
	static CustomizeViewGroup drawview;
	public static Controller controller;
	private SeekBar alphaBar;
	
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if ((getIntent().getFlags() & Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY) != 0) {
			startActivity(new Intent(this, Overplayed.class));
			finish();
		} else {
			if (drawview == null) {
				drawview = new CustomizeViewGroup(this);
				controller = MainService.controller;
			} else {
				((ViewGroup) drawview.getParent()).removeView(drawview);
			}

			LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
			this.setContentView(drawview, params);

			MenuActivity.drawview.invalidate();
		}
	}

	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
	}

	protected Dialog onCreateDialog(int id, Bundle args) {
		switch (id) {
		case 0: {
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			LayoutInflater inflater = this.getLayoutInflater();
			View view = inflater.inflate(R.layout.menu, null);
			view.findViewById(R.id.loadButton).setOnClickListener(new LoadButtonListener());
			view.findViewById(R.id.editButton).setOnClickListener(new EditButtonListener());

			EditText editScale = (EditText) view.findViewById(R.id.editScale);
			editScale.setOnEditorActionListener(new EditScaleListener());
			editScale.addTextChangedListener(new EditScaleWatcher());
			editScale.setText(Float.toString(Preferences.Load.scale(this)));

			alphaBar = ((SeekBar) view.findViewById(R.id.alphaBar));
			alphaBar.setOnSeekBarChangeListener(new AlphaChangeListener());
			alphaBar.setProgress((int) (Preferences.Load.alpha(this) * alphaBar.getMax()));

			CheckBox hapticToggle = (CheckBox) view.findViewById(R.id.hapticToggle);
			hapticToggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener(){
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					controller.haptic = isChecked;
					Preferences.Save.haptic(MenuActivity.this, isChecked);
				}
			});
			hapticToggle.setChecked(Preferences.Load.haptic(MenuActivity.this));
			
			builder.setView(view).setTitle("Overplayed Menu").setNeutralButton("OK", new CancelButtonListener())
					.setNegativeButton("Quit", new QuitButtonListener()).setOnCancelListener(this);
			AlertDialog ad = builder.create();
			return ad;
		}
		case 1: {
		}
		}
		return null;
	}

	@SuppressWarnings("deprecation")
	void dismissMenu() {
		Preferences.Save.alpha(this, (float) alphaBar.getProgress() / alphaBar.getMax());
		dismissDialog(0);
	}

	public void onCancel(DialogInterface arg0) {
		dismissMenu();
		MenuActivity.this.finish();
	}

	public void onBackPressed() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Save before exiting?");
		builder.setPositiveButton("Save", new OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				try {
					controller.writeXmlFile(MenuActivity.this);
				} catch (Exception e) {
				}
				dialog.dismiss();
				MenuActivity.super.onBackPressed();
			}
		}).setNegativeButton("Don't Save", new OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				File file = new File(MainService.staticThis.getExternalFilesDir(null), Preferences.Load.defaultController(MenuActivity.this) + ".xml");
				MainService.controller = new Controller(MainService.staticThis, file);
				dialog.dismiss();
				MenuActivity.super.onBackPressed();
			}
		}).setNeutralButton("Cancel", new OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		});
		builder.show();
	}

	protected void onPause() {
		if (isFinishing()) {
			startController();
		}
		super.onPause();
	}

	void startController() {
		try {
			drawview = null;
			controller.customize = false;
			controller.detach();
			controller = null;
			MainService.controller.attach((WindowManager) MainService.staticThis.getSystemService(WINDOW_SERVICE), MainService.staticThis);
			MainService.menuButton.active = true;
			MainService.controller.wm.addView(MainService.menuButton, MainService.menuParams);
		} catch (Exception e) {
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.editmenu, menu);
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		boolean active = (controller.activeControl != null);
		menu.findItem(R.id.properties).setEnabled(active);
		menu.findItem(R.id.removebutton).setEnabled(active);
		menu.findItem(R.id.locktogrid).setChecked(controller.lockToGrid);
		return true;
	}

	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		if (item.getItemId() == R.id.properties) {
			displayProperties();
		} else if (item.getItemId() == R.id.save) {
			try {
				controller.writeXmlFile(MenuActivity.this);
			} catch (Exception e) {
			}
			return true;
		} else if (item.getItemId() == R.id.saveas) {
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			final EditText edit = new EditText(this);
			edit.setText(controller.name);
			builder.setPositiveButton("Save", new OnClickListener() {

				public void onClick(DialogInterface arg0, int arg1) {
					if (edit.getText().toString().length() > 0) {
						String oldName = controller.name;
						try {
							controller.name = edit.getText().toString();
							controller.writeXmlFile(MenuActivity.this);
							arg0.dismiss();
						} catch (Exception e) {
							controller.name = oldName;
							Toast.makeText(MenuActivity.this, "Error", Toast.LENGTH_SHORT).show();
						}
					}
				}
			}).setNegativeButton("Cancel", new OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
				}

			}).setView(edit).setTitle("Save as...").show();
			return true;
		} else if (item.getItemId() == R.id.locktogrid) {
			item.setChecked(!item.isChecked());
			controller.lockToGrid = item.isChecked();
		} else if (item.getItemId() == R.id.addButton) {
			ButtonControl button = new ButtonControl(this, 1f, (byte) 0, Gravity.LEFT | Gravity.TOP, 0, 0, "Button", controller);
			controller.controls.add(button);
			controller.wm.addView(button, button.params);
			controller.activeControl = button;
			displayProperties();
		} else if (item.getItemId() == R.id.addDPad) {
			DPad button = new DPad(this, 3f, (byte) 12, (byte) 13, (byte) 14, (byte) 15, Gravity.LEFT | Gravity.TOP, 0, 0, controller);
			controller.controls.add(button);
			controller.wm.addView(button, button.params);
			controller.activeControl = button;
			displayProperties();
		} else if (item.getItemId() == R.id.addJoystick) {
			Joystick button = new Joystick(this, 3f, (byte) 0, (byte) 1, Gravity.LEFT | Gravity.TOP, 0, 0, controller);
			controller.controls.add(button);
			controller.wm.addView(button, button.params);
			controller.activeControl = button;
			displayProperties();
		} else if (item.getItemId() == R.id.removebutton) {
			controller.controls.remove(controller.activeControl);
			controller.wm.removeView(controller.activeControl);
			controller.activeControl = null;
		}
		return false;
	}

	void displayProperties() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		LayoutInflater inflater = this.getLayoutInflater();
		View view = inflater.inflate(R.layout.properties, null);
		RadioGroup radioGroup = (RadioGroup) view.findViewById(R.id.vRadioGroup);
		if ((controller.activeControl.params.gravity & Gravity.TOP) == Gravity.TOP)
			radioGroup.check(R.id.radioTop);
		else if ((controller.activeControl.params.gravity & Gravity.BOTTOM) == Gravity.BOTTOM)
			radioGroup.check(R.id.radioBottom);
		radioGroup.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			public void onCheckedChanged(RadioGroup group, int checkedId) {
				if (checkedId == R.id.radioTop)
					MenuActivity.controller.activeControl.params.gravity = MenuActivity.controller.activeControl.params.gravity & (Gravity.BOTTOM ^ 0xffffffff)
							| Gravity.TOP;
				else if (checkedId == R.id.radioBottom)
					MenuActivity.controller.activeControl.params.gravity = MenuActivity.controller.activeControl.params.gravity & (Gravity.TOP ^ 0xffffffff)
							| Gravity.BOTTOM;
				else
					MenuActivity.controller.activeControl.params.gravity = MenuActivity.controller.activeControl.params.gravity
							& ((Gravity.TOP | Gravity.BOTTOM) ^ 0xffffffff) | Gravity.CENTER;
				MenuActivity.drawview.updateViewLayout(MenuActivity.controller.activeControl, MenuActivity.controller.activeControl.params);
			}
		});

		radioGroup = (RadioGroup) view.findViewById(R.id.hRadioGroup);
		if ((controller.activeControl.params.gravity & Gravity.LEFT) == Gravity.LEFT)
			radioGroup.check(R.id.radioLeft);
		else if ((controller.activeControl.params.gravity & Gravity.RIGHT) == Gravity.RIGHT)
			radioGroup.check(R.id.radioRight);
		radioGroup.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			public void onCheckedChanged(RadioGroup group, int checkedId) {
				if (checkedId == R.id.radioLeft)
					MenuActivity.controller.activeControl.params.gravity = MenuActivity.controller.activeControl.params.gravity & (Gravity.RIGHT ^ 0xffffffff)
							| Gravity.LEFT;
				else if (checkedId == R.id.radioRight)
					MenuActivity.controller.activeControl.params.gravity = MenuActivity.controller.activeControl.params.gravity & (Gravity.LEFT ^ 0xffffffff)
							| Gravity.RIGHT;
				else
					MenuActivity.controller.activeControl.params.gravity = MenuActivity.controller.activeControl.params.gravity
							& ((Gravity.LEFT | Gravity.RIGHT) ^ 0xffffffff) | Gravity.CENTER;
				MenuActivity.drawview.updateViewLayout(MenuActivity.controller.activeControl, MenuActivity.controller.activeControl.params);
			}
		});

		EditText editText = (EditText) view.findViewById(R.id.editSize);
		editText.setText(Float.toString(MenuActivity.controller.activeControl.radiusScale));
		editText.addTextChangedListener(new TextWatcher() {
			public void afterTextChanged(Editable arg0) {
				try {
					float newRadius = Float.valueOf(arg0.toString());
					if (newRadius > 0f)
						MenuActivity.controller.activeControl.radiusScale = newRadius;
				} catch (Exception e) {
				}
				MenuActivity.controller.activeControl.updateParams();
				MenuActivity.drawview.updateViewLayout(MenuActivity.controller.activeControl, MenuActivity.controller.activeControl.params);
			}

			public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
			}

			public void onTextChanged(CharSequence s, int start, int before, int count) {
			}

		});

		editText = (EditText) view.findViewById(R.id.editXPos);
		editText.setText(Float.toString(MenuActivity.controller.activeControl.xOffsetScale / 2f));
		editText.addTextChangedListener(new TextWatcher() {
			public void afterTextChanged(Editable arg0) {
				try {
					MenuActivity.controller.activeControl.xOffsetScale = Float.valueOf(arg0.toString()) * 2f;
				} catch (Exception e) {
				}
				MenuActivity.controller.activeControl.updateParams();
				MenuActivity.drawview.updateViewLayout(MenuActivity.controller.activeControl, MenuActivity.controller.activeControl.params);
			}

			public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
			}

			public void onTextChanged(CharSequence s, int start, int before, int count) {
			}

		});

		editText = (EditText) view.findViewById(R.id.editYPos);
		editText.setText(Float.toString(MenuActivity.controller.activeControl.yOffsetScale / 2f));
		editText.addTextChangedListener(new TextWatcher() {
			public void afterTextChanged(Editable arg0) {
				try {
					MenuActivity.controller.activeControl.yOffsetScale = Float.valueOf(arg0.toString()) * 2f;
				} catch (Exception e) {
				}
				MenuActivity.controller.activeControl.updateParams();
				MenuActivity.drawview.updateViewLayout(MenuActivity.controller.activeControl, MenuActivity.controller.activeControl.params);
			}

			public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
			}

			public void onTextChanged(CharSequence s, int start, int before, int count) {
			}

		});

		LinearLayout propertiesLayout = (LinearLayout) view.findViewById(R.id.propertiesLayout);
		controller.activeControl.appendProperties(this, propertiesLayout);
		builder.setView(view).setTitle("Button Properties").setNeutralButton("OK", new OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		});
		AlertDialog ad = builder.create();
		ad.show();
	}

	class CancelButtonListener implements OnClickListener {
		public void onClick(DialogInterface dialog, int id2) {
			MenuActivity.this.onCancel(dialog);
		}
	}

	class QuitButtonListener implements OnClickListener {
		public void onClick(DialogInterface dialog, int id2) {
			MainService.staticThis.stopSelf();
			MenuActivity.this.dismissMenu();
			MenuActivity.this.finish();
		}
	}

	class EditButtonListener implements android.view.View.OnClickListener {
		public void onClick(View arg0) {
			controller.customize = true;
			drawview.invalidate();
			dismissMenu();
		}
	}

	class EditScaleWatcher implements TextWatcher {
		public void afterTextChanged(Editable s) {
			try {
				float newScale = Float.parseFloat(s.toString());
				if ((int) (newScale * ControlView.getStandardRadius(getResources().getDisplayMetrics())) > 0) {
					Preferences.Save.scale(MenuActivity.this, newScale);
					controller.updateScale(newScale);
				}
			} catch (NumberFormatException e) {
			}
		}

		public void beforeTextChanged(CharSequence s, int start, int count, int after) {
		}

		public void onTextChanged(CharSequence s, int start, int before, int count) {
		}
	}

	class EditScaleListener implements OnEditorActionListener {
		public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
			((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(view.getWindowToken(),
					InputMethodManager.RESULT_UNCHANGED_SHOWN);
			return true;
		}
	}

	class AlphaChangeListener implements OnSeekBarChangeListener {
		public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
			MenuActivity.controller.updateAlpha((float) progress / seekBar.getMax());
			MenuActivity.drawview.invalidate();
		}

		public void onStartTrackingTouch(SeekBar arg0) {
		}

		public void onStopTrackingTouch(SeekBar arg0) {
		}
	}

	class LoadButtonListener implements android.view.View.OnClickListener, OnItemLongClickListener {
		File[] files;
		ArrayAdapter<String> adapter;

		public void onClick(View view) {
			try{
				AlertDialog.Builder builder = new AlertDialog.Builder(MenuActivity.this);
				files = MenuActivity.this.getExternalFilesDir(null).listFiles(new FilenameFilter() {
					public boolean accept(File dir, String filename) {
						return filename.substring(filename.length() - 4).equals(".xml");
					}
				});
				adapter = new ArrayAdapter<String>(MenuActivity.this, android.R.layout.simple_list_item_1);

				for (int i = 0; i < files.length; i++) {
					String string = files[i].getName();
					adapter.add(string.substring(0, string.length() - 4));
				}
				builder.setTitle("Load Controller");
				builder.setNegativeButton("Cancel", new OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
					}
				});
				builder.setAdapter(adapter, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface arg0, int which) {
						Preferences.Save.alpha(MenuActivity.this, (float) alphaBar.getProgress() / alphaBar.getMax());
						controller.detach();
						controller = new Controller(MenuActivity.this, files[which]);
						controller.attach((ViewManager) drawview, MenuActivity.this);
					}
				});
				AlertDialog ad = builder.create();
				ad.getListView().setOnItemLongClickListener(this);
				ad.show();
			} catch (Exception e){
				Toast.makeText(MenuActivity.this, "External storage unavailable", Toast.LENGTH_LONG).show();
			}
		}

		public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, long id) {
			AlertDialog.Builder builder = new AlertDialog.Builder(MenuActivity.this);
			builder.setTitle("Delete?");
			builder.setPositiveButton("Delete", new OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					files[position].delete();
					adapter.remove(adapter.getItem(position));
					dialog.dismiss();
				}
			}).setNegativeButton("Cancel", new OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
				}
			});
			builder.show();
			return true;
		}
	}

	public class CustomizeViewGroup extends ViewGroup {

		public CustomizeViewGroup(Context context) {
			super(context);
		}

		@Override
		protected void onLayout(boolean changed, int l, int t, int r, int b) {
			for (int i = 0; i < this.getChildCount(); i++) {
				ControlView v = (ControlView) getChildAt(i);
				v.updateOffset();
				v.layout(v.xOffset, v.yOffset, v.xOffset + v.params.height, v.yOffset + v.params.width);
			}
		}

		@SuppressWarnings("deprecation")
		protected void onAttachedToWindow() {
			super.onAttachedToWindow();
			if (controller.active) controller.detach();
			controller.attach((ViewManager) drawview, MenuActivity.this);
			MenuActivity.this.showDialog(0, new Bundle());
		}

		@Override
		public boolean onTouchEvent(MotionEvent event) {
			if (controller.activeControl != null) {
				ControlView tempControl = controller.activeControl;
				controller.activeControl = null;
				tempControl.invalidate();
				return true;
			}
			return false;
		}
	}

}