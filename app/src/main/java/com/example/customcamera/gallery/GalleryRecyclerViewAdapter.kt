package com.example.customcamera.gallery

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.customcamera.R
import com.example.customcamera.model.GalleryDataClass
import kotlinx.android.synthetic.main.gallery_item.view.*

class GalleryRecyclerViewAdapter(var galleryData : ArrayList<GalleryDataClass>,val context : Context,val width : Int) : RecyclerView.Adapter<GalleryRecyclerViewAdapter.MainViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MainViewHolder {
        return  MainViewHolder(


            LayoutInflater.from(parent.context)
                .inflate(R.layout.gallery_item, parent, false))
    }

    override fun getItemCount(): Int {
       return galleryData.size
    }

    override fun onBindViewHolder(holder: MainViewHolder, position: Int) {
        holder.itemTitle.text = galleryData[position].itemTitle
        holder.itemRecyclerView.layoutManager = GridLayoutManager(context,4)
        holder.itemRecyclerView.adapter =
            GalleyItemRecyclerViewAdapter(
                galleryData[position].imageList,
                context,
                width
            )
    }


    inner class MainViewHolder(val view : View) : RecyclerView.ViewHolder(view){
        val itemTitle : TextView = view.galley_item_title
        val itemRecyclerView : RecyclerView = view.gallery_item_recyclerview
    }
}