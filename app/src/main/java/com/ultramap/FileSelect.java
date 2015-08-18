package com.ultramap;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;

import android.os.Bundle;
import android.os.Environment;
import android.app.Activity;
import android.content.Intent;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class FileSelect extends WindowBase {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Main.initialize(this);
        setContentView(R.layout.file_select);
        success = false;
        
        try {
            boolean result = Settings.dir.mkdirs();
 //           Log.v("FileSelect", "onCreate 1 " + result);
        }
        catch(SecurityException e) {
            Log.v("FileSelect", "onCreate " + e.toString());
        }
        mFileList = Settings.dir.list();
        Arrays.sort(mFileList);
        
        // http://www.vogella.com/articles/AndroidListView/article.html
        ListView listView = (ListView) findViewById(R.id.listView1);

//        Log.v("FileSelect", "onCreate 2 " + 
//        		this + " " + 
//        		Environment.getRootDirectory() + " " +
//        		Environment.getDataDirectory() + " " +
//        		mPath + " " +
//        		mFileList);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
        		  android.R.layout.simple_list_item_1, 
        		  android.R.id.text1, 
        		  mFileList);
        listView.setAdapter(adapter); 
        
        listView.setOnItemClickListener(new ListView.OnItemClickListener() 
        {
        	public void onItemClick(AdapterView<?> parent, 
        		View view,
        	    int position, 
        	    long id) 
        	{
//        	    Log.v("FileSelect", "onItemClick " + position);
        	    String string = mFileList[position];
        	    TextView title = (TextView)findViewById(R.id.file_text);
        	    title.setText(string);
        	}


        });
        
        Log.v("FileSelect", "onCreate selectedFile=" + FileSelect.selectedFile);
        
        if(FileSelect.selectedFile != null)
        {
        	TextView title = (TextView)findViewById(R.id.file_text);
        	title.setText(FileSelect.selectedFile);
        }
    }
    
    public void onClick(View view)
    {
    	TextView title = (TextView)findViewById(R.id.file_text);
    	FileSelect.selectedFile = title.getText().toString();


        switch (view.getId())
        {
	        case R.id.file_ok:
	        {
//        	    Log.v("FileSelect", "onClick 1 " + FileSelect.selectedFile);
            	success = true;
	        }
	        break;
	        
	        case R.id.file_cancel:
	        {
        	    Log.v("FileSelect", "onClick 2");
        	    success = false;
	        }
	        break;
        }
        
        

        startActivity( new Intent(this, nextWindow));
    	return;
    }
    

    
    
// http://stackoverflow.com/questions/3592717/choose-file-dialog
    private String[] mFileList;
	static String selectedFile = null;
	static boolean success = false;
// next window after closing
    static Class nextWindow = null;
}
