import 'dart:async';
import 'dart:typed_data';

import 'package:flutter/cupertino.dart';
import 'package:flutter/services.dart';

enum RecognitionStatus { OK, FAIL, ERROR }

class SafecertFaceRecognition {
  static const MethodChannel _channel =
      const MethodChannel('safecert_face_recognition');

  // static Future<String> get platformVersion async {
  //   final String version = await _channel.invokeMethod('handle_face_recognize');
  //   return version;
  // }

  static Future<RecognitionStatus> startWithImageData(
      {@required Uint8List imageData, String name}) async {
    int resp = await _channel.invokeMethod(
        'handle_face_recognize', {"imageFirst": imageData, "name": name ?? ''});
    return _getResult(resp);
  }

  static Future<RecognitionStatus> startWithImageUrl(
      {@required String url, String name}) async {
    Uint8List bytes = (await NetworkAssetBundle(Uri.parse(url)).load(url))
        .buffer
        .asUint8List();
    int resp = await _channel.invokeMethod(
        'handle_face_recognize', {"imageFirst": bytes, "name": name ?? ''});
    return _getResult(resp);
  }

  static Future<RecognitionStatus> startWithImages(
      {@required Uint8List dataImageFirst,
      @required Uint8List dataImageSecond,
      String name}) async {
    int resp = await _channel.invokeMethod('handle_face_recognize_two_data', {
      "imageFirst": dataImageFirst,
      "imageSecond": dataImageSecond,
      "name": name ?? ""
    });
    return _getResult(resp);
  }

  static RecognitionStatus _getResult(int type) {
    RecognitionStatus recognitionStatus;
    switch (type) {
      case 0:
        recognitionStatus = RecognitionStatus.FAIL;
        break;
      case 1:
        recognitionStatus = RecognitionStatus.OK;
        break;
      case -1:
        recognitionStatus = RecognitionStatus.ERROR;
        break;
      default:
        recognitionStatus = RecognitionStatus.ERROR;
    }
    return recognitionStatus;
  }
}
