package com.example.gitrich.analytics

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.example.gitrich.R
import com.example.gitrich.request.RequestQueueSingleton
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter

/**
 * A simple [Fragment] subclass.
 * Use the [ExpenseByMonth.newInstance] factory method to
 * create an instance of this fragment.
 */
class ExpenseByMonth : Fragment() {
    private lateinit var username:String

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_expense_by_month, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        getAnalytics()
    }

    private fun getAnalytics () {
        username = RequestQueueSingleton.getUsername()
        val url = "http://ec2-18-136-119-32.ap-southeast-1.compute.amazonaws.com:8000/users/${username}/analytics/expense"

        val jsonObjectRequest = JsonObjectRequest(
            Request.Method.GET, url, null,
            { response ->

                if (response.getInt("code") == 200) {
                    var res = response.getJSONObject("data")

                    val view = view

                    val xValue = ArrayList<String>()
                    var months = res.names()

                    val lineEntry = ArrayList<Entry>()

                    for (i in 0 until months.length()) {
                        val keys: String = months.getString(i)
                        val amount: Int = res.getInt(keys)
                        xValue.add(keys)
                        lineEntry.add(
                            Entry(
                                i.toFloat(),
                                amount.toFloat()
                            )
                        )
                    }


                    val expenseChart = view?.findViewById<LineChart>(R.id.expenseChart)
                    if (expenseChart != null) {
                        expenseChart.setTouchEnabled(true)
                        expenseChart.setPinchZoom(true)

                        val yAxisRight = expenseChart.axisRight
                        yAxisRight.isEnabled = false

                        val xAxis = expenseChart.xAxis
                        xAxis.position = XAxis.XAxisPosition.BOTTOM
                        xAxis.setDrawGridLines(false)
                        xAxis.setLabelCount(months.length(), true)
                        xAxis.valueFormatter = IndexAxisValueFormatter(xValue)
                        xAxis.labelRotationAngle = 315f
                    }

                    val lineDataSet = LineDataSet(lineEntry, "Monthly Expense")
                    lineDataSet.color = Color.BLUE

                    val lineData = LineData(lineDataSet)
                    expenseChart?.description?.isEnabled = false
                    expenseChart?.data = lineData
                    expenseChart?.setBackgroundColor(Color.WHITE)
                    expenseChart?.animateXY(1500, 1500)
                    expenseChart?.invalidate()
                }
            },

            { error ->
                // TODO: Handle error
            }
        )

        RequestQueueSingleton.getInstance(requireActivity()).addToRequestQueue(jsonObjectRequest)
    }

}