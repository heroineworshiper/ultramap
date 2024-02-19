/*
 * Ultramap
 * Copyright (C) 2024 Adam Williams <broadcast at earthling dot net>
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

// play noise to keep a speaker alive

package com.ultramap;


import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaPlayer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

public class KeepAlive extends Thread
{

//	MediaPlayer mediaPlayer = null;
//	StreamProxy proxy = null;
	final int BUFFER_SIZE = 32768;
	final int SAMPLE_RATE = 44100;
// contiguous samples extracted from the loop
	byte [] hwData = new byte[BUFFER_SIZE];
	int readOffset = 0;

	byte [] loopData;
	int loopSize = 0;

	boolean done = false;
	AudioTrack atrack;
	
	KeepAlive()
	{
// loop of random samples from 0-1
		loopSize = SAMPLE_RATE * 60;
		loopData = new byte[loopSize];
		for(int i = 0; i < loopSize; i++)
            loopData[i] = (byte)(Math.random() + .5);

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



