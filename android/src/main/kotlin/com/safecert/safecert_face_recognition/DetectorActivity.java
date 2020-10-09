/*
 * Copyright 2019 The TensorFlow Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.safecert.safecert_face_recognition;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.hardware.camera2.CameraCharacteristics;
import android.media.ExifInterface;
import android.media.ImageReader.OnImageAvailableListener;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Size;
import android.util.TypedValue;
import android.widget.Toast;

import com.safecert.safecert_face_recognition.customview.OverlayView;
import com.safecert.safecert_face_recognition.env.BorderedText;
import com.safecert.safecert_face_recognition.env.ImageUtils;
import com.safecert.safecert_face_recognition.env.Logger;
import com.safecert.safecert_face_recognition.tflite.SimilarityClassifier;
import com.safecert.safecert_face_recognition.tflite.TFLiteObjectDetectionAPIModel;
import com.safecert.safecert_face_recognition.tracking.MultiBoxTracker;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

/**
 * An activity that uses a TensorFlowMultiBoxDetector and ObjectTracker to detect and then track
 * objects.
 */
interface CallBackImport {
    void done();
}

public class DetectorActivity extends CameraActivity implements OnImageAvailableListener {
    private static final Logger LOGGER = new Logger();

    // FaceNet
//  private static final int TF_OD_API_INPUT_SIZE = 160;
//  private static final boolean TF_OD_API_IS_QUANTIZED = false;
//  private static final String TF_OD_API_MODEL_FILE = "facenet.tflite";
//  //private static final String TF_OD_API_MODEL_FILE = "facenet_hiroki.tflite";

    // MobileFaceNet
    private static final int TF_OD_API_INPUT_SIZE = 112;
    private static final boolean TF_OD_API_IS_QUANTIZED = false;
    private static final String TF_OD_API_MODEL_FILE = "mobile_face_net.tflite";


    private static final String TF_OD_API_LABELS_FILE = "file:///android_asset/labelmap.txt";

    private static final DetectorMode MODE = DetectorMode.TF_OD_API;
    // Minimum detection confidence to track a detection.
    private static final float MINIMUM_CONFIDENCE_TF_OD_API = 0.5f;
    private static final boolean MAINTAIN_ASPECT = false;

    private static final Size DESIRED_PREVIEW_SIZE = new Size(640, 480);
    //private static final int CROP_SIZE = 320;
    //private static final Size CROP_SIZE = new Size(320, 320);


    private static final boolean SAVE_PREVIEW_BITMAP = false;
    private static final float TEXT_SIZE_DIP = 10;
    OverlayView trackingOverlay;
    private Integer sensorOrientation;

    private SimilarityClassifier detector;

    private long lastProcessingTimeMs;
    private Bitmap rgbFrameBitmap = null;
    private Bitmap croppedBitmap = null;
    private Bitmap cropCopyBitmap = null;

    private Bitmap rgbFrameBitmapImport = null;
    private Bitmap croppedBitmapImport = null;
    private Bitmap cropCopyBitmapImport = null;
    private Bitmap portraitBmpImport = null;
    // here the face is cropped and drawn
    private Bitmap faceBmpImport = null;

    private boolean computingDetection = false;
    private boolean addPending = false;
    //private boolean adding = false;

    private long timestamp = 0;

    private Matrix frameToCropTransform;
    private Matrix cropToFrameTransform;
    private Matrix frameToCropTransformImport;
    private Matrix cropToFrameTransformImport;
    //private Matrix cropToPortraitTransform;

    private MultiBoxTracker tracker;

    private BorderedText borderedText;

    // Face detector
    private FaceDetector faceDetector;

    // here the preview image is drawn in portrait way
    private Bitmap portraitBmp = null;
    // here the face is cropped and drawn
    private Bitmap faceBmp = null;

    private FloatingActionButton fabAdd;

    //    private byte[] image;
    private List<String> path;
    private List<String> name;
    private Boolean muti = false;
    Bitmap crop = null;
//    Bitmap newBitmap;

    //private HashMap<String, Classifier.Recognition> knownFaces = new HashMap<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        path = getIntent().getStringArrayListExtra("path");
        name = getIntent().getStringArrayListExtra("name");
        muti = getIntent().getBooleanExtra("muti", false);
//        fabAdd = findViewById(R.id.fab_add);
//        fabAdd.setOnClickListener(view -> onAddClick());
//    fabAdd.setOnTouchListener(new View.OnTouchListener() {
//      @Override
//      public boolean onTouch(View view, MotionEvent event) {
//        if (gestureDetector.onTouchEvent(event)) {
//          // code for single tap or onclick
//
//          // pass your intent here
//        } else {
//
//          switch (event.getAction()) {
//            //  code for move and drag
//
//            // handle your MotionEvents here
//          }
//          return true;
//        }
//      }
//    });

        // Real-time contour detection of multiple faces
        FaceDetectorOptions options =
                new FaceDetectorOptions.Builder()
                        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                        .setContourMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
                        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
                        .build();


        FaceDetector detector = FaceDetection.getClient(options);

        faceDetector = detector;



//        Bitmap bmp = BitmapFactory.decodeFile(path);
//        newBitmap = RecognitionImageData.reSizeBitmap(bmp, 400, 800);
//        onImportImage(path, name, newBitmap, count);
        //checkWritePermission();
        importDataFace(path, name, 0);
    }

    private void importDataFace(List<String> listPath, List<String> listName, int count) {
        final int[] _count = {count};
        Bitmap bmp = BitmapFactory.decodeFile(listPath.get(count));
        Bitmap newBitmap = RecognitionImageData.reSizeBitmap(bmp, 400, 800);
        onImportImage(listPath.get(count), listName.get(count), newBitmap, 0, new CallBackImport() {
            @Override
            public void done() {
                if (count == listPath.size() - 1)
                    return;
                _count[0] += 1;
                importDataFace(listPath, listName, _count[0]);
            }
        });
    }

//    private void onAddClick() {
//
//        addPending = true;
//        //Toast.makeText(this, "click", Toast.LENGTH_LONG ).show();
//
//    }

    @Override
    public void onPreviewSizeChosen(final Size size, final int rotation) {
        final float textSizePx =
                TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP, TEXT_SIZE_DIP, getResources().getDisplayMetrics());
        borderedText = new BorderedText(textSizePx);
        borderedText.setTypeface(Typeface.MONOSPACE);

        tracker = new MultiBoxTracker(this);


        try {
            detector =
                    TFLiteObjectDetectionAPIModel.create(
                            getAssets(),
                            TF_OD_API_MODEL_FILE,
                            TF_OD_API_LABELS_FILE,
                            TF_OD_API_INPUT_SIZE,
                            TF_OD_API_IS_QUANTIZED);
            //cropSize = TF_OD_API_INPUT_SIZE;
        } catch (final IOException e) {
            e.printStackTrace();
            LOGGER.e(e, "Exception initializing classifier!");
            Toast toast =
                    Toast.makeText(
                            getApplicationContext(), "Classifier could not be initialized", Toast.LENGTH_SHORT);
            toast.show();
            finish();
        }

        previewWidth = size.getWidth();
        previewHeight = size.getHeight();

        sensorOrientation = rotation - getScreenOrientation();
        LOGGER.i("Camera orientation relative to screen canvas: %d", sensorOrientation);

        LOGGER.i("Initializing at size %dx%d", previewWidth, previewHeight);
        rgbFrameBitmap = Bitmap.createBitmap(previewWidth, previewHeight, Config.ARGB_8888);


        int targetW, targetH;
        if (sensorOrientation == 90 || sensorOrientation == 270) {
            targetH = previewWidth;
            targetW = previewHeight;
        } else {
            targetW = previewWidth;
            targetH = previewHeight;
        }
        int cropW = (int) (targetW / 2.0);
        int cropH = (int) (targetH / 2.0);

        croppedBitmap = Bitmap.createBitmap(cropW, cropH, Config.ARGB_8888);

        portraitBmp = Bitmap.createBitmap(targetW, targetH, Config.ARGB_8888);
        faceBmp = Bitmap.createBitmap(TF_OD_API_INPUT_SIZE, TF_OD_API_INPUT_SIZE, Config.ARGB_8888);

        frameToCropTransform =
                ImageUtils.getTransformationMatrix(
                        previewWidth, previewHeight,
                        cropW, cropH,
                        sensorOrientation, MAINTAIN_ASPECT);

//    frameToCropTransform =
//            ImageUtils.getTransformationMatrix(
//                    previewWidth, previewHeight,
//                    previewWidth, previewHeight,
//                    sensorOrientation, MAINTAIN_ASPECT);

        cropToFrameTransform = new Matrix();
        frameToCropTransform.invert(cropToFrameTransform);


        Matrix frameToPortraitTransform =
                ImageUtils.getTransformationMatrix(
                        previewWidth, previewHeight,
                        targetW, targetH,
                        sensorOrientation, MAINTAIN_ASPECT);


        trackingOverlay = findViewById(R.id.tracking_overlay);
        trackingOverlay.addCallback(
                canvas -> {
                    tracker.draw(canvas);
                    if (isDebug()) {
                        tracker.drawDebug(canvas);
                    }
                });

        tracker.setFrameConfiguration(previewWidth, previewHeight, sensorOrientation);
    }


    @Override
    protected void processImage() {
        ++timestamp;
        final long currTimestamp = timestamp;
        trackingOverlay.postInvalidate();

        // No mutex needed as this method is not reentrant.
        if (computingDetection) {
            readyForNextImage();
            return;
        }
        computingDetection = true;

        LOGGER.i("Preparing image " + currTimestamp + " for detection in bg thread.");

        rgbFrameBitmap.setPixels(getRgbBytes(), 0, previewWidth, 0, 0, previewWidth, previewHeight);

        readyForNextImage();

        final Canvas canvas = new Canvas(croppedBitmap);
        canvas.drawBitmap(rgbFrameBitmap, frameToCropTransform, null);
        // For examining the actual TF input.
        if (SAVE_PREVIEW_BITMAP) {
            ImageUtils.saveBitmap(croppedBitmap);
        }

        InputImage image = InputImage.fromBitmap(croppedBitmap, 0);

        faceDetector
                .process(image)
                .addOnSuccessListener(faces -> {
                    if (faces.size() == 0) {
                        updateResults(currTimestamp, new LinkedList<>());
                        return;
                    }
                    runInBackground(
                            () -> {
                                onFacesDetected(currTimestamp, faces, false);
                                addPending = false;
                            });
                });


    }

    @Override
    protected int getLayoutId() {
        return R.layout.tfe_od_camera_connection_fragment_tracking;
    }

    @Override
    protected Size getDesiredPreviewFrameSize() {
        return DESIRED_PREVIEW_SIZE;
    }

    // Which detection model to use: by default uses Tensorflow Object Detection API frozen
    // checkpoints.
    private enum DetectorMode {
        TF_OD_API;
    }

    @Override
    protected void setUseNNAPI(final boolean isChecked) {
        runInBackground(() -> detector.setUseNNAPI(isChecked));
    }

    @Override
    protected void setNumThreads(final int numThreads) {
        runInBackground(() -> detector.setNumThreads(50));
    }


    // Face Processing
    private Matrix createTransform(
            final int srcWidth,
            final int srcHeight,
            final int dstWidth,
            final int dstHeight,
            final int applyRotation) {

        Matrix matrix = new Matrix();
        if (applyRotation != 0) {
            if (applyRotation % 90 != 0) {
                LOGGER.w("Rotation of %d % 90 != 0", applyRotation);
            }

            // Translate so center of image is at origin.
            matrix.postTranslate(-srcWidth / 2.0f, -srcHeight / 2.0f);

            // Rotate around origin.
            matrix.postRotate(applyRotation);
        }

//        // Account for the already applied rotation, if any, and then determine how
//        // much scaling is needed for each axis.
//        final boolean transpose = (Math.abs(applyRotation) + 90) % 180 == 0;
//        final int inWidth = transpose ? srcHeight : srcWidth;
//        final int inHeight = transpose ? srcWidth : srcHeight;

        if (applyRotation != 0) {

            // Translate back from origin centered reference to destination frame.
            matrix.postTranslate(dstWidth / 2.0f, dstHeight / 2.0f);
        }

        return matrix;

    }

//    private void showAddFaceDialog(SimilarityClassifier.Recognition rec) {
//
//        AlertDialog.Builder builder = new AlertDialog.Builder(this);
//        LayoutInflater inflater = getLayoutInflater();
//        View dialogLayout = inflater.inflate(R.layout.image_edit_dialog, null);
//        ImageView ivFace = dialogLayout.findViewById(R.id.dlg_image);
//        TextView tvTitle = dialogLayout.findViewById(R.id.dlg_title);
//        EditText etName = dialogLayout.findViewById(R.id.dlg_input);
//
//        tvTitle.setText("Add Face");
//        ivFace.setImageBitmap(rec.getCrop());
//        etName.setHint("Input name");
//
//        builder.setPositiveButton("OK", (dlg, i) -> {
//
//            String name = etName.getText().toString();
//            if (name.isEmpty()) {
//                return;
//            }
//            detector.register(name, rec);
//            //knownFaces.put(name, rec);
//            dlg.dismiss();
//        });
//        builder.setView(dialogLayout);
//        builder.show();
//
//    }

    private void updateResults(long currTimestamp, final List<SimilarityClassifier.Recognition> mappedRecognitions) {

        tracker.trackResults(mappedRecognitions, currTimestamp);
        trackingOverlay.postInvalidate();
        computingDetection = false;
        //adding = false;


//        if (mappedRecognitions.size() > 0) {
//            LOGGER.i("Adding results");
//            SimilarityClassifier.Recognition rec = mappedRecognitions.get(0);
//            if (rec.getExtra() != null) {
//                showAddFaceDialog(rec);
//            }
//
//        }

//        runOnUiThread(
//                () -> {
//                    showFrameInfo(previewWidth + "x" + previewHeight);
//                    showCropInfo(croppedBitmap.getWidth() + "x" + croppedBitmap.getHeight());
//                    showInference(lastProcessingTimeMs + "ms");
//                });

    }

    private void onFacesDetected(long currTimestamp, List<Face> faces, boolean add) {

        cropCopyBitmap = Bitmap.createBitmap(croppedBitmap);
        final Canvas canvas = new Canvas(cropCopyBitmap);
        final Paint paint = new Paint();
        paint.setColor(Color.RED);
        paint.setStyle(Style.STROKE);
        paint.setStrokeWidth(2.0f);

        float minimumConfidence = MINIMUM_CONFIDENCE_TF_OD_API;
        switch (MODE) {
            case TF_OD_API:
                minimumConfidence = MINIMUM_CONFIDENCE_TF_OD_API;
                break;
        }

        final List<SimilarityClassifier.Recognition> mappedRecognitions =
                new LinkedList<SimilarityClassifier.Recognition>();


        //final List<Classifier.Recognition> results = new ArrayList<>();

        // Note this can be done only once
        int sourceW = rgbFrameBitmap.getWidth();
        int sourceH = rgbFrameBitmap.getHeight();
        int targetW = portraitBmp.getWidth();
        int targetH = portraitBmp.getHeight();
        Matrix transform = createTransform(
                sourceW,
                sourceH,
                targetW,
                targetH,
                sensorOrientation);
        final Canvas cv = new Canvas(portraitBmp);

        // draws the original image in portrait mode.
        cv.drawBitmap(rgbFrameBitmap, transform, null);

        final Canvas cvFace = new Canvas(faceBmp);

        boolean saved = false;

        for (Face face : faces) {

            LOGGER.i("FACE" + face.toString());
            LOGGER.i("Running detection on face " + currTimestamp);
            //results = detector.recognizeImage(croppedBitmap);

            final RectF boundingBox = new RectF(face.getBoundingBox());

            //final boolean goodConfidence = result.getConfidence() >= minimumConfidence;
            final boolean goodConfidence = true; //face.get;
            if (boundingBox != null && goodConfidence) {

                // maps crop coordinates to original
                cropToFrameTransform.mapRect(boundingBox);

                // maps original coordinates to portrait coordinates
                RectF faceBB = new RectF(boundingBox);
                transform.mapRect(faceBB);

                // translates portrait to origin and scales to fit input inference size
                //cv.drawRect(faceBB, paint);
                float sx = ((float) TF_OD_API_INPUT_SIZE) / faceBB.width();
                float sy = ((float) TF_OD_API_INPUT_SIZE) / faceBB.height();
                Matrix matrix = new Matrix();
                matrix.postTranslate(-faceBB.left, -faceBB.top);
                matrix.postScale(sx, sy);

                cvFace.drawBitmap(portraitBmp, matrix, null);

                //canvas.drawRect(faceBB, paint);

                String label = "";
                float confidence = -1f;
                Integer color = Color.BLUE;
                Object extra = null;
                Bitmap crop = null;

//                if (add) {
//                    crop = Bitmap.createBitmap(portraitBmp,
//                            (int) faceBB.left,
//                            (int) faceBB.top,
//                            (int) faceBB.width(),
//                            (int) faceBB.height());
//                }
                final long startTime = SystemClock.uptimeMillis();
                final List<SimilarityClassifier.Recognition> resultsAux = detector.recognizeImage(faceBmp, false);
                lastProcessingTimeMs = SystemClock.uptimeMillis() - startTime;

                if (resultsAux.size() > 0) {

                    SimilarityClassifier.Recognition result = resultsAux.get(0);

                    extra = result.getExtra();
//          Object extra = result.getExtra();
//          if (extra != null) {
//            LOGGER.i("embeeding retrieved " + extra.toString());
//          }

                    float conf = result.getDistance();
                    if (conf < 1.0f) {

                        confidence = conf;
                        label = result.getTitle();
                        if (result.getId().equals("0")) {
                            color = Color.GREEN;
                            Intent data = new Intent();
                            data.putExtra("result", 1);
                            setResult(RESULT_OK, data);
//                                File file = new File(path);
//                                if (file.exists()) file.delete();
                            if (!muti)
                                finish();
                        } else {
                            color = Color.RED;
                        }
                    }
                }

                if (getCameraFacing() == CameraCharacteristics.LENS_FACING_FRONT) {

                    // camera is frontal so the image is flipped horizontally
                    // flips horizontally
                    Matrix flip = new Matrix();
                    if (sensorOrientation == 90 || sensorOrientation == 270) {
                        flip.postScale(1, -1, previewWidth / 2.0f, previewHeight / 2.0f);
                    } else {
                        flip.postScale(-1, 1, previewWidth / 2.0f, previewHeight / 2.0f);
                    }
                    //flip.postScale(1, -1, targetW / 2.0f, targetH / 2.0f);
                    flip.mapRect(boundingBox);

                }

                final SimilarityClassifier.Recognition result = new SimilarityClassifier.Recognition(
                        "0", label, confidence, boundingBox);

                result.setColor(color);
                result.setLocation(boundingBox);
                result.setExtra(extra);
                result.setCrop(crop);
                mappedRecognitions.add(result);

            }


        }

        //    if (saved) {
//      lastSaved = System.currentTimeMillis();
//    }

        updateResults(currTimestamp, mappedRecognitions);


    }

    private void onHandle(List<Face> faces, String name, CallBackImport callBackImport) {
//        faceBmp1 = Bitmap.createBitmap(TF_OD_API_INPUT_SIZE, TF_OD_API_INPUT_SIZE, Config.ARGB_8888);
//        cropCopyBitmap1 = Bitmap.createBitmap(croppedBitmap1);
//        final Canvas canvas = new Canvas(cropCopyBitmap1);
//        final Paint paint = new Paint();
//        paint.setColor(Color.RED);
//        paint.setStyle(Style.STROKE);
//        paint.setStrokeWidth(2.0f);
//
//        float minimumConfidence = MINIMUM_CONFIDENCE_TF_OD_API;
//        switch (MODE) {
//            case TF_OD_API:
//                minimumConfidence = MINIMUM_CONFIDENCE_TF_OD_API;
//                break;
//        }

        final List<SimilarityClassifier.Recognition> mappedRecognitions =
                new LinkedList<SimilarityClassifier.Recognition>();

//
//        //final List<Classifier.Recognition> results = new ArrayList<>();
//
//        // Note this can be done only once
        int sourceW = rgbFrameBitmapImport.getWidth();
        int sourceH = rgbFrameBitmapImport.getHeight();
        int targetW = portraitBmpImport.getWidth();
        int targetH = portraitBmpImport.getHeight();
        Matrix transform = createTransform(
                sourceW,
                sourceH,
                targetW,
                targetH,
                0);
        final Canvas cv = new Canvas(portraitBmpImport);
//
//        // draws the original image in portrait mode.
        cv.drawBitmap(rgbFrameBitmapImport, transform, null);
//
        final Canvas cvFace = new Canvas(faceBmpImport);
//
//        boolean saved = false;

        for (Face face : faces) {

            LOGGER.i("FACE" + face.toString());
//            LOGGER.i("Running detection on face " + currTimestamp);
//            results = detector.recognizeImage(croppedBitmap);

            final RectF boundingBox = new RectF(face.getBoundingBox());

            //final boolean goodConfidence = result.getConfidence() >= minimumConfidence;
            final boolean goodConfidence = true; //face.get;
            if (boundingBox != null && goodConfidence) {

                // maps crop coordinates to original
//                cropToFrameTransform1.mapRect(boundingBox);

                // maps original coordinates to portrait coordinates
                RectF faceBB = new RectF(boundingBox);
                transform.mapRect(faceBB);

                // translates portrait to origin and scales to fit input inference size
//                cv.drawRect(faceBB, paint);
                float sx = ((float) TF_OD_API_INPUT_SIZE) / faceBB.width();
                float sy = ((float) TF_OD_API_INPUT_SIZE) / faceBB.height();
                Matrix matrix = new Matrix();
                matrix.postTranslate(-faceBB.left, -faceBB.top);
                matrix.postScale(sx, sy);

                cvFace.drawBitmap(portraitBmpImport, matrix, null);

//                canvas.drawRect(faceBB, paint);

                String label = name;
                float confidence = -1f;
                Integer color = Color.GREEN;
                Object extra = null;

//                if (add) {
                crop = Bitmap.createBitmap(portraitBmpImport,
                        (int) faceBB.left,
                        (int) faceBB.top,
                        (int) faceBB.width(),
                        (int) faceBB.height());
//                crop = Bitmap.createScaledBitmap(crop, TF_OD_API_INPUT_SIZE, TF_OD_API_INPUT_SIZE, true);
////                }

                final long startTime = SystemClock.uptimeMillis();
                final List<SimilarityClassifier.Recognition> resultsAux = detector.recognizeImage(faceBmpImport, true);
                lastProcessingTimeMs = SystemClock.uptimeMillis() - startTime;

                if (resultsAux.size() > 0) {

                    SimilarityClassifier.Recognition result = resultsAux.get(0);
//
                    extra = result.getExtra();
//          Object extra = result.getExtra();
//          if (extra != null) {
//            LOGGER.i("embeeding retrieved " + extra.toString());
                }

//                    float conf = result.getDistance();
//                    if (conf < 1.0f) {
//
//                        confidence = conf;
//                        label = result.getTitle();
//                        if (result.getId().equals("0")) {
//                            color = Color.GREEN;
//                        }
//                        else {
//                            color = Color.RED;
//                        }
//                    }

//                }

                if (getCameraFacing() == CameraCharacteristics.LENS_FACING_FRONT) {

//                     camera is frontal so the image is flipped horizontally
////                     flips horizontally
                    Matrix flip = new Matrix();
                    if (sensorOrientation == 90 || sensorOrientation == 270) {
                        flip.postScale(1, -1, previewWidth / 2.0f, previewHeight / 2.0f);
                    } else {
                        flip.postScale(-1, 1, previewWidth / 2.0f, previewHeight / 2.0f);
                    }
                    flip.postScale(1, -1, targetW / 2.0f, targetH / 2.0f);
                    flip.mapRect(boundingBox);

                }
                for (int i = 0; i < 4; i++) {
                    SimilarityClassifier.Recognition result = new SimilarityClassifier.Recognition(
                            "0", label, confidence, boundingBox);
                    result.setColor(color);
                    result.setLocation(boundingBox);
                    result.setExtra(extra);
                    result.setCrop(crop);
                    mappedRecognitions.add(result);
                    crop = ImageRotator.rotateImageOrientation(crop, 90 * (i + 1));
                }

            }


        }

        //    if (saved) {
//      lastSaved = System.currentTimeMillis();
//    }

//        updateResults(currTimestamp, mappedRecognitions);
        if (mappedRecognitions.size() > 0) {
            LOGGER.i("Adding results");
//            SimilarityClassifier.Recognition rec = mappedRecognitions.get(0);
            for (int i = 0; i < mappedRecognitions.size(); i++) {
                SimilarityClassifier.Recognition rec = mappedRecognitions.get(i);
                detector.register(name, rec);
            }
            callBackImport.done();
//            if (rec.getExtra() != null) {
//            Bitmap r = rec.getCrop();

//                showAddFaceDialog(rec);
//            }

        }

    }

    private void onImportImage(String path, String name, Bitmap bmp, int count, CallBackImport callBackImport) {
        final int[] _count = {count};
        InputImage image = InputImage.fromBitmap(bmp, 0);
        int targetW = bmp.getWidth();
        int targetH = bmp.getHeight();
        int cropW = (int) (targetW / 2.0);
        int cropH = (int) (targetH / 2.0);

        rgbFrameBitmapImport = bmp;
        croppedBitmapImport = Bitmap.createBitmap(cropW, cropH, Config.ARGB_8888);
        portraitBmpImport = Bitmap.createBitmap(targetW, targetH, Config.ARGB_8888);
        faceBmpImport = Bitmap.createBitmap(TF_OD_API_INPUT_SIZE, TF_OD_API_INPUT_SIZE, Config.ARGB_8888);

        faceDetector
                .process(image)
                .addOnSuccessListener(faces -> {
                    if (faces.size() == 0) {
                        try {
                            _count[0] += 1;
                            if (_count[0] > 3) {
                                if (!muti) {
                                    Intent data = new Intent();
                                    data.putExtra("result", -1);
                                    setResult(RESULT_OK, data);
                                    finish();
                                }
                                return;
                            }
                            onImportImage(path, name, ImageRotator.rotateImage(bmp, path), _count[0], callBackImport);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    runInBackground(
                            () -> {
                                onHandle(faces, name, new CallBackImport() {
                                    @Override
                                    public void done() {
                                        callBackImport.done();
                                    }
                                });
                                addPending = false;
                            });
                });
    }

}

class ImageRotator {
    public static Bitmap rotateImage(String path) throws IOException {
        Bitmap bitmap = BitmapFactory.decodeFile(path);
        return rotateImage(bitmap, path);
    }

    public static Bitmap rotateImage(Bitmap bitmap, String path) throws IOException {
        int rotate = 0;
        ExifInterface exif;
        exif = new ExifInterface(path);
        int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL);
        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_270:
                rotate = 270;
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                rotate = 180;
                break;
            case ExifInterface.ORIENTATION_ROTATE_90:
                rotate = 90;
                break;
        }
        Matrix matrix = new Matrix();
        matrix.postRotate(rotate);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(),
                bitmap.getHeight(), matrix, true);
    }

    public static Bitmap rotateImageOrientation(Bitmap bitmap, int rotate) {
        Matrix matrix = new Matrix();
        matrix.postRotate(rotate);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(),
                bitmap.getHeight(), matrix, true);
    }
}