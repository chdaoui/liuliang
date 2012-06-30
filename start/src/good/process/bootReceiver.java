package good.process;

import good.start.Traffic;
import android.content.*;
import android.widget.*;

public class bootReceiver extends BroadcastReceiver {
	
	@Override
	public void onReceive(Context arg0, Intent arg1) {
		// TODO Auto-generated method stub
		if (arg1.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
			Toast.makeText(arg0, "开机了开机了", 3000).show();	
			Intent bootActivityIntent = new Intent(arg0, Traffic.class);
			bootActivityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			arg0.startActivity(bootActivityIntent);
		}
	}
}