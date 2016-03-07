
package com.ultramap;


import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaPlayer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class Metronome extends Thread
{

//	MediaPlayer mediaPlayer = null;
//	StreamProxy proxy = null;
	final int BUFFER_SIZE = 32768;
	final int SAMPLE_RATE = 44100;
	final int HEADER_SIZE = 44;
	byte [] hwData = new byte[BUFFER_SIZE];
	byte [] loopData;
	boolean done = false;
	int loopSize = 0;
	int readOffset = 0;
	AudioTrack atrack;
	
	Metronome()
	{
		loopSize = SAMPLE_RATE * 60 / Settings.beatsPerMinute;
		loopData = new byte[loopSize];
		InputStream sound = Main.main.getResources().openRawResource(Settings.soundToFile(Settings.sound));
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		int nRead;
		byte [] data = new byte[16384];
		try {
			while((nRead = sound.read(data, 0, data.length)) != -1)
            {
                buffer.write(data, 0, nRead);
            }

			buffer.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
		byte [] sound2 = buffer.toByteArray();


		for(int i = HEADER_SIZE;
			i < sound2.length && i - HEADER_SIZE < loopData.length;
			i++)
		{
			loopData[i - HEADER_SIZE] = sound2[i];
		}
		
		for(int i = sound2.length - HEADER_SIZE; i < loopData.length; i++)
		{
			loopData[i] = (byte)0x80;
		}
		
// 		StreamProxy proxy = new StreamProxy();
// 		proxy.setData(Loopback.data);
// 		proxy.init();
// 		proxy.start();
// 		String proxyUrl = String.format("http://127.0.0.1:%d/%s", 
// 				proxy.getPort(), 
// 				"x.wav");
// 		MediaPlayer mediaPlayer = new MediaPlayer();
// 		mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
// 		try {
// 			mediaPlayer.setDataSource(proxyUrl);
// 			mediaPlayer.prepare(); // might take long! (for buffering, etc)
// 		} catch (Exception e) {
// 			e.printStackTrace();
// 		}
// 		mediaPlayer.start();
		
		
		
    	atrack = new AudioTrack(
    			AudioManager.STREAM_MUSIC, 
    			SAMPLE_RATE, 
    			AudioFormat.CHANNEL_CONFIGURATION_MONO,
    			AudioFormat.ENCODING_PCM_8BIT, 
    			BUFFER_SIZE, 
    			AudioTrack.MODE_STREAM);
    	atrack.play();

    	byte hwData[] = new byte[BUFFER_SIZE];

	}

	public void stop2()
	{
		atrack.stop();
		done = true;
		
// 		if(mediaPlayer != null) {
// 			mediaPlayer.stop();
// 			mediaPlayer = null;
// 			proxy.stop();
// 			proxy = null;
// 		}
	}

	public void run() 
	{
		while(!done)
		{
			for(int i = 0; i < BUFFER_SIZE; i++)
			{
				hwData[i] = loopData[readOffset];
				readOffset++;
				if(readOffset >= loopSize)
				{
					readOffset = 0;
				}
			}
			atrack.write(hwData, 0, BUFFER_SIZE);
		}
	}
	
}



