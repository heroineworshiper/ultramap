package com.ultramap;

import java.util.Formatter;
import java.util.List;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.android.gms.maps.GoogleMap;

public class IntervalTraining extends WindowBase
{
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Main.initialize(this);
        setContentView(R.layout.interval_training);
        
        updateButton();

        dbToList();

// read times from database
        
        
        
        updateGUI();

    }
    
    void dbToList()
    {
    	GridView listView = (GridView) findViewById(R.id.interval_list);
        ArrayAdapter<String> data = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
        SQLiteDatabase db = Settings.db.getWritableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + IntervalDB.TABLE_NAME, null);
        if (cursor.moveToFirst()) {
        	do {
        		double duration = cursor.getFloat(1);
//        		Log.v("IntervalTraining", "dbToList duration=" + duration);
        		double pace = cursor.getFloat(2);
        		data.add(Integer.toString((int)duration) + "s");

// convert to seconds per mile
        		pace = Main.miToM(pace);
        		
        		data.add(Integer.toString((int)(pace / 60)) + "m" +
        				(int)(pace % 60) + "s/mile");
            } while (cursor.moveToNext());
        }
        listView.setAdapter(data); 
        
    }

	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.interval_menu, menu);
        
        return true;
    }


    public boolean onOptionsItemSelected(MenuItem item) 
    {
    	switch(item.getItemId())
    	{
//			case R.id.menu_settings: {
//				Intent i = new Intent(this, SettingsWin.class);
//				i.setFlags(i.getFlags() | Intent.FLAG_ACTIVITY_NO_HISTORY);
//				this.startActivity(i);
//				return true;
//			}

			case R.id.menu_interval_settings: {
				Intent i = new Intent(this, IntervalSettings.class);
				i.setFlags(i.getFlags() | Intent.FLAG_ACTIVITY_NO_HISTORY);
				this.startActivity(i);
				return true;
			}


    	case R.id.menu_resetinterval:
        	reset();
    		break;
    	
    	default:
    		return Main.onOptionsItemSelected(this, item);
    	}
		return false;
    	
    }

	public void reset() {
		
		

    	Builder dialog = new AlertDialog.Builder(this);
    	
    	dialog.setMessage("Reset interval training?");
    	dialog.setPositiveButton("Yes", new DialogInterface.OnClickListener() 
    	{
            public void onClick(DialogInterface dialog, int which) 
            {
        		Button button = (Button)findViewById(R.id.intervalButton);
        		button.setText("Start");


				Main.resetIntervals();
        		dbToList();
            }

        });
    	dialog.setNegativeButton("No", null);
    	dialog.show();


    	
    	
	}
    
    
    public void onClick(View view)
    {
    	
    	
        switch (view.getId()){
        case R.id.intervalButton:
        	Main.handleIntervalButton();
        	updateButton();
        	break;
        }
    }
    
    void updateButton()
    {
		Button button = (Button)findViewById(R.id.intervalButton);
		if(!Settings.intervalActive) 
			button.setText("Start");
		else
			button.setText("Pause");

    }

	public void updateGUI()
	{
        TextView title = (TextView) findViewById(R.id.elapsed);
        
        if(Settings.intervalState == Settings.COUNTDOWN)
        {
        	title.setText(new Formatter().format("%ds", 
            		Settings.intervalCountdown + 1).toString());
        }
        else
        if((Settings.intervalState == Settings.WORK &&
        		Settings.workUnits != Settings.SECONDS) ||
        	(Settings.intervalState == Settings.REST &&
        		Settings.restUnits != Settings.SECONDS))
        {
// get distance without rounding
        	double miles = Main.mToMi(Main.getIntervalDistance()) * 100;
        	miles = Math.floor(miles) / 100;
        	
//        	Log.v("IntervalTraining", "updateGUI 1");
// This rounds up
        	title.setText(new Formatter().format("%.2fmi", 
        		miles).toString());
        	
        }
        else
        {
        	title.setText(new Formatter().format("%ds", 
        		Settings.intervalTimer.getTime() / 1000).toString());
        	
        }
        
        if(Settings.intervalDBChanged)
        {
        	Settings.intervalDBChanged = false;
        	dbToList();
        }
	}
	
	
    
    
}
