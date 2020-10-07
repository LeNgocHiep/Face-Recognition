import 'dart:typed_data';

import 'package:flutter/material.dart';
import 'dart:async';

import 'package:flutter/services.dart';
import 'package:safecert_face_recognition/safecert_face_recognition.dart';

void main() {
  runApp(MyApp());
}

class MyApp extends StatefulWidget {
  @override
  _MyAppState createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  String _platformVersion = 'Unknown';

  @override
  void initState() {
    super.initState();
  }

  // Platform messages are asynchronous, so we initialize in an async method.
  Future<void> recognizeFromUrl() async {
    int platformVersion;
    // Platform messages may fail, so we use a try/catch PlatformException.
    try {
      platformVersion = await SafecertFaceRecognition.startWithImageUrl(
          url: "https://i1.sndcdn.com/artworks-000368577903-4933qc-t500x500.jpg", name: "Le ngoc Hiep");
      print(platformVersion);
    } on PlatformException {
      platformVersion = 0;
    }
  }

  Future<void> recognize2Image() async {
    int platformVersion;
    // Platform messages may fail, so we use a try/catch PlatformException.
    try {
      Uint8List bytes = (await NetworkAssetBundle(
                  Uri.parse("https://i1.sndcdn.com/artworks-000368577903-4933qc-t500x500.jpg"))
              .load("https://i1.sndcdn.com/artworks-000368577903-4933qc-t500x500.jpg"))
          .buffer
          .asUint8List();
      Uint8List bytes2 = (await NetworkAssetBundle(
                  Uri.parse("https://vtv1.mediacdn.vn/thumb_w/650/2020/9/14/11-16000550120591280108448.jpg"))
              .load("https://vtv1.mediacdn.vn/thumb_w/650/2020/9/14/11-16000550120591280108448.jpg"))
          .buffer
          .asUint8List();
      platformVersion = await SafecertFaceRecognition.startWith2Image(
          dataImageFirst: bytes, dataImageSecond: bytes2, name: "Le ngoc Hiep");
      print(platformVersion);
    } on PlatformException {
      platformVersion = 0;
    }

    // If the widget was removed from the tree while the asynchronous platform
    // message was in flight, we want to discard the reply rather than calling
    // setState to update our non-existent appearance.
    if (!mounted) return;

    setState(() {
      _platformVersion = platformVersion.toString();
    });
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: const Text('Plugin example app'),
          actions: [
            IconButton(
              onPressed: () {
                recognizeFromUrl();
              },
              icon: Icon(Icons.print),
            )
          ],
        ),
        body: Center(
          child: Column(
            children: [
              FlatButton(
                child: Text("Recognize from url"),
                onPressed: () {
                  recognizeFromUrl();
                },
              ),
              SizedBox(
                height: 10,
              ),
              FlatButton(
                  child: Text("Recognize 2 Image"),
                  onPressed: () {
                    recognize2Image();
                  }),
              SizedBox(
                height: 50,
              ),
              Text(_platformVersion.toString())
            ],
          ),
        ),
      ),
    );
  }
}
