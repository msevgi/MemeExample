package com.msevgi.memeex.util;

import android.graphics.Bitmap;
import android.util.Log;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationSet;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;

import com.googlecode.javacv.FrameGrabber.Exception;
import com.msevgi.memeex.ChooseTenSecondClip;

import java.util.ArrayList;

public class AnimationsHelper {
	
	protected static final String TAG = AnimationsHelper.class.getSimpleName();
	private static boolean sAbortAnimations;

	public static void abortAnimations(){
		AnimationsHelper.setAbortAnimations(true);
	}
	
	public static void restartAnimations(){
		AnimationsHelper.setAbortAnimations(false);
	}

    public static void switchImageAnimations(final ChooseTenSecondClip activity, final ImageView imageView, final ArrayList<Double> doubles, final int imageIndex, final boolean forever) {

          //imageView <-- The View which displays the images
          //images[] <-- Holds R references to the images to display
          //imageIndex <-- index of the first image to show in images[] 
          //forever <-- If equals true then after the last image it starts all over again with the first image resulting in an infinite loop. You have been warned.

            int fadeInDuration = 500; // Configure time values here
            int timeBetween = 1000;
            int fadeOutDuration = 1000;

            imageView.setVisibility(View.INVISIBLE);    //Visible or invisible by default - this will apply when the animation ends
            Bitmap bmp = null;
			try {
				bmp = activity.getFrameForTime(doubles.get(imageIndex));
			} catch (Exception e) {
				Log.e(TAG, "Couldn't load frame");
				e.printStackTrace();
			}
            imageView.setImageBitmap(bmp);

            Animation fadeIn = new AlphaAnimation(0, 1);
            fadeIn.setInterpolator(new DecelerateInterpolator()); // add this
            fadeIn.setDuration(fadeInDuration);

            Animation fadeOut = new AlphaAnimation(1, 0);
            fadeOut.setInterpolator(new AccelerateInterpolator()); // and this
            fadeOut.setStartOffset(fadeInDuration + timeBetween);
            fadeOut.setDuration(fadeOutDuration);

            AnimationSet animation = new AnimationSet(false); // change to false
            animation.addAnimation(fadeIn);
            animation.addAnimation(fadeOut);
            animation.setRepeatCount(1);
            imageView.setAnimation(animation);

            animation.setAnimationListener(new AnimationListener() {
                public void onAnimationEnd(Animation animation) {
                	if(sAbortAnimations == true){
                		Log.i(TAG, "Aborting animations");
                		return;
                	}
                    if (doubles.size() - 1 > imageIndex) {
                        switchImageAnimations(activity, imageView, doubles, imageIndex + 1,forever); //Calls itself until it gets to the end of the array
                    }
                    else {
                        if (forever == true){
                            switchImageAnimations(activity, imageView, doubles, 0,forever);  //Calls itself to start the animation all over again in a loop if forever = true
                        }
                    }
                }
                public void onAnimationRepeat(Animation animation) {
                    // TODO Auto-generated method stub
                }
                public void onAnimationStart(Animation animation) {
                    // TODO Auto-generated method stub
                }
            });
        }

	/**
	 * @return the sAbortAnimations
	 */
	public static boolean isAbortAnimations() {
		return sAbortAnimations;
	}

	/**
	 * @param sAbortAnimations the sAbortAnimations to set
	 */
	public static void setAbortAnimations(boolean sAbortAnimations) {
		AnimationsHelper.sAbortAnimations = sAbortAnimations;
	}


}
