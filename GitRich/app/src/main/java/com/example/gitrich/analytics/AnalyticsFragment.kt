package com.example.gitrich.analytics

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import com.example.gitrich.R

/**
 * A simple [Fragment] subclass.
 * Use the [AnalyticsFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class AnalyticsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        var view = inflater.inflate(R.layout.fragment_analytics, container, false)

        var expenseByCategoryFragment = ExpenseByCategory()
        var expenseByMonthFragment = ExpenseByMonth()

        makeCurrentFragment(expenseByCategoryFragment)


        var categoryButton = view.findViewById<Button>(R.id.byCategory)
        var expenseButton = view.findViewById<Button>(R.id.byMonth)

        categoryButton.setOnClickListener {
            makeCurrentFragment(expenseByCategoryFragment)
        }

        expenseButton.setOnClickListener {
            makeCurrentFragment(expenseByMonthFragment)
        }

        return view
    }

    private fun makeCurrentFragment(fragment: Fragment) =
        requireActivity().supportFragmentManager.beginTransaction().apply{
            replace(R.id.fl_wrapper_child, fragment)
            commit()
        }
}