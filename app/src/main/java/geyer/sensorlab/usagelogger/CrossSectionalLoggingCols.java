package geyer.sensorlab.usagelogger;

import android.provider.BaseColumns;

public class CrossSectionalLoggingCols {

    public static abstract class CrossSectionalLoggingColsNames implements BaseColumns {
        public static final String
                TABLE_NAME = "app_and_permissions_database",
                COLUMN_NAME_ENTRY = "column_id",
                APP = "app",
                PERMISSION = "permission",
                APPROVED = "approved",
                TIME = "time";
    }
}
