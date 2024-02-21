package com.amoebasoft.lifepilotwear.presentation

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.text.Layout
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager.widget.PagerAdapter
import com.amoebasoft.lifepilotwear.R

class ViewPagerAdapter (
    var images: MutableList<Int>
) : RecyclerView.Adapter<ViewPagerAdapter.ViewPagerViewHolder>() {
    inner class ViewPagerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
    //Active layout selected on swipe
    private var active = R.layout.quickdata
    //Global variable to for bpm sensor
    companion object {
        var heartRateSensorValue: Float = 0f
        var accelSensorValue: Int = 0
        var calBurned: Float = 0f
        var extraCal: Float = 0f
    }
    //Create ViewPager
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewPagerViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(active, parent, false)
        return ViewPagerViewHolder(view)
    }
    //ViewPager Size
    override fun getItemCount(): Int {
        return images.size
    }
    override fun onBindViewHolder(holder: ViewPagerViewHolder, position: Int) {
        val curImage = images[position]

        //Main screens to slide on
        val activeLayoutRes = when (curImage) {
            R.layout.quickdata -> R.layout.quickdata
            R.layout.sync -> R.layout.sync
            R.layout.buttons -> R.layout.buttons
            else -> R.layout.sync // Default to sync layout if unknown
        }

        //Update ViewPager for new UI data
        val inflater = LayoutInflater.from(holder.itemView.context)
        val inflatedView = inflater.inflate(activeLayoutRes, holder.itemView as ViewGroup?, false)
        (holder.itemView as? ViewGroup)?.removeAllViews()
        (holder.itemView as? ViewGroup)?.addView(inflatedView)

        // Update UI info
        if (activeLayoutRes == R.layout.quickdata) {
            //sensor adjustments
            val bpmTextView = inflatedView.findViewById<TextView>(R.id.bpmtext)
            bpmTextView.text = heartRateSensorValue.toString()
            val kcalTextView = inflatedView.findViewById<TextView>(R.id.kcaltext)
            calBurned =  3.0f * 72.57f * (accelSensorValue / 1300.0f)
            extraCal = if (heartRateSensorValue > 99) {5.0f} else {0f}
            kcalTextView.text = (calBurned + extraCal).toString()
            val stepTextView = inflatedView.findViewById<TextView>(R.id.setpstext)
            stepTextView.text = accelSensorValue.toString()
        }
    }
}

