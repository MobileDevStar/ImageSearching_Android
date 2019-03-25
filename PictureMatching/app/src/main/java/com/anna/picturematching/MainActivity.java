package com.anna.picturematching;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;

import org.apache.sanselan.Sanselan;
import org.apache.sanselan.formats.jpeg.JpegImageMetadata;
import org.apache.sanselan.formats.jpeg.exifRewrite.ExifRewriter;
import org.apache.sanselan.formats.tiff.TiffImageMetadata;
import org.apache.sanselan.formats.tiff.write.TiffOutputDirectory;
import org.apache.sanselan.formats.tiff.write.TiffOutputSet;
import org.json.JSONObject;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.DMatch;
import org.opencv.core.Mat;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.Scalar;
import org.opencv.features2d.DescriptorExtractor;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.FeatureDetector;
import org.opencv.features2d.GFTTDetector;
import org.opencv.imgproc.Imgproc;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.hardware.Camera.Size;
import android.media.Image;
import android.media.ThumbnailUtils;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.StatFs;
import android.text.format.Formatter;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SubMenu;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;


public class MainActivity extends Activity  implements CvCameraViewListener2 {
    private static final String TAG = "PictureMatching::Activity";

    SqlTable sql = new SqlTable(this,"img_desc",null,1);

    private CameraView mOpenCvCameraView;
    private List<Size> mResolutionList;
    private MenuItem[] mEffectMenuItems;
    private SubMenu mColorEffectsMenu;
    private MenuItem[] mResolutionMenuItems;
    private SubMenu mResolutionMenu;

    private Button mCaptureButton;
    private ImageButton mImageView;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");
                    mOpenCvCameraView.enableView();
                    mOpenCvCameraView.setMainActivity(MainActivity.this);
                   // mOpenCvCameraView.setOnTouchListener(MainActivity.this);
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    public MainActivity() {
        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_main);

        mCaptureButton = (Button) findViewById(R.id.button_capture);
        mCaptureButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                takePicture();
            }
        });
        mImageView = (ImageButton) findViewById(R.id.button_imageView);
        mImageView.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View v) {
                mImageView.setVisibility(View.INVISIBLE);
            }
        });

        mOpenCvCameraView = (CameraView) findViewById(R.id.java_surface_view);

        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);

        mOpenCvCameraView.setCvCameraViewListener(this);
    }

    @Override
    public void onPause()
    {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume()
    {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    public void onCameraViewStarted(int width, int height) {
    }

    public void onCameraViewStopped() {
    }

    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        return inputFrame.rgba();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        List<String> effects = mOpenCvCameraView.getEffectList();

        if (effects == null) {
            Log.e(TAG, "Color effects are not supported by device!");
            return true;
        }

        mColorEffectsMenu = menu.addSubMenu("Color Effect");
        mEffectMenuItems = new MenuItem[effects.size()];

        int idx = 0;
        ListIterator<String> effectItr = effects.listIterator();
        while(effectItr.hasNext()) {
            String element = effectItr.next();
            mEffectMenuItems[idx] = mColorEffectsMenu.add(1, idx, Menu.NONE, element);
            idx++;
        }

        mResolutionMenu = menu.addSubMenu("Resolution");
        mResolutionList = mOpenCvCameraView.getResolutionList();
        mResolutionMenuItems = new MenuItem[mResolutionList.size()];

        ListIterator<Size> resolutionItr = mResolutionList.listIterator();
        idx = 0;
        while(resolutionItr.hasNext()) {
            Size element = resolutionItr.next();
            mResolutionMenuItems[idx] = mResolutionMenu.add(2, idx, Menu.NONE,
                    Integer.valueOf(element.width).toString() + "x" + Integer.valueOf(element.height).toString());
            idx++;
        }

        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        Log.i(TAG, "called onOptionsItemSelected; selected item: " + item);
        if (item.getGroupId() == 1)
        {
            mOpenCvCameraView.setEffect((String) item.getTitle());
            Toast.makeText(this, mOpenCvCameraView.getEffect(), Toast.LENGTH_SHORT).show();
        }
        else if (item.getGroupId() == 2)
        {
            int id = item.getItemId();
            Size resolution = mResolutionList.get(id);
            mOpenCvCameraView.setResolution(resolution);
            resolution = mOpenCvCameraView.getResolution();
            String caption = Integer.valueOf(resolution.width).toString() + "x" + Integer.valueOf(resolution.height).toString();
            Toast.makeText(this, caption, Toast.LENGTH_SHORT).show();
        }

        return true;
    }

    private void takePicture() {
        mOpenCvCameraView.takePicture();
    }

    @SuppressLint("SimpleDateFormat")
    public void showMatchedImage(byte[] imageData) {

        new ImageProcessor(this).execute(imageData);
    }
    /*
     * Image modifiers
     */
    byte[] resizeImage(byte[] input, int width, int height) {

        //down scale and crop image
        Bitmap original = BitmapFactory.decodeByteArray(input, 0, input.length);
        Bitmap resized = ThumbnailUtils.extractThumbnail(original, width, height, ThumbnailUtils.OPTIONS_RECYCLE_INPUT);
        Bitmap dest = resized;

        //set encoding format
        ByteArrayOutputStream blob = new ByteArrayOutputStream();

        dest.compress(Bitmap.CompressFormat.JPEG, 100, blob);

        resized.recycle();

        return blob.toByteArray();
    }

    private byte[] addJPEGExifTagsFromSource(byte[] sourceMetaData, byte[] destImageData) {
        try {
            TiffOutputSet outputSet = null;
            JpegImageMetadata metadata = (JpegImageMetadata) Sanselan.getMetadata(sourceMetaData);
            if (null != metadata) {
                TiffImageMetadata exif = metadata.getExif();
                if (null != exif) {
                    outputSet = exif.getOutputSet();

                    List<?> dirs = outputSet.getDirectories();
                    for (int i = 0; i < dirs.size(); i++) {
                        try {
                            TiffOutputDirectory dir = (TiffOutputDirectory) dirs.get(i);
                            dir.setJpegImageData(null);
                            dir.setTiffImageData(null);
                        } catch (Exception e) {
                            Log.d(TAG, "Exception on removing thumbnail image: " + e.getMessage());
                        }
                    }
                }
            }

            if (null != outputSet) {
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                ExifRewriter ER = new ExifRewriter();
                ER.updateExifMetadataLossless(destImageData, bos, outputSet);
                return bos.toByteArray();
            }
        } catch (Exception e) {
            Log.d(TAG, "Exception on exif adding: " + e.getMessage());
        }
        return destImageData;
    }


    private class ImageProcessor extends AsyncTask<byte[], Void, Bitmap> {

        byte[] imageData = null;
        Image mTargetImage = null;

        private Mat descriptors1;
        private FeatureDetector detector;
        private DescriptorExtractor DescExtractor;
        private DescriptorMatcher matcher;
        private MatOfKeyPoint keypoints1;
        private MatOfDMatch matches, matches_final_mat;

        private ProgressDialog pd;
        private MainActivity asyncTaskContext=null;
        private Scalar RED = new Scalar(255,0,0);
        private Scalar GREEN = new Scalar(0,255,0);

        public ImageProcessor (MainActivity context)
        {
            asyncTaskContext=context;
        }
        @Override
        protected void onPreExecute() {
            pd = new ProgressDialog(asyncTaskContext);
            pd.setIndeterminate(true);
            pd.setCancelable(true);
            pd.setCanceledOnTouchOutside(false);
            pd.setMessage("Processing...");
            pd.show();
        }


        @Override
        protected Bitmap doInBackground(byte[]... params) {
            imageData = params[0];

            final String timeStamp = String.valueOf(System.currentTimeMillis());
            final String FILENAME = "IMG_" + timeStamp + ".jpg";

            byte[] imageResize = resizeImage(imageData, 640, 480);
            imageResize = addJPEGExifTagsFromSource(imageData, imageResize);

            Bitmap bitmap1 = BitmapFactory.decodeByteArray(imageResize, 0, imageResize.length);

            //////////OpenCV Image Processing/////////////////

            Mat img1 = new Mat();
            Utils.bitmapToMat(bitmap1, img1);

            Imgproc.cvtColor(img1, img1, Imgproc.COLOR_BGR2RGB);
            detector = FeatureDetector.create(FeatureDetector.PYRAMID_FAST);
            DescExtractor = DescriptorExtractor.create(DescriptorExtractor.ORB);
            matcher = DescriptorMatcher
                    .create(DescriptorMatcher.BRUTEFORCE_HAMMING);

            keypoints1 = new MatOfKeyPoint();
            descriptors1 = new Mat();

            detector.detect(img1, keypoints1);
            Log.d("LOG!", "number of query Keypoints= " + keypoints1.size());
            // Descript keypoints
            DescExtractor.compute(img1, keypoints1, descriptors1);

            int maxSimCount = 0;
            String strSimName = "";
            Bitmap matchedImg = null;
            Cursor cursor = sql.dbGetAll();
            while (cursor.moveToNext()) {
                String name = cursor.getString(0);
                int t = cursor.getInt(1);
                int w = cursor.getInt(2);
                int h = cursor.getInt(3);
                byte[] p = cursor.getBlob(4);
                Mat descriptors2 = new Mat(h, w, t);
                // matching descriptors
                matches = new MatOfDMatch();
                matcher.match(descriptors1, descriptors2, matches);
                Log.d("LOG!", "Matches Size " + matches.size());
                // New method of finding best matches
                List<DMatch> matchesList = matches.toList();
                List<DMatch> matches_final = new ArrayList<DMatch>();
                for (int i = 0; i < matchesList.size(); i++) {
                    if (matchesList.get(i).distance <= 10) {
                        matches_final.add(matches.toList().get(i));
                    }
                }

                if (matches_final.size() >= 50) {
                    if (matches_final.size() > maxSimCount) {
                        strSimName = name;
                    }
                }
            }
            /*
               ///////////////////////////////////////////////////////
            */

            String fileName = Environment.getExternalStorageDirectory().getPath() +
                    "/" + FILENAME;

            try {
                FileOutputStream fos = new FileOutputStream(fileName);
                fos.write(imageResize);
                fos.close();

                sql.dbPutImg(FILENAME, descriptors1);

                if (strSimName.length() > 0) {
                    String simPath = Environment.getExternalStorageDirectory().getPath() + "/" + strSimName;
                    FileInputStream fin = new FileInputStream(new File(simPath));
                    int imgLen = fin.available();
                    byte[] matchData = new byte[imgLen];
                    fin.read(matchData, 0, imgLen);
                    fin.close();

                    Log.e("++++++++++++++", strSimName);


                    Bitmap image = BitmapFactory.decodeByteArray(matchData, 0, matchData.length);
                    return image;
                }
            } catch (java.io.IOException e) {
                Log.e("PictureDemo", "Exception in photoCallback", e);
            }


            return null;
        }

        @Override
        protected void onPostExecute(Bitmap image) {
            pd.dismiss();
            if (image != null) {
                mImageView.setImageBitmap(image);
                mImageView.setAlpha(156);
                mImageView.setVisibility(View.VISIBLE);
                Toast.makeText(MainActivity.this, "Matched", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(MainActivity.this, "saved", Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        protected void onProgressUpdate(Void... values) {
        }
    }
}
