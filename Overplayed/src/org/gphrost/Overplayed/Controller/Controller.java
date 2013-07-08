package org.gphrost.Overplayed.Controller;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;

import org.gphrost.Overplayed.MainService;
import org.gphrost.Overplayed.NetworkThread;
import org.gphrost.Overplayed.Preferences;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

import com.gphrost.Overplayed.R;

import android.util.Xml;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.content.Context;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Paint.Style;
import android.view.ViewManager;
import android.widget.Toast;

public class Controller {

	// The visibility state of the game controller
	public boolean active = true;

	public List<Short> analogState;
	// Digital button values for the network packet
	public List<Boolean> buttonState;

	// Opaque paint for rendering button bitmaps
	protected final Paint downPaint = new Paint();
	// Paint used for creating button bitmaps
	protected final Paint fillPaint = new Paint();
	protected Paint halfPaint = new Paint();
	// Transparent paint for rendering button bitmaps
	protected final Paint upPaint = new Paint();

	protected final Paint strokePaint = new Paint();
	protected final Paint clearPaint = new Paint();
	protected final Paint blackPaint = new Paint();
	{
		downPaint.setARGB(255, 128, 128, 255);
		upPaint.setARGB(128, 255, 255, 255);
		halfPaint.setARGB(128, 255, 255, 255);
		fillPaint.setARGB(255, 192, 192, 192);
		fillPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
		strokePaint.setARGB(255, 255, 255, 255);
		strokePaint.setStyle(Style.STROKE);
		strokePaint.setStrokeWidth(5);
		blackPaint.setARGB(255, 0, 0, 0);

		clearPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
	}
	public ArrayList<ControlView> controls; // Collection of controls

	MainService parentService; // Handle for the MainService
	/*
	 * AlphaButton alphaButton; DisableButton hideButton; // Button used to
	 * toggle visibilty of the ControlView stopButton; // Button used to quit
	 */
	public NetworkThread thread; // Handle for the thread
	public ViewManager wm; // Handle for the WindowManager

	public String name;

	public boolean customize = false;

	public ControlView activeControl = null;

	public boolean lockToGrid = true;

	public boolean haptic;

	public Controller(Context context, File file) {
		name = file.getName();
		name = name.substring(0, name.length() - 4);
		if (context.getClass() == MainService.class)
			this.parentService = (MainService) context;

		analogState = Collections.synchronizedList(new ArrayList<Short>(4));
		analogState.add((short) 16383);
		analogState.add((short) 16383);
		analogState.add((short) 16383);
		analogState.add((short) 16383);

		buttonState = Collections.synchronizedList(new ArrayList<Boolean>(16));
		buttonState.add(false);
		buttonState.add(false);
		buttonState.add(false);
		buttonState.add(false);
		buttonState.add(false);
		buttonState.add(false);
		buttonState.add(false);
		buttonState.add(false);
		buttonState.add(false);
		buttonState.add(false);
		buttonState.add(false);
		buttonState.add(false);
		buttonState.add(false);
		buttonState.add(false);
		buttonState.add(false);
		buttonState.add(false);
		XmlPullParser xrp;
		xrp = Xml.newPullParser();

		boolean writeDefault = false;
		try {
			xrp.setInput(new FileReader(file));
		} catch (Exception e) {
			file = new File(MainService.staticThis.getExternalFilesDir(null), "default" + ".xml");
			name = "default";
			try {
				Toast.makeText(context, "File not found, loading default", Toast.LENGTH_LONG).show();
				xrp.setInput(new FileReader(file));
			} catch (Exception e1) {
				xrp = MainService.staticThis.getResources().getXml(R.xml.defaultlayout);
				writeDefault = true;
			}
		}
		ArrayList<ControlView> controls = new ArrayList<ControlView>();
		try {
			xrp.next();
			int eventType = xrp.getEventType();
			while (eventType != XmlPullParser.END_DOCUMENT) {
				if (eventType == XmlPullParser.START_TAG && !xrp.getName().equalsIgnoreCase("controller")) {
					float radiusScale = Float.parseFloat(xrp.getAttributeValue(null, "radiusScale"));
					int gravity = Integer.parseInt(xrp.getAttributeValue(null, "gravity"));
					float xOffset = Float.parseFloat(xrp.getAttributeValue(null, "xOffset"));
					float yOffset = Float.parseFloat(xrp.getAttributeValue(null, "yOffset"));
					if (xrp.getName().equalsIgnoreCase("joystick")) {
						byte xIndex = Byte.parseByte(xrp.getAttributeValue(null, "xIndex"));
						byte yIndex = Byte.parseByte(xrp.getAttributeValue(null, "yIndex"));
						controls.add(new Joystick(context, radiusScale, xIndex, yIndex, gravity, xOffset, yOffset, this));
					} else if (xrp.getName().equalsIgnoreCase("button")) {
						byte buttonIndex = Byte.parseByte(xrp.getAttributeValue(null, "buttonIndex"));
						String label = xrp.getAttributeValue(null, "label");
						controls.add(new ButtonControl(context, radiusScale, buttonIndex, gravity, xOffset, yOffset, label, this));
					} else if (xrp.getName().equalsIgnoreCase("dpad")) {
						byte upIndex = (byte) Integer.parseInt(xrp.getAttributeValue(null, "upIndex"));
						byte downIndex = (byte) Integer.parseInt(xrp.getAttributeValue(null, "downIndex"));
						byte leftIndex = (byte) Integer.parseInt(xrp.getAttributeValue(null, "leftIndex"));
						byte rightIndex = (byte) Integer.parseInt(xrp.getAttributeValue(null, "rightIndex"));
						controls.add(new DPad(context, radiusScale, upIndex, downIndex, leftIndex, rightIndex, gravity, xOffset, yOffset, this));
					}
				}
				eventType = xrp.next();
			}
		} catch (XmlPullParserException e) {
			Toast.makeText(context, "File parse error", Toast.LENGTH_LONG).show();
		} catch (IOException e) {
			Toast.makeText(context, "File access error", Toast.LENGTH_LONG).show();
		}
		this.controls = controls;
		if (writeDefault) {
			name = "default";
			Toast.makeText(context, "Default file not found, writing default file", Toast.LENGTH_LONG).show();
			this.writeXmlFile(context);
		}

		updateAlpha(Preferences.Load.alpha(context));
		Preferences.Save.defaultController(context, name);
		
		haptic = Preferences.Load.haptic(context);
	}

	/**
	 * Make the controller visible by adding the control views to the
	 * WindowManager. This does not check whether or not the views are already
	 * added to the WindowManager and thus might throw an exception.
	 */
	public void setActive(Context context) {
		active = true;
		for (ControlView view : controls) {
			wm.addView(view, view.params);
		}
		updateAlpha(Preferences.Load.alpha(context));
		/*
		 * wm.removeView(stopButton); // Hide the quit button
		 * wm.removeView(alphaButton); // Hide the quit button
		 * wm.removeView(hideButton); wm.addView(hideButton, hideButton.params);
		 */
	}

	/**
	 * Make the controller invisible by removing the control views from the
	 * WindowManager. This does not check whether or not the views are already
	 * removed from the WindowManager and thus might throw an exception.
	 */
	public void setInactive() {
		active = false;
		for (ControlView view : controls) {
			wm.removeView(view);
		}
		/*
		 * wm.addView(stopButton, stopButton.params); // Show the quit button
		 * wm.addView(alphaButton, alphaButton.params); // Show the quit button
		 */
	}

	public void updateAlpha(float alpha) {
		// TODO
		upPaint.setARGB((int) (255 * alpha), 255, 255, 255);
		fillPaint.setARGB(255, 192, 192, 192);
		for (ControlView control : controls)
			control.invalidate();
	}

	public void updateScale(float scale) {
		// Preferences.Save.scale(this.getC, scale);
		for (ControlView control : controls)
			control.updateParams();
	}

	public void attach(ViewManager wm, Context context) {
		this.wm = wm;
		// Add the buttons so when we activate the controller we don't
		// get an exception
		/*
		 * if (!customize) { wm.addView(hideButton, hideButton.params);
		 * wm.addView(alphaButton, alphaButton.params); wm.addView(stopButton,
		 * stopButton.params); }
		 */
		setActive(context);
	}

	public void detach() {
		if (active)
			setInactive();
		// Remove the last views and get out of Dodge
		/*
		 * if (!customize) { wm.removeView(hideButton);
		 * wm.removeView(alphaButton); wm.removeView(stopButton); }
		 */
		// This let's this service know it's not running
	}

	public void writeXmlFile(Context context) {
		try {
			// create a new file called "new.xml" in the SD card
			File newxmlfile = new File(MainService.staticThis.getExternalFilesDir(null), name + ".xml");

			// newxmlfile.createNewFile();
			// we have to bind the new file with a FileOutputStream
			FileOutputStream fileos = new FileOutputStream(newxmlfile);

			// we create a XmlSerializer in order to write xml data
			XmlSerializer serializer = Xml.newSerializer();
			// we set the FileOutputStream as output for the serializer,
			// using UTF-8 encoding
			serializer.setOutput(fileos, "UTF-8");
			// Write <?xml declaration with encoding (if encoding not null)
			// and standalone flag (if standalone not null)
			serializer.startDocument(null, Boolean.valueOf(true));
			// set indentation option
			serializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
			serializer.startTag(null, "controller");

			for (ControlView control : controls)
				control.writeXml(serializer, fileos);

			serializer.endTag(null, "controller");
			serializer.endDocument();
			// write xml data into the FileOutputStream
			serializer.flush();
			// finally we close the file stream
			fileos.close();

			Preferences.Save.defaultController(context, name);
			Toast.makeText(context, "Saved", Toast.LENGTH_SHORT).show();
		} catch (IllegalArgumentException e) {
			Toast.makeText(context, "Invalid filename", Toast.LENGTH_SHORT).show();
		} catch (Exception e) {
			Toast.makeText(context, "Error writing file", Toast.LENGTH_SHORT).show();
		}
	}
}
