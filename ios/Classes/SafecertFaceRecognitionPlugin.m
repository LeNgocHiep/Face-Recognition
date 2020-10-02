#import "SafecertFaceRecognitionPlugin.h"
#if __has_include(<safecert_face_recognition/safecert_face_recognition-Swift.h>)
#import <safecert_face_recognition/safecert_face_recognition-Swift.h>
#else
// Support project import fallback if the generated compatibility header
// is not copied when this plugin is created as a library.
// https://forums.swift.org/t/swift-static-libraries-dont-copy-generated-objective-c-header/19816
#import "safecert_face_recognition-Swift.h"
#endif

@implementation SafecertFaceRecognitionPlugin
+ (void)registerWithRegistrar:(NSObject<FlutterPluginRegistrar>*)registrar {
  [SwiftSafecertFaceRecognitionPlugin registerWithRegistrar:registrar];
}
@end
