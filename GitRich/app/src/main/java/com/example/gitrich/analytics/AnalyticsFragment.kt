package com.example.gitrich.analytics

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import com.example.gitrich.R


// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [AnalyticsFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class AnalyticsFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null


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


//    @RequiresApi(Build.VERSION_CODES.O)
//    override fun onActivityCreated(savedInstanceState: Bundle?) {
//        super.onActivityCreated(savedInstanceState)
//        if (savedInstanceState != null) {
//            //Restore the fragment's state here
//
//        } else {
//
//        }
//    }







}