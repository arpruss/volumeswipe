package mobi.omegacentauri.VolumeSwipe;

import android.content.Context;
import android.media.AudioManager;
import android.media.audiofx.Equalizer;
import android.os.Build;
import android.util.Log;

public class VolumeController {
	private AudioManager am;
	private int extraDB = 1500;
	private short bands;
	private Equalizer eq = null;
	private int maxStreamVolume;

	VolumeController(Context context, float boost) {
		am = (AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
		maxStreamVolume = 100*am.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
		
		VolumeSwipe.log("maxStreamVolume "+maxStreamVolume);
		
		if (Build.VERSION.SDK_INT>=9) {
			try {
				eq = new Equalizer(0, 0);
				bands = eq.getNumberOfBands();
				extraDB = (int)(eq.getBandLevelRange()[1] * boost);
				eq.setEnabled(extraDB>0);
			}
			catch(UnsupportedOperationException e) {
				Log.e("VolumeSwipe", e.toString());
				extraDB = 0;
				eq = null;
			}
		}
	}
	
	public int getPercent() {
		return 100 * getVolume() / maxStreamVolume;
	}
	
	public int getMaxVolume() {
		return maxStreamVolume + extraDB;
	}
	
	void setVolume(int v) {
		VolumeSwipe.log("Need to set to "+v);
		
		if (v > maxStreamVolume + extraDB)
			v=maxStreamVolume + extraDB;
		else if (v<0)
			v=0;

		am.setStreamVolume(AudioManager.STREAM_MUSIC, v <= maxStreamVolume ? v/100 : maxStreamVolume/100, 0/*AudioManager.FLAG_SHOW_UI*/);

		if (extraDB > 0) {
			if (maxStreamVolume < v) {
				try {
					int value = v-maxStreamVolume;
					for (short i=0; i<bands; i++) {
						short adj = (short)value;

						if (true) {
							int hz = eq.getCenterFreq((short)i)/1000;
							if (hz < 150)
								adj = 0;
							else if (hz < 250)
								adj = (short)(value/2);
							else if (hz > 8000)
								adj = (short)(3*(int)value/4);
						}
						eq.setBandLevel((short)i, adj);
					}
				}
				catch(UnsupportedOperationException e) {
					Log.e("VolumeSwipe", e.toString());
				}
				VolumeSwipe.log("Boost set to "+getVolume());
			}
			else {
				reset();
				VolumeSwipe.log("Set to "+getVolume());
			}
		}
	}
	
	public int getVolume() {
		int volume = 100*am.getStreamVolume(AudioManager.STREAM_MUSIC);
		
		if (extraDB == 0)
			return volume;
		
		try {
			float total = 0;
			int count = 0;
			
			for (short i=0; i<bands; i++) {
				int hz = eq.getCenterFreq((short)i)/1000;
				if (250 <= hz && hz <= 8000) {
					total += eq.getBandLevel(i);
					count++;
					VolumeSwipe.log(""+i+" "+eq.getBandLevel(i));
				}
			}
			
			if (0<count)
				volume += (int)(total/count+.5f);
		}
		catch (UnsupportedOperationException e) {
			Log.e("VolumeSwipe", e.toString());			
		}
		
		return volume;
	}
	
	void reset() {
		if (extraDB > 0) {
			for (int i=0; i<bands; i++) {
				try {
					eq.setBandLevel((short)i, (short)0);
				}
				catch(UnsupportedOperationException e) {
					Log.e("VolumeSwipe", e.toString());
				}
			}
		}
	}
}
