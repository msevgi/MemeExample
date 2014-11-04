
package com.msevgi.memeex;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import java.io.IOException;

public class MyActivity extends Activity implements View.OnClickListener {
   private Button    mSelectButton;
   private Button    mFinishButton;
   private EditText  mEditText;
   private ImageView mImageView;
   Bitmap            bm;

   @Override
   protected void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.activity_my);
      mSelectButton = (Button) findViewById(R.id.selectButton);
      mFinishButton = (Button) findViewById(R.id.finishButton);
      mFinishButton.setOnClickListener(this);
      mEditText = (EditText) findViewById(R.id.editText);
      mImageView = (ImageView) findViewById(R.id.imageView);
      mSelectButton.setOnClickListener(this);
   }

   @Override
   public boolean onCreateOptionsMenu(Menu menu) {
      // Inflate the menu; this adds items to the action bar if it is present.
      getMenuInflater().inflate(R.menu.my, menu);
      return true;
   }

   @Override
   public boolean onOptionsItemSelected(MenuItem item) {
      // Handle action bar item clicks here. The action bar will
      // automatically handle clicks on the Home/Up button, so long
      // as you specify a parent activity in AndroidManifest.xml.
      int id = item.getItemId();

      // noinspection SimplifiableIfStatement
      if (id == R.id.action_settings) {
         return true;
      }

      return super.onOptionsItemSelected(item);
   }

   @Override
   public void onClick(View v) {
      switch (v.getId()) {
         case R.id.selectButton :
            Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(intent, 1);
            break;
         case R.id.finishButton :
            mEditText.setCursorVisible(false);
            mEditText.buildDrawingCache();
            Bitmap bmp = Bitmap.createBitmap(mEditText.getDrawingCache());
            combineImages(bm, bmp);
            break;
         default :
            break;
      }

   }

   @Override
   protected void onActivityResult(int requestCode, int resultCode, Intent data) {
      super.onActivityResult(requestCode, resultCode, data);
      if (resultCode == RESULT_OK) {
         if (requestCode == 1) {
            Uri selectedImageUri = data.getData();

            try {
               bm = MediaStore.Images.Media.getBitmap(getApplicationContext().getContentResolver(), selectedImageUri);
            }
            catch (IOException e) {
               e.printStackTrace();
            }
            String selectedImagePath = getPath(selectedImageUri);
            System.out.println("Image Path : " + selectedImagePath);
            mImageView.setImageURI(selectedImageUri);
         }
      }
   }

   public String getPath(Uri uri) {
      String[] projection = {MediaStore.Images.Media.DATA};
      Cursor cursor = managedQuery(uri, projection, null, null, null);
      int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
      cursor.moveToFirst();
      return cursor.getString(column_index);
   }

   public Bitmap combineImages(Bitmap background, Bitmap foreground) {

      int width = 0, height = 0;
      Bitmap cs;

      width = getWindowManager().getDefaultDisplay().getWidth();
      height = getWindowManager().getDefaultDisplay().getHeight();

      cs = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
      Canvas comboImage = new Canvas(cs);
      // background = Bitmap.createScaledBitmap(background, width, height, true);
      comboImage.drawBitmap(background, 0, 0, null);
      comboImage.drawBitmap(foreground, background.getWidth(), 0f, null);
      mImageView.setImageBitmap(cs);
      return cs;
   }
}
