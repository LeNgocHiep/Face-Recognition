package com.safecert.safecert_face_recognition

import android.app.Activity
import android.content.Context
import android.content.Intent
import io.flutter.embedding.android.FlutterActivity
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.PluginRegistry

class ActivityHelper(private var applicationContext: Context?,
                     var activity: Activity? = null) : PluginRegistry.ActivityResultListener{
    private val REQUEST_CODE = 15
    private var resultCode = 0
    private var rs: MethodChannel.Result? = null
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean {
        if (requestCode == REQUEST_CODE) {
            if (resultCode == FlutterActivity.RESULT_OK) {
                this.resultCode = data?.getIntExtra("result", 0)!!
            } else this.resultCode = resultCode
            rs!!.success(this.resultCode)
        }
        return  true
    }
    fun onLoginSuccess(name: String, image: ByteArray,rs: MethodChannel.Result) {
        this.rs = rs
        val intent = Intent(applicationContext, DetectorActivity::class.java)
        intent.putExtra("name", name)
        intent.putExtra("image", image)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK
        activity!!.startActivityForResult(intent, REQUEST_CODE)
    }
}