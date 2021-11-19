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
        if(mFileList != null)
        {
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

        }



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
        
// only used in Map, so go back
        finish();


//         Intent i = new Intent(this, nextWindow);
//         i.setFlags(i.getFlags() | Intent.FLAG_ACTIVITY_NO_HISTORY);
//         startActivity(i);
    }
    

    
    
// http://stackoverflow.com/questions/3592717/choose-file-dialog
    private String[] mFileList;
	static String selectedFile = null;
	static boolean success = false;
// next window after closing
    static Class nextWindow = null;
}
