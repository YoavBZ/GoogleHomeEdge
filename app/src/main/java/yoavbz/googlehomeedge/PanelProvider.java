package yoavbz.googlehomeedge;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;
import com.samsung.android.sdk.look.cocktailbar.SlookCocktailManager;
import com.samsung.android.sdk.look.cocktailbar.SlookCocktailProvider;
import su.litvak.chromecast.api.v2.ChromeCast;

public class PanelProvider extends SlookCocktailProvider {

	static final String ACTION_REFRESH = "yoavbz.googlehomeedge.action.ACTION_REFRESH";
	private static final String TAG = PanelProvider.class.getSimpleName();
	private static final String ACTION_PLUS = "yoavbz.googlehomeedge.action.ACTION_PLUS";
	private static final String ACTION_MINUS = "yoavbz.googlehomeedge.action.ACTION_MINUS";
	private static final String ACTION_VOLUME_DIALOG = "yoavbz.googlehomeedge.action.ACTION_VOLUME_DIALOG";
	private static boolean panelDisabled = true;
	private RemoteViews panelRv = null;
	private RemoteViews stateRv = null;
	private ChromeCast chromeCast;

	@Override
	public void onEnabled(Context context) {
		super.onEnabled(context);
		updatePanelState(context);
		fetchVolume(context);
	}

	@Override
	public void onReceive(final Context context, Intent intent) {
		super.onReceive(context, intent);
		String action = intent.getAction();
		Log.d(TAG, "onReceive() : action - " + action);

		switch (action) {
			case ACTION_PLUS:
				changeVolume(context, 0.05f);
				break;
			case ACTION_MINUS:
				changeVolume(context, -0.05f);
				break;
			case ACTION_VOLUME_DIALOG:
				Intent volumeIntent = new Intent(context, VolumeDialog.class);
				SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
				float volume = pref.getFloat("volume", 0.1f);
				volumeIntent.putExtra("volume", (int) (volume * 100));
				context.startActivity(volumeIntent);
				break;
			case ACTION_REFRESH:
				updatePanelState(context);
				animateRefresh(context, true);
				fetchVolume(context);
		}
	}

	private void animateRefresh(Context context, boolean start) {
		stateRv.setViewVisibility(R.id.refresh_button, start ? View.GONE : View.VISIBLE);
		stateRv.setViewVisibility(R.id.progress_bar, start ? View.VISIBLE : View.GONE);
		SlookCocktailManager manager = SlookCocktailManager.getInstance(context);
		int[] ids = manager.getCocktailIds(new ComponentName(context, PanelProvider.class));
		manager.updateCocktail(ids[0], panelRv, stateRv);
	}

	private void fetchVolume(final Context context) {
		new Thread(() -> {
			try {
				String ip = PreferenceManager.getDefaultSharedPreferences(context).getString("ip", null);
				if (chromeCast == null) {
					chromeCast = new ChromeCast(ip);
				}
				Log.d(TAG, "Connecting to Chromecast address: " + chromeCast.getAddress());
				chromeCast.connect();
				float volume = chromeCast.getStatus().volume.level;
				SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
				pref.edit().putFloat("volume", volume).apply();
				panelDisabled = false;
				updatePanelState(context);
				animateRefresh(context, false);
//					Log.d(TAG, "Disconnecting from Chromecast device: " + chromeCast);
//					chromeCast.disconnect();
			} catch (Exception e) {
				Log.e(TAG, "fetchVolume() : exception - " + e.toString());
				panelDisabled = true;
				updatePanelState(context);
				animateRefresh(context, false);
			}
		}).start();
	}

	private void changeVolume(final Context context, final float amount) {
		new Thread(() -> {
			SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
			float volume = pref.getFloat("volume", 0.1f);
			Log.d(TAG, "changeVolume: Current volume " + volume + ", adding " + amount);
			volume = Math.min(100, Math.max(0, volume += amount));
			pref.edit().putFloat("volume", volume).apply();
			String ip = PreferenceManager.getDefaultSharedPreferences(context).getString("ip", null);
			try {
				if (chromeCast == null) {
					chromeCast = new ChromeCast(ip);
				}
				chromeCast.connect();
				chromeCast.setVolume(volume);
				Log.d(TAG, String.format("Added amount: %.2f, to current volume: %.2f", amount, volume));
				panelDisabled = false;
				updatePanelState(context);
//					chromeCast.disconnect();
			} catch (Exception e) {
				Log.e(TAG, "Got an exception " + e);
				volume -= amount;
				pref.edit().putFloat("volume", volume).apply();
				panelDisabled = true;
				updatePanelState(context);
			}
		}).start();
	}

	@Override
	public void onUpdate(Context context, SlookCocktailManager cocktailManager, int[] cocktailIds) {
		panelRv = (panelRv != null) ? panelRv : createPanelView(context);
		stateRv = (stateRv != null) ? stateRv : createStateView(context);

		cocktailManager.updateCocktail(cocktailIds[0], panelRv, stateRv);
	}

	private RemoteViews createPanelView(Context context) {
		RemoteViews panelView = new RemoteViews(context.getPackageName(), R.layout.main_panel);

		Intent plusIntent = new Intent(context, PanelProvider.class);
		plusIntent.setAction(ACTION_PLUS);
		Intent minusIntent = new Intent(context, PanelProvider.class);
		minusIntent.setAction(ACTION_MINUS);
		Intent volumeInputIntent = new Intent(context, PanelProvider.class);
		volumeInputIntent.setAction(ACTION_VOLUME_DIALOG);
		Intent dialogIntent = new Intent(context, DeviceDialog.class);
		dialogIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

		PendingIntent plusPendingIntent = PendingIntent.getBroadcast(context, 0, plusIntent,
		                                                             PendingIntent.FLAG_UPDATE_CURRENT);
		PendingIntent minusPendingIntent = PendingIntent.getBroadcast(context, 0, minusIntent,
		                                                              PendingIntent.FLAG_UPDATE_CURRENT);
		PendingIntent volumeInputPendingIntent = PendingIntent.getBroadcast(context, 0, volumeInputIntent,
		                                                                    PendingIntent.FLAG_UPDATE_CURRENT);
		PendingIntent dialogPendingIntent = PendingIntent.getActivity(context, 0, dialogIntent,
		                                                              PendingIntent.FLAG_UPDATE_CURRENT);

		panelView.setOnClickPendingIntent(R.id.plus, plusPendingIntent);
		panelView.setOnClickPendingIntent(R.id.minus, minusPendingIntent);
		panelView.setOnClickPendingIntent(R.id.volume_text, volumeInputPendingIntent);
		panelView.setOnClickPendingIntent(R.id.device_select, dialogPendingIntent);
		panelView.setImageViewResource(R.id.logo, R.drawable.google_home);

		return panelView;
	}

	private RemoteViews createStateView(Context context) {
		RemoteViews stateView = new RemoteViews(context.getPackageName(), R.layout.state_layout);
		Intent refreshIntent = new Intent(context, PanelProvider.class);
		refreshIntent.setAction(ACTION_REFRESH);
		PendingIntent refreshPendingIntent = PendingIntent.getBroadcast(context, 0, refreshIntent,
		                                                                PendingIntent.FLAG_UPDATE_CURRENT);
		stateView.setOnClickPendingIntent(R.id.refresh_button, refreshPendingIntent);
		return stateView;
	}

	private void updatePanelState(Context context) {
		SlookCocktailManager cocktailManager = SlookCocktailManager.getInstance(context);
		int[] cocktailIds = cocktailManager.getCocktailIds(new ComponentName(context, PanelProvider.class));
		panelRv = (panelRv != null) ? panelRv : createPanelView(context);
		stateRv = (stateRv != null) ? stateRv : createStateView(context);
		if (panelDisabled) {
			panelRv.setBoolean(R.id.plus, "setEnabled", false);
			panelRv.setBoolean(R.id.minus, "setEnabled", false);
			panelRv.setTextViewText(R.id.volume_title, "No connection.");
			panelRv.setViewVisibility(R.id.volume_text, View.GONE);
			panelRv.setViewVisibility(R.id.device_select, View.GONE);
		} else {
			panelRv.setBoolean(R.id.plus, "setEnabled", true);
			panelRv.setBoolean(R.id.minus, "setEnabled", true);
			panelRv.setTextViewText(R.id.volume_title, "Volume: ");
			panelRv.setViewVisibility(R.id.volume_text, View.VISIBLE);
			SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
			float volume = pref.getFloat("volume", 0.1f);
			panelRv.setTextViewText(R.id.volume_text, Integer.toString((int) (volume * 100)));
			panelRv.setViewVisibility(R.id.device_select, View.VISIBLE);
			String selectedDevice = PreferenceManager.getDefaultSharedPreferences(context)
			                                         .getString("device", "Select device");
			panelRv.setTextViewText(R.id.device_select, selectedDevice);
		}
		if (cocktailIds.length != 0) {
			cocktailManager.updateCocktail(cocktailIds[0], panelRv, stateRv);
		}
	}
}