package com.amoebasoft.lifepilotwear.presentation

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.GradientDrawable
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.transition.Scene
import android.transition.Slide
import android.transition.Transition
import android.transition.TransitionManager
import android.util.AttributeSet
import android.util.Log
import android.view.GestureDetector
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.viewpager2.widget.ViewPager2
import com.amoebasoft.lifepilotwear.R
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Node
import com.google.android.gms.wearable.Wearable
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.abs

class MainActivity : AppCompatActivity(), View.OnClickListener, SensorEventListener, GestureDetector.OnGestureListener {

    //Initialize Sensor Data
    private val ALPHA = 0.8f
    private val STEP_THRESHOLD = 8
    private val STEP_DELAY_NS = 250000000
    private lateinit var mSensorManager : SensorManager
    private var mHeartRateSensor : Sensor ?= null
    private var mStepDetectSensor : Sensor ?= null
    private val PERMISSION_BODY_SENSORS = Manifest.permission.BODY_SENSORS
    private var lastStepTimeNs: Long = 0
    private var stepCount: Int = 0
    private var start = 0
    private var isSensorScreen = false
    //timer stopwatch data
    private lateinit var handler: Handler
    private lateinit var runnable: Runnable
    private var startTime: Long = 0
    private var isRunning = false
    private var elapsedTime = 0L
    //running timer data
    private lateinit var runninghandler: Handler
    private lateinit var runningrunnable: Runnable
    private var runningstartTime: Long = 0
    private var runningelapsedTime = 0L
    private var runningisRunning = false
    //back button
    private var backvariable = false
    //gesture data
    private var homeAnimation: Scene? = null
    lateinit var gestureDetector: GestureDetector
    private val sensorHandler = Handler(Looper.getMainLooper())
    var x2:Float = 0.0f
    var x1:Float = 0.0f
    //setting variables
    private var notif: Boolean = true
    private var routine: String = ""
    companion object {
        const val MIN_DISTANCE = 50
        private const val PERMISSION_REQUEST_BODY_SENSORS = 100
    }
    //sensor permission data
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Log.i("Permission: ", "Granted")
            findViewById<Button>(R.id.buttonRuntimePermission).visibility = View.GONE
        } else {
            Log.i("Permission: ", "Denied")
        }
    }
    override fun onAccuracyChanged(sensor: Sensor?, bpm: Int) {
        return
    }
    //Sensor Updates RealTime
    override fun onSensorChanged(event: SensorEvent?) {
        if (event != null) {
            if (event.sensor.type == Sensor.TYPE_HEART_RATE) {
                ViewPagerAdapter.heartRateSensorValue = event.values[0]
            }
            if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                val currentTimeNs = System.nanoTime()
                if (currentTimeNs - lastStepTimeNs >= STEP_DELAY_NS) {
                    // Low-pass filter to smooth out accelerometer data
                    val gravity = floatArrayOf(0f, 0f, 0f)
                    val linearAcceleration = floatArrayOf(0f, 0f, 0f)
                    gravity[0] = ALPHA * gravity[0] + (1 - ALPHA) * event.values[0]
                    gravity[1] = ALPHA * gravity[1] + (1 - ALPHA) * event.values[1]
                    gravity[2] = ALPHA * gravity[2] + (1 - ALPHA) * event.values[2]
                    linearAcceleration[0] = event.values[0] - gravity[0]
                    linearAcceleration[1] = event.values[1] - gravity[1]
                    linearAcceleration[2] = event.values[2] - gravity[2]
                    // Magnitude of the acceleration vector
                    val accelerationMagnitude = Math.sqrt(
                        (linearAcceleration[0] * linearAcceleration[0] +
                                linearAcceleration[1] * linearAcceleration[1] +
                                linearAcceleration[2] * linearAcceleration[2]).toDouble()
                    ).toFloat()
                    // Check for step and update
                    if (accelerationMagnitude > STEP_THRESHOLD) {
                        stepCount++
                        lastStepTimeNs = currentTimeNs
                        ViewPagerAdapter.accelSensorValue = stepCount
                    }
                }
            }
        }
    }
    fun requestPermission() {
        if (ContextCompat.checkSelfPermission(this, PERMISSION_BODY_SENSORS)
            == PackageManager.PERMISSION_GRANTED) {
            // Permission granted
            findViewById<Button>(R.id.buttonRuntimePermission).visibility = View.GONE
        } else if (ActivityCompat.shouldShowRequestPermissionRationale(this, PERMISSION_BODY_SENSORS)) {
            val builder = AlertDialog.Builder(this)
            builder.setMessage("This app requires BODY_SENSORS permission for particular features to work as expected.")
                .setTitle("Permission Required")
                .setCancelable(false)
                .setPositiveButton("Ok") { dialog, which ->
                    ActivityCompat.requestPermissions(this, arrayOf(PERMISSION_BODY_SENSORS), PERMISSION_REQUEST_BODY_SENSORS)
                    dialog.dismiss()
                }
                .setNegativeButton("Cancel") { dialog, which ->
                    dialog.dismiss()
                }
            builder.show()
        } else {
            requestPermissionLauncher.launch(PERMISSION_BODY_SENSORS)
        }
    }

    //OnStartup for App
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            setContentView(R.layout.home)
            sensorMethod()

            //settings saved inputs
            //notif
            //bluetooth
        }
        //gesture
        gestureDetector = GestureDetector(this, this)
        //Sensor Requirements
        mSensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        mHeartRateSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE)
        mStepDetectSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    }
    //Sensor start and Stops
    override fun onResume() {
        super.onResume()
        mSensorManager.registerListener(this, mHeartRateSensor, SensorManager.SENSOR_DELAY_NORMAL)
        mSensorManager.registerListener(this, mStepDetectSensor, SensorManager.SENSOR_DELAY_NORMAL)
    }
    override fun onPause() {
        super.onPause()
        mSensorManager.unregisterListener(this)
    }
    private val uiUpdateHandler = Handler(Looper.getMainLooper())
    private lateinit var uiUpdateRunnable: Runnable
    //Update Sensor UI with PageViewer from Sensor Updates
    @SuppressLint("NotifyDataSetChanged")
    private fun sensorMethod() {
            setContentView(R.layout.home)
            //permission recheck on load
            if (ContextCompat.checkSelfPermission(this, PERMISSION_BODY_SENSORS)
                == PackageManager.PERMISSION_GRANTED) {
                findViewById<Button>(R.id.buttonRuntimePermission).visibility = View.GONE
            }
            // Get ViewPager reference
            val viewPager = findViewById<ViewPager2>(R.id.viewPager)
            // Set adapter for ViewPager
            val images = mutableListOf(
                R.layout.quickdata,
                R.layout.sync,
                R.layout.buttons
            )
            val adapter = ViewPagerAdapter(images)
            viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                @SuppressLint("NotifyDataSetChanged")
                override fun onPageSelected(position: Int) {
                    super.onPageSelected(position)
                    //navigation update
                    when (position) {
                        0 -> {
                            findViewById<ImageView>(R.id.maindot1).visibility = View.VISIBLE
                            findViewById<ImageView>(R.id.maindot2).visibility = View.GONE
                            findViewById<ImageView>(R.id.maindot3).visibility = View.GONE
                            isSensorScreen = true
                            uiUpdateRunnable = Runnable {
                                adapter.notifyDataSetChanged()
                                viewPager.adapter = adapter
                                uiUpdateHandler.postDelayed(uiUpdateRunnable, 2000L)
                            }
                            // Start the UI update loop
                            uiUpdateHandler.postDelayed(uiUpdateRunnable, 2000L)
                        }
                        1 -> {
                            findViewById<ImageView>(R.id.maindot1).visibility = View.GONE
                            findViewById<ImageView>(R.id.maindot2).visibility = View.VISIBLE
                            findViewById<ImageView>(R.id.maindot3).visibility = View.GONE
                            if(isSensorScreen) {
                                uiUpdateHandler.removeCallbacksAndMessages(null)}
                            isSensorScreen = false
                        }
                        2 -> {
                            findViewById<ImageView>(R.id.maindot1).visibility = View.GONE
                            findViewById<ImageView>(R.id.maindot2).visibility = View.GONE
                            findViewById<ImageView>(R.id.maindot3).visibility = View.VISIBLE
                            isSensorScreen = false
                        }
                    }
                }
            })
            adapter.notifyDataSetChanged()
            viewPager.adapter = adapter
            if(start == 0) {
                start += 1
                viewPager.currentItem = 1
            }
            if(backvariable) {
                viewPager.setCurrentItem(2, false)
            }
            timeSet()
    }
    // Set home time
    fun timeSet() {
        val timeEdit = findViewById<TextView>(R.id.time)
        val formatter = DateTimeFormatter.ofPattern("HH:mm")
        val curTime = LocalDateTime.now().format(formatter)
        timeEdit.setText(curTime)
    }
    //timer settings
    fun timerSet() {
        handler = Handler(Looper.getMainLooper())
        startTime = if (!isRunning) {
            System.currentTimeMillis()
        } else {
            // When the timer is resumed, update the start time to maintain continuity
            System.currentTimeMillis() - elapsedTime
        }
        runnable = object : Runnable {
            override fun run() {
                val currentTime = System.currentTimeMillis()
                elapsedTime = currentTime - startTime
                //timer calculations
                val millis = elapsedTime % 1000
                val seconds = (elapsedTime / 1000 % 60).toInt()
                val minutes = (elapsedTime / (1000 * 60) % 60).toInt()
                val hours = (elapsedTime / (1000 * 60 * 60) % 24).toInt()
                val time = String.format("%02d:%02d:%02d:%03d", hours, minutes, seconds, millis)
                //update timer UI
                findViewById<TextView>(R.id.timerText).text = time
                if (isRunning) {
                    handler.postDelayed(this, 10)
                }
            }
        }
        handler.post(runnable)
    }
    fun runningtimerSet() {
        runninghandler = Handler(Looper.getMainLooper())
        runningstartTime = if (!runningisRunning) {
            System.currentTimeMillis()
        } else {
            // When the timer is resumed, update the start time to maintain continuity
            System.currentTimeMillis() - runningelapsedTime
        }
        runningrunnable = object : Runnable {
            override fun run() {
                val currentTime = System.currentTimeMillis()
                runningelapsedTime = currentTime - runningstartTime
                //timer calculations
                val seconds = (runningelapsedTime / 1000 % 60).toInt()
                val minutes = (runningelapsedTime / (1000 * 60) % 60).toInt()
                val hours = (runningelapsedTime / (1000 * 60 * 60) % 24).toInt()
                val time = String.format("%02d:%02d:%02d", hours, minutes, seconds)
                //using estimated steps than actual for now
                //val km = String.format("%.2f km", stepCount * 0.000762)
                val km = String.format("%.2f km", (runningelapsedTime * .000002))
                //update timer UI
                findViewById<TextView>(R.id.runningText).text = time
                findViewById<TextView>(R.id.runningkm).text = km
                if (runningisRunning) {
                    runninghandler.postDelayed(this, 10)
                }
            }
        }
        runninghandler.post(runningrunnable)
    }
    //OnClicks for buttons
    @SuppressLint("SetTextI18n")
    override fun onClick(view: View?) {
        val id = view?.id
        //sync buttons
        if(id == R.id.buttonSync) {
            findViewById<Button>(R.id.buttonSync).visibility = View.GONE
            findViewById<FrameLayout>(R.id.workoutstart).visibility = View.VISIBLE
            val gradientDrawable = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 60f
            }
            gradientDrawable.setColor(ContextCompat.getColor(this@MainActivity, R.color.darkgray))
            findViewById<Button>(R.id.workoutbutton1).background = gradientDrawable
            findViewById<Button>(R.id.workoutbutton2).background = gradientDrawable
            findViewById<Button>(R.id.workoutcompletebutton).background = gradientDrawable
            val gradientDrawable2 = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 60f
            }
            gradientDrawable2.setColor(ContextCompat.getColor(this@MainActivity, R.color.royalPurple))
            findViewById<Button>(R.id.finishbutton).background = gradientDrawable2
        }
        else if(id == R.id.checkbutton1) {
            editButtonInfo(findViewById<Button>(R.id.workoutbutton1), true)
        }
        else if(id == R.id.xbutton1) {
            editButtonInfo(findViewById<Button>(R.id.workoutbutton1), false)
        }
        else if(id == R.id.checkbutton2) {
            editButtonInfo(findViewById<Button>(R.id.workoutbutton2), true)
        }
        else if(id == R.id.xbutton2) {
            editButtonInfo(findViewById<Button>(R.id.workoutbutton2), false)
        }
        else if(id == R.id.finishbutton) {
            findViewById<Button>(R.id.buttonSync).visibility = View.VISIBLE
            findViewById<FrameLayout>(R.id.workoutstart).visibility = View.GONE
        }
        else if(id == R.id.buttonRuntimePermission) {
            requestPermission()
            findViewById<Button>(R.id.buttonRuntimePermission).visibility = View.GONE
        }
        //running buttons
        else if(id == R.id.buttonRunning) {
            setContentView(R.layout.running)
            timeSet()
            runningtimerSet()
            homeAnimation = Scene.getSceneForLayout(findViewById(R.id.runninglayout), R.layout.buttonsfake, this)
        }
        else if(id == R.id.runningbuttonplay) {
            if(findViewById<ImageView>(R.id.runningPlay).visibility == View.VISIBLE) {
                findViewById<ImageView>(R.id.runningPlay).visibility = View.GONE
                findViewById<ImageView>(R.id.runningPause).visibility = View.VISIBLE
                //start time
                runningisRunning = true
                runningtimerSet()
            } else {
                findViewById<ImageView>(R.id.runningPlay).visibility = View.VISIBLE
                findViewById<ImageView>(R.id.runningPause).visibility = View.GONE
                //pause time
                runningisRunning = false
            }
        }
        else if(id == R.id.runningResetbutton) {
            findViewById<ImageView>(R.id.runningPlay).visibility = View.VISIBLE
            findViewById<ImageView>(R.id.runningPause).visibility = View.GONE
            //pause and reset time
            runningisRunning = false
            runningtimerSet()
            runninghandler.postDelayed({
                //reset the timer after delay to finish tasks
                findViewById<TextView>(R.id.runningText).text = "00:00:00"
                //reset elapsed time
                runningelapsedTime = 0
            },50)
        }
        //timer buttons
        else if(id == R.id.buttonStopwatch) {
            setContentView(R.layout.timer)
            timeSet()
            timeSet()
            homeAnimation = Scene.getSceneForLayout(findViewById(R.id.timerlayout), R.layout.buttonsfake, this)
        }
        else if(id == R.id.Timerbuttonplay) {
            if(findViewById<ImageView>(R.id.TimerPlay).visibility == View.VISIBLE) {
                findViewById<ImageView>(R.id.TimerPlay).visibility = View.GONE
                findViewById<ImageView>(R.id.TimerPause).visibility = View.VISIBLE
                //start time
                isRunning = true
                timerSet()
            } else {
                findViewById<ImageView>(R.id.TimerPlay).visibility = View.VISIBLE
                findViewById<ImageView>(R.id.TimerPause).visibility = View.GONE
                //pause time
                isRunning = false
            }
        }
        else if(id == R.id.TimerResetbutton) {
            findViewById<ImageView>(R.id.TimerPlay).visibility = View.VISIBLE
            findViewById<ImageView>(R.id.TimerPause).visibility = View.GONE
            //pause and reset time
            isRunning = false
            timerSet()
            handler.postDelayed({
                //reset the timer after delay to finish tasks
                findViewById<TextView>(R.id.timerText).text = "00:00:00:000"
                //reset elapsed time
                elapsedTime = 0
            },50)
        }
        //user button
        else if(id == R.id.buttonUser) {
            setContentView(R.layout.user)
            timeSet()
            homeAnimation = Scene.getSceneForLayout(findViewById(R.id.userlayout), R.layout.buttonsfake, this)
        }
        //settings button
        else if(id == R.id.buttonSettings) {
            setContentView(R.layout.settings)
            timeSet()
            val gradientDrawable = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 60f
            }
            findViewById<Button>(R.id.settingsbutton1).background = gradientDrawable
            findViewById<Button>(R.id.settingsbutton2).background = gradientDrawable
            homeAnimation = Scene.getSceneForLayout(findViewById(R.id.settingslayout), R.layout.buttonsfake, this)
            //settings saved
            findViewById<Switch>(R.id.notifswitch1).isChecked = notif
        }
        else if(id == R.id.syncbuttonsettings) {
            //sync to bluetooth phone
            blueToothSync()
        }
        else if(id == R.id.notifswitch1) {
            notif = findViewById<Switch>(R.id.notifswitch1).isChecked == true
        }
        //if lost
        else {
            sensorMethod()
        }
    }
    fun editButtonInfo(view: View, check:Boolean){
        val gradientDrawable = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 60f
        }
        if (check) {gradientDrawable.setColor(ContextCompat.getColor(this@MainActivity, R.color.passGreen))}
        else {gradientDrawable.setColor(ContextCompat.getColor(this@MainActivity, R.color.deleteRed))}
        view.background = gradientDrawable
    }
    //gesture allowance for scrollview
    class CustomScrollView : ScrollView {
        private val mainActivityInstance: MainActivity
        constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
            this.mainActivityInstance = context as MainActivity
        }
        constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(context, attrs, defStyle) {
            this.mainActivityInstance = context as MainActivity
        }
        private var initialX = 0f
        private var initialY = 0f
        override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
            when (ev.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = ev.x
                    initialY = ev.y
                    mainActivityInstance.backvariable = true
                    return super.onInterceptTouchEvent(ev)
                }
                MotionEvent.ACTION_MOVE -> {
                    val deltaX = Math.abs(ev.x - initialX)
                    val deltaY = Math.abs(ev.y - initialY)
                    return if (deltaX > deltaY && deltaX > MIN_DISTANCE) {
                        mainActivityInstance.onBackSwipe()
                        false
                    } else {
                        super.onInterceptTouchEvent(ev)
                    }
                }
                else -> {
                    return super.onInterceptTouchEvent(ev)
                }
            }
        }
    }
    //on touch events for gesturing
    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        gestureDetector.onTouchEvent(event)
        return super.dispatchTouchEvent(event)
    }
    override fun onTouchEvent(event: MotionEvent): Boolean {
        gestureDetector.onTouchEvent(event)
        when (event.action){
            0->
            {
                x1 = event.x
            }
            1->
            {
                x2 = event.x
                val valueX:Float = x2-x1
                if(abs(valueX) > MIN_DISTANCE)
                {
                    if (x2 > x1)
                    {
                        onBackSwipe()
                        return true
                    }
                }
            }
        }
        return super.onTouchEvent(event)
    }
    fun onBackSwipe() {
        //in case timers running
        isRunning = false
        runningisRunning = false
        //go to home after delay with transition slide
        sensorHandler.postDelayed({
            window.setBackgroundDrawableResource(R.drawable.gradientblackbackground)
            val slide: Transition = Slide(Gravity.END)
            TransitionManager.go(homeAnimation, slide)
            timeSet()
        }, 50)
        sensorHandler.postDelayed({
            backvariable = true
            sensorMethod()
            backvariable = false
        }, 700)
    }
    override fun onDown(e: MotionEvent): Boolean {
        return false
    }
    override fun onShowPress(e: MotionEvent) {
    }
    override fun onSingleTapUp(e: MotionEvent): Boolean {
        return false
    }
    override fun onScroll(e1: MotionEvent?, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
        return false
    }
    override fun onLongPress(e: MotionEvent) {
    }
    override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
        return false
    }

    fun blueToothSync() {
        val messageClient: MessageClient = Wearable.getMessageClient(this)
        // Send data
        val data = (ViewPagerAdapter.btBPM + ViewPagerAdapter.btCAL + ViewPagerAdapter.btSteps).toByteArray()
        try {
            val nodes: List<Node> = Tasks.await(Wearable.getNodeClient(this).connectedNodes)
            var phoneNodeId: String? = null
            for (node in nodes) {
                if (node.isNearby) {
                    phoneNodeId = node.id
                    break
                }
            }
            if (phoneNodeId != null) {
                val sendMessageTask: Task<Int> = messageClient.sendMessage(phoneNodeId, "/weardata", data)
                sendMessageTask.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        // Message Sent Successfully
                        Toast.makeText(this, "Data Sent", Toast.LENGTH_SHORT)
                            .show()
                    } else {
                        // Failed to send message
                        Toast.makeText(this, "Bluetooth Error", Toast.LENGTH_SHORT)
                            .show()
                    }
                }
            } else {
                // No connected phone found
                Toast.makeText(this, "Phone not connected", Toast.LENGTH_SHORT)
                    .show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // exceptions
            Toast.makeText(this, "Error", Toast.LENGTH_SHORT)
                .show()
        }
        // Receive data
        messageClient.addListener(object : MessageClient.OnMessageReceivedListener {
            override fun onMessageReceived(messageEvent: MessageEvent) {
                val receivedData = String(messageEvent.data, Charsets.UTF_8)
                routine = receivedData
            }
        })
    }
}
