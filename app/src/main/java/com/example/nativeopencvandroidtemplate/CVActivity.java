package com.example.nativeopencvandroidtemplate;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.constraint.solver.widgets.Rectangle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfInt;
import org.opencv.core.Rect;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CVActivity extends AppCompatActivity {

    List<MatOfPoint> squares = new ArrayList<MatOfPoint>();
    List<MatOfPoint> selectedSquares = new ArrayList<MatOfPoint>();
    int N = 11;
    private Bitmap imgBitmap;
    private ImageView imageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        String imagePath = intent.getStringExtra("image"); //if it's a string you stored.

        setContentView(R.layout.activity_cv);

        OpenCVLoader.initDebug();

        imageView = findViewById(R.id.cv_img);
        File imgFile = new File(imagePath);
        imgBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
        imageView.setImageBitmap(imgBitmap);

        imageView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {

                int action = event.getAction();
                if (action == MotionEvent.ACTION_DOWN) {
                    float x = event.getX();
                    float y = event.getY();

                    double scaleX = (double) imgBitmap.getWidth() / imageView.getWidth();
                    double scaleY = (double)imgBitmap.getHeight() / imageView.getHeight();
                    Point p = new Point(x * scaleX, y * scaleY);

                    MatOfPoint selSquare = null;
                    for (MatOfPoint square : squares) {
                        if (Imgproc.pointPolygonTest(new MatOfPoint2f(square.toArray()), p, true) > 0) {
                            selSquare = square;
                            break;
                        }
                    }

                    if(selSquare != null) {
                        if(selectedSquares.indexOf(selSquare) != -1) {
                            selectedSquares.remove(selSquare);
                        }
                        else {
                            selectedSquares.add(selSquare);
                        }
                    }

                    repaint();
                }

                return false;
            }
        });

        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                applyFilter();
            }
        }, 1000);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_save) {
            String dirName = Environment.getExternalStorageDirectory()+"/crops";
            File dir = new File(dirName);
            if (dir.isDirectory())
            {
                String[] children = dir.list();
                for (int i = 0; i < children.length; i++)
                {
                    new File(dir, children[i]).delete();
                }
            }
            else {
                dir.mkdir();
            }

            int idx = 1;

            Mat img = new Mat();
            Utils.bitmapToMat(imgBitmap, img);

            for(MatOfPoint square : selectedSquares) {

                Mat cropImg = img.clone();

                Rect roi = Imgproc.boundingRect(square);
                Mat cropped = new Mat(cropImg, roi);

                final Bitmap bitmap =
                        Bitmap.createBitmap(roi.width, roi.height, Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(cropped, bitmap, false);

                try (FileOutputStream out = new FileOutputStream(dirName + "/" + String.valueOf(idx) + ".png")) {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out); // bmp is your Bitmap instance
                    // PNG is a lossless format, the compression factor (100) is ignored
                } catch (IOException e) {
                    e.printStackTrace();
                }

                idx ++;
            }

            // String picturePath contains the path of selected Image
            Intent myIntent = new Intent(this, GalleryActivity.class);
            this.startActivity(myIntent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void applyFilter() {

        Mat img = new Mat();
        Utils.bitmapToMat(imgBitmap, img);

        Imgproc.cvtColor(img, img, Imgproc.COLOR_RGB2BGRA);

        Mat img_result = img.clone();



        findSquares(img_result, squares);


        Toast.makeText(this, "Total rectangles: " + squares.size(), Toast.LENGTH_SHORT).show();
    }

    private void repaint() {

        Mat img = new Mat();
        Utils.bitmapToMat(imgBitmap, img);

        Mat img_result = img.clone();

//        Imgproc.polylines(img_result, squares, true, new Scalar(255, 127, 80, 255), 10);
//        Imgproc.polylines(img_result, selectedSquares, true, new Scalar(255, 127, 80, 255), 10);


        for ( int contourIdx=0; contourIdx < selectedSquares.size(); contourIdx++ )
        {
            // Minimum size allowed for consideration
            MatOfPoint2f approxCurve = new MatOfPoint2f();
            MatOfPoint2f contour2f = new MatOfPoint2f( selectedSquares.get(contourIdx).toArray() );
            //Processing on mMOP2f1 which is in type MatOfPoint2f
            double approxDistance = Imgproc.arcLength(contour2f, true)*0.02;
            Imgproc.approxPolyDP(contour2f, approxCurve, approxDistance, true);

            //Convert back to MatOfPoint
            MatOfPoint points = new MatOfPoint( approxCurve.toArray() );

            // Get bounding rect of contour
            Rect rect = Imgproc.boundingRect(points);

            Imgproc.rectangle(img_result, new Point(rect.x, rect.y), new Point(rect.x + rect.width, rect.y + rect.height), new Scalar(255, 127, 80, 255), 3);




        }








        final Bitmap bitmap =
                Bitmap.createBitmap(img_result.cols(), img_result.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(img_result, bitmap, false);
        imageView.setImageBitmap(bitmap);
    }

    // helper function:
    // finds a cosine of angle between vectors
    // from pt0->pt1 and from pt0->pt2
    double angle(Point pt1, Point pt2, Point pt0) {
        double dx1 = pt1.x - pt0.x;
        double dy1 = pt1.y - pt0.y;
        double dx2 = pt2.x - pt0.x;
        double dy2 = pt2.y - pt0.y;
        return (dx1 * dx2 + dy1 * dy2) / Math.sqrt((dx1 * dx1 + dy1 * dy1) * (dx2 * dx2 + dy2 * dy2) + 1e-10);
    }


    // returns sequence of squares detected on the image.
    // the sequence is stored in the specified memory storage
    void findSquares(Mat image, List<MatOfPoint> squares) {

        squares.clear();

        Mat smallerImg = new Mat(new Size(image.width() / 2, image.height() / 2), image.type());
        Imgproc.cvtColor(image, image, Imgproc.COLOR_RGB2GRAY);
        Imgproc.cvtColor(image, image, Imgproc.COLOR_GRAY2RGB, 4);

        Mat gray = new Mat(image.size(), image.type());

        Mat gray0 = new Mat(image.size(), CvType.CV_8UC3);





        // down-scale and upscale the image to filter out the noise
        Imgproc.pyrDown(image, smallerImg, smallerImg.size());
        Imgproc.pyrUp(smallerImg, image, image.size());


        // find squares in every color plane of the image
        for (int c = 0; c < 3; c++) {

            extractChannel(image, gray, c);

            // try several threshold levels
            for (int l = 1; l < N; l++) {
                //Cany removed... Didn't work so well


                Imgproc.threshold(gray, gray0, (l + 1) * 255 / N, 255, Imgproc.THRESH_BINARY);


                List<MatOfPoint> contours = new ArrayList<MatOfPoint>();

                // find contours and store them all as a list
                Imgproc.findContours(gray0, contours, new Mat(), Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);
                MatOfPoint approx = new MatOfPoint();

                // test each contour
                for (int i = 0; i < contours.size(); i++) {

                    // approximate contour with accuracy proportional
                    // to the contour perimeter
                    approx = approxPolyDP(contours.get(i), Imgproc.arcLength(new MatOfPoint2f(contours.get(i).toArray()), true) * 0.02, true);


                    // square contours should have 4 vertices after approximation
                    // relatively large area (to filter out noisy contours)
                    // and be convex.
                    // Note: absolute value of an area is used because
                    // area may be positive or negative - in accordance with the
                    // contour orientation

            Log.d("maizer","            "+Math.abs(Imgproc.contourArea(approx)));
                    if (
//                            approx.toArray().length == 4 &&
                            Math.abs(Imgproc.contourArea(approx)) > 6000 &&
                            Math.abs(Imgproc.contourArea(approx)) < 140000
//                                    &&
//                            Imgproc.isContourConvex(approx)
                    )
                    {


                        double maxCosine = 0;

                        for (int j = 2; j < 5; j++) {
                            // find the maximum cosine of the angle between joint edges
                            double cosine = Math.abs(angle(approx.toArray()[j % 4], approx.toArray()[j - 2], approx.toArray()[j - 1]));
                            maxCosine = Math.max(maxCosine, cosine);
                        }

                        // if cosines of all angles are small
                        // (all angles are ~90 degree) then write quandrange
                        // vertices to resultant sequence
//                        squares.add(approx);

                        if (maxCosine < 0.9){
                            squares.add(approx);
                        }

                    }
                }
            }
        }
    }

    void extractChannel(Mat source, Mat out, int channelNum) {
        List<Mat> sourceChannels = new ArrayList<Mat>();
        List<Mat> outChannel = new ArrayList<Mat>();

        Core.split(source, sourceChannels);

        outChannel.add(new Mat(sourceChannels.get(0).size(), sourceChannels.get(0).type()));

        Core.mixChannels(sourceChannels, outChannel, new MatOfInt(channelNum, 0));

        Core.merge(outChannel, out);
    }

    MatOfPoint approxPolyDP(MatOfPoint curve, double epsilon, boolean closed) {
        MatOfPoint2f tempMat = new MatOfPoint2f();

        Imgproc.approxPolyDP(new MatOfPoint2f(curve.toArray()), tempMat, epsilon, closed);

        return new MatOfPoint(tempMat.toArray());
    }

}
