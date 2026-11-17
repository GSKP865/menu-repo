package com.menu.app.admin

import android.app.DatePickerDialog
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.WindowManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.core.content.res.ResourcesCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.menu.app.BuildConfig
import com.menu.app.R
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.Calendar

class AdminActivity : AppCompatActivity() {
    private lateinit var repository: AdminRepository
    private lateinit var root: LinearLayout
    private lateinit var content: LinearLayout
    private var section = "overview"
    private var date = AdminRules.day(System.currentTimeMillis())
    private var search = ""
    private var status = "all"
    private var page = 0
    private var pending = false
    private var loading: Job? = null
    private val ink = Color.rgb(36, 49, 58)
    private fun dp(value: Int) = (resources.displayMetrics.density * value).toInt()
    private fun column() = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
    private fun text(value: String, size: Float = 13f, bold: Boolean = false) = TextView(this).apply {
        text = value; textSize = size; setTextColor(ink)
        typeface = Typeface.create(ResourcesCompat.getFont(this@AdminActivity,R.font.manrope),if(bold) Typeface.BOLD else Typeface.NORMAL)
        setPadding(dp(4),dp(4),dp(4),dp(4))
    }
    private fun button(parent: LinearLayout, title: String, enabled: Boolean = true, action: () -> Unit) {
        parent.addView(Button(this).apply {
            text=title; textSize=12f; isAllCaps=false; minHeight=dp(48); isEnabled=enabled && !pending
            setOnClickListener { if(!pending) action() }
        },LinearLayout.LayoutParams(-1,-2))
    }
    private fun polishButtons(group: android.view.ViewGroup) {
        for(index in 0 until group.childCount) {
            val view=group.getChildAt(index)
            if(view is Button) {
                view.typeface=ResourcesCompat.getFont(this,R.font.manrope)
                view.backgroundTintList=null
                view.background=GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                    if(view.isSelected) intArrayOf(Color.rgb(61,108,94),Color.rgb(36,75,64))
                    else intArrayOf(Color.WHITE,Color.rgb(231,240,235))).apply {
                    cornerRadius=dp(12).toFloat(); setStroke(dp(1),Color.rgb(204,219,210))
                }
                view.setTextColor(if(view.isSelected) Color.WHITE else ink)
                view.alpha=if(view.isEnabled) 1f else 0.45f
                view.elevation=dp(1).toFloat()
                (view.layoutParams as? android.view.ViewGroup.MarginLayoutParams)?.apply {
                    setMargins(dp(3),dp(4),dp(3),dp(4))
                }
            } else if(view is android.view.ViewGroup) polishButtons(view)
        }
    }
    private fun card(): LinearLayout = column().apply {
        background=GradientDrawable().apply { setColor(Color.WHITE); cornerRadius=dp(16).toFloat() }
        setPadding(dp(12),dp(10),dp(12),dp(10))
        content.addView(this,LinearLayout.LayoutParams(-1,-2).apply { bottomMargin=dp(10) })
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val demo = intent.getBooleanExtra("demo_admin",false)
        if(demo && !BuildConfig.DEBUG) { finish(); return }
        repository=AdminRepository(this,demo)
        if(!demo) window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        savedInstanceState?.let {
            section=it.getString("section","overview"); date=it.getString("date",date)
            search=it.getString("search",""); status=it.getString("status","all"); page=it.getInt("page")
        }
        load()
    }
    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("section",section); outState.putString("date",date)
        outState.putString("search",search); outState.putString("status",status); outState.putInt("page",page)
        super.onSaveInstanceState(outState)
    }
    private fun load() {
        loading?.cancel()
        shell()
        content.addView(text("Loading admin data…"))
        loading=lifecycleScope.launch {
            try {
                val data=repository.load(section,date,search,status,page)
                content.removeAllViews()
                content.addView(text(data.optString("actor","Authorized administrator"),11f))
                if(section=="overview") overview(data.getJSONObject("metrics")) else records(data)
                polishButtons(root)
            } catch(cancelled: CancellationException) { throw cancelled }
            catch(error: Exception) {
                content.removeAllViews()
                content.addView(text(error.message ?: "Unable to load. Check your connection and retry."))
                button(content,"Retry") { load() }
            }
        }
    }
    private fun shell() {
        root=column().apply { setPadding(dp(14),dp(8),dp(14),0); setBackgroundColor(Color.rgb(241,246,244)) }
        setContentView(root)
        root.addView(text("Me’n’U · Admin",20f,true))
        root.addView(text(if(repository.demo) "DEMO · dummy admin · local sample data only" else "LIVE · server-authorized access",11f,true))
        val tabs=LinearLayout(this)
        listOf("overview" to "Overview","users" to "Users","calls" to "Calls","payouts" to "Withdrawals","reports" to "Reports","audit" to "Audit").forEach { (key,label) ->
            tabs.addView(Button(this).apply {
                text=label; textSize=11f; isAllCaps=false; isEnabled=!pending; isSelected=section==key
                setOnClickListener { section=key; search=""; status="all"; page=0; load() }
            })
        }
        root.addView(HorizontalScrollView(this).apply { isHorizontalScrollBarEnabled=false; addView(tabs) })
        val filters=LinearLayout(this)
        filters.addView(Button(this).apply {
            text=date + " · IST"; textSize=11f; isAllCaps=false; isEnabled=!pending
            setOnClickListener {
                val bits=date.split("-").map { it.toInt() }
                DatePickerDialog(this@AdminActivity,{ _,y,m,d ->
                    date=String.format(java.util.Locale.US,"%04d-%02d-%02d",y,m+1,d); page=0; load()
                },bits[0],bits[1]-1,bits[2]).show()
            }
        },LinearLayout.LayoutParams(0,-2,1f))
        filters.addView(Button(this).apply { text="Refresh"; textSize=11f; isAllCaps=false; isEnabled=!pending; setOnClickListener { load() } })
        root.addView(filters)
        if(section!="overview") {
            val query=EditText(this).apply {
                hint="Search name, user ID or record ID"; textSize=12f; setSingleLine(true); setText(search); isEnabled=!pending
            }
            root.addView(query)
            val row=LinearLayout(this)
            val options=when(section) {
                "users" -> arrayOf("all","active","suspended","blocked")
                "calls" -> arrayOf("all","ringing","in_progress","completed","failed","dropped")
                "payouts" -> arrayOf("all","pending_review","approved","processing","paid","rejected","failed","reversed")
                "reports" -> arrayOf("all","open","resolved")
                else -> arrayOf("all")
            }
            val picker=Spinner(this).apply {
                adapter=ArrayAdapter(this@AdminActivity,android.R.layout.simple_spinner_dropdown_item,options)
                setSelection(options.indexOf(status).coerceAtLeast(0)); isEnabled=!pending
            }
            row.addView(picker,LinearLayout.LayoutParams(0,dp(48),1f))
            row.addView(Button(this).apply {
                text="Apply"; textSize=12f; isAllCaps=false; isEnabled=!pending
                setOnClickListener { search=query.text.toString(); status=picker.selectedItem.toString(); page=0; load() }
            })
            root.addView(row)
        }
        content=column()
        root.addView(ScrollView(this).apply { addView(content) },LinearLayout.LayoutParams(-1,0,1f))
        button(root,"Exit admin") { finish() }
        polishButtons(root)
    }
    private fun overview(m: JSONObject) {
        val today=card()
        today.addView(text("Calls on " + date + " · Asia/Kolkata",14f,true))
        today.addView(text("Total " + m.optLong("calls_day") + "  ·  Completed " + m.optLong("completed_day")))
        today.addView(text("Failed / dropped " + m.optLong("failed_day") + "  ·  Minutes " + m.optLong("duration_day")/60))
        val users=card()
        users.addView(text("Users & safety",14f,true))
        users.addView(text("Users " + m.optLong("users") + "  ·  Blocked " + m.optLong("blocked")))
        users.addView(text("Open reports " + m.optLong("open_reports")))
        val wallet=card()
        wallet.addView(text("Earnings & withdrawals · all time",14f,true))
        wallet.addView(text("Earned from calls: " + AdminRules.money(m.optLong("earned_minor"))))
        wallet.addView(text("Available: " + AdminRules.money(m.optLong("available_minor"))))
        wallet.addView(text("Awaiting review: " + AdminRules.money(m.optLong("pending_payout_minor"))))
        content.addView(text("Daily counts use call creation date in IST. Both participants count in per-user totals; platform calls count once. Earnings are not platform profit. Approval does not transfer money.",11f))
    }
    private fun records(data: JSONObject) {
        val items=data.getJSONArray("items")
        content.addView(text(if(section in setOf("users","calls")) "Call metrics for selected IST day." else "All-time records; date filter applies to call metrics only.",11f))
        if(items.length()==0) content.addView(text("No matching records."))
        for(i in 0 until items.length()) {
            val item=items.getJSONObject(i); val box=card(); val id=item.optString("id")
            when(section) {
                "users" -> {
                    box.addView(text(item.optString("display_name",id),14f,true))
                    box.addView(text(id + " · " + item.optString("status"),11f))
                    box.addView(text("Calls " + item.optLong("calls_day") + " · Made " + item.optLong("outgoing_day") + " · Received " + item.optLong("incoming_day")))
                    box.addView(text("Completed " + item.optLong("completed_day") + " · Minutes " + item.optLong("duration_day")/60))
                    box.addView(text("Earned " + AdminRules.money(item.optLong("earned_minor"))))
                    box.addView(text("Withdrawable " + AdminRules.money(item.optLong("available_minor"))))
                    box.addView(text("Pending " + AdminRules.money(item.optLong("pending_minor")) + " · Frozen " + AdminRules.money(item.optLong("frozen_minor"))))
                    button(box,"View this user's calls") { section="calls"; search=id; status="all"; page=0; load() }
                    button(box,"View withdrawals") { section="payouts"; search=id; status="all"; page=0; load() }
                    button(box,"Change user status") {
                        MaterialAlertDialogBuilder(this).setTitle("User status").setItems(arrayOf("Suspend","Block","Restore")) { _,which ->
                            confirm("user",id,arrayOf("suspended","blocked","active")[which])
                        }.show()
                    }
                }
                "calls" -> {
                    box.addView(text(id + " · " + item.optString("status"),13f,true))
                    box.addView(text(item.optString("caller_id") + " → " + item.optString("receiver_id"),11f))
                    box.addView(text("Duration: " + item.optLong("duration_seconds") + " seconds"))
                }
                "payouts" -> {
                    box.addView(text(AdminRules.money(item.optLong("amount_minor")) + " · " + item.optString("status"),14f,true))
                    box.addView(text(item.optString("user_id") + "\n" + id,11f))
                    box.addView(text("Requested: " + item.optString("requested_at"),11f))
                    button(box,"Approve for processing",AdminRules.canReview(item.optString("status"))) { confirm("payout",id,"approve") }
                    button(box,"Reject and release reservation",AdminRules.canReview(item.optString("status"))) { confirm("payout",id,"reject") }
                    box.addView(text("Approval is a review decision, not a bank transfer.",11f))
                }
                "reports" -> {
                    box.addView(text(item.optString("category") + " · " + item.optString("status"),14f,true))
                    box.addView(text(item.optString("user_id"),11f))
                    box.addView(text(item.optString("description")))
                    button(box,"Resolve report",item.optString("status")=="open") { confirm("report",id,"resolve") }
                    button(box,"Find reported user") { section="users"; search=item.optString("user_id"); status="all"; page=0; load() }
                }
                "audit" -> {
                    box.addView(text(item.optString("action"),14f,true))
                    box.addView(text(item.optString("admin_id") + "\nTarget: " + item.optString("target_id"),11f))
                    box.addView(text(item.optString("reason")))
                    box.addView(text(item.optString("created_at"),11f))
                }
            }
        }
        val total=data.optInt("total")
        content.addView(text("Page " + (page+1) + " · " + total + " matching records",11f))
        button(content,"Previous",page>0) { page--; load() }
        button(content,"Next",(page+1)*20<total) { page++; load() }
    }
    private fun confirm(kind: String, id: String, decision: String) {
        val reason=EditText(this).apply { hint="Required reason (5–500 characters)"; maxLines=4 }
        val dialog=MaterialAlertDialogBuilder(this).setTitle(decision.replaceFirstChar { it.uppercase() } + " · " + id)
            .setMessage(if(repository.demo) "Demo action only. No real users or money are affected." else "This changes a live record and is audit logged.")
            .setView(reason).setNegativeButton("Cancel",null).setPositiveButton("Confirm",null).create()
        dialog.setOnShowListener {
            dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val note=reason.text.toString()
                try { AdminRules.requireReason(note) } catch(e: IllegalArgumentException) { reason.error=e.message; return@setOnClickListener }
                if(pending) return@setOnClickListener
                pending=true; loading?.cancel(); dialog.dismiss(); shell()
                content.addView(text("Saving…"))
                lifecycleScope.launch {
                    try {
                        repository.change(kind,id,decision,note)
                        Toast.makeText(this@AdminActivity,"Saved to audit log",Toast.LENGTH_SHORT).show()
                    } catch(cancelled: CancellationException) { throw cancelled }
                    catch(e: Exception) { Toast.makeText(this@AdminActivity,e.message ?: "Failed; refresh before retrying.",Toast.LENGTH_LONG).show() }
                    finally { pending=false }
                    load()
                }
            }
        }
        dialog.show()
    }
}