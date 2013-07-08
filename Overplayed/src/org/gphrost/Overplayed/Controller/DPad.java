/* Copyright (c) 2013 All Right Reserved Steven T. Ramzel
 *
 *	This file is part of Overplayed.
 *
 *	Overplayed is free software: you can redistribute it and/or modify
 *	it under the terms of the GNU Lesser General Public License as published by
 *	the Free Software Foundation, either version 3 of the License, or
 *	(at your option) any later version.
 *
 *	Overplayed is distributed in the hope that it will be useful,
 *	but WITHOUT ANY WARRANTY; without even the implied warranty of
 *	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *	GNU Lesser General Public License for more details.
 *
 *	You should have received a copy of the GNU Lesser General Public License
 *	along with Overplayed.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.gphrost.Overplayed.Controller;

import java.io.IOException;
import java.io.OutputStream;

import org.xmlpull.v1.XmlSerializer;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.AdapterView.OnItemSelectedListener;

/**
 * Game controller directional pad.
 * 
 * @author Steven T. Ramzel
 * @see ControlView
 */
@SuppressLint("ViewConstructor")
public class DPad extends ControlView {
	// Paint used to create d-pad lines
	protected static final Paint strokePaint = new Paint();
	{
		strokePaint.setARGB(255, 128, 128, 128);
		strokePaint.setStyle(Paint.Style.STROKE);
		strokePaint.setFlags(Paint.ANTI_ALIAS_FLAG);
		strokePaint.setStrokeWidth(4);

		// Set paint to clear text from button
		strokePaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
	}
	// Bitmap used for rendering d-pad
	private Bitmap dpadBitmap;
	// Circle shape for d-pad image generation
	private ShapeDrawable dpadShape = new ShapeDrawable();
	private int deadZone;// How far from center until key press registers
	private int downIndex; // Button to map Down to
	private int leftIndex; // Button to map Left to
	private int rightIndex; // Button to map Right to
	private int upIndex; // Button to map Up to

	/**
	 * @param context
	 *            Application context.
	 * @param radiusScale
	 *            Radius of d-pad as multiple of auto-generated standard button
	 *            radius.
	 * @param radiusScale
	 * @param upIndex
	 *            Button to Up map to.
	 * @param downIndex
	 *            Button to Down map to.
	 * @param leftIndex
	 *            Button to Left map to.
	 * @param rightIndex
	 *            Button to Right map to.
	 * @param gravity
	 *            Placement of button within the screen as per Gravity.
	 * @param xOffset
	 *            X position for this button.
	 * @param yOffset
	 *            Y position for this button.
	 * @param controller
	 */
	public DPad(Context context, float radiusScale, byte upIndex, byte downIndex, byte leftIndex, byte rightIndex, int gravity, float xOffset, float yOffset,
			Controller controller) {
		super(context, radiusScale, gravity, xOffset, yOffset, controller);
		this.upIndex = upIndex;
		this.downIndex = downIndex;
		this.leftIndex = leftIndex;
		this.rightIndex = rightIndex;
		// updateParams();
	}

	@Override
	public void onDraw(Canvas canvas) {
		// If down, draw opaque. Otherwise draw transparent.
		canvas.drawBitmap(dpadBitmap, 0, 0, isDown ? parent.downPaint : parent.upPaint);
		super.onDraw(canvas);
	}

	/**
	 * Generates bitmap used for drawing d-pad.
	 * 
	 * @param radius
	 *            Radius of d-pad in pixels.
	 */
	@Override
	void generateBitmap() {
		super.generateBitmap();
		// Size and shape
		dpadShape.setShape(new OvalShape());
		dpadShape.setBounds(new Rect(PADDING, PADDING, (int) radius * 2 - PADDING, (int) radius * 2 - PADDING));

		// Create d-pad bitmap and render
		dpadBitmap = Bitmap.createBitmap((int) radius * 2, (int) radius * 2, Bitmap.Config.ARGB_4444);
		Canvas dpadCanvas = new Canvas(dpadBitmap);
		dpadShape.getPaint().set(parent.fillPaint);
		dpadShape.draw(dpadCanvas);
		deadZone = (int) radius / 4;
		float sideOffset = ControlView.circleY(radius - 2, deadZone);
		dpadCanvas.drawLine(radius - sideOffset, radius + deadZone, radius - deadZone, radius + deadZone, strokePaint);
		dpadCanvas.drawLine(radius - sideOffset, radius - deadZone, radius - deadZone, radius - deadZone, strokePaint);
		dpadCanvas.drawLine(radius + deadZone, radius + deadZone, radius + sideOffset, radius + deadZone, strokePaint);
		dpadCanvas.drawLine(radius + deadZone, radius - deadZone, radius + sideOffset, radius - deadZone, strokePaint);
		dpadCanvas.drawLine(radius + deadZone, radius + deadZone, radius + deadZone, radius + sideOffset, strokePaint);
		dpadCanvas.drawLine(radius - deadZone, radius + deadZone, radius - deadZone, radius + sideOffset, strokePaint);
		dpadCanvas.drawLine(radius + deadZone, radius - sideOffset, radius + deadZone, radius - deadZone, strokePaint);
		dpadCanvas.drawLine(radius - deadZone, radius - sideOffset, radius - deadZone, radius - deadZone, strokePaint);
	}

	/**
	 * Updates d-pad status for next network send.
	 * 
	 * @param screenX
	 *            X coordinate of touch with respect to entire screen.
	 * @param screenY
	 *            Y coordinate of touch with respect to entire screen.
	 */
	@Override
	public void updateStatus(float screenX, float screenY) {
		screenY -= radius + yOffset;
		screenX -= radius + xOffset;
		boolean temp = screenY < -deadZone;
		if (temp != parent.buttonState.get(upIndex)) {
			if (parent.haptic) performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
			parent.buttonState.set(upIndex, temp);
		}
		temp = screenY > deadZone;
		if (temp != parent.buttonState.get(downIndex)) {
			if (parent.haptic) performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
			parent.buttonState.set(downIndex, temp);
		}
		temp = screenX < -deadZone;
		if (temp != parent.buttonState.get(leftIndex)) {
			if (parent.haptic) performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
			parent.buttonState.set(leftIndex, temp);
		}
		temp = screenX > deadZone;
		if (temp != parent.buttonState.get(rightIndex)) {
			if (parent.haptic) performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
			parent.buttonState.set(rightIndex, temp);
		}
	}

	@Override
	void writeXml(XmlSerializer serializer, OutputStream fileos) throws IllegalArgumentException, IllegalStateException, IOException {
		serializer.startTag(null, "dpad");
		super.writeXml(serializer, fileos);
		serializer.attribute(null, "upIndex", Integer.toString(upIndex));
		serializer.attribute(null, "downIndex", Integer.toString(downIndex));
		serializer.attribute(null, "leftIndex", Integer.toString(leftIndex));
		serializer.attribute(null, "rightIndex", Integer.toString(rightIndex));
		serializer.endTag(null, "dpad");
	}

	@Override
	public void appendProperties(Context context, LinearLayout propertiesLayout) {
		TextView text = new TextView(context);
		text.setText("Up Index:");

		Spinner spinner = new Spinner(context);
		ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(context, com.gphrost.Overplayed.R.array.buttonLabel_array,
				android.R.layout.simple_spinner_item);
		// Specify the layout to use when the list of choices appears
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		// Apply the adapter to the spinner
		spinner.setAdapter(adapter);
		spinner.setSelection(upIndex);
		spinner.setOnItemSelectedListener(new OnItemSelectedListener() {
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				upIndex = position;
			}

			public void onNothingSelected(AdapterView<?> parent) {
			}
		});

		TextView text2 = new TextView(context);
		text2.setText("Down Index:");

		Spinner spinner2 = new Spinner(context);
		// Apply the adapter to the spinner
		spinner2.setAdapter(adapter);
		spinner2.setSelection(downIndex);
		spinner2.setOnItemSelectedListener(new OnItemSelectedListener() {
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				downIndex = position;
			}

			public void onNothingSelected(AdapterView<?> parent) {
			}
		});

		TextView text3 = new TextView(context);
		text3.setText("Left Index:");

		Spinner spinner3 = new Spinner(context);
		// Apply the adapter to the spinner
		spinner3.setAdapter(adapter);
		spinner3.setSelection(leftIndex);
		spinner3.setOnItemSelectedListener(new OnItemSelectedListener() {
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				leftIndex = position;
			}

			public void onNothingSelected(AdapterView<?> parent) {
			}
		});

		TextView text4 = new TextView(context);
		text4.setText("Right Index:");

		Spinner spinner4 = new Spinner(context);
		// Apply the adapter to the spinner
		spinner4.setAdapter(adapter);
		spinner4.setSelection(rightIndex);
		spinner4.setOnItemSelectedListener(new OnItemSelectedListener() {
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				rightIndex = position;
			}

			public void onNothingSelected(AdapterView<?> parent) {
			}
		});

		propertiesLayout.addView(text);
		propertiesLayout.addView(spinner);
		propertiesLayout.addView(text2);
		propertiesLayout.addView(spinner2);
		propertiesLayout.addView(text3);
		propertiesLayout.addView(spinner3);
		propertiesLayout.addView(text4);
		propertiesLayout.addView(spinner4);

	}
}
