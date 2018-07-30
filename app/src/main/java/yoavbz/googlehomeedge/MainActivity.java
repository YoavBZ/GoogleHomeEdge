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

		final EditTextPreference url = (EditTextPreference) getPreferenceManager().findPreference("url");
		final String lastIp = url.getText();
		final ListPreference listPreference = (ListPreference) getPreferenceManager().findPreference("deviceId");

		// Set action when setting URL
		url.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object o) {
				try {
					Log.d(getLocalClassName(), "onPreferenceChange() : object - " + o);
					// Perform GET call on the entered address to verify server is up
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
									Log.d(getLocalClassName(), "URL remains: " + url.getText());
									url.setText(lastIp);
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

		// Updating listPreference onPreferenceChange listener to save selected entry (device name), since the default
		// listener saves only the selected entry value
		listPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(Preference preference, Object newValue) {
				PreferenceManager.getDefaultSharedPreferences(getApplicationContext())
						.edit()
						.putString("deviceName", (String) listPreference.getEntries()[listPreference.findIndexOfValue((String) newValue)])
						.apply();
				return true;
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
		// Fetching URL
		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
		String url = pref.getString("url", null);

		// Fetching current devices names and ids
		final Request request = new Request.Builder().url(url + "/device").build();
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