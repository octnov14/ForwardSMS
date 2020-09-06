package com.wrdn.forward_sms

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Telephony
import android.telephony.SmsManager
import android.util.Log
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.appbar.CollapsingToolbarLayout
import kotlinx.android.synthetic.main.activity_scrolling.*
import kotlinx.android.synthetic.main.content_scrolling.*
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.util.*
import kotlin.Comparator
import kotlin.collections.ArrayList


class ScrollingActivity : AppCompatActivity() {

    //퍼미션 응답 처리 코드
    private val multiplePermissionsCode = 100

    //필요한 퍼미션 리스트
    //원하는 퍼미션을 이곳에 추가하면 된다.
    private val requiredPermissions = arrayOf(
        Manifest.permission.READ_SMS
        , Manifest.permission.SEND_SMS
    )



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scrolling)
        setSupportActionBar(findViewById(R.id.toolbar))
        findViewById<CollapsingToolbarLayout>(R.id.toolbar_layout).title = title

        checkPermissions()

        setDefaultValue()


        fab.setOnClickListener {
            AlertDialog.Builder(this).run {
                setTitle("문자를 보낼까요?")
                setMessage("한번에 많은 문자가 보내지니 신중히 살펴보십시오~")

                setPositiveButton(
                    "보내기"
                ) { _, _ ->
                    send()
                }

                setNegativeButton(
                    "취소"
                ) { _, _ ->
                }

                show()
            }
            //Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG).setAction("Action", null).show()
        }


        btnQuery.setOnClickListener {
            setEditText(result, "조회 중입니다")

            rememberCondition()

            val smsList = readSMS(this, dateAfter.text.toString(), fromNumber.text.toString(), includingText.text.toString())
            val mmsList = readMMS(this, dateAfter.text.toString(), fromNumber.text.toString(), includingText.text.toString())

            var s = "보낼 내용을 검토하십시오\n\n화살표(-->)를 삭제하면 발송되지 않습니다\n\nSMS는 자동 발송되며\nMMS는 내용 확인 후 발송합니다\n\n\n[SMS 수신 내역]\n\n"
            for (x in smsList) {
                s += "${x}\n\n"
            }

            s += "\n\n[MMS 수신 내역]\n\n"
            for (x in mmsList) {
                s += "${x}\n\n"
            }

            setEditText(result, s)
        }

    }

    private fun rememberCondition() {
        val pref = getSharedPreferences("Config", Context.MODE_PRIVATE)

        pref.edit().let { conf ->
            conf.putString("dateAfter", dateAfter.text.toString())
            conf.putString("fromNumber", fromNumber.text.toString())
            conf.putString("includingText", includingText.text.toString())
            conf.putString("toNumber", toNumber.text.toString())

            conf.apply()
        }

    }

    private fun send() {
        //sendSMS("01023573773", "한글도 잘 되겠지? abc 123")

        val arr = result.text.toString().split("\n")

        var n = 1
        for (x in arr) {
            if (x.startsWith("--> SMS ")) {
                val y = x.replace("--> SMS ", "")

                println("${n++} SMS : $y")

                sendSMS(toNumber.text.toString(), y)

            } else if (x.startsWith("--> MMS ")) {
                val y = x.replace("--> MMS ", "")

                println("${n++} MMS : $y")

                sendMMS(toNumber.text.toString(), y)
            }
        }

    }

    private fun sortList(list: MutableList<String>): MutableList<String> {
        val com = Comparator { o1: String, o2: String ->
            return@Comparator if (o1 > o2) {
                -1
            } else {
                1
            }
        }

        return list.sortedWith(com).toMutableList()
    }

    private fun setEditText(ed: EditText, s: String) {
        ed.setText(s, TextView.BufferType.EDITABLE)
    }

    private fun setDefaultValue() {
        val pref = getSharedPreferences("Config", Context.MODE_PRIVATE)

        pref.getString("dateAfter", YCalendar.displayYYYYMMDD())?.let { setEditText(dateAfter, it) }
        pref.getString("fromNumber", "")?.let { setEditText(fromNumber, it) }
        pref.getString("includingText", "")?.let { setEditText(includingText, it) }
        pref.getString("toNumber", "")?.let { setEditText(toNumber, it) }
    }


    private fun readSMS(context: Context, dateAfter: String, num: String, inct: String): MutableList<String> {
        val rtn = ArrayList<String>()


        val uri = Telephony.Sms.Inbox.CONTENT_URI  // Uri.parse("content://sms/inbox")

        val cursor = context.contentResolver.query(uri, null, null, null, "date desc")

        if (cursor != null && cursor.moveToFirst()) {
            do {
                val smsDate = cursor.getString(cursor.getColumnIndexOrThrow("date"))
                val number = cursor.getString(cursor.getColumnIndexOrThrow("address"))
                val body = cursor.getString(cursor.getColumnIndexOrThrow("body")).replace("""\s""".toRegex(), " ")

                val yc = YCalendar(Date(smsDate.toLong()))
                if (yc.getYYYYMMDD() < dateAfter) break

                if (number.indexOf(num) >= 0 && body.indexOf(inct) >= 0) {
                    rtn.add("${yc}   (${number})\n--> SMS ${body}")
                }


            } while (cursor.moveToNext())
        }

        cursor?.close()

        return rtn
    }


    // 참조 URL
    // https://www.it-swarm.dev/ko/android/mms-android%EC%9D%98-%EB%8D%B0%EC%9D%B4%ED%84%B0%EB%A5%BC-%EC%9D%BD%EB%8A%94-%EB%B0%A9%EB%B2%95/969694767/
    private fun readMMS(context: Context, dateAfter: String, num: String, inct: String): MutableList<String> {
        val rtn = ArrayList<String>()


        val uri = Telephony.Mms.Inbox.CONTENT_URI

        val proj = arrayOf("*")
        val cursor = context.contentResolver.query(uri, proj, null, null, "date desc")

        if (cursor != null) {
            cursor.moveToFirst()

            do {
                // mms columns
                // date 에는 뒤에 000 을 붙여야 한다!!!!!!!!!!
                // 아이디, 수신번호만 의미있다
                // ct_t 는 컨텐트 타입이다
                // Available columns: [_id, thread_id, date, date_sent, msg_box, read, m_id, sub, sub_cs, ct_t, ct_l, exp,
                // m_cls, m_type, v, m_size, pri, rr, rpt_a, resp_st, st, tr_id, retr_st, retr_txt, retr_txt_cs,
                // read_status, ct_cls, resp_txt, d_tm, d_rpt, locked, seen, sub_id, phone_id, creator,
                // imsi_data, group_id, save_call_type, msg_boxtype, type, address, name, tag, tag_eng,
                // spam_report, reserve_time, insert_time, sender_num, textlink, text_only, c0_iei,
                // kt_tm_send_type, line_address, notification]

                val id = cursor.getString(cursor.getColumnIndexOrThrow("_id"))
                val date = cursor.getString(cursor.getColumnIndexOrThrow("date"))
                val number = cursor.getString(cursor.getColumnIndexOrThrow("address"))
                val body = getMMSBody(id).replace("""\s""".toRegex(), " ")

                val yc = YCalendar(Date("${date}000".toLong()))
                if (yc.getYYYYMMDD() < dateAfter) break


                if (number.indexOf(num) >= 0 && body.indexOf(inct) >= 0) {
                    rtn.add("${yc}   (${number})\n--> MMS ${body}")
                }


            } while (cursor.moveToNext())

            cursor.close()
        }

        return rtn
    }

    private fun sendSMS(phoneNo: String, sms: String) {
        try {
            var msg = sms.replace("[Web발신] ", "")
            if(msg.length > 70) msg = msg.substring(0, 70)

            println("${msg.length} : $msg")

            val smsManager: SmsManager = SmsManager.getDefault()
            smsManager.sendTextMessage(phoneNo, null, msg, null, null)
            Toast.makeText(applicationContext, "전송 완료!", Toast.LENGTH_LONG).show()

        } catch (e: Exception) {
            Toast.makeText(applicationContext, "전송 실패", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }

    private fun sendMMS(phoneNo: String, msg: String) {
        try {
            val sendIntent = Intent(Intent.ACTION_SEND)

            sendIntent.setClassName("com.android.mms", "com.android.mms.ui.ComposeMessageActivity");
            sendIntent.putExtra("address", phoneNo)
            sendIntent.putExtra("subject", "")
            sendIntent.putExtra("sms_body", msg)

            //sendIntent.setType("image/*")
            //sendIntent.putExtra(Intent.EXTRA_STREAM, imgUri)

            startActivity(sendIntent)

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    private fun getMMSBody(mmsId: String): String {
        val selectionPart = "mid=$mmsId"
        val uri = Uri.parse("content://mms/part")
        val cursor = contentResolver.query(uri, null, selectionPart, null, null)

        var body = ""
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                do {


                    val partId = cursor.getString(cursor.getColumnIndex("_id"))
                    val type = cursor.getString(cursor.getColumnIndex("ct"))

                    if ("text/plain" == type) {
                        val data: String? = cursor.getString(cursor.getColumnIndex("_data"))
                        if (data != null) {
                            // implementation of this method below
                            body = getMmsText(partId)
                        } else {
                            body = cursor.getString(cursor.getColumnIndex("text"))
                        }
                    }
                } while (cursor.moveToNext())

                cursor.close()
            }
        }

        return body.replace("""\s""".toRegex(), " ")
    }

    private fun getMmsText(id: String): String {
        val partURI = Uri.parse("content://mms/part/$id")
        var `is`: InputStream? = null
        val sb = StringBuilder()
        try {
            `is` = contentResolver.openInputStream(partURI)
            if (`is` != null) {
                val isr = InputStreamReader(`is`, "UTF-8")
                val reader = BufferedReader(isr)
                var temp: String = reader.readLine()
                while (temp != null) {
                    sb.append(temp)
                    temp = reader.readLine()
                }
            }
        } catch (e: IOException) {
        } finally {
            if (`is` != null) {
                try {
                    `is`.close()
                } catch (e: IOException) {
                }
            }
        }
        return sb.toString()
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