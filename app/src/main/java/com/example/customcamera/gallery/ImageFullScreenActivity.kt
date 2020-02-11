package com.example.customcamera.gallery

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.example.customcamera.R
import kotlinx.android.synthetic.main.activity_imagefullscreen.*
import java.text.SimpleDateFormat
import java.util.*

class ImageFullScreenActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_imagefullscreen)

        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        window.statusBarColor = ContextCompat.getColor(this,R.color.colorPrimary)

        val imageUri =  intent?.getStringExtra("imageUri")
        val imageName = intent?.getStringExtra("imageName")

        val toolbar = fullscreen_toolbar
        setSupportActionBar(toolbar)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        val date = getFormattedDate(imageName!!)
        supportActionBar?.title =  date.substringBefore(',')
        supportActionBar?.subtitle = date.substringAfter(',')

        Glide.with(this)
            .load(imageUri)
            .centerCrop()
            .into(full_screen_image_view)
    }

    private fun getFormattedDate(lastModified: String): String{
        val valid = lastModified.substringBefore('.').toLong()
        return SimpleDateFormat("MMMM d, hh:mm aaa").format(Date(valid))
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}