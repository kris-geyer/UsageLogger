package geyer.sensorlab.usagelogger;

import android.provider.BaseColumns;

public class ProspectiveSQLCol {


    public static abstract class ProspectiveSQLColName implements BaseColumns {
        public static final String
                TABLE_NAME = "prospective_database",
                COLUMN_NAME_ENTRY = "column_id",
                EVENT = "event",
                TIME = "time";
    }
}
