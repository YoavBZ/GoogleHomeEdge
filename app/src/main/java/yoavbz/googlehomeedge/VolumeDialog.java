package yoavbz.googlehomeedge;

import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import org.adw.library.widgets.discreteseekbar.DiscreteSeekBar;
import su.litvak.chromecast.api.v2.ChromeCast;

public class VolumeDialog extends Activity {

	private static final String TAG = PanelProvider.class.getSimpleName();
	private ChromeCast chromeCast;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getWindow().setBackgroundDrawable(new ColorDrawable(0));
		setFinishOnTouchOutside(false);

		String ip = PreferenceManager.getDefaultSharedPreferences(this).getString("ip", null);
		chromeCast = new ChromeCast(ip);
		int currentVolume = getIntent().getIntExtra("volume", 20);
		Log.d(TAG, "Current volume = " + currentVolume);

		final DiscreteSeekBar seekBar = new DiscreteSeekBar(this);
		seekBar.setProgress(currentVolume);
		seekBar.setMin(1);
		seekBar.setMax(100);
		seekBar.setOnProgressChangeListener(new DiscreteSeekBar.OnProgressChangeListener() {
			@Override
			public void onProgressChanged(DiscreteSeekBar seekBar, int value, boolean fromUser) {
			}

			@Override
			public void onStartTrackingTouch(DiscreteSeekBar seekBar) {
			}

			@Override
			public void onStopTrackingTouch(DiscreteSeekBar seekBar) {
				final int progress = seekBar.getProgress();
				Log.d(TAG, "Seekbar changed to value: " + progress);
				new Thread(() -> {
					try {
						chromeCast.connect();
						chromeCast.setVolume((float) progress / 100);
						Intent intent = new Intent(getApplicationContext(), PanelProvider.class);
						intent.setAction(PanelProvider.ACTION_REFRESH);
						sendBroadcast(intent);
					} catch (Exception e) {
						Log.e(TAG, e.toString());
					}
				}).start();
			}
		});
		setTheme(R.style.Dialog);
		new AlertDialog.Builder(this).setTitle("Enter Volume")
		                             .setIcon(R.drawable.ic_volume)
		                             .setView(seekBar)
		                             .setOnDismissListener(dialog -> finish())
		                             .show();
	}
}
