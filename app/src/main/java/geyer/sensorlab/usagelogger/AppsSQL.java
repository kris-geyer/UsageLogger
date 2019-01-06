package geyer.sensorlab.usagelogger;

import android.content.Context;

import net.sqlcipher.database.SQLiteDatabase;
import net.sqlcipher.database.SQLiteOpenHelper;

public class AppsSQL extends SQLiteOpenHelper {


    private static AppsSQL instance;
    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "apps.db",
            SQL_CREATE_ENTRIES =
                    "CREATE TABLE " + AppsSQLCols.AppsSQLColsName.TABLE_NAME + " (" +
                            AppsSQLCols.AppsSQLColsName.COLUMN_NAME_ENTRY + " INTEGER PRIMARY KEY AUTOINCREMENT,"+
                            AppsSQLCols.AppsSQLColsName.APP + " TEXT," +
                            AppsSQLCols.AppsSQLColsName.INSTALLED+ " TEXT," +
                            AppsSQLCols.AppsSQLColsName.TIME + " INTEGER" + " )",
            SQL_DELETE_ENTRIES = "DROP TABLE IF EXISTS " + AppsSQLCols.AppsSQLColsName.TABLE_NAME;


    public AppsSQL(Context context){
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    static public synchronized AppsSQL getInstance(Context context){
        if(instance == null) {
            instance = new AppsSQL(context);
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
