package com.msevgi.memeex;

import android.app.Activity;
import android.content.ClipData;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.util.Log;
import android.view.DragEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.samsung.spensdk.multiwindow_applistener.SMultiWindowDropListener;

import java.util.HashMap;

public class AcquireContent extends Activity {

	static final int PICK_IMAGE_FROM_GALLERY = 1;
	static final int RECORD_VIDEO_FROM_CAMERA = 2;
	static final int PICK_VIDEO_FROM_GALLERY = 3;
	static final int TAKE_PICTURE_FROM_CAMERA = 4;
	private static final String TAG = AcquireContent.class.getSimpleName();
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_acquire_content);
        PreferenceManager.setDefaultValues(this, R.xml.pref_general, false);
        FileUtilsHelper.removeAllImageFilesInFolder(this);
    }

	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_acquire_content, menu);
        return true;
    }
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Log.d(TAG,"Menu Item id Value " + item.getItemId());
		switch (item.getItemId()) {
		case R.id.menu_settings:
			Intent i = new Intent(this, SettingsActivity.class);
			startActivity(i);
			break;
		}
		return super.onOptionsItemSelected(item);
	}
    
    private void getImageFromGallery(){
    	FileUtilsHelper.removeAllImageFilesInFolder(this);
    	Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
        photoPickerIntent.setType("image/*");
        startActivityForResult(photoPickerIntent, PICK_IMAGE_FROM_GALLERY);
    }
    
    private void getVideoFromGallery(){
    	FileUtilsHelper.removeAllImageFilesInFolder(this);
    	Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
        photoPickerIntent.setType("video/*");
        startActivityForResult(photoPickerIntent, PICK_VIDEO_FROM_GALLERY);
    }
    
    private void recordContentFromCamera(){
    	FileUtilsHelper.removeAllImageFilesInFolder(this);
    	Intent videoRecorderIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
    	startActivityForResult(videoRecorderIntent, RECORD_VIDEO_FROM_CAMERA);
    }
    
    private void getPictureFromCamera(){
    	FileUtilsHelper.removeAllImageFilesInFolder(this);
    	Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(takePictureIntent, TAKE_PICTURE_FROM_CAMERA);
    }
    
    public void acquirePhotoGalleryMemeContent(View v){
    	getImageFromGallery();
    }
    
    public void acquireTemplateGalleryContent(View v){
    	Intent i = new Intent(this, MemeTemplateGallery.class);
    	startActivity(i);
    }
    
    public void acquireVideoGalleryMemeContent(View v){
    	getVideoFromGallery();
    }
    
    public void acquirePhotoCameraMemeContent(View v){
    	getPictureFromCamera();
    }
    
    public void acquireVideoCameraMemeContent(View v){
    	recordContentFromCamera();
    }

	@Override
	protected void onPause() {
		View myView = this.findViewById(R.id.activityAcquireContentRootView);
		myView.setOnDragListener(null);
		super.onPause();
	}

	@Override
	protected void onResume() {
		View myView = this.findViewById(R.id.activityAcquireContentRootView);
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
			myView.setOnLongClickListener(onLongClickListener);*/
		SMultiWindowDropListener dl = new SMultiWindowDropListener(){

			@Override
			public void onDrop(DragEvent event) {
				ClipData clipData = event.getClipData();
				if(clipData != null){
					int count = clipData.getItemCount();
					for(int index = 0; index < count; ++index){
						ClipData.Item item = clipData.getItemAt(index);
						if(item.getUri() != null){
							Log.d(AcquireContent.TAG, "It's a URI");
							Uri uri = item.getUri();
							ContentResolver cR = AcquireContent.this.getContentResolver();
							String type = cR.getType(uri);
							Intent i = new Intent();
							i.setType(type);
							i.setData(uri);
							Log.d(AcquireContent.TAG, "Handling type: " + type);
							if(type.contains("video")){
								AcquireContent.this.onActivityResult(AcquireContent.PICK_VIDEO_FROM_GALLERY, RESULT_OK, i);
							}else if(type.contains("image")){
								AcquireContent.this.onActivityResult(AcquireContent.PICK_IMAGE_FROM_GALLERY, RESULT_OK, i);
							}else{
								Toast.makeText(AcquireContent.this, AcquireContent.this.getResources().getString(R.string.invalid_drop_content) , Toast.LENGTH_SHORT).show();
							}
						}
						else if(item.getIntent() != null){
							Intent i = item.getIntent();
							String type = i.getType();
							Log.d(AcquireContent.TAG, "Handling type: " + type);
							if(type.contains("video")){
								AcquireContent.this.onActivityResult(AcquireContent.PICK_VIDEO_FROM_GALLERY, RESULT_OK, i);
							}else if(type.contains("image")){
								AcquireContent.this.onActivityResult(AcquireContent.PICK_IMAGE_FROM_GALLERY, RESULT_OK, i);
							}else{
								Toast.makeText(AcquireContent.this, AcquireContent.this.getResources().getString(R.string.invalid_drop_content) , Toast.LENGTH_SHORT).show();
							}
						}
					}
				}
			}
			
		};
		myView.setOnDragListener(dl);
		super.onResume();
	}
	
	/**
	 * Will rotate a given bitmap to a given orientation
	 * @param b The bitmap to rotate
	 * @param orientation The orientation to which to rotate the bitmap
	 * @return The rotated bitmap
	 */
	
	private Bitmap rotateBitmapToOrientation(Bitmap b, int orientation){
		 Matrix matrix = new Matrix();
	     matrix.postRotate(orientation);
	     Canvas offscreenCanvas = new Canvas();
	     offscreenCanvas.drawBitmap(b, matrix, null);
	     return b;
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
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		switch (requestCode) {
			case PICK_IMAGE_FROM_GALLERY:
				{
					if (resultCode == RESULT_OK)
					{
							Log.d(TAG, "Got Picture!");
							Log.d(TAG,"File type - " + data.getType());
							Uri photoUri = data.getData();
					        if (photoUri != null)
					        {
						        try {
							        String[] filePathColumn = {MediaStore.Images.Media.DATA};
							        String[] orientationColumn = {MediaStore.Images.Media.ORIENTATION};
							        int orientation = -1;
							        Cursor cursor = getContentResolver().query(photoUri, filePathColumn, null, null, null); 
							        cursor.moveToFirst();
							        int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
							        String filePath = cursor.getString(columnIndex);
							        cursor.close();
							        cursor = getContentResolver().query(photoUri, orientationColumn, null, null, null);
							        if(cursor != null && cursor.moveToFirst()){
							        	orientation = cursor.getInt(cursor.getColumnIndex(orientationColumn[0]));
							        }
							        cursor.close();
							        //Bitmap bitmap = BitmapFactory.decodeFile(filePath);
							        HashMap<String, Integer> pRes = this.getImageResolutionSetting();
							        Bitmap shrunkenBitmap = FileUtilsHelper.shrinkBitmap(filePath, pRes.get("width"), pRes.get("height"));
							        shrunkenBitmap = rotateBitmapToOrientation(shrunkenBitmap, orientation);
							        String res = FileUtilsHelper.saveBitmapAsJpeg(shrunkenBitmap, this);
							        Log.d(TAG,"File Path: " + res);
							        shrunkenBitmap.recycle();
							        Intent editImage = new Intent(this, EditMemeImage.class);
							        editImage.addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT);
									editImage.putExtra("com.samsung.smcl.maximumultimatememecreatorxturbo.path", res);
									editImage.putExtra("com.samsung.smcl.maximumultimatememecreatorxturbo.video",false);
									startActivity(editImage);
						        }catch(Exception e){
						        	Toast.makeText(this, R.string.cant_save_image,Toast.LENGTH_SHORT).show();
						        	
							    }
						        
					        }
							
					}
				}
				break;
			case TAKE_PICTURE_FROM_CAMERA:
			{
				if (resultCode == RESULT_OK)
				{
						Log.d("AquireContent", "Got Picture!");
						Log.d("AcquireContent::onActivityResult","File type - " + data.getType());
						Uri photoUri = data.getData();
				        if (photoUri != null)
				        {
					        try {
						        String[] filePathColumn = {MediaStore.Images.Media.DATA};
						        String[] orientationColumn = {MediaStore.Images.Media.ORIENTATION};
						        int orientation = -1;
						        Cursor cursor = getContentResolver().query(photoUri, filePathColumn, null, null, null); 
						        cursor.moveToFirst();
						        int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
						        String filePath = cursor.getString(columnIndex);
						        cursor.close();
						        cursor = getContentResolver().query(photoUri, orientationColumn, null, null, null);
						        if(cursor != null && cursor.moveToFirst()){
						        	orientation = cursor.getInt(cursor.getColumnIndex(orientationColumn[0]));
						        }
						        cursor.close();
						        HashMap<String, Integer> pRes = this.getImageResolutionSetting();
						        Bitmap shrunkenBitmap = FileUtilsHelper.shrinkBitmap(filePath, pRes.get("width"), pRes.get("height"));
						        shrunkenBitmap = rotateBitmapToOrientation(shrunkenBitmap, orientation);
						        String res = FileUtilsHelper.saveBitmapAsJpeg(shrunkenBitmap, this);
						        Log.d("AcquireContent::onActivityResult-TakePicture","File Path: " + res);
						        shrunkenBitmap.recycle();
						        Intent editImage = new Intent(this, EditMemeImage.class);
						        editImage.addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT);
								editImage.putExtra("com.samsung.smcl.maximumultimatememecreatorxturbo.path", res);
								editImage.putExtra("com.samsung.smcl.maximumultimatememecreatorxturbo.video",false);
								startActivity(editImage);
					        }catch(Exception e){
					        	Toast.makeText(this, R.string.cant_save_image,Toast.LENGTH_SHORT).show();
						    }
					        
				        }
						
				}
			}
			break;
			case RECORD_VIDEO_FROM_CAMERA:
			{
				if(resultCode == RESULT_OK){
					if(data != null){
						Intent i = new Intent(this,ChooseTenSecondClip.class);
						Uri videoUri = data.getData();
						Log.d("AcquireContent::onActivityResult()","Video Intent: " + videoUri);
						i.setData(videoUri);
						i.addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT);
						startActivity(i);
					}else{
						Log.e("AcquireContent::onActivityResult()-RecordVideo","Oh No! Recording returned no intent");
					}
				}
			}
			break;
			case PICK_VIDEO_FROM_GALLERY:
			{
				if(resultCode == RESULT_OK){
					if(data != null){
						Intent i = new Intent(this,ChooseTenSecondClip.class);
						Uri videoUri = data.getData();
						Log.d("AcquireContent::onActivityResult()","Video Intent: " + videoUri);
						i.setData(videoUri);
						i.addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT);
						startActivity(i);
					}else{
						Log.e("AcquireContent::onActivityResult()-RecordVideo","Oh No! Choosing Video returned no intent");
					}
				}
			}
			break;
		}
	}
}
