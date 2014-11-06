package com.msevgi.memeex;

import static com.googlecode.javacv.cpp.opencv_imgproc.cvCvtColor;

import java.io.File;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMetadataRetriever;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.v4.app.NavUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.googlecode.javacv.FFmpegFrameGrabber;
import com.googlecode.javacv.FrameGrabber.Exception;
import com.googlecode.javacv.cpp.opencv_core;
import com.googlecode.javacv.cpp.opencv_core.IplImage;
import com.googlecode.javacv.cpp.opencv_imgproc;
import com.msevgi.memeex.util.AnimationsHelper;
import com.msevgi.memeex.util.DiskLruImageCache;

public class ChooseTenSecondClip extends Activity {
	protected static final String TAG = ChooseTenSecondClip.class.getSimpleName();
	private Uri mVideoUri;
	private ArrayList<ImageView> mImageViews;
	private ImageView mVideoView;
	private static double NUMBER_OF_SECONDS_IN_CLIP = 10;
	private int mMillisecondOffset = 0;
	private AsyncTask<Integer, Integer, Long> mThunbmailsAsync;
	private AsyncTask<Integer, Integer, Long> mGrabFramesAsync;
	private MediaMetadataRetriever mRetriever;
	private Gallery mHsv;
	private static double GOOGLE_MEDIA_PLAYER_CONSTANT = 1000000;
	private static double FRAMES_PER_SECOND_TO_CAPTURE = 16;
	private Point mScrollPosition = null;
	public double mFps = 24;
	private String mFullPath;
	private GetGalleryFramesAtTimeAsyncTask mGalleryAsync;
	private DiskLruImageCache mDiskLruCache;
	private final Object mDiskCacheLock = new Object();
	private boolean mDiskCacheStarting = true;
	private Bitmap mCreateCache;
	private static final int DISK_CACHE_SIZE = 1024 * 1024 * 250; // 250MB
	private static final String DISK_CACHE_SUBDIR = "thumbnailsetc";
	
	/** 
	 * -----------------------------------------------------------------------
	 * Activity Implementation
	 * ------------------------------------------------------------------------
	 */

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_choose_ten_second_clip);
		// Show the Up button in the action bar.
		getActionBar().setDisplayHomeAsUpEnabled(true);
		Intent data = this.getIntent();
		if(data != null){
			mVideoUri = (Uri) data.getData();
		}
		if(savedInstanceState != null){
			Log.d(ChooseTenSecondClip.TAG, "Resuming from sleep: ");
			String videoUri = savedInstanceState.getString("videoUri");
			Log.d(ChooseTenSecondClip.TAG, "Video URI: " + videoUri);
			if(videoUri != null){
				mVideoUri = Uri.parse(videoUri);
			}
			int x = savedInstanceState.getInt("scrollX");
			int y = savedInstanceState.getInt("scrollY");
			if(x != 0 && y != 0){
				this.mScrollPosition = new Point(x,y);
			}
		}
	    // Initialize disk cache on background thread
	    File cacheDir = FileUtilsHelper.getDiskCacheDir(this, DISK_CACHE_SUBDIR);
	    new InitDiskCacheTask().execute(cacheDir);
		Log.d(ChooseTenSecondClip.TAG,"Video URI: " + mVideoUri);
	}
	
	/**
	 * Get the FPS from the user settings, if undefined, will return 1000 which means just use the default
	 * @return The fps from the user prefs or 1000
	 */
	
	public int getUserPreferredFps(){
		int fps = 24;
		SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
		fps = sharedPref.getInt(SettingsActivity.KEY_PREF_MEME_FPS, 1000);
		return fps;
	}
	
	public void addBitmapToMemoryCache(String key, Bitmap bitmap) {   
	    //new SaveBitmapToCacheTask(String.valueOf(key)).execute(bitmap);
	    synchronized (mDiskCacheLock) {
	        if (mDiskLruCache != null && mDiskLruCache.getBitmap(String.valueOf(key)) == null) {
	        	//new SaveBitmapToCacheTask(String.valueOf(key)).execute(bitmap); 
	           mDiskLruCache.put(String.valueOf(key), bitmap);
	        }
	    }
	}
	
	public Bitmap getBitmapFromDiskCache(String key) {
	    synchronized (mDiskCacheLock) {
	        // Wait while disk cache is started from background thread
	        while (mDiskCacheStarting) {
	            try {
	                mDiskCacheLock.wait();
	            } catch (InterruptedException e) {}
	        }
	        if (mDiskLruCache != null) {
	            return mDiskLruCache.getBitmap(key);
	        }
	    }
	    return null;
	}

	public Bitmap getBitmapFromMemCache(String key) {
		Bitmap b;
		synchronized(mDiskCacheLock){
			b = getBitmapFromDiskCache(key);
		}
	    return b;
	}

	@Override
	protected void onDestroy() {
		if(mThunbmailsAsync != null){
			if(!mThunbmailsAsync.isCancelled()){
				mThunbmailsAsync.cancel(true);
			}
		}
		if(mGrabFramesAsync != null){
			if(!mGrabFramesAsync.isCancelled()){
				mGrabFramesAsync.cancel(true);
			}
		}
		if(mRetriever != null){
			mRetriever.release();
			mRetriever = null;
		}
		super.onDestroy();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState.putString("videoUri",mVideoUri.toString());
		outState.putInt("scrollX", this.mScrollPosition.x);
		outState.putInt("scrollY", this.mScrollPosition.y);
		super.onSaveInstanceState(outState);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_choose_ten_second_clip, menu);
		return true;
	}
	
	/**
	 * Will get the image resolution setting from prefs
	 * @return A map of image width and height
	 */
	public HashMap<String,Integer> getVideoResolutionSetting(){
		HashMap<String, Integer> resMap = new HashMap<String,Integer>();
		SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
		String syncConnPref = sharedPref.getString(SettingsActivity.KEY_PREF_MEME_VID_RES, "640x480");
		String[] split = syncConnPref.split("x");
		resMap.put("width", Integer.parseInt(split[0]));
		resMap.put("height", Integer.parseInt(split[1]));
		return resMap;
	}
	
	public int getVideoFrameRate(){
		SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
		int syncConnPref = sharedPref.getInt(SettingsActivity.KEY_PREF_MEME_FPS, 1000);
		return syncConnPref;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			// This ID represents the Home or Up button. In the case of this
			// activity, the Up button is shown. Use NavUtils to allow users
			// to navigate up one level in the application structure. For
			// more details, see the Navigation pattern on Android Design:
			//
			// http://developer.android.com/design/patterns/navigation.html#up-vs-back
			//
			NavUtils.navigateUpFromSameTask(this);
			return true;
		case R.id.menu_settings:
			Intent i = new Intent(this, SettingsActivity.class);
			startActivity(i);
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onPause() {
		if(mThunbmailsAsync != null){
			if(!mThunbmailsAsync.isCancelled()){
				mThunbmailsAsync.cancel(true);
			}
		}
		if(mGrabFramesAsync != null){
			if(!mGrabFramesAsync.isCancelled()){
				mGrabFramesAsync.cancel(true);
			}
		}
		if(this.mGalleryAsync != null){
			if(!mGalleryAsync.isCancelled()){
				mGalleryAsync.cancel(true);
			}
		}
		if(mRetriever != null){
			mRetriever.release();
			mRetriever = null;
		}
		super.onPause();
	}

	@Override
	protected void onResume() {
		super.onResume();
		double length = getVideoLength();
		if(this.mVideoUri != null){
            try {
                Log.i(ChooseTenSecondClip.TAG, "Mime: " + getVideoMimeDetails());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
		Log.d(TAG,"Video Length: " + length);
		mImageViews = getArrayListOfImageViewsForLength(length);
		LinearLayout lt = (LinearLayout) findViewById(R.id.imageHolder1);
		mHsv = (Gallery) findViewById(R.id.gallery1);
		if(this.mScrollPosition != null){
			mHsv.scrollTo(this.mScrollPosition.x, this.mScrollPosition.y);
		}
		mHsv.setOnTouchListener(new OnTouchListener() {

	        public boolean onTouch(View v, MotionEvent event) {
	        	Log.d(TAG, "scroll view touched");
	        	int action = event.getAction();
	            if (action == MotionEvent.ACTION_UP) {
	            	Log.d(TAG, "starting scroller task");
	            	mHsv.startScrollerTask();
	            }

	            return false;
	        }
		});
		mHsv.setOnScrollStoppedListener(new Gallery.OnScrollStoppedListener(){

			@Override
			public void onScrollStopped() {
				//scrolling stopped, now load views
				Log.d(TAG,"Scrolling stopped");
				ChooseTenSecondClip.this.mScrollPosition = new Point(ChooseTenSecondClip.this.mHsv.getScrollX(), 
						ChooseTenSecondClip.this.mHsv.getScrollY());
				ChooseTenSecondClip.this.loadThumbnailsForVisibleViews();
			}
			
		});
		lt.removeAllViews();
	    	int i, l = mImageViews.size();
	    	for(i=0; i < l; i++){
	    		ImageView im = mImageViews.get(i);
		 		lt.addView(im);
		 	}
		lt.requestLayout();
		if(mThunbmailsAsync != null){
			if(!mThunbmailsAsync.isCancelled()){
				mThunbmailsAsync.cancel(false);
				mThunbmailsAsync = null;
			}
		}
		mVideoView = (ImageView) findViewById(R.id.videoView1);
		mVideoView.setOnTouchListener(new OnTouchListener(){

			@Override
			public boolean onTouch(View arg0, MotionEvent arg1) {
				Log.d(TAG,"Responding to video view touch");
				if(arg1.getAction() == MotionEvent.ACTION_UP){
					AnimationsHelper.abortAnimations();
					ChooseTenSecondClip.this.chooseCurrentClip();
				}
				return false;
			}
			
		});
		mHsv.startScrollerTask();
		if(this.mVideoUri != null){
			loadThumbnailsForVisibleViews();
			startClipPlayback(this.mMillisecondOffset);
		}
	}
	
	protected void loadThumbnailsForVisibleViews() {
		ArrayList<ImageView> visibleViews = new ArrayList<ImageView>();
		Rect scrollViewDrawingRect = new Rect();
		this.mHsv.getDrawingRect(scrollViewDrawingRect);
		for(ImageView imv : this.mImageViews){
			Rect hitRect = new Rect();
			imv.getHitRect(hitRect);
			// add buffer to load images partially on the screen
			scrollViewDrawingRect.left = scrollViewDrawingRect.left - 20;
			scrollViewDrawingRect.right = scrollViewDrawingRect.right + 20;
			if(scrollViewDrawingRect.contains(hitRect)){
				visibleViews.add(imv);
			}else{
				Drawable d = this.getResources().getDrawable(R.drawable.why);
				imv.setImageDrawable(d);
			}
		}
		if(mThunbmailsAsync != null && !mThunbmailsAsync.isCancelled()){
			mThunbmailsAsync.cancel(true);
		}
		mThunbmailsAsync = new PopulateThumbnailsAsyncTask(visibleViews).execute(1);
	}

	/**
	 * ------------------------------------------------------------------------
	 * Begin media interface actions
	 * ------------------------------------------------------------------------
	 */
	
	protected void chooseCurrentClip() {
		Log.d(TAG,"Selecting clip images asynchronously");
		if(mThunbmailsAsync != null){
			if(!mThunbmailsAsync.isCancelled()){
				mThunbmailsAsync.cancel(false);
				mThunbmailsAsync = null;
			}
		}
		mGrabFramesAsync = new GrabFramesAsyncTask(this.mMillisecondOffset, this).execute(1);
	}
	
	public void updateProgressIndicator(float p) {
		ProgressBar prog = (ProgressBar)findViewById(R.id.progressBar1);
		prog.setMax(100);
		prog.setProgress((int)Math.ceil(p));
	}
	
	public void doneSavingClipImages() {
		Log.d(TAG,"Finished saving clip images, transitioning to editor");
		Intent i = new Intent(this, VideoFrameEditActivity.class);
		i.putExtra("framesPerSecond", this.mFps);
		startActivity(i);
	}
	
	public void animateGallery() {
		ArrayList<Double> doubles = new ArrayList<Double>();
		for(Double b : this.mGalleryAsync.mBitmaps){
			doubles.add(b);
		}
		AnimationsHelper.switchImageAnimations(this, mVideoView, doubles, 0, true);
	}
	
	/**
	 * Starts a video clip playing from a given location in milliseconds
	 * @param seek millsecond offset for playback
	 */
	
	private void startClipPlayback(double seek){
		Log.i(TAG,"Starting clip playback at: " + (int)seek);
		Log.i(TAG,"Starting clip playback at double: " + seek);
		if(this.mGalleryAsync != null){
			this.mGalleryAsync.cancel(true);
			this.mGalleryAsync = null;
		}
		if(this.mGalleryAsync == null){
			this.mGalleryAsync = new GetGalleryFramesAtTimeAsyncTask(seek);
		}
		this.mGalleryAsync.setSeek(seek);
		this.mGalleryAsync.execute(1);
	}
	
	public Bitmap getFrameForTime(double time) throws Exception{
		Bitmap map2;
		if((map2 = this.getBitmapFromMemCache(getHashFromString(this.mFullPath) + "_" + String.valueOf(time) + "_thumb")) != null){
	        return map2;
		}
		Log.d(ChooseTenSecondClip.TAG, "Grabber path: " + this.mFullPath);
		FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(this.mFullPath);
		grabber.start();
		grabber.setTimestamp((long)time);
		IplImage _image = grabber.grab();
		//We need to convert from 3 channels to 4 channels so that we can convert
		//from IplImage to Bitmap
		IplImage image = IplImage.create(_image.width() ,_image.height(), opencv_core.IPL_DEPTH_8U,4);
		//source, dest, code
		cvCvtColor(_image,image, opencv_imgproc.CV_BGR2RGBA);
		int width = image.width();
		int height = image.height();
		if(this.mCreateCache == null){
			this.mCreateCache = Bitmap.createBitmap(width,height, Config.ARGB_8888);
		}
		Bitmap map3 = null;
		synchronized(this.mCreateCache){
			//this.mCreateCache.setPixels(null, 0, 0, 0, 0, 0, 0);
			//RGB_565 or ARGB_8888 ( more colors )
			//Make sure you use getByteBuffer()!
			this.mCreateCache.copyPixelsFromBuffer(image.getByteBuffer());
		
        	map3 = ThumbnailUtils.extractThumbnail(this.mCreateCache, 320, 240);
		}
        this.addBitmapToMemoryCache(getHashFromString(this.mFullPath) + "_" + String.valueOf(time) + "_thumb", map3);
        grabber.stop();
        grabber.release();
        _image.release();
        image.release();
        return map3;
	}
	
	public Bitmap getFullFrameForTime(double time) throws Exception{
		//String synthPath = FileUtilsHelper.getFolderForTemporaryMeme(this) + "/tmp-thumb-" + time;
		Bitmap mp;
		if((mp = this.getBitmapFromMemCache(getHashFromString(this.mFullPath) + "_" + String.valueOf(time))) != null){
	        return mp;
		}
		Log.d(ChooseTenSecondClip.TAG, "Grabber path: " + this.mFullPath);
		FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(this.mFullPath);
		grabber.start();
		this.mFps = grabber.getFrameRate();
		grabber.setTimestamp((long)time);
		IplImage _image = grabber.grab();
		//We need to convert from 3 channels to 4 channels so that we can convert
		//from IplImage to Bitmap
		IplImage image = IplImage.create(_image.width() ,_image.height(), opencv_core.IPL_DEPTH_8U,4);
		//source, dest, code
		cvCvtColor(_image,image, opencv_imgproc.CV_BGR2RGBA);
		int width = image.width();
		int height = image.height();
		if(this.mCreateCache != null){
			this.mCreateCache.recycle();
			System.gc();
		}
		this.mCreateCache = null;
		if(this.mCreateCache == null){
			this.mCreateCache = Bitmap.createBitmap(width,height, Config.ARGB_8888);
		}
		synchronized(this.mCreateCache){
			//this.mCreateCache.setPixels(null, 0, 0, 0, 0, 0, 0);
			//RGB_565 or ARGB_8888 ( more colors )
			//Make sure you use getByteBuffer()!
			this.mCreateCache.copyPixelsFromBuffer(image.getByteBuffer());
		}
        this.addBitmapToMemoryCache(getHashFromString(this.mFullPath) + "_" + String.valueOf(time), this.mCreateCache);
        grabber.stop();
        grabber.release();
        _image.release();
        image.release();
        return this.mCreateCache;
	}
	
	private String getHashFromString(String path){
		byte[] d;
		StringBuffer hexString = new StringBuffer();
		try {
			d = MessageDigest.getInstance("MD5").digest(path.getBytes());
			for (int i = 0; i < d.length; i++) {
	            if ((0xff & d[i]) < 0x10) {
	                hexString.append("0"
	                        + Integer.toHexString((0xFF & d[i])));
	            } else {
	                hexString.append(Integer.toHexString(0xFF & d[i]));
	            }
	        }
		} catch (NoSuchAlgorithmException e) {
			Log.e(TAG, "Could not create digest");
			e.printStackTrace();
		}
		return hexString.toString();
	}
	
	private String getVideoMimeDetails() throws IOException {
		if(this.mVideoUri == null){
			return null;
		}
		MediaExtractor extractor = new MediaExtractor();
		 extractor.setDataSource(getRealPathFromURI(this.mVideoUri));
		 int numTracks = extractor.getTrackCount();
		 StringBuilder b = new StringBuilder();
		 for (int i = 0; i < numTracks; ++i) {
		   MediaFormat format = extractor.getTrackFormat(i);
		   String mime = format.getString(MediaFormat.KEY_MIME);
		   b.append(mime + ", ");
		 }
		 extractor.release();
		 return b.toString();
	}
	
	private int getFramesPerSecond() throws Exception{
		if(this.mVideoUri == null){
			return (int)this.mFps;
		}
		Log.d(ChooseTenSecondClip.TAG, "Grabber path: " + this.mFullPath);
		FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(this.mFullPath);
		grabber.start();
		this.mFps = grabber.getFrameRate();
		grabber.stop();
		grabber.release();
		return (int)this.mFps;
	}
	
	private String getRealPathFromURI(Uri contentUri) {
		String cur = null;
		if(contentUri != null){
	    String[] proj = { MediaStore.Images.Media.DATA };
	    CursorLoader loader = new CursorLoader(this, contentUri, proj, null, null, null);
	    Cursor cursor = loader.loadInBackground();
	    int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
	    cursor.moveToFirst();
	    cur = cursor.getString(column_index);
	    cursor.close();
		}
	    return cur;
	}
	
	private double getVideoLength(){
		if(mRetriever == null){
			mRetriever = new MediaMetadataRetriever();
			try {
				mRetriever.setDataSource(this, this.mVideoUri);
			}catch (IllegalArgumentException ex) {
				ex.printStackTrace();
				mRetriever.release();
				mRetriever = null;
		        this.finish();
		        return Double.valueOf(0);
		    } catch (RuntimeException ex) {
		        Toast.makeText(this, R.string.cant_open_video,Toast.LENGTH_SHORT).show();
		        mRetriever.release();
				mRetriever = null;
		        this.finish();
		        return Double.valueOf(0);
		    }
		}
	    return Double.parseDouble(mRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));
	}
	
	private ArrayList<ImageView> getArrayListOfImageViewsForLength(double length){
		Log.i(TAG, "Length of clip as double: " + length);
		double lengthInSeconds = length / (double)1000;
		Log.d(TAG,"Length of Clip Seconds: " + lengthInSeconds);
		float segments = (float)Math.ceil(lengthInSeconds/NUMBER_OF_SECONDS_IN_CLIP);
		ArrayList<ImageView> imageViews = new ArrayList<ImageView>();
		for(int i=0; i < segments; i++){
			ImageView v = new ImageView(this);
			v.setOnClickListener(new OnClickListener(){
				@Override
				public void onClick(View v) {
					Log.d(TAG,"Segment Id : " + (v.getId() - 9000));
					double msOffset = (double)(v.getId() - 9000) * (NUMBER_OF_SECONDS_IN_CLIP * (double)1000); //((int)GOOGLE_MEDIA_PLAYER_CONSTANT / 100);
					startClipPlayback(msOffset);
					ChooseTenSecondClip.this.setMillisecondOffset((int) msOffset);
				}
				
			});
			RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(160, 160);
			v.setId(9000 + i);
			layoutParams.addRule(RelativeLayout.RIGHT_OF, v.getId() - 1);
			v.setPadding(10, 0, 10, 0);
			v.setLayoutParams(layoutParams);
			Drawable d = this.getResources().getDrawable(R.drawable.why);
			v.setImageDrawable(d);
			imageViews.add(v);
		}
		return imageViews;
	}
	
	protected void setMillisecondOffset(int msOffset) {
		mMillisecondOffset = msOffset;
	}
	
	protected int getMillisecondOffset(){
		return mMillisecondOffset;
	}
	
	/**
	 * ------------------------------------------------------------------------
	 * Frame grabber for memes AsyncTask
	 * ------------------------------------------------------------------------
	 */
	
	private class GrabFramesAsyncTask extends AsyncTask <Integer, Integer, Long> {
		private int mStartingOffset;
		private Activity mActivity;
		private ProgressDialog mProgressDialog;
		private Bitmap mPreviousBitmap = null;

		GrabFramesAsyncTask(int startingFrameOffset, Activity act){
			mStartingOffset = startingFrameOffset;
			mActivity = act;
			if(mFullPath == null){
				ChooseTenSecondClip.this.mFullPath = ChooseTenSecondClip.this.getRealPathFromURI(ChooseTenSecondClip.this.mVideoUri);
			}
		}
		
		
		
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			mProgressDialog = new ProgressDialog(mActivity);
			mProgressDialog.setIndeterminate(false);
			mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			mProgressDialog.setTitle(R.string.choose_clip_progress_dialog_description);
			mProgressDialog.setCanceledOnTouchOutside(false);
			mProgressDialog.setOnDismissListener(new OnDismissListener(){

				@Override
				public void onDismiss(DialogInterface arg0) {
					GrabFramesAsyncTask.this.cancel(true);
				}
				
			});
			mProgressDialog.setProgress(0);
			mProgressDialog.show();
			getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
			try {
				ChooseTenSecondClip.this.mFps = getFramesPerSecond();
			} catch (Exception e) {
				Log.e(ChooseTenSecondClip.TAG, "Couldn't determine FPS: " + e.getLocalizedMessage());
				e.printStackTrace();
				ChooseTenSecondClip.this.mFps = ChooseTenSecondClip.FRAMES_PER_SECOND_TO_CAPTURE;
			}
		}



		protected Long doInBackground(Integer... i){
			// get images from time indexes and write to the thumbs folder
			String motionPath = FileUtilsHelper.getFramesFolderForMotionMeme(mActivity);
			Log.d(TAG,"Starting grabbing images and saving them");
			int ufps = ChooseTenSecondClip.this.getUserPreferredFps();
			double frames;
			if(ufps == 1000){
				frames = ChooseTenSecondClip.NUMBER_OF_SECONDS_IN_CLIP * ChooseTenSecondClip.this.mFps;
			}else{
				frames = ufps;
			}
			// mStartingOffset is already in milliseconds so we don't need to convert from seconds
			double timeIndex = (double)mStartingOffset * (GOOGLE_MEDIA_PLAYER_CONSTANT / 1000);
			int iv = 0;
			double clipLength = ChooseTenSecondClip.this.getVideoLength();
			for(iv=0; iv < (int)frames; iv++){
				if(this.isCancelled()){
					return (long)iv;
				}
				double expandedLength = (double) (clipLength * GOOGLE_MEDIA_PLAYER_CONSTANT);
				Log.d(TAG,"Clip Length : " + expandedLength + ", Time Index : " + timeIndex );
				if(timeIndex <= expandedLength ){
					Bitmap b = null;
					try {
						b = getFullFrameForTime(timeIndex);
					} catch (Exception e) {
						Log.e(ChooseTenSecondClip.TAG, "Couldn't grab frame!");
						e.printStackTrace();
					}
					if(b == null || mPreviousBitmap == null || mPreviousBitmap.sameAs(b) == false){
						mPreviousBitmap = b;
						Log.i(ChooseTenSecondClip.TAG, "Writing File... Not a Dup - frame " + iv);
						HashMap<String, Integer> resMap = ChooseTenSecondClip.this.getVideoResolutionSetting();
						Bitmap bShrunk = FileUtilsHelper.shrinkBitmap(b, resMap.get("width"), resMap.get("height"));
						FileUtilsHelper.saveBitmapAsHighQualityPngToLocation(bShrunk, motionPath + "/video-" + iv + "-.png");
						bShrunk.recycle();
					}
					publishProgress((int) ((iv / (float) frames) * 100));
					double finalFrames;
					if(ufps == 1000){
						finalFrames = ChooseTenSecondClip.this.mFps;
					}else{
						finalFrames = (double)ufps;
					}
					timeIndex += (GOOGLE_MEDIA_PLAYER_CONSTANT - 100) / finalFrames;
				}
			}
			return (long)iv;
		}
		protected void onProgressUpdate(Integer... progress){
			float p = progress[0];
			Log.d(TAG, p + " done.");
			mProgressDialog.setProgress((int) Math.ceil((double)p));
			ChooseTenSecondClip.this.updateProgressIndicator(p);
		}
		protected void onCancelled(Long result){
			Log.d(TAG,"frame grabber cancelled");
			publishProgress(0);
			mProgressDialog.dismiss();
			getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
			FileUtilsHelper.removeAllImageFilesInFolder(mActivity);
			AnimationsHelper.restartAnimations();
			ChooseTenSecondClip.this.startClipPlayback(ChooseTenSecondClip.this.mMillisecondOffset);
		}
		protected void onPostExecute(Long result){
			publishProgress(100);
			mProgressDialog.setProgress(100);
			mProgressDialog.dismiss();
			getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
			ChooseTenSecondClip.this.doneSavingClipImages();
			AnimationsHelper.restartAnimations();
			ChooseTenSecondClip.this.startClipPlayback(ChooseTenSecondClip.this.mMillisecondOffset);
		}
	}

	/**
	 * ------------------------------------------------------------------------
	 * Thumbnail Populator AsyncTask
	 * ------------------------------------------------------------------------
	 */
	 private class PopulateThumbnailsAsyncTask extends AsyncTask <Integer, Integer, Long> {
		private ArrayList<Bitmap> mBitmaps;
		private ArrayList<ImageView> mImageViewsAsync;

		PopulateThumbnailsAsyncTask(ArrayList<ImageView> imageViews){
			 mBitmaps = new ArrayList<Bitmap>();
			 mImageViewsAsync = imageViews;
			 if(mFullPath == null){
					ChooseTenSecondClip.this.mFullPath = ChooseTenSecondClip.this.getRealPathFromURI(ChooseTenSecondClip.this.mVideoUri);
				}
		 }
		
		private void publishProgress(int progress){
			ChooseTenSecondClip.this.updateProgressIndicator(progress);
		}
		
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			Log.d(TAG,"Starting to redraw thumbs");
			publishProgress(0);
		}
		
	     protected Long doInBackground(Integer... i) {
	    	double timeIndex = 0;
	    	if(mImageViewsAsync.size() > 0){
	    		 ImageView v = this.mImageViewsAsync.get(0);
	    		 int viewId = v.getId() - 9000;
	    		 Log.d(TAG, "View ID " + viewId);
	    		 timeIndex = (viewId * (NUMBER_OF_SECONDS_IN_CLIP * GOOGLE_MEDIA_PLAYER_CONSTANT));
	    	}
	 		int k, length = mImageViewsAsync.size();
	 		for(k=0; k < length; k++){
	 			if(this.isCancelled()){
	 				return (long)timeIndex;
	 			}
	 			Log.d(TAG,"Thumbnail at time: " + timeIndex);
	 			try {
					mBitmaps.add(getFrameForTime(timeIndex));
				} catch (Exception e) {
					Log.e(ChooseTenSecondClip.TAG, "Couldn't load at index");
					e.printStackTrace();
				}
	 			Log.d(TAG,"Created Bitmap Thumbnail: " + k);
	 			timeIndex += (NUMBER_OF_SECONDS_IN_CLIP * GOOGLE_MEDIA_PLAYER_CONSTANT);
	 			publishProgress((int) ((k / (float) length) * 100), k);
	 		}
	 		return (long)timeIndex;
	     }

	     protected void onProgressUpdate(Integer... progress) {
	         publishProgress(progress[0]);
	         /*ImageView im = mImageViews.get(progress[1]);
	         im.setImageBitmap(mBitmaps.get(progress[1]));
	         Log.d(TAG,"Added image view: " + progress[1]);*/
	     }
	    protected void onCancelled(Long result){
	    	Log.d(TAG,"Thumbnail grabber cancelled");
	    	publishProgress(0);
		}
	     protected void onPostExecute(Long result) {
	    	int i, length = mBitmaps.size();
	        for(i=0; i < length; i++){
	        	ImageView im = mImageViewsAsync.get(i);
	        	im.setImageBitmap(mBitmaps.get(i));
	        	Log.d(TAG,"Added image view: " + i);
		 	}
		 	publishProgress(0);
	     }
	 }
	 
	 /**
		 * ------------------------------------------------------------------------
		 * Gallery Populator AsyncTask
		 * ------------------------------------------------------------------------
		 */
		 private class GetGalleryFramesAtTimeAsyncTask extends AsyncTask <Integer, Integer, Long> {
			private ArrayList<Double> mBitmaps;
			private double mSeek;

			GetGalleryFramesAtTimeAsyncTask(double time){
				 mBitmaps = new ArrayList<Double>();
				 this.mSeek = time;
				 if(mFullPath == null){
						ChooseTenSecondClip.this.mFullPath = ChooseTenSecondClip.this.getRealPathFromURI(ChooseTenSecondClip.this.mVideoUri);
					}
			 }
			
			public void setSeek(double seek) {
				this.mSeek = seek;
				
			}

			private void publishProgress(int progress){
				ChooseTenSecondClip.this.updateProgressIndicator(progress);
			}
			
			@Override
			protected void onPreExecute() {
				super.onPreExecute();
				Log.d(TAG,"Starting to redraw thumbs");
				publishProgress(0);
			}
			
		     protected Long doInBackground(Integer... i) {
		    	double timeIndex = (this.mSeek / 1000) * GOOGLE_MEDIA_PLAYER_CONSTANT;
		 		int k, length = (int)NUMBER_OF_SECONDS_IN_CLIP;
		 		for(k=0; k < length; k++){
		 			if(this.isCancelled()){
		 				return (long)timeIndex;
		 			}
		 			Log.d(TAG,"Thumbnail at time: " + timeIndex);
		 			mBitmaps.add(timeIndex);
		 			Log.d(TAG,"Created Gallery Image: " + k);
		 			timeIndex += GOOGLE_MEDIA_PLAYER_CONSTANT;
		 			publishProgress((int) ((k / (float) length) * 100), k);
		 		}
		 		return (long)timeIndex;
		     }

		     protected void onProgressUpdate(Integer... progress) {
		         publishProgress(progress[0]);
		         /*ImageView im = mImageViews.get(progress[1]);
		         im.setImageBitmap(mBitmaps.get(progress[1]));
		         Log.d(TAG,"Added image view: " + progress[1]);*/
		     }
		    protected void onCancelled(Long result){
		    	Log.d(TAG,"Thumbnail grabber cancelled");
		    	publishProgress(0);
			}
		     protected void onPostExecute(Long result) {
		    	ChooseTenSecondClip.this.animateGallery();
			 	publishProgress(0);
		     }
		 }
		 
		 class InitDiskCacheTask extends AsyncTask<File, Void, Void> {
			    @Override
			    protected Void doInBackground(File... params) {
			        synchronized (mDiskCacheLock) {
			            mDiskLruCache = new DiskLruImageCache(ChooseTenSecondClip.this,
								"thumbnailsetc", DISK_CACHE_SIZE, Bitmap.CompressFormat.PNG, 60);
						mDiskCacheStarting = false; // Finished initialization
						mDiskCacheLock.notifyAll(); // Wake any waiting threads
			           
			        }
			        return null;
			    }
			}
		 
		 class SaveBitmapTask extends AsyncTask<Bitmap, Void, Void> {
			 
			 private String mFilename;
			SaveBitmapTask(String filename){
				 this.mFilename = filename;
			 }
			    @Override
			    protected Void doInBackground(Bitmap... params) {
			        FileUtilsHelper.saveBitmapAsHighQualityPngToLocation(params[0], this.mFilename);
			        return null;
			    }
			}
		 
		 	class SaveBitmapToCacheTask extends AsyncTask<Bitmap, Void, Void> {
			 
			 private String mKey;
			 SaveBitmapToCacheTask(String key){
				 this.mKey = key;
			 }
			    @Override
			    protected Void doInBackground(Bitmap... params) {
			    	// Also add to disk cache
				    synchronized (mDiskCacheLock) {
				        if (mDiskLruCache != null && mDiskLruCache.getBitmap(String.valueOf(mKey)) == null) {
				            mDiskLruCache.put(String.valueOf(mKey), params[0]);
				        }
				    }
			        return null;
			    }
			}
}
