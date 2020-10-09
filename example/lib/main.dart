import 'dart:io';
import 'dart:typed_data';
import 'package:flutter/material.dart';
import 'dart:async';

import 'package:flutter/services.dart';
import 'package:image_picker/image_picker.dart';
import 'package:safecert_face_recognition/safecert_face_recognition.dart';

void main() {
  runApp(MaterialApp(home: new MyApp()));
}

class MyApp extends StatefulWidget {
  @override
  _MyAppState createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  String _platformVersion = 'Unknown';
  RecognitionStatus recognitionStatus;
  TextEditingController controller = TextEditingController();

  @override
  void initState() {
    super.initState();
  }

  List<String> listPath = List();
  List<String> listName = List();
  File _image;
  File _image1;
  File _image2;
  final picker = ImagePicker();

  Future<String> getImage(int type) async {
    final pickedFile = await picker.getImage(
        source: type == 0 ? ImageSource.camera : ImageSource.gallery);
    return pickedFile.path;
    // setState(() {
    //   if (pickedFile != null) {
    //     _image = File(pickedFile.path);
    //   } else {
    //     print('No image selected.');
    //   }
    // });
  }

  // Platform messages are asynchronous, so we initialize in an async method.
  Future<void> recognizeFromUrl() async {
    int platformVersion;
    // Platform messages may fail, so we use a try/catch PlatformException.
    try {
      recognitionStatus = await SafecertFaceRecognition.startWithImageUrl(
          url:
              "https://i1.sndcdn.com/artworks-000368577903-4933qc-t500x500.jpg",
          name: "Binz");
      print(platformVersion);
    } on PlatformException {
      platformVersion = 0;
    }
  }

  Future<void> recognize2Image() async {
    int platformVersion;
    // Platform messages may fail, so we use a try/catch PlatformException.
    try {
      Uint8List bytes = (await NetworkAssetBundle(Uri.parse(
                  "https://i1.sndcdn.com/artworks-000368577903-4933qc-t500x500.jpg"))
              .load(
                  "https://i1.sndcdn.com/artworks-000368577903-4933qc-t500x500.jpg"))
          .buffer
          .asUint8List();
      Uint8List bytes2 = (await NetworkAssetBundle(Uri.parse(
                  "https://vtv1.mediacdn.vn/thumb_w/650/2020/9/14/11-16000550120591280108448.jpg"))
              .load(
                  "https://vtv1.mediacdn.vn/thumb_w/650/2020/9/14/11-16000550120591280108448.jpg"))
          .buffer
          .asUint8List();
      recognitionStatus = await SafecertFaceRecognition.startWithImages(
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
      if (recognitionStatus == RecognitionStatus.OK)
        _platformVersion = "OK";
      else if (recognitionStatus == RecognitionStatus.FAIL)
        _platformVersion = "FAIL";
      else
        _platformVersion = "ERROR";
      // _platformVersion = platformVersion.toString();
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
        body: SingleChildScrollView(
          child: Center(
            child: Column(
              children: [
                // FlatButton(
                //   child: Text("Recognize from url"),
                //   onPressed: () {
                //     recognizeFromUrl();
                //   },
                // ),
                // SizedBox(
                //   height: 10,
                // ),
                // FlatButton(
                //     child: Text("Recognize 2 Image"),
                //     onPressed: () {
                //       recognize2Image();
                //     }),
                // SizedBox(
                //   height: 50,
                // ),
                // Text(_platformVersion.toString())
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceEvenly,
                  children: [
                    FlatButton(
                      color: Colors.blue,
                      child: Text("Select local image"),
                      onPressed: () async {
                        String uri = await getImage(1);
                        setState(() {
                          _image = File(uri);
                        });
                      },
                    ),
                    FlatButton(
                      color: Colors.blue,
                      child: Text("Open Camera"),
                      onPressed: () async {
                        String uri = await getImage(0);
                        setState(() {
                          _image = File(uri);
                        });
                      },
                    ),
                  ],
                ),
                if (_image != null)
                  Container(
                    width: 300,
                    height: 300,
                    alignment: Alignment.center,
                    child: Image.file(_image),
                  ),
                if (_image != null) Text("input name"),
                if (_image != null)
                  Container(
                    width: 300,
                    height: 50,
                    child: TextField(
                      controller: controller,
                      decoration: new InputDecoration(
                        focusedBorder: OutlineInputBorder(
                          borderSide:
                              BorderSide(color: Colors.greenAccent, width: 5.0),
                        ),
                        enabledBorder: OutlineInputBorder(
                          borderSide: BorderSide(color: Colors.red, width: 5.0),
                        ),
                        hintText: 'input name here',
                      ),
                    ),
                  ),
                SizedBox(
                  height: 5,
                ),
                  Column(
                    mainAxisAlignment: MainAxisAlignment.spaceEvenly,
                    children: [
                      FlatButton(
                        color: Colors.blue,
                        child: Text("Add"),
                        onPressed: () {
                          listPath.add(_image.path);
                          String name = "${controller.text}";
                          listName.add(name);
                          controller.clear();
                        },
                      ),
                      FlatButton(
                        color: Colors.green,
                        child: Text("Start"),
                        onPressed: () async {
                          // image.Image image_temp = image.decodeImage(_image.readAsBytesSync());
                          // image.Image resized_img = image.copyResize(image_temp,width: 120,height: 120);
                          // recognitionStatus = await SafecertFaceRecognition.startWithImageData(imageData: resized_img.getBytes(),name: controller.text);
                          // setState(() {
                          //   if (recognitionStatus == RecognitionStatus.OK)
                          //     _platformVersion = "OK";
                          //   else if (recognitionStatus == RecognitionStatus.FAIL)
                          //     _platformVersion = "FAIL";
                          //   else
                          //     _platformVersion = "ERROR";
                          //   // _platformVersion = platformVersion.toString();
                          // });

                          // image.Image iconImage =
                          //     image.Image.fromBytes(150, 150, _image.readAsBytesSync());
                          // ui.decodeImageFromList(image.encodePng(iconImage),
                          //     (ui.Image img) {
                          //   img.toByteData().then((value) => {
                          SafecertFaceRecognition.startWithImagePath(
                                  path: listPath, name: listName, isMuti: true)
                              .then((value2) => {
                                    listPath.clear(),
                                    listName.clear(),
                                    setState(() {
                                      if (value2 == RecognitionStatus.OK)
                                        _platformVersion = "OK";
                                      else if (value2 == RecognitionStatus.FAIL)
                                        _platformVersion = "FAIL";
                                      else
                                        _platformVersion = "ERROR";
                                    })
                                  });
                          //       });
                          // });
                        },
                      ),
                    ],
                  ),
                Text("_______________________"),
                Text(_platformVersion.toString()),
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceEvenly,
                  children: [
                    FlatButton(
                      color: Colors.blue,
                      child: Text("Select image 1"),
                      onPressed: () async {
                        String uri = await getImage(1);
                        setState(() {
                          _image1 = File(uri);
                        });
                      },
                    ),
                    FlatButton(
                      color: Colors.blue,
                      child: Text("Select image 2"),
                      onPressed: () async {
                        String uri = await getImage(1);
                        setState(() {
                          _image2 = File(uri);
                        });
                      },
                    ),
                  ],
                ),
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceEvenly,
                  children: [
                    if (_image1 != null)
                      Container(
                        width: 150,
                        height: 150,
                        alignment: Alignment.center,
                        child: Image.file(_image1),
                      ),
                    if (_image2 != null)
                      Container(
                        width: 150,
                        height: 150,
                        alignment: Alignment.center,
                        child: Image.file(_image2),
                      ),
                  ],
                ),
                FlatButton(
                  color: Colors.green,
                  child: Text("Start2"),
                  onPressed: () async {
                    recognitionStatus =
                        await SafecertFaceRecognition.startWithPaths(
                            pathFirst: _image1.path, pathSecond: _image2.path);
                    setState(() {
                      if (recognitionStatus == RecognitionStatus.OK)
                        _platformVersion = "OK";
                      else if (recognitionStatus == RecognitionStatus.FAIL)
                        _platformVersion = "FAIL";
                      else
                        _platformVersion = "ERROR";
                      // _platformVersion = platformVersion.toString();
                    });
                  },
                ),
                // Text("_________Muti-face__________"),
                // FlatButton(
                //   color: Colors.green,
                //   child: Text("Input data face"),
                //   onPressed: () async {
                //     String uri = await getImage(0);
                //       _imageMuti = File(uri);
                //
                //   },
                // ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}


