package com.example.gitrich

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IAxisValueFormatter
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter


// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [ExpenseByMonth.newInstance] factory method to
 * create an instance of this fragment.
 */
class ExpenseByMonth : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null
    private lateinit var username:String

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
        return inflater.inflate(R.layout.fragment_expense_by_month, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        getAnalytics()
    }

    private fun getAnalytics () {
        username = MySingleton.getUsername()
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


                    val expenseChart = view?.findViewById<LineChart>(R.id.expenseChart);
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

        MySingleton.getInstance(requireActivity()).addToRequestQueue(jsonObjectRequest)
    }

}