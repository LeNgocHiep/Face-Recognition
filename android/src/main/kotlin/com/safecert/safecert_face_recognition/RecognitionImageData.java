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

interface CallBackListFace {
    void complete(List<Face> faceList, Bitmap bitmapFirst, Bitmap bitmapSecond);
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

    Context context;

    Object extra = null;

    public void init(Context context) {
        this.context = context;
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

    public void onRecognizePath(String first, String second, String name, CallBackFace callBackFace) {
        try {
            similarityClassifier.close();
            similarityClassifier = TFLiteObjectDetectionAPIModel.create(
                    context.getAssets(),
                    TF_OD_API_MODEL_FILE,
                    TF_OD_API_LABELS_FILE,
                    TF_OD_API_INPUT_SIZE,
                    TF_OD_API_IS_QUANTIZED);
        } catch (IOException e) {
            e.printStackTrace();
        }
        Bitmap _first = BitmapFactory.decodeFile(first);
        Bitmap _second = BitmapFactory.decodeFile(second);
        Bitmap _newFirst = reSizeBitmap(_first, 400, 800);
        if (_newFirst.getWidth() > _newFirst.getHeight())
            _newFirst = ImageRotator.rotateImageOrientation(_newFirst, -90);
        Bitmap _newSecond = reSizeBitmap(_second, 400, 800);
        if (_newSecond.getWidth() > _newSecond.getHeight())
            _newSecond = ImageRotator.rotateImageOrientation(_newSecond, -90);
        int count = 0;
        getFaceFromBitmapFirst(_newFirst, _newSecond, count, new CallBackListFace() {
            @Override
            public void complete(List<Face> faceList, Bitmap bitmapFirst, Bitmap bitmapSecond) {
                if (faceList.size() != 2) {
                    callBackFace.completeRecognition(-1);
                    return;
                }
                RectF boundingBoxFirst = new RectF(faceList.get(0).getBoundingBox());
                RectF boundingBoxSecond = new RectF(faceList.get(1).getBoundingBox());
                SimilarityClassifier.Recognition recognitionFirst = new SimilarityClassifier.Recognition("0", name, -1f, boundingBoxFirst);
                Bitmap bitmapFaceFirst = Bitmap.createBitmap(bitmapFirst,
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
                Bitmap bitmapFaceSecond = Bitmap.createBitmap(bitmapSecond,
                        (int) boundingBoxSecond.left,
                        (int) boundingBoxSecond.top,
                        (int) boundingBoxSecond.width(),
                        (int) boundingBoxSecond.height());
                Bitmap newBitmap2 = Bitmap.createScaledBitmap(bitmapFaceSecond, TF_OD_API_INPUT_SIZE,
                        TF_OD_API_INPUT_SIZE, false);
                for (int i = 0; i < 4; i++) {
                    List<SimilarityClassifier.Recognition> recognitionList = similarityClassifier.recognizeImage(newBitmap2, true);
                    if (recognitionList.get(0).getDistance() < 1.0f) {
                        callBackFace.completeRecognition(1);
                        return;
                    } else {
                        newBitmap2 = ImageRotator.rotateImageOrientation(newBitmap2, 90 * (1 + i));
                    }
                }
                callBackFace.completeRecognition(0);
            }
        });

    }


    void getFaceFromBitmapFirst(Bitmap bitmapFirst, Bitmap bitmapSecond, int count, CallBackListFace callBackListFace) {
        final int[] _count = {count};
        List<Face> faceList = new ArrayList<Face>();
        InputImage inputImage = InputImage.fromBitmap(bitmapFirst, 0);
        faceDetector.process(inputImage).addOnSuccessListener(faces -> {
            if (faces.size() == 1) {
                faceList.addAll(faces);
                getFaceFromBitmapSecond(faceList, bitmapFirst, bitmapSecond, 0, callBackListFace);

            } else {
                _count[0] += 1;
                if (_count[0] > 3)
                    return;
                Bitmap bitmap1 = ImageRotator.rotateImageOrientation(bitmapFirst, 90 * _count[0]);
                getFaceFromBitmapFirst(bitmap1, bitmapSecond, _count[0], callBackListFace);
            }
        });
    }

    void getFaceFromBitmapSecond(List<Face> faceList, Bitmap bitmapFirst, Bitmap bitmapSecond, int count, CallBackListFace callBackListFace) {
        final int[] _count = {count};
        List<Face> _faceList = faceList;
        InputImage inputImage = InputImage.fromBitmap(bitmapSecond, 0);
        faceDetector.process(inputImage).addOnSuccessListener(faces -> {
            if (faces.size() == 1) {
                _faceList.addAll(faces);
                callBackListFace.complete(_faceList, bitmapFirst, bitmapSecond);
                return;
            } else {
                _count[0] += 1;
                if (_count[0] > 3)
                    return;
                Bitmap bitmap1 = ImageRotator.rotateImageOrientation(bitmapSecond, 90 * _count[0]);
                getFaceFromBitmapSecond(faceList, bitmapFirst, bitmap1, _count[0], callBackListFace);
            }
        });
    }

    static Bitmap reSizeBitmap(Bitmap image, int maxWidth, int maxHeight) {
        if (maxHeight > 0 && maxWidth > 0) {
            int width = image.getWidth();
            int height = image.getHeight();
            float ratioBitmap = (float) width / (float) height;
            float ratioMax = (float) maxWidth / (float) maxHeight;

            int finalWidth = maxWidth;
            int finalHeight = maxHeight;
            if (ratioMax > ratioBitmap) {
                finalWidth = (int) ((float) maxHeight * ratioBitmap);
            } else {
                finalHeight = (int) ((float) maxWidth / ratioBitmap);
            }
            image = Bitmap.createScaledBitmap(image, finalWidth, finalHeight, true);
        }
        return image;
    }
}
