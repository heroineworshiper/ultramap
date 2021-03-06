/*
 * Ultramap
 * Copyright (C) 2021 Adam Williams <broadcast at earthling dot net>
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 * 
 */

package com.ultramap;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class IntervalDB extends SQLiteOpenHelper {
	static final String TABLE_NAME = "intervals";
	static final String KEY_ID = "id";
	static final String KEY_DURATION = "duration";
	static final String KEY_PACE = "pace";
	static final String KEY_DISTANCE = "distance";
	static final int VERSION = 5;

	
	IntervalDB(Context context)
	{
		super(context, "IntervalDB", null, VERSION);
		Log.v("IntervalDB", "IntervalDB");
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		Log.v("IntervalDB", "onCreate");
        String command = "CREATE TABLE " + TABLE_NAME + "(" +
                KEY_ID + " INTEGER PRIMARY KEY," + 
        		KEY_DURATION + " FLOAT," + 
        		KEY_PACE + " FLOAT," + 
        		KEY_DISTANCE + " FLOAT" + ")";
        db.execSQL(command);
		
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		Log.v("IntervalDB", "onUpgrade");
		// Drop older table if existed
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
 
        // Create tables again
        onCreate(db);
	}
	
	
	void addInterval(double duration, double pace, double distance)
	{
		SQLiteDatabase db = this.getWritableDatabase();
		ContentValues values = new ContentValues();
        values.put(KEY_DURATION, duration);
        values.put(KEY_PACE, pace);
        values.put(KEY_DISTANCE, distance);
		// Inserting Row
        db.insert(TABLE_NAME, null, values);
        db.close(); // Closing database connection
	}
	
	void updateInterval(double duration, double pace, double distance)
	{
		SQLiteDatabase db = this.getWritableDatabase();
// get last row
		Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_NAME + " ORDER BY " + KEY_ID + " DESC LIMIT 1", null);
		
		if (cursor.moveToFirst()) 
		{
			ContentValues values = new ContentValues();
        	values.put(KEY_DURATION, duration);
        	values.put(KEY_PACE, pace);
        	values.put(KEY_DISTANCE, distance);
        	Log.v("updateInterval", "id=" + cursor.getInt(0));
        	db.update(TABLE_NAME, values, "id=" + cursor.getInt(0), null);
		}
        db.close(); // Closing database connection
	}
	
	int totalIntervals()
	{
		SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_NAME, null);
        return cursor.getCount();
	}
	
}
