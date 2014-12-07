package org.coursera.Paint;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Bundle;
import android.util.Log;
import android.view.*;
import android.widget.ImageView;
import android.widget.SeekBar;
import yuku.ambilwarna.AmbilWarnaDialog;


public class Painter extends Activity implements View.OnTouchListener {
    private static final String TAG = Painter.class.getSimpleName();
    private static final String KEY_PAINT_COLOR = "p_color";
    private static final String KEY_PAINT_SIZE = "p_size";
    private static final String KEY_BACK_COLOR = "b_color";

    private static final int DEF_BACK_COLOR = 0xffffffff; // White
    private static final int DEF_PAINT_COLOR = 0xff000099; // Blue
    private static final int DEF_PAINT_SIZE = 10;
    private static final int MAX_PAINT_SIZE = 64;
    private static final int REQUEST_CODE = 12345;
    private SharedPreferences mPrefs;

    ;
    private Bitmap mBitmap;
    private Canvas mCanvas;
    private ImageView mImageView;
    private float mScaleX, mScaleY;
    private Paint mPaint;
    private int mBackColor;
    // These variables will store previous touch point coordinates
    private float mX, mY;

    // this enum will store objects that color possible to change
    private static enum coloredObj {
        BACKGROUND, BRUSH
    }

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main);
        mImageView = (ImageView) findViewById(R.id.imageView);

        //force screen orientation
        FileOperations.forceScreenOrientation(this);

        mPaint = new Paint();
        mPaint.setStyle(Paint.Style.FILL);

        // From top left to bottom right
        mPrefs = getPreferences(MODE_PRIVATE);
        loadPreferences();

        mImageView.setOnTouchListener(this);
    }

    //These two methods change background
    private void setBackground(int color) {
        // Get size of the screen
        PairWH<Integer> displaySize = FileOperations.getScreenSize(this);

        Log.d(TAG, "Height:" + displaySize.h + ", width:" + displaySize.w);

        // create bitmap & canvas
        mBitmap = Bitmap.createBitmap(displaySize.w, displaySize.h, Bitmap.Config.ARGB_8888);
        mCanvas = new Canvas(mBitmap);
        mBackColor = color;

        mCanvas.drawColor(mBackColor);
        // Update image
        mImageView.setImageBitmap(mBitmap);

    }

    private void setBackground(Bitmap bitmap) {
        mBitmap.recycle();
        mCanvas = null;

        mBitmap = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(),
                Bitmap.Config.ARGB_8888);
        mCanvas = new Canvas(mBitmap);
        mCanvas.drawBitmap(bitmap, 0, 0, null);

        bitmap.recycle();
        // Update image
        mImageView.setImageBitmap(mBitmap);


    }

    private void setScale() {
        Log.d(TAG, "ImageView Width:" + mImageView.getWidth() + " Height:" + mImageView.getHeight());
        Log.d(TAG, "Bitmap Width:" + mBitmap.getWidth() + " Height:" + mBitmap.getHeight());

        mScaleX = ((float) mBitmap.getWidth()) / mImageView.getWidth();
        mScaleY = ((float) mBitmap.getHeight()) / mImageView.getHeight();

        //mCanvas.scale(1f/mScaleX,1f/mScaleY);

        Log.d(TAG, "ScaleX:" + mScaleX + " ScaleY:" + mScaleY);
    }

    // These two methods deal with user preferences
    private void loadPreferences() {
        setBackground(mPrefs.getInt(KEY_BACK_COLOR, DEF_BACK_COLOR));
        mPaint.setColor(mPrefs.getInt(KEY_PAINT_COLOR, DEF_PAINT_COLOR));
        mPaint.setStrokeWidth(mPrefs.getInt(KEY_PAINT_SIZE, DEF_PAINT_SIZE));
    }

    private void savePreferences() {
        SharedPreferences.Editor editor = mPrefs.edit();
        editor.putInt(KEY_BACK_COLOR, mBackColor);
        editor.putInt(KEY_PAINT_COLOR, mPaint.getColor());
        editor.putInt(KEY_PAINT_SIZE, (int) mPaint.getStrokeWidth());
        editor.commit();
    }

    private void openImageGallery() {
        // Do some Intent magic to open the Gallery? Yes!
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(
                Intent.createChooser(intent, "Select..."), REQUEST_CODE);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        //after reloading set new scale
        setScale();
    }

    @Override
    public void onPause() {
        savePreferences();
        super.onPause();
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        float x = motionEvent.getX() * mScaleX;
        float y = motionEvent.getY() * mScaleY;

        switch (motionEvent.getAction()) {
            case MotionEvent.ACTION_DOWN: // press
                Log.d(TAG, "Begin x:" + x + ", y:" + y);
                break;

            case MotionEvent.ACTION_MOVE: // move
                // Draw line and update image
                mCanvas.drawLine(mX, mY, x, y, mPaint);
                break;
            case MotionEvent.ACTION_UP: // release
            case MotionEvent.ACTION_CANCEL:
                Log.d(TAG, "End x:" + x + ", y:" + y);
                return true;
        }

        // Save positions
        mX = x;
        mY = y;

        //Draw a circle @ the end to nicer look
        mCanvas.drawCircle(x, y, mPaint.getStrokeWidth() / 2, mPaint);
        // Update image
        mImageView.setImageBitmap(mBitmap);
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // when new backround returned
        if (requestCode == REQUEST_CODE && resultCode == RESULT_OK) {
            Bitmap bm = FileOperations.loadImage(this, data);
            if (bm != null) {
                setBackground(bm);
            }
        }

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.brush_color:
                pickColor(mPaint.getColor(), coloredObj.BRUSH);
                return true;
            case R.id.brush_size:
                pickSize((int) mPaint.getStrokeWidth());
                return true;
            case R.id.background_solid_color:
                pickColor(mBackColor, coloredObj.BACKGROUND);
                return true;
            case R.id.save:
                FileOperations.saveImage(this, mBitmap);
                return true;
            case R.id.load:
                openImageGallery();
                return true;
            case R.id.reset:
                Intent intent = getIntent();
                finish();
                startActivity(intent);
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    // Show size picking dialog
    private void pickSize(int initialSize) {
        final SeekBar sB = new SeekBar(this);
        sB.setMax(MAX_PAINT_SIZE);
        sB.setProgress(initialSize);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        // Add the buttons
        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked OK button
                mPaint.setStrokeWidth(sB.getProgress());
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User cancelled the dialog
                //Do nothing
            }
        });

        // Set other dialog properties
        builder.setTitle(R.string.size_dialog_title)
                .setView(sB);

        // Create and show the AlertDialog
        builder.create().show();

    }

    //Show color picking dialog
    private void pickColor(int initialColor, final coloredObj cObj) {
        // initialColor is the initially-selected color to be shown in the rectangle on the left of the arrow.
        // for example, 0xff000000 is black, 0xff0000ff is blue. Please be aware of the initial 0xff which is the alpha.
        AmbilWarnaDialog dialog = new AmbilWarnaDialog(this, initialColor, true, new AmbilWarnaDialog.OnAmbilWarnaListener() {
            @Override
            public void onOk(AmbilWarnaDialog dialog, int color) {
                // color is the color selected by the user.
                switch (cObj) {
                    case BACKGROUND:
                        setBackground(color);
                        break;
                    case BRUSH:
                        mPaint.setColor(color);
                        break;
                }

            }

            @Override
            public void onCancel(AmbilWarnaDialog dialog) {
                // cancel was selected by the user
            }
        });

        dialog.show();
    }



}