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
import java.util.Set;

public class DeviceDialog extends Activity {

	private static final String TAG = DeviceDialog.class.getSimpleName();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getWindow().setBackgroundDrawable(new ColorDrawable(0));
		setFinishOnTouchOutside(false);

		final SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
		Set<String> devices = pref.getStringSet("devices", Collections.<String>emptySet());
		final String[] names = new String[devices.size()], ids = new String[devices.size()];
		int i = 0;
		for (String device : devices) {
			names[i] = device.substring(0, device.charAt('_'));
			ids[i++] = device.substring(device.charAt('_') + 1);
		}
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