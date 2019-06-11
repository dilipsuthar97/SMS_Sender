package com.dilipsuthar.smssender

import android.app.Activity
import android.app.Dialog
import android.app.PendingIntent
import android.app.ProgressDialog
import android.content.*
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.preference.PreferenceManager
import android.telephony.SmsManager
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.Window
import android.widget.CheckBox
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import butterknife.BindView
import butterknife.ButterKnife
import com.ajts.androidmads.library.ExcelToSQLite
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.obsez.android.lib.filechooser.ChooserDialog
import es.dmoral.toasty.Toasty

class MainActivity : AppCompatActivity() {

    companion object {
        const val RC_CODE = 101
        const val SHOW_INTRO = "SHOW_INTRO"
        const val TAG = "debug_MainActivity"
        const val SENT = "SMS_SENT"
        const val DELIVERED = "SMS_DELIVERED"
    }

    // Views
    @BindView(R.id.toolbar) lateinit var toolbar: Toolbar
    @BindView(R.id.btnFileChooser) lateinit var chooseFileBtn: MaterialButton
    @BindView(R.id.btnShowContact) lateinit var showContactBtn: MaterialButton
    @BindView(R.id.btnSendSMS) lateinit var sendSMS: FloatingActionButton
    @BindView(R.id.fieldMessage) lateinit var fieldMessage: TextInputEditText
    @BindView(R.id.rootView) lateinit var rootView: View

    // Others
    private lateinit var dbHelper: DatabaseHelper
    private lateinit var sharedPreferences: SharedPreferences
    //private lateinit var progressDialog: ProgressDialog
    private var contactList: ArrayList<String> = ArrayList()

    private lateinit var sentPI: PendingIntent
    private lateinit var deliveredPI: PendingIntent

    override fun onCreate(savedInstanceState: Bundle?) {
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        ButterKnife.bind(this)

        // Receiver listener for SMS
        sentPI = PendingIntent.getBroadcast(this, 0, Intent(SENT), 0)
        deliveredPI = PendingIntent.getBroadcast(this, 0, Intent(DELIVERED), 0)
        // --when the sms has been sent
        this.registerReceiver(object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (resultCode) {
                    Activity.RESULT_OK ->
                        Toasty.success(this@MainActivity, "SMS sent success!", Toasty.LENGTH_SHORT, true).show()
                    SmsManager.RESULT_ERROR_NO_SERVICE ->
                        Toasty.error(this@MainActivity, "No active network to send SMS.", Toasty.LENGTH_SHORT, true).show()
                    SmsManager.RESULT_ERROR_RADIO_OFF ->
                        Toasty.error(this@MainActivity, "SMS not sent!", Toasty.LENGTH_SHORT, true).show()
                }
            }
        }, IntentFilter(SENT))

        // --When SMS has been delivered
        this.registerReceiver(object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (resultCode) {
                    Activity.RESULT_OK ->
                        Toasty.success(this@MainActivity, "SMS delivered.", Toasty.LENGTH_SHORT, true).show()
                    Activity.RESULT_CANCELED ->
                        Toasty.error(this@MainActivity, "SMS not delivered.", Toasty.LENGTH_SHORT, true).show()
                }
            }
        }, IntentFilter(DELIVERED))

        Tools.setSystemBarColor(this, R.color.colorAccent)
        dbHelper = DatabaseHelper(this)

        // Request for permission
        ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.SEND_SMS,
            android.Manifest.permission.READ_EXTERNAL_STORAGE,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE), RC_CODE)

        if (!sharedPreferences.getBoolean(SHOW_INTRO, false))
            showIntroDialog()

        initToolbar()
        initComponent()

    }

    private fun initToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.title = resources.getString(R.string.app_name)
    }

    private fun initComponent() {
        // Action file chooser
        chooseFileBtn.setOnClickListener {
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                dbHelper.deleteData()

                // For import file from storage
                ChooserDialog(this)
                    .withChosenListener { path, file ->
                        Log.d(TAG, "file_chooser: opened")

                        Toast.makeText(this, path, Toast.LENGTH_SHORT).show()

                        // Convert file Excel 2 SQLite
                        val excelToSQLite = ExcelToSQLite(this, dbHelper.databaseName, false)
                        val progressContactImport = showProgressDialog("Please wait", "importing contacts...")
                        excelToSQLite.importFromFile(path, object : ExcelToSQLite.ImportListener {
                            override fun onStart() {
                                Log.d(TAG, "excel_to_sqlite: onStart(): called")
                                progressContactImport.show()
                            }

                            override fun onCompleted(dbName: String?) {
                                Log.d(TAG, "excel_to_sqlite: onCompleted(): called")
                                val cursor = dbHelper.getData()
                                contactList.clear()
                                while (cursor.moveToNext()) {
                                    contactList.add(cursor.getString(0))
                                }

                                progressContactImport.dismiss()
                                Tools.showSnackBar(rootView, "Contacts import successfully", Snackbar.LENGTH_SHORT, resources.getDrawable(R.drawable.drawable_snackbar_success))
                            }

                            override fun onError(e: Exception?) {
                                Log.d(TAG, "excel_to_sqlite: onError(): called")

                                progressContactImport.dismiss()
                                e?.printStackTrace()
                                Tools.showSnackBar(rootView, "Unable to import Contacts", Snackbar.LENGTH_SHORT, resources.getDrawable(R.drawable.drawable_snackbar_error))
                            }
                        })
                    }
                    .show()
            }
        }

        // Action show imported contacts
        showContactBtn.setOnClickListener {
            if (contactList.isEmpty())
                Tools.showSnackBar(it, "First import contact list", Snackbar.LENGTH_SHORT)
            else
                showContactListDialog()
        }

        // Action send sms
        sendSMS.setOnClickListener {
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED) {

                val message = fieldMessage.text.toString()
                val progressSendSMS = showProgressDialog("Sending SMS", "please wait...")
                if (message.isEmpty())
                    fieldMessage.error = "Please enter message"
                else if (contactList.isEmpty())
                    Snackbar.make(it, "First import contact list", Snackbar.LENGTH_SHORT).show()
                else {
                    progressSendSMS.show()
                    try {
                        val smsManager = SmsManager.getDefault()
                        for (number: String in contactList) {
                            smsManager.sendTextMessage(number, null, message, sentPI, deliveredPI)
                        }
                        progressSendSMS.dismiss()
                    } catch (e: Exception) {
                        Log.d(TAG, e.message)
                        e.printStackTrace()
                        progressSendSMS.dismiss()
                        Toasty.error(this@MainActivity, "SMS Failed to send, please try again!", Toasty.LENGTH_SHORT, true).show()
                    }
                }

            } else
                ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.SEND_SMS), RC_CODE)
        }
    }

    private fun showIntroDialog() {
        val dialog: Dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_intro)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.setCancelable(false)

        val checkBox = dialog.findViewById<CheckBox>(R.id.checkBoxDontShow)
        if (sharedPreferences.getBoolean(SHOW_INTRO, false))
            checkBox.isChecked = true
        checkBox.setOnCheckedChangeListener { buttonView, isChecked ->
            sharedPreferences.edit().putBoolean(SHOW_INTRO, isChecked).apply()
        }

        (dialog.findViewById<MaterialButton>(R.id.btnOk)).setOnClickListener {
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun showAboutDialog() {
        val dialog: Dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_about)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.setCancelable(true)

        (dialog.findViewById<MaterialButton>(R.id.btn_about_me)).setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://about.me/dilip.suthar")))
        }
        dialog.show()
    }

    private fun showProgressDialog(title: String, msg: String): ProgressDialog {
        val progressDialog = ProgressDialog(this)
        progressDialog.setTitle(title)
        progressDialog.setMessage(msg)
        progressDialog.setCancelable(false)
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER)

        return progressDialog
    }

    private fun showContactListDialog() {
        AlertDialog.Builder(this)
            .setTitle("Imported contact list")
            .setCancelable(true)
            .setItems(contactList.toTypedArray(), null)
            .setNegativeButton("Cancel") {dialog, which ->

            }
            .show()

    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val menuInflater = menuInflater
        menuInflater.inflate(R.menu.menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (item?.itemId == R.id.action_intro)
            showIntroDialog()
        else if (item?.itemId == R.id.action_about)
            showAboutDialog()

        return super.onOptionsItemSelected(item)
    }

}
