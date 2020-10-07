package com.safecert.safecert_face_recognition;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.RectF;
import android.util.Log;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;
import com.safecert.safecert_face_recognition.tflite.SimilarityClassifier;
import com.safecert.safecert_face_recognition.tflite.TFLiteObjectDetectionAPIModel;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static android.graphics.Color.GREEN;

interface CallBackFace {
    void completeRecognition(int result);
}

public class RecognitionImageData {

    private SimilarityClassifier similarityClassifier;
    private static final boolean TF_OD_API_IS_QUANTIZED = false;
    private static final int TF_OD_API_INPUT_SIZE = 112;
    private static final String TF_OD_API_MODEL_FILE = "mobile_face_net.tflite";
    private static final String TF_OD_API_LABELS_FILE = "file:///android_asset/labelmap.txt";
    //    private List<Face> listFace = new ArrayList<Face>();
    Face faceFirst, faceSecond;
    // Minimum detection confidence to track a detection.
    private static final float MINIMUM_CONFIDENCE_TF_OD_API = 0.5f;

    // Face detector
    private FaceDetector faceDetector;

    Object extra = null;

    public void init(Context context) {
        FaceDetectorOptions options =
                new FaceDetectorOptions.Builder()
                        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                        .setContourMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
                        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
                        .build();


        FaceDetector _detector = FaceDetection.getClient(options);
        faceDetector = _detector;

        try {
            similarityClassifier = TFLiteObjectDetectionAPIModel.create(
                    context.getAssets(),
                    TF_OD_API_MODEL_FILE,
                    TF_OD_API_LABELS_FILE,
                    TF_OD_API_INPUT_SIZE,
                    TF_OD_API_IS_QUANTIZED);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void onRecognizeData(byte[] first, byte[] second, String name, CallBackFace callBackFace) {
        Bitmap _first = BitmapFactory.decodeByteArray(first, 0, first.length);
        Bitmap _second = BitmapFactory.decodeByteArray(second, 0, second.length);

        InputImage inputImageFirst = InputImage.fromBitmap(_first, 0);
        InputImage inputImageSecond = InputImage.fromBitmap(_second, 0);

        faceDetector.process(inputImageFirst).addOnSuccessListener(facesFirst -> {
            if (facesFirst.size() == 1) {
                faceFirst = facesFirst.get(0);
                faceDetector.process(inputImageSecond).addOnSuccessListener(facesSecond -> {
                    if (facesSecond.size() == 1) {
                        faceSecond = facesSecond.get(0);
                        if (faceFirst == null || faceSecond == null)
                            return;
                        RectF boundingBoxFirst = new RectF(faceFirst.getBoundingBox());
                        RectF boundingBoxSecond = new RectF(faceSecond.getBoundingBox());
                        SimilarityClassifier.Recognition recognitionFirst = new SimilarityClassifier.Recognition("0", name, -1f, boundingBoxFirst);
                        Bitmap bitmapFaceFirst = Bitmap.createBitmap(_first,
                                (int) boundingBoxFirst.left,
                                (int) boundingBoxFirst.top,
                                (int) boundingBoxFirst.width(),
                                (int) boundingBoxFirst.height());
                        Bitmap newBitmap = Bitmap.createScaledBitmap(bitmapFaceFirst, TF_OD_API_INPUT_SIZE,
                                TF_OD_API_INPUT_SIZE, false);
                        final List<SimilarityClassifier.Recognition> resultsAux = similarityClassifier.recognizeImage(newBitmap, true);
                        if (resultsAux.size() > 0) {
                            SimilarityClassifier.Recognition result = resultsAux.get(0);
                            extra = result.getExtra();
                        }
                        recognitionFirst.setColor(GREEN);
                        recognitionFirst.setLocation(boundingBoxFirst);
                        recognitionFirst.setExtra(extra);
                        recognitionFirst.setCrop(bitmapFaceFirst);
                        similarityClassifier.register(name, recognitionFirst);
                        Bitmap bitmapFaceSecond = Bitmap.createBitmap(_second,
                                (int) boundingBoxSecond.left,
                                (int) boundingBoxSecond.top,
                                (int) boundingBoxSecond.width(),
                                (int) boundingBoxSecond.height());
                        Bitmap newBitmap2 = Bitmap.createScaledBitmap(bitmapFaceSecond, TF_OD_API_INPUT_SIZE,
                                TF_OD_API_INPUT_SIZE, false);
                        List<SimilarityClassifier.Recognition> recognitionList = similarityClassifier.recognizeImage(newBitmap2, true);
                        if (recognitionList.get(0).getDistance() < 1.0f) {
                            callBackFace.completeRecognition(1);
                        } else callBackFace.completeRecognition(0);
                    } else callBackFace.completeRecognition(-1);
                });
            } else callBackFace.completeRecognition(-1);
        });
    }
}
