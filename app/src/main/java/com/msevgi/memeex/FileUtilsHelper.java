package com.msevgi.memeex;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.Locale;

import us.sigsegv.android.maximumultimatememecreatorxturbo.util.ByteBufferBackedInputStream;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

public class FileUtilsHelper {
	
	private static final String TAG = FileUtilsHelper.class.getSimpleName();
	public static int MEDIA_TYPE_IMAGE = 1;
	public static int MEDIA_TYPE_VIDEO = 2;
	
	private static File getOutputMediaFile(int type){

        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.

        //      Environment.getExternalStorageState();

        //      File mediaStorageDir = new File(Environment.getExternalStorageDirectory(), Constants.ALBUM.AlbumInPhone);
        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "meme_content");
        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.

        // Create the storage directory if it does not exist
        if (! mediaStorageDir.exists()){
            if (! mediaStorageDir.mkdirs()){
                Log.d("FileUtilsHelper", "failed to create directory");
                return null;
            }
        }

        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        File mediaFile;
        if (type == MEDIA_TYPE_IMAGE){
            mediaFile = new File(mediaStorageDir.getPath() + File.separator + "IMG_"+ timeStamp + ".jpg");
        } else if(type == MEDIA_TYPE_VIDEO) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator + "VID_"+ timeStamp + ".mp4");
        } else {
            return null;
        }

        return mediaFile;
    }
	
	public static File copyMemeGifIntoGallery(String source){
		File f = new File(source);
		File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "meme_content");
        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.

        // Create the storage directory if it does not exist
        if (! mediaStorageDir.exists()){
            if (! mediaStorageDir.mkdirs()){
                Log.d("FileUtilsHelper", "failed to create directory");
                return null;
            }
        }
        File destFile = null;
        if(f.exists()){
        	destFile = new File(mediaStorageDir.getAbsolutePath() + "/" + f.getName());
        	FileInputStream fis = null;
    		FileOutputStream fos = null;

    		try {
    			
    			fis = new FileInputStream(f.getAbsolutePath());
    			fos = new FileOutputStream(destFile);
    			
    			byte[] buffer = new byte[1024];
    			int noOfBytes = 0;

    			Log.d("FileUtilsHelper","Copying file using streams");

    			// read bytes from source file and write to destination file
    			while ((noOfBytes = fis.read(buffer)) != -1) {
    				fos.write(buffer, 0, noOfBytes);
    			}

    		}
    		catch (FileNotFoundException e) {
    			Log.e("FileUtilsHelper","File not found" + e);
    		}
    		catch (IOException ioe) {
    			Log.e("FileUtilsHelper","Exception while copying file " + ioe);
    		}
    		finally {
    			// close the streams using close method
    			try {
    				if (fis != null) {
    					fis.close();
    				}
    				if (fos != null) {
    					fos.close();
    				}
    			}
    			catch (IOException ioe) {
    				Log.e("FileUtilsHelper","Error while closing stream: " + ioe);
    			}

    		}
        }
        return destFile;
	}
	
	// Creates a unique subdirectory of the designated app cache directory. Tries to use external
	// but if not mounted, falls back on internal storage.
	public static File getDiskCacheDir(Activity context, String uniqueName) {
	    // Check if media is mounted or storage is built-in, if so, try and use external cache dir
	    // otherwise use internal cache dir
	    final String cachePath =
	            Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState()) ||
	                    !Environment.isExternalStorageRemovable() ? context.getExternalCacheDir().getPath() :
	                            context.getCacheDir().getPath();

	    return new File(cachePath + File.separator + uniqueName);
	}
	
	public static String getApplicationFolder(Context appContext){
		File appFolder = appContext.getExternalFilesDir(null);
		return appFolder.getAbsolutePath();
	}
	
	public static String getFolderForTemporaryMeme(Context ctx){
		String fullMotionMemePath = FileUtilsHelper.getApplicationFolder(ctx) + "/temp-meme/";
		File dir = new File(fullMotionMemePath);
		boolean motionMemeFolderExists = dir.isDirectory();
		if(motionMemeFolderExists == false){
			boolean didCreate = dir.mkdirs();
			if(didCreate == false){
				Log.e("FileUtilsHelper:getFramesFolderForMotionMeme()","Could not create video-frames folder!");
			}
		}
		return dir.getAbsolutePath();
	}
	
	public static String getFramesFolderForMotionMeme(Context ctx){
		String fullMotionMemePath = FileUtilsHelper.getApplicationFolder(ctx) + "/video-frames/";
		File dir = new File(fullMotionMemePath);
		boolean motionMemeFolderExists = dir.isDirectory();
		if(motionMemeFolderExists == false){
			boolean didCreate = dir.mkdirs();
			if(didCreate == false){
				Log.e("FileUtilsHelper:getFramesFolderForMotionMeme()","Could not create video-frames folder!");
			}
		}
		return dir.getAbsolutePath();
	}
	
	public static String saveByteBufferAsBitmap(ByteBuffer buf, String pathWithFilename){
		File mediaFile = new File(pathWithFilename);
        Log.d(TAG,"Bitmap file path - " + mediaFile.getAbsolutePath());
        BitmapFactory.Options bmpFactoryOptions = new BitmapFactory.Options();
        bmpFactoryOptions.inPurgeable = true;
        bmpFactoryOptions.inInputShareable = true;
        bmpFactoryOptions.inJustDecodeBounds = true;
		Bitmap bmp = BitmapFactory.decodeStream(new ByteBufferBackedInputStream(buf), null, bmpFactoryOptions);
		FileOutputStream out = null;
        try {
		       out = new FileOutputStream(mediaFile);
		       bmp.compress(Bitmap.CompressFormat.PNG, 60, out);
		       out.close();
		} catch (Exception e) {
			Log.e("FileUtilsHelper::saveBitmapAsHighQualityPngToLocation()","Exception saving to path - " + mediaFile.getAbsolutePath());
		       e.printStackTrace();
		}
		return mediaFile.getAbsolutePath();
	}
	
	/**
	 * Will save a bitmap as a high quality PNG to a given location
	 * @param bitmap The bitmap to be saved
	 * @param pathWithFilename The path in which to save the bitmap
	 * @return
	 */
	
	public static String saveBitmapAsHighQualityPngToLocation(Bitmap bitmap, String pathWithFilename){
        File mediaFile = new File(pathWithFilename);
        Log.d("FileUtilsHelper::saveBitmapAsHighQualityPngToLocation()","Bitmap file path - " + mediaFile.getAbsolutePath());
		FileOutputStream out = null;
        try {
		       out = new FileOutputStream(mediaFile);
		       bitmap.compress(Bitmap.CompressFormat.PNG, 60, out);
		       out.close();
		} catch (Exception e) {
			Log.e("FileUtilsHelper::saveBitmapAsHighQualityPngToLocation()","Exception saving to path - " + mediaFile.getAbsolutePath());
		       e.printStackTrace();
		}
		return mediaFile.getAbsolutePath();
	}
	
	public static String saveBitmapAsJpeg(Bitmap bitmap, Context context){
		File mediaStorageDir = new File(getFolderForTemporaryMeme(context));
		// Create the storage directory if it does not exist
        if (! mediaStorageDir.exists()){
            if (! mediaStorageDir.mkdirs()){
                Log.d("FileUtilsHelper", "failed to create directory");
                return null;
            }
        }
     // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        File mediaFile = new File(mediaStorageDir.getPath() + File.separator + "IMG_"+ timeStamp + ".jpg");
        Log.d("FileUtilsHelper::saveBitmapAsJpeg()","Bitmap file path - " + mediaFile.getAbsolutePath());
		try {
		       FileOutputStream out = new FileOutputStream(mediaFile);
		       bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
		       out.close();
		} catch (Exception e) {
		       e.printStackTrace();
		}
		return mediaFile.getAbsolutePath();
	}
	
	public static String getPreEditPathForEditedFile(File file, Context ctx){
		int num = getNumberFromFile(file);
		return getFramesFolderForMotionMeme(ctx) + "/video-" + num + "-.png";
	}
	
	public static ArrayList<File> getListOfIncludedBitmapsForAniGif(Context ctx){
		String basePath = FileUtilsHelper.getFramesFolderForMotionMeme(ctx);
		File f = new File(basePath);
		ArrayList<File> farr = new ArrayList<File>();
		if(f.isDirectory())
		{
			File[] files = f.listFiles();
			sortFilesByNumber(files);
			for(File fe : files){
				String fn = fe.getName();
				Log.d("FileUtilsHelper:getListOfIncludedBitmapsForAniGif()","File name: " + fn);
				if(fn.contains("-fin.png") == false){
					String[] fileNumberParts = fn.split("-");
					String num = fileNumberParts[1];
					// video + num + -.png
					String path = basePath + "/frame-" + num + "-fin.png";
					if(new File(path).exists()){
						farr.add(new File(path));
					}else{
						farr.add(fe);
					}
				}
			}
		}
		return farr;
	}
	
	public static String getPathForAniGifOutput(Context ctx){
		String fullMotionMemePath = FileUtilsHelper.getApplicationFolder(ctx) + "/animated-gif/";
		File dir = new File(fullMotionMemePath);
		boolean motionMemeFolderExists = dir.isDirectory();
		if(motionMemeFolderExists == false){
			boolean didCreate = dir.mkdirs();
			if(didCreate == false){
				Log.e("FileUtilsHelper:getFramesFolderForMotionMeme()","Could not create animated-gif folder!");
			}
		}
		return dir.getAbsolutePath();
	}

	public static Uri getOutputMediaFileUri(int type){
        return Uri.fromFile(getOutputMediaFile(type));
    }

	public static byte[] convertBitmapToByteArray(Bitmap bmp){
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		bmp.compress(Bitmap.CompressFormat.PNG, 60, stream);
		byte[] byteArray = stream.toByteArray();
		return byteArray;
	}
	
	public static Bitmap convertByteArrayToBitmap(byte[] byteArray){
		BitmapFactory.Options bmpFactoryOptions = new BitmapFactory.Options();
        bmpFactoryOptions.inPurgeable = true;
        bmpFactoryOptions.inInputShareable = true;
		Bitmap bmp = BitmapFactory.decodeByteArray(byteArray,0,byteArray.length, bmpFactoryOptions);
		Bitmap mutableBitmap = bmp.copy(Bitmap.Config.ARGB_8888, true);
		bmp.recycle();
		return mutableBitmap;
	}
	
	public static Bitmap loadCompressedImageFromDiskAtPath(String filePath){
		BitmapFactory.Options bmpFactoryOptions = new BitmapFactory.Options();
		bmpFactoryOptions.inPurgeable = true;
        bmpFactoryOptions.inInputShareable = true;
		Bitmap bmp = BitmapFactory.decodeFile(filePath, bmpFactoryOptions);
		Bitmap bmp2 = bmp.copy(Bitmap.Config.ARGB_8888, true);
		bmp.recycle();
		return bmp2;
	}
	
	public static Bitmap loadCompressedImageFromDiskAtPathForGif(String filePath){
		BitmapFactory.Options bmpFactoryOptions = new BitmapFactory.Options();
		bmpFactoryOptions.inPurgeable = true;
        bmpFactoryOptions.inInputShareable = true;
        bmpFactoryOptions.inDither = true;
		Bitmap bmp = BitmapFactory.decodeFile(filePath, bmpFactoryOptions);
		Bitmap bmp2 = bmp.copy(Bitmap.Config.RGB_565, true);
		bmp.recycle();
		return bmp2;
	}
	
	public static void removeAllImageFilesInFolder(Context ctx){
		String path = FileUtilsHelper.getFramesFolderForMotionMeme(ctx);
		File f = new File(path);
		if(f.isDirectory())
		{
			File[] files = f.listFiles();
			for(File fs : files){
				fs.delete();
			}
		}
	}
	
	public static int getNumberOfImagesFromClipFolder(Context ctx){
		String path = FileUtilsHelper.getFramesFolderForMotionMeme(ctx);
		File f = new File(path);
		int count = 0;
		if(f.isDirectory())
		{
			File[] files = f.listFiles();
			count = files.length;
		}
		return count;
	}
	
	public static Bitmap scaleBitmap(String file, int width, int height){
		  
		BitmapFactory.Options bmpFactoryOptions = new BitmapFactory.Options();
        bmpFactoryOptions.inJustDecodeBounds = true;
        bmpFactoryOptions.inPurgeable = true;
        bmpFactoryOptions.inInputShareable = true;
        Bitmap bitmap = BitmapFactory.decodeFile(file, bmpFactoryOptions);
        int origHeight = bmpFactoryOptions.outHeight;
        int origWidth = bmpFactoryOptions.outWidth;
        Log.d(TAG,"Original file dimensions " + origWidth + " x " + origHeight);
        float scaleWidth;
		int newWidth = width;
    	int newHeight = height;
		float scaleHeight;
		if(origWidth >= origHeight){
			Log.d(TAG, "Width Greator");
			scaleWidth = (float)newWidth/(float)origWidth;
        	scaleHeight = scaleWidth;
        }else{
        	Log.d(TAG, "Height Greator");
			scaleHeight = (float)newHeight/(float)origHeight;
			Log.d(TAG, "Scale Height " + scaleHeight);
        	scaleWidth = scaleHeight;
        	Log.d(TAG, "Scale Width " + scaleWidth);
        }
		Log.d(TAG, "Width " + (origWidth * scaleWidth));
		Log.d(TAG, "Height " + (origHeight * scaleHeight));
		bmpFactoryOptions.inJustDecodeBounds = false;
		Bitmap b = BitmapFactory.decodeFile(file, bmpFactoryOptions);
		bitmap = Bitmap.createScaledBitmap(b, (int)(origWidth * scaleWidth), (int)(origHeight * scaleHeight), false);
		b.recycle();
        return bitmap;
	}
	
	public static Bitmap shrinkBitmap(Bitmap bitmap, int width, int height){
	     Log.d(FileUtilsHelper.TAG, "Starting to decode byte array after options change");
	     Bitmap smallBitmap = Bitmap.createScaledBitmap(bitmap, width, height, false);
	     Log.d(FileUtilsHelper.TAG, "Finished decoding byte array after options change");
	     return smallBitmap;
	}
	
	public static Bitmap shrinkBitmap(byte[] bitmapArr, int width, int height){
		  
	     BitmapFactory.Options bmpFactoryOptions = new BitmapFactory.Options();
	        bmpFactoryOptions.inJustDecodeBounds = true;
	        bmpFactoryOptions.inPurgeable = true;
	        bmpFactoryOptions.inInputShareable = true;
	        Log.d(FileUtilsHelper.TAG, "Starting to decode byte array");
	        Bitmap bitmap = BitmapFactory.decodeByteArray(bitmapArr, 0, bitmapArr.length, bmpFactoryOptions);
	        Log.d(FileUtilsHelper.TAG, "Finished decoding byte array");
	        int heightRatio = (int)Math.ceil(bmpFactoryOptions.outHeight/(float)height);
	        int widthRatio = (int)Math.ceil(bmpFactoryOptions.outWidth/(float)width);
	        
	        if (heightRatio > 1 || widthRatio > 1)
	        {
	         if (heightRatio > widthRatio)
	         {
	          bmpFactoryOptions.inSampleSize = heightRatio;
	         } else {
	          bmpFactoryOptions.inSampleSize = widthRatio; 
	         }
	        }
	        
	        bmpFactoryOptions.inJustDecodeBounds = false;
	        Log.d(FileUtilsHelper.TAG, "Starting to decode byte array after options change");
	        bitmap = BitmapFactory.decodeByteArray(bitmapArr, 0, bitmapArr.length, bmpFactoryOptions);
	        Log.d(FileUtilsHelper.TAG, "Finished decoding byte array after options change");
	     return bitmap;
	    }
	
	public static Bitmap shrinkBitmap(String file, int width, int height){
		  
	     BitmapFactory.Options bmpFactoryOptions = new BitmapFactory.Options();
	        bmpFactoryOptions.inJustDecodeBounds = true;
	        bmpFactoryOptions.inPurgeable = true;
	        bmpFactoryOptions.inInputShareable = true;
	        Bitmap bitmap = BitmapFactory.decodeFile(file, bmpFactoryOptions);
	        
	        int heightRatio = (int)Math.ceil(bmpFactoryOptions.outHeight/(float)height);
	        int widthRatio = (int)Math.ceil(bmpFactoryOptions.outWidth/(float)width);
	        
	        if (heightRatio > 1 || widthRatio > 1)
	        {
	         if (heightRatio > widthRatio)
	         {
	          bmpFactoryOptions.inSampleSize = heightRatio;
	         } else {
	          bmpFactoryOptions.inSampleSize = widthRatio; 
	         }
	        }
	        
	        bmpFactoryOptions.inJustDecodeBounds = false;
	        bitmap = BitmapFactory.decodeFile(file, bmpFactoryOptions);
	     return bitmap;
	    }
	
	private static void sortFilesByNumber(File[] files){
		Arrays.sort(files, new Comparator<File>() {
	        @Override
	        public int compare(File s1, File s2) {
				int int1 = getNumberFromFile(s1);
				int int2 = getNumberFromFile(s2);
				// integers should always be positive in this case
				// overflow not a concern
	            return int1 - int2;
	        }
	    });
	}
	
	public static void runMediaScanner(Context ctx, String path){
		Intent scan = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
		scan.setData(Uri.fromFile(new File(path)));
		ctx.sendBroadcast(scan);
	}
	
	public static int getNumberFromFile(File file){
		String fn2 = file.getName();
    	String[] fileNumberParts2 = fn2.split("-");
		String num2 = fileNumberParts2[1];
		return Integer.parseInt(num2);
	}

	public static ArrayList<File> getImageFramesAfterFrameFileNamed(String thisFileName, Context ctx) {
		String path = FileUtilsHelper.getFramesFolderForMotionMeme(ctx);
		File f = new File(path);
		ArrayList<File> fileArrayList = new ArrayList<File>();
		if(f.isDirectory())
		{
			File[] files = f.listFiles();
			sortFilesByNumber(files);
			boolean hasPassedFile = false;
			for(File fl : files){
				if(fl.getName().equals(thisFileName)){
					hasPassedFile = true;
				}
				if(hasPassedFile){
					fileArrayList.add(fl);
				}
			}
		}
		return fileArrayList;
	}
	
	public static boolean compareBitmaps(Bitmap bitmap1, Bitmap bitmap2) {
	    ByteBuffer buffer1 = ByteBuffer.allocate(bitmap1.getHeight() * bitmap1.getRowBytes());
	    bitmap1.copyPixelsToBuffer(buffer1);

	    ByteBuffer buffer2 = ByteBuffer.allocate(bitmap2.getHeight() * bitmap2.getRowBytes());
	    bitmap2.copyPixelsToBuffer(buffer2);

	    return Arrays.equals(buffer1.array(), buffer2.array());
	}

	public static void deleteAllItemsForPath(String path, Context ctx) {
		String basePath = FileUtilsHelper.getFramesFolderForMotionMeme(ctx);
		File fe = new File(path);
		String fn = fe.getName();
		String[] fileNumberParts = fn.split("-");
		String num = fileNumberParts[1];
		// video + num + -.png
		String p2 = basePath + "/frame-" + num + "-fin.png";
		File deleteFin = new File(p2);
		deleteFin.delete();
		String p1 = basePath + "/video-" + num + "-.png";
		File deletePre = new File(p1);
		deletePre.delete();
	}
}
