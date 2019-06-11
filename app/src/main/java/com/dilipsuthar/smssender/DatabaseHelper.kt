package com.dilipsuthar.smssender

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper constructor(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, 1) {

    companion object {
        const val COL_1 = "ID"
        const val COL_2 = "NUMBER"
        const val DATABASE_NAME = "com.dilipsuthar.smssender.db"
        const val TABLE_NAME = "Contact"
    }

    override fun onCreate(db: SQLiteDatabase?) {
        db?.execSQL("CREATE TABLE IF NOT EXISTS $TABLE_NAME ($COL_2 INTEGER)")
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
        onCreate(db)
    }

    public fun getData(): Cursor {
        val db = writableDatabase
        return db.rawQuery("SELECT * FROM $TABLE_NAME", null)
    }

    public fun deleteData() {
        val db = writableDatabase
        db.execSQL("DELETE FROM $TABLE_NAME")
    }

}