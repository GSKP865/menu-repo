package com.menu.app.reference

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.content.res.ColorStateList
import androidx.core.content.res.ResourcesCompat
import android.os.Bundle
import android.view.Gravity
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.menu.app.R
import com.menu.app.auth.LoginActivity
import com.menu.app.domain.auth.AuthServices
import com.menu.app.profile.ProfileActivity

/** Interactive offline reference layouts. All displayed financial/call records are fixtures. */
class ReferenceActivity : AppCompatActivity() {
    private var screen = "Home"
    private var female = false
    private lateinit var root: LinearLayout
    private lateinit var body: LinearLayout
    private val ink = Color.rgb(43, 48, 55)
    private val teal = Color.rgb(32, 98, 102)
    private val bodyFont by lazy { ResourcesCompat.getFont(this, R.font.manrope) }
    private val headingFont by lazy { ResourcesCompat.getFont(this, R.font.lora) }
    private fun dp(n:Int)=(n*resources.displayMetrics.density).toInt()
    private fun column()=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL }
    private fun line()=LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL; gravity=Gravity.CENTER_VERTICAL }
    private fun bg(color:Int,r:Int=16)=GradientDrawable().apply { setColor(color); cornerRadius=dp(r).toFloat() }
    private fun gradient(start:String,end:String,r:Int=0)=GradientDrawable(GradientDrawable.Orientation.TL_BR,intArrayOf(Color.parseColor(start),Color.parseColor(end))).apply { cornerRadius=dp(r).toFloat() }
    private fun text(s:String,size:Float=16f,bold:Boolean=false,color:Int=ink)=TextView(this).apply {
        text=s; textSize=when { size >= 24f -> size - 6f; size >= 16f -> size - 2f; size >= 12f -> size - 1f; else -> size }; setTextColor(color)
        typeface = Typeface.create(if (size >= 20f) headingFont else bodyFont, if (bold) Typeface.BOLD else Typeface.NORMAL)
        includeFontPadding = false
        setLineSpacing(dp(1).toFloat(), 1f)
        setPadding(dp(3),dp(3),dp(3),dp(3))
    }
    private fun portrait(index:Int=0,size:Int=44)=ImageView(this).apply {
        setImageResource(intArrayOf(R.drawable.avatar_portrait_0,R.drawable.avatar_portrait_1,R.drawable.avatar_portrait_2,R.drawable.avatar_portrait_3)[index%4])
        layoutParams=LinearLayout.LayoutParams(dp(size),dp(size))
        contentDescription="Demo profile portrait"
        scaleType = ImageView.ScaleType.CENTER_CROP
        background = bg(Color.parseColor("#F7E8E3"), size / 3)
        clipToOutline = true
    }
    override fun onCreate(savedInstanceState:Bundle?) {
        super.onCreate(savedInstanceState)
        female=intent.getBooleanExtra("female",false)
        screen=savedInstanceState?.getString("screen") ?: intent.getStringExtra("screen") ?: "Home"
        onBackPressedDispatcher.addCallback(this,object:OnBackPressedCallback(true) {
            override fun handleOnBackPressed() { if(screen!="Home") render("Home") else finish() }
        })
        render(screen)
    }
    override fun onSaveInstanceState(out:Bundle) { out.putString("screen",screen); super.onSaveInstanceState(out) }
    private fun render(next:String) {
        screen=next
        window.statusBarColor=Color.parseColor(if(female) "#F4DFE8" else "#DCEEE8")
        window.navigationBarColor=Color.WHITE
        root=column().apply { background=gradient("#FCF8F5","#EDF5F2") }
        setContentView(root)
        androidx.core.view.WindowCompat.getInsetsController(window, root).isAppearanceLightStatusBars=true
        if(screen=="Call") { call(); return }
        val header=column().apply {
            background=gradient(if(female) "#F4DFE8" else "#DCEEE8","#FCF8F5")
            setPadding(dp(18),dp(12),dp(18),dp(12))
        }
        val title=when(screen) {
            "Home" -> if(female) "Let sparks happen" else "Find your spark"
            "Wallet" -> if(female) "Withdraw Earnings" else "Global Coin Balances"
            "History" -> "Call History"
            "More" -> "More"
            else -> screen
        }
        header.addView(text("ME ’N’ U  /  MADE FOR CONNECTION", 10f, true, teal).apply {
            letterSpacing = .08f
            setPadding(dp(3), 0, 0, dp(7))
        })
        val titleRow=line()
        if(screen!="Home") titleRow.addView(text("‹",24f).apply { minWidth=dp(40); minHeight=dp(48); setOnClickListener { render("Home") } })
        titleRow.addView(text(title,if(screen=="Home") 26f else 24f,true,ink),LinearLayout.LayoutParams(0,-2,1f))
        if(screen=="Home" || screen=="More") titleRow.addView(portrait(1,32).apply { setOnClickListener { render("More") } })
        header.addView(titleRow)
        if(screen=="Home" && !female) {
            header.addView(text("A little hello. A beautiful beginning.",13f,false,teal))
            header.addView(text("🪙 1,000 Coins",16f,true).apply {
                background=bg(Color.parseColor("#D9FFFFFF"),18)
                setPadding(dp(10),dp(6),dp(10),dp(6))
            },LinearLayout.LayoutParams(-2,-2).apply { topMargin=dp(7) })
        }
        if(screen=="Wallet") {
            header.addView(text("Available balance",12f).apply { gravity=Gravity.CENTER })
            header.addView(text(if(female) "🪙 5 Coins" else "🪙 30 Coins",28f,true,ink).apply { gravity=Gravity.CENTER })
        }
        if(screen=="More") {
            val summary=line(); summary.addView(portrait(1,60))
            summary.addView(text("Profile summary\nProfile summary",15f,true)); header.addView(summary)
        }
        root.addView(header)
        body=column().apply { setPadding(dp(14),dp(6),dp(14),dp(12)) }
        root.addView(ScrollView(this).apply { addView(body); isFillViewport=true },LinearLayout.LayoutParams(-1,0,1f))
        when(screen) {
            "Home" -> if(female) femaleHome() else maleHome()
            "Wallet" -> if(female) withdraw() else wallet()
            "History" -> history()
            "More" -> more()
            "Discover" -> { body.addView(text("Discover",20f,true)); people("Available Women",false) }
        }
        if(screen!="Wallet") nav()
    }
    private fun card(parent:LinearLayout=body):LinearLayout=column().apply {
        background=gradient("#FFFFFFFF","#F7FCFA",18).apply { setStroke(dp(1),Color.WHITE) }
        elevation=dp(1).toFloat()
        setPadding(dp(12),dp(10),dp(12),dp(10))
        parent.addView(this,LinearLayout.LayoutParams(-1,-2).apply { bottomMargin=dp(10) })
    }
    private fun action(parent:LinearLayout,label:String,click:()->Unit) {
        parent.addView(androidx.appcompat.widget.AppCompatButton(this).apply {
            text=label; isAllCaps=false; textSize=12f; setTextColor(Color.WHITE)
            typeface=Typeface.create(bodyFont,Typeface.BOLD)
            letterSpacing=.01f
            supportBackgroundTintList=null
            background=RippleDrawable(ColorStateList.valueOf(Color.parseColor("#40FFFFFF")),
                GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                    intArrayOf(Color.parseColor("#397C7E"),Color.parseColor("#205C63"))).apply {
                    cornerRadius=dp(14).toFloat()
                    setStroke(dp(1),Color.parseColor("#669BB0AA"))
                },null)
            elevation=dp(2).toFloat()
            setPadding(dp(8),0,dp(8),0)
            setOnClickListener { click() }
        },LinearLayout.LayoutParams(-1,dp(48)).apply { topMargin=dp(4) })
    }
    private fun demo(s:String) { MaterialAlertDialogBuilder(this).setTitle("Demo preview").setMessage(s).setPositiveButton("Done",null).show() }
    private fun people(title:String,recent:Boolean) {
        val section=card(); section.addView(text(title,16f,true))
        val cards=line()
        listOf("Nana Name","Neno Meme","Nana Name").forEachIndexed { i,name ->
            val person=column().apply { gravity=Gravity.CENTER; background=gradient("#FFF5F0","#F0F6F3",20); setPadding(dp(8),dp(8),dp(8),dp(8)) }
            person.addView(portrait(i,60)); person.addView(text(if(recent) "Recent Calls" else name,12f,true))
            if(!recent) {
                person.addView(text(if(i==1) "● Offline" else "● Online",11f,false,if(i==1) Color.GRAY else Color.rgb(57,137,85)))
                action(person,"Quick Call") { if(i==1) demo("This sample profile is offline.") else render("Call") }
            } else person.setOnClickListener { render("History") }
            cards.addView(person,LinearLayout.LayoutParams(dp(120),-2).apply { marginEnd=dp(8) })
        }
        section.addView(HorizontalScrollView(this).apply { isHorizontalScrollBarEnabled=false; addView(cards) })
    }
    private fun maleHome() {
        people("Available Women",false); people("Recommended users",true)
        val quick=line()
        val support=column(); val call=column()
        quick.addView(support,LinearLayout.LayoutParams(0,-2,1f).apply { marginEnd=dp(8) })
        quick.addView(call,LinearLayout.LayoutParams(0,-2,1f))
        action(support,"Support") { openProfile("Help") }
        action(call,"Quick Call") { render("Call") }
        body.addView(quick)
    }
    private fun femaleHome() {
        val balance=card(); balance.addView(text("Coin earnings",13f)); balance.addView(text("🪙 1100 Coins",24f,true))
        balance.addView(text("Notifications (1)",16f,true))
        balance.addView(text("☎  Call notifications\n     Connection notifications",14f))
        balance.addView(text("☎  Call notifications\n     Incoming call notifications",14f))
        val recent=card(); recent.addView(text("Recent Callers",17f,true))
        for(i in 0..2) personRow(recent,i,"Nana Name","Call received","+120 Coins\nper call") { render("History") }
        action(card(),"Upcoming Scheduled Calls  ›") { demo("Upcoming scheduled calls will appear here. No live calls are scheduled in this preview.") }
    }
    private fun personRow(parent:LinearLayout,i:Int,name:String,detail:String,end:String,click:()->Unit) {
        val row=line().apply { minimumHeight=dp(64); setPadding(0,dp(6),0,dp(6)); setOnClickListener { click() } }
        row.addView(portrait(i,40)); row.addView(text("$name\n$detail",13f,true),LinearLayout.LayoutParams(0,-2,1f))
        row.addView(text(end,12f,false,if(female) Color.rgb(76,132,72) else teal))
        parent.addView(row)
        parent.addView(android.view.View(this).apply { setBackgroundColor(Color.parseColor("#E9F0F2")) },LinearLayout.LayoutParams(-1,dp(1)))
    }
    private fun history() {
        val list=card(); list.addView(text(if(female) "Calls received" else "Calls made/received",16f,true))
        for(i in 0 until if(female) 6 else 8) personRow(list,i,"Nana Name",if(female) "✓ received" else if(i==3) "↙ received" else "↗ Calls made",
            if(female) "+120\nCoins" else listOf("2:37 AM","3:08 AM","3:33 AM","5:30 AM")[i%4]+"\nDuration") { demo("Sample call history. No live call record is connected.") }
    }
    private fun wallet() {
        val offers=card(); offers.addView(text("Coin package",17f,true))
        val packages=line()
        listOf("Starter\n₹100","Popular\n₹300","Pro\n₹300").forEach { label ->
            val pack=column().apply { gravity=Gravity.CENTER; background=bg(Color.parseColor("#F2F9F9"),12); setPadding(dp(6),dp(12),dp(6),dp(12)) }
            pack.addView(text("🪙",28f)); pack.addView(text(label,17f,true).apply { gravity=Gravity.CENTER })
            action(pack,"Select") { demo("$label selected for preview only.") }
            packages.addView(pack,LinearLayout.LayoutParams(0,-2,1f).apply { marginEnd=dp(6) })
        }
        offers.addView(packages)
        body.addView(text("Payment method",17f,true))
        val payment=card(); payment.addView(text("PayPal                  VISA   ●●",18f,true,teal))
        action(payment,"Secure payment") { demo("Payment preview only. No payment will be taken.") }
        payment.addView(text("✓ Secure payment",13f))
    }
    private fun withdraw() {
        val balance=card(); balance.addView(text("Available\n₹120 Coins\nBalance",22f,true).apply { gravity=Gravity.CENTER })
        balance.addView(text("Available                         30 Coins\nPending                           -0 Coins\nWithdrawable                    ₹20 Coins",16f))
        action(balance,"Withdraw") { demo("Withdrawal preview only. No money will move.") }
        balance.addView(text("Bank / UPI details",13f).apply { gravity=Gravity.CENTER; minHeight=dp(48); setOnClickListener { openProfile("Payment methods") } })
    }
    private fun more() {
        val menu=card()
        action(menu,"Admin console (authorized accounts)") {
            startActivity(Intent(this,com.menu.app.admin.AdminActivity::class.java))
        }
        if(com.menu.app.BuildConfig.DEBUG) action(menu,"Dummy admin demo") {
            startActivity(Intent(this,com.menu.app.admin.AdminActivity::class.java).putExtra("demo_admin",true))
        }
        val entries=listOf("♙  Profile" to "Personal information","▣  Payments/Earnings" to "Wallet","?  Support" to "Help","◇  Safety" to "Security","♧  Notifications" to "Notifications","◎  Language" to "Preferences")
        entries.forEach { (label,destination) ->
            val view=text("$label   ›",16f,true).apply {
                minimumHeight=dp(52); gravity=Gravity.CENTER_VERTICAL
                background=bg(Color.WHITE,8); elevation=dp(1).toFloat()
                setOnClickListener { if(destination=="Wallet") render("Wallet") else openProfile(destination) }
            }
            menu.addView(view,LinearLayout.LayoutParams(-1,-2).apply { bottomMargin=dp(8) })
        }
        val logout=text("⇥  Logout",16f,true).apply {
            minHeight=dp(52); gravity=Gravity.CENTER_VERTICAL
            setOnClickListener { MaterialAlertDialogBuilder(this@ReferenceActivity).setTitle("Log out?").setNegativeButton("Cancel",null).setPositiveButton("Log out") { _,_ ->
                AuthServices.repository.logout()
                startActivity(Intent(this@ReferenceActivity,LoginActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)); finish()
            }.show() }
        }; menu.addView(logout)
    }
    private fun nav() {
        val bar=line().apply {
            background=gradient("#FFFFFFFF","#F8FAF8",18)
            elevation=dp(3).toFloat()
            setPadding(dp(4),dp(4),dp(4),dp(4))
        }
        val icons=intArrayOf(R.drawable.ic_home,R.drawable.ic_discover,R.drawable.ic_phone_otp,R.drawable.ic_wallet,R.drawable.ic_profile)
        listOf("Home" to "Home","Discover" to "Discover","Calls" to "History","Wallet" to "Wallet","Profile" to "More").forEachIndexed { i,(title,destination) ->
            val item=column().apply {
                gravity=Gravity.CENTER; minimumHeight=dp(48); contentDescription=title
                isSelected=screen==destination
                if(isSelected) background=bg(Color.parseColor("#E1EEEA"),18)
                setOnClickListener { render(destination) }
            }
            item.addView(ImageView(this).apply {
                setImageResource(icons[i]); setColorFilter(if(screen==destination) teal else Color.rgb(102,117,127))
            },LinearLayout.LayoutParams(dp(20),dp(20)))
            item.addView(text(title,10f,false,teal))
            bar.addView(item,LinearLayout.LayoutParams(0,-2,1f))
        }; root.addView(bar,LinearLayout.LayoutParams(-1,-2).apply {
            setMargins(dp(12),dp(2),dp(12),dp(6))
        })
    }
    private fun openProfile(target:String) { startActivity(Intent(this,ProfileActivity::class.java).putExtra("page",target)) }
    private fun call() {
        root.background=gradient("#325B61","#241E35")
        window.statusBarColor=Color.parseColor("#325B61")
        androidx.core.view.WindowCompat.getInsetsController(window, root).isAppearanceLightStatusBars=false
        body=column().apply { gravity=Gravity.CENTER_HORIZONTAL; setPadding(dp(24),dp(24),dp(24),dp(24)) }
        root.addView(ScrollView(this).apply { addView(body) },LinearLayout.LayoutParams(-1,0,1f))
        body.addView(text("⌄                         Call preview",14f,false,Color.WHITE).apply { setOnClickListener { render("Home") } })
        body.addView(portrait(1,128),LinearLayout.LayoutParams(dp(128),dp(128)).apply { topMargin=dp(48) })
        body.addView(text("Nana Name",24f,true,Color.WHITE))
        body.addView(text("Status",14f,false,Color.WHITE))
        body.addView(text("◷ 20.0 min     🪙 Coin/minute",14f,false,Color.WHITE))
        val controls=line()
        listOf("Mute" to R.drawable.ic_phone_otp,"Speaker" to R.drawable.ic_speaker,"Video" to R.drawable.ic_video).forEach { (title,icon) ->
            val control=column().apply { gravity=Gravity.CENTER }
            control.addView(ImageButton(this).apply {
                setImageResource(icon); setColorFilter(Color.WHITE); contentDescription=title
                background=bg(Color.parseColor("#507C90"),40); setPadding(dp(16),dp(16),dp(16),dp(16))
                setOnClickListener { demo("$title is a preview control. No live call is active.") }
            },LinearLayout.LayoutParams(dp(54),dp(54)))
            control.addView(text(title,12f,false,Color.WHITE))
            controls.addView(control,LinearLayout.LayoutParams(0,-2,1f))
        }
        body.addView(controls,LinearLayout.LayoutParams(-1,-2).apply { topMargin=dp(80) })
        body.addView(ImageButton(this).apply {
            setImageResource(R.drawable.ic_end_call); setColorFilter(Color.WHITE)
            contentDescription="End call"; background=bg(Color.parseColor("#E84C60"),40)
            setPadding(dp(16),dp(16),dp(16),dp(16)); setOnClickListener { render("Home") }
        },LinearLayout.LayoutParams(dp(58),dp(58)).apply { topMargin=dp(24) })
    }
}
