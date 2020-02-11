package com.example.customcamera.model

 data class GalleryDataClass(var itemTitle : String, var imageList : ArrayList<GalleryImageDataClass> )

 data class GalleryImageDataClass(var fileName : String,var filePath: String,var isUploaded : Boolean, var isUploading : Boolean)