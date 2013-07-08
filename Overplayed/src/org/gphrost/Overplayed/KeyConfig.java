package org.gphrost.Overplayed;

import java.util.ArrayList;
import java.util.HashMap;

import com.gphrost.Overplayed.R;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnKeyListener;
import android.os.Build;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnGenericMotionListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.SimpleAdapter;

public class KeyConfig extends Activity {
	public static int id;
	static KeyConfig configInstance;
	private SimpleAdapter sa;
	ArrayList<HashMap<String, String>> list = new ArrayList<HashMap<String, String>>();

	@TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		configInstance = this;
		setContentView(R.layout.config);

		ListView buttonList = ((android.widget.ListView) this
				.findViewById(R.id.list));
		HashMap<String, String> item;
		for (int i = 0; i < controlStrings.length; i++) {
			item = new HashMap<String, String>();
			item.put("line1", controlStrings[i]);
			item.put("line2", keyStrings[Overplayed.boundButtons[i]]);
			list.add(item);
		}
		for (int i = 0; i < axisStrings.length; i++) {
			item = new HashMap<String, String>();
			item.put("line1", axisStrings[i]);
			try {
				item.put("line2",
						MotionEvent.axisToString(Overplayed.boundAxis[i]));
				list.add(item);
			} catch (NoSuchMethodError e) {

			}
		}
		sa = new SimpleAdapter(this, list, android.R.layout.two_line_list_item,
				new String[] { "line1", "line2" }, new int[] {
						android.R.id.text1, android.R.id.text2 });
		buttonList.setAdapter(sa);
		buttonList.setOnItemClickListener(mMessageClickedHandler);
	}

	// Create a message handling object as an anonymous class.
	private OnItemClickListener mMessageClickedHandler = new OnItemClickListener() {
		@TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
		public void onItemClick(AdapterView<?> parent, View v, int position,
				final long id2) {
			id = (int) id2;
			// Use the Builder class for convenient dialog construction
			AlertDialog.Builder builder = new AlertDialog.Builder(
					v.getContext());

			if (id < controlStrings.length) {
				builder.setMessage("Press key to bind...")
						.setPositiveButton("Clear",
								new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog,
											int id2) {
										changeKey((int) id, 0);
									}
								})
						.setNegativeButton("Cancel",
								new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog,
											int id2) {
									}
								});
				// Create the AlertDialog object and return it
				AlertDialog ad = builder.create();
				ad.setOnKeyListener(new OnKeyListener() {
					public boolean onKey(DialogInterface arg0, int arg1,
							KeyEvent arg2) {
						changeKey((int) id, arg1);
						arg0.dismiss();
						return true;
					}

				});
				ad.show();
			} else {
				String[] strings = { "Move Axis to bind..." };
				builder.setItems(strings,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id2) {
							}
						}).setNegativeButton("Cancel",
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id2) {
							}
						}); // Create the AlertDialog object and return
							// it
				final AlertDialog ad = builder.create();
				ad.getListView().setOnGenericMotionListener(
						new OnGenericMotionListener() {

							public boolean onGenericMotion(View v,
									MotionEvent event) {
								for (int i = 0; i < event.getDevice()
										.getMotionRanges().size(); i++) {
									int axisID = event.getDevice()
											.getMotionRanges().get(i).getAxis();
									if (Math.abs(event.getAxisValue(axisID)) > event
											.getDevice().getMotionRanges()
											.get(i).getFlat()) {
										KeyConfig.configInstance
												.changeAxis(
														(int) KeyConfig.id,
														axisID);
										ad.dismiss();
										return true;
									}
								}
								return false;
							}

						});
				ad.show();
			}
		}

	};

	void changeKey(int id, int arg1) {
		Overplayed.boundButtons[id] = arg1;
		list.get((int) id).put("line2",
				keyStrings[Overplayed.boundButtons[id]]);
		list.set((int) id, list.get((int) id));
		sa.notifyDataSetChanged();
		Preferences.Save.boundButtons(this, Overplayed.boundButtons);
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
	void changeAxis(int id, int arg1) {
		Overplayed.boundAxis[(int) id
				- KeyConfig.controlStrings.length] = arg1;
		list.get((int) id).put(
				"line2",
				MotionEvent.axisToString(Overplayed.boundAxis[id
						- KeyConfig.controlStrings.length]));
		list.set((int) id, list.get((int) id));
		sa.notifyDataSetChanged();
		Preferences.Save.boundAxis(this, Overplayed.boundAxis);
	}

	final static String[] keyStrings = { "", "SOFT LEFT", "SOFT RIGHT", "HOME",
			"BACK", "CALL", "ENDCALL", "0", "1", "2", "3", "4", "5", "6", "7",
			"8", "9", "STAR", "POUND", "DPAD UP", "DPAD DOWN", "DPAD LEFT",
			"DPAD RIGHT", "DPAD CENTER", "VOLUME UP", "VOLUME DOWN", "POWER",
			"CAMERA", "CLEAR", "A", "B", "C", "D", "E", "F", "G", "H", "I",
			"J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V",
			"W", "X", "Y", "Z", "COMMA", "PERIOD", "ALT LEFT", "ALT RIGHT",
			"SHIFT LEFT", "SHIFT RIGHT", "TAB", "SPACE", "SYM", "EXPLORER",
			"ENVELOPE", "ENTER", "DEL", "GRAVE", "MINUS", "EQUALS",
			"LEFT BRACKET", "RIGHT BRACKET", "BACKSLASH", "SEMICOLON",
			"APOSTROPHE", "SLASH", "AT", "NUM", "HEADSETHOOK", "FOCUS", "PLUS",
			"MENU", "NOTIFICATION", "SEARCH", "MEDIA PLAY PAUSE", "MEDIA STOP",
			"MEDIA NEXT", "MEDIA PREVIOUS", "MEDIA REWIND",
			"MEDIA FAST FORWARD", "MUTE", "PAGE UP", "PAGE DOWN",
			"PICTSYMBOLS", "SWITCH CHARSET", "BUTTON A", "BUTTON B",
			"BUTTON C", "BUTTON X", "BUTTON Y", "BUTTON Z", "BUTTON L1",
			"BUTTON R1", "BUTTON L2", "BUTTON R2", "BUTTON THUMBL",
			"BUTTON THUMBR", "BUTTON START", "BUTTON SELECT", "BUTTON MODE",
			"ESCAPE", "FORWARD DEL", "CTRL LEFT", "CTRL RIGHT", "CAPS LOCK",
			"SCROLL LOCK", "META LEFT", "META RIGHT", "FUNCTION", "SYSRQ",
			"BREAK", "MOVE HOME", "MOVE END", "INSERT", "FORWARD",
			"MEDIA PLAY", "MEDIA PAUSE", "MEDIA CLOSE", "MEDIA EJECT",
			"MEDIA RECORD", "F1", "F2", "F3", "F4", "F5", "F6", "F7", "F8",
			"F9", "F10", "F11", "F12", "NUM LOCK", "NUMPAD 0", "NUMPAD 1",
			"NUMPAD 2", "NUMPAD 3", "NUMPAD 4", "NUMPAD 5", "NUMPAD 6",
			"NUMPAD 7", "NUMPAD 8", "NUMPAD 9", "NUMPAD DIVIDE",
			"NUMPAD MULTIPLY", "NUMPAD SUBTRACT", "NUMPAD ADD", "NUMPAD DOT",
			"NUMPAD COMMA", "NUMPAD ENTER", "NUMPAD EQUALS",
			"NUMPAD LEFT PAREN", "NUMPAD RIGHT PAREN", "VOLUME MUTE", "INFO",
			"CHANNEL UP", "CHANNEL DOWN", "ZOOM IN", "ZOOM OUT", "TV",
			"WINDOW", "GUIDE", "DVR", "BOOKMARK", "CAPTIONS", "SETTINGS",
			"TV POWER", "TV INPUT", "STB POWER", "STB INPUT", "AVR POWER",
			"AVR INPUT", "PROG RED", "PROG GREEN", "PROG YELLOW", "PROG BLUE",
			"APP SWITCH", "BUTTON 1", "BUTTON 2", "BUTTON 3", "BUTTON 4",
			"BUTTON 5", "BUTTON 6", "BUTTON 7", "BUTTON 8", "BUTTON 9",
			"BUTTON 10", "BUTTON 11", "BUTTON 12", "BUTTON 13", "BUTTON 14",
			"BUTTON 15", "BUTTON 16", "LANGUAGE SWITCH", "MANNER MODE",
			"3D MODE", "CONTACTS", "CALENDAR", "MUSIC", "CALCULATOR",
			"ZENKAKU HANKAKU", "EISU", "MUHENKAN", "HENKAN",
			"KATAKANA HIRAGANA", "YEN", "RO", "KANA" };
	final static String[] controlStrings = { "BUTTON A", "BUTTON B",
			"BUTTON X", "BUTTON Y", "BUTTON L1", "BUTTON R1", "BUTTON L2",
			"BUTTON R2", "Select", "Start", "BUTTON L3", "BUTTON R3",
			"DPAD UP", "DPAD DOWN", "DPAD LEFT", "DPAD RIGHT" };
	final static String[] axisStrings = { "Right Analog X", "Right Analog Y",
			"Left Analog X", "Left Analog Y" };
}