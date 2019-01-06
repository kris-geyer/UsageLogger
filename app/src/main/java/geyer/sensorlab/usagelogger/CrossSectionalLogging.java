package geyer.sensorlab.usagelogger;

import android.content.Context;

import net.sqlcipher.database.SQLiteDatabase;
import net.sqlcipher.database.SQLiteOpenHelper;

public class CrossSectionalLogging extends SQLiteOpenHelper {


    private static CrossSectionalLogging instance;
    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "apps_and_permission.db",
            SQL_CREATE_ENTRIES =
                    "CREATE TABLE " + CrossSectionalLoggingCols.CrossSectionalLoggingColsNames.TABLE_NAME + " (" +
                            CrossSectionalLoggingCols.CrossSectionalLoggingColsNames.COLUMN_NAME_ENTRY + " INTEGER PRIMARY KEY AUTOINCREMENT,"+
                            CrossSectionalLoggingCols.CrossSectionalLoggingColsNames.APP + " TEXT," +
                            CrossSectionalLoggingCols.CrossSectionalLoggingColsNames.PERMISSION + " TEXT," +
                            CrossSectionalLoggingCols.CrossSectionalLoggingColsNames.APPROVED + " TEXT," +
                            CrossSectionalLoggingCols.CrossSectionalLoggingColsNames.TIME + " INTEGER" + " )",
            SQL_DELETE_ENTRIES = "DROP TABLE IF EXISTS " + CrossSectionalLoggingCols.CrossSectionalLoggingColsNames.TABLE_NAME;


    public CrossSectionalLogging(Context context){
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    static public synchronized CrossSectionalLogging getInstance(Context context){
        if(instance == null) {
            instance = new CrossSectionalLogging(context);
        }
        return instance;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_ENTRIES);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(SQL_DELETE_ENTRIES);
        onCreate(db);
    }
}
