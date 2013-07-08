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

import org.gphrost.Overplayed.Overplayed;
import org.gphrost.Overplayed.Preferences;
import org.xmlpull.v1.XmlSerializer;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RectShape;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.FloatMath;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.LinearLayout;

/**
 * Game controller parent class.
 * 
 * @author Steven T. Ramzel
 */
public abstract class ControlView extends View {
	Controller parent;

	protected static final int PADDING = 2; // Padding for drawing circles
	static Rect display = new Rect();
	public static long refresh = 1000; // Last network ping
	static float standardRadius; // Radius used for proper sizing and

	/**
	 * Function of a circle
	 * 
	 * @param radius
	 *            Radius of the circle
	 * @param x
	 *            X input to function
	 * @return Y coordinate of circle
	 */
	@SuppressLint("FloatMath")
	public static float circleY(float radius, float x) {
		return FloatMath.sqrt(radius * radius - x * x);
	}

	/**
	 * @param x
	 *            X coordinate in pixels with respect to the center of the
	 *            button
	 * @param y
	 *            Y coordinate in pixels with respect to the center of the
	 *            button
	 * @return
	 */
	public static boolean inRadius(float radius, float x, float y) {
		float xWidth = ControlView.circleY(radius, y);
		float yWidth = ControlView.circleY(radius, x);
		return (((x < xWidth) && (x > -xWidth) && ((y < yWidth)) && (y > -yWidth)));
	}

	protected boolean isDown = false; // State of control
	// WindowManager
	private long nextRenderTime; // Next time to render
	public LayoutParams params; // Parameters used to attach view to
	// pressed by the same finger as another
	// button
	private int pointerID = -1; // Current index of MotionEvent
								// screen
	protected float radius; // Radius of button in pixels
	public float radiusScale; // Radius of button in multiples of
								// standardRadius with respect to gravity
	private boolean secondary = false; // Flag whether or not this is a button

	public int xOffset; // X position of control with respect of entire

	// standardRadius
	public float xOffsetScale; // X position of control as multiples of

	// screen
	public int yOffset; // Y position of control with respect of entire

	// standardRadius with respect to gravity
	public float yOffsetScale; // Y position of control as multiples of

	private MODE customizeMode = MODE.MOVING;

	enum MODE {
		MOVING, SELECTING, SCALING
	};

	protected Bitmap handlesBitmap;

	private static float scaleOffsetRatio;

	private static float anchorX;
	private static float anchorY;

	private static boolean scaleHandleFarS;
	private static boolean scaleHandleFarE;

	/**
	 * @param context
	 *            The Context the view is running in, through which it can
	 *            access the current theme, resources, etc.
	 * @param radiusScale
	 *            Radius of button as multiple of auto-generated standard button
	 *            radius.
	 * @param gravity
	 *            Placement of button within the screen as per Gravity.
	 * @param xOffset
	 *            X position for this button.
	 * @param yOffset
	 *            Y position for this button.
	 * @param controller
	 */
	public ControlView(Context context, float radiusScale, int gravity, float xOffset, float yOffset, Controller controller) {
		super(context);
		parent = controller;
		this.radiusScale = radiusScale;
		params = new WindowManager.LayoutParams(android.view.ViewGroup.LayoutParams.WRAP_CONTENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
		// Go on top of everything, I would do TYPE_SYSTEM_ALERT but it
		// messes with the notification bar.
				WindowManager.LayoutParams.TYPE_SYSTEM_ERROR,
				// Let touch events pass to other apps
				WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
				// Let the background of controls be transparent
				PixelFormat.TRANSLUCENT);
		params.gravity = gravity;
		xOffsetScale = xOffset;
		yOffsetScale = yOffset;
		// updateParams();
		// generateBitmap((int) radius);
	}

	/**
	 * Sets the control as down.
	 * 
	 * @param view
	 *            The control to set as down.
	 */
	void down(MotionEvent event, ControlView view, int p, int pID) {
		// Haptick feedback
		if (parent.haptic) performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
		view.pointerID = pID;
		view.isDown = true;
		view.updateStatus(event.getX(p) + xOffset, event.getY(p) + yOffset);
		view.invalidate();
	}

	/**
	 * Put the MotionEvent coordinates in respect to the center of the control
	 * and check if in button radius with inRadius()
	 * 
	 * @param control
	 *            The control to check
	 * @param event
	 *            The MotionEvent to use
	 * @param p
	 *            This pointer index of the coordinates in the MotionEvent to be
	 *            checked
	 * @return true if the pointer is within control's radius
	 */
	public boolean inBounds(ControlView control, MotionEvent event, int p) {
		return inRadius(control.radius, event.getX(p) + xOffset - control.xOffset - control.radius, event.getY(p) + yOffset - control.yOffset - control.radius);
	}

	@Override
	public void invalidate() {
		super.invalidate();
		if (parent.thread != null)
			parent.thread.changed = true;
	}

	@Override
	protected void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		// standardRadius may need to be recalculated
		// Update layout params with new standardRadius
		updateParams();

	}

	@Override
	public boolean dispatchKeyEvent(KeyEvent event) {
		int keyCode = event.getKeyCode();
		// Keyboard buttons 1-9 are bound the to their respective button indices
		for (int i = 0; i < Overplayed.boundButtons.length; i++) {
			if (keyCode == Overplayed.boundButtons[i]) {
				parent.buttonState.set(i, event.getAction() == KeyEvent.ACTION_DOWN);
				parent.thread.changed = true;
				return true;
			}
		}
		return super.dispatchKeyEvent(event);
	}

	@Override
	protected void onAttachedToWindow() {
		super.onAttachedToWindow();
		updateParams();
	}

	protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
		super.onLayout(changed, left, top, right, bottom);
		// updateParams();
		updateOffset();
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (parent.customize)
			handleCustomizerTouch(event);
		else {
			int action = event.getActionMasked();
			if (action == MotionEvent.ACTION_DOWN) {
				// An untouched control view is now touched
				// Check if it's within the controls radius (event.getX() and
				// getY()
				// is in respect to control view, so just subtract the radius so
				// coordinates are in respect to axis origin
				if (inRadius(radius, event.getX() - radius, event.getY() - radius)) {
					// A little haptick feedback
					if (parent.haptic) performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
					pointerID = event.getPointerId(0);// Set the pointer ID
					isDown = true;// Update button state
					updateStatus(event.getX() + xOffset, event.getY() + yOffset);
					// Immediately invalidate
					invalidate();
				}
			} else {
				for (ControlView view : parent.controls) {
					// We don't know which control the touch has interacted with
					// so
					// check them all
					if (action < 4) {
						// The action is either ACTION_MOVE or ACTION_UP
						// These actions can be analyzed by using pointer IDs
						// less
						// than getPointerCount()
						for (int p = 0; p < event.getPointerCount(); p++) {
							// Get the global pointer ID for comparison with the
							// control's saved index
							int pID = event.getPointerId(p);
							if (action == MotionEvent.ACTION_MOVE) {
								// An already down pointer has moved
								if (view.pointerID == pID) {
									// The pointer belongs to this control
									if (view.secondary && !inBounds(view, event, p))
										// This is a button pressed by a pointer
										// that started its touch from another
										// button, but the pointer has moved
										// from
										// this button's radius so change its
										// state
										// to not down
										up(view);
									else {
										// This is a pointer thats already
										// pressing
										// a control and has moved so update its
										// status
										view.updateStatus(event.getX(p) + xOffset, event.getY(p) + yOffset);
										// Update when reasonable
										view.restrictedInvalidate();
									}
								} else
									for (ControlView testView : parent.controls)
										if (testView.pointerID == pID && testView.getClass() == ButtonControl.class && view.getClass() == ButtonControl.class
												&& !view.isDown && inBounds(view, event, p)) {
											// This pointer has moved from the
											// calling button-view to this
											// button,
											// so we can set this to down and
											// flag
											// it as a secondary press
											down(event, view, p, pID);
											view.secondary = true;
										}
							} else if (action == MotionEvent.ACTION_UP && view.pointerID == pID)
								// The pointer attached to this control has been
								// lifted, so update accordingly
								up(view);
						}
					} else { // This is a multi-touch action
						// Multi-touch actions have the index given by
						// getActionIndex()
						int p = event.getActionIndex();
						// Get the global pointer ID for comparison with
						// control's
						// saved index
						int pID = event.getPointerId(p);

						if (action == MotionEvent.ACTION_POINTER_DOWN && inBounds(view, event, p))
							// An untouched control view is now touched
							down(event, view, p, pID);
						else if (action == MotionEvent.ACTION_POINTER_UP && view.pointerID == pID)
							// The pointer attached to this control has been
							// lifted,
							// so update accordingly
							up(view);
					}
				}
			}
		}
		return true;
	}

	private boolean handleCustomizerTouch(MotionEvent event) {
		int action = event.getActionMasked();
		if (action == MotionEvent.ACTION_DOWN) {
			if (parent.activeControl != this) {
				ControlView lastControl = parent.activeControl;
				parent.activeControl = this;
				invalidate();
				if (lastControl != null)
					lastControl.invalidate();
				customizeMode = MODE.SELECTING;
			} else {
				// An untouched control view is now touched
				// Check if it's within the controls radius (event.getX() and
				// getY()
				// is in respect to control view, so just subtract the radius so
				// coordinates are in respect to axis origin
				float xypoint = (Math.abs(event.getX() - radius) + Math.abs(event.getY() - radius)) / 2f;
				if (xypoint * 1.414213562373095f > radius) {
					scaleHandleFarS = (event.getY() > radius);
					scaleHandleFarE = (event.getX() > radius);
					scaleOffsetRatio = (radius * 1.707106781186548f) / (xypoint + radius);

					anchorX = params.width;
					anchorY = params.height;
					if ((params.gravity & Gravity.LEFT) != Gravity.LEFT && (params.gravity & Gravity.RIGHT) != Gravity.RIGHT) {
						anchorX /= 2;
						if (scaleHandleFarE)
							anchorX = -anchorX;
					}
					if ((params.gravity & Gravity.TOP) != Gravity.TOP && (params.gravity & Gravity.BOTTOM) != Gravity.BOTTOM) {
						anchorY /= 2;
						if (scaleHandleFarS)
							anchorY = -anchorY;
					}
					anchorX += params.x;
					anchorY += params.y;
					customizeMode = MODE.SCALING;
				} else {
					anchorX = event.getX();
					anchorY = event.getY();
					customizeMode = MODE.MOVING;
				}
			}
		} else if (customizeMode != MODE.SELECTING) {
			if (action == MotionEvent.ACTION_MOVE && this == parent.activeControl) {
				if (customizeMode == MODE.MOVING) {
					getWindowVisibleDisplayFrame(display);
					int newx = (int) (event.getX() - anchorX + xOffset);
					int newy = (int) (event.getY() - anchorY + yOffset);

					if ((params.gravity & Gravity.CENTER) == Gravity.CENTER) {
						params.x = (int) (newx + radius - (display.right - display.left)/2);
						params.y = (int) (newy + radius - (display.bottom - display.top)/2);
					}
					
					if ((params.gravity & Gravity.LEFT) == Gravity.LEFT)
						params.x = newx;
					else if ((params.gravity & Gravity.RIGHT) == Gravity.RIGHT)
						params.x = (int) (display.right - display.left - newx - 2 * radius);

					if ((params.gravity & Gravity.TOP) == Gravity.TOP)
						params.y = newy;
					else if ((params.gravity & Gravity.BOTTOM) == Gravity.BOTTOM)
						params.y = (int) (display.bottom - display.top - newy - 2 * radius);

					if (parent.lockToGrid) {
						params.x -= params.x % (standardRadius / 2);
						params.y -= params.y % (standardRadius / 2);
					}

					xOffsetScale = params.x / standardRadius;
					yOffsetScale = params.y / standardRadius;
					// updateParams();
					updateOffset();
					layout(xOffset, yOffset, xOffset + params.height, yOffset + params.width);
				} else {
					float xOff = event.getX() - radius;
					float yOff = event.getY() - radius;
					if (!scaleHandleFarE)
						xOff = -xOff;
					if (!scaleHandleFarS)
						yOff = -yOff;
					float xypoint = ((xOff + yOff) / 2f + radius) * 0.585786437626905f;
					float newRadiusScale = xypoint * scaleOffsetRatio / standardRadius;

					if (parent.lockToGrid)
						newRadiusScale -= newRadiusScale % .25;

					if (newRadiusScale >= 0.5f)
						radiusScale = newRadiusScale;
					updateParams();

					float changeX = params.x - anchorX;
					float changeY = params.y - anchorY;
					if (scaleHandleFarE) {
						if ((params.gravity & Gravity.RIGHT) == Gravity.RIGHT)
							params.x -= changeX + params.width;
						else if ((params.gravity & Gravity.LEFT) != Gravity.LEFT)
							params.x -= changeX - params.width / 2;
					} else if ((params.gravity & Gravity.LEFT) == Gravity.LEFT)
						params.x -= changeX + params.width;
					else if ((params.gravity & Gravity.RIGHT) != Gravity.RIGHT)
						params.x -= changeX + params.width / 2;

					if (scaleHandleFarS) {
						if ((params.gravity & Gravity.BOTTOM) == Gravity.BOTTOM)
							params.y -= changeY + params.height;
						else if ((params.gravity & Gravity.TOP) != Gravity.TOP)
							params.y -= changeY - params.height / 2;
					} else if ((params.gravity & Gravity.TOP) == Gravity.TOP)
						params.y -= changeY + params.height;
					else if ((params.gravity & Gravity.BOTTOM) != Gravity.BOTTOM)
						params.y -= changeY + params.height / 2;

					xOffsetScale = params.x / standardRadius;
					yOffsetScale = params.y / standardRadius;
					updateOffset();
					layout(xOffset, yOffset, xOffset + params.height, yOffset + params.width);
				}
			}
		}
		return true;
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
	@Override
	public boolean dispatchGenericMotionEvent(MotionEvent event) {
		try {
			handleGameMotionEvent(event);
			return true;
		} catch (NoSuchMethodError e) {
		}
		return super.dispatchGenericMotionEvent(event);
	}

	/**
	 * Invalidate only if the amount of time equal to the network ping has
	 * elapsed. Overloaded by Buttons class, which do not redraw during being
	 * pressed.
	 */
	public void restrictedInvalidate() {
		if (System.currentTimeMillis() > nextRenderTime) {
			invalidate();
			nextRenderTime = System.currentTimeMillis() + refresh;
		}
	}

	/**
	 * Sets the control as not down.
	 * 
	 * @param view
	 *            The control to set as not down.
	 */
	void up(ControlView view) {
		// Haptick feedback when the pointer is lifted
		if (parent.haptic) performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
		view.pointerID = -1;
		view.isDown = false;
		view.secondary = false;
		// Set the input to the center of the control (for d-pads and joysticks)
		view.updateStatus(view.radius + view.xOffset, view.radius + view.yOffset);
		view.invalidate();
	}

	/**
	 * Generates bitmaps used for drawing joystick.
	 */
	void generateBitmap() {
		// Size and shape of joystick gate
		ShapeDrawable squareShape = new ShapeDrawable();
		squareShape.setShape(new RectShape());
		squareShape.setBounds(0, 0, (int) radius * 2, (int) radius * 2);

		// Create joystick gate bitmap and render shape
		handlesBitmap = Bitmap.createBitmap((int) radius * 2, (int) radius * 2, Bitmap.Config.ARGB_4444);
		Canvas handlesCanvas = new Canvas(handlesBitmap);
		squareShape.getPaint().set(parent.blackPaint);
		squareShape.draw(handlesCanvas);
		parent.strokePaint.setStrokeWidth(standardRadius * .1f);
		squareShape.getPaint().set(parent.strokePaint);
		squareShape.draw(handlesCanvas);
		handlesCanvas.rotate(45, radius, radius);
		squareShape.getPaint().set(parent.clearPaint);
		squareShape.draw(handlesCanvas);
		parent.strokePaint.setStrokeWidth(standardRadius * .05f);
		squareShape.getPaint().set(parent.strokePaint);
		squareShape.draw(handlesCanvas);
	}

	/**
	 * Calculates xOffset and yOffset based on gravity and coordinates from
	 * layout params
	 */
	public void updateOffset() {
		getWindowVisibleDisplayFrame(display);
		if ((params.gravity & Gravity.CENTER) == Gravity.CENTER) {
			xOffset = (display.right - display.left - params.width) / 2 + params.x;
			yOffset = (display.bottom - params.height) / 2 + params.y;
		}

		if ((params.gravity & Gravity.LEFT) == Gravity.LEFT)
			xOffset = display.left + params.x;
		else if ((params.gravity & Gravity.RIGHT) == Gravity.RIGHT)
			xOffset = display.right - params.width - params.x;

		if ((params.gravity & Gravity.TOP) == Gravity.TOP)
			yOffset = params.y;
		else if ((params.gravity & Gravity.BOTTOM) == Gravity.BOTTOM) {
			yOffset = display.bottom - display.top - params.height - params.y;
		}
		
		xOffset = Math.max(Math.min(xOffset, display.right - display.left - params.width), 0);
		yOffset = Math.max(Math.min(yOffset, display.bottom - display.top - params.height), 0);
	}

	public static int getStandardRadius(DisplayMetrics metrics) {
		// The standardRadius is smallest of either 1/7th of an inch,
		// 1/14th the screen height , or 1/20th the screen width
		// This is so everything fits on the screen
		return (int) Math.min(Math.min(metrics.xdpi / 7f, metrics.widthPixels / 20f), Math.min(metrics.ydpi / 7f, metrics.heightPixels / 14f));
	}

	/**
	 * Updates the standard radius so buttons are scaled to appropriately fit on
	 * screen
	 */
	public void updateParams() {
		DisplayMetrics metrics = getResources().getDisplayMetrics();
		standardRadius = (int) (Preferences.Load.scale(this.getContext()) * getStandardRadius(metrics));
		this.radius = radiusScale * standardRadius;
		params.x = (int) (xOffsetScale * standardRadius);// +.5 to round
		params.y = (int) (yOffsetScale * standardRadius);

		params.height = params.width = (int) (2f * radiusScale * standardRadius);
		try {
			// invalidate(); // Imediately redraw the control
			// Update rendering bitmap with new standardRadius
			generateBitmap();
			parent.wm.updateViewLayout(this, params);
			// Update the control layout

		} catch (Exception e) {

		}
		try {
			// invalidate(); // Imediately redraw the control
			// Update rendering bitmap with new standardRadius
			generateBitmap();
			parent.wm.updateViewLayout(this, params);
			// Update the control layout

		} catch (Exception e) {

		}
	}

	/**
	 * Updates control status for next network send.
	 * 
	 * @param screenX
	 *            X coordinate of touch with respect to entire screen.
	 * @param screenY
	 *            Y coordinate of touch with respect to entire screen.
	 */
	abstract void updateStatus(float x, float y);

	@TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
	public void handleGameMotionEvent(MotionEvent event) {
		if (event.getDevice() != null) {
			int historySize = event.getHistorySize();
			for (int i = 0; i < historySize; i++) {
				processJoystickInput(event.getDevice(), event, i);
			}
			processJoystickInput(event.getDevice(), event, -1);
		}
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
	private void processJoystickInput(InputDevice device, MotionEvent event, int historyPos) {

		for (int i = 0; i < Overplayed.boundAxis.length; i++) {
			InputDevice.MotionRange range = device.getMotionRange(Overplayed.boundAxis[i], event.getSource());
			if (range != null) {
				float axisValue;
				if (historyPos >= 0) {
					axisValue = event.getHistoricalAxisValue(Overplayed.boundAxis[i], historyPos);
				} else {
					axisValue = event.getAxisValue(Overplayed.boundAxis[i]);
				}
				parent.analogState.set(i, (short) ((processAxis(range, axisValue) + 1f) * .5f * Short.MAX_VALUE));
			}
		}
		parent.thread.changed = true;
	}

	@TargetApi(Build.VERSION_CODES.GINGERBREAD)
	public static float processAxis(InputDevice.MotionRange range, float axisvalue) {
		float absaxisvalue = Math.abs(axisvalue);
		float deadzone = range.getFlat();
		if (absaxisvalue <= deadzone) {
			return 0.0f;
		}
		float normalizedvalue;
		if (axisvalue < 0.0f)
			normalizedvalue = absaxisvalue / range.getMin();
		else
			normalizedvalue = absaxisvalue / range.getMax();
		return normalizedvalue;
	}

	@Override
	public void onDraw(Canvas canvas) {
		// if (parent.customize) canvas.drawBitmap(outlineBitmap, 0, 0,
		// parent.halfPaint);
		if (parent.customize)
			canvas.drawBitmap(handlesBitmap, 0, 0, (parent.activeControl != null) && (parent.activeControl == this) ? parent.blackPaint : parent.halfPaint);
	}

	void writeXml(XmlSerializer serializer, OutputStream fileos) throws IllegalArgumentException, IllegalStateException, IOException {
		// set an attribute called "attribute" with a "value" for <child2>
		serializer.attribute(null, "radiusScale", Float.toString(radiusScale));
		serializer.attribute(null, "gravity", Integer.toString(params.gravity));
		serializer.attribute(null, "xOffset", Float.toString(xOffsetScale));
		serializer.attribute(null, "yOffset", Float.toString(yOffsetScale));
	}

	public abstract void appendProperties(Context context, LinearLayout propertiesLayout);
}