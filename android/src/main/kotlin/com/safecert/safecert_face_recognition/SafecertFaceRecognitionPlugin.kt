package com.safecert.safecert_face_recognition

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.annotation.NonNull
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.PluginRegistry
import io.flutter.plugin.common.PluginRegistry.Registrar


/** SafecertFaceRecognitionPlugin */
public class SafecertFaceRecognitionPlugin : FlutterPlugin, MethodCallHandler, PluginRegistry.ActivityResultListener, ActivityAware {

    /// The MethodChannel that will the communication between Flutter and native Android
    ///
    /// This local reference serves to register the plugin with the Flutter Engine and unregister it
    /// when the Flutter Engine is detached from the Activity
    private lateinit var channel: MethodChannel
    private val REQUEST_CODE = 1994
    private var resultCode = 0
    private var rs: MethodChannel.Result? = null
    private var activity: Activity? = null
    private var context: Context? = null
    private var recognitionImageData: RecognitionImageData? = RecognitionImageData()

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean {
        if (requestCode == REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                this.resultCode = data?.getIntExtra("result", 0)!!
            } else this.resultCode = resultCode
            rs!!.success(this.resultCode)
            return true
        }
        return false
    }

    override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        channel = MethodChannel(flutterPluginBinding.getFlutterEngine().getDartExecutor(), "safecert_face_recognition")
        channel.setMethodCallHandler(this)
        context = flutterPluginBinding.applicationContext
        recognitionImageData?.init(context)
    }

    private fun recognizeCamera(name: String, image: ByteArray) {
        val intent = Intent(context, DetectorActivity::class.java)
        intent.putExtra("name", name)
        intent.putExtra("imageFirst", image)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK
        activity!!.startActivityForResult(intent, REQUEST_CODE)
    }

    // This static function is optional and equivalent to onAttachedToEngine. It supports the old
    // pre-Flutter-1.12 Android projects. You are encouraged to continue supporting
    // plugin registration via this function while apps migrate to use the new Android APIs
    // post-flutter-1.12 via https://flutter.dev/go/android-project-migration.
    //
    // It is encouraged to share logic between onAttachedToEngine and registerWith to keep
    // them functionally equivalent. Only one of onAttachedToEngine or registerWith will be called
    // depending on the user's project. onAttachedToEngine or registerWith must both be defined
    // in the same class.
    companion object {
        @JvmStatic
        fun registerWith(registrar: Registrar) {
            val channel = MethodChannel(registrar.messenger(), "safecert_face_recognition")
            channel.setMethodCallHandler(SafecertFaceRecognitionPlugin())
        }
    }

    override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: Result) {
        this.rs = result
        val imageFirst: ByteArray
        val imageSecond: ByteArray
        val name: String
        if (call.method == "handle_face_recognize") {
            imageFirst = call.argument<ByteArray>("imageFirst")!!
            name = call.argument<String>("name").toString()
            if (imageFirst != null) {
                recognizeCamera(name.toString(), imageFirst)
            } else result.success(0)
        } else if (call.method == "handle_face_recognize_two_data") {
            imageFirst = call.argument<ByteArray>("imageFirst")!!
            imageSecond = call.argument<ByteArray>("imageSecond")!!
            name = call.argument<String>("name").toString()
            if (imageFirst != null && imageSecond != null) {
//                recognize(name.toString(), imageFirst, imageSecond)
                recognitionImageData?.onRecognizeData(imageFirst, imageSecond, name.toString()) { r ->
                    result.success(r)
                }
            } else result.success(0)
        } else {
            result.notImplemented()
        }
    }

    override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
        channel.setMethodCallHandler(null)
    }


    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
        this.activity = binding.activity
        binding.addActivityResultListener(this)
    }

    override fun onDetachedFromActivityForConfigChanges() {
        TODO("Not yet implemented")
    }

    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
        TODO("Not yet implemented")
    }

    override fun onDetachedFromActivity() {
        TODO("Not yet implemented")
    }
}

