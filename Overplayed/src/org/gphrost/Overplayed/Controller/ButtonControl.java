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
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

/**
 * Game controller button.
 * 
 * @author Steven T. Ramzel
 * @see ControlView
 */
// Button is not called from xml, so not implementing proper constructor
@SuppressLint("ViewConstructor")
public class ButtonControl extends ControlView {
	// Paint used to printing text on button
	static final Paint textPaint = new Paint();

	// Bitmap used for rendering button
	protected Bitmap buttonBitmap;
	private int buttonIndex; // Button to map to
	// Circle shape for button image generation
	private ShapeDrawable buttonShape = new ShapeDrawable();
	private String label; // Text on button face

	/**
	 * @param context
	 *            Application context.
	 * @param radiusScale
	 *            Radius of button as multiple of auto-generated standard button
	 *            radius.
	 * @param buttonIndex
	 *            Button to map to.
	 * @param gravity
	 *            Placement of button within the screen as per Gravity.
	 * @param xOffset
	 *            X position for this button.
	 * @param yOffset
	 *            Y position for this button.
	 * @param label
	 *            Text on button face.
	 * @param controller 
	 */
	public ButtonControl(Context context, float radiusScale, byte buttonIndex,
			int gravity, float xOffset, float yOffset, String label, Controller controller) {
		super(context, radiusScale, gravity, xOffset, yOffset, controller);
		this.label = label;
		this.buttonIndex = buttonIndex;
		//updateParams();
	}




	@Override
	public void onDraw(Canvas canvas) {

		// If down, draw opaque. Otherwise draw transparent.
		canvas.drawBitmap(buttonBitmap, 0, 0, isDown ? parent.downPaint : parent.upPaint);
		super.onDraw(canvas);
	}
	/**
	 * Buttons dont need to be updated while being pressed. This does nothing
	 * for buttons.
	 */
	@Override
	public void restrictedInvalidate() {
	}

	/**
	 * Generates bitmap used for drawing button.
	 * 
	 * @param radius
	 *            Radius of button in pixels.
	 */
	@Override
	void generateBitmap() {
		super.generateBitmap();
		//Size and shape
		buttonShape.setShape(new OvalShape());
		buttonShape.setBounds(new Rect(PADDING, PADDING, (int) radius * 2 - PADDING, (int) radius * 2 - PADDING));

		//Create button bitmap and render shape
		buttonBitmap = Bitmap.createBitmap((int) radius * 2,(int) radius * 2,
				Bitmap.Config.ARGB_4444);
		Canvas handleCanvas = new Canvas(buttonBitmap);
		buttonShape.getPaint().set(parent.fillPaint);
		buttonShape.draw(handleCanvas);

		//Set text style
		textPaint.setStyle(Paint.Style.FILL);
		textPaint.setTypeface(Typeface.DEFAULT_BOLD);
		textPaint.setTextAlign(Paint.Align.CENTER);
		textPaint.setColor(Color.BLACK);
		//Set size (one character has a standard design, more has dynamic size)
		if (label.length() == 1)
			textPaint.setTextSize(radius);
		else
			textPaint.setTextSize(radius / label.length()*2.5f);
		
		//Set paint to clear text from button
		textPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
		
		//Render the button
		handleCanvas.drawText(label, radius, radius
				+ (textPaint.getTextSize() * .33333f), textPaint);
	}

	/**
	 * Updates button status for next network send.
	 * 
	 * @param screenX
	 *            Not used.
	 * @param screenY
	 *            Not used.
	 */
	@Override
	public void updateStatus(float screenX, float screenY) {
		parent.buttonState.set(buttonIndex, isDown);
	}

	@Override
	void writeXml(XmlSerializer serializer, OutputStream fileos) throws IllegalArgumentException, IllegalStateException, IOException {
        serializer.startTag(null, "button");
        super.writeXml(serializer, fileos);
        //set an attribute called "attribute" with a "value" for <child2>
        serializer.attribute(null, "label", label);
        serializer.attribute(null, "buttonIndex", Integer.toString(buttonIndex));
        serializer.endTag(null, "button");
	}

	@Override
	public void appendProperties(Context context, LinearLayout propertiesLayout) {
		TextView text = new TextView(context);
		text.setText("Button Index:");
		
		Spinner spinner = new Spinner(context);
		ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(context,
		        com.gphrost.Overplayed.R.array.buttonLabel_array, android.R.layout.simple_spinner_item);
		// Specify the layout to use when the list of choices appears
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		// Apply the adapter to the spinner
		spinner.setAdapter(adapter);
		spinner.setSelection(buttonIndex);
		spinner.setOnItemSelectedListener(new OnItemSelectedListener(){
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				buttonIndex = position;
			}
			public void onNothingSelected(AdapterView<?> parent) {
			}
		});
		

		TextView text2 = new TextView(context);
		text2.setText("Label:");
		
		EditText editText = new EditText(context);
		editText.setText(label);
		editText.addTextChangedListener(new TextWatcher(){
			public void afterTextChanged(Editable s) {
				label = s.toString();
				generateBitmap();
				invalidate();
			}
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			}
			public void onTextChanged(CharSequence s, int start, int before, int count) {
			}
		});
		editText.setSingleLine();
		editText.setSelectAllOnFocus(true);
		
		propertiesLayout.addView(text);
		propertiesLayout.addView(spinner);
		propertiesLayout.addView(text2);
		propertiesLayout.addView(editText);
	}
}
