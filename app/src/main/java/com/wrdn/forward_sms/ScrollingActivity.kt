package com.wrdn.forward_sms

import android.Manifest
import android.animation.ObjectAnimator
import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.provider.Telephony
import android.telephony.SmsManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_scrolling.*
import kotlinx.android.synthetic.main.content_scrolling.*
import kotlinx.android.synthetic.main.item_msg_list.view.*
import kotlinx.coroutines.*
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.util.*
import kotlin.Comparator
import kotlin.collections.ArrayList


class ScrollingActivity : AppCompatActivity() {

    private val SELECT_PHONE_NUMBER = 10002
    private val SEND_MMS = 10001
    private val multiplePermissionsCode = 100

    //필요한 퍼미션 리스트
    //원하는 퍼미션을 이곳에 추가하면 된다.
    private val requiredPermissions = arrayOf(
        Manifest.permission.READ_SMS
        , Manifest.permission.SEND_SMS
        , Manifest.permission.READ_CONTACTS
    )


    val myAdapter = MyAdapter(arrayListOf("\n조회 먼저 하십시오"))
    val mmsQueue: Queue<String> = LinkedList()


    private var viewModelJob = Job()
    private val uiScope = CoroutineScope(Dispatchers.Main + viewModelJob)


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scrolling)
        setSupportActionBar(toolbar) //setSupportActionBar(findViewById(R.id.toolbar))
        toolbar_layout.title = "받은 문자 조회 후 한번에 전달할 수 있습니다" //findViewById<CollapsingToolbarLayout>(R.id.toolbar_layout).title = title


        checkPermissions()

        setDefaultValue()

        rvList.apply {
            adapter = myAdapter
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(this@ScrollingActivity)
            isNestedScrollingEnabled = false
        }


        dateAfter.setOnFocusChangeListener { v, hasFocus -> if (hasFocus) showDatePicker(v as EditText?) }
        dateAfter.setOnClickListener { showDatePicker(it as EditText?) }

        dateBefore.setOnFocusChangeListener { v, hasFocus -> if (hasFocus) showDatePicker(v as EditText?) }
        dateBefore.setOnClickListener { showDatePicker(it as EditText?) }


        btnQuery.setOnClickListener { query() }
        fab.setOnClickListener { showSendDialog() }

        toNumber.setOnFocusChangeListener { _, hasFocus ->
            reformatToNumber()
            if (hasFocus) {
                val i = Intent(Intent.ACTION_PICK)
                i.type = ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE
                startActivityForResult(i, SELECT_PHONE_NUMBER)
            }
        }

        reformatToNumber()
    }

    override fun onStart() {
        super.onStart()

        val da = YCalendar(numberOnly(dateAfter))
        if (da.getYYYYMMDD() == YCalendar.displayYYYYMMDD()) {
            query()
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        rememberCondition()

        viewModelJob.cancel()
    }

    private fun query() {
        rememberCondition()

        myAdapter.setList(arrayListOf("\n조회 중입니다"))


        uiScope.launch {
            withContext(Dispatchers.IO) {
                val da = YCalendar(numberOnly(dateAfter))
                da.setHMS(0, 0, 0)
                val db = YCalendar(numberOnly(dateBefore))
                db.addDate(1)

                var msgList = readSMS(this@ScrollingActivity, da, db, numberOnly(fromNumber), includingText.text.toString())
                val mmsList = readMMS(this@ScrollingActivity, da, db, numberOnly(fromNumber), includingText.text.toString())

                msgList.addAll(mmsList)
                msgList = msgList.sortedWith(Comparator { o1, o2 -> if (o1 > o2) -1 else 1 }).toMutableList()


                withContext(Dispatchers.Main) {
                    myAdapter.setList(msgList)

                    ObjectAnimator.ofInt(scrollView, "scrollY", findDistanceToScroll(rvList)).setDuration(300).start()
                    //scrollView.requestChildFocus(condition_container, result)
                }
            }
        }
    }

    class MyAdapter(private var msgList: MutableList<String>) : RecyclerView.Adapter<MyAdapter.MyViewHolder>() {

        inner class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

            init {
                setEventHandler()
            }

            fun bind(position: Int) {
                Log.i("haha", "pos;   $position")
                val msg = Message.getMessage(msgList[position])

                itemView.txtDateTime.text = msg.date
                itemView.txtSender.text = msg.sender
                itemView.txtMsg.text = msg.msg

                setBgColor(msg.date)
            }

            private fun setBgColor(msg: String) {
                Log.i("haha", "bg color")
                if (msg.startsWith("X ")) {
                    itemView.setBackgroundResource(R.color.deletedMsgColor)
                } else {
                    itemView.setBackgroundResource(R.color.defaultMsgColor)
                }
            }

            private fun setEventHandler() {
                itemView.itemContainer.setOnClickListener {
                    var msg = msgList[adapterPosition]

                    msg = if (msg.startsWith("X ")) msg.substring(2) else "X $msg"
                    val uimsg = if (msg.startsWith("X ")) "발신 제외" else "발신 포함"

                    msgList[adapterPosition] = msg
                    notifyDataSetChanged()

                    Snackbar.make(itemView, uimsg, Snackbar.LENGTH_LONG).setAction("Action", null).show()
                }
            }

        } // end of MyViewHolder



        fun setList(list: MutableList<String>) {
            msgList = list
            notifyDataSetChanged()
        }

        fun getList(): MutableList<String> {
            return msgList
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyAdapter.MyViewHolder {
            return MyViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_msg_list, parent, false))
        }

        override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
            holder.bind(position)
        }

        override fun getItemCount() = msgList.size
    }

    data class Message(
        var date: String = "",
        var sender: String = "",
        var msg: String = ""
    ) {
        fun log() {
            Log.i("haha", "\n\n${date};   ${sender};   ${msg}")
        }

        companion object {
            fun getMessage(s: String): Message {
                val list = (s + "\n\n\n\n\n").split("\n")
                val msg = list.subList(2, list.size).joinToString("\n")

                return Message(list[0], list[1], msg.trim())
            }
        }
    }

    private fun readSMS(context: Context, dateAfter: YCalendar, dateBefore: YCalendar, num: String, inct: String): MutableList<String> {
        val rtn = ArrayList<String>()


        val uri = Telephony.Sms.Inbox.CONTENT_URI  // Uri.parse("content://sms/inbox")

        val whereClause = "date > ? and date < ? and INSTR(address, ?) > 0 and INSTR(body, ?) > 0"
        val whereArgs = arrayOf("${dateAfter.timeInMillis}", "${dateBefore.timeInMillis}", num, inct)

        val cursor = context.contentResolver.query(uri, arrayOf("*"), whereClause, whereArgs, "date asc")

        if (cursor != null && cursor.moveToFirst()) {
            do {
                viewModelJob.ensureActive()

                val smsDate = cursor.getLong(cursor.getColumnIndexOrThrow("date"))
                var number = cursor.getString(cursor.getColumnIndexOrThrow("address"))
                val body = cursor.getString(cursor.getColumnIndexOrThrow("body"))//.replace("""\s""".toRegex(), " ")

                Log.i("haha", "$smsDate;   $number;   $body")

                val yc = YCalendar(Date(smsDate))

                number = reformatNumber(number, "-")
                var name = getContactName(context, number)
                name = if (name == "") {
                    number
                } else {
                    "$name ($number)"
                }

                rtn.add("${yc}\n${name}\n${body}")


            } while (cursor.moveToNext())
        }

        cursor?.close()

        return rtn
    }


    // 참조 URL
    // https://www.it-swarm.dev/ko/android/mms-android%EC%9D%98-%EB%8D%B0%EC%9D%B4%ED%84%B0%EB%A5%BC-%EC%9D%BD%EB%8A%94-%EB%B0%A9%EB%B2%95/969694767/
    private fun readMMS(context: Context, dateAfter: YCalendar, dateBefore: YCalendar, num: String, inct: String): MutableList<String> {
        val rtn = ArrayList<String>()


        val uri = Telephony.Mms.Inbox.CONTENT_URI

        val whereClause = "date > ? and date < ? and INSTR(address, ?) > 0"
        val whereArgs = arrayOf("${dateAfter.timeInMillis / 1000}", "${dateBefore.timeInMillis / 1000}", num)

        val cursor = context.contentResolver.query(uri, arrayOf("*"), whereClause, whereArgs, "date asc")


        if (cursor != null && cursor.moveToFirst()) {
            do {
                viewModelJob.ensureActive()

                // mms columns
                // date 에는 뒤에 000 을 붙여야 한다!!!!!!!!!!
                // 아이디, 수신번호만 의미있다
                // ct_t 는 컨텐트 타입이다

                val id = cursor.getString(cursor.getColumnIndexOrThrow("_id"))
                val date = cursor.getLong(cursor.getColumnIndexOrThrow("date"))
                var number = cursor.getString(cursor.getColumnIndexOrThrow("address"))
                val body = getMMSBody(id)//.replace("""\s""".toRegex(), " ")

                Log.i("haha", "$date;   $number;   $body")

                val yc = YCalendar(Date(date * 1000))

                if (body.indexOf(inct) >= 0) {
                    number = reformatNumber(number, "-")
                    var name = getContactName(context, number)
                    name = if (name == "") {
                        number
                    } else {
                        "$name ($number)"
                    }

                    rtn.add("${yc}\n${name}\n${body}")
                }


            } while (cursor.moveToNext())

            cursor.close()
        }

        return rtn
    }


    private fun send() {
        var list = myAdapter.getList()
        list = list.sortedWith(Comparator { o1, o2 -> if (o1 < o2) -1 else 1 }).toMutableList()

        val tonum = numberOnly(toNumber)
        list.forEach { x ->
            if(x.startsWith("X ")) return@forEach

            val msg = Message.getMessage(x)

            msg.msg = msg.msg.replace("[Web발신]", "").trim()

            msg.log()

            if (msg.msg.length > 70) {
                mmsQueue.add(msg.msg)
            } else {
                sendSMS(tonum, msg.msg)
            }
        }

        sendMMS()
    }

    private fun sendSMS(phoneNo: String, msg_: String) {
        try {
            var msg = msg_
            if (msg.length > 70) msg = msg.substring(0, 70)

            val smsManager: SmsManager = SmsManager.getDefault()
            smsManager.sendTextMessage(phoneNo, null, msg, null, null)
            //Toast.makeText(applicationContext, "전송 완료!", Toast.LENGTH_LONG).show()

        } catch (e: Exception) {
            //Toast.makeText(applicationContext, "전송 실패", Toast.LENGTH_LONG).show()
            Snackbar.make(btnQuery, "문자 전송에 실패했습니다", Snackbar.LENGTH_LONG).setAction("Action", null).show()
            e.printStackTrace()
        }
    }

    private fun sendMMS() {
        val msg = mmsQueue.poll() ?: return

        try {
            val sendIntent = Intent(Intent.ACTION_SEND)

            sendIntent.setClassName("com.android.mms", "com.android.mms.ui.ComposeMessageActivity")
            sendIntent.putExtra("address", toNumber.text.toString())
            sendIntent.putExtra("subject", "")
            sendIntent.putExtra("sms_body", msg)

            //sendIntent.setType("image/*")
            //sendIntent.putExtra(Intent.EXTRA_STREAM, imgUri)

            startActivityForResult(sendIntent, SEND_MMS)

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun showSendDialog() {
        rememberCondition()

        AlertDialog.Builder(this).run {
            //setTitle("문자를 보낼까요?")
            setMessage("문자 메시지 요금이\n발생할 수 있습니다!!\n\n한번에 많은 문자가 보내지니\n신중히 살펴보십시오!\n\n문자를 보내겠습니까?")

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

    private fun showDatePicker(ed: EditText?) {
        if (ed == null) return

        val c = YCalendar(numberOnly(ed))

        DatePickerDialog(
            this,
            DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
                val m = "0${month + 1}".takeLast(2)
                val d = "0${dayOfMonth}".takeLast(2)
                setEditText(ed, "$year $m $d")
            },
            c.get(Calendar.YEAR),
            c.get(Calendar.MONTH),
            c.get(Calendar.DAY_OF_MONTH)
        ).show()
    }


    private fun rememberCondition() {
        reformatToNumber()

        val pref = getSharedPreferences("Config", Context.MODE_PRIVATE)

        pref.edit().let { conf ->
            conf.putString("dateAfter", dateAfter.text.toString())
            conf.putString("dateBefore", dateBefore.text.toString())
            conf.putString("fromNumber", fromNumber.text.toString())
            conf.putString("includingText", includingText.text.toString())
            conf.putString("toNumber", toNumber.text.toString())

            conf.apply()
        }

    }

    private fun reformatToNumber() {
        setEditText(toNumber, reformatNumber(numberOnly(toNumber), "-"))
        txtToNumber.text = getContactName(this, numberOnly(toNumber))
    }

    private fun reformatNumber(s: String, deli: String): String {
        return when (s.length) {
            11 -> { //  010 3333 5555
                "${s.substring(0, 3)}${deli}${s.substring(3, 7)}${deli}${s.substring(7)}"
            }

            10 -> { // 010 333 3333
                "${s.substring(0, 3)}${deli}${s.substring(3, 6)}${deli}${s.substring(6)}"
            }

            8 -> { // 1500 0000
                "${s.substring(0, 4)}${deli}${s.substring(4)}"
            }

            9 -> { // 02 333 3333
                "${s.substring(0, 2)}${deli}${s.substring(2, 5)}${deli}${s.substring(5)}"
            }
            else -> s
        }

    }

    private fun numberOnly(ed: EditText): String {
        val rtn = ed.text.toString().replace("""[^0-9]""".toRegex(), "")
        Log.i("haha", "번호만 추출 : $rtn")
        return rtn
    }

    private fun setEditText(ed: EditText, s: String) {
        ed.setText(s, TextView.BufferType.EDITABLE)
    }

    private fun setDefaultValue() {
        val pref = getSharedPreferences("Config", Context.MODE_PRIVATE)

        pref.getString("dateAfter", "")?.let { setEditText(dateAfter, it) }
        pref.getString("dateBefore", "")?.let { setEditText(dateBefore, it) }
        pref.getString("fromNumber", "")?.let { setEditText(fromNumber, it) }
        pref.getString("includingText", "")?.let { setEditText(includingText, it) }
        pref.getString("toNumber", "")?.let { setEditText(toNumber, it) }
    }

    private fun getContactName(context: Context, phoneNumber: String): String {
        var contactName = ""

        try {
            val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber))
            val projection = arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME)

            val cursor = context.contentResolver.query(uri, projection, null, null, null)

            if (cursor != null && cursor.moveToFirst()) {
                contactName = cursor.getString(0)
                cursor.close()
            }
        } catch (e: Exception) {
        }

        return contactName
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
                        body = if (data != null) {
                            // implementation of this method below
                            getMmsText(partId)
                        } else {
                            cursor.getString(cursor.getColumnIndex("text"))
                        }
                    }
                } while (cursor.moveToNext())

                cursor.close()
            }
        }

        return body//.replace("""\s""".toRegex(), " ")
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


    private fun findDistanceToScroll(view: View): Int {
        var distance = view.top
        var viewParent = view.parent
        //traverses 10 times
        for (i in 0..9) {
            if ((viewParent as View).id == R.id.scrollView) {
                return distance
            }
            distance += (viewParent as View).top
            viewParent = viewParent.getParent()
        }
        return 0
    }

    private fun checkPermissions() {
        //거절되었거나 아직 수락하지 않은 권한(퍼미션)을 저장할 문자열 배열 리스트
        val rejectedPermissionList = ArrayList<String>()

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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            SEND_MMS -> {
                sendMMS()
            }

            SELECT_PHONE_NUMBER -> {
                if (resultCode != RESULT_OK) return
                val contactUri = data?.data ?: return

                val projection = arrayOf(
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER
                )
                val cursor = contentResolver.query(contactUri, projection, null, null, null)

                if (cursor != null && cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                    val numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                    val name = cursor.getString(nameIndex)
                    val number = cursor.getString(numberIndex)

                    setEditText(toNumber, number)
                    txtToNumber.text = name
                    reformatToNumber()
                }
                cursor?.close()

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