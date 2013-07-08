package org.gphrost.Overplayed.Menu;

import android.content.Context;
import android.util.AttributeSet;

public class MenuButtonLinearLayout extends android.widget.LinearLayout {

	public boolean active = true;

	public MenuButtonLinearLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		if (active) this.setMeasuredDimension(getMeasuredWidth(),
					getMeasuredHeight() / 2);
	}
}