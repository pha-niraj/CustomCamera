package com.example.customcamera.googledrive

import android.app.Service
import android.app.job.JobParameters
import android.app.job.JobService
import android.content.Intent
import android.util.Log

class UploadImageService : JobService() {

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return Service.START_STICKY
    }


    override fun onStartJob(p0: JobParameters?): Boolean {
        Log.d("Drive","OnStartJob")
        Thread(
            Runnable {
                DriveUploadHelper.syncImagesWithDrive(this)
            }
        ).start()

        return true
    }

    override fun onStopJob(p0: JobParameters?): Boolean {
        Log.d("Drive","OnstopJob")
        return true
    }
}