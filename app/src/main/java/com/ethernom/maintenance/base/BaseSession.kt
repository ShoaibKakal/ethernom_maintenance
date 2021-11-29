package com.ethernom.maintenance.base
import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences

@SuppressLint("CommitPrefEdits")
abstract class BaseSession(context: Context) {
    private val mShare: SharedPreferences
    private val mEditor: SharedPreferences.Editor
    protected abstract val preferenceName: String

    init {
        mShare = context.getSharedPreferences("ig_$preferenceName.xml", Context.MODE_PRIVATE)
        mEditor = mShare.edit()
    }

    fun save(key: String, value: Long) {
        mEditor.putLong(key, value)
        mEditor.commit()
    }

    fun save(key: String, value: String) {
        mEditor.putString(key, value)
        mEditor.commit()
    }

    fun save(key: String, value: Boolean) {
        mEditor.putBoolean(key, value)
        mEditor.commit()
    }

    fun save(key: String, value: Int) {
        mEditor.putInt(key, value)
        mEditor.commit()
    }

    operator fun get(key: String, def: String): String? {
        return mShare.getString(key, def)
    }

    fun `is`(key: String, def: Boolean): Boolean {
        return mShare.getBoolean(key, def)
    }

    operator fun get(key: String, def: Int): Int {
        return mShare.getInt(key, def)
    }

    operator fun get(key: String, def: Long): Long {
        return mShare.getLong(key, def)
    }

    fun clear() {
        mEditor.clear()
        mEditor.commit()
    }
}