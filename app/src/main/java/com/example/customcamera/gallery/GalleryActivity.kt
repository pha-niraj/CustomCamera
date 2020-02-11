package com.example.customcamera.gallery

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.customcamera.model.GalleryDataClass
import com.example.customcamera.model.GalleryImageDataClass
import kotlinx.android.synthetic.main.activity_gallery.*
import java.io.File
import kotlin.collections.ArrayList
import com.example.customcamera.R
import com.example.customcamera.googledrive.DriveUploadHelper
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.services.drive.DriveScopes
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*

class GalleryActivity : AppCompatActivity() {

    companion object{
        private lateinit var galleryData : ArrayList<GalleryDataClass>
    }
    private lateinit var adapter: GalleryRecyclerViewAdapter

    private val localBroadcastReceiver = object : BroadcastReceiver(){
        override fun onReceive(p0: Context?, p1: Intent?) {
                adapter.notifyDataSetChanged()
            Log.d("Drive","onReceive")
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gallery)
        val toolbar = gallery_toolbar
        setSupportActionBar(toolbar)
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        window.statusBarColor = ContextCompat.getColor(this,R.color.colorPrimary)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        drive_sync_switch.isChecked = getSyncState()

        if (drive_sync_switch.isChecked){
            signIn()
        }

        drive_sync_switch.setOnCheckedChangeListener { p0, p1 ->
            if (p1){
                signIn()
            }else{
               DriveUploadHelper.stopSync(this)
            }
            saveSyncState(p1)
        }
    }

    private fun registerReceiver(){
        val intentFilter= IntentFilter()
        intentFilter.addAction("GALLERY_UPDATE")
        LocalBroadcastManager.getInstance(this).registerReceiver(localBroadcastReceiver,intentFilter)
        Log.d("Drive","Reciever Registered")
    }

    private fun signIn(){
        val account = GoogleSignIn.getLastSignedInAccount(this)
        if (account==null){
            signInToDrive()
        }else{
            DriveUploadHelper.createDriveService(account,this)
        }
    }

    override fun onStart() {
        super.onStart()
        try {
            buildGalleryData(DriveUploadHelper.getSortedFileList(this))

            gallery_recyclerview.layoutManager = LinearLayoutManager(this)
            adapter =  GalleryRecyclerViewAdapter(
                galleryData,
                this,
                getScreenWidth()
            )
            gallery_recyclerview.adapter = adapter

        }catch (e :Exception){}

    }

    private fun buildGalleryData( sortedFileList: List<File>) {
        galleryData = ArrayList()

        var imageArrayList = ArrayList<GalleryImageDataClass>()
        var hourCounter = SimpleDateFormat("hh").format(Date(sortedFileList.first().lastModified()))
        var syncCounter = true
        var lastFileUploaded = File("")

        if (DriveUploadHelper.getLastFileUploaded(this)==null){
            syncCounter=false
        }else{
            lastFileUploaded = File(DriveUploadHelper.getLastFileUploaded(this))
        }

        for (file in sortedFileList){
            val currentHour = SimpleDateFormat("hh").format(Date(file.lastModified()))
            if (hourCounter == currentHour){
                imageArrayList.add(GalleryImageDataClass(file.name,file.path.toString(),
                    isUploaded = syncCounter,
                    isUploading = false
                ))
            }else{
                galleryData.add(GalleryDataClass(getFormattedDate(imageArrayList.first().fileName),imageArrayList))
                hourCounter = currentHour
                imageArrayList =  ArrayList<GalleryImageDataClass>()
                imageArrayList.add(GalleryImageDataClass(file.name,file.path.toString(),
                    isUploaded = syncCounter,
                    isUploading = false))
            }

            if (file==lastFileUploaded){
                syncCounter = false
            }
        }

        galleryData.add(GalleryDataClass(getFormattedDate(imageArrayList.first().fileName),imageArrayList))
    }

    private fun getScreenWidth() : Int {
        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        return  displayMetrics.widthPixels
    }

    private fun getFormattedDate(lastModified: String): String{
        val valid = lastModified.substringBefore('.').toLong()
        return SimpleDateFormat("MMMM d, hh:mm aaa").format(Date(valid))
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }


    private fun signInToDrive(){
        val signInOption = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(DriveScopes.DRIVE_FILE))
            .build()

        val client = GoogleSignIn.getClient(this,signInOption)

        startActivityForResult(client.signInIntent,110)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode==110){
                handleSignInIntent(data)
            }
    }

    private fun handleSignInIntent(data: Intent?) {
        GoogleSignIn.getSignedInAccountFromIntent(data)
            .addOnSuccessListener {
                    Toast.makeText(this,"Signed in as ${it.displayName}",Toast.LENGTH_SHORT).show()
                    DriveUploadHelper.createDriveService(it,this)
            }.addOnFailureListener {
                    Toast.makeText(this,it.message,Toast.LENGTH_SHORT).show()
            }
    }

    private fun getSyncState() : Boolean{
       return getSharedPreferences("SyncState", Context.MODE_PRIVATE).getBoolean("syncstate",false)
    }

    private fun saveSyncState(state : Boolean){
        getSharedPreferences("SyncState", Context.MODE_PRIVATE).edit().putBoolean("syncstate",state).apply()
    }


    override fun onResume() {
        super.onResume()
        registerReceiver()
    }

    override fun onPause() {
        super.onPause()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(localBroadcastReceiver)
        Log.d("Drive","Reciever Un-registered")
    }
}