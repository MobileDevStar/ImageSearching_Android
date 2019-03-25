package com.anna.picturematching;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import org.opencv.core.Mat;

public class SqlTable extends SQLiteOpenHelper {
    String table = "mydb";

    public SqlTable(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("create table "+table+" (name TEXT UNIQUE, tD INTEGER, wD INTEGER, hD INTEGER, pixD BLOB);");
    }

    @Override
    public void onUpgrade(SQLiteDatabase arg0, int arg1, int arg2) {
    }

    public void dbPut(String name, Mat m) {
        long nbytes = m.total() * m.elemSize();
        byte[] bytes = new byte[ (int)nbytes ];
        m.get(0, 0,bytes);

        dbPut(name, m.type(), m.cols(), m.rows(), bytes);
    }

    public void dbPut(String name, int t, int w, int h, byte[] bytes) {
        Log.d("dbput", name + " " + t + " " + w + "x" + h);
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("name", name);
        values.put("tD", t);
        values.put("wD", w);
        values.put("hD", h);
        values.put("pixD", bytes);
        db.insert(table, null, values);
        db.close();
    }

    public Mat dbGet(String name) {
        SQLiteDatabase db = this.getReadableDatabase();
        String [] columns = {"t","w","h","pix"};
        Cursor cursor = db.query(table,columns," name = ?",
                new String[] { name }, // d. selections args
                null, // e. group by
                null, // f. having
                null, // g. order by
                null); // h. limit

        if (cursor != null)
            cursor.moveToFirst();

        int t = cursor.getInt(0);
        int w = cursor.getInt(1);
        int h = cursor.getInt(2);
        byte[] p = cursor.getBlob(3);
        Mat m = new Mat(h,w,t);
        m.put(0,0,p);
        Log.d("dbget("+name+")", m.toString());
        return m;
    }
    public Cursor dbGetAll() {
        SQLiteDatabase db = this.getReadableDatabase();
        String [] columns = {"name", "tD","wD","hD","pixD"};
        Cursor cursor = db.query(table,columns,null,
                null, // d. selections args
                null, // e. group by
                null, // f. having
                null, // g. order by
                null); // h. limit

        if (cursor != null)
            cursor.moveToFirst();
//
//        int t = cursor.getInt(0);
//        int w = cursor.getInt(1);
//        int h = cursor.getInt(2);
//        byte[] p = cursor.getBlob(3);
//        Mat m = new Mat(h,w,t);
//        m.put(0,0,p);
//        Log.d("dbget("+name+")", m.toString());
        return cursor;
    }

    public void dbPutImg(String name, Mat dM) {

        long nbytes = dM.total() * dM.elemSize();
        byte[] dbytes = new byte[ (int)nbytes ];
        dM.get(0, 0, dbytes);

        dbPutImg(name, dM.type(), dM.cols(), dM.rows(), dbytes);
    }

    public void dbPutImg(String name, int tD, int wD, int hD, byte[] bytesD) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("name", name);
        values.put("tD", tD);
        values.put("wD", wD);
        values.put("hD", hD);
        values.put("pixD", bytesD);

        db.insert(table, null, values);
        db.close();
    }
}
