package yoavbz.googlehomeedge;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.*;
import android.support.annotation.Nullable;
import android.util.ArraySet;
import android.util.Log;
import android.widget.Toast;
import com.samsung.android.sdk.SsdkUnsupportedException;
import com.samsung.android.sdk.look.Slook;
import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public class MainActivity extends PreferenceActivity {

	OkHttpClient client = new OkHttpClient();

	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		checkEdgeSupport();

		addPreferencesFromResource(R.xml.preferences);

		final EditTextPreference ip = (EditTextPreference) getPreferenceManager().findPreference("ip");
		final ListPreference listPreference = (ListPreference) getPreferenceManager().findPreference("device");

		// Set action when setting an IP address
		ip.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object o) {
				try {
					// Perform GET call on the entered address
					Log.d(getLocalClassName(), "onPreferenceChange() : object - " + o);
					Request req = new Request.Builder().url((String) o).build();
					client.newCall(req).enqueue(new Callback() {
						@Override
						public void onFailure(Call call, IOException e) {
							Log.e(getLocalClassName(), e.toString());
							MainActivity.this.runOnUiThread(new Runnable() {
								@Override
								public void run() {
									Toast.makeText(getApplicationContext(), "Connection Failed!", Toast.LENGTH_SHORT)
											.show();
									listPreference.setEnabled(false);
									Log.d(getLocalClassName(), "IP address remains: " + ip.getText());
									ip.setText(ip.getText());
								}
							});
						}

						@Override
						public void onResponse(Call call, Response response) {
							try {
								final String[][] updatedContent = updateList();
								MainActivity.this.runOnUiThread(new Runnable() {
									@Override
									public void run() {
										try {
											listPreference.setEntries(updatedContent[0]);
											listPreference.setEntryValues(updatedContent[1]);
											listPreference.setEnabled(true);
											Toast.makeText(getApplicationContext(), "Connection Succeeded", Toast.LENGTH_LONG)
													.show();
										} catch (Exception e) {
											Log.d(getLocalClassName(), e.toString());
											Toast.makeText(getApplicationContext(), "Connection Failed", Toast.LENGTH_LONG)
													.show();
											listPreference.setEnabled(false);
										}
									}
								});
							} catch (Exception e) {
								Log.e(getLocalClassName(), e.toString());
							}
						}
					});
					return true;
				} catch (Exception e) {
					Log.e(getLocalClassName(), e.toString());
				}
				return false;
			}
		});
	}

	private void checkEdgeSupport() {
		Slook slook = new Slook();
		try {
			slook.initialize(this);
			if (!slook.isFeatureEnabled(Slook.COCKTAIL_PANEL)) {
				Toast.makeText(this, getString(R.string.no_support), Toast.LENGTH_LONG).show();
				finish();
			}
		} catch (SsdkUnsupportedException e) {
			Toast.makeText(this, getString(R.string.no_support), Toast.LENGTH_LONG).show();
			finish();
		}
	}

	private String[][] updateList() throws IOException, JSONException {
		// Fetching IP address
		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
		String ip = pref.getString("ip", null);

		// Fetching current devices names and ids
		final Request request = new Request.Builder().url(ip + "/device").build();
		Response res = client.newCall(request).execute();
		JSONArray arr = new JSONArray(res.body().string());

		ArraySet<String> devices = new ArraySet<>();
		String[] names = new String[arr.length()], ids = new String[arr.length()];
		for (int i = 0; i < arr.length(); i++) {
			JSONObject o = arr.getJSONObject(i);
			devices.add(String.format("%s_%s", o.getString("name"), o.getString("id")));
			names[i] = arr.getJSONObject(i).getString("name");
			ids[i] = arr.getJSONObject(i).getString("id");
		}
		// Storing current devices list
		pref.edit().putStringSet("devices", devices).apply();
		return new String[][]{names, ids};
	}
}