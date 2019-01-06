package geyer.sensorlab.usagelogger;

import android.content.Context;

import net.sqlcipher.database.SQLiteDatabase;
import net.sqlcipher.database.SQLiteOpenHelper;

public class ProspectiveSQL extends SQLiteOpenHelper {


    private static ProspectiveSQL instance;
    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "prospective.db",
            SQL_CREATE_ENTRIES =
                    "CREATE TABLE " + ProspectiveSQLCol.ProspectiveSQLColName.TABLE_NAME + " (" +
                            ProspectiveSQLCol.ProspectiveSQLColName.COLUMN_NAME_ENTRY + " INTEGER PRIMARY KEY AUTOINCREMENT,"+
                            ProspectiveSQLCol.ProspectiveSQLColName.EVENT + " TEXT," +
                            ProspectiveSQLCol.ProspectiveSQLColName.TIME + " INTEGER" + " )",
            SQL_DELETE_ENTRIES = "DROP TABLE IF EXISTS " + ProspectiveSQLCol.ProspectiveSQLColName.TABLE_NAME;


    public ProspectiveSQL(Context context){
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    static public synchronized ProspectiveSQL getInstance(Context context){
        if(instance == null) {
            instance = new ProspectiveSQL(context);
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
