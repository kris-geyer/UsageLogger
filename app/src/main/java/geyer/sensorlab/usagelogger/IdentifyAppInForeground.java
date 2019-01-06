package geyer.sensorlab.usagelogger;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.annotation.RequiresApi;

import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

public class IdentifyAppInForeground {

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @SuppressLint("WrongConstant")
    public String identifyForegroundTaskLollipop(UsageStatsManager usm, Context applicationContext) {

        String currentApp = "xrthdrfghdhdhd";

        long time = System.currentTimeMillis();
        List<UsageStats> appList = null;


        if (usm != null) {
            appList = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, time - 1000 * 1000, time);
        }

        if (appList != null && appList.size() > 0) {
            SortedMap<Long, UsageStats> mySortedMap = new TreeMap<>();
            for (UsageStats usageStats : appList) {
                mySortedMap.put(usageStats.getLastTimeUsed(), usageStats);
            }
            if (!mySortedMap.isEmpty()) {
                currentApp = mySortedMap.get(mySortedMap.lastKey()).getPackageName();
                PackageManager packageManager = applicationContext.getPackageManager();

                try {
                    currentApp = (String) packageManager.getApplicationLabel(packageManager.getApplicationInfo(currentApp, PackageManager.GET_META_DATA));
                } catch (PackageManager.NameNotFoundException e) {
                    e.printStackTrace();
                }

            }

        }
        return currentApp;

    }


    public String identifyForegroundTaskUnderLollipop(ActivityManager am) {
        List<ActivityManager.RunningAppProcessInfo> tasks;

        String currentApp = "xrthdrfghdhdhd";

        if (am != null) {
            tasks = am.getRunningAppProcesses();
            currentApp = tasks.get(0).processName;
        }

        return currentApp;
    }
}
