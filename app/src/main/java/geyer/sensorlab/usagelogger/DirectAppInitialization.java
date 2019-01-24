package geyer.sensorlab.usagelogger;

import android.app.ActivityManager;
import android.app.AppOpsManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Process;
import android.provider.Settings;
import android.util.Log;

import net.sqlcipher.Cursor;
import net.sqlcipher.database.SQLiteDatabase;

import java.io.File;

class DirectAppInitialization {

    private static final String TAG = "directApp";


    private AppOpsManager appOpsManager;
    private ActivityManager manager;

    private String pkg;
    private MainActivity mainActivityContext;
    private SharedPreferences sharedPreferences;

    private ResearcherInput researcherInput;

    private Boolean requestUsagePermission, requestNotificationPermission;


    DirectAppInitialization(
            MainActivity MainActivityContext
    )
    {
        researcherInput = new ResearcherInput();


        mainActivityContext = MainActivityContext;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            appOpsManager = (AppOpsManager) mainActivityContext.getSystemService(Context.APP_OPS_SERVICE);
        }
        manager = (ActivityManager) MainActivityContext.getSystemService(Context.ACTIVITY_SERVICE);
        pkg = mainActivityContext.getPackageName();

        sharedPreferences = mainActivityContext.getSharedPreferences("app initialization prefs", Context.MODE_PRIVATE);

        establishIfPermissionsAreNeeded();
    }

    private void establishIfPermissionsAreNeeded() {
        if(researcherInput.ProspectiveLoggingEmployed){
            switch (researcherInput.LevelOfProspectiveLogging){
                case 1:
                    requestUsagePermission = false;
                    requestNotificationPermission = false;
                    break;
                case 2:
                    requestUsagePermission = true;
                    requestNotificationPermission = false;
                    break;
                case 3:
                    requestUsagePermission = false;
                    requestNotificationPermission = true;
                    break;
                case 4:
                    requestUsagePermission = true;
                    requestNotificationPermission = true;
                    break;
            }
        }else{
            requestNotificationPermission = false;
        }

        if(researcherInput.RetrospectiveLoggingEmployed){
            requestUsagePermission = true;
        }

    }


    /**
     * States:
     * 1 - inform user
     * 2 - request password
     * 3 - document apps & permissions
     * 4 - request usage permission
     * 5 - request notification permission
     * 6 - All permission provided
     * 7 - retrospectively log data
     * 8 - retrospective data generating complete
     * 9 - start Service
     * 10 - start NotificationListenerService
     * 11 - service running
     *
     */

    int detectState(){
        int state = 1;

        if(sharedPreferences.getBoolean("instructions shown", false) || !researcherInput.InformUserRequired){
            state = 2;
            if(sharedPreferences.getBoolean("password generated", false) || !researcherInput.PasswordRequired){

                if(appDatabaseExists() || !researcherInput.PerformCrossSectionalAnalysis || DeterminePointOfDataExtraction.pointOfCrossSectionalLogging == 2){
                    state = detectPermissionsState();
                    if(state == 6){
                        if(researcherInput.RetrospectiveLoggingEmployed && DeterminePointOfDataExtraction.pointOfRetrospectiveLogging == 1){
                            if(!fileExists(Constants.PAST_USAGE_FILE)) {
                                return 7;
                            }else{
                                if(researcherInput.ProspectiveLoggingEmployed) {
                                    return returnStateOfService();
                                }
                            }
                        }else{
                            if(researcherInput.ProspectiveLoggingEmployed) {
                                return returnStateOfService();
                            }
                        }
                    }
                }else{
                    state = 3;
                }
            }
        }
        return state;
    }

    private int returnStateOfService() {
        if(requestNotificationPermission){
            return 10;
        }else{
            if(serviceIsRunning(ProspectiveLogger.class)){
                return 11;
            }else{
                return 9;
            }
        }
    }

    private boolean appDatabaseExists() {
        SQLiteDatabase db = AppsSQL.getInstance(mainActivityContext).getReadableDatabase(sharedPreferences.getString("password", "not to be used"));
        String selectQuery = "SELECT * FROM " + AppsSQLCols.AppsSQLColsName.TABLE_NAME;
        Cursor c = db.rawQuery(selectQuery, null);
        c.moveToLast();
        Log.i(TAG, "table size: " + c.getCount());
        int length = c.getCount();
        c.close();
        if(db.isOpen()) {db.close();}
        return length > 0;
    }

    private boolean fileExists(String file) {
        String directory = (String.valueOf(mainActivityContext.getFilesDir()) + File.separator);
        File inspectedFile = new File(directory + File.separator + file);
        return inspectedFile.exists();
    }


    //This establishes what permission is required based on what the researchers have indicated that they wish to include in their study.
    public int detectPermissionsState() {

        //if requestUsagePermission or requestNotificationPermission are null then they will be re-initialized
        if(requestUsagePermission != null && requestNotificationPermission != null){
             establishIfPermissionsAreNeeded();
        }
        Boolean permissionsStillRequired = requestUsagePermission || requestNotificationPermission;

        if(permissionsStillRequired){
            if(!requestNotificationPermission){
                if(establishStateOfUsageStatisticsPermission()){
                    Log.i(TAG, "usage statistics permission granted");
                    return 6;
                }else{
                    Log.i(TAG, "usage statistics permission not granted");
                    return 4;
                }
            }else if(!requestUsagePermission){
                if(establishStateOfNotificationListenerPermission()){
                    Log.i(TAG, "notification listener permission granted");
                    return 6;
                }else{
                    return 5;
                }
            }else{
                if(establishStateOfUsageStatisticsPermission()){
                    if(establishStateOfNotificationListenerPermission()){
                        Log.i(TAG, "all permissions permission granted");
                        return 6;
                    }else{
                        return 5;
                    }
                }else{
                    Log.i(TAG, "usage statistics permission not granted *2");
                    return 4;
                }
            }
        }else{
            return 6;
        }

    }

    //establishes if the usage statistics permissions are provided
    private Boolean establishStateOfUsageStatisticsPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            int mode = 2;
            if (appOpsManager != null) {
                mode = appOpsManager.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), String.valueOf(pkg));
            }
            return (mode == AppOpsManager.MODE_ALLOWED);
        }else{
            return true;
        }
    }

    private boolean establishStateOfNotificationListenerPermission() {
        ComponentName cn = new ComponentName(mainActivityContext, ProspectiveNotificationLogger.class);
        String flat = Settings.Secure.getString(mainActivityContext.getContentResolver(), "enabled_notification_listeners");
        return flat == null || flat.contains(cn.flattenToString());
    }

    //detects if the background logging behaviour is running, this will not detect if data is being collected.
    private boolean serviceIsRunning(Class<?> serviceClass) {
        if (manager != null) {
            for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
                if (serviceClass.getName().equals(service.service.getClassName())) {
                    return true;
                }
            }
        }
        return false;
    }

}
