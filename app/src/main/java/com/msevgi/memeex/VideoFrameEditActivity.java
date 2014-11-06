package com.msevgi.memeex;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;

import us.sigsegv.android.maximumultimatememecreatorxturbo.R;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnDismissListener;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.NavUtils;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.util.Log;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

public class VideoFrameEditActivity extends FragmentActivity {

	protected static final Integer EDIT_MODE = 1;

	protected static final Integer NORMAL_MODE = 2;

	protected static final String TAG = VideoFrameEditActivity.class.getSimpleName();

	/**
	 * The {@link android.support.v4.view.PagerAdapter} that will provide
	 * fragments for each of the sections. We use a
	 * {@link android.support.v4.app.FragmentPagerAdapter} derivative, which
	 * will keep every loaded fragment in memory. If this becomes too memory
	 * intensive, it may be best to switch to a
	 * {@link android.support.v4.app.FragmentStatePagerAdapter}.
	 */
	SectionsPagerAdapter mSectionsPagerAdapter;

	/**
	 * The {@link ViewPager} that will host the section contents.
	 */
	ViewPager mViewPager;

	private String mClipTitle;
	
	public float mFps;

	public String getClipTitle() {
		return mClipTitle;
	}



	public void setClipTitle(String mClipTitle) {
		this.mClipTitle = mClipTitle;
	}



	public String getClipDescription() {
		return mClipDescription;
	}



	public void setClipDescription(String mClipDescription) {
		this.mClipDescription = mClipDescription;
	}
	
	/**
	 * Will get the image resolution setting from prefs
	 * @return A map of image width and height
	 */
	public HashMap<String,Integer> getImageResolutionSetting(){
		HashMap<String, Integer> resMap = new HashMap<String,Integer>();
		SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
		String syncConnPref = sharedPref.getString(SettingsActivity.KEY_PREF_MEME_IMAGE_RES, "640x480");
		String[] split = syncConnPref.split("x");
		resMap.put("width", Integer.parseInt(split[0]));
		resMap.put("height", Integer.parseInt(split[1]));
		return resMap;
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

	private String mClipDescription;

	private ArrayList<File> mFiles;
	
	private ArrayList<File> mFilesToDelete = new ArrayList<File>();

	public boolean mPublishComplete = false;

	public String mPath;

	private AsyncTask<Integer, Integer, Long> mPublishVideoAsync;

	protected int mCurrentPage;

	protected ArrayList<Integer> mFramesToDelete;

	protected ActionMode mActionMode;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_video_frame_edit);
		Intent i = this.getIntent();
		if(i != null){
			int ufps = this.getUserPreferredFps();
			// if the user has set their preferred fps to default, just use the old value
			// otherwise, set mFps to the other value just in case
			if(ufps == 1000){
				this.mFps = (float)i.getDoubleExtra("framesPerSecond", (double)12);
			}else{
				this.mFps = ufps;
			}
		}else{
			this.mFps = (float)this.getUserPreferredFps();
		}
		// Show the Up button in the action bar.
		getActionBar().setDisplayHomeAsUpEnabled(true);

		// Create the adapter that will return a fragment for each of the three
		// primary sections of the app.
		mSectionsPagerAdapter = new SectionsPagerAdapter(
				getSupportFragmentManager());

		// Set up the ViewPager with the sections adapter.
		mViewPager = (ViewPager) findViewById(R.id.pager);
		mViewPager.setAdapter(mSectionsPagerAdapter);
		mViewPager.setOnPageChangeListener(new OnPageChangeListener(){

			@Override
			public void onPageSelected(int arg0) {
				VideoFrameEditActivity.this.mCurrentPage = arg0;
			}

			@Override
			public void onPageScrollStateChanged(int arg0) {
				// I don't really want to override this method, there is no
				// information that I need from this
				
			}

			@Override
			public void onPageScrolled(int arg0, float arg1, int arg2) {
				
			}
			
		});
		if(savedInstanceState != null){
			String title = savedInstanceState.getString("clipTitle");
			if(title != null){
				setClipTitle(title);
			}
			String description = savedInstanceState.getString("clipDescription");
			if(description != null){
				setClipDescription(description);
			}
			String path = savedInstanceState.getString("videoPath");
			if(path != null){
				mPath = path;
			}
			boolean publishComplete = savedInstanceState.getBoolean("videoProcessingComplete");
			if(publishComplete == true){
				this.mPublishComplete = true;
			}
			ArrayList<CharSequence> seq = savedInstanceState.getCharSequenceArrayList("files");
			this.mFiles = new ArrayList<File>();
			for(CharSequence s : seq){
				this.mFiles.add(new File((String) s));
			}
			int currentPage = savedInstanceState.getInt("currentPage");
			if(currentPage != 0){
				this.mCurrentPage = currentPage;
			}
			float fps = savedInstanceState.getFloat("fps");
			if(fps > 0){
				this.mFps = fps;
			}
		}
	}
	
	private ActionMode.Callback mActionModeCallback = new ActionMode.Callback() {
		
		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
			// Don't do anything, but we want to return false
			return false;
		}
		
		@Override
		public void onDestroyActionMode(ActionMode mode) {
			mode.setTag(VideoFrameEditActivity.NORMAL_MODE);
			VideoFrameEditActivity.this.mActionMode = null;
			VideoFrameEditActivity.this.mFilesToDelete.clear();
			ViewPager pager = (ViewPager)findViewById(R.id.pager);
			pager.getAdapter().notifyDataSetChanged();
			
		}
		
		@Override
		public boolean onCreateActionMode(ActionMode mode, Menu menu) {
			mode.setTitle(R.string.delete_frames);
			getMenuInflater().inflate(R.menu.delete_context_menu, menu);
			return true;
		}
		
		@Override
		public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
			switch(item.getItemId()){
			case R.id.delete_selected_frames:
				removeDeletedFiles();
				mode.finish();
				break;
			case R.id.select_all:
				Log.d(VideoFrameEditActivity.TAG, "User Chose to select all!");
				selectAllFramesToggle(item);
				break;
			}
			return false;
		}

		private void selectAllFramesToggle(MenuItem item) {
			String selectAll = VideoFrameEditActivity.this.getResources().getString(R.string.select_all_frames);
			//String unselectAll = VideoFrameEditActivity.this.getResources().getString(R.string.unselect_all_frames);
			if(item.getTitle().equals(selectAll)){
				ArrayList<File> fs = VideoFrameEditActivity.this.getFiles();
				VideoFrameEditActivity.this.mFilesToDelete.addAll(fs);
				ViewPager pager = (ViewPager)findViewById(R.id.pager);
				int i, pageCount = pager.getChildCount();
				for(i=0;i<pageCount;i++){
					pager.getChildAt(i).setSelected(true);
				}
				item.setTitle(R.string.unselect_all_frames);
				((SectionsPagerAdapter)pager.getAdapter()).updateBitmapCount();
				pager.getAdapter().notifyDataSetChanged();
				VideoFrameEditActivity.this.mActionMode.setTitle(String.format(VideoFrameEditActivity.this.getString(R.string.delete_frames_detail), VideoFrameEditActivity.this.mFilesToDelete.size()));
			}else{
				VideoFrameEditActivity.this.mFilesToDelete.clear();
				ViewPager pager = (ViewPager)findViewById(R.id.pager);
				int i, pageCount = pager.getChildCount();
				for(i=0;i<pageCount;i++){
					pager.getChildAt(i).setSelected(false);
				}
				item.setTitle(R.string.select_all_frames);
				((SectionsPagerAdapter)pager.getAdapter()).updateBitmapCount();
				pager.getAdapter().notifyDataSetChanged();
			}
		}
	};
	
	public void removeDeletedFiles(){
		if(VideoFrameEditActivity.this.mFilesToDelete.size() == 0){
			return;
		}
		ViewPager pager = (ViewPager)findViewById(R.id.pager);
		for(File fileToBeDeleted : VideoFrameEditActivity.this.mFilesToDelete){
			FileUtilsHelper.deleteAllItemsForPath(fileToBeDeleted.getAbsolutePath(), VideoFrameEditActivity.this);
			VideoFrameEditActivity.this.mFiles.remove(fileToBeDeleted);
			if(VideoFrameEditActivity.this.mCurrentPage > VideoFrameEditActivity.this.mFiles.size() - 1){
				VideoFrameEditActivity.this.mCurrentPage = VideoFrameEditActivity.this.mFiles.size() - 1;
			}
		}
		VideoFrameEditActivity.this.mFilesToDelete.clear();
		((SectionsPagerAdapter)pager.getAdapter()).updateBitmapCount();
		pager.getAdapter().notifyDataSetChanged();
	}
	
	public void onDeleteFrame() {
		ViewPager pager = (ViewPager)findViewById(R.id.pager);
		if(VideoFrameEditActivity.this.mFiles.size() == 1){
			Builder d = new Builder(this);
			d.setTitle(R.string.dialog_error_title);
			d.setMessage(R.string.dialog_cant_delete_only_frame);
			d.setPositiveButton("OK", null);
			d.create().show();
			return;
		}
		/*if(this.mCurrentPage == 0){
			Builder d = new AlertDialog.Builder(this);
			d.setTitle("Error");
			d.setMessage(R.string.dialog_cant_delete_first_frame);
			d.setPositiveButton("OK", null);
			d.create().show();
			return;
		}*/
		File fileToBeDeleted = VideoFrameEditActivity.this.mFiles.get(VideoFrameEditActivity.this.mCurrentPage);
		FileUtilsHelper.deleteAllItemsForPath(fileToBeDeleted.getAbsolutePath(), VideoFrameEditActivity.this);
		VideoFrameEditActivity.this.mFiles.remove(fileToBeDeleted);
		if(VideoFrameEditActivity.this.mCurrentPage > VideoFrameEditActivity.this.mFiles.size() - 1){
			VideoFrameEditActivity.this.mCurrentPage = VideoFrameEditActivity.this.mFiles.size() - 1;
		}
		((SectionsPagerAdapter)pager.getAdapter()).updateBitmapCount();
		pager.getAdapter().notifyDataSetChanged();
	}



	@Override
	protected void onResume() {
		super.onResume();
		Intent i = getIntent();
		if(getClipTitle() == null){
		setClipTitle(i.getStringExtra("com.samsung.smcl.maximumultimatememecreateorxturbo.title"));
		}
		if(getClipDescription() == null){
			setClipDescription(i.getStringExtra("com.samsung.smcl.maximumultimatememecreatorxturbo.description"));
		}
		setFiles(FileUtilsHelper.getListOfIncludedBitmapsForAniGif(this));
		ViewPager pager = (ViewPager)findViewById(R.id.pager);
		pager.getAdapter().notifyDataSetChanged();
	}



	@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState.putString("clipTitle", getClipTitle());
		outState.putString("clipDescription", getClipDescription());
		outState.putString("videoPath", mPath);
		outState.putBoolean("videoProcessingComplete", mPublishComplete);
		outState.putFloat("fps", this.mFps);
		ArrayList<CharSequence> stringFiles = new ArrayList<CharSequence>();
		for(File f : this.mFiles){
			stringFiles.add(f.getAbsolutePath());
		}
		outState.putCharSequenceArrayList("files", stringFiles);
		outState.putInt("currentPage", mCurrentPage);
		super.onSaveInstanceState(outState);
	}



	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_video_frame_edit, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Log.d("VideoFrameEditActivity::onOptionsItemSelected","Menu Item id Value " + item.getItemId());
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
			break;
		case R.id.publish_video:
			if(this.mPublishComplete == false){
				mPublishVideoAsync = new PublishVideoAsyncTask().execute(1);
			}else{
				if(this.mPath != null){
					this.shareContent();
				}else{
					this.mPublishComplete = false;
					mPublishVideoAsync = new PublishVideoAsyncTask().execute(1);
				}
			}
			break;
		case R.id.preview_video:
			mPublishVideoAsync = new PublishVideoAsyncTask().execute(2);
			
			break;
		case R.id.save_video:
			mPublishVideoAsync = new PublishVideoAsyncTask().execute(3);
			break;
		case R.id.delete_frame:
			onDeleteFrame();
			break;
		case R.id.menu_settings:
			Intent i = new Intent(this, SettingsActivity.class);
			startActivity(i);
			break;
		}
		return super.onOptionsItemSelected(item);
	}
	
	
	
	@Override
	protected void onDestroy() {
		if(mPublishVideoAsync != null){
			if(!mPublishVideoAsync.isCancelled()){
				mPublishVideoAsync.cancel(false);
			}
		}
		super.onDestroy();
	}



	public void updateProgressIndicator(float p) {
		ProgressBar prog = (ProgressBar)findViewById(R.id.progressBar1);
		prog.setMax(100);
		prog.setProgress((int)Math.ceil(p));
	}

	public ArrayList<File> getFiles() {
		return mFiles;
	}



	public void setFiles(ArrayList<File> files) {
		this.mFiles = files;
	}
	public void shareContent(){
		Intent shareCaptionIntent = new Intent(Intent.ACTION_SEND);
		String captionString = "Created with Maximum Ultimate Meme Creator X Turbo!";
	    shareCaptionIntent.setType("image/gif");

	    //set photo
	    Uri examplePhoto = Uri.fromFile(new File(mPath));
	    this.getIntent().setData(examplePhoto);
	    this.getIntent().setType("image/gif");
	    shareCaptionIntent.putExtra(Intent.EXTRA_STREAM, examplePhoto);

	    //set caption
	    shareCaptionIntent.putExtra(Intent.EXTRA_TEXT, captionString);
	    shareCaptionIntent.putExtra(Intent.EXTRA_SUBJECT, captionString);
	    shareCaptionIntent.putExtra(Intent.EXTRA_TITLE, captionString);
	    startActivity(Intent.createChooser(shareCaptionIntent,"Share Meme"));
	}

	/**
	 * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
	 * one of the sections/tabs/pages.
	 */
	public class SectionsPagerAdapter extends FragmentPagerAdapter  {

		private int mBitmaps;

		public SectionsPagerAdapter(FragmentManager fm) {
			super(fm);
			updateBitmapCount();
		}
		
		public void updateBitmapCount(){
			mBitmaps = FileUtilsHelper.getListOfIncludedBitmapsForAniGif(VideoFrameEditActivity.this).size();
		}

		@Override
		public Fragment getItem(int position) {
			// getItem is called to instantiate the fragment for the given page.
			// Return a DummySectionFragment (defined as a static inner class
			// below) with the page number as its lone argument.
			Fragment fragment = new DummySectionFragment();
			Bundle args = new Bundle();
			args.putInt(DummySectionFragment.ARG_SECTION_NUMBER, position);
			fragment.setArguments(args);
			return fragment;
		}

		@Override
	    public int getItemPosition(Object object){
	        //return ((Fragment)object).getArguments().getInt(DummySectionFragment.ARG_SECTION_NUMBER);
			return POSITION_NONE;
	    }

		@Override
		public int getCount() {
			// Show 3 total pages.
			return mBitmaps;
		}

		@Override
		public CharSequence getPageTitle(int position) {
			return "Frame " + (position + 1);
		}
	}

	/**
	 * A dummy fragment representing a section of the app, but that simply
	 * displays dummy text.
	 */
	public static class DummySectionFragment extends Fragment {
		/**
		 * The fragment argument representing the section number for this
		 * fragment.
		 */
		public static final String ARG_SECTION_NUMBER = "section_number";

		public DummySectionFragment() {
		}
		
		/**
		 * ------------------------------------------------------------------------
		 * Primary Image Populator AsyncTask
		 * ------------------------------------------------------------------------
		 */
		 private class LoadPrimaryImageAsyncTask extends AsyncTask <Integer, Integer, Long> {

		     private ImageView mImage;
		     private String mPath;
		     private Bitmap mBitmap;
		     
		     LoadPrimaryImageAsyncTask(ImageView imv, String path){
		    	 this.mImage = imv;
		    	 this.mPath = path;
		     }

		     protected Long doInBackground(Integer... i) {
		    	 this.mBitmap = FileUtilsHelper.loadCompressedImageFromDiskAtPath(mPath);
		    	 return (long)3;
		     }
		     protected void onProgressUpdate(Integer... progress) {
		         
		     }

		     protected void onPostExecute(Long result) {
		    	 mImage.setImageBitmap(this.mBitmap);
		     }
		 }

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			// Create a new TextView and set its text to the fragment's section
			// number argument value.
			ImageView imageView = new ImageView(getActivity());
			imageView.setClickable(true);
			int sectionId = getArguments().getInt(
					ARG_SECTION_NUMBER);
			ArrayList<File> files = ((VideoFrameEditActivity)getActivity()).getFiles();
			Log.d("VideoFrameEditActivity::onCreateView()","Section Id: " + sectionId);
			Log.d("VideoFrameEditActivity::onCreateView()","Number of files : " + files.size());
			File file = files.get(sectionId);
			if(((VideoFrameEditActivity)getActivity()).mFilesToDelete.contains(file)){
				((ImageView)imageView).setColorFilter(getActivity().getResources().getColor(R.color.selected_image));
				imageView.setSelected(true);
			}else{
				imageView.setSelected(false);
				imageView.clearColorFilter();
			}
			String path = file.getAbsolutePath();
			imageView.setId(sectionId);
			new LoadPrimaryImageAsyncTask(imageView, path).execute(1);
			final String displayImageFile = FileUtilsHelper.getPreEditPathForEditedFile(file,getActivity());
			imageView.setOnClickListener(new OnClickListener(){

				@Override
				public void onClick(View v) {
					VideoFrameEditActivity act = (VideoFrameEditActivity)getActivity();
					if(act.mActionMode == null || act.mActionMode.getTag() != VideoFrameEditActivity.EDIT_MODE ){
						Intent editImage = new Intent(getActivity(), EditMemeImage.class);
						String res = displayImageFile;
						editImage.putExtra("com.samsung.smcl.maximumultimatememecreatorxturbo.path", res);
						editImage.putExtra("com.samsung.smcl.maximumultimatememecreatorxturbo.video", true);
						startActivity(editImage);
					}else{
						setImageViewToSelectedAndAddToDeleteStack(v);
					}
				}
				
			});
			imageView.setOnLongClickListener(new OnLongClickListener(){

				@Override
				public boolean onLongClick(View v) {
					VideoFrameEditActivity act = (VideoFrameEditActivity)getActivity();
					if(act.mActionMode != null){
						return false;
					}
					act.mActionMode = getActivity().startActionMode(act.mActionModeCallback);
					act.mActionMode.setTag(VideoFrameEditActivity.EDIT_MODE);
					setImageViewToSelectedAndAddToDeleteStack(v);
					return true;
				}
				
			});
			return imageView;
		}
		
		public void setImageViewToSelectedAndAddToDeleteStack(View v){
			VideoFrameEditActivity act = (VideoFrameEditActivity)getActivity();
			if(v.isSelected() == true){
				v.setSelected(false);
				((ImageView)v).clearColorFilter();
				int id = v.getId();
				ArrayList<File> files = ((VideoFrameEditActivity)getActivity()).getFiles();
				Log.d(VideoFrameEditActivity.TAG,"Section Id: " + id);
				Log.d(VideoFrameEditActivity.TAG,"Number of files : " + files.size());
				File file = files.get(id);
				act.mFilesToDelete.remove(file);
				act.mActionMode.setTitle(String.format(act.getString(R.string.delete_frames_detail), act.mFilesToDelete.size()));
			}else{
				v.setSelected(true);
				((ImageView)v).setColorFilter(act.getResources().getColor(R.color.selected_image));
				int id = v.getId();
				ArrayList<File> files = ((VideoFrameEditActivity)getActivity()).getFiles();
				Log.d(VideoFrameEditActivity.TAG,"Section Id: " + id);
				Log.d(VideoFrameEditActivity.TAG,"Number of files : " + files.size());
				File file = files.get(id);
				act.mFilesToDelete.add(file);
				act.mActionMode.setTitle(String.format(act.getString(R.string.delete_frames_detail), act.mFilesToDelete.size()));
			}
		}
	}
	
	/**
	 * ------------------------------------------------------------------------
	 * Thumbnail Populator AsyncTask
	 * ------------------------------------------------------------------------
	 */
	 private class PublishVideoAsyncTask extends AsyncTask <Integer, Integer, Long> {

	     private String mPath;
	     private ProgressDialog mProgressDialog;
		private int mPreview = 1;
		private int PREVIEW_ONLY = 2;
		private int PUBLISH_ONLY = 1;
		private int SAVE_ONLY = 3;
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			mProgressDialog = new ProgressDialog(VideoFrameEditActivity.this);
			mProgressDialog.setIndeterminate(false);
			mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			mProgressDialog.setCanceledOnTouchOutside(false);
			mProgressDialog.setTitle(R.string.publish_video_gif_progress_dialog_description);
			mProgressDialog.setOnDismissListener(new OnDismissListener(){

				@Override
				public void onDismiss(DialogInterface arg0) {
					PublishVideoAsyncTask.this.cancel(true);
				}
				
			});
			mProgressDialog.setProgress(0);
			mProgressDialog.show();
			getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		}

		protected Long doInBackground(Integer... i) {
			if(i[0].equals(Integer.valueOf(2))){
				mPreview = PREVIEW_ONLY;
			}else if(i[0].equals(Integer.valueOf(3))){
				mPreview = SAVE_ONLY;
			}
			String fileName = VideoFrameEditActivity.this.getClipTitle() + "-" + VideoFrameEditActivity.this.getClipDescription() + ".gif";
	    	mPath = FileUtilsHelper.getPathForAniGifOutput(VideoFrameEditActivity.this) + "/" + fileName;
	    	try{
		 		FileOutputStream fos = new FileOutputStream(new File(mPath));
		 		BufferedOutputStream buf = new BufferedOutputStream(fos);
		 		AnimatedGifEncoder encoder = new AnimatedGifEncoder();
		 		encoder.setRepeat(0);
		 		encoder.setQuality(20);
		 		int userFps = VideoFrameEditActivity.this.getUserPreferredFps();
		 		if(userFps == 1000){
		 			encoder.setFrameRate(VideoFrameEditActivity.this.mFps);
		 		}else{
		 			encoder.setFrameRate(userFps);
		 		}
		 		encoder.start(buf);
		 		ArrayList<File> files = FileUtilsHelper.getListOfIncludedBitmapsForAniGif(VideoFrameEditActivity.this);
		 		int progress = 0;
		 		for(File f : files){
		 			if(this.isCancelled()){
		 				return (long)1;
		 			}
		 			//Bitmap b = FileUtilsHelper.shrinkBitmap(f.getAbsolutePath(), 640, 480);
		 			Bitmap b = FileUtilsHelper.loadCompressedImageFromDiskAtPathForGif(f.getAbsolutePath());
		 			encoder.addFrame(b);
		 			b.recycle();
		 			progress = progress + 1;
		 			publishProgress((int) ((progress / (float) files.size()) * 100));
		 		}
		 		encoder.finish();
	    	}catch(Exception e){
	    		Log.e("VideoFrameEditActivity::PublishVideoAsyncTask::doInBackground()","Exception writing gif");
	    		e.printStackTrace();
	    	}
	 		return (long)1;
	     }

	     protected void onProgressUpdate(Integer... progress) {
	    	 float p = progress[0];
	    	 mProgressDialog.setProgress((int) Math.ceil((double)p));
			 Log.d("ChooseTenSecondClip::GrabFramesAsyncTask::onProgressUpdate()", p + " done.");
			 VideoFrameEditActivity.this.updateProgressIndicator(p);
	     }
	     
	     protected void onCancelled(Long result){
	    	 getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
				publishProgress(0);
				mProgressDialog.setProgress(0);
				mProgressDialog.dismiss();
			}

	     protected void onPostExecute(Long result) {
			VideoFrameEditActivity.this.updateProgressIndicator(0);
			VideoFrameEditActivity.this.mPublishComplete  = true;
			VideoFrameEditActivity.this.mPath = this.mPath;
			mProgressDialog.setProgress(100);
			mProgressDialog.dismiss();
			getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
			if(mPreview == PUBLISH_ONLY){
				File dest = FileUtilsHelper.copyMemeGifIntoGallery(this.mPath);
		    	Log.d("VideoFrameEditActivity::PublishVideoAsyncTask::onPostExecute()","Finished, let's publish this!");
		 		FileUtilsHelper.runMediaScanner(VideoFrameEditActivity.this, dest.getAbsolutePath());
		    	Intent shareCaptionIntent = new Intent(Intent.ACTION_SEND);
		 		String captionString = "Created with Maximum Ultimate Meme Creator X Turbo!";
		 	    shareCaptionIntent.setType("image/gif");
		 	    
		 	    //set photo
		 	    Uri examplePhoto = Uri.fromFile(new File(mPath));
		 	    VideoFrameEditActivity.this.getIntent().putExtra("resultContent", examplePhoto.toString());
		 	    shareCaptionIntent.putExtra(Intent.EXTRA_STREAM, examplePhoto);
	
		 	    //set caption
		 	    shareCaptionIntent.putExtra(Intent.EXTRA_TEXT, captionString);
		 	    shareCaptionIntent.putExtra(Intent.EXTRA_SUBJECT, captionString);
		 	    shareCaptionIntent.putExtra(Intent.EXTRA_TITLE, captionString);
		 	    startActivity(Intent.createChooser(shareCaptionIntent,"Share Meme"));
			}else if(mPreview == PREVIEW_ONLY){
				Uri pathUri = Uri.fromFile(new File(mPath));
				Intent i = new Intent(VideoFrameEditActivity.this, PreviewActivity.class);
				i.putExtra("com.samsung.smcl.maximumultimatememecreatorxturbo.preview-uri",pathUri.toString());
				startActivity(i);
			}else{
				File dest = FileUtilsHelper.copyMemeGifIntoGallery(this.mPath);
				FileUtilsHelper.runMediaScanner(VideoFrameEditActivity.this, dest.getAbsolutePath());
				Uri examplePhoto = Uri.fromFile(new File(this.mPath));
				VideoFrameEditActivity.this.getIntent().putExtra("resultContent", examplePhoto.toString());
				Toast.makeText(VideoFrameEditActivity.this, "Saved to Gallery", Toast.LENGTH_SHORT).show();
			}
	     }
	 }
}
