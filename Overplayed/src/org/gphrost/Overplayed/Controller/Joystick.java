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
import android.graphics.Rect;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.AdapterView.OnItemSelectedListener;

/**
 * Game controller joystick.
 * 
 * @author Steven T. Ramzel
 * @see ControlView
 */
@SuppressLint("ViewConstructor")
public class Joystick extends ControlView {
	private Bitmap gate; // Bitmap used for drawing the joystick gate
	// Half of the radius, for rendering
	private float halfRadius;
	private Bitmap handle; // Bitmap used for drawing the joystick handle
	private byte xIndex; // Axis index to map x-axis to
	private byte yIndex; // Axis index to map y-axis to
	private float screenX; // X coordinate of joystick handle
	private float screenY; // Y coordinate of joystick handle

	/**
	 * @param context
	 *            Application context.
	 * @param radiusScale
	 *            Radius of button as multiple of auto-generated standard button
	 *            radius.
	 * @param id 
	 * @param indexX
	 *            Axis index to map x-axis to
	 * @param indexY
	 *            Axis index to map y-axis to
	 * @param gravity
	 *            Placement of button within the screen as per Gravity.
	 * @param xOffset
	 *            X position for this button.
	 * @param yOffset
	 *            Y position for this button.
	 * @param controller 
	 */
	public Joystick(Context context, float radiusScale, byte indexX, byte indexY,
			int gravity, float xOffset, float yOffset, Controller controller) {
		super(context, radiusScale, gravity, xOffset, yOffset, controller);
		xIndex = indexX;
		yIndex = indexY;
		//updateParams();
		updateStatus(radius + xOffset, radius + yOffset);
	}

	/**
	 * Generates bitmaps used for drawing joystick.
	 * 
	 * @param radius
	 *            Radius of joystick in pixels.
	 */
	@Override
	void generateBitmap() {
		super.generateBitmap();
		// Size and shape of joystick gate
		ShapeDrawable gateShape = new ShapeDrawable();
		gateShape.setShape(new OvalShape());
		gateShape.setBounds(PADDING, PADDING, (int) radius * 2 - PADDING, (int) radius * 2
				- PADDING);

		// Size and shape of joystick handle
		ShapeDrawable handleShape = new ShapeDrawable();
		handleShape.setShape(new OvalShape());
		handleShape.setBounds(new Rect(PADDING, PADDING, (int) radius - PADDING,
				(int) radius - PADDING));

		// Create joystick gate bitmap and render shape
		gate = Bitmap.createBitmap((int) radius * 2, (int) radius * 2,
				Bitmap.Config.ARGB_4444);
		Canvas gateCanvas = new Canvas(gate);
		gateShape.getPaint().set(parent.fillPaint);
		gateShape.draw(gateCanvas);

		// Create joystick handle bitmap and render shape
		handle = Bitmap.createBitmap((int) radius, (int) radius, Bitmap.Config.ARGB_4444);
		Canvas handleCanvas = new Canvas(handle);
		handleShape.getPaint().set(parent.fillPaint);
		handleShape.draw(handleCanvas);

		// Used to draw
		halfRadius = radius * .5f;
	}

	@Override
	public void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		// Draw the joystick gate behind the handle
		canvas.drawBitmap(gate, 0, 0, parent.upPaint);
		// If down, draw handle opaque. Otherwise draw transparent.
		canvas.drawBitmap(handle, screenX + halfRadius, screenY + halfRadius,
				isDown ? parent.downPaint : parent.upPaint);
	}

	/**
	 * Updates joystick status for next network send.
	 * 
	 * @param screenX
	 *            X coordinate of touch with respect to entire screen.
	 * @param screenY
	 *            Y coordinate of touch with respect to entire screen.
	 */
	@Override
	public void updateStatus(float x, float y) {
		x -= radius + xOffset;
		y -= radius + yOffset;
		if (x == 0 && y == 0) {
			parent.analogState.set(xIndex, (short) (Short.MAX_VALUE / 2));
			parent.analogState.set(yIndex, (short) (Short.MAX_VALUE / 2));
		} else {
			// Distance between origin and touch coordinates
			float length = (float) Math.sqrt(y * y + x * x);
			// Sin and Cos are between 0 and radius
			float sin = y / length * radius;
			float cos = x / length * radius;
			float absY = Math.abs(y);
			float absX = Math.abs(x);
			float absCos = Math.abs(cos);
			float absSin = Math.abs(sin);

			// If outside radius than stick to the rim of the handle
			if (absSin < absY)
				y = sin;
			if (absCos < absX)
				x = cos;

			// Map the square coordinates to circular coordinates
			float maxLength = (absY > absX) ? absSin : absCos;

			// Update the network data, normalize to short value
			parent.analogState.set(xIndex, (short) (((x / maxLength) + 1f) * .5f * Short.MAX_VALUE));
			parent.analogState.set(yIndex, (short) (((y / maxLength) + 1f) * .5f * Short.MAX_VALUE));
		}

		// Update drawing info
		screenY = y;
		screenX = x;
	}
	
	@Override
	void writeXml(XmlSerializer serializer, OutputStream fileos) throws IllegalArgumentException, IllegalStateException, IOException {
        serializer.startTag(null, "joystick");
        super.writeXml(serializer, fileos);
        serializer.attribute(null, "xIndex", Integer.toString(xIndex));
        serializer.attribute(null, "yIndex", Integer.toString(yIndex));
        serializer.endTag(null, "joystick");
	}
	
	@Override
	public void appendProperties(Context context, LinearLayout propertiesLayout) {
		TextView text = new TextView(context);
		text.setText("X axis Index:");

		Spinner spinner = new Spinner(context);
		ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(context, com.gphrost.Overplayed.R.array.analogLabel_array,
				android.R.layout.simple_spinner_item);
		// Specify the layout to use when the list of choices appears
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		// Apply the adapter to the spinner
		spinner.setAdapter(adapter);
		spinner.setSelection(xIndex);
		spinner.setOnItemSelectedListener(new OnItemSelectedListener(){
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				xIndex = (byte) position;
			}
			public void onNothingSelected(AdapterView<?> parent) {
			}
		});
		
		TextView text2 = new TextView(context);
		text2.setText("Y axis Index:");

		Spinner spinner2 = new Spinner(context);
		// Apply the adapter to the spinner
		spinner2.setAdapter(adapter);
		spinner2.setSelection(yIndex);
		spinner2.setOnItemSelectedListener(new OnItemSelectedListener(){
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				yIndex = (byte) position;
			}
			public void onNothingSelected(AdapterView<?> parent) {
			}
		});
		
		propertiesLayout.addView(text);
		propertiesLayout.addView(spinner);
		propertiesLayout.addView(text2);
		propertiesLayout.addView(spinner2);
		
	}
}
