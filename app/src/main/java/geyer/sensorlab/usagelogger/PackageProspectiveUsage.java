package geyer.sensorlab.usagelogger;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

import net.sqlcipher.Cursor;
import net.sqlcipher.database.SQLiteDatabase;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.concurrent.atomic.AtomicInteger;

public class PackageProspectiveUsage extends AsyncTask<Object, Integer, Integer> {

    Context mContext;
    private SharedPreferences prefs;
    private static final String TAG = "pkgPro";

    PackageProspectiveUsage(MainActivity delegate) {
        this.delegate = delegate;
    }

    private AsyncResult delegate;

    @Override
    protected Integer doInBackground(Object[] objects) {
        initializeComponents(objects);

        if(databaseExists("app database")) { packageApps(); }

        if(databaseExists("permission database")) { packagePermissions(); }

        if(databaseExists("prospective database")){ packageProspectiveData(); }

        /**
         * Uncomment when prospective logging is assessed
         */

        Boolean appsFileExists = fileExists(Constants.APPS_FILE),
                crossSectionalFileExists = fileExists(Constants.CROSS_SECTIONAL_FILE),
                prospectiveFileExists = fileExists(Constants.PROSPECTIVE_LOGGING);

        if(prospectiveFileExists){
            if(appsFileExists){
                if(crossSectionalFileExists){
                    return 9;
                }else{
                    return 10;
                }
            }else{
                if (crossSectionalFileExists) {
                    return 11;
                }else{
                    return 12;
                }
            }
        }else{
            if(appsFileExists){
                if(crossSectionalFileExists){
                    return 13;
                }else{
                    return 14;
                }
            }else{
                if (crossSectionalFileExists) {
                    return 15;
                }else{
                    return 16;
                }
            }
        }
    }

    private void initializeComponents(Object[] objects) {
        mContext = (Context) objects[0];
        prefs =  mContext.getSharedPreferences("app initialization prefs", Context.MODE_PRIVATE);
        SQLiteDatabase.loadLibs(mContext);
    }

    private boolean databaseExists(String app_database) {
        int length = 0;
        switch (app_database){
            case "app database":
                SQLiteDatabase db = AppsSQL.getInstance(mContext).getReadableDatabase(prefs.getString("password", "not to be used"));
                String selectQuery = "SELECT * FROM " + AppsSQLCols.AppsSQLColsName.TABLE_NAME;
                Cursor c = db.rawQuery(selectQuery, null);
                c.moveToLast();
                length = c.getCount();
                Log.i(TAG, "app database size: " + c.getCount());
                c.close();
                break;
            case "permission database":
                SQLiteDatabase permDB = CrossSectionalLogging.getInstance(mContext).getReadableDatabase(prefs.getString("password", "not to be used"));
                String selectPermQuery = "SELECT * FROM " + CrossSectionalLoggingCols.CrossSectionalLoggingColsNames.TABLE_NAME;
                Cursor cPerm = permDB.rawQuery(selectPermQuery , null);
                cPerm.moveToLast();
                length = cPerm.getCount();
                Log.i(TAG, "permission database size: " + cPerm.getCount());
                cPerm.close();
                break;
            case "prospective database":
                SQLiteDatabase proDB = ProspectiveSQL.getInstance(mContext).getReadableDatabase(prefs.getString("password", "not to be used"));
                String selectProQuery = "SELECT * FROM " + ProspectiveSQLCol.ProspectiveSQLColName.TABLE_NAME;
                Cursor cPro = proDB.rawQuery(selectProQuery , null);
                cPro.moveToLast();
                length = cPro.getCount();
                Log.i(TAG, "prospective database size: " + cPro.getCount());
                cPro.close();
                break;
        }

        return length > 0;
    }

    private void packagePermissions() {

        //creates document
        Document document = new Document();
        //getting destination
        File path = mContext.getFilesDir();
        File file = new File(path, Constants.CROSS_SECTIONAL_FILE);
        // Location to save
        PdfWriter writer = null;
        try {
            writer = PdfWriter.getInstance(document, new FileOutputStream(file));
        } catch (DocumentException e) {
            Log.e("MAIN", "document exception: " + e);
        } catch (FileNotFoundException e) {
            Log.e("MAIN", "File not found exception: " + e);
        }
        try {
            if (writer != null) {
                writer.setEncryption("concretepage".getBytes(), prefs.getString("pdfPassword", "sensorlab").getBytes(), PdfWriter.ALLOW_COPY, PdfWriter.ENCRYPTION_AES_128);
            }
        } catch (DocumentException e) {
            Log.e("MAIN", "document exception: " + e);
        }
        if (writer != null) {
            writer.createXmpMetadata();
        }
        // Open to write
        document.open();

        String selectQuery = "SELECT * FROM " + CrossSectionalLoggingCols.CrossSectionalLoggingColsNames.TABLE_NAME;
        SQLiteDatabase db = CrossSectionalLogging.getInstance(mContext).getReadableDatabase(prefs.getString("password", "not to be used"));

        Cursor c = db.rawQuery(selectQuery, null);

        int appName = c.getColumnIndex(CrossSectionalLoggingCols.CrossSectionalLoggingColsNames.APP);
        int permission = c.getColumnIndex(CrossSectionalLoggingCols.CrossSectionalLoggingColsNames.PERMISSION);
        int approved = c.getColumnIndex(CrossSectionalLoggingCols.CrossSectionalLoggingColsNames.APPROVED);
        int time = c.getColumnIndex(CrossSectionalLoggingCols.CrossSectionalLoggingColsNames.TIME);

        PdfPTable table = new PdfPTable(1);
        //attempts to add the columns
        c.moveToLast();
        int rowLength =  c.getCount();
        AtomicInteger progress = new AtomicInteger();
        if(rowLength > 0){
            try {
                for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
                    table.addCell(c.getString(appName));
                    table.addCell(c.getString(permission));
                    table.addCell(c.getString(approved));
                    table.addCell(c.getString(time));
                    if (c.getCount() != 0) {
                        int currentProgress = (progress.incrementAndGet() * 100) / rowLength;
                        publishProgress(currentProgress);
                    }
                }
            } catch (Exception e) {
                Log.e("file construct", "error " + e);
            }

            c.close();
            db.close();

            //add to document
            document.setPageSize(PageSize.A4);
            document.addCreationDate();
            try {
                document.add(table);
            } catch (DocumentException e) {
                Log.e("MAIN", "Document exception: " + e);
            }
            document.addAuthor("Kris");
            document.close();
        }
    }



    private void packageApps() {
        //creates document
        Document document = new Document();
        //getting destination
        File path = mContext.getFilesDir();
        File file = new File(path, Constants.APPS_FILE);
        // Location to save
        PdfWriter writer = null;
        try {
            writer = PdfWriter.getInstance(document, new FileOutputStream(file));
        } catch (DocumentException e) {
            Log.e("MAIN", "document exception: " + e);
        } catch (FileNotFoundException e) {
            Log.e("MAIN", "File not found exception: " + e);
        }
        try {
            if (writer != null) {
                writer.setEncryption("concretepage".getBytes(), prefs.getString("pdfPassword", "sensorlab").getBytes(), PdfWriter.ALLOW_COPY, PdfWriter.ENCRYPTION_AES_128);
            }
        } catch (DocumentException e) {
            Log.e("MAIN", "document exception: " + e);
        }
        if (writer != null) {
            writer.createXmpMetadata();
        }
        // Open to write
        document.open();


        String selectQuery = "SELECT * FROM " + AppsSQLCols.AppsSQLColsName.TABLE_NAME;
        SQLiteDatabase db = AppsSQL.getInstance(mContext).getReadableDatabase(prefs.getString("password", "not to be used"));

        Cursor c = db.rawQuery(selectQuery, null);

        int appName = c.getColumnIndex(AppsSQLCols.AppsSQLColsName.APP);
        int installed = c.getColumnIndex(AppsSQLCols.AppsSQLColsName.INSTALLED);
        int time = c.getColumnIndex(AppsSQLCols.AppsSQLColsName.TIME);

        PdfPTable table = new PdfPTable(1);
        //attempts to add the columns
        c.moveToLast();
        int rowLength =  c.getCount();

        AtomicInteger progress = new AtomicInteger();
        if(rowLength > 0){
            try {
                for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
                    table.addCell(c.getString(appName));
                    table.addCell(c.getString(installed));
                    table.addCell(c.getString(time));
                    if (c.getCount() != 0) {
                        int currentProgress = (progress.incrementAndGet() * 100) / rowLength;
                        publishProgress(currentProgress);
                    }
                }
            } catch (Exception e) {
                Log.e("file construct", "error " + e);
            }

            c.close();
            db.close();


            //add to document
            document.setPageSize(PageSize.A4);
            document.addCreationDate();
            try {
                document.add(table);
            } catch (DocumentException e) {
                Log.e("MAIN", "Document exception: " + e);
            }
            document.addAuthor("Kris");
            document.close();
        }
    }



    private boolean fileExists(String file) {
        String directory = (String.valueOf(mContext.getFilesDir()) + File.separator);
        File inspectedFile = new File(directory + File.separator + file);
        return inspectedFile.exists();
    }



    private void packageProspectiveData() {

        //creates document
        Document document = new Document();
        //getting destination
        File path = mContext.getFilesDir();
        File file = new File(path, Constants.PROSPECTIVE_LOGGING);
        // Location to save
        PdfWriter writer = null;
        try {
            writer = PdfWriter.getInstance(document, new FileOutputStream(file));
        } catch (DocumentException e) {
            Log.e("MAIN", "document exception: " + e);
        } catch (FileNotFoundException e) {
            Log.e("MAIN", "File not found exception: " + e);
        }
        try {
            if (writer != null) {
                writer.setEncryption("concretepage".getBytes(), prefs.getString("pdfPassword", "sensorlab").getBytes(), PdfWriter.ALLOW_COPY, PdfWriter.ENCRYPTION_AES_128);
            }
        } catch (DocumentException e) {
            Log.e("MAIN", "document exception: " + e);
        }
        if (writer != null) {
            writer.createXmpMetadata();
        }
        // Open to write
        document.open();


        String selectQuery = "SELECT * FROM " + ProspectiveSQLCol.ProspectiveSQLColName.TABLE_NAME;
        SQLiteDatabase db = ProspectiveSQL.getInstance(mContext).getReadableDatabase(prefs.getString("password", "not to be used"));

        Cursor c = db.rawQuery(selectQuery, null);

        int event = c.getColumnIndex(ProspectiveSQLCol.ProspectiveSQLColName.EVENT);
        int time = c.getColumnIndex(ProspectiveSQLCol.ProspectiveSQLColName.TIME);

        PdfPTable table = new PdfPTable(2);
        //attempts to add the columns
        c.moveToLast();
        int rowLength =  c.getCount();
        AtomicInteger progress = new AtomicInteger();
        if(rowLength > 0){
            try {
                for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
                    Log.i("pkgSQL", "event: " + event);
                    table.addCell(c.getString(event));
                    table.addCell(String.valueOf(c.getLong(time)));
                    if (c.getCount() != 0) {
                        int currentProgress = (progress.incrementAndGet() * 100) / rowLength;
                        publishProgress(currentProgress);
                    }
                }
            } catch (Exception e) {
                Log.e("file construct", "error " + e);
            }finally{
                if(!c.isClosed()){
                    c.close();
                }
                if(db.isOpen()){
                    db.close();
                }
            }

            //add to document
            document.setPageSize(PageSize.A4);
            document.addCreationDate();
            try {
                document.add(table);
            } catch (DocumentException e) {
                Log.e("MAIN", "Document exception: " + e);
            }
            document.addAuthor("Kris");
            document.close();
        }

    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        super.onProgressUpdate(values);
        Log.i("Main", "Progress update: " + values[0]);
        informMain(values[0]);
    }

    private void informMain(int progressBarValue) {
        Intent intent = new Intent("changeInService");
        intent.putExtra("progress bar update", true);
        intent.putExtra("progress bar progress", progressBarValue);
        intent.putExtra("asyncTask","packaging files");
        LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent);
        Log.i("service", "data sent to main");
    }

    @Override
    protected void onPostExecute(Integer integer) {
        super.onPostExecute(integer);
        delegate.processFinish(integer);
    }
}
