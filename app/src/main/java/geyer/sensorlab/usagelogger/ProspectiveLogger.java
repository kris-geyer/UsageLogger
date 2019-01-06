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

import java.util.Objects;

public class ProspectiveLogger extends Service {
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    private static final String TAG = "LOGGER";

    BroadcastReceiver screenReceiver, appReceiver;
    SharedPreferences prefs;
    SharedPreferences.Editor editor;

    Handler handler;
    IdentifyAppInForeground identifyAppInForeground;

    UsageStatsManager usm;
    ActivityManager am;

    String currentlyRunningApp, runningApp;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        initializeService();
        initializeSQLCipher();
        initializeSharedPreferences();
        initializeHandler();

        if(flags == 0){
            //retrieve data from bundle
            if (intent.hasExtra("has extras")){
                Bundle bundle = intent.getExtras();
                if(bundle != null){
                    Log.i(TAG, "bundle extra (usage log): " + bundle.getBoolean("usage log"));
                    if(bundle.getBoolean("usage log")){

                        Log.i("Bundle", "true");
                        initializeBroadcastReceivers();
                    }else{
                        Log.i("Bundle", "false");
                        initializeBroadcastReceiversWithoutUsageCapabilities();
                    }
                    if(bundle.getBoolean("document apps")){
                        Log.i(TAG, "bundle included a request to initialize the apps");
                        //initializeAppBroadcastReceiver();
                    }else{
                        Log.i(TAG, "bundle did not include a request to initialize the apps");
                    }

                }else{
                    Log.i(TAG, "bundle was null");
                    initializeBroadcastReceiversWithoutUsageCapabilities();
                }
            }else{
                initializeBroadcastReceiversWithoutUsageCapabilities();
            }
        }

        return START_STICKY;
    }
/*

    private void initializeAppBroadcastReceiver(){

        appReceiver = new BroadcastReceiver() {
            @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
            @Override
            public void onReceive(Context context, Intent intent) {
                if(intent.getAction() != null){
                    switch (intent.getAction()){
                        case Intent.ACTION_PACKAGE_ADDED:
                            intent.getExtras()
                            //generateListOfNewApps(returnListsOfNovelAppsFromSQL(context), context);
                            //ensureCanBeEnteredIntoDatabase(updateRecordOfApps() + " added");
                            break;
                        case Intent.ACTION_PACKAGE_REMOVED:
                            //generateListOfNewApps(returnListsOfNovelAppsFromSQL(context), context);
                            //ensureCanBeEnteredIntoDatabase(updateRecordOfApps() + " removed");

                    }
                }
            }


            private HashMap<String, Boolean> returnListsOfNovelAppsFromSQL(Context context) {
                String selectQuery = "SELECT * FROM " + AppsSQLCols.AppsSQLColsNames.TABLE_NAME;
                SQLiteDatabase db = AppsSQL.getInstance(context).getReadableDatabase(prefs.getString("password", "not to be used"));

                Cursor c = db.rawQuery(selectQuery, null);

                int appsInt = c.getColumnIndex(AppsSQLCols.AppsSQLColsNames.APP);
                int installedInt = c.getColumnIndex(AppsSQLCols.AppsSQLColsNames.INSTALLED);
                HashMap<String, Boolean> appList = new HashMap<>();

                c.moveToLast();
                int rowLength = c.getCount();
                if (rowLength > 0) {
                    try {
                        String lastReadApp = "";
                        for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
                            if(!lastReadApp.equals(c.getString(appsInt))){
                                if(c.getString(installedInt).equals("installed")){
                                    String newApp = c.getString(appsInt);
                                    appList.put(newApp, true);
                                    Log.i(TAG, "app: " + newApp);
                                }else{
                                    Log.i(TAG,c.getString(installedInt));
                                    String newApp = c.getString(appsInt);
                                    appList.put(newApp, false);
                                    Log.i(TAG, "app: " + newApp);
                                }
                            }

                        }
                    } catch (Exception e) {
                        Log.e("file construct", "error " + e);
                    }
                    c.close();
                }
                return appList;
            }

            @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
            private void generateListOfNewApps(HashMap<String, Boolean> appsHashmap, Context context) {

                PackageManager pm = context.getPackageManager();
                final List<PackageInfo> appInstall= pm.getInstalledPackages(PackageManager.GET_PERMISSIONS|PackageManager.GET_RECEIVERS|
                        PackageManager.GET_SERVICES|PackageManager.GET_PROVIDERS);

                List<String> newApps = new ArrayList<>();
                for (PackageInfo packageInfo:appInstall){
                    newApps.add((String) packageInfo.applicationInfo.loadLabel(pm));
                }

                String appOfInterest;
                ArrayList <String> toAddToSQL = new ArrayList<>();

                ArrayList<String> apps = new ArrayList<>();


                //identify apps still installed
                //go through the hashmap and see what the final result was, true or not.
                //put this into a List referred to as apps.

                Iterator it = appsHashmap.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry pair = (Map.Entry)it.next();
                    if((Boolean) pair.getValue()){
                        apps.add((String) pair.getKey());
                    }else if(apps.contains(pair.getKey())){
                        apps.remove(pair.getKey());
                    }
                    Log.i(TAG,pair.getKey() + " = " + pair.getValue());
                    it.remove(); // avoids a ConcurrentModificationException
                }
                Boolean added;
                if(apps.size() > newApps.size()){
                    added = false;
                    apps.removeAll(newApps);
                    if(apps.size() == 1){
                        appOfInterest = apps.get(0);

                    }else{
                        appOfInterest  = "problem";
                    }

                }else{
                    added = true;
                    newApps.removeAll(apps);
                    if(newApps.size() == 1){
                        appOfInterest = newApps.get(0);

                    }else{
                        appOfInterest = "problem";
                    }
                }

                Log.i(TAG, "new app: + " + appOfInterest);

                for (PackageInfo packageInfo: appInstall) {
                    if (packageInfo.applicationInfo.loadLabel(pm).equals(appOfInterest)) {
                        for (int i = 0; i < packageInfo.requestedPermissions.length; i++) {

                            String[] reqPermission = packageInfo.requestedPermissions;
                            int[] reqPermissionFlag = packageInfo.requestedPermissionsFlags;
                            String tempPermission = reqPermission[i];
                            int tempPermissionFlag = reqPermissionFlag[i];
                            boolean approved = tempPermissionFlag == 3;
                            toAddToSQL.add(tempPermission + " $ " + approved);
                        }
                    }
                }



                storeAppRecordsInSQL(toAddToSQL, appOfInterest, context,added );
            }


            private void storeAppRecordsInSQL(ArrayList<String> appPermsList, String appName, Context context, Boolean added) {
                //initialize the SQL cipher
                SQLiteDatabase.loadLibs(context);
                SQLiteDatabase database = AppsSQL.getInstance(context).getWritableDatabase(prefs.getString("password", "not to be used"));
                //start loop that adds each app name, if it is installed, permission, approved or not, time

                final long time = System.currentTimeMillis();
                final int permsSize = appPermsList.size();

                ContentValues values = new ContentValues();
                if(permsSize==0) {
                    values.put(AppsSQLCols.AppsSQLColsNames.APP, appName);
                    if(added){
                        values.put(AppsSQLCols.AppsSQLColsNames.INSTALLED, "installed"); }
                    else{
                        values.put(AppsSQLCols.AppsSQLColsNames.INSTALLED, "uninstalled");
                    }
                    values.put(AppsSQLCols.AppsSQLColsNames.PERMISSION, "no permissions");
                    values.put(AppsSQLCols.AppsSQLColsNames.APPROVED, "false");
                    values.put(AppsSQLCols.AppsSQLColsNames.TIME, time);

                    database.insert(AppsSQLCols.AppsSQLColsNames.TABLE_NAME, null, values);
                    Cursor cursor = database.rawQuery("SELECT * FROM '" + AppsSQLCols.AppsSQLColsNames.TABLE_NAME + "';", null);

                    cursor.close();
                }else {


                    for (int i = 0; i < appPermsList.size(); i++) {
                        String[] currentPermissionSplit = ((String) appPermsList.get(i)).split("\\$");
                        Log.i(TAG, "split string: " + currentPermissionSplit[0]);
                        Log.i(TAG, "split string: " + currentPermissionSplit[1]);
                        values.put(AppsSQLCols.AppsSQLColsNames.APP, appName);
                        if(added){
                            values.put(AppsSQLCols.AppsSQLColsNames.INSTALLED, "installed"); }
                        else{
                            values.put(AppsSQLCols.AppsSQLColsNames.INSTALLED, "uninstalled");
                        }
                        values.put(AppsSQLCols.AppsSQLColsNames.PERMISSION, currentPermissionSplit[0]);
                        values.put(AppsSQLCols.AppsSQLColsNames.APPROVED, currentPermissionSplit[1]);
                        values.put(AppsSQLCols.AppsSQLColsNames.TIME, time);

                        database.insert(AppsSQLCols.AppsSQLColsNames.TABLE_NAME, null, values);
                        Cursor cursor = database.rawQuery("SELECT * FROM '" + AppsSQLCols.AppsSQLColsNames.TABLE_NAME + "';", null);

                        cursor.close();

                    }
                }


                database.close();

                Log.d(TAG, "SQL attempted to document apps");


                //stop looping
            }




            private void ensureCanBeEnteredIntoDatabase(String toAdd) {
                if(toAdd.contains("[")){
                    toAdd = toAdd.replace("[", "");
                    toAdd = toAdd.replace("]", "");
                    Log.i(TAG, "toAdd = " + toAdd );
                    storeData(toAdd);
                }else{
                    storeData(toAdd);
                }
            }

            private String updateRecordOfApps() {
                SharedPreferences appPrefs = getSharedPreferences("app prefs to update", MODE_PRIVATE);
                SharedPreferences.Editor appEditor = appPrefs.edit();
                Collection<String> oldApps = new ArrayList<>();
                for (int i = 0; i <appPrefs.getInt("number of apps", 0); i++){
                    oldApps.add(appPrefs.getString("app"+i, "notapp"));
                }

                Collection<String> newApps = returnNewAppList();

                Log.d("oldApps", String.valueOf(oldApps));
                Log.d("newApps", String.valueOf(newApps));

                if(oldApps.size() > newApps.size()){
                    oldApps.removeAll(newApps);
                    Log.i("oldApps", "after removal: " + oldApps);
                    if(oldApps.size() == 1){
                        appEditor.clear().apply();
                        appEditor.putString("app" + appPrefs.getInt("number of apps", 0), ((ArrayList<String>) oldApps).get(0))
                                .putInt("number of apps",(appPrefs.getInt("number of apps", 0)+1))
                                .apply();
                        for(int i = 0; i < newApps.size();i++){
                            appEditor.putString("app" + appPrefs.getInt("number of apps", 0), ((ArrayList<String>) newApps).get(i))
                                    .putInt("number of apps",(appPrefs.getInt("number of apps", 0)+1))
                                    .apply();
                        }
                        return String.valueOf(((ArrayList) oldApps).get(0));
                    }else{
                        Log.e("app error", "issue when old apps bigger in size");
                        for(int i = 0; i < oldApps.size();i++){
                            Log.e("app", String.valueOf(((ArrayList) oldApps).get(i)));
                        }
                        return"PROBLEM!!!";
                    }
                }else{
                    newApps.removeAll(oldApps);
                    Log.i("newApps", "after removal: " + newApps);
                    if(newApps.size() == 1){
                        appEditor.clear().apply();
                        appEditor.putString("app" + appPrefs.getInt("number of apps", 0), ((ArrayList<String>) newApps).get(0))
                                .putInt("number of apps",(appPrefs.getInt("number of apps", 0)+1))
                                .apply();
                        for(int i = 0; i < oldApps.size();i++){
                            appEditor.putString("app" + appPrefs.getInt("number of apps", 0), ((ArrayList<String>) oldApps).get(i))
                                    .putInt("number of apps",(appPrefs.getInt("number of apps", 0)+1))
                                    .apply();
                        }

                        return String.valueOf(((ArrayList) newApps).get(0));
                    }else{
                        Log.e("app error", "issue when old apps bigger in size");
                        for(int i = 0; i < newApps.size();i++){
                            Log.e("app", String.valueOf(((ArrayList) newApps).get(i)));
                        }
                        return"PROBLEM!!!";
                    }
                }
            }

            private ArrayList<String> returnNewAppList() {
                ArrayList<String> currentApps = new ArrayList<>();
                PackageManager pm = getApplicationContext().getPackageManager();
                final List<PackageInfo> appInstall= pm.getInstalledPackages(PackageManager.GET_PERMISSIONS|PackageManager.GET_RECEIVERS|
                        PackageManager.GET_SERVICES| PackageManager.GET_PROVIDERS);

                for(PackageInfo pInfo:appInstall) {
                    currentApps.add(pInfo.applicationInfo.loadLabel(pm).toString());
                }
                return currentApps;
            }
        };

        Log.i(TAG, "initialization of app receiver called");
        IntentFilter appReceiverFilter = new IntentFilter();
        appReceiverFilter.addAction(Intent.ACTION_PACKAGE_ADDED);
        appReceiverFilter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        appReceiverFilter.addDataScheme("package");

        registerReceiver(appReceiver, appReceiverFilter);

    }
*/

    private void initializeSharedPreferences() {
        prefs = getSharedPreferences("app initialization prefs", MODE_PRIVATE);
        editor = prefs.edit();
        editor.apply();
    }

    private void initializeService() {
        Log.i(TAG, "running");

        if (Build.VERSION.SDK_INT >= 26) {
            if (Build.VERSION.SDK_INT > 26) {
                String CHANNEL_ONE_ID = "sensor.example. geyerk1.inspect.screenservice";
                String CHANNEL_ONE_NAME = "Screen service";
                NotificationChannel notificationChannel = null;
                notificationChannel = new NotificationChannel(CHANNEL_ONE_ID,
                        CHANNEL_ONE_NAME, NotificationManager.IMPORTANCE_MIN);
                notificationChannel.setShowBadge(true);
                notificationChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
                NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                assert manager != null;
                manager.createNotificationChannel(notificationChannel);

                Bitmap icon = BitmapFactory.decodeResource(getResources(), R.drawable.ic_prospective_logger);
                Notification notification = new Notification.Builder(getApplicationContext())
                        .setChannelId(CHANNEL_ONE_ID)
                        .setContentTitle("Recording data")
                        .setContentText("activity logger is collecting data")
                        .setSmallIcon(R.drawable.ic_prospective_logger)
                        .setLargeIcon(icon)
                        .build();

                Intent notificationIntent = new Intent(getApplicationContext(),MainActivity.class);
                notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                notification.contentIntent = PendingIntent.getActivity(getApplicationContext(), 0, notificationIntent, 0);

                startForeground(101, notification);
            } else {
                startForeground(101, updateNotification());
            }
        } else {
            Intent notificationIntent = new Intent(this, MainActivity.class);

            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                    notificationIntent, 0);

            Notification notification = new NotificationCompat.Builder(this)
                    .setContentTitle("Recording data")
                    .setContentText("activity logger is collecting data")
                    .setContentIntent(pendingIntent).build();

            startForeground(101, notification);
        }
    }

    private Notification updateNotification() {

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, MainActivity.class), 0);

        return new NotificationCompat.Builder(this)
                .setContentTitle("Recording data")
                .setContentText("activity logger is collecting data")
                .setSmallIcon(R.drawable.ic_prospective_logger)
                .setContentIntent(pendingIntent)
                .setOngoing(true).build();
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
        unregisterReceiver(appReceiver);
    }
}
