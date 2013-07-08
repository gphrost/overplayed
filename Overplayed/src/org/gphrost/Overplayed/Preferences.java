package org.gphrost.Overplayed;

import org.json.JSONArray;

import com.gphrost.Overplayed.R;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

public class Preferences {

	static final String HOST_HIST_PREFS_NAME = "HostHistory";

	static SharedPreferences getPrefs(Context context) {
		return context.getSharedPreferences(HOST_HIST_PREFS_NAME, 0);
	}

	public static class Load {
		static String hostname(Context context) {
			return getPrefs(context).getString("lastHost", "");
		}

		static String port(Context context) {
			return getPrefs(context).getString("lastPort", context.getResources().getString(R.string.port_default));
		}

		public static float alpha(Context context) {
			return getPrefs(context).getFloat("alpha", .5f);
		}

		public static float scale(Context context) {
			return getPrefs(context).getFloat("scale", 1f);
		}

		public static String defaultController(Context context) {
			return getPrefs(context).getString("defaultController", "default");
		}

		public static boolean haptic(Context context) {
			return getPrefs(context).getBoolean("haptic", true);
		}

		static void boundButtons(Context context, Integer[] boundButtons) {
			getArrayPref(context, "boundButtons", boundButtons, Integer.class);
		}

		static void boundAxis(Context context, Integer[] boundAxis) {
			getArrayPref(context, "boundAxis", boundAxis, Integer.class);
		}
		
		public static <T> void getArrayPref(Context context, String key, T[] values, Class<T> type) {
			String json = context.getSharedPreferences(HOST_HIST_PREFS_NAME, 0).getString(key, null);
			if (json != null) {
				try {
					JSONArray a = new JSONArray(json);
					for (int i = 0; i < a.length(); i++) {
						if (type == Integer.class) {
							Integer[] values2 = ((Integer[]) values);
							values2[i] = (Integer) a.optInt(i, (Integer) values[i]);
						} else if (type == Double.class) {
							Double[] values2 = ((Double[]) values);
							values2[i] = (Double) a.optDouble(i, (Double) values[i]);
						} else if (type == String.class) {
							String[] values2 = ((String[]) values);
							values2[i] = (String) a.optString(i, (String) values[i]);
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

	public static class Save {
		public static void alpha(Context context, float alpha) {
			getPrefs(context).edit().putFloat("alpha", alpha).commit();
		}

		public static void scale(Context context, float scale) {
			getPrefs(context).edit().putFloat("scale", scale).commit();
		}

		static void hostname(Context context, String addressString) {
			getPrefs(context).edit().putString("lastHost", addressString).commit();
		}

		static void port(Context context, String portString) {
			getPrefs(context).edit().putString("lastPort", portString).commit();
		}

		public static void defaultController(Context context, String defaultString) {
			getPrefs(context).edit().putString("defaultController", defaultString).commit();
		}

		public static void haptic(Context context, boolean haptic) {
			getPrefs(context).edit().putBoolean("haptic", haptic).commit();
		}
		
		static void boundButtons(Context context, Integer[] boundButtons) {
			setStringArrayPref(context, "boundButtons", boundButtons);
		}

		static void boundAxis(Context context, Integer[] boundAxis) {
			setStringArrayPref(context, "boundButtons", boundAxis);
		}
		
		public static <T> void setStringArrayPref(Context context, String key, T[] values) {
			JSONArray a = new JSONArray();
			Editor editor = context.getSharedPreferences(HOST_HIST_PREFS_NAME, 0).edit();
			for (T value : values)
				a.put(value);
			if (values.length > 0)
				editor.putString(key, a.toString());
			else
				editor.putString(key, null);
			editor.commit();
		}
	}
}
