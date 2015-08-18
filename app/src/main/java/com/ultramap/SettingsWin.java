package com.ultramap;

import java.util.Formatter;

import com.google.android.gms.maps.GoogleMap;
import com.ultramap.R;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

public class SettingsWin extends WindowBase implements OnItemSelectedListener
{
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Main.initialize(this);
        setContentView(R.layout.settings);

        EditText text;

        CheckBox checkbox;
        checkbox = (CheckBox) findViewById(R.id.follow_position);
        checkbox.setChecked(Settings.followPosition);
        checkbox = (CheckBox) findViewById(R.id.keep_alive);
        checkbox.setChecked(Settings.enableService);
        checkbox = (CheckBox) findViewById(R.id.external_gps);
        checkbox.setChecked(Settings.externalGPS);
        checkbox = (CheckBox) findViewById(R.id.voice_feedback);
        checkbox.setChecked(Settings.voiceFeedback);

        double distance = Main.mToMi(Main.getDistance(Main.settings.route));
        
        
        TextView title = (TextView) findViewById(R.id.distance);
        title.setText(new Formatter().format("%.2fmi", distance).toString());
        title = (TextView) findViewById(R.id.points);
        title.setText(new Formatter().format("%d", Main.settings.route.size()).toString());
 
        distance = Main.mToMi(Main.getDistance(Main.settings.log));
        
        title = (TextView) findViewById(R.id.log_distance);
        title.setText(new Formatter().format("%.2fmi", distance).toString());
        title = (TextView) findViewById(R.id.log_points);
        title.setText(new Formatter().format("%d", Main.settings.log.size()).toString());
 
        title = (TextView) findViewById(R.id.bluetooth_status);
        title.setText(Main.bluetoothStatus);
        
        
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.map_type_options, 
                R.layout.spinner_layout);

        Spinner menu = (Spinner) findViewById(R.id.map_type_spinner);
        menu.setAdapter(adapter);
        menu.setOnItemSelectedListener(this);
        switch(Settings.mapType)
        {
        case GoogleMap.MAP_TYPE_HYBRID:
        	menu.setSelection(0);
        	break;
        case GoogleMap.MAP_TYPE_NORMAL:
        	menu.setSelection(1);
        	break;
        case GoogleMap.MAP_TYPE_SATELLITE:
        	menu.setSelection(2);
        	break;
        case GoogleMap.MAP_TYPE_TERRAIN:
        	menu.setSelection(3);
        	break;
        default:
        	menu.setSelection(0);
        	break;
        }

        
//        Log.v("SettingsWin", "onCreate");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.settings_menu, menu);
        return true;
    }


    public boolean onOptionsItemSelected(MenuItem item) 
    {
    	return Main.onOptionsItemSelected(this, item);
    }
    
    public void onClick(View view)
    {
    	CheckBox checkbox = null;
    	
    	
        switch (view.getId())
		{
        case R.id.follow_position:
        	checkbox = (CheckBox) findViewById(R.id.follow_position);
        	Settings.followPosition = checkbox.isChecked();
        	Settings.save(Main.context);
        	break;

        case R.id.keep_alive:
        	checkbox = (CheckBox) findViewById(R.id.keep_alive);
        	Settings.enableService = checkbox.isChecked();
        	Settings.save(Main.context);
        	Main.main.setAlarm();
        	break;

        case R.id.external_gps:
        	checkbox = (CheckBox) findViewById(R.id.external_gps);
        	Settings.externalGPS = checkbox.isChecked();
        	Settings.save(Main.context);
        	break;

        case R.id.voice_feedback:
        	checkbox = (CheckBox) findViewById(R.id.voice_feedback);
        	Settings.voiceFeedback = checkbox.isChecked();
        	Settings.save(Main.context);
        	break;
    	}
    }

	public void updateGUI()
	{
		if(Main.main == null) return;
		
        double distance = Main.mToMi(Main.getDistance(Main.settings.log));
        
        TextView title = (TextView) findViewById(R.id.log_distance);
        title.setText(new Formatter().format("%.2fmi", distance).toString());
        title = (TextView) findViewById(R.id.log_points);
        title.setText(new Formatter().format("%d", Main.settings.log.size()).toString());

        title = (TextView) findViewById(R.id.bluetooth_status);
        title.setText(Main.bluetoothStatus);

		if(Settings.route.size() == 0)
		{
			clearRouteDistance();
		}
	}
	
	public void clearRouteDistance()
	{
        TextView title = (TextView) findViewById(R.id.distance);
        title.setText(new Formatter().format("%.2fmi", 0.0).toString());
        title = (TextView) findViewById(R.id.points);
        title.setText(new Formatter().format("%d", 0).toString());
	}

	@Override
	public void onItemSelected(AdapterView<?> parent, 
			View view, 
            int pos, 
            long id) 
	{
    	switch (parent.getId()){
    	case R.id.map_type_spinner:
    		switch(pos)
    		{
    		case 0:
        		Settings.mapType = GoogleMap.MAP_TYPE_HYBRID;
        		break;
    		case 1:
        		Settings.mapType = GoogleMap.MAP_TYPE_NORMAL;
        		break;
    		case 2:
        		Settings.mapType = GoogleMap.MAP_TYPE_SATELLITE;
        		break;
    		case 3:
        		Settings.mapType = GoogleMap.MAP_TYPE_TERRAIN;
        		break;
    		}
    		
    		Settings.save(Main.context);
    		break;
    	}
    	
	}

	@Override
	public void onNothingSelected(AdapterView<?> arg0) 
	{
	}


}
