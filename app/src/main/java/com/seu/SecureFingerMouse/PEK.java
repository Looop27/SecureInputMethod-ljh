/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.seu.SecureFingerMouse;
//package com.seu.android.softkeyboard;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.preference.PreferenceActivity;

import com.android.inputmethodcommon.InputMethodSettingsFragment;

import android.util.Log;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;


/**
 * Displays the IME preferences inside the input method setting.
 */
public class PEK extends PreferenceActivity{
//	
//	private PowerManager pm;  
//	private PowerManager.WakeLock mWakeLock; 
	
    @Override
    public Intent getIntent() {
        final Intent modIntent = new Intent(super.getIntent());
        Log.e("seu", "getIntent()");
        modIntent.putExtra(EXTRA_SHOW_FRAGMENT, Settings.class.getName());
        modIntent.putExtra(EXTRA_NO_HEADERS, true);
        return modIntent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.e("seu", "onCreate()");
        
//        this.pm = (PowerManager) this.getSystemService(Context.POWER_SERVICE);  
//        this.mWakeLock = this.pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "My Tag"); 
        
//        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
//        
//        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);//竖屏

        // We overwrite the title of the activity, as the default one is "Voice Search".
        setTitle(R.string.settings_name);

    }
//    @Override
//    protected void onResume(){
//    	super.onResume();
////    	mWakeLock.acquire(); 
//    }
//    @Override
//    protected void onPause(){
//    	super.onPause();
////    	mWakeLock.release();  
//    }

    public static class Settings extends InputMethodSettingsFragment implements OnSeekBarChangeListener{
    	
    	SeekBar bar; // declare seekbar object variable
    	    	
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setInputMethodSettingsCategoryTitle(R.string.language_selection_title);
            setSubtypeEnablerTitle(R.string.select_language);
            Log.e("seu", "SettingsOnCreate()");

            // Load the preferences from an XML resource
            addPreferencesFromResource(R.xml.ime_preferences);
            
            Log.e("seu", "after add preference");
            
//            bar = (SeekBar)getActivity().findViewById(R.id.seekBar1); // make seekbar object
//            bar.setOnSeekBarChangeListener(this); // set seekbar listener.
        }
    
		@Override
		public void onProgressChanged(SeekBar seekBar, int progress,
				boolean fromUser) {
			// TODO Auto-generated method stub
			
		}
	
		@Override
		public void onStartTrackingTouch(SeekBar seekBar) {
			// TODO Auto-generated method stub
			
		}
	
		@Override
		public void onStopTrackingTouch(SeekBar seekBar) {
			// TODO Auto-generated method stub
			
		}
    }
}
