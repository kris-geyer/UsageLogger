package geyer.sensorlab.usagelogger;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.usage.UsageStatsManager;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import net.sqlcipher.Cursor;
import net.sqlcipher.database.SQLiteDatabase;

import java.nio.file.FileSystemNotFoundException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ProspectiveLogger extends Service {
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    private static final String TAG = "LOGGER";

    private static int NOTIFICATION_ID = 8675664;

    BroadcastReceiver screenReceiver, appReceiver;
    SharedPreferences prefs, serviceDirectionPrefs;
    SharedPreferences.Editor editor, serviceDirectionEditor;

    Handler handler;
    IdentifyAppInForeground identifyAppInForeground;

    UsageStatsManager usm;
    ActivityManager am;

    String currentlyRunningApp, runningApp;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        initializeSharedPreferences();
        initializeSQLCipher();
        callNotification();
        initializeHandler();

        Boolean callFromMain = callCameFromMain(intent);
        Log.i(TAG, "From main: " + callFromMain);
        if(callFromMain){
            utilizeDirectionFromMain(intent);
        }else{
            requestSavedDirectionForService();
        }
        informMain("please close and open the screen", false);

        return START_STICKY;
    }

    private boolean callCameFromMain(Intent intent) {
            Bundle bundle = intent.getExtras();
            return !bundle.getBoolean("from_restart_service");
    }

    private void utilizeDirectionFromMain(Intent intent) {
        determineService(intent);
        savedDirectionForService(intent);
    }

    private void determineService(Intent intent) {
        Bundle bundle = intent.getExtras();
        Log.i(TAG, "bundle extra (usage log): " + bundle.getBoolean("usage log"));
        if (bundle.getBoolean("usage log")) {
            Log.i("Bundle", "true");
            initializeBroadcastReceivers();
        } else {
            Log.i("Bundle", "false");
            initializeBroadcastReceiversWithoutUsageCapabilities();
        }
        if (bundle.getBoolean("document apps")) {
            Log.i(TAG, "bundle included a request to initialize the apps");
            initializeAppBroadcastReceiver();
        } else {
            Log.i(TAG, "bundle did not include a request to initialize the apps");
        }
    }

    private void savedDirectionForService(Intent intent) {
        Bundle bundle = intent.getExtras();
        if (bundle.getBoolean("usage log")) {
            serviceDirectionEditor.putBoolean("usage log", true);
        } else {
            Log.i("Bundle", "false");
            serviceDirectionEditor.putBoolean("usage log", false);
        }
        if (bundle.getBoolean("document apps")) {
            serviceDirectionEditor.putBoolean("app log", true);
        } else {
            serviceDirectionEditor.putBoolean("app log", false);
        }
        serviceDirectionEditor.apply();
    }

    private void requestSavedDirectionForService() {

        if (serviceDirectionPrefs.getBoolean("usage log",false)) {
            Log.i("Bundle", "true");
            initializeBroadcastReceivers();
        } else {
            Log.i("Bundle", "false");
            initializeBroadcastReceiversWithoutUsageCapabilities();
        }
        if (serviceDirectionPrefs.getBoolean("document apps", false)) {
            Log.i(TAG, "bundle included a request to initialize the apps");
            initializeAppBroadcastReceiver();
        } else {
            Log.i(TAG, "bundle did not include a request to initialize the apps");
        }
    }

    private void callNotification() {
        Notification note = initializeService();
        startForeground(NOTIFICATION_ID, note);
    }

    private Notification initializeService() {

        String contentTitle = "Usage Logger",
                contentText = "Usage Logger is recording data";


        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

            NotificationChannel channel = new NotificationChannel("usage logger", getString(R.string.app_name), NotificationManager.IMPORTANCE_DEFAULT);
            channel.enableLights(false);
            channel.enableVibration(false);
            channel.setSound(null,null);
            channel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);

            channel.setShowBadge(true);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }

        }

        NotificationCompat.Builder nfc = new NotificationCompat.Builder(getApplicationContext(),"usage logger")
                .setSmallIcon(R.drawable.ic_prospective_logger)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_prospective_logger))
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setVisibility(NotificationCompat.VISIBILITY_SECRET) //This hides the notification from lock screen
                .setContentTitle(contentTitle)
                .setContentText("Usage Logger is collecting data")
                .setOngoing(true);


        nfc.setContentTitle(contentTitle);
        nfc.setContentText(contentText);
        nfc.setStyle(new NotificationCompat.BigTextStyle().bigText(contentText).setBigContentTitle(contentTitle));
        nfc.setWhen(System.currentTimeMillis());

        return nfc.build();

    }


    private void initializeAppBroadcastReceiver(){

        appReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if(intent.getAction() != null){
                    switch (intent.getAction()){
                        case Intent.ACTION_PACKAGE_ADDED:
                            generateListOfNewApps(returnListsOfNovelAppsFromSQL(context), context);
                            break;
                        case Intent.ACTION_PACKAGE_REMOVED:
                            generateListOfNewApps(returnListsOfNovelAppsFromSQL(context), context);
                            break;
                    }
                }
            }


            private ArrayList<String> returnListsOfNovelAppsFromSQL(Context context) {
                String selectQuery = "SELECT * FROM " + AppsSQLCols.AppsSQLColsName.TABLE_NAME;
                SQLiteDatabase db = AppsSQL.getInstance(context).getReadableDatabase(prefs.getString("password", "not to be used"));

                Cursor c = db.rawQuery(selectQuery, null);

                int appsInt = c.getColumnIndex(AppsSQLCols.AppsSQLColsName.APP);
                int installedInt = c.getColumnIndex(AppsSQLCols.AppsSQLColsName.INSTALLED);

                ArrayList<String> installedApps = new ArrayList<>();

                c.moveToLast();
                int rowLength = c.getCount();
                if (rowLength > 0) {
                    try {
                        for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
                            if(c.getString(installedInt).equals("true")){
                                installedApps.add(c.getString(appsInt));
                            }else{
                                String app = c.getString(appsInt);
                                installedApps.remove(app);
                            }
                        }
                    } catch (Exception e) {
                        Log.e("file construct", "error " + e);
                    }
                    c.close();
                }
                return installedApps;
            }

            private void generateListOfNewApps(ArrayList<String> documentedInstalledApps, Context context) {

                PackageManager pm = context.getPackageManager();
                final List<PackageInfo> appInstall= pm.getInstalledPackages(PackageManager.GET_PERMISSIONS|PackageManager.GET_RECEIVERS|
                        PackageManager.GET_SERVICES|PackageManager.GET_PROVIDERS);

                List<String> newApps = new ArrayList<>();
                for (PackageInfo packageInfo:appInstall){
                    newApps.add((String) packageInfo.applicationInfo.loadLabel(pm));
                }

                String appOfInterest;

                /**
                 * Document everything in a Log tag
                 */

                //minus means that document installed is smaller
                Log.i(TAG, "size discrepancy: " + (documentedInstalledApps.size() - newApps.size()));

                Boolean added;
                if(documentedInstalledApps.size() > newApps.size()){
                    added = false;
                    documentedInstalledApps.removeAll(newApps);
                    if(documentedInstalledApps.size() == 1){
                        appOfInterest = documentedInstalledApps.get(0);

                    }else{
                        appOfInterest  = "problem";
                        informMain("error with updating app", true);
                    }

                }else{
                    added = true;
                    newApps.removeAll(documentedInstalledApps);
                    if(newApps.size() == 1){
                        appOfInterest = newApps.get(0);

                    }else{
                        appOfInterest = "problem";
                        informMain("error with updating app", true);
                    }
                }

                Log.i(TAG, "app of interest: " + appOfInterest);
                storeAppRecordsInSQL( appOfInterest, context, added);
            }

            private void storeAppRecordsInSQL(String appName, Context context, Boolean added) {
                //initialize the SQL cipher
                SQLiteDatabase.loadLibs(context);
                SQLiteDatabase database = AppsSQL.getInstance(context).getWritableDatabase(prefs.getString("password", "not to be used"));
                //start loop that adds each app name, if it is installed, permission, approved or not, time

                final long time = System.currentTimeMillis();

                ContentValues values = new ContentValues();

                    values.put(AppsSQLCols.AppsSQLColsName.APP, appName);
                    if(added){
                        values.put(AppsSQLCols.AppsSQLColsName.INSTALLED, "true"); }
                    else{
                        values.put(AppsSQLCols.AppsSQLColsName.INSTALLED, "false");
                    }
                    values.put(AppsSQLCols.AppsSQLColsName.TIME, time);

                    database.insert(AppsSQLCols.AppsSQLColsName.TABLE_NAME, null, values);
                    Cursor cursor = database.rawQuery("SELECT * FROM '" + AppsSQLCols.AppsSQLColsName.TABLE_NAME + "';", null);

                    cursor.close();

                database.close();

                /**
                 * Document the permissions (without approval of new app if just added)
                 */

                Log.d(TAG, "SQL attempted to document apps");

            }

        };

        Log.i(TAG, "initialization of app receiver called");
        IntentFilter appReceiverFilter = new IntentFilter();
        appReceiverFilter.addAction(Intent.ACTION_PACKAGE_ADDED);
        appReceiverFilter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        appReceiverFilter.addDataScheme("package");

        registerReceiver(appReceiver, appReceiverFilter);

    }

    private void initializeSharedPreferences() {
        prefs = getSharedPreferences("app initialization prefs", MODE_PRIVATE);
        serviceDirectionPrefs = getSharedPreferences("service direction prefs", MODE_PRIVATE);
        editor = prefs.edit();
        editor.apply();

        serviceDirectionEditor = serviceDirectionPrefs.edit();
        serviceDirectionEditor.apply();
    }

    private void initializeSQLCipher() {
        SQLiteDatabase.loadLibs(this);
    }
    @SuppressLint("WrongConstant")
    private void initializeHandler() {
        handler = new Handler();
        identifyAppInForeground = new IdentifyAppInForeground();

        currentlyRunningApp = "";
        runningApp = "x";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            usm = (UsageStatsManager) this.getSystemService("usagestats");
        }else{
            am = (ActivityManager)this.getSystemService(getApplicationContext().ACTIVITY_SERVICE);
        }
    }

    private void initializeBroadcastReceivers() {
        screenReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if(intent.getAction() != null){
                    switch (intent.getAction()){
                        case Intent.ACTION_SCREEN_OFF:
                            storeData("screen off");
                            handler.removeCallbacks(callIdentifyAppInForeground);
                            break;
                        case Intent.ACTION_SCREEN_ON:
                            storeData("screen on");
                            handler.postDelayed(callIdentifyAppInForeground, 100);
                            break;
                        case Intent.ACTION_USER_PRESENT:
                            storeData("user present");
                            break;
                    }
                }
            }


            final Runnable callIdentifyAppInForeground = new Runnable() {
                @Override
                public void run() {
                    String appRunningInForeground;

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        appRunningInForeground = identifyAppInForeground.identifyForegroundTaskLollipop(usm, getApplicationContext());
                    } else {
                        appRunningInForeground = identifyAppInForeground.identifyForegroundTaskUnderLollipop(am);
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                        if (Objects.equals(appRunningInForeground, "usage architecture")) {
                            informMain("", false);
                        }
                        if (!Objects.equals(appRunningInForeground, currentlyRunningApp)) {
                            storeData("App: " + appRunningInForeground);
                            currentlyRunningApp = appRunningInForeground;
                        }
                    } else {
                        if(appRunningInForeground.equals("usage architecture")){
                            informMain("", false);
                        }
                        if (appRunningInForeground.equals(currentlyRunningApp)) {
                            storeData("App: " + appRunningInForeground);
                            currentlyRunningApp = appRunningInForeground;
                        }
                    }
                    handler.postDelayed(callIdentifyAppInForeground, 1000);
                }
            };
        };

        IntentFilter screenReceiverFilter = new IntentFilter();
        screenReceiverFilter.addAction(Intent.ACTION_SCREEN_OFF);
        screenReceiverFilter.addAction(Intent.ACTION_SCREEN_ON);
        screenReceiverFilter.addAction(Intent.ACTION_USER_PRESENT);

        registerReceiver(screenReceiver, screenReceiverFilter);

    }

    private void initializeBroadcastReceiversWithoutUsageCapabilities() {
        Log.i(TAG, "call for broadcast receiver without usage capabilities");
        screenReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if(intent.getAction() != null){
                    switch (intent.getAction()){
                        case Intent.ACTION_SCREEN_OFF:
                            storeData("screen off");
                            break;
                        case Intent.ACTION_SCREEN_ON:
                            storeData("screen on");
                            break;
                        case Intent.ACTION_USER_PRESENT:
                            storeData("user present");
                            break;
                    }
                }
            }
        };

        IntentFilter screenReceiverFilter = new IntentFilter();
        screenReceiverFilter.addAction(Intent.ACTION_SCREEN_OFF);
        screenReceiverFilter.addAction(Intent.ACTION_SCREEN_ON);
        screenReceiverFilter.addAction(Intent.ACTION_USER_PRESENT);

        registerReceiver(screenReceiver, screenReceiverFilter);
    }

    private void storeData(String event) {

        SQLiteDatabase database = ProspectiveSQL.getInstance(this).getWritableDatabase(prefs.getString("password", "not to be used"));

        final long time = System.currentTimeMillis();

        ContentValues values = new ContentValues();
        values.put(ProspectiveSQLCol.ProspectiveSQLColName.EVENT, event);
        values.put(ProspectiveSQLCol.ProspectiveSQLColName.TIME, time);

        database.insert(ProspectiveSQLCol.ProspectiveSQLColName.TABLE_NAME, null, values);

        Cursor cursor = database.rawQuery("SELECT * FROM '" + ProspectiveSQLCol.ProspectiveSQLColName.TABLE_NAME + "';", null);
        Log.d("BackgroundLogging", "Update: " + event + " " + time);
        cursor.close();
        database.close();
        informMain("event", false);
    }

    private void informMain(String message, boolean error) {
        if(prefs.getBoolean("main in foreground",true)){
            if(!error){
                Intent intent = new Intent("changeInService");
                intent.putExtra("dataToReceive", true);
                intent.putExtra("dataToRelay", message);
                LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
                Log.i("service", "data sent to main");
            }else {
                Intent intent = new Intent("changeInService");
                intent.putExtra("dataToDisplay", true);
                intent.putExtra("dataToRelay", "error detected: " + message);
                LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
                Log.i("service", "data sent to main");
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(screenReceiver);
        if(serviceDirectionPrefs.getBoolean("document apps", false)){
            unregisterReceiver(appReceiver);
        }
    }
}
