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

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.Date;
import java.util.concurrent.Semaphore;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

// Started from http://cs.au.dk/~amoeller/WWW/javaweb/server.html

public class WebServer extends Thread 
{
	public void run()
	{
		
        ServerSocket socket = null; 
        try {
            socket = new ServerSocket(Settings.port); 
        } catch (IOException e) {
            Log.v("WebServer", "run: Could not start web server: " + e);
        }

        Log.v("WebServer", "run: started web server on port " + Settings.port);
        
        
        // request handler loop
        while (true) {
            Socket connection = null;
            try {
                // wait for request
                connection = socket.accept();
                Log.v("WebServer", "run: got connection");
                if(connection != null) startConnection(connection);
                
            } catch (IOException e) 
            { 
            	Log.v("WebServer", "run: " + e); 
            }
        }
    }               


	void startConnection(Socket connection)
	{
		WebServerThread thread = null;
		synchronized(this)
		{
			for(int i = 0; i < TOTAL_THREADS; i++)
			{
				if(threads[i] == null) 
					threads[i] = new WebServerThread();

				if(!threads[i].busy)
				{
					thread = threads[i];
					threads[i].startConnection(connection);
					break;
				}
			}
		}
		
		if(thread == null) 
		{
			Log.v("WebServer", "startConnection: out of threads");
			return;
		}
		

		
	}

    private static void log(Socket connection, String msg)
    {
        Log.v("WebServer", "log: " +
//        		new Date() + 
//        		" [" + 
//        		connection.getInetAddress().getHostAddress() +      
//        		":" + 
//        		connection.getPort() + 
//        		"] " + 
        		msg);
    }

    private static void errorReport(PrintStream pout, Socket connection,
                                    String code, String title, String msg)
    {
        pout.print("HTTP/1.0 " + code + " " + title + "\r\n" +
                   "\r\n" +
                   "<!DOCTYPE HTML PUBLIC \"-//IETF//DTD HTML 2.0//EN\">\r\n" +
                   "<TITLE>" + code + " " + title + "</TITLE>\r\n" +
                   "</HEAD><BODY>\r\n" +
                   "<H1>" + title + "</H1>\r\n" + msg + "<P>\r\n" +
                   "<HR><ADDRESS>Ultramap at " + 
                   connection.getLocalAddress().getHostName() + 
                   " Port " + connection.getLocalPort() + "</ADDRESS>\r\n" +
                   "</BODY></HTML>\r\n");
        log(connection, code + " " + title);
    }            
                
    private static String guessContentType(String path)
    {
        if (path.endsWith(".js")) 
            return "application/javascript";
        if (path.endsWith(".html") || path.endsWith(".htm")) 
            return "text/html";
        else if (path.endsWith(".txt") || path.endsWith(".java")) 
            return "text/plain";
        else if (path.endsWith(".gif")) 
            return "image/gif";
        else if (path.endsWith(".class"))
            return "application/octet-stream";
        else if (path.endsWith(".jpg") || path.endsWith(".jpeg"))
            return "image/jpeg";
        else    
            return "text/plain";
    }

    void sendHeader(PrintStream pout, String contentType)
    {
    	pout.print("HTTP/1.0 200 OK\r\n" +
     	   "Content-Type: " + contentType + "\r\n" +
     	   "Date: " + new Date() + "\r\n" +
     	   "Server: Ultramap\r\n\r\n");
    }

	void sendFileList(PrintStream pout, String req, File dir)
	{
		String[] mFileList = dir.list();
        Arrays.sort(mFileList);
		
		JSONObject jsonObject = new JSONObject();

		try {
			
			JSONArray jsonArray = new JSONArray();
			for(int i = 0; i < mFileList.length; i++)
			{
				JSONArray jsonRow = new JSONArray();
				File file =  new File(dir.getPath() + "/" + mFileList[i]);
				long date = file.lastModified();
				long size = file.length();
				
				Log.v("sendFileList", "file=" + file + " date=" + date + " size=" + size);
				jsonRow.put(mFileList[i]);
				jsonRow.put(size);
				jsonRow.put(date);
				
				jsonArray.put(jsonRow);
			}
			
		
			jsonObject.put("files", jsonArray);
		} catch (JSONException e) {
			e.printStackTrace();
		}





		sendHeader(pout, "application/json");
		int start = req.indexOf("callback=");
		int end;
		String callback = "";
		if(start >= 0) 
		{
			start += 9;
			end = req.indexOf('&', start);
			if(end >= start)
				callback = req.substring(start, end);
		}
		
		
		pout.print(callback + 
			"(" + 
			jsonObject.toString() + 
			")\r\n");

			
// example return value
// 		pout.print(callback + 
// 			"({\r\n" +
// 				"\t\"anotherKey\": \"anotherValue\",\r\n" +
// 				"\t\"key\": \"value\",\r\n" +
// 				"\t\"items\": \r\n" +
// 				"\t[\r\n" +
// 				"\t\t{\r\n" +
// 				"\t\t\t\"path\": \"workout_\",\r\n" +
// 				"\t\t\t\"date\": \"x\",\r\n" +
// 				"\t\t\t\"distance\": \"y\",\r\n" +
// 				"\t\t\t\"time\": \"z\"\r\n" +
// 				"\t\t}\r\n" +
// 				"\t]\r\n" +
// 			"})\r\n");
	}

	void sendWorkout(PrintStream pout, String req)
	{
		
// 		InputStream file = new FileInputStream(f);
// 		sendHeader(pout, "text/plain");
// 		sendFile(file, out); // send raw file 
// 		log(connection, "200 OK");
		
		
	}
	
	
	private static void sendFile(InputStream file, OutputStream out)
	{
	    try {
	        byte[] buffer = new byte[1024];
	        while (file.available()>0) 
	            out.write(buffer, 0, file.read(buffer));
	    } catch (IOException e) 
		{ 
			Log.v("WebServer", "sendFile " + e); 
		}
	}
	
	public class WebServerThread extends Thread
	{
		public WebServerThread()
		{
			start();
		}
		
		public void startConnection(Socket connection)
		{
			this.connection = connection;
			busy = true;
			lock.release();
		}

		
		
		public void run()
		{
			while(true)
			{
				try {
					lock.acquire();
				
					
//					Log.v("WebServerThread", "run: running");
			    	BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
			    	OutputStream out = new BufferedOutputStream(connection.getOutputStream());
			    	PrintStream pout = new PrintStream(out);  
			    	
			    	
			    	// read first line of request (ignore the rest)
			    	String request = in.readLine();
			    	if (request==null)
			    	{
			            if (connection != null) connection.close(); 
			    		continue;
			    	}
					
			    	log(connection, request);
			    	while (true) {
			        	String misc = in.readLine();
			        	if (misc==null || misc.length()==0)
			            	break;
			    	}

			    	// parse the line
			    	if (!request.startsWith("GET") || 
						request.length() < 14 ||
			        	!(request.endsWith("HTTP/1.0") || 
						request.endsWith("HTTP/1.1"))) 
					{
			        	// bad request
			        	errorReport(pout, connection, "400", "Bad Request", 
			                    	"Your browser sent a request that " + 
			                    	"this server could not understand.");
			    	} 
					else 
					{
			        	String req = request.substring(4, request.length() - 9).trim();
//			        	Log.v("WebServer", "run request=" +  request + " req=" + req);
						
						if (req.indexOf("..")!=-1 || 
			            	req.indexOf("/.ht")!=-1 || req.endsWith("~")) {
			            	// evil hacker trying to read non-wwwhome or secret file
			            	errorReport(pout, connection, "403", "Forbidden",
			                        	"You don't have permission to access the requested URL.");
			        	} 
						else
						// test for database request
						if(req.startsWith("/workoutlist?"))
						{
							sendFileList(pout, req, Settings.dir);
						}
						else
						// test for database request
						if(req.startsWith("/camlist?"))
						{
							sendFileList(pout, req, Settings.camDir);
						}
						else
// ordinary file
						{
							String filename = req;
							int end = filename.indexOf('?');
			            	if(end >= 0) filename = filename.substring(0, end);
			            	
// try html directory
			            	String path = Settings.wwwhome + "/" + filename;
			            	if(!(new File(path).exists()))
			            	{
			            		// try ultramap directory 
			            		path = Settings.dir + "/" + filename;
			            		
			            		if(!(new File(path).exists()))
			            		{
			            			// try cam directory
			            			path = Settings.camDir + "/" + filename;
			            		}
			            	}
			            	
			            	
			            	
			            	
			            	
			            	Log.v("WebServer", "run path=" + path);
			            	
			            	File f = new File(path);
			            	if (f.isDirectory() && !path.endsWith("/")) {
			                	// redirect browser if referring to directory without final '/'
			                	pout.print("HTTP/1.0 301 Moved Permanently\r\n" +
			                        	   "Location: http://" + 
			                        	   connection.getLocalAddress().getHostAddress() + ":" +
			                        	   connection.getLocalPort() + "/" + req + "/\r\n\r\n");
			                	log(connection, "301 Moved Permanently");
			            	} else {
			                	if (f.isDirectory()) { 
			                    	// if directory, implicitly add 'index.html'
			                    	path = path + "index.html";
			                    	f = new File(path);
			                	}
			                	
			                	
			                	try { 
			                    	// send file
			                    	InputStream file = new FileInputStream(f);
			                    	sendHeader(pout, guessContentType(path));
			                    	sendFile(file, out); // send raw file 
			                    	log(connection, "200 OK");
			                	} catch (FileNotFoundException e) { 
			                    	// file not found
			                    	errorReport(pout, connection, "404", "Not Found",
			                                	"The requested URL was not found on this server.");
			                	}
			            	}
			        	}
			    	}                
			    	out.flush();         
			    	
			    	Log.v("WebServerThread", "run: finished");
	                if (connection != null) connection.close(); 
				} catch(Exception e)
				{
					Log.v("WebServerThread", "run " + e);
				}

			
				busy = false;
	    	}
		}
		
		boolean busy = false;
		Socket connection;
		Semaphore lock = new Semaphore(0);
	}
	
	int TOTAL_THREADS = 20;
	WebServerThread threads[] = new WebServerThread[TOTAL_THREADS];

}
