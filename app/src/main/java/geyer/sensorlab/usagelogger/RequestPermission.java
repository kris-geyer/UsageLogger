package geyer.sensorlab.usagelogger;

import android.content.Intent;
import android.provider.Settings;

public class RequestPermission {
    private MainActivity activityContext;

    RequestPermission(MainActivity mainActivity) {
        activityContext = mainActivity;
    }

    public void requestSpecificPermission(int permissionRequest){
        switch (permissionRequest){
            case Constants.USAGE_STATISTIC_PERMISSION_REQUEST:
                activityContext.startActivityForResult(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS), Constants.USAGE_STATISTIC_PERMISSION_REQUEST);
                break;
            case Constants.NOTIFICATION_LISTENER_PERMISSIONS:
                activityContext.startActivityForResult(new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"), Constants.NOTIFICATION_LISTENER_PERMISSIONS);
        }

    }

}
