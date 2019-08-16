package sogou.com.packageinstaller;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInstaller;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

public class InstallResultReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.e("InstallResultReceiver", "onReceive");
        if (intent != null) {
            final int status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS,
                    PackageInstaller.STATUS_FAILURE);
            if (status == PackageInstaller.STATUS_SUCCESS) {
                // success
            } else {
                Log.e("InstallResultReceiver", intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE));
//                Toast.makeText(context, intent., Toast.LENGTH_SHORT).show();
                getErrorState(context,intent);

            }
        }
    }


    protected void getErrorState(Context context,Intent intent) {
        Bundle extras = intent.getExtras();
        if ("cm.android.intent.action.INSTALL_COMPLETE".equals(intent.getAction())) {
            int status = extras.getInt(PackageInstaller.EXTRA_STATUS);
            String message = extras.getString(PackageInstaller.EXTRA_STATUS_MESSAGE);
            switch (status) {
                case PackageInstaller.STATUS_PENDING_USER_ACTION:
                    // This test app isn't privileged, so the user has to confirm the install.
                    Intent confirmIntent = (Intent) extras.get(Intent.EXTRA_INTENT);
                    context.startActivity(confirmIntent);
                    break;
                case PackageInstaller.STATUS_SUCCESS:
                    Toast.makeText(context, "Install succeeded!", Toast.LENGTH_SHORT).show();
                    break;
                case PackageInstaller.STATUS_FAILURE:
                case PackageInstaller.STATUS_FAILURE_ABORTED:
                case PackageInstaller.STATUS_FAILURE_BLOCKED:
                case PackageInstaller.STATUS_FAILURE_CONFLICT:
                case PackageInstaller.STATUS_FAILURE_INCOMPATIBLE:
                case PackageInstaller.STATUS_FAILURE_INVALID:
                case PackageInstaller.STATUS_FAILURE_STORAGE:
                    Toast.makeText(context, "Install failed! " + status + ", " + message,
                            Toast.LENGTH_SHORT).show();
                    break;
                default:
                    Toast.makeText(context, "Unrecognized status received from installer: " + status,
                            Toast.LENGTH_SHORT).show();
            }
        }
    }
}
