package geyer.sensorlab.usagelogger;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.ContextCompat;

public class RestartService extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Boolean basicLogger = intent.getBooleanExtra("prospectiveLogger", true);

        if(basicLogger){
            Intent serviceIntent = new Intent(context, ProspectiveLogger.class);
            serviceIntent.putExtra("from_restart_service", true);
            ContextCompat.startForegroundService(context, serviceIntent);
        }else{
            Intent serviceIntent = new Intent(context, ProspectiveNotificationLogger.class);
            serviceIntent.putExtra("from_restart_service", true);
            ContextCompat.startForegroundService(context, serviceIntent);
        }

    }
}
