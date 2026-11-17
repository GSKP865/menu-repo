package com.menu.app.calling

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.WindowManager
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.menu.app.auth.LoginActivity
import com.menu.app.domain.auth.AuthServices
import com.menu.app.domain.auth.SessionStore
import io.agora.rtc2.*
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject

/** Two-party voice calling. The foreground service keeps Android alive while a call is backgrounded. */
class VoiceLobbyActivity : AppCompatActivity() {
    private val api = VoiceApi()
    private val uiMutex = Mutex()
    private lateinit var content: LinearLayout
    private lateinit var status: TextView
    private var poll: Job? = null
    private var engine: RtcEngine? = null
    private var joinedCall: String? = null
    private var activeCall: JSONObject? = null
    private var snapshot = JSONObject()
    private var muted = false
    private var speaker = false
    private var foreground = false
    private var pendingPermission: (() -> Unit)? = null
    private var failures = 0
    private var rtcStatus = ""
    private var ringtone: android.media.Ringtone? = null
    private var connectedAt: Long? = null
    private val callClock = Handler(Looper.getMainLooper())
    private val clockTick = object : Runnable {
        override fun run() {
            if (joinedCall != null && connectedAt != null) {
                renderCallClock()
                callClock.postDelayed(this, 1000)
            }
        }
    }
    private var callClockView: TextView? = null
    private val permission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        val next = pendingPermission
        pendingPermission = null
        if (granted) next?.invoke() else Toast.makeText(this, "Microphone permission is needed to call.", Toast.LENGTH_LONG).show()
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (AuthServices.repository.currentSession() == null) { signOut(); return }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val p = (20 * resources.displayMetrics.density).toInt()
            setPadding(p, p, p, p)
        }
        root.addView(TextView(this).apply {
            text = "Me’n’U · ${AuthServices.repository.currentSession()?.name}"
            textSize = 25f
        })
        root.addView(TextView(this).apply { text = "Keep this screen open on both phones for calls."; textSize = 14f })
        status = TextView(this).apply { text = "Connecting…"; textSize = 17f }
        root.addView(status)
        content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(ScrollView(this).apply { addView(content) }, LinearLayout.LayoutParams(-1, 0, 1f))
        root.addView(Button(this).apply { text = "Sign out"; setOnClickListener { signOut() } })
        setContentView(root)
    }
    override fun onStart() {
        super.onStart()
        if (!::content.isInitialized) return
        foreground = true
        poll = lifecycleScope.launch {
            update("online")
            while (isActive) {
                delay(3000)
                update("snapshot")
            }
        }
    }
    override fun onStop() {
        foreground = false
        poll?.cancel()
        stopRinging()
        super.onStop()
    }
    private fun signOut() {
        foreground = false
        poll?.cancel()
        stopRinging()
        leaveAudio()
        lifecycleScope.launch {
            uiMutex.withLock { runCatching { api.action("offline") } }
            AuthServices.repository.logout()
            startActivity(Intent(this@VoiceLobbyActivity, LoginActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
            finish()
        }
    }
    private fun microphone(next: () -> Unit) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) next()
        else { pendingPermission = next; permission.launch(Manifest.permission.RECORD_AUDIO) }
    }
    private fun act(action: String, target: String? = null, call: String? = null) {
        if (action == "create" || action == "accept") CallKeepAliveService.start(this)
        lifecycleScope.launch { update(action, target, call) }
    }
    private suspend fun update(action: String, target: String? = null, call: String? = null) = uiMutex.withLock {
        if (!foreground) return@withLock
        try {
            val result = api.action(action, target, call)
            if (!foreground) return@withLock
            failures = 0
            snapshot = result
            activeCall = result.optJSONObject("call")
            val current = activeCall
            if (current == null || current.optString("status") != "in_progress") {
                leaveAudio()
                if (current == null) CallKeepAliveService.stop(this@VoiceLobbyActivity)
            }
            else if (joinedCall != current.getString("id")) joinAudio(current.getString("id"))
            render()
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (e: Exception) {
            if (!foreground) return@withLock
            status.text = e.message ?: "Connection unavailable. Retrying…"
            if (++failures >= 3) {
                leaveAudio()
                runCatching { api.action("offline") }
            }
        }
    }
    private fun button(label: String, enabled: Boolean = true, action: () -> Unit) {
        content.addView(Button(this).apply { text = label; isAllCaps = false; isEnabled = enabled; setOnClickListener { action() } })
    }
    private fun render() {
        content.removeAllViews()
        val online = snapshot.optBoolean("online")
        status.text = if (online) "Online" else "Offline"
        val c = activeCall
        if (c != null) {
            val id = c.getString("id")
            val incoming = c.getString("receiver_id") == SessionStore.loadAuthUserId()
            val ringing = c.getString("status") == "ringing"
            if (ringing && incoming) {
                if (ringtone == null) {
                    ringtone = android.media.RingtoneManager.getRingtone(this,
                        android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_RINGTONE))
                    if (android.os.Build.VERSION.SDK_INT >= 28) ringtone?.isLooping = true
                    ringtone?.play()
                }
            } else stopRinging()
            content.addView(TextView(this).apply {
                textSize = 23f
                text = if (ringing) (if (incoming) "Incoming call from " else "Calling ") + c.getString("peer_name")
                    else "${c.getString("peer_name")}\n$rtcStatus"
            })
            if (!ringing) {
                callClockView = TextView(this).apply { textSize = 18f; setPadding(0, 8, 0, 12) }
                content.addView(callClockView)
                renderCallClock()
            } else callClockView = null
            if (ringing && incoming) {
                button("Accept") { microphone { act("accept", call = id) } }
                button("Reject") { act("reject", call = id) }
            } else {
                if (!ringing) {
                    button(if (muted) "Unmute" else "Mute") {
                        val next = !muted
                        if (engine?.muteLocalAudioStream(next) == 0) { muted = next; render() }
                    }
                    button(if (speaker) "Use earpiece" else "Use speaker") {
                        val next = !speaker
                        if (engine?.setEnableSpeakerphone(next) == 0) { speaker = next; render() }
                    }
                }
                button(if (ringing) "Cancel call" else "End call") {
                    leaveAudio(); act("end", call = id); CallKeepAliveService.stop(this)
                }
            }
        } else {
            stopRinging()
            button(if (online) "Go offline" else "Go online") { act(if (online) "offline" else "online") }
            val peers = snapshot.optJSONArray("peers")
            if (peers == null || peers.length() == 0) content.addView(TextView(this).apply { text = "No other users yet." })
            if (peers != null) for (i in 0 until peers.length()) {
                val peer = peers.getJSONObject(i)
                val available = peer.optBoolean("online") && !peer.optBoolean("busy")
                val label = if (peer.optBoolean("busy")) "Busy" else if (available) "Online" else "Offline"
                button("Call ${peer.getString("display_name")} · $label", online && available) {
                    microphone { act("create", target = peer.getString("id")) }
                }
            }
        }
    }
    private suspend fun joinAudio(id: String) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            api.action("end", call = id)
            throw IllegalStateException("Microphone permission required. Call ended.")
        }
        leaveAudio()
        val credentials = api.token(id)
        if (!foreground) return
        try {
            val handler = object : IRtcEngineEventHandler() {
                override fun onJoinChannelSuccess(channel: String?, uid: Int, elapsed: Int) = runOnUiThread {
                    if (joinedCall == id) { rtcStatus = "Waiting for the other participant…"; render() }
                }
                override fun onUserJoined(uid: Int, elapsed: Int) = runOnUiThread {
                    if (joinedCall == id) {
                        rtcStatus = "Connected"
                        connectedAt = SystemClock.elapsedRealtime()
                        callClock.removeCallbacks(clockTick)
                        callClock.post(clockTick)
                        render()
                    }
                }
                override fun onUserOffline(uid: Int, reason: Int) = runOnUiThread {
                    if (joinedCall == id && foreground) { leaveAudio(); act("end", call = id) }
                }
                override fun onError(err: Int) = runOnUiThread {
                    if (joinedCall == id && foreground) {
                        leaveAudio(); act("end", call = id)
                        Toast.makeText(this@VoiceLobbyActivity, "Agora connection error: $err", Toast.LENGTH_LONG).show()
                    }
                }
                override fun onTokenPrivilegeWillExpire(token: String?) { renew(id) }
                override fun onRequestToken() { renew(id) }
            }
            val rtc = RtcEngine.create(RtcEngineConfig().apply {
                mContext = applicationContext; mAppId = credentials.getString("app_id"); mEventHandler = handler
            })
            engine = rtc
            joinedCall = id
            rtcStatus = "Connecting audio…"
            rtc.enableAudio()
            rtc.setDefaultAudioRoutetoSpeakerphone(false)
            val result = rtc.joinChannel(credentials.getString("token"), credentials.getString("channel"), credentials.getInt("uid"), ChannelMediaOptions().apply {
                channelProfile = Constants.CHANNEL_PROFILE_COMMUNICATION
                clientRoleType = Constants.CLIENT_ROLE_BROADCASTER
                publishMicrophoneTrack = true
                autoSubscribeAudio = true
            })
            check(result == 0) { "Unable to join Agora ($result)" }
        } catch (e: Exception) {
            leaveAudio()
            runCatching { api.action("end", call = id) }
            throw e
        }
    }
    private fun renew(id: String) = runOnUiThread {
        lifecycleScope.launch {
            uiMutex.withLock {
                if (joinedCall != id || !foreground) return@withLock
                try {
                    val token = api.token(id).getString("token")
                    if (joinedCall == id && foreground) check(engine?.renewToken(token) == 0)
                } catch (cancelled: CancellationException) { throw cancelled }
                catch (_: Exception) { leaveAudio(); act("end", call = id) }
            }
        }
    }
    private fun leaveAudio() {
        callClock.removeCallbacks(clockTick)
        connectedAt = null
        callClockView = null
        joinedCall = null
        val old = engine
        engine = null
        if (old != null) { old.leaveChannel(); RtcEngine.destroy() }
        muted = false; speaker = false; rtcStatus = ""
    }
    private fun renderCallClock() {
        val elapsedSeconds = connectedAt?.let { ((SystemClock.elapsedRealtime() - it) / 1000).toInt() } ?: 0
        callClockView?.text = String.format("%02d:%02d", elapsedSeconds / 60, elapsedSeconds % 60)
    }
    private fun stopRinging() { ringtone?.stop(); ringtone = null }
}
