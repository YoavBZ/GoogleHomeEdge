package yoavbz.googlehomeedge;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.*;
import android.support.annotation.Nullable;
import android.util.ArraySet;
import android.util.Log;
import android.widget.Toast;
import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;

public class SettingsActivity extends PreferenceActivity {

	OkHttpClient client = new OkHttpClient();

	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
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
							SettingsActivity.this.runOnUiThread(new Runnable() {
								@Override
								public void run() {
									Toast.makeText(getApplicationContext(), "Connection Failed!", Toast.LENGTH_SHORT)
											.show();
									listPreference.setEnabled(false);
									ip.setText(ip.getText());
								}
							});
						}

						@Override
						public void onResponse(Call call, Response response) {
							try {
								final String[][] updatedContent = updateList();
								SettingsActivity.this.runOnUiThread(new Runnable() {
									@Override
									public void run() {
										try {
											listPreference.setEntries(updatedContent[0]);
											listPreference.setEntryValues(updatedContent[1]);
										} catch (Exception e) {
											Log.d(getLocalClassName(), e.toString());
											Toast.makeText(getApplicationContext(), "Connection Failed", Toast.LENGTH_LONG)
													.show();
											listPreference.setEnabled(false);
											return;
										}
										listPreference.setEnabled(true);
										Toast.makeText(getApplicationContext(), "Connection Succeeded", Toast.LENGTH_LONG)
												.show();
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

	private String[][] updateList() throws IOException, JSONException {
		SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
		String ip = pref.getString("ip", null);
		ArraySet<String> names = new ArraySet<>(), ids = new ArraySet<>();

		final Request request = new Request.Builder().url(ip + "/device").build();
		Response res = client.newCall(request).execute();
		JSONArray arr = new JSONArray(res.body().string());
		for (int i = 0; i < arr.length(); i++) {
			names.add(arr.getJSONObject(i).getString("name"));
			ids.add(arr.getJSONObject(i).getString("id"));
		}
		 pref.edit().putStringSet("names", names).putStringSet("ids", ids).apply();
		return new String[][]{names.toArray(new String[0]), ids.toArray(new String[0])};
	}
}