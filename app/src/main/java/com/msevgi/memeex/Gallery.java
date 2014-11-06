package com.msevgi.memeex;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.HorizontalScrollView;

public class Gallery extends HorizontalScrollView {
	
	private static final String TAG = "Gallery";

	private static int CHECK_INTERVAL = 100;
	
	private int mInitialPosition;
	
	private boolean isScrolling = false;
	
	public interface OnScrollStoppedListener{
	    void onScrollStopped();
	}

	private OnScrollStoppedListener onScrollStoppedListener;

	private Runnable mScrollerTask;
	
	public Gallery(Context context, AttributeSet set){
		super(context,set);
		setRunnable();
	}

	public Gallery(Context context) {
		super(context);
		setRunnable();
	}
	
	private void setRunnable(){
		mScrollerTask = new Runnable() {


			public void run() {
				setScrolling(true);
				Log.d("ScrollerTaskRunnable","Starting scroller runnable");
	            int newPosition = getScrollX();
	            if(mInitialPosition - newPosition == 0){//has stopped
	            	Log.d(TAG, "The scroller has stopped!");
	            	setScrolling(false);
	                if(onScrollStoppedListener!=null){

	                    onScrollStoppedListener.onScrollStopped();
	                }
	            }else{
	                mInitialPosition = getScrollX();
	                Gallery.this.postDelayed(mScrollerTask, CHECK_INTERVAL);
	            }
	        }
	    };
	}
	
	
	/**
	 * Configure scroll stopped listener
	 * @param listener
	 */
	public void setOnScrollStoppedListener(OnScrollStoppedListener listener){
	    onScrollStoppedListener = listener;
	}

	public void startScrollerTask(){

	    mInitialPosition = getScrollX();
	    Log.d(TAG, "Initial Position of Scroller: " + mInitialPosition);
	    boolean success = Gallery.this.postDelayed(mScrollerTask, CHECK_INTERVAL);
	    Log.d(TAG, "Was the scheduling of the delayed task successful? " + success);
	}

	public boolean isScrolling() {
		return isScrolling;
	}

	public void setScrolling(boolean isScrolling) {
		this.isScrolling = isScrolling;
	}
}
