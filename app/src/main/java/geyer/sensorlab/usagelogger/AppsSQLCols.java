package geyer.sensorlab.usagelogger;

import android.provider.BaseColumns;

public class AppsSQLCols {

    public static abstract class AppsSQLColsName implements BaseColumns {
        public static final String
                TABLE_NAME = "app_database",
                COLUMN_NAME_ENTRY = "column_id",
                APP = "app",
                INSTALLED = "installed",
                TIME = "time";
    }
}
