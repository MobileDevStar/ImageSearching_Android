package com.anna.picturematching;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera.Size;
import android.media.ThumbnailUtils;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.sanselan.Sanselan;
import org.apache.sanselan.formats.jpeg.JpegImageMetadata;
import org.apache.sanselan.formats.jpeg.exifRewrite.ExifRewriter;
import org.apache.sanselan.formats.tiff.TiffImageMetadata;
import org.apache.sanselan.formats.tiff.write.TiffOutputDirectory;
import org.apache.sanselan.formats.tiff.write.TiffOutputSet;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.CvType;
import org.opencv.core.DMatch;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.features2d.DescriptorExtractor;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.FeatureDetector;
import org.opencv.imgproc.Imgproc;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;


public class CameraActivity extends Activity  implements CvCameraViewListener2 {
    private static final String TAG = "PictureMatching::Activity";

    SqlTable sql = new SqlTable(this,"img_desc",null,1);

    private CameraView mOpenCvCameraView;
    private List<Size> mResolutionList;
    private MenuItem[] mEffectMenuItems;
    private SubMenu mColorEffectsMenu;
    private MenuItem[] mResolutionMenuItems;
    private SubMenu mResolutionMenu;

    private Button mCaptureButton;
    private CircleImageView mImageView;
    private TextView mLabelMatch;
    private CheckBox mChkMatch;

    private boolean mBlTaking = true;
    private int     mImgIndex = 0;

    private Mat                     mRgba;
    private int                     mState = 0;
    private Mat                     mCaptured;

    public static LinkedList<Mat> desc2List = null;
    public static LinkedList<String> prevNames = null;
    private Toast matchToast;


    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");
                    mOpenCvCameraView.enableView();
                    mOpenCvCameraView.setCameraActivity(CameraActivity.this);

                    if (desc2List == null) {
                        desc2List = new LinkedList<Mat>();
                        prevNames = new LinkedList<String>();
                    }
//                    PictureMatchingApp.mData = new ImgMatchData();
//                    if (PictureMatchingApp.mData == null) {
//                        PictureMatchingApp.mData.initialize();
//                    }

                    if (mBlTaking != true) {
                        String imageName = prevNames.get(mImgIndex);
                        new ImageLoader(null, mImageView, true).execute(imageName);
                    }

                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    public CameraActivity() {
        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_camera);

        matchToast = Toast.makeText(CameraActivity.this, "Matched", Toast.LENGTH_SHORT);

        Intent intent = this.getIntent();
        mBlTaking = intent.getBooleanExtra("taking", true);
        mImgIndex = intent.getIntExtra("imageIndex", 0);

        mCaptureButton = (Button) findViewById(R.id.button_capture);
        mCaptureButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                takePicture();
            }
        });
        mImageView = (CircleImageView) findViewById(R.id.button_imageView);

        mLabelMatch = (TextView) findViewById(R.id.lb_match);
        if (mBlTaking) {
            mCaptureButton.setEnabled(true);
            mCaptureButton.setBackground(getResources().getDrawable(R.drawable.capture));
            mLabelMatch.setText(new String("Taking..."));
        }
        else {
            mState = 0;
            mCaptureButton.setEnabled(false);
            mCaptureButton.setBackground(getResources().getDrawable(R.drawable.capture_disabled));
            mLabelMatch.setText(new String("Matching..."));
        }

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
        mRgba = new Mat(height, width, CvType.CV_8UC4);
        mCaptured = new Mat(height, width, CvType.CV_8UC4);
    }

    public void onCameraViewStopped() {
        mRgba.release();
        mCaptured.release();
    }

    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        mRgba = inputFrame.rgba();

        if (!mBlTaking) {
            if (mState == 0) {
                mState = 1;
                new ImageMatcher().execute(mRgba);
            }
        }

        return mRgba;
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
        mCaptured = mRgba;
        if (!mBlTaking) {
            mState = 2;
            mCaptureButton.setEnabled(false);
            mCaptureButton.setBackground(getResources().getDrawable(R.drawable.capture_disabled));
        }
        mOpenCvCameraView.takePicture();
    }

    @SuppressLint("SimpleDateFormat")
    public void showMatchedImage(byte[] imageData) {

        new ImageSaver(this).execute(imageData);
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

    private Mat descriptors1 = null;

    private class ImageMatcher extends AsyncTask<Mat, Void, String> {

        private FeatureDetector detector;
        private DescriptorExtractor DescExtractor;
        private MatOfKeyPoint keypoints1;

        @Override
        protected String doInBackground(Mat... mats) {

            Mat curImage = mats[0];
            Mat img1 = new Mat();

            Imgproc.cvtColor(curImage, img1, Imgproc.COLOR_BGR2RGB);
            detector = FeatureDetector.create(FeatureDetector.ORB);
            DescExtractor = DescriptorExtractor.create(DescriptorExtractor.ORB);
            DescriptorMatcher matcher = DescriptorMatcher
                    .create(DescriptorMatcher.BRUTEFORCE_HAMMING);

            keypoints1 = new MatOfKeyPoint();
            descriptors1 = new Mat();
            detector.detect(img1, keypoints1);
            Log.d("LOG!", "number of query Keypoints= " + keypoints1.size());
            // Descript keypoints
            DescExtractor.compute(img1, keypoints1, descriptors1);

            int minSimCount = 10000;
            String strSimName = "";
            MatOfDMatch matches = new MatOfDMatch();

            Mat descriptors2 = desc2List.get(mImgIndex);
            if (descriptors2 != null) {
                matcher.match(descriptors1, descriptors2, matches);
                Log.d("LOG!", "Matches Size " + matches.size());
                // New method of finding best matches
                List<DMatch> matchesList = matches.toList();
                List<DMatch> matches_final = new ArrayList<DMatch>();
                Double max_dist = 0.0;
                Double min_dist = 100.0;

                for (int i = 0; i < matchesList.size(); i++) {
                    Double dist = (double) matchesList.get(i).distance;
                    if (dist < min_dist)
                        min_dist = dist;
                    if (dist > max_dist)
                        max_dist = dist;
                }

                LinkedList<DMatch> good_matches = new LinkedList<DMatch>();
                for (int i = 0; i < matchesList.size(); i++) {
                    if (matchesList.get(i).distance <= (1.5 * min_dist))
                        good_matches.addLast(matchesList.get(i));
                }

                minSimCount = good_matches.size();
                strSimName = prevNames.get(mImgIndex);
            }


            if (strSimName.length() > 0 && minSimCount < 3) {
                return strSimName;
            }


            return null;
        }

        @Override
        protected void onPostExecute(String imageName) {
            //  pd.dismiss();
            if (imageName != null) {
                mState = 0;
                mCaptureButton.setEnabled(true);
                mCaptureButton.setBackground(getResources().getDrawable(R.drawable.capture));
                matchToast.show();
            } else {
                matchToast.cancel();
                mState = 0;
            }
        }
    }

    private class ImageSaver extends AsyncTask<byte[], Void, Bitmap> {

        byte[] imageData = null;

        private FeatureDetector detector;
        private DescriptorExtractor DescExtractor;
        private MatOfKeyPoint keypoints1;


        public ImageSaver (CameraActivity context)
        {

        }
        @Override
        protected void onPreExecute() {
            Toast.makeText(CameraActivity.this, "saving...", Toast.LENGTH_SHORT).show();
        }


        @Override
        protected Bitmap doInBackground(byte[]... params) {
            imageData = params[0];

            final String timeStamp = String.valueOf(System.currentTimeMillis());
            final String FILENAME = "IMG_" + timeStamp + ".jpg";

            byte[] imageResize = resizeImage(imageData, 640, 480);
            imageResize = addJPEGExifTagsFromSource(imageData, imageResize);

            //////////OpenCV Image Processing/////////////////

            Mat img1 = new Mat();

            Imgproc.cvtColor(mCaptured, img1, Imgproc.COLOR_BGR2RGB);
            detector = FeatureDetector.create(FeatureDetector.ORB);
            DescExtractor = DescriptorExtractor.create(DescriptorExtractor.ORB);
            DescriptorMatcher matcher = DescriptorMatcher
                    .create(DescriptorMatcher.BRUTEFORCE_HAMMING);

            keypoints1 = new MatOfKeyPoint();
            descriptors1 = new Mat();
            detector.detect(img1, keypoints1);
            Log.d("LOG!", "number of query Keypoints= " + keypoints1.size());
            // Descript keypoints
            DescExtractor.compute(img1, keypoints1, descriptors1);
            String fileName = Environment.getExternalStorageDirectory().getPath() +
                    "/" + FILENAME;

            try {
                FileOutputStream fos = new FileOutputStream(fileName);
                fos.write(imageResize);
                fos.close();

                desc2List.addLast(descriptors1);
                prevNames.addLast(FILENAME);
            } catch (java.io.IOException e) {
                Log.e("PictureDemo", "Exception in photoCallback", e);
            }


            return null;
        }

        @Override
        protected void onPostExecute(Bitmap image) {
            mState = 0;
            Toast.makeText(CameraActivity.this, "saved", Toast.LENGTH_SHORT).show();
        }

        @Override
        protected void onProgressUpdate(Void... values) {
        }
    }
}
