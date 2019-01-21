package geyer.sensorlab.usagelogger;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import net.sqlcipher.Cursor;
import net.sqlcipher.database.SQLiteDatabase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class CrossSectionalQuery extends AsyncTask<Object, Integer, Integer> {

    private static final String TAG = "D_APPS";

    private Context mContextApps;
    private SharedPreferences appPrefs;
    private MainActivity mainActivityContext;

    // you may separate this or combined to caller class.

    CrossSectionalQuery(MainActivity delegate) {
        this.delegate = delegate;
    }

    private AsyncResult delegate;

    /**
     *Objects:
     * 1 - context
     * 2 - shared prefs
     */


    @Override
    protected Integer doInBackground(Object... objects) {
        initializeObjects(objects);
        int levelOfAnalysis = (Integer) objects[2];
        SQLiteDatabase.loadLibs(mContextApps);

        //return the list of installed apps for documenting installed apps


        HashMap<String, ArrayList> appPermissions = recordInstalledApps(levelOfAnalysis);
        documentApps(appPermissions.keySet());
        if(levelOfAnalysis>1){
            if(levelOfAnalysis == 2){
                storeAppRecordsInSQL(appPermissions, false);
            }else{
                storeAppRecordsInSQL(appPermissions, true);
            }
        }

        if(databaseExists("app database")){
            if(databaseExists("permission database")){
                return 1;
            }else{
                return 2;
            }
        }else{
            if(databaseExists("permission database")){
                return 3;
            }else{
                return 4;
            }
        }
    }

    private void initializeObjects(Object[] objects) {
        mContextApps = (Context) objects[0];
        appPrefs = mContextApps.getSharedPreferences("app initialization prefs",Context.MODE_PRIVATE);

        mainActivityContext = (MainActivity) objects[1];
    }

    private HashMap<String, ArrayList> recordInstalledApps(int levelOfAnalysis) {
        HashMap<String, ArrayList> appPermissions = new HashMap<>();

        PackageManager pm = mContextApps.getPackageManager();
        final List<PackageInfo> appInstall= pm.getInstalledPackages(PackageManager.GET_PERMISSIONS|PackageManager.GET_RECEIVERS|
                PackageManager.GET_SERVICES|PackageManager.GET_PROVIDERS);

        for(PackageInfo pInfo:appInstall) {
            String[] reqPermission = pInfo.requestedPermissions;
            int[] reqPermissionFlag = new int[0];

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN) {
                reqPermissionFlag = pInfo.requestedPermissionsFlags;
            }else{
                if(levelOfAnalysis < 2){
                    levelOfAnalysis = 2;
                }
            }

            if(levelOfAnalysis > 2){
                ArrayList<String> permissions = new ArrayList<>();
                if (reqPermission != null){
                    for (int i = 0; i < reqPermission.length; i++){
                        String tempPermission = reqPermission[i];
                        int tempPermissionFlag = reqPermissionFlag[i];
                        boolean approved = tempPermissionFlag == 3;
                        permissions.add(tempPermission + " $ " + approved);
                    }
                }
                Log.i("app", (String) pInfo.applicationInfo.loadLabel(pm));
                appPermissions.put(""+pInfo.applicationInfo.loadLabel(pm), permissions);
            }else{
                ArrayList<String> permissions = new ArrayList<>();
                if (reqPermission != null){
                    for (String tempPermission : reqPermission) {
                        permissions.add("*&^" + tempPermission);
                    }
                }
                Log.i("app", (String) pInfo.applicationInfo.loadLabel(pm));
                appPermissions.put(""+pInfo.applicationInfo.loadLabel(pm), permissions);
            }
        }
        return appPermissions;
    }

    private void documentApps(Set<String> apps) {
        SQLiteDatabase database = AppsSQL.getInstance(mainActivityContext).getWritableDatabase(appPrefs.getString("password", "not to be used"));

        final long time = System.currentTimeMillis();
        final int numApps = apps.size();

        ContentValues values = new ContentValues();

        AtomicInteger progress = new AtomicInteger();
        for(String app: apps){
            values.put(AppsSQLCols.AppsSQLColsName.APP, app);
            values.put(AppsSQLCols.AppsSQLColsName.INSTALLED, "true");
            values.put(AppsSQLCols.AppsSQLColsName.TIME, time);

            database.insert(AppsSQLCols.AppsSQLColsName.TABLE_NAME, null, values);
            Cursor cursor = database.rawQuery("SELECT * FROM '" + AppsSQLCols.AppsSQLColsName.TABLE_NAME + "';", null);
            cursor.close();

            int currentProgress = (progress.incrementAndGet() * 100) / numApps;
            publishProgress(currentProgress);
        }
    }

    private void storeAppRecordsInSQL(HashMap<String, ArrayList> appPermsList, boolean approvalReported) {
        //initialize the SQL cipher
        SQLiteDatabase database = CrossSectionalLogging.getInstance(mainActivityContext).getWritableDatabase(appPrefs.getString("password", "not to be used"));
        //start loop that adds each app name, if it is installed, permission, approved or not, time

        final long time = System.currentTimeMillis();
        final int numApps = appPermsList.size();

        ContentValues values = new ContentValues();
        AtomicInteger progress = new AtomicInteger();
        for (Map.Entry<String, ArrayList> item : appPermsList.entrySet()) {

            String app = item.getKey();
            ArrayList permission= item.getValue();

            //if there are permissions
            if(approvalReported){
                for (int i = 0; i < permission.size(); i++){
                    //do something with the permissions

                    String[] currentPermissionSplit = ((String) permission.get(i)).split("\\$");
                    Log.i(TAG, "split string: " + currentPermissionSplit[0]);
                    Log.i(TAG, "split string: " + currentPermissionSplit[1]);
                    values.put(CrossSectionalLoggingCols.CrossSectionalLoggingColsNames.APP, app);
                    values.put(CrossSectionalLoggingCols.CrossSectionalLoggingColsNames.PERMISSION, currentPermissionSplit[0]);
                    values.put(CrossSectionalLoggingCols.CrossSectionalLoggingColsNames.APPROVED, currentPermissionSplit[1]);
                    values.put(CrossSectionalLoggingCols.CrossSectionalLoggingColsNames.TIME, time);

                    database.insert(CrossSectionalLoggingCols.CrossSectionalLoggingColsNames.TABLE_NAME, null, values);
                    Cursor cursor = database.rawQuery("SELECT * FROM '" + CrossSectionalLoggingCols.CrossSectionalLoggingColsNames.TABLE_NAME + "';", null);

                    cursor.close();
                }
            }else{
                values.put(CrossSectionalLoggingCols.CrossSectionalLoggingColsNames.APP, app);
                values.put(CrossSectionalLoggingCols.CrossSectionalLoggingColsNames.PERMISSION, "NA");
                values.put(CrossSectionalLoggingCols.CrossSectionalLoggingColsNames.APPROVED, "false");
                values.put(CrossSectionalLoggingCols.CrossSectionalLoggingColsNames.TIME, time);

                database.insert(CrossSectionalLoggingCols.CrossSectionalLoggingColsNames.TABLE_NAME, null, values);
                Cursor cursor = database.rawQuery("SELECT * FROM '" + CrossSectionalLoggingCols.CrossSectionalLoggingColsNames.TABLE_NAME + "';", null);

                cursor.close();
            }

            /**
             * Experiment with what can be taken out of the loop in from the below four lines of code
             */


            int currentProgress = (progress.incrementAndGet() * 100) / numApps;
            publishProgress(currentProgress);
        }
        database.close();

        Log.d(TAG, "SQL attempted to document apps");

        //stop looping
    }

    private boolean databaseExists(String app_database) {
        int length = 0;
        switch (app_database){
            case "app database":
                SQLiteDatabase db = AppsSQL.getInstance(mContextApps).getReadableDatabase(appPrefs.getString("password", "not to be used"));
                String selectQuery = "SELECT * FROM " + AppsSQLCols.AppsSQLColsName.TABLE_NAME;
                Cursor c = db.rawQuery(selectQuery, null);
                c.moveToLast();
                length = c.getCount();
                c.close();
                Log.i(TAG, "table size: " + c.getCount());
                break;
            case "permission database":
                SQLiteDatabase permDB = CrossSectionalLogging.getInstance(mContextApps).getReadableDatabase(appPrefs.getString("password", "not to be used"));
                String selectPermQuery = "SELECT * FROM " + CrossSectionalLoggingCols.CrossSectionalLoggingColsNames.TABLE_NAME;
                Cursor cPerm = permDB.rawQuery(selectPermQuery , null);
                cPerm.moveToLast();
                length = cPerm.getCount();
                cPerm.close();
                Log.i(TAG, "table size: " + cPerm.getCount());
                break;
        }


        return length > 0;
    }




    @Override
    protected void onProgressUpdate(Integer... values) {
        super.onProgressUpdate(values);
        Log.i("Main", "Progress update: " + values[0]);
        //set up sending local signal to update main activity

        informMain(values[0]);
    }

    private void informMain(int progressBarValue) {
        Intent intent = new Intent("changeInService");
        intent.putExtra("progress bar update", true);
        intent.putExtra("progress bar progress", progressBarValue);
        intent.putExtra("asyncTask","documenting installed apps");
        LocalBroadcastManager.getInstance(mainActivityContext).sendBroadcast(intent);
        Log.i("service", "data sent to main");
    }

    @Override
    protected void onPostExecute(Integer integer) {
        super.onPostExecute(integer);
        delegate.processFinish(integer);
    }
}
