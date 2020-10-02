
import 'dart:async';
import 'dart:typed_data';

import 'package:flutter/services.dart';

class SafecertFaceRecognition {
  static const MethodChannel _channel =
      const MethodChannel('safecert_face_recognition');

  // static Future<String> get platformVersion async {
  //   final String version = await _channel.invokeMethod('handle_face_recognize');
  //   return version;
  // }
  static const RECOGNITION_SUCCESS = 1;
  static const RECOGNITION_FAIL = 0;
  static const RECOGNITION_DATA_ERROR = -1;

  static Future<int> startWithImageData({Uint8List imageData, String name}) async {
    return await _channel.invokeMethod(
        'handle_face_recognize', {"image": imageData, "name": name});
  }

  static Future<int> startWithImageUrl({String url, String name}) async {
    Uint8List bytes = (await NetworkAssetBundle(Uri.parse(url)).load(url))
        .buffer
        .asUint8List();
    return await _channel
        .invokeMethod('handle_face_recognize', {"image": bytes, "name": name});
  }
}
