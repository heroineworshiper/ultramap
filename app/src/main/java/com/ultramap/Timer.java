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

import java.util.Calendar;

import android.util.Log;

public class Timer {
	
	void dump()
	{
		Log.v("Timer", "dump active=" + active + 
		" accum=" + accum +
		" startTime=" + startTime);
	}
	
	int loadState(byte [] buffer, int offset)
	{
	// restore timer
		active = (Settings.read_int32(buffer, offset) > 0) ? true : false;
		offset += 4;
		accum = Settings.read_int64(buffer, offset);
		offset += 8;
		startTime = Settings.read_int64(buffer, offset);
		offset += 8;
		return offset;
	}
	
	int saveState(byte [] buffer, int offset)
	{
		offset = Settings.write_int32(buffer, offset, active ? 1 : 0);
		offset = Settings.write_int64(buffer, offset, accum);
		offset = Settings.write_int64(buffer, offset, startTime);
		return offset;
	}
	
	
	// get time in ms
	long getTime()
	{
		if(active)
		{
			Calendar c = Calendar.getInstance();
			return accum + c.getTimeInMillis() - startTime;
		}
		else
		{
			return accum;
		}
	}
	void stop()
	{
		Calendar c = Calendar.getInstance();
		accum += c.getTimeInMillis() - startTime;
		active = false;
	}
	void start()
	{
		Calendar c = Calendar.getInstance();
		startTime = c.getTimeInMillis();
		active = true;
	}
	void reset()
	{
		accum = 0;
		active = false;
	}
// accumulated time in ms
	long accum;
// last start time in ms
	long startTime;
	boolean active;
}
