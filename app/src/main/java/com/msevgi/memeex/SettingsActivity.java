package com.msevgi.memeex;

import android.app.Activity;
import android.os.Bundle;


public class SettingsActivity extends Activity {

	public static final String KEY_PREF_MEME_IMAGE_RES = "image_resolutions";
	public static final String KEY_PREF_MEME_VID_RES = "video_resolutions";
	public static final String KEY_PREF_MEME_FPS = "fps";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		
		// Display the fragment as the main content.
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new SettingsFragment())
                .commit();

	}
	
}
