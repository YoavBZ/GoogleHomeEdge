package yoavbz.googlehomeedge;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.util.Log;

import java.util.Arrays;
import java.util.Collections;

public class DeviceDialog extends Activity {

	private static final String TAG = DeviceDialog.class.getSimpleName();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getWindow().setBackgroundDrawable(new ColorDrawable(0));
		setFinishOnTouchOutside(false);

		final SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
		String[] names = pref.getStringSet("names", Collections.<String>emptySet()).toArray(new String[0]);
		final String[] ids = pref.getStringSet("ids", Collections.<String>emptySet()).toArray(new String[0]);
		Log.d(TAG, "Devices names: " + Arrays.toString(names) + ", ids: " + Arrays.toString(ids));

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Select Device").setItems(names, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				pref.edit().putString("id", ids[which]).apply();
			}
		}).setOnDismissListener(new DialogInterface.OnDismissListener() {
			@Override
			public void onDismiss(DialogInterface dialog) {
				finish();
			}
		}).show();
	}
}