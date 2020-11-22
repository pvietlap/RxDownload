package com.example.rxdownload

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.widget.Toast
import androidx.core.app.ActivityCompat
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.schedulers.Schedulers

class MainActivity : AppCompatActivity() {
    private val REQUEST_CODE=225
    private val URL=""
    private val MIMETYPE="/pdf"
    private val FILENAME="RxDownloadTemp_"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        checkPermission()
    }

    private fun downloadFile(isShowNotification : Boolean){
        if (hasInternetConnection()){
            val rxDownload= DownloadService(applicationContext)
            if (!TextUtils.isEmpty(URL)){
                rxDownload.download(URL,"${FILENAME}${System.currentTimeMillis()}",MIMETYPE,isShowNotification)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe {
                        Toast.makeText(applicationContext,it,Toast.LENGTH_SHORT).show()
                    }
            }
        }
    }

    private fun checkPermission(){
        val permission= ActivityCompat.checkSelfPermission(applicationContext,android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
        if (permission!=PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, Array<String>(1){android.Manifest.permission.WRITE_EXTERNAL_STORAGE},REQUEST_CODE)
        }
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode==REQUEST_CODE){
            if (grantResults.isNotEmpty()&& grantResults[0]==PackageManager.PERMISSION_GRANTED){
                downloadFile(true)
            }else{
                Toast.makeText(applicationContext,"Permission denied to read your External storage",Toast.LENGTH_SHORT).show()
            }
        }
    }


    @SuppressLint("MissingPermission")
    fun hasInternetConnection(): Boolean {
        val connectivityManager =
            getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetworkInfo = connectivityManager.activeNetworkInfo
        return activeNetworkInfo != null && activeNetworkInfo.isConnected
    }

}