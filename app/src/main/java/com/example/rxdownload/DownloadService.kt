package com.example.rxdownload

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Environment
import android.util.LongSparseArray
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.subjects.PublishSubject
import java.io.File

class DownloadService {

    private val DEFAULT_MIME_TYPE = "*/*"

    private var mContext: Context? = null
    private var mDownloadManager: DownloadManager? = null
    private val subjectMap: LongSparseArray<PublishSubject<String>> = LongSparseArray()

    constructor(context: Context?) {
        this.mContext = context
        val downloadStatusReceiver = DownloadStatusReceiver()
        val intentFilter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        mContext?.registerReceiver(downloadStatusReceiver, intentFilter)
    }

    private fun getDownloadManger(): DownloadManager? {
        if (mDownloadManager == null) {
            mDownloadManager =
                mContext?.applicationContext?.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager?
        }
        if (mDownloadManager == null) {
            throw RuntimeException("Can't get DownloadManager from system service")
        }
        return mDownloadManager
    }


    fun download(
        url: String?,
        fileName: String?,
        showCompletedNotification: Boolean
    ): Observable<String> {
        return download(createRequest(url, fileName, DEFAULT_MIME_TYPE, showCompletedNotification))
    }

    fun download(
        url: String?,
        fileName: String?,
        mimeType: String?,
        showCompletedNotification: Boolean
    ): Observable<String> {
        return download(createRequest(url, fileName, mimeType, showCompletedNotification))
    }


    private fun createRequest(
        url: String?,
        fileName: String?,
        mimeType: String?,
        showCompletedNotification: Boolean
    ): DownloadManager.Request {
        val request = DownloadManager.Request(Uri.parse(url))
        request.setDescription(fileName)
        request.setMimeType(mimeType)

        val destinationPath = Environment.DIRECTORY_DOWNLOADS
        val destinationFolder = File(mContext?.filesDir, destinationPath)
        createFolderIfNeeded(destinationFolder)
        removeDuplicateFileIfExist(destinationFolder, fileName)
        request.setNotificationVisibility(if (showCompletedNotification) DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED else DownloadManager.Request.VISIBILITY_VISIBLE)
        return request
    }

    private fun createFolderIfNeeded(folder: File) {
        if (!folder.exists() && !folder.mkdirs()) {
            throw java.lang.RuntimeException("Can't create directory")
        }
    }

    private fun removeDuplicateFileIfExist(folder: File, fileName: String?) {
        val file = File(folder, fileName)
        if (file.exists() && !file.delete()) {
            throw java.lang.RuntimeException("Can't delete file")
        }
    }

    private fun download(request: DownloadManager.Request): Observable<String> {
        val downloadId = getDownloadManger()?.enqueue(request)
        val publishSubject = PublishSubject.create<String>()
        downloadId?.let { subjectMap.put(it, publishSubject) }
        return publishSubject
    }

    private inner class DownloadStatusReceiver : BroadcastReceiver() {
        override fun onReceive(p0: Context?, p1: Intent?) {
            val id = p1?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, 0L)
            val publishSubject = id?.let { subjectMap.get(it) } ?: return
            val query = DownloadManager.Query()
            query.setFilterById(id)
            val downloadManager = getDownloadManger()
            val cursor = downloadManager?.query(query)

            if (cursor?.moveToFirst() == false) {
                cursor.close()
                downloadManager.remove(id)
                publishSubject.onError(IllegalAccessError("Cursor empty, this shouldn't happened"))
                subjectMap.remove(id)
                return
            }

            val statusIndex = cursor?.getColumnIndex(DownloadManager.COLUMN_STATUS)
            if (DownloadManager.STATUS_SUCCESSFUL != statusIndex?.let { cursor.getInt(it) }) {
                cursor?.close()
                downloadManager?.remove(id)
                publishSubject.onError(IllegalAccessError("Download Failed"))
                subjectMap.remove(id)
                return
            }

            val uriIndex = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)
            val downloadPackageUriString = cursor.getString(uriIndex)
            cursor.close()

            publishSubject.onNext(downloadPackageUriString)
            publishSubject.onComplete()
            subjectMap.remove(id)
        }

    }


}