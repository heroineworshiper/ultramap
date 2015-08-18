package com.ultramap;

import java.util.Formatter;

import com.google.android.gms.maps.GoogleMap;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

public class IntervalSettings extends WindowBase implements OnItemSelectedListener {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Main.initialize(this);
        setContentView(R.layout.interval_settings);
        updateText();

        updateButton();
        
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.work_unit_options, 
                R.layout.spinner_layout);

        Spinner menu = (Spinner) findViewById(R.id.work_unit_spinner);
        menu.setAdapter(adapter);
        menu.setOnItemSelectedListener(this);
        menu.setSelection(Settings.workUnits);
      
        menu = (Spinner) findViewById(R.id.rest_unit_spinner);
        menu.setAdapter(adapter);
        menu.setOnItemSelectedListener(this);
        menu.setSelection(Settings.restUnits);

        EditText text = (EditText)findViewById(R.id.work_amount);
        text.addTextChangedListener(new TextWatcher()
		{

			@Override
			public void afterTextChanged(Editable e) {
				try
				{
					switch(Settings.workUnits)
					{
			        case Settings.METERS:
			        	Settings.intervalWork = Float.valueOf(e.toString());
			        	break;
			        case Settings.MILES:
			        	Settings.intervalWork = Main.miToM(Float.valueOf(e.toString()));
			        	break;
			        case Settings.SECONDS:
			        	Settings.intervalWorkTime = Integer.parseInt(e.toString());
			        	break;
					
					}
				}catch(Exception x)
				{
				}

				Settings.save(getApplicationContext());
//				Log.v("SettingsWin", "work_amount=" + e.toString());
			}

			@Override
			public void beforeTextChanged(CharSequence arg0, int arg1,
					int arg2, int arg3) {
				
			}

			@Override
			public void onTextChanged(CharSequence arg0, int arg1, int arg2,
					int arg3) {
				
			}
        	
        });
        text = (EditText)findViewById(R.id.rest_amount);
        text.addTextChangedListener(new TextWatcher()
		{

 			@Override
 			public void afterTextChanged(Editable e) {
				try
				{
					switch(Settings.restUnits)
					{
			        case Settings.METERS:
			        	Settings.intervalRestDistance = Float.valueOf(e.toString());
			        	break;
			        case Settings.MILES:
			        	Settings.intervalRestDistance = Main.miToM(Float.valueOf(e.toString()));
			        	break;
			        case Settings.SECONDS:
			        	Settings.intervalRest = Integer.parseInt(e.toString());
			        	break;
					
					}
				} catch(Exception x)
				{
				}
				Settings.save(getApplicationContext());
// 				Log.v("SettingsWin", "rest_amount=" + e.toString());
 			}

 			@Override
 			public void beforeTextChanged(CharSequence arg0, int arg1,
 					int arg2, int arg3) {
 				
 			}

 			@Override
 			public void onTextChanged(CharSequence arg0, int arg1, int arg2,
 					int arg3) {
 				
 			}
         	
         });

    }


    public boolean onOptionsItemSelected(MenuItem item) 
    {
    	return Main.onOptionsItemSelected(this, item);
    }
    
   
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.settings_menu, menu);
        return true;
    }
    
    void updateText()
    {
        EditText text = (EditText)findViewById(R.id.work_amount);
        
        switch(Settings.workUnits)
        {
        case Settings.METERS:
            text.setText(new Formatter().format("%d", 
            		(int)Settings.intervalWork).toString());
            break;
        case Settings.MILES:
            text.setText(new Formatter().format("%.2f", 
            		Main.mToMi(Settings.intervalWork)).toString());
        	break;
        case Settings.SECONDS:
            text.setText(new Formatter().format("%d", 
            		Settings.intervalWorkTime).toString());
            break;
        }
        
        
        
        
        text = (EditText)findViewById(R.id.rest_amount);
        switch(Settings.restUnits)
        {
        case Settings.METERS:
            text.setText(new Formatter().format("%d", 
            		(int)Settings.intervalRestDistance).toString());
            break;
        case Settings.MILES:
            text.setText(new Formatter().format("%.2f", 
            		Main.mToMi(Settings.intervalRestDistance)).toString());
        	break;
        case Settings.SECONDS:
            text.setText(new Formatter().format("%d", 
            		Settings.intervalRest).toString());
            break;
        }
        
    	
    }


	@Override
	public void onItemSelected(AdapterView<?> parent, 
			View view, 
            int pos, 
            long id) 
	{
    	switch (parent.getId()){
    	case R.id.rest_unit_spinner:
    		Settings.restUnits = pos;
    		Settings.save(Main.context);
    		break;
    	case R.id.work_unit_spinner:
    		Settings.workUnits = pos;
    		Settings.save(Main.context);
    		break;
    	}
		
    	updateText();
	}


	@Override
	public void onNothingSelected(AdapterView<?> arg0) {
		
		
	}


    
    public void onClick(View view)
    {
        switch (view.getId()){
        case R.id.intervalButton2:
        	Main.handleIntervalButton();
        	updateButton();
        	break;
        }
    }
    
    void updateButton()
    {
		Button button = (Button)findViewById(R.id.intervalButton2);
		if(!Settings.intervalActive) 
			button.setText("Start");
		else
			button.setText("Pause");

    }

	
}
