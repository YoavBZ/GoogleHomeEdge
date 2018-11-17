package yoavbz.googlehomeedge;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import com.samsung.android.sdk.SsdkUnsupportedException;
import com.samsung.android.sdk.look.Slook;
import su.litvak.chromecast.api.v2.ChromeCast;
import su.litvak.chromecast.api.v2.ChromeCasts;
import su.litvak.chromecast.api.v2.ChromeCastsListener;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class DeviceDialog extends Activity {

	private static final String TAG = DeviceDialog.class.getSimpleName();
	DialogAdapter adapter;
	ProgressBar progressBar;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		checkEdgeSupport();

		getWindow().setBackgroundDrawable(new ColorDrawable(0));
		setFinishOnTouchOutside(false);

		final AlertDialog.Builder builder = new AlertDialog.Builder(DeviceDialog.this);
		adapter = new DialogAdapter(this, new ArrayList<ChromeCast>());
		adapter.setNotifyOnChange(true);
		View titleView = getLayoutInflater().inflate(R.layout.dialog_title, null);
		progressBar = titleView.findViewById(R.id.device_progress_bar);
		builder.setTitle("Select Device")
		       .setIcon(R.drawable.google_home)
		       .setOnDismissListener(new DialogInterface.OnDismissListener() {
			       @Override
			       public void onDismiss(DialogInterface dialog) {
				       discoveryThread.interrupt();
				       finish();
			       }
		       })
		       .setAdapter(adapter, new DialogInterface.OnClickListener() {
			       public void onClick(DialogInterface dialog, int which) {
				       ChromeCast deviceSelected = adapter.getItem(which);
				       Log.d(TAG, String.format("Selected device = %s", deviceSelected));
				       SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(DeviceDialog.this);
				       pref.edit()
				           .putString("device", deviceSelected.getTitle())
				           .putString("ip", deviceSelected.getAddress())
				           .apply();
				       dialog.dismiss();
			       }
		       })
		       .setCustomTitle(titleView)
		       .show();
		discoveryThread.start();
	}

	Thread discoveryThread = new Thread(new Runnable() {
		@Override
		public void run() {
			try {
				Log.d(TAG, "Starting Chromecast device discovery");
				ChromeCasts.registerListener(addDeviceToDialogListener);
				ChromeCasts.startDiscovery(InetAddress.getLocalHost());
				TimeUnit.SECONDS.sleep(30);
				stopDiscovery();
				progressBar.setVisibility(View.INVISIBLE);
			} catch (IOException e) {
				Log.e(TAG, e.getMessage());
			} catch (InterruptedException e) {
				Log.d(TAG, "Device selection interrupted discoveryThread");
				stopDiscovery();
			}
		}
	});

	private void stopDiscovery() {
		try {
			Log.d(TAG, "Stopping Chromecast device discovery");
			ChromeCasts.stopDiscovery();
		} catch (IOException e) {
			Log.e(TAG, e.getMessage());
		}
	}

	ChromeCastsListener addDeviceToDialogListener = new ChromeCastsListener() {
		int index = 0;

		@Override
		public void newChromeCastDiscovered(final ChromeCast chromeCast) {
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					Log.d(TAG, String.format("Found device #%d = %s", index++, chromeCast));
					adapter.add(chromeCast);
				}
			});
		}

		@Override
		public void chromeCastRemoved(ChromeCast chromeCast) {
		}
	};

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

	private class DialogAdapter extends ArrayAdapter<ChromeCast> {
		private Context mContext;
		private List<ChromeCast> chromeCasts;

		DialogAdapter(@NonNull Context context, ArrayList<ChromeCast> list) {
			super(context, 0, list);
			mContext = context;
			chromeCasts = list;
		}

		@Nullable
		@Override
		public ChromeCast getItem(int position) {
			return chromeCasts.get(position);
		}

		@NonNull
		@Override
		public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
			View listItem = convertView;
			if (listItem == null) {
				listItem = LayoutInflater.from(mContext).inflate(R.layout.list_item, parent, false);
			}

			ChromeCast currentDevice = chromeCasts.get(position);

			TextView title = listItem.findViewById(R.id.device_title);
			title.setText(currentDevice.getTitle());

			return listItem;
		}
	}
}
