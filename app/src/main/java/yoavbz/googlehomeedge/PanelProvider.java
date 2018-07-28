package yoavbz.googlehomeedge;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.ArraySet;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;
import com.samsung.android.sdk.look.cocktailbar.SlookCocktailManager;
import com.samsung.android.sdk.look.cocktailbar.SlookCocktailProvider;
import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public class PanelProvider extends SlookCocktailProvider {

	private static final String TAG = PanelProvider.class.getSimpleName();

	private static final String ACTION_PLUS = "yoavbz.googlehomeedge.action.ACTION_PLUS";
	private static final String ACTION_MINUS = "yoavbz.googlehomeedge.action.ACTION_MINUS";
	private static final String ACTION_ENABLE = "com.samsung.android.cocktail.action.COCKTAIL_ENABLED";
	private static final String ACTION_REFRESH = "yoavbz.googlehomeedge.action.ACTION_REFRESH";
	private RemoteViews panelRv = null;
	private RemoteViews stateRv = null;
	private OkHttpClient client = new OkHttpClient();
	private static int volume;
	private static boolean panelDisabled = true;

	@Override
	public void onReceive(final Context context, Intent intent) {
		super.onReceive(context, intent);
		String action = intent.getAction();
		Log.d(TAG, "onReceive() : action - " + action);

		switch (action) {
			case ACTION_ENABLE:
				fetchVolume(context);
				break;
			case ACTION_PLUS:
				changeVolume(context, 5);
				break;
			case ACTION_MINUS:
				changeVolume(context, -5);
				break;
			case ACTION_REFRESH:
				fetchVolume(context);
				updateDeviceList(context);
				// Animate button
				break;
		}
	}

	private void updateDeviceList(Context context) {
		final SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
		String ip = pref.getString("ip", null);

		// Fetching current devices names and ids
		final Request request = new Request.Builder().url(ip + "/device").build();
		client.newCall(request).enqueue(new Callback() {
			@Override
			public void onFailure(Call call, IOException e) {
				Log.e(TAG, e.toString());
			}

			@Override
			public void onResponse(Call call, Response response) throws IOException {
				try {
					JSONArray arr = new JSONArray(response.body().string());
					ArraySet<String> devices = new ArraySet<>();
					for (int i = 0; i < arr.length(); i++) {
						JSONObject o = arr.getJSONObject(i);
						devices.add(String.format("%s_%s", o.getString("name"), o.getString("id")));
					}
					// Storing current devices list
					pref.edit().putStringSet("devices", devices).apply();
				} catch (JSONException e) {
					Log.e(TAG, e.toString());
				}
			}
		});
	}

	private void fetchVolume(final Context context) {
		String url = PreferenceManager.getDefaultSharedPreferences(context).getString("ip", null) + "/device";

		final Request request = new Request.Builder().url(url).build();
		client.newCall(request).enqueue(new Callback() {
			@Override
			public void onFailure(Call call, IOException e) {
				Log.d(TAG, "fetchVolume() : exception - " + e.toString());
				panelDisabled = true;
				updatePanelState(context);
			}

			@Override
			public void onResponse(Call call, Response response) {
				try {
					String deviceId = PreferenceManager.getDefaultSharedPreferences(context).getString("device", null);
					JSONArray arr = new JSONArray(response.body().string());
					for (int i = 0; i < arr.length(); i++) {
						if (arr.getJSONObject(i).getString("id").equals(deviceId)) {
							volume = arr.getJSONObject(i).getJSONObject("status").getInt("volume");
							break;
						}
					}
					panelDisabled = false;
					updatePanelState(context);
				} catch (Exception e) {
					Log.d(TAG, "fetchVolume() : exception - " + e.toString());
				}
			}
		});
	}

	private void changeVolume(final Context context, final int amount) {
		//String url = "http://192.168.1.4:3000/device/f141b64894ad1e71268958b5a10b2dde/volume/25";
		String url = PreferenceManager.getDefaultSharedPreferences(context).getString("ip", null);
		if (url == null) {
			panelDisabled = false;
			updatePanelState(context);
			return;
		}
		String id = PreferenceManager.getDefaultSharedPreferences(context).getString("device", null);
		url = String.format("%s/device/%s/volume/%d", url, id, volume);

		Log.d(TAG, "changeVolume() : url - " + url);
		Request request = new Request.Builder().url(url).build();
		client.newCall(request).enqueue(new Callback() {
			@Override
			public void onFailure(Call call, IOException e) {
				Log.d(TAG, e.getMessage());
				panelDisabled = true;
				updatePanelState(context);
			}

			@Override
			public void onResponse(Call call, Response response) {
				// Limit volume to values between 0-100
				volume = Math.min(100, Math.max(0, volume += amount));
				Log.d(TAG, String.format("Added amount: %d, to current volume: %d", volume, amount));
				panelDisabled = false;
				updatePanelState(context);
			}
		});
	}

	@Override
	public void onUpdate(Context context, SlookCocktailManager cocktailManager, int[] cocktailIds) {
		panelRv = (panelRv != null) ? panelRv : createPanelView(context);
		stateRv = (stateRv != null) ? stateRv : createStateView(context);

		cocktailManager.updateCocktail(cocktailIds[0], panelRv);
		cocktailManager.updateCocktail(cocktailIds[0], panelRv, stateRv);
	}

	private RemoteViews createPanelView(Context context) {
		RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.main_panel);
		Intent plusIntent = new Intent(context, getClass());
		plusIntent.setAction(ACTION_PLUS);
		Intent minusIntent = new Intent(context, getClass());
		minusIntent.setAction(ACTION_MINUS);
		Intent dialogIntent = new Intent(context, DeviceDialog.class);
		dialogIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

		PendingIntent plusPendingIntent = PendingIntent.getBroadcast(context, 0, plusIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		PendingIntent minusPendingIntent = PendingIntent.getBroadcast(context, 0, minusIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		PendingIntent dialogPendingIntent = PendingIntent.getActivity(context, 0, dialogIntent, PendingIntent.FLAG_UPDATE_CURRENT);

		rv.setOnClickPendingIntent(R.id.plus, plusPendingIntent);
		rv.setOnClickPendingIntent(R.id.minus, minusPendingIntent);
		rv.setOnClickPendingIntent(R.id.device_select, dialogPendingIntent);
		rv.setImageViewResource(R.id.logo, R.drawable.google_home);

		return rv;
	}

	private RemoteViews createStateView(Context context) {
		RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.state_layout);

		Intent refreshIntent = new Intent(context, DeviceDialog.class);
		refreshIntent.setAction(ACTION_REFRESH);
		PendingIntent refreshPendingIntent = PendingIntent.getActivity(context, 0, refreshIntent, PendingIntent.FLAG_UPDATE_CURRENT);

		rv.setOnClickPendingIntent(R.id.refresh_button, refreshPendingIntent);
		return rv;
	}

	private void updatePanelState(Context context) {
		SlookCocktailManager cocktailManager = SlookCocktailManager.getInstance(context);
		int[] cocktailIds = cocktailManager.getCocktailIds(new ComponentName(context, getClass()));
		if (panelRv == null) {
			panelRv = createPanelView(context);
		}
		if (stateRv == null) {
			stateRv = new RemoteViews(context.getPackageName(), R.layout.state_layout);
		}
		if (panelDisabled) {
			panelRv.setBoolean(R.id.plus, "setEnabled", false);
			panelRv.setBoolean(R.id.minus, "setEnabled", false);
			panelRv.setTextViewText(R.id.volume_title, "No connection.");
			panelRv.setViewVisibility(R.id.volume_text, View.GONE);
		} else {
			panelRv.setBoolean(R.id.plus, "setEnabled", true);
			panelRv.setBoolean(R.id.minus, "setEnabled", true);
			panelRv.setTextViewText(R.id.volume_title, "Volume: ");
			panelRv.setViewVisibility(R.id.volume_text, View.VISIBLE);
			panelRv.setTextViewText(R.id.volume_text, Integer.toString(volume));
		}

		for (int id : cocktailIds) {
			cocktailManager.updateCocktail(id, panelRv);
		}
		if (cocktailIds.length != 0) {
			cocktailManager.updateCocktail(cocktailIds[0], panelRv, stateRv);
		}
	}
}