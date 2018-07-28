package yoavbz.googlehomeedge;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import com.samsung.android.sdk.SsdkUnsupportedException;
import com.samsung.android.sdk.look.Slook;

public class MainActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Slook slook = new Slook();

		try {
			slook.initialize(this);
			if (!slook.isFeatureEnabled(Slook.COCKTAIL_PANEL)) {
				((TextView) findViewById(R.id.main_text)).setText(getString(R.string.no_support));
			}
		} catch (SsdkUnsupportedException e) {
//			((TextView)findViewById(R.id.main_text)).setText(getString(R.string.no_support));
		}
		Intent settingsActivity = new Intent(this, SettingsActivity.class);
		settingsActivity.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_TASK_ON_HOME);
		startActivity(settingsActivity);
//		this.finish();
	}
}