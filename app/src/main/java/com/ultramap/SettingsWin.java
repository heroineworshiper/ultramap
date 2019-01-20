package com.ultramap;

import java.util.Formatter;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.maps.GoogleMap;
import com.ultramap.R;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.NumberPicker;
import android.widget.Spinner;
import android.widget.TextView;

public class SettingsWin extends WindowBase implements OnItemSelectedListener
{
    final int MIN_TEMPO = 40;
    final int MAX_TEMPO = 90;
    final int windowUpdateDownsample = 5;
    int windowUpdateCounter = 0;

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
        checkbox = (CheckBox) findViewById(R.id.metronome);
        checkbox.setChecked(Settings.metronome);
        checkbox = (CheckBox) findViewById(R.id.flashlight);
        checkbox.setChecked(Settings.flashlight);

        EditText number = (EditText)findViewById(R.id.beats_per_minute);
        number.setText(Integer.toString(Settings.beatsPerMinute));
        number.setOnEditorActionListener(new EditText.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                Log.v("SettingsWin", "onEditorAction " + v.getText());
                int newTempo = Integer.getInteger(v.getText().toString());
                if(newTempo >= MIN_TEMPO && newTempo <= MAX_TEMPO)
                {
                    Settings.beatsPerMinute = newTempo;
                    Settings.save(Main.context);
                }
                return false;
            }
        });


        double distance = Main.mToMi(Main.getDistance(Main.settings.route));
        
        
        TextView title = (TextView) findViewById(R.id.distance);
        title.setText(new Formatter().format("%.2fmi (%d)", distance, Main.settings.route.size()).toString());
        title = (TextView) findViewById(R.id.points);
        title.setText(new Formatter().format("%d", Main.settings.route.size()).toString());
 
        
        title = (TextView) findViewById(R.id.bluetooth_status);
        title.setText(Main.bluetoothStatus);


        ArrayAdapter<CharSequence> adapter;
        Spinner menu;
        adapter = ArrayAdapter.createFromResource(this,
                R.array.sounds,
                R.layout.spinner_layout);
        menu = (Spinner) findViewById(R.id.sound_spinner);
        menu.setAdapter(adapter);
        menu.setOnItemSelectedListener(this);
        menu.setSelection(Settings.sound);


        adapter = ArrayAdapter.createFromResource(this,
                R.array.map_type_options, 
                R.layout.spinner_layout);

        menu = (Spinner) findViewById(R.id.map_type_spinner);
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

        updateButtonText();







        number = (EditText)findViewById(R.id.cutoff_time);
        number.setText(Integer.toString(Settings.cutoffTime / 3600) + "h" + 
			Integer.toString(Settings.cutoffTime % 60) + "m");
        number.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable e) {
                Log.v("SettingsWin", "onEditorAction " + e.toString());

                String input = e.toString();
                String hString = new String();
                String mString = new String();
                int ptr = 0;
                while(ptr < input.length() && input.charAt(ptr) != 'h')
                {
                    hString += input.charAt(ptr);
                    ptr++;
                }

                while(ptr < input.length() && !Character.isDigit(input.charAt(ptr)) )
                {
                    ptr++;
                }

                while(ptr < input.length() && input.charAt(ptr) != 's')
                {
                    mString += input.charAt(ptr);
                    ptr++;
                }

                int hours = 0;
                int minutes = 0;

                try {
                    hours = Integer.parseInt(hString);
                    minutes = Integer.parseInt(mString);
                }catch(Exception x)
                {
                }


                Log.i("SettingsWin", "hString=" + hString + " mString=" + mString);

                Settings.cutoffTime = minutes * 60 + hours * 3600;
                Settings.save(Main.context);

                updateDistances();
            }
        });




        number = (EditText)findViewById(R.id.cutoff_distance);

        number.setText(new Formatter().format("%.2f", Main.mToMi(Settings.cutoffDistance)).toString());
        number.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable e) {
                Log.v("SettingsWin", "onEditorAction " + e.toString());
                double newDistance = Float.parseFloat(e.toString());
                Settings.cutoffDistance = (int)Main.miToM(newDistance);
                Settings.save(Main.context);

                updateDistances();
            }

        });



		updateDistances();


		
		
// A route was selected
        Log.v("Map", "onCreate FileSelect.success=" + FileSelect.success + 
        		" FileSelect.selectedFile=" + FileSelect.selectedFile);
        if(FileSelect.success &&
        		FileSelect.selectedFile != null)
        {
        	if(Settings.selectSave)
        	{
        		Main.saveRoute(Settings.dir + "/" + FileSelect.selectedFile);
        	}
        	else
        	if(Settings.selectSaveLog)
        	{
        		Main.saveLog(Settings.dir + "/" + FileSelect.selectedFile);
        	}
        }

        Settings.selectLoad = false;
        Settings.selectSave = false;
        Settings.selectSaveLog = false;
        
//        Log.v("SettingsWin", "onCreate");
    }


    public void onResume() {
        super.onResume();
        updateButtonText();
    }

    void updateButtonText()
    {

        Button button = (Button)findViewById(R.id.settings_pause);
        if(button != null)
        {
            if(Settings.recordRoute)
            {
                button.setText("Pause");
            }
            else
            {
                button.setText("Record");
            }
        }
    }


	void updateDistances()
	{
		double recordedDistance = Main.mToMi(Main.getDistance(Main.settings.log));
        TextView title = (TextView) findViewById(R.id.log_distance);
        title.setText(new Formatter().format("%.2fmi (%d)", recordedDistance, Main.settings.log.size()).toString());
        title = (TextView) findViewById(R.id.log_points);
        title.setText(new Formatter().format("%d", Main.settings.log.size()).toString());

//        Log.i("updateDistances", "cutoffTime=" + Settings.cutoffTime + " cutoffDistance=" + Settings.cutoffDistance);

		RoutePoint point1 = null;
		double distance = 0;
		if(Settings.log.size() > 0)
		{
			point1 = Settings.log.get(Settings.log.size() - 1);
			distance = point1.distance;
		}
		
		String text = new String();
        long duration = 0;

        if(distance > 0 && point1 != null)
		{
			duration = point1.relativeTime;
			double pace = duration / distance;
			pace = Main.miToM(pace);
			text = new Formatter().format("%dm%ds",
				(int)(pace / 60),
				(int)(pace % 60)).toString();
		}
		else
		{
			text = "Unknown";
		}
	
		title = (TextView) findViewById(R.id.total_pace);
		title.setText(text);
		
		double distanceLeft = Settings.cutoffDistance - distance;
		if(distanceLeft > 0)
		{
			double timeLeft = Settings.cutoffTime - duration;
			double pace = timeLeft / distanceLeft;
			pace = Main.miToM(pace);
			text = new Formatter().format("%dm%ds",
				(int)(pace / 60),
				(int)(pace % 60)).toString();
		}
		else
		{
			text = "Unknown";
		}
		
		title = (TextView) findViewById(R.id.remaneing_pace);
		title.setText(text);
		
		
	}
	
	
	
	
	

    void toggleRecording()
    {
        Main.toggleRecording();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.settings_menu, menu);
        return true;
    }


    public boolean onOptionsItemSelected(MenuItem item) 
    {
        switch(item.getItemId())
        {
            case R.id.menu_interval: {
                Intent i = new Intent(this, IntervalTraining.class);
                //i.setFlags(i.getFlags() | Intent.FLAG_ACTIVITY_NO_HISTORY);
                this.startActivity(i);
                return true;
            }

            case R.id.settings_saveroute: {
                Settings.saveGPX = false;
                Settings.selectLoad = false;
                Settings.selectSave = true;
                Settings.selectSaveLog = false;
                FileSelect.nextWindow = SettingsWin.class;

                Intent i = new Intent(Main.context, FileSelect.class);
                i.setFlags(i.getFlags() | Intent.FLAG_ACTIVITY_NO_HISTORY);
                startActivity(i);
                break;
            }

            case R.id.settings_savelog:
                FileSelect.nextWindow = SettingsWin.class;
                Main.saveRoute(false);
                break;

//    	case R.id.menu_savegpx:
//    		saveRoute(true);
//    		break;

//    	case R.id.menu_recordlog:
//    		toggleRecording();
//    		item.setChecked(Settings.recordRoute);
//
//    		break;



            default:
                return Main.onOptionsItemSelected(this, item);
        }

        return false;
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

            case R.id.metronome:
                checkbox = (CheckBox) findViewById(R.id.metronome);
                Settings.metronome = checkbox.isChecked();
                Settings.save(Main.context);
                break;

            case R.id.flashlight:
                checkbox = (CheckBox) findViewById(R.id.flashlight);
                Settings.flashlight = checkbox.isChecked();
                Settings.save(Main.context);
                Main.main.updateFlashlight();
                break;

            case R.id.tempo_minus: {
                EditText number = (EditText) findViewById(R.id.beats_per_minute);
                if(Settings.beatsPerMinute > MIN_TEMPO)
                {
                    Settings.beatsPerMinute--;
                    Settings.save(Main.context);
                    number.setText(Integer.toString(Settings.beatsPerMinute));
                }

                break;
            }

            case R.id.tempo_plus: {
                EditText number = (EditText) findViewById(R.id.beats_per_minute);
                if(Settings.beatsPerMinute < MAX_TEMPO)
                {
                    Settings.beatsPerMinute++;
                    Settings.save(Main.context);
                    number.setText(Integer.toString(Settings.beatsPerMinute));
                }

                break;
            }

            case R.id.settings_pause:
                toggleRecording();
                updateButtonText();
                break;
        }
    }

	public void updateGUI()
	{
		if(Main.main == null) return;
        windowUpdateCounter++;
        if(windowUpdateCounter < windowUpdateDownsample)
        {
            return;
        }
        windowUpdateCounter = 0;

		updateDistances();
		
        TextView title;
        title = (TextView) findViewById(R.id.bluetooth_status);
        title.setText(Main.bluetoothStatus);

		if(Settings.route.size() == 0)
		{
			clearRouteDistance();
		}


        TextView debugText = (TextView)findViewById(R.id.debug_text);
        if(debugText != null)
        {
            debugText.setText(new Formatter().format("have location=%s\nabstime=%d\nreltime=%d\nreltimeout=%d",
                    Main.haveLocation ? "true" : "false",
                    Main.locationTime % 60000,
                    Main.lastLocationTimer.getTime(),
                    Main.locationTimeout).toString());
        }
	}

	public void clearRouteDistance()
	{
        TextView title = (TextView) findViewById(R.id.distance);
        title.setText(new Formatter().format("%.2fmi (0)", 0.0).toString());
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
            case R.id.sound_spinner:
                Settings.sound = pos;
                Settings.save(Main.context);
                break;

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
