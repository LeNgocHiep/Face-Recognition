import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:safecert_face_recognition/safecert_face_recognition.dart';

void main() {
  const MethodChannel channel = MethodChannel('safecert_face_recognition');

  TestWidgetsFlutterBinding.ensureInitialized();

  setUp(() {
    channel.setMockMethodCallHandler((MethodCall methodCall) async {
      return '42';
    });
  });

  tearDown(() {
    channel.setMockMethodCallHandler(null);
  });

  test('getPlatformVersion', () async {
    expect(await SafecertFaceRecognition.platformVersion, '42');
  });
}
