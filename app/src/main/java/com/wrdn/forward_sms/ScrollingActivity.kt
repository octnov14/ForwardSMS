package com.wrdn.forward_sms

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.Telephony
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.appbar.CollapsingToolbarLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import java.util.*
import kotlin.collections.ArrayList


class ScrollingActivity : AppCompatActivity() {

    //퍼미션 응답 처리 코드
    private val multiplePermissionsCode = 100

    //필요한 퍼미션 리스트
    //원하는 퍼미션을 이곳에 추가하면 된다.
    private val requiredPermissions = arrayOf(
        Manifest.permission.READ_SMS
    )


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scrolling)
        setSupportActionBar(findViewById(R.id.toolbar))
        findViewById<CollapsingToolbarLayout>(R.id.toolbar_layout).title = title

        checkPermissions()

        findViewById<FloatingActionButton>(R.id.fab).setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show()
        }



        haha(this)

    }

    private fun haha(context: Context) {
        val cursor = context.contentResolver.query(Telephony.Sms.CONTENT_URI, null, null, null, null)

        if (cursor != null && cursor.moveToFirst()) {
            do {
                val smsDate = cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Sms.DATE))
                val number = cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS))
                val body = cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Sms.BODY))

                System.out.printf("%s; %s; %s\n", Date(smsDate.toLong()), number, body.replace("\\s".toRegex()," "))

            } while (cursor.moveToNext())
        }

        cursor?.close()
    }

    private fun sample__getSmsConversation() {
        getSmsConversation(this, "15447200") { conversations ->
            conversations?.forEach { conversation ->
                println("\n\nNumber: ${conversation.number}")

                for (msg in conversation.message) {
                    println("num: ${msg.number}")
                    println("body: ${msg.body}")
                    println("date: ${msg.date}")
                    println("\n\n\n")
                }
            }
        }
    }

    class Conversation(val number: String, val message: List<Message>)
    class Message(val number: String, val body: String, val date: Date)

    fun getSmsConversation(context: Context, number: String? = null, completion: (conversations: List<Conversation>?) -> Unit) {
        val cursor = context.contentResolver.query(Telephony.Sms.CONTENT_URI, null, null, null, null)
//        val cursor = context.contentResolver.query(Uri.parse("content://sms/inbox"), null, null, null, null)

        val numbers = ArrayList<String>()
        val messages = ArrayList<Message>()
        var results = ArrayList<Conversation>()

        if (cursor != null && cursor.moveToFirst()) {
            do {
                val smsDate = cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Sms.DATE))
                val number = cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS))
                val body = cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Sms.BODY))

                numbers.add(number)
                messages.add(Message(number, body, Date(smsDate.toLong())))
            } while (cursor.moveToNext())
        }

        cursor?.close()

        numbers.forEach { number ->
            if (results.find { it.number == number } == null) {
                val msg = messages.filter { it.number == number }
                results.add(Conversation(number = number, message = msg))
            }
        }

        if (number != null) {
            results = results.filter { it.number == number } as ArrayList<Conversation>
        }

        completion(results)
    }


    private fun checkPermissions() {
        //거절되었거나 아직 수락하지 않은 권한(퍼미션)을 저장할 문자열 배열 리스트
        var rejectedPermissionList = ArrayList<String>()

        //필요한 퍼미션들을 하나씩 끄집어내서 현재 권한을 받았는지 체크
        for (permission in requiredPermissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                //만약 권한이 없다면 rejectedPermissionList에 추가
                rejectedPermissionList.add(permission)
            }
        }
        //거절된 퍼미션이 있다면...
        if (rejectedPermissionList.isNotEmpty()) {
            //권한 요청!
            val array = arrayOfNulls<String>(rejectedPermissionList.size)
            ActivityCompat.requestPermissions(this, rejectedPermissionList.toArray(array), multiplePermissionsCode)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>, grantResults: IntArray
    ) {
        when (requestCode) {
            multiplePermissionsCode -> {
                if (grantResults.isNotEmpty()) {
                    for ((i, permission) in permissions.withIndex()) {
                        if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                            //권한 획득 실패
                            Log.i("TAG", "The user has denied to $permission")
                            Log.i("TAG", "I can't work for you anymore then. ByeBye!")
                        }
                    }
                }
            }
        }
    }
/*
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_scrolling, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }
*/
}