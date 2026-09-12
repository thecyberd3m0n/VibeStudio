package com.vibestudio.app.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "vibestudio.db";
    private static final int DATABASE_VERSION = 2;

    public static final String TABLE_API_KEYS = "api_keys";
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_PROVIDER = "provider";
    public static final String COLUMN_KEY_VALUE = "key_value";

    public static final String TABLE_SETTINGS = "settings";
    public static final String COLUMN_SETTING_KEY = "setting_key";
    public static final String COLUMN_SETTING_VALUE = "setting_value";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createApiKeysTable = "CREATE TABLE " + TABLE_API_KEYS + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_PROVIDER + " TEXT UNIQUE NOT NULL, " +
                COLUMN_KEY_VALUE + " TEXT NOT NULL" +
                ");";

        String createSettingsTable = "CREATE TABLE " + TABLE_SETTINGS + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_SETTING_KEY + " TEXT UNIQUE NOT NULL, " +
                COLUMN_SETTING_VALUE + " TEXT NOT NULL" +
                ");";

        db.execSQL(createApiKeysTable);
        db.execSQL(createSettingsTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_API_KEYS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SETTINGS);
        onCreate(db);
    }

    public void saveApiKey(String provider, String key) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_PROVIDER, provider);
        values.put(COLUMN_KEY_VALUE, key);

        int rows = db.update(TABLE_API_KEYS, values, COLUMN_PROVIDER + "=?", new String[]{provider});
        if (rows == 0) {
            db.insert(TABLE_API_KEYS, null, values);
        }
        db.close();
    }

    public String getApiKey(String provider) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_API_KEYS, new String[]{COLUMN_KEY_VALUE},
                COLUMN_PROVIDER + "=?", new String[]{provider},
                null, null, null);

        String key = null;
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                key = cursor.getString(0);
            }
            cursor.close();
        }
        db.close();
        return key;
    }

    public void setSetting(String key, String value) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_SETTING_KEY, key);
        values.put(COLUMN_SETTING_VALUE, value);

        int rows = db.update(TABLE_SETTINGS, values, COLUMN_SETTING_KEY + "=?", new String[]{key});
        if (rows == 0) {
            db.insert(TABLE_SETTINGS, null, values);
        }
        db.close();
    }

    public String getSetting(String key) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_SETTINGS, new String[]{COLUMN_SETTING_VALUE},
                COLUMN_SETTING_KEY + "=?", new String[]{key},
                null, null, null);

        String val = null;
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                val = cursor.getString(0);
            }
            cursor.close();
        }
        db.close();
        return val;
    }

    public boolean isEnvInitialized() {
        String initialized = getSetting("env_initialized");
        return "true".equals(initialized);
    }

    public void setEnvInitialized(boolean initialized) {
        setSetting("env_initialized", initialized ? "true" : "false");
    }
}
