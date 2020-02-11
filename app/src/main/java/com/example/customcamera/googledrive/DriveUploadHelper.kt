package com.example.customcamera.googledrive

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.FileContent
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import java.io.File
import java.lang.Exception
import java.util.*
import java.util.concurrent.Executors
import java.util.Collections.singletonList
import java.util.concurrent.Callable
import android.app.job.JobScheduler
import android.content.Context.JOB_SCHEDULER_SERVICE
import android.app.job.JobInfo
import android.content.ComponentName
import android.content.Intent
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.gms.auth.api.signin.GoogleSignIn


object DriveUploadHelper {
    private val executor = Executors.newSingleThreadExecutor()
    private var googleDriveServices : Drive? = null
    var currentFileUpload = ""

    fun createDriveService(it: GoogleSignInAccount,context: Context) {
        val credential = GoogleAccountCredential.usingOAuth2(
            context,Collections.singleton(DriveScopes.DRIVE_FILE))

        credential.selectedAccount = it.account

         googleDriveServices = Drive.Builder(
            AndroidHttp.newCompatibleTransport(),
            GsonFactory(),
            credential).setApplicationName("Custom Camera").build()

        scheduleJob(context)
    }

    fun stopSync(context: Context){
        val scheduler = context.getSystemService(JOB_SCHEDULER_SERVICE) as JobScheduler?
        scheduler!!.cancel(110)
        Log.d("Drive", "Job cancelled")
    }

    private fun scheduleJob(context: Context) {
        val componentName = ComponentName(context, UploadImageService::class.java)
        val info = JobInfo.Builder(110, componentName)
            .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
            .setPersisted(true)
            .setPeriodic(15*60*1000)
            .build()

        val scheduler = context.getSystemService(JOB_SCHEDULER_SERVICE) as JobScheduler?
        val resultCode = scheduler!!.schedule(info)
        if (resultCode == JobScheduler.RESULT_SUCCESS) {
            Log.d("Drive", "Job scheduled")
        } else {
            Log.d("Drive", "Job scheduling failed")
        }
    }

    fun syncImagesWithDrive(context: Context) {
        if (googleDriveServices==null){
            val account = GoogleSignIn.getLastSignedInAccount(context)
            val credential = GoogleAccountCredential.
                usingOAuth2(
                context,Collections.singleton(DriveScopes.DRIVE_FILE))

            credential.selectedAccount = account?.account

            googleDriveServices = Drive.Builder(
                AndroidHttp.newCompatibleTransport(),
                GsonFactory(),
                credential).setApplicationName("Custom Camera").build()
        }

        val sortedFileList = getSortedFileList(context)
        createFolderInDrive(context, getDriveFolderId(context)).addOnSuccessListener {

            var lastFileIndex = 0

            if (getLastFileUploaded(context)!=null){
                val lastFileUploaded= File(getLastFileUploaded(context))
                 lastFileIndex = sortedFileList.indexOf(lastFileUploaded)
            }
            if (lastFileIndex==sortedFileList.lastIndex){
                Toast.makeText(context,"All images are in Sync",Toast.LENGTH_SHORT).show()
                stopSync(context)

            }else{
                if (getLastFileUploaded(context)!=null) {
                    lastFileIndex++
                }
                for (i in lastFileIndex..sortedFileList.lastIndex){
                    uploadImageFile(sortedFileList[i].path,it,context)
                }
            }
        }
    }

    private fun createFolderInDrive(context: Context, driveFolderId: String?): Task<String>{
        return  Tasks.call<String>(executor, Callable{
            if (driveFolderId==null){
                val driveFile = com.google.api.services.drive.model.File().apply {
                    name = "Custom Camera"
                    parents = singletonList("root")
                    mimeType = "application/vnd.google-apps.folder"
                }

                var result = com.google.api.services.drive.model.File()
                try {
                    result = googleDriveServices!!.files().create(driveFile).execute()
                    saveDriveFolderId(context,result.id)
                    Log.d("Drive","Drive Folder created")
                }catch (e : Exception){
                    Log.d("Drive",e.message)
                }
                result.id
            }else{
               driveFolderId
            }
        })
    }

    private fun uploadImageFile(
        filePath: String,
        driveFolderId: String,
        context: Context
    ): Task<String>? {
        return  Tasks.call<String>(executor, Callable{

            val file = File(filePath.substringAfter(':'))
            val mediaContent = FileContent("image/jpeg",file)
            currentFileUpload = file.name
            sendLocalBroadcast(context)

            val driveFile = com.google.api.services.drive.model.File().apply {
                name = file.name
                parents = singletonList(driveFolderId)
            }

            var result = com.google.api.services.drive.model.File()
            try {
                result = googleDriveServices!!.files().create(driveFile,mediaContent).execute()
                Log.d("Drive","${result.name} uploaded")
                saveLastFileUploaded(context,file.path)
                currentFileUpload = ""
               sendLocalBroadcast(context)

            }catch (e : Exception){
                Log.d("Drive:",e.message)
                currentFileUpload = ""
                sendLocalBroadcast(context)

            }
            result.id
        })
    }

    private fun sendLocalBroadcast(context: Context) {
        val intent  = Intent("GALLERY_UPDATE")
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
    }

    fun getLastFileUploaded(context: Context) : String?{
        val sharedPreferences = context.getSharedPreferences("DriveSync", Context.MODE_PRIVATE)
        return sharedPreferences.getString("FileName",null)
    }

    private fun saveLastFileUploaded(context: Context,filePath : String){

        if (getSortedFileList(context).last().path==filePath)
            stopSync(context)

        val sharedPreferences = context.getSharedPreferences("DriveSync",Context.MODE_PRIVATE).edit()
        sharedPreferences.putString("FileName",filePath)
        sharedPreferences.apply()
    }

    fun getSortedFileList(context: Context) : List<File> {

        val imageDir = File("/storage/emulated/0/Android/media/com.example.customcamera")

        val fileList = imageDir.listFiles()

        val sortedFileList = fileList.sortedWith(Comparator { d1, d2 ->
            d1.lastModified().compareTo(d2.lastModified())
        })
        return sortedFileList
    }

    private fun getDriveFolderId(context: Context): String?{
        val sharedPreferences = context.getSharedPreferences("DriveSync", Context.MODE_PRIVATE)
        return sharedPreferences.getString("DriveFolderId",null)
    }

    private fun saveDriveFolderId(context: Context,driveFolderId: String){
        val sharedPreferences = context.getSharedPreferences("DriveSync",Context.MODE_PRIVATE).edit()
        sharedPreferences.putString("DriveFolderId",driveFolderId)
        sharedPreferences.apply()
    }

}