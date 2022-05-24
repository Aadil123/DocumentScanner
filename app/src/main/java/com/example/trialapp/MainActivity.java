package com.example.trialapp;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {
    static {System.loadLibrary("opencv_java4");}
    static String TAG;
    JavaCameraView javaCameraView;
    Button takePicture;
    Rect rectFinal;
    RotatedRect rotatedRectFinal;
    private Mat myFrame;
    BaseLoaderCallback mloaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case BaseLoaderCallback.SUCCESS: {
                    javaCameraView.enableView();
                    break;
                }
                default: {
                    super.onManagerConnected(status);
                    break;
                }
            }
            super.onManagerConnected(status);
        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if(OpenCVLoader.initDebug()){
            Log.d(TAG, "OpenCV successfully imported!");
        }
        else {
            Log.d(TAG, "OpenCV couldn't be imported successfully!");
        }
        rectFinal = new Rect();
        rotatedRectFinal = new RotatedRect();
        javaCameraView = findViewById(R.id.java_camera_view);
        takePicture = findViewById(R.id.button);
        javaCameraView.setCameraPermissionGranted();
        javaCameraView.setVisibility(SurfaceView.VISIBLE);
        javaCameraView.setCvCameraViewListener(this);
        takePicture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
                String filename = "temp.png";
                File file = new File(path, filename);
                Mat intMat = new Mat();
                Imgproc.rectangle(myFrame,rectFinal.tl(),rectFinal.br(),new Scalar(0,0,0),3);
                Imgproc.cvtColor(myFrame,intMat,Imgproc.COLOR_RGBA2BGR,3);
                Imgcodecs.imwrite(file.toString(),intMat);
                Intent intent = new Intent(MainActivity.this,DisplayImage.class);
                intent.putExtra("RECT_X",rectFinal.x);
                intent.putExtra("RECT_Y",rectFinal.y);
                intent.putExtra("RECT_W",rectFinal.width);
                intent.putExtra("RECT_H",rectFinal.height);
                startActivity(intent);
            }
        });
    }
    @Override
    protected void onPause(){
        super.onPause();
        if(javaCameraView!=null)
            javaCameraView.disableView();
    }
    @Override
    protected void onDestroy(){
        super.onDestroy();;
        if(javaCameraView!=null)
            javaCameraView.disableView();
    }
    @Override
    protected void onResume(){
        super.onResume();
        if(OpenCVLoader.initDebug()){
            Log.d(TAG, "OpenCV successfully imported!");
            mloaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
        else{
            Log.d(TAG,"OpenCV couldn't be imported successfully!");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION,this, mloaderCallback);
        }
    }


    @Override
    public void onCameraViewStarted(int width, int height) {
        myFrame = new Mat(height,width, CvType.CV_8UC4);
    }

    @Override
    public void onCameraViewStopped() {
        myFrame.release();
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        myFrame = inputFrame.rgba();
        Mat rotImage = Imgproc.getRotationMatrix2D(new Point(myFrame.cols() / 2,
                myFrame.rows() / 2), 270, 1.5);
        Imgproc.warpAffine(myFrame, myFrame, rotImage, myFrame.size());
        Mat mGray = new Mat();
        Imgproc.cvtColor(myFrame, mGray, Imgproc.COLOR_BGR2GRAY);
        Imgproc.GaussianBlur(mGray, mGray, new Size(5, 5), 5);
        Imgproc.Canny(mGray, mGray, 50, 150, 3, false);
        Mat kernell = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(9,9));
        Imgproc.morphologyEx(mGray, mGray, Imgproc.MORPH_CLOSE, kernell);

        List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(mGray, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
        MatOfPoint2f approxCurve = new MatOfPoint2f();
        double maxVal = 0;
        int maxValIdx = 0;
        double areaRect = 0;
        for (int idx = 0; idx < contours.size() ; idx++) {
            double contourArea = Imgproc.contourArea(contours.get(idx));
            if (maxVal<contourArea){
                maxVal = contourArea;
                maxValIdx = idx;
            }

            MatOfPoint2f contour2f = new MatOfPoint2f( contours.get(idx).toArray() );
            //Processing on mMOP2f1 which is in type MatOfPoint2f
            RotatedRect rotatedRect = Imgproc.minAreaRect(contour2f);
            double approxDistance = Imgproc.arcLength(contour2f, true)*0.02;
            Imgproc.approxPolyDP(contour2f, approxCurve, approxDistance, true);

            //Convert back to MatOfPoint
            MatOfPoint points = new MatOfPoint( approxCurve.toArray() );

            // Get bounding rect of contour
            Rect rect = Imgproc.boundingRect(points);
            //double areaRectTemp = rotatedRect.size.area();
            double areaRectTemp = rect.area();
            if (areaRect<areaRectTemp){
                areaRect = areaRectTemp;
                rectFinal = rect;
            }
        }

        Imgproc.rectangle(myFrame,rectFinal.tl(),rectFinal.br(),new Scalar(0,255,0,255),3);
       /* Point points[] = new Point[4];
        rotatedRectFinal.points(points);
        for (int i=0;i<4;i++){
            Imgproc.line(myFrame,points[i],points[(i+1)%4],new Scalar(0,255,0),3);
        }*/
        return myFrame;
    }

}