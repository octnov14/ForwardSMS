package com.wrdn.forward_sms

import android.content.Intent
import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.*


class YCalendar : GregorianCalendar {

    private var agd_name: String? = null
    private var agd_action1_name: String? = null
    private var agd_action_name: String? = null
    private var agd_action_time: String? = null

    fun setAGDInfo(
        groupName: String?,
        action1Name: String?,
        actionName: String?,
        actionTime: String?
    ) {
        agd_name = groupName
        agd_action1_name = action1Name
        agd_action_name = actionName
        agd_action_time = actionTime
    }

    init {
        firstDayOfWeek = MONDAY
        minimalDaysInFirstWeek = 7
    }

    constructor() : super() {}

    constructor(cal: Calendar) {
        setTime(cal.time)
    }

    constructor(d: Date) {
        setTime(d)
    }

    constructor(format: String, s: String) {
        try {
            SimpleDateFormat(format, Locale.getDefault()).parse(s, ParsePosition(0))?.let {
                setTime(it)
            }
        } catch (e: Exception) {
        }
    }

    constructor(s: String) {
        try {
            when (s.length) {
                8 -> SimpleDateFormat("yyyyMMdd", Locale.getDefault()).parse(s, ParsePosition(0))?.let {
                    setTime(it)
                }

                14 -> SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault()).parse(
                    s,
                    ParsePosition(0)
                )?.let {
                    setTime(it)
                }
            }
        } catch (e: Exception) {
        }
    }

    constructor(year: Int, month: Int) : this(year, month, 1) {}

    constructor(year: Int, month: Int, date: Int) : super(year, month, date) {}

    constructor(year: Int, month: Int, date: Int, hour: Int, minute: Int) : super(
        year, month, date, hour, minute
    ) {
    }

    constructor(
        year: Int,
        month: Int,
        date: Int,
        hour: Int,
        minute: Int,
        second: Int
    ) : super(year, month, date, hour, minute, second) {

    }


    fun equals(cal: YCalendar): Boolean {
        return compareTo(cal) == 0
    }

    fun equalsDate(cal: YCalendar): Boolean {
        return getYYYYMMDD() == cal.getYYYYMMDD()
    }

    override fun compareTo(other: Calendar): Int {
        return getTime().compareTo(other.time)
    }


    private fun getYear(): Int {
        return get(YEAR)
    }

    private fun getMonth(): Int {
        return get(MONTH)
    }

    private fun getDate(): Int {
        return get(DATE)
    }

    fun getHour(): Int {
        return get(HOUR)
    }

    private fun getHour24(): Int {
        return get(HOUR_OF_DAY)
    }

    private fun getMinute(): Int {
        return get(MINUTE)
    }

    fun getSecond(): Int {
        return get(SECOND)
    }

    fun getDayOfWeek(): Int {
        return get(DAY_OF_WEEK)
    }

    private fun setYear(t: Int) {
        set(YEAR, t)
    }

    private fun setMonth(t: Int) {
        set(MONTH, t)
    }

    fun setDate(t: Int) {
        set(DATE, t)
    }

    private fun setHour(t: Int) {
        set(HOUR_OF_DAY, t)
    }

    private fun setMinute(t: Int) {
        set(MINUTE, t)
    }

    fun setSecond(t: Int) {
        set(SECOND, t)
    }

    private fun setYMD(y: Int, m: Int, d: Int) {
        setYear(y)
        setMonth(m)
        setDate(d)
    }

    fun setHMS(h: Int, m: Int, s: Int) {
        setHour(h)
        setMinute(m)
        setSecond(s)
    }

    fun format(s: String): String {
        return try {
            SimpleDateFormat(s, Locale.getDefault()).format(getTime())
        } catch (e: Exception) {
            ""
        }
    }

    fun getYYYYMM(): String {
        return format("yyyyMM")
    }

    fun getYYYYMMDD(): String {
        return format("yyyyMMdd")
    }

    fun getYYYY_MM_DD(): String {
        return format("yyyy-MM-dd")
    }

    fun getYMDE(): String {
        return format("yyyy-MM-dd(EEE)")
    }

    fun getYMDHMS(): String {
        return format("yyyyMMddHHmmss")
    }

    fun getYMDHMSSql(): String {
        return format("yyyy-MM-dd HH:mm:ss")
    }


    fun getTimeGapStr(): String {
        val now = YCalendar()

        var h = getHour24() - now.getHour24()
        var m = getMinute() - now.getMinute()

        if (m < 0) {
            m = 60 + m
            h--
        }

        return if (h > 0) "${h}시간 ${m}분 전"
        else "${m}분 전"
    }

    fun getDayStr(): String {
        val today = YCalendar()

        return when (val n = getDaysBetween(today, this)) {
            -2 -> "그제"
            -1 -> "어제"
            0 -> "오늘"
            1 -> "내일"
            2 -> "모레"
            else -> {
                if(n > 0) "" + n + "일 후"
                else "" + -n + "일 전"
            }
        }
    }

    fun getMinuteStr(): String {
        val mStr = when (val m = getMinute()) {
            0 -> "정각"
            else -> "" + m + "분"
        }

        return mStr
    }

    override fun toString(): String {
        return format("yyyy-MM-dd(E) HH:mm:ss")
    }


    //
    // 날짜연산
    //

    //
// 날짜연산
//
    fun addDate(d: Int): YCalendar {
        add(DATE, d)
        return this
    }

    fun addMonth(d: Int): YCalendar {
        add(MONTH, d)
        return this
    }

    fun addYear(d: Int): YCalendar {
        add(YEAR, d)
        return this
    }

    private fun firstDay(): YCalendar {
        set(DATE, 1)
        return this
    }

    fun middleDay(): YCalendar {
        set(DATE, 15)
        return this
    }

    fun lastDay(): YCalendar {
        add(MONTH, 1)
        firstDay()
        add(DATE, -1)
        return this
    }

    fun firstMondayOfMonth() {
        set(DATE, 1)
        add(DATE, -1)
        nextMonday()
    }

    fun monday(): String {
        add(DATE, -7)
        nextMonday()
        return getYYYYMMDD()
    }

    private fun nextMonday() {
        nextDayOfWeek(MONDAY)
    }

    fun dayOfWeek(dayOfWeek: Int) {
        add(DATE, -7)
        nextDayOfWeek(dayOfWeek)
    }

    private fun nextDayOfWeek(dayOfWeek: Int) {
        while (true) {
            add(DAY_OF_MONTH, 1)
            if (get(DAY_OF_WEEK) == dayOfWeek) {
                break
            }
        }
    }

    fun putAGDInfoExtra(intent: Intent) {
        intent.putExtra("agd_name", agd_name)
        intent.putExtra("agd_action1_name", agd_action1_name)
        intent.putExtra("agd_action_name", agd_action_name)
        intent.putExtra("agd_action_time", agd_action_time)
    }

    fun setYMD(c: YCalendar) {
        setYMD(c.getYear(), c.getMonth(), c.getDate())
    }

    //
    // static Methods
    //

    companion object {

        fun displayYMDHMS(): String {
            return YCalendar().getYMDHMS()
        }

        fun displayTime(s: String): String {
            val c = YCalendar(s)
            return if (c.getYMDHMS() != s) {
                s
            } else {
                c.toString()
            }
        }

        fun displayTime(): String {
            return YCalendar().toString()
        }

        fun displayYMDE(s: String): String {
            val c = YCalendar(s)
            return if (c.getYYYYMMDD() != (s + "12345678").substring(0, 8)) {
                s
            } else {
                c.getYMDE()
            }
        }

        fun displayYMDE(): String {
            return YCalendar().getYMDE()
        }

        fun displayYYYY_MM_DD(s: String): String {
            val c = YCalendar(s)
            return if (c.getYYYYMMDD() != (s + "12345678").substring(0, 8)) {
                s
            } else {
                c.getYYYY_MM_DD()
            }
        }

        fun displayYYYY_MM_DD(): String {
            return YCalendar().getYYYY_MM_DD()
        }

        fun displayYYYYMMDD(s: String): String {
            val c = YCalendar(s)
            return if (c.getYYYYMMDD() != (s + "12345678").substring(0, 8)) {
                s
            } else {
                c.getYYYYMMDD()
            }
        }

        fun displayYYYYMMDD(): String {
            return YCalendar().getYYYYMMDD()
        }

        fun isWeekend(cal: YCalendar): Boolean {
            return cal.get(DAY_OF_WEEK) == SATURDAY || cal.get(DAY_OF_WEEK) == SUNDAY
        }

        fun isInSameWeek(c1: YCalendar, c2: YCalendar): Boolean {
            val cal1 = YCalendar(c1)
            val cal2 = YCalendar(c2)
            return cal1.monday() == cal2.monday()
        }

        fun getMonthsBetween(d1: YCalendar, d2: YCalendar): Int {
            val from = YCalendar(d1)
            val to = YCalendar(d2)
            from.setDate(1)
            from.setHMS(0, 0, 0)
            to.setDate(1)
            to.setHMS(0, 0, 0)
            if (from > to) return -1
            var rtn = 0
            while (from < to) {
                from.addMonth(1)
                rtn++
            }
            return rtn
        }

        fun getDaysBetween(d1: YCalendar, d2: YCalendar): Int {
            var sign = 1
            val from: YCalendar
            val to: YCalendar

            if (d1 < d2) {
                from = YCalendar(d1.getYYYYMMDD())
                to = YCalendar(d2.getYYYYMMDD())
            } else {
                sign = -1
                from = YCalendar(d2.getYYYYMMDD())
                to = YCalendar(d1.getYYYYMMDD())
            }

            var rtn = 0
            while (from < to) {
                from.addDate(1)
                rtn++
            }

            return rtn * sign
        }

        fun getWeekCountOfMonth(year: Int, month: Int): Int {
            val cal = YCalendar()
            cal.set(year, month, 1)
            cal.set(DAY_OF_MONTH, cal.getActualMaximum(DAY_OF_MONTH))
            return cal.get(WEEK_OF_MONTH)
        }

        fun getMondaysOfMonth(year: Int, month: Int): Array<YCalendar> {
            val mondays = arrayOf<YCalendar>()
            val cal = YCalendar(year, month)
            var monday_index = 0
            for (d in 1..cal.getActualMaximum(DAY_OF_MONTH)) {
                cal.set(DAY_OF_MONTH, d)
                if (cal.get(DAY_OF_WEEK) == MONDAY) {
                    mondays[monday_index++] = YCalendar(year, month, d)
                }
            }
            return mondays
        }

        fun createHHMM(ss: String): YCalendar {
            val c1 = YCalendar("HHmm", ss)
            val today = YCalendar()

            c1.setYMD(today)
            c1.setSecond(0)

            return c1
        }
    }


}
