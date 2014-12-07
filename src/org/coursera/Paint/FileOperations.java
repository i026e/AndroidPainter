package org.coursera.Paint;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Copyright (c) 2014 Lawrence Angrave
 */
public class FileOperations {
    private static final String TAG = FileOperations.class.getSimpleName();
    private static final float ROTATE_ANGLE = 90;


    public static PairWH<Integer> getScreenSize(Context context){
        int displayW = context.getResources().getDisplayMetrics().widthPixels;
        int displayH = context.getResources().getDisplayMetrics().heightPixels - getStatusBarHeight(context);
        return new PairWH<Integer>(displayW, displayH);
    }

    //return height of status bar. It is where battery charge is shown
    public static int getStatusBarHeight(Context context) {
        int result = 0;
        int resourceId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = context.getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }


    public static void saveImage(Context context, Bitmap bitmap){
        if (bitmap == null) {
            return;
        }
        File path = Environment
                .getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        Log.d(TAG, "saveAndShare path = " + path);
        path.mkdirs();

        // Note, for display purposes
        // SimpleDateFormat.getTimeInstance()
        // getDateTimeInstance() or getDateIntance
        // are more appropriate.
        // For filenames we can use the following specification
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss")
                .format(new Date());

        String filename = "Image_" + timestamp + ".jpg";
        // Alternatively ... use System.currentTimeMillis()

        // Creating a new File object in Java does not create a new
        // file on the device. The file object just represents
        // a location or path that may or may not exist
        File file = new File(path, filename);
        FileOutputStream stream;
        try {
            // This can fail if the external storage is mounted via USB
            stream = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
            Toast.makeText(context, context.getString(R.string.ok_save_message) + filename, Toast.LENGTH_LONG).show();
            stream.close();

            Uri uri = Uri.fromFile(file);

            // Tell Android that a new public picture exists
            Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            intent.setData(uri);
            context.sendBroadcast(intent);


        } catch (Exception e) {
            Log.e(TAG, "saveAndShare (compressing):", e);
            Toast.makeText(context, context.getString(R.string.problem_save_message) + filename, Toast.LENGTH_LONG).show();
            return; // Do not continue
        }

    }
    public static Bitmap loadImage(Context context, Intent data){
        Uri uri = data.getData();
        return loadImage(context,uri);


    }

    public static Bitmap RotateBitmap(Bitmap source, float angle)
    {
        Log.d(TAG, "Rotation");
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }


    public static Bitmap loadImage(Context context, Uri uri){
        Log.d(TAG, uri.toString());
        Toast.makeText(context, uri.toString(),
                Toast.LENGTH_SHORT).show();

        try {
            InputStream stream = context.getContentResolver().openInputStream(uri);

            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;

            BitmapFactory.decodeStream(stream, null, options);
            stream.close();

            int w = options.outWidth;
            int h = options.outHeight;
            Log.d(TAG, "Bitmap raw size:" + w + " x " + h);

            PairWH<Integer> displaySize = getScreenSize(context);


            // check if need to rotate screen
            if ((h > w && displaySize.h < displaySize.w) || (h < w && displaySize.h > displaySize.w)) {
                // rotate screen
                changeScreenOrientation(context);
                //switch height and width
                displaySize = new PairWH<Integer>(displaySize.h, displaySize.w);
            }


            // the decoder uses a final value based on powers of 2,
            // any other value will be rounded down to the nearest power of 2.
            int sample = Math.max(1, Math.max(w / displaySize.w, h / displaySize.h));

            //sample = 1;
            //while (w > displayW * sample || h > displayH * sample) {
                //sample = sample * 2;
            //}
            Log.d(TAG, "Sampling at " + sample);

            options.inJustDecodeBounds = false;
            options.inSampleSize = sample;

            stream = context.getContentResolver().openInputStream(uri);
            Bitmap bm = BitmapFactory.decodeStream(stream, null, options);
            stream.close();


            return bm;

        } catch (Exception e) {
            Log.e(TAG, "Decoding Bitmap", e);
            Toast.makeText(context, context.getString(R.string.problem_load_message) +uri.toString(),
                    Toast.LENGTH_LONG).show();
            return null;
        }


    }

    public static void changeScreenOrientation(Context context){
        Activity activity = (Activity) context;
        int orientation = context.getResources().getConfiguration().orientation;
        switch(orientation) {
            case Configuration.ORIENTATION_PORTRAIT:
                activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                break;
            case Configuration.ORIENTATION_LANDSCAPE:
                activity.setRequestedOrientation (ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                break;
        }
    }

    public static void forceScreenOrientation(Context context){
        Activity activity = (Activity) context;
        int orientation = context.getResources().getConfiguration().orientation;
        switch(orientation) {
            case Configuration.ORIENTATION_PORTRAIT:
                activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                break;
            case Configuration.ORIENTATION_LANDSCAPE:
                activity.setRequestedOrientation (ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                break;
        }
    }
}
