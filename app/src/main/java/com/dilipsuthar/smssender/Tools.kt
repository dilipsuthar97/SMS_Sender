package com.dilipsuthar.smssender

import android.app.Activity
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.os.Build
import android.view.View
import android.view.WindowManager
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.appcompat.widget.Toolbar
import com.google.android.material.snackbar.Snackbar

object Tools {

    fun setSystemBarColor(activity: Activity, @ColorRes color: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val window = activity.window
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            window.statusBarColor =activity.resources.getColor(color)
        }
    }

    fun showSnackBar(view: View ,msg: String, duration: Int) {
        Snackbar.make(view, msg, duration).show()
    }

    fun showSnackBar(view: View ,msg: String, duration: Int, color: Drawable) {
        val snackBar: Snackbar = Snackbar.make(view, msg, duration)
        val v = snackBar.view
        v.background = color
        snackBar.show()
    }

}