package com.msevgi.memeex;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;

import us.sigsegv.android.maximumultimatememecreatorxturbo.R;
import com.samsung.spensdk.multiwindow_applistener.SMultiWindowDropListener;

import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnDismissListener;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.support.v4.app.NavUtils;
import android.util.Log;
import android.view.DragEvent;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

public class EditMemeImage extends Activity {
	private static final String TAG = EditMemeImage.class.getSimpleName();
	public static final int SMALL_TEXT_SIZE = 1;
	public static final int MEDIUM_TEXT_SIZE = 2;
	public static final int LARGE_TEXT_SIZE = 3;
	public static final int SMALL_FONT_SIZE = 32;
	public static final int MEDIUM_FONT_SIZE = 48;
	public static final int LARGE_FONT_SIZE = 56;
	private Bitmap mImageToEdit;
	private String mBaseImage;
	private int mTextSize = 56;
	private int mCurrentTextSize = 3;
	private boolean mIsVideo;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_edit_meme_image);
		// Show the Up button in the action bar.
		getActionBar().setDisplayHomeAsUpEnabled(true);
		if(savedInstanceState != null){
			boolean isVideo = savedInstanceState.getBoolean("isVideo");
			if(isVideo == true){
				mIsVideo = true;
			}
			byte[] barr = savedInstanceState.getByteArray("imageToEdit");
			if(barr != null){
				mImageToEdit = FileUtilsHelper.convertByteArrayToBitmap(barr);
			}
			String baseImage = savedInstanceState.getString("baseImage");
			if(baseImage != null){
				mBaseImage = baseImage;
			}
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_edit_meme_image, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Log.d("VideoFrameEditActivity::onOptionsItemSelected","Menu Item id Value " + item.getItemId());
		ImageView pic = (ImageView) this.findViewById(R.id.imageView1);
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
		case R.id.menu_settings:
			Intent i2 = new Intent(this, SettingsActivity.class);
			startActivity(i2);
			break;
		case R.id.preview:
			updateText(pic);
			Bitmap b = pic.getDrawingCache();
			String imagePath = FileUtilsHelper.saveBitmapAsJpeg(b, this);
			Log.d("EditMemeImage::onOptionsItemSelected()","Image Path - " + imagePath);
			Uri pathUri = Uri.fromFile(new File(imagePath));
			Intent i = new Intent(this, PreviewActivity.class);
			i.putExtra("com.samsung.smcl.maximumultimatememecreatorxturbo.preview-uri",pathUri.toString());
			startActivity(i);
			break;
		case R.id.publish_frame:
			publishMemeImage(pic);
		case R.id.apply_text:
			if(this.mIsVideo == true){
				publishImageToIntent(pic);
			}else{
				this.updateText(pic);
			}
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// I don't want to override, this but I feel that I must here to 
		// keep passing the value back up and out
		super.onActivityResult(requestCode, resultCode, data);
	}

	@Override
	protected void onPause() {
		View layout = (View) this.findViewById(R.id.editMemeImageLayout);
		layout.setOnDragListener(null);
		super.onPause();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState.putBoolean("isVideo", mIsVideo);
		outState.putByteArray("imageToEdit",FileUtilsHelper.convertBitmapToByteArray(mImageToEdit));
		outState.putString("baseImage", mBaseImage);
		super.onSaveInstanceState(outState);
	}
	
	public Bitmap loadBitmap(Uri uri)
	{

	    Bitmap tempBitmap = null;
	    ContentResolver cr = getContentResolver();
	    try {           
	      InputStream in = cr.openInputStream(uri);
	      BitmapFactory.Options options = new BitmapFactory.Options();
	      options.inSampleSize=8;
	      tempBitmap = BitmapFactory.decodeStream(in,null,options);
	    }catch (Exception ee) {
	      Log.e(EditMemeImage.TAG, "Couldn't load bitmap");
	      ee.printStackTrace();
	    }
	    return tempBitmap;
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

	@Override
	protected void onResume() {
		super.onResume();
		Intent intent = getIntent();
		String pict = intent.getStringExtra("com.samsung.smcl.maximumultimatememecreatorxturbo.path");
		if(pict == null){
			pict = intent.getDataString();
		}
		if(mIsVideo == false){
			mIsVideo = intent.getBooleanExtra("com.samsung.smcl.maximumultimatememecreatorxturbo.video",false);
		}
		Log.d("EditMemeImage::onResume()","Path from intent " + pict);
		if(mImageToEdit == null){
			if(pict != null){
				Bitmap b = FileUtilsHelper.loadCompressedImageFromDiskAtPath(pict);
				if(b.getWidth() < getImageResolutionSetting().get("width")){
					Log.d(TAG,"Scaling Bitmap Up to " + getImageResolutionSetting().get("width").toString() + " x " + getImageResolutionSetting().get("height").toString());
					mImageToEdit = FileUtilsHelper.scaleBitmap(pict, getImageResolutionSetting().get("width"), getImageResolutionSetting().get("height"));
					Log.d(TAG,"Resulting Image Width " + mImageToEdit.getWidth());
				}else{
					mImageToEdit = b;
				}
			}else{
				Log.e("EditMemeImage::onResume()","Somehow the path got lost for loading the image to edit");
				this.finish();
			}
		}
		if(mBaseImage == null){
			mBaseImage = pict;
		}
		ImageView pic = (ImageView) this.findViewById(R.id.imageView1);
		pic.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View arg0) {
				EditMemeImage.this.updateText(EditMemeImage.this.findViewById(R.id.imageView1));
			}
			
		});
		pic.setOnLongClickListener(new OnLongClickListener(){

			@Override
			public boolean onLongClick(View arg0) {
				EditMemeImage.this.resizeText();
				return false;
			}
			
		});
		View layout = (View) this.findViewById(R.id.editMemeImageLayout);
		/*View.OnLongClickListener onLongClickListener = new View.OnLongClickListener() {
			  public boolean onLongClick(View v) {
			String[] mimeTypes = {"image/*", "video/*"};
			    // Create the ClipData
			    ClipData dragData = new ClipData( getLocalClassName(), mimeTypes, 
			new ClipData.Item("Drag & Drop"));
			    ClipData.Item uriItem = new ClipData.Item(Uri.parse("http://www.samsung.net/"));
			    dragData.addItem(uriItem);
			    // Create the DragShadow to be displayed while dragging
			    DragShadowBuilder dragShadowBuilder = new DragShadowBuilder(v);
			    // Begin the drag
			return v.startDrag(dragData, dragShadowBuilder, null, 0); 
			}   };

			// Register View.OnLongClickListener in the view that receives the long click event
			layout.setOnLongClickListener(onLongClickListener);*/
		SMultiWindowDropListener dl = new SMultiWindowDropListener(){

			@Override
			public void onDrop(DragEvent event) {
				ClipData clipData = event.getClipData();
				ImageView pic = (ImageView) EditMemeImage.this.findViewById(R.id.imageView1);
				if(clipData != null){
					int count = clipData.getItemCount();
					for(int index = 0; index < count; ++index){
						ClipData.Item item = clipData.getItemAt(index);
						if(item.getUri() != null){
							Log.d(EditMemeImage.TAG, "It's a URI");
							Uri uri = item.getUri();
							ContentResolver cR = EditMemeImage.this.getContentResolver();
							String type = cR.getType(uri);
							Intent i = new Intent();
							i.setType(type);
							i.setData(uri);
							Log.d(EditMemeImage.TAG, "Handling type: " + type);
							if(type.contains("image")){
								EditMemeImage.this.updateText(pic);
								Bitmap b = loadBitmap(uri);
								b.setDensity(EditMemeImage.this.mImageToEdit.getDensity());
								if(b != null){
									Resources r = getResources();
									Drawable[] layers = new Drawable[2];
									layers[0] = new BitmapDrawable(r, EditMemeImage.this.mImageToEdit);
									layers[1] = new BitmapDrawable(r, b);
									LayerDrawable layerDrawable = new LayerDrawable(layers);
									pic.setImageDrawable(layerDrawable);
									EditMemeImage.this.mImageToEdit = pic.getDrawingCache();
									FileUtilsHelper.saveBitmapAsHighQualityPngToLocation(EditMemeImage.this.mImageToEdit, EditMemeImage.this.mBaseImage);
								}
							}else{
								Toast.makeText(EditMemeImage.this, EditMemeImage.this.getResources().getString(R.string.invalid_drop_content) , Toast.LENGTH_SHORT).show();
							}
						}
						else if(item.getIntent() != null){
							Intent i = item.getIntent();
							String type = i.getType();
							Uri uri = i.getData();
							Log.d(EditMemeImage.TAG, "Handling type: " + type);
							 if(type.contains("image")){
								 EditMemeImage.this.updateText(pic);
									Bitmap b = loadBitmap(uri);
									if(b != null){
										Resources r = getResources();
										Drawable[] layers = new Drawable[2];
										layers[0] = new BitmapDrawable(r, EditMemeImage.this.mImageToEdit);
										layers[1] = new BitmapDrawable(r, b);
										LayerDrawable layerDrawable = new LayerDrawable(layers);
										pic.setImageDrawable(layerDrawable);
										EditMemeImage.this.mImageToEdit = pic.getDrawingCache();
										FileUtilsHelper.saveBitmapAsHighQualityPngToLocation(EditMemeImage.this.mImageToEdit, EditMemeImage.this.mBaseImage);
									}
							}else{
								Toast.makeText(EditMemeImage.this, EditMemeImage.this.getResources().getString(R.string.invalid_drop_content) , Toast.LENGTH_SHORT).show();
							}
						}
					}
				}
			}
			
		};
		layout.setOnDragListener(dl);
		pic.setDrawingCacheEnabled(true);
		pic.setImageBitmap(mImageToEdit);
		EditText title = (EditText) this.findViewById(R.id.editText1);
		title.setOnEditorActionListener(new OnEditorActionListener(){

			@Override
			public boolean onEditorAction(TextView arg0, int arg1, KeyEvent arg2) {
				ImageView pic = (ImageView) EditMemeImage.this.findViewById(R.id.imageView1);
				EditMemeImage.this.updateText(pic);
				return false;
			}
			
		});
		EditText detail = (EditText) this.findViewById(R.id.editText2);
		detail.setOnEditorActionListener(new OnEditorActionListener(){

			@Override
			public boolean onEditorAction(TextView arg0, int arg1, KeyEvent arg2) {
				ImageView pic = (ImageView) EditMemeImage.this.findViewById(R.id.imageView1);
				EditMemeImage.this.updateText(pic);
				return false;
			}
			
		});
	}
	
	protected void resizeText() {
		switch(this.getCurrentTextSize()){
			case EditMemeImage.LARGE_TEXT_SIZE:
				this.setTextSize(EditMemeImage.SMALL_FONT_SIZE);
				this.setCurrentTextSize(EditMemeImage.SMALL_TEXT_SIZE);
				break;
			case EditMemeImage.MEDIUM_TEXT_SIZE:
				this.setTextSize(EditMemeImage.LARGE_FONT_SIZE);
				this.setCurrentTextSize(EditMemeImage.LARGE_TEXT_SIZE);
				break;
			case EditMemeImage.SMALL_TEXT_SIZE:
				this.setTextSize(EditMemeImage.MEDIUM_FONT_SIZE);
				this.setCurrentTextSize(EditMemeImage.MEDIUM_TEXT_SIZE);
				break;
		}
		this.updateText(this.findViewById(R.id.imageView1));
	}

	private void clearCanvas(){
		mImageToEdit = null;
		mImageToEdit = FileUtilsHelper.loadCompressedImageFromDiskAtPath(mBaseImage);
		if(mImageToEdit.getWidth() < getImageResolutionSetting().get("width")){
			Log.d(TAG,"Scaling Bitmap Up to " + getImageResolutionSetting().get("width").toString() + " x " + getImageResolutionSetting().get("height").toString());
			mImageToEdit = FileUtilsHelper.scaleBitmap(this.mBaseImage, getImageResolutionSetting().get("width"), getImageResolutionSetting().get("height"));
			Log.d(TAG,"Resulting Image Width " + mImageToEdit.getWidth());
		}
		ImageView pic = (ImageView) this.findViewById(R.id.imageView1);
		pic.setImageBitmap(mImageToEdit);
		
	}
	
	/**
	 * @return the mImageToEdit
	 */
	public Bitmap getImageToEdit() {
		return mImageToEdit;
	}

	/**
	 * @return the mTextSize
	 */
	public int getTextSize() {
		return mTextSize;
	}

	/**
	 * @param mTextSize the mTextSize to set
	 */
	public void setTextSize(int mTextSize) {
		this.mTextSize = mTextSize;
	}

	/**
	 * @return the mCurrentTextSize
	 */
	public int getCurrentTextSize() {
		return mCurrentTextSize;
	}

	/**
	 * @param mCurrentTextSize the mCurrentTextSize to set
	 */
	public void setCurrentTextSize(int mCurrentTextSize) {
		this.mCurrentTextSize = mCurrentTextSize;
	}

	/**
	 * @param mImageToEdit the imageToEdit to set
	 */
	public void setImageToEdit(Bitmap imageToEdit) {
		this.mImageToEdit = imageToEdit;
	}

	public void updateText(View v){
		clearCanvas();
		Log.d(EditMemeImage.TAG, "Creating Text With Size: " + this.getTextSize());
		Log.d("updateText","What is mIsVideo " + this.mIsVideo);
		Canvas canvas = new Canvas(mImageToEdit);
		EditText title = (EditText) this.findViewById(R.id.editText1);
		EditText detail = (EditText) this.findViewById(R.id.editText2);
		String titleContent = title.getText().toString();
		String detailContent = detail.getText().toString();
		Paint strokePaint = new Paint();
	    strokePaint.setARGB(255, 0, 0, 0);
	    strokePaint.setTextAlign(Align.CENTER);
	    strokePaint.setTextSize(this.getTextSize());
	    strokePaint.setTypeface(Typeface.DEFAULT_BOLD);
	    strokePaint.setStyle(Paint.Style.STROKE);
	    strokePaint.setStrokeWidth((float)this.getTextSize() / 18.6f);
		Paint textPaint2 = new Paint(Paint.ANTI_ALIAS_FLAG);
		textPaint2.setTextAlign(Align.CENTER);
	    textPaint2.setTextSize(this.getTextSize());
        textPaint2.setColor(Color.WHITE);
        textPaint2.setTypeface(Typeface.DEFAULT_BOLD);
        Paint strokePaint2 = new Paint();
	    strokePaint2.setARGB(255, 0, 0, 0);
	    strokePaint2.setTextAlign(Align.CENTER);
	    strokePaint2.setTextSize(this.getTextSize());
	    strokePaint2.setTypeface(Typeface.DEFAULT_BOLD);
	    strokePaint2.setStyle(Paint.Style.STROKE);
	    strokePaint2.setStrokeWidth((float)this.getTextSize() / 18.6f);
		Paint textPaint3 = new Paint(Paint.ANTI_ALIAS_FLAG);
		textPaint3.setTextAlign(Align.CENTER);
	    textPaint3.setTextSize(this.getTextSize());
        textPaint3.setColor(Color.WHITE);
        textPaint3.setTypeface(Typeface.DEFAULT_BOLD);
    	splitAndDrawLines(canvas,
        		titleContent, 
        		(mImageToEdit.getWidth() / 2),
        		Math.round((float)this.getTextSize() * 1.06f),
        		textPaint2,
        		strokePaint,
        		mImageToEdit.getWidth());
        splitAndDrawLines(canvas,
        		detailContent, 
        		(mImageToEdit.getWidth() / 2),
        		Math.round((float)mImageToEdit.getHeight() - (this.getTextSize() * 1.05f)),
        		textPaint3,
        		strokePaint2,
        		mImageToEdit.getWidth());
	}
	
	public void splitAndDrawLines(Canvas canvas,String text, int x, int y, Paint textPaint, Paint textStroke, int width){
	    ArrayList<String> lines = new ArrayList<String>();
	    Log.d("EditMemeImage::splitAndDrawLines()","Input Text - " + text);
	    String test = String.copyValueOf(text.toCharArray());
	    while(test.isEmpty() == false){
	        int newLength = textPaint.breakText(test, true, canvas.getWidth(), null);
	        String broken = test.substring(0, newLength);
	        if(broken.contains(" ") && test.length() > broken.length()){
	        	Log.d(TAG,"Moving back to newline");
	        	int index = broken.lastIndexOf(" ");
	        	if(index > 0){
	        		broken = test.substring(0, index);
	        		newLength = index;
	        	}
	        }
	        Log.d("EditMemeImage::splitAndDrawLines()","Broken - " + broken);
	        lines.add(broken);
	        test = test.substring(newLength);
	    }
	    Rect bounds = new Rect();
	    int yoff = 0;
	    for(String line:lines){
	        canvas.drawText(line, x,y + yoff, textPaint);
	        canvas.drawText(line, x,y + yoff, textStroke);
	        textPaint.getTextBounds(line, 0, line.length(), bounds);
	        yoff += bounds.height() + 10;
	    }
	}
	
	public Bitmap updateTextWithBitmap(Bitmap b){
		Canvas canvas = new Canvas(b);
		EditText title = (EditText) this.findViewById(R.id.editText1);
		EditText detail = (EditText) this.findViewById(R.id.editText2);
		String titleContent = title.getText().toString();
		String detailContent = detail.getText().toString();
		Paint strokePaint = new Paint();
	    strokePaint.setARGB(255, 0, 0, 0);
	    strokePaint.setTextAlign(Align.CENTER);
	    strokePaint.setTextSize(56);
	    strokePaint.setTypeface(Typeface.DEFAULT_BOLD);
	    strokePaint.setStyle(Paint.Style.STROKE);
	    strokePaint.setStrokeWidth(4);
		Paint textPaint2 = new Paint(Paint.ANTI_ALIAS_FLAG);
		textPaint2.setTextAlign(Align.CENTER);
	    textPaint2.setTextSize(56);
	    textPaint2.setColor(Color.WHITE);
	    textPaint2.setTypeface(Typeface.DEFAULT_BOLD);
	    Paint strokePaint2 = new Paint();
	    strokePaint2.setARGB(255, 0, 0, 0);
	    strokePaint2.setTextAlign(Align.CENTER);
	    strokePaint2.setTextSize(56);
	    strokePaint2.setTypeface(Typeface.DEFAULT_BOLD);
	    strokePaint2.setStyle(Paint.Style.STROKE);
	    strokePaint2.setStrokeWidth(4);
		Paint textPaint3 = new Paint(Paint.ANTI_ALIAS_FLAG);
		textPaint3.setTextAlign(Align.CENTER);
	    textPaint3.setTextSize(56);
	    textPaint3.setColor(Color.WHITE);
	    textPaint3.setTypeface(Typeface.DEFAULT_BOLD);
	    splitAndDrawLines(canvas,
	    		titleContent, 
	    		(b.getWidth() / 2),
	    		59,
	    		textPaint2,
	    		strokePaint,
	    		b.getWidth());
	    splitAndDrawLines(canvas,
	    		detailContent, 
	    		(b.getWidth() / 2),
	    		(b.getHeight() - 63),
	    		textPaint3,
	    		strokePaint2,
	    		b.getWidth());
	    return b;
	}
	
	public void publishImageToIntent(View v){
		if(this.mIsVideo == false){
			publishMemeImage(v);
		}else{
			new ApplyTextToFramesAsyncTask().execute(1);
		}
	}
	
	private void publishMemeImage(View v){
		updateText(v);
		//ImageView pic = (ImageView) this.findViewById(R.id.imageView1);
		Bitmap b = this.mImageToEdit;
		String imagePath = FileUtilsHelper.saveBitmapAsJpeg(b, this);
		FileUtilsHelper.copyMemeGifIntoGallery(imagePath);
		FileUtilsHelper.runMediaScanner(this, imagePath);
		Log.d("EditMemeImage::publishImageToIntent()","Image Path - " + imagePath);
		Intent shareCaptionIntent = new Intent(Intent.ACTION_SEND);
		String captionString = "Created with Maximum Ultimate Meme Creator X Turbo!";
	    shareCaptionIntent.setType("image/jpeg");

	    //set photo
	    Uri examplePhoto = Uri.fromFile(new File(imagePath));
	    Intent i = getIntent();
	    i.setData(examplePhoto);
	    i.setType("image/jpeg");
	    shareCaptionIntent.putExtra(Intent.EXTRA_STREAM, examplePhoto);

	    //set caption
	    shareCaptionIntent.putExtra(Intent.EXTRA_TEXT, captionString);
	    shareCaptionIntent.putExtra(Intent.EXTRA_SUBJECT, captionString);
	    shareCaptionIntent.putExtra(Intent.EXTRA_TITLE, captionString);
	    startActivity(Intent.createChooser(shareCaptionIntent,"Share Meme"));
	}
	
	public void updateProgressIndicator(float p) {
		ProgressBar prog = (ProgressBar)findViewById(R.id.progressBar1);
		prog.setMax(100);
		prog.setProgress((int)Math.ceil(p));
	}
	

	public void doneSavingClipImages() {
		Log.d("EditMemeImage::doneSavingClipImages()","Finished saving clip images, transitioning to editor");
		Intent i = new Intent(this, VideoFrameEditActivity.class);
		String title = ((EditText)findViewById(R.id.editText1)).getText().toString();
		String content = ((EditText)findViewById(R.id.editText2)).getText().toString();
		i.putExtra("com.samsung.smcl.maximumultimatememecreateorxturbo.title", title);
		i.putExtra("com.samsung.smcl.maximumultimatememecreatorxturbo.description", content);
		i.setFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT);
		startActivity(i);
	}
	
	/**
	 * ------------------------------------------------------------------------
	 * Apply text to all frames after this one
	 * ------------------------------------------------------------------------
	 */
	
	private class ApplyTextToFramesAsyncTask extends AsyncTask <Integer, Integer, Long> {

		private Bitmap mThisBitmap;
		private ProgressDialog mProgressDialog;
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			mProgressDialog = new ProgressDialog(EditMemeImage.this);
			mProgressDialog.setIndeterminate(false);
			mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			mProgressDialog.setTitle(R.string.process_text_progress_dialog_description);
			mProgressDialog.setCanceledOnTouchOutside(false);
			mProgressDialog.setOnDismissListener(new OnDismissListener(){

				@Override
				public void onDismiss(DialogInterface arg0) {
					ApplyTextToFramesAsyncTask.this.cancel(true);
				}
				
			});
			mProgressDialog.setProgress(0);
			mProgressDialog.show();
			getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		}
		protected Long doInBackground(Integer... i){
			File f = new File(mBaseImage);
			String thisFileName = f.getName();
			Bitmap mux = updateTextWithBitmap(FileUtilsHelper.loadCompressedImageFromDiskAtPath(f.getAbsolutePath()));
			mThisBitmap = mux;
			String fnx = f.getName();
			String[] fileNumberPartsX = fnx.split("-");
			String numx = fileNumberPartsX[1];
			String pathx = FileUtilsHelper.getFramesFolderForMotionMeme(EditMemeImage.this) + "/frame-" + numx + "-fin.png";
			FileUtilsHelper.saveBitmapAsHighQualityPngToLocation(mux, pathx);
			ArrayList<File> filesThatComeAfterThisOne = FileUtilsHelper.getImageFramesAfterFrameFileNamed(thisFileName, EditMemeImage.this);
			Log.d("EditMemeImage::ApplyTextToFramesAsyncTask::doInBackground()", "Files get response " + filesThatComeAfterThisOne.size());
			int progress = 0;
			for(File fil : filesThatComeAfterThisOne){
				if(this.isCancelled()){
					return (long)2;
				}
				Bitmap b = FileUtilsHelper.loadCompressedImageFromDiskAtPath(fil.getAbsolutePath());
				updateTextWithBitmap(b);
				String fn = fil.getName();
				String[] fileNumberParts = fn.split("-");
				String num = fileNumberParts[1];
				String path = FileUtilsHelper.getFramesFolderForMotionMeme(EditMemeImage.this) + "/frame-" + num + "-fin.png";
				FileUtilsHelper.saveBitmapAsHighQualityPngToLocation(b, path);
				progress = progress + 1;
				publishProgress((int) ((progress / (float) filesThatComeAfterThisOne.size()) * 100));
			}
			return (long)2;
		}
		protected void onProgressUpdate(Integer... progress){
			float p = progress[0];
			Log.d("EditMemeImage::ApplyTextToFramesAsyncTask::onProgressUpdate()", p + " done.");
			mProgressDialog.setProgress((int) Math.ceil((double)p));
			ImageView imv = (ImageView)EditMemeImage.this.findViewById(R.id.imageView1);
			imv.setImageBitmap(mThisBitmap);
			EditMemeImage.this.updateProgressIndicator(p);
		}
		protected void onCancelled(Long result){
			publishProgress(0);
			mProgressDialog.setProgress(0);
			mProgressDialog.dismiss();
			getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
			FileUtilsHelper.removeAllImageFilesInFolder(EditMemeImage.this);
		}
		protected void onPostExecute(Long result){
			publishProgress(100);
			mProgressDialog.setProgress(100);
			mProgressDialog.dismiss();
			getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
			EditMemeImage.this.doneSavingClipImages();
		}
	}
	
	
}
