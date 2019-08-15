package sogou.com.packageinstaller;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInstaller;
import android.util.Log;

public class InstallResultReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.e("InstallResultReceiver", "onReceive");
        if (intent!= null) {
            final int status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS,
                    PackageInstaller.STATUS_FAILURE);
            if (status == PackageInstaller.STATUS_SUCCESS) {
                // success
            } else {
                Log.e("InstallResultReceiver", intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE));
            }
    }
}
}
