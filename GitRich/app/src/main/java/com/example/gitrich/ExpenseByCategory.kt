package com.example.gitrich

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.skydoves.powerspinner.PowerSpinnerView
import org.json.JSONObject

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [ExpenseByCategory.newInstance] factory method to
 * create an instance of this fragment.
 */
class ExpenseByCategory : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null
    private lateinit var username:String
    private lateinit var records:JSONObject

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_expense_by_category, container, false)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        getAnalytics()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun getAnalytics () {
        username = MySingleton.getUsername()
        val url = "http://ec2-18-136-119-32.ap-southeast-1.compute.amazonaws.com:8000/users/${username}/analytics/category"

        val jsonObjectRequest = JsonObjectRequest(
            Request.Method.GET, url, null,
            { response ->
                if (response.getInt("code") == 200) {
                    records = response.getJSONObject("data")

                    setSpinner(records)
                }
            },

            { error ->
                // TODO: Handle error
            }
        )

        MySingleton.getInstance(requireActivity()).addToRequestQueue(jsonObjectRequest)
    }


    private fun setSpinner(data:JSONObject) {
        val view = getView()

        var months = data.names()
        val spinner = view?.findViewById<PowerSpinnerView>(R.id.spinner)

        val monthList = ArrayList<String>()

        for (i in 0 until months.length()) {
            val keys: String = months.getString(i)
            monthList.add(keys)
        }

        if (spinner != null) {
            spinner.setItems(monthList)
            if (monthList.size > 0) {
                spinner.selectItemByIndex(0)

            }
            spinner.setOnSpinnerItemSelectedListener<String> { oldIndex, oldItem, newIndex, newItem ->
                setPieChart(newItem)
            }
        }

        if (monthList.size > 0) {
            setPieChart(monthList[0])
        }


    }

    private fun setPieChart(month: String) {
        var pieChart = requireView().findViewById<PieChart>(R.id.pieChart1)

        val pieEntries: ArrayList<PieEntry> = ArrayList()

        //initializing data
        val typeAmountMap: MutableMap<String, Int> = HashMap()

        var latest = records.getJSONObject(month)
        var keys = latest.names()

        for (i in 0 until keys.length()) {
            val keys: String = keys.getString(i)
            val value: Int = latest.getInt(keys)
            typeAmountMap[keys] = value
        }

        //initializing colors for the entries
        val colors: ArrayList<Int> = ArrayList()
        colors.add(Color.parseColor("#304567"))
        colors.add(Color.parseColor("#309967"))
        colors.add(Color.parseColor("#476567"))
        colors.add(Color.parseColor("#890567"))
        colors.add(Color.parseColor("#a35567"))
        colors.add(Color.parseColor("#ff5f67"))
        colors.add(Color.parseColor("#3ca567"))

        //input data and fit data into pie chart entry
        for (type in typeAmountMap.keys) {
            pieEntries.add(PieEntry(typeAmountMap[type]!!.toFloat(), type))
        }

        //collecting the entries with label name

        val pieDataSet = PieDataSet(pieEntries, "")
        //setting text size of the value
        pieDataSet.valueTextSize = 14f
        pieDataSet.xValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE
        //providing color list for coloring different entries
        pieDataSet.colors = colors
        //grouping the data set from entry to chart
        val pieData = PieData(pieDataSet)
        //showing the value of the entries, default true if not set
        pieData.setDrawValues(true)

        pieChart.holeRadius = 48f

//        pieChart.setBackgroundColor(Color)
        pieChart.description.isEnabled = false
        pieChart.data = pieData
        pieChart.setEntryLabelColor(Color.BLACK)
        pieChart.setExtraOffsets(26f,26f,26f,26f)
        pieChart.invalidate()
        pieChart.animateXY(2000,2000)

    }

}