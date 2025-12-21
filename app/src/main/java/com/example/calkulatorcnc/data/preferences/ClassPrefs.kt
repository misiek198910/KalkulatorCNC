package com.example.calkulatorcnc.data.preferences

import android.content.Context

class ClassPrefs {
    var context: Context? = null
    fun savePrefString(context: Context, filename: String?, content: String?) {
        this.context = context
        val preferences = context.getSharedPreferences("def_shared", Context.MODE_PRIVATE)
        val editor = preferences.edit()
        editor.putString(filename, content)
        editor.apply()
    }

    fun loadPrefString(context: Context, filename: String?): String {
        val sp = context.getSharedPreferences("def_shared", Context.MODE_PRIVATE)
        val content: String = sp.getString(filename, "")!!

        return content
    }

    fun savePrefInt(context: Context, filename: String?, content: Int) {
        this.context = context
        val preferences = context.getSharedPreferences("def_shared", Context.MODE_PRIVATE)
        val editor = preferences.edit()
        editor.putInt(filename, content)
        editor.apply()
    }

    fun savePrefFloat(context: Context, filename: String?, content: Float) {
        this.context = context
        val preferences = context.getSharedPreferences("def_shared", Context.MODE_PRIVATE)
        val editor = preferences.edit()
        editor.putFloat(filename, content)
        editor.apply()
    }

    fun loadPrefInt(context: Context, filename: String?): Int {
        val sp = context.getSharedPreferences("def_shared", Context.MODE_PRIVATE)
        val content = sp.getInt(filename, 0)

        return content
    }

    fun loadPrefFloat(context: Context, filename: String?): Float {
        val sp = context.getSharedPreferences("def_shared", Context.MODE_PRIVATE)
        val content = sp.getFloat(filename, 0f)

        return content
    }
}