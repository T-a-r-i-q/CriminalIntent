package com.bignerdranch.android.criminalintent

import android.app.Dialog
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.ImageView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.navigation.fragment.navArgs
import java.io.File

class ImageDetailFragment: DialogFragment() {
    private val args: ImageDetailFragmentArgs by navArgs()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_image, null)

        val imageView: ImageView = view.findViewById(R.id.dialog_image_view)

        val photoPath = args.photoPath
        val photoFile = File(photoPath)
        if (photoFile.exists()) {
            val bitmap = BitmapFactory.decodeFile(photoFile.path)
            imageView.setImageBitmap(bitmap)
        }

        return AlertDialog.Builder(requireContext())
            .setView(view)
            .setPositiveButton(android.R.string.ok, null)
            .create()
    }

    companion object {
        const val REQUEST_KEY_IMAGE = "REQUEST_KEY_IMAGE"
        const val BUNDLE_KEY_IMAGE = "BUNDLE_KEY_IMAGE"
    }
}