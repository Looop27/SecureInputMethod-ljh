/*
 * Copyright (C) 2008-2009 The Android Open Source Project
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

//package com.seu.android.softkeyboard;
package com.seu.SecureFingerMouse;

//package com.random.android.randomkeyboard;

import java.util.*;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.graphics.drawable.Drawable;
import android.inputmethodservice.Keyboard;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Xml;
import android.view.inputmethod.EditorInfo;
//lingzhen
/////////////////lingzhen//////////////////////////
///////////////////////////////////////////////////
public class LatinKeyboard extends Keyboard {

    private Key mEnterKey;
    private Key mSpaceKey;
    
    public static final int KEYCODE_RND = -117;
    
    //lingzhen
    int mDefaultWidth, mDefaultHeight, mDefaultHorizontalGap, mDefaultVerticalGap;
    Context context;
    List<Key> mKeys = new ArrayList<Key>();
    List<Key> mModifierKeys = new ArrayList<Key>();
    ArrayList<Row> rows = new ArrayList<Row>();
    static String keysequence = "abcdefghijklmnopqrstuvwxyz";
    static String symbolsequence = "1234567890@#$%&*-=()!\"':;/?";
    static String symbolshiftsequence = "~±×÷•°`´{}©£€^®¥_+[]¡<>¢|\\¿";
    static int setRandom = 0;

    public LatinKeyboard(Context context, int xmlLayoutResId) {
    	super(context, xmlLayoutResId);
    	Log.e("cf","LatinKeyboard constructor go!");
        //lingzhen
//    	 super(context, R.layout.keyboard_template);
//    	int x = 0;
//        int y = 0;
//        int column = 0;
//        int mTotalWidth = 0;
//        int columns = 10;
//        final CharSequence characters = "qwertyuiopasdfghjklzxcvbnm";
//        final int horizontalPadding = 0;
//        int mTotalHeight = 0;
//        
//        DisplayMetrics dm = context.getResources().getDisplayMetrics();
//        int mDisplayWidth = dm.widthPixels;
//        int mDisplayHeight = dm.heightPixels;
//        mDefaultHorizontalGap = 0;
//        mDefaultWidth = mDisplayWidth / 10;
//        mDefaultVerticalGap = 0;
//        mDefaultHeight = mDefaultWidth;
//         
//        Row row = new Row(this);
//        row.defaultHeight = mDefaultHeight;
//        row.defaultWidth = mDefaultWidth;
//        row.defaultHorizontalGap = mDefaultHorizontalGap;
//        row.verticalGap = mDefaultVerticalGap;
//        row.rowEdgeFlags = EDGE_TOP | EDGE_BOTTOM;
//        final int maxColumns = columns == -1 ? Integer.MAX_VALUE : columns;
//        Log.e("lingzhen","lingzhen Hello3 "+maxColumns);
//        for (int i = 0; i < characters.length(); i++) {
//            char c = characters.charAt(i);
//            Log.e("lingzhen","lingzhen Hello4 "+c);
//            if (column >= maxColumns 
//                    || x + mDefaultWidth + horizontalPadding > mDisplayWidth) {
//                x = 0;
//                y += mDefaultVerticalGap + mDefaultHeight;
//                column = 0;
//            }
//            Log.e("lingzhen","lingzhen Hello5 x:"+x+"y:"+y);
//            final Key key = new Key(row);
//            key.x = x;
//            key.y = y;
//            key.label = String.valueOf(c);
//            key.codes = new int[] { c };
//            column++;
//            x += key.width + key.gap;
//            
//            mKeys.add(key);
//            //row.mKeys.add(key);
//            if (x > mTotalWidth) {
//                mTotalWidth = x;
//            }
//        }
//        mTotalHeight = y + mDefaultHeight;
//        rows.add(row);
    	/*
        super(context, R.layout.keyboard_template);
    	
    	Log.e("lingzhen","lingzhen Hello");
    	this.context = context;
		mDefaultWidth = 32;
		mDefaultHeight = R.dimen.key_height;
		int column = 0;
		int x = 0;
		int y = 0;
				//context.getResources().getDimensionPixelSize(R.dimen.keySize);
		mKeys = new ArrayList<Key>();

		Row row = new Row(this);
		row.defaultHeight = mDefaultHeight;
		row.defaultWidth = mDefaultWidth;
		row.defaultHorizontalGap = 0;//mDefaultHorizontalGap;
		row.verticalGap = 0;//mDefaultVerticalGap;
		row.rowEdgeFlags = EDGE_TOP | EDGE_BOTTOM;
		Random r = new Random();
		
		for (int i = 0; i < 5; i++) {
			char c = (char)(r.nextInt(26) + 'a');
			if (column >= 10) {
                x = 0;
                y += 2 + mDefaultHeight;
                column = 0;
            }
			final LatinKey key = new LatinKey(row);
			if (i == 0)
				key.edgeFlags = EDGE_LEFT;
			else if (i == 4)
				key.edgeFlags = EDGE_RIGHT;
			else
				key.edgeFlags = 0;
			key.x = x;
	        key.y = y;
	        Log.e("lingzhen","lingzhen x:"+x+"y:"+y);
	        key.width = mDefaultWidth;
	        key.height = mDefaultHeight;
	        key.gap = 0;
	        key.label = String.valueOf(c);
	        Log.e("lingzhen","lingzhen key.label:"+key.label);
	        Log.e("lingzhen","lingzhen key.gap:"+key.gap);
	        Log.e("lingzhen","lingzhen key.edgeFlags:"+key.edgeFlags);
	        key.codes = new int[] { c };
	        key.icon = context.getResources().getDrawable(R.drawable.sym_keyboard_delete);
	        column++;
	        x += key.width + key.gap;
	        mKeys.add(key);
	        
		}
		rows.add(row);
		*/
    }

	public LatinKeyboard(Context context, int layoutTemplateResId, 
            CharSequence characters, int columns, int horizontalPadding) {
        super(context, layoutTemplateResId, characters, columns, horizontalPadding);
    }

	public void setParameter(int z){
		setRandom = z;
	}
	static int inital = 0;
    @Override
    protected Key createKeyFromXml(Resources res, Row parent, int x, int y, XmlResourceParser parser) {
    	
    	Log.e("cf","createKeyFromXml go!");
    	
    	Key key;
    	int flag = 0;
    	if (setRandom == 0){
    		AttributeSet as = Xml.asAttributeSet(parser);
			 for(int i = 0; i < as.getAttributeCount(); ++i){
				 if (as.getAttributeName(i).equals("codes") && as.getAttributeValue(i).equals("-117")){
//					 key = new LatinKey(res, parent, x, y, parser);
//					 key.edgeFlags = 2;
					 flag = 1;
//					 Log.e("lingzhen","lingzhen get it!!!!!!!!!");
					 break;
				 }
				 
//				Log.e("lingzhen","lingzhen getAttr:"+as.getAttributeCount()+as.getAttributeName(i)+":"+as.getAttributeValue(i));
			 }
			 if(flag == 1){
				 key = new LatinKey(res, parent, x+250, y, parser);
//				 Log.e("lingzhen","lingzhen get it!!!!!!!!!"+key.edgeFlags);
//				 key.edgeFlags = 1;
//				 Log.e("lingzhen","lingzhen get it!!!!!!!!!"+key.edgeFlags);
				 flag = 0;
			 }else{
				 key = new LatinKey(res, parent, x, y, parser);
//				 Log.e("lingzhen","lingzhen edgeFlags"+key.edgeFlags);
			 }
    	}else{
    		
    		key = new LatinKey(res, parent, x, y, parser);
//    			 as.getAttributeIntValue("keyEdgeFlags", attribute, defaultValue)
//    			key = new LatinKey(res, parent, x, y, parser);
    		 
    		
    	}
    	
/////////////////lingzhen//////////////////////////
//        keysequence = "abcdefghijklmnopqrstuvwxyz";
//        List<Character> characters = new ArrayList<Character>();
//        for(char a:keysequence.toCharArray()){
//            characters.add(a);
//        }
        int randpicker;
        char c;
        
        //characters = Arrays.asList(keysequence.toCharArray());
//        Collections.shuffle(characters);
//        Log.e("lingzhen","lingzhen characters:"+characters);
        if (setRandom == 1){
	        if (key.label != null && key.label.toString().matches("[a-z]")) {
		        Random r = new Random();
	//	        int randpicker = r.nextInt(characters.size());
	//	        char c = characters.remove(randpicker);
		        randpicker = r.nextInt(keysequence.length());
		        c = keysequence.charAt(randpicker);
		        keysequence = keysequence.replace(""+c, "");
//		        Log.e("lingzhen","lingzhen keysequence:"+keysequence);
//		        Log.e("lingzhen","lingzhen c:"+c);
		        key.label = String.valueOf(c);
		        key.codes = new int[] { c }; 
	        }
	        if (keysequence.length() == 0){
	        	keysequence = "abcdefghijklmnopqrstuvwxyz";
	        	setRandom = 0;
	        }
        } else if (setRandom == 2){
        	if (key.label != null && key.label.toString().matches("[0123456789@#$%&*-=()!\"':;/?]")) {
		        Random r = new Random();
	//	        int randpicker = r.nextInt(characters.size());
	//	        char c = characters.remove(randpicker);
		        randpicker = r.nextInt(symbolsequence.length());
		        c = symbolsequence.charAt(randpicker);
		        symbolsequence = symbolsequence.replace(""+c, "");
//		        Log.e("lingzhen","lingzhen symbolsequence:"+symbolsequence);
//		        Log.e("lingzhen","lingzhen c:"+c);
		        key.label = String.valueOf(c);
		        key.codes = new int[] { c }; 
	        }
	        if (symbolsequence.length() == 0){
	        	symbolsequence = "1234567890@#$%&*-=()!\"':;/?";
	        	setRandom = 0;
	        }
        } else if (setRandom == 3){
//        	Log.e("lingzhen","lingzhen key.label:"+key.label);
//        	Log.e("lingzhen","lingzhen key.codes:"+key.codes[0]);
        	if (key.label != null && key.label.toString().matches("[~±×÷•°`´{}©£€^®¥_+\\[\\]¡<>¢|\\\\¿]")) {
		        Random r = new Random(); 
	//	        int randpicker = r.nextInt(characters.size());
	//	        char c = characters.remove(randpicker);
		        randpicker = r.nextInt(symbolshiftsequence.length());
		        c = symbolshiftsequence.charAt(randpicker);
		        symbolshiftsequence = symbolshiftsequence.replace(""+c, "");
//		        Log.e("lingzhen","lingzhen symbolshiftsequence:"+symbolshiftsequence);
//		        Log.e("lingzhen","lingzhen c:"+c);
		        key.label = String.valueOf(c);
		        key.codes = new int[] { c }; 
	        }
	        if (symbolshiftsequence.length() == 0){
	        	symbolshiftsequence = "~±×÷•°`´{}©£€^®¥_+[]¡<>¢|\\¿";
	        	setRandom = 0;
	        }
        }
//Log.e("lingzhen","lingzhen x:"+x+"y:"+y);
//Log.e("lingzhen","lingzhen key.codes:"+key.codes);
//Log.e("lingzhen","lingzhen key.label:"+key.label);
//Log.e("lingzhen","lingzhen key.gap:"+key.gap);
//Log.e("lingzhen","lingzhen key.edgeFlags:"+key.edgeFlags);
///////////////////////////////////////////////////
        if (key.codes[0] == 10) {
            mEnterKey = key;
        } else if (key.codes[0] == ' ') {
            mSpaceKey = key;
        }
        return key;
    }
    
    /**
     * This looks at the ime options given by the current editor, to set the
     * appropriate label on the keyboard's enter key (if it has one).
     */
    void setImeOptions(Resources res, int options) {
        if (mEnterKey == null) {
            return;
        } 
        
        switch (options&(EditorInfo.IME_MASK_ACTION|EditorInfo.IME_FLAG_NO_ENTER_ACTION)) {
            case EditorInfo.IME_ACTION_GO:
            	Log.e("cfws","EditorInfo.IME_ACTION_GO go!");
                mEnterKey.iconPreview = null;
                mEnterKey.icon = null;
                mEnterKey.label = res.getText(R.string.label_go_key);
                break;
            case EditorInfo.IME_ACTION_NEXT:
            	Log.e("cfws","EditorInfo.IME_ACTION_NEXT go!");
                mEnterKey.iconPreview = null;
                mEnterKey.icon = null;
                mEnterKey.label = res.getText(R.string.label_next_key);
                break;
            case EditorInfo.IME_ACTION_SEARCH:
            	Log.e("cfws","EditorInfo.IME_ACTION_SEARCH go!");
                mEnterKey.icon = res.getDrawable(R.drawable.sym_keyboard_search);
                mEnterKey.label = null;
                break;
            case EditorInfo.IME_ACTION_SEND:
            	Log.e("cfws","EditorInfo.IME_ACTION_SEND go!");
                mEnterKey.iconPreview = null;
                mEnterKey.icon = null;
                mEnterKey.label = res.getText(R.string.label_send_key);
                break;
            default:
            	Log.e("cfws","EditorInfo.default.RETURN go!");
                mEnterKey.icon = res.getDrawable(R.drawable.sym_keyboard_return);
                mEnterKey.label = null;
                break;
        }
    }

    void setSpaceIcon(final Drawable icon) {
        if (mSpaceKey != null) {
            mSpaceKey.icon = icon;
        }
    }

    static class LatinKey extends Keyboard.Key {
        
        public LatinKey(Resources res, Keyboard.Row parent, int x, int y, XmlResourceParser parser) {
            super(res, parent, x, y, parser);
        }
        //lingzhen
        public LatinKey(Keyboard.Row r) {
        	super(r); 
    	}
                
        
        /**
         * Overriding this method so that we can reduce the target area for the key that
         * closes the keyboard. 
         */
        @Override
        public boolean isInside(int x, int y) {
            return super.isInside(x, codes[0] == KEYCODE_CANCEL ? y - 10 : y);
        }
    }

}
