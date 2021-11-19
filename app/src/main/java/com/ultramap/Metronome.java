
package com.ultramap;


import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaPlayer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

public class Metronome extends Thread
{

//	MediaPlayer mediaPlayer = null;
//	StreamProxy proxy = null;
	final int BUFFER_SIZE = 32768;
	final int SAMPLE_RATE = 44100;
	final int HEADER_SIZE = 44;
// contiguous samples extracted from the loop
	byte [] hwData = new byte[BUFFER_SIZE];
	int readOffset = 0;

	byte [] loopData;
	int loopSize = 0;

	boolean done = false;
	AudioTrack atrack;
	
	Metronome()
	{
		// the array of sound files for different beats
		int[] sounds = Settings.soundToFiles(Settings.sound);
		// the smallest buffer which can contain all the sounds at the desired tempo
		loopSize = SAMPLE_RATE * 60 * sounds.length / Settings.beatsPerMinute;
		loopData = new byte[loopSize];
		Arrays.fill(loopData, (byte) 0x80);

		for(int i = 0; i < sounds.length; i++)
		{
			InputStream sound = Main.main.getResources().openRawResource(sounds[i]);
			// extract byte array from the sound file
			try {
				int samples = sound.available() - HEADER_SIZE;
				int dstOffset = SAMPLE_RATE * 60 * i / Settings.beatsPerMinute;
				if(samples + dstOffset > loopSize)
				{
					samples = loopSize - dstOffset;
				}
				sound.skip(HEADER_SIZE);
				sound.read(loopData, dstOffset, samples);
			} catch (IOException e) {
				e.printStackTrace();
			}

		}

		
    	atrack = new AudioTrack(
    			AudioManager.STREAM_MUSIC, 
    			SAMPLE_RATE, 
    			AudioFormat.CHANNEL_CONFIGURATION_MONO,
    			AudioFormat.ENCODING_PCM_8BIT, 
    			BUFFER_SIZE, 
    			AudioTrack.MODE_STREAM);
    	atrack.play();


	}

	public void stop2()
	{
		atrack.stop();
		done = true;
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



