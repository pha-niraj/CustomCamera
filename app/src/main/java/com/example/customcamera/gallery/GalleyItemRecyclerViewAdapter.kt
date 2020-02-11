package com.example.customcamera.gallery

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.RecyclerView
import com.example.customcamera.model.GalleryImageDataClass
import kotlinx.android.synthetic.main.gallery_imageview.view.*
import com.bumptech.glide.Glide
import com.example.customcamera.R
import com.example.customcamera.googledrive.DriveUploadHelper

class GalleyItemRecyclerViewAdapter(
    val data: ArrayList<GalleryImageDataClass>,
    val context: Context,
    val width: Int
): RecyclerView.Adapter<GalleyItemRecyclerViewAdapter.ItemViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
      return ItemViewHolder(
          LayoutInflater.from(parent.context)

              .inflate(R.layout.gallery_imageview, parent, false)
      )
    }

    override fun getItemCount(): Int {
        return data.size
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        holder.imageView.layoutParams.height= width/4
        holder.imageView.layoutParams.width = width/4

        Glide.with(context)
            .load(data[position].filePath)
            .centerCrop()
            .into(holder.imageView)

        holder.imageView.setOnClickListener {
            val intent = Intent(context,ImageFullScreenActivity::class.java)
            intent.putExtra("imageUri",data[position].filePath)
            intent.putExtra("imageName",data[position].fileName)
            context.startActivity(intent)
        }

        if (data[position].isUploaded){
            holder.cloudImageView.visibility = View.VISIBLE
        }

        if (data[position].filePath==DriveUploadHelper.getLastFileUploaded(context)){
            holder.cloudImageView.visibility = View.VISIBLE
        }

        if (data[position].fileName==DriveUploadHelper.currentFileUpload){
            holder.progressBar.visibility = View.VISIBLE
        }
    }

    class ItemViewHolder(  view : View) : RecyclerView.ViewHolder(view)
    {
        val imageView: ImageView = view.image_view
        val cloudImageView : ImageView = view.cloud_image_view
        val progressBar : ProgressBar = view.upload_progress_bar
    }
}