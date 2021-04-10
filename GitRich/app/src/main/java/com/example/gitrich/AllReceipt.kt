package com.example.gitrich

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import com.example.gitrich.models.Receipt

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [AllReceipt.newInstance] factory method to
 * create an instance of this fragment.
 */

class AllReceipt : Fragment() {
    // TODO: Rename and change types of parameters

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            receipts = it.getSerializable("receipts") as ArrayList<Receipt>
        }

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_all_receipt, container, false)

        val listView = view.findViewById<ListView>(R.id.receipt_all_list)
        listView.adapter = receipts_summary.CustomAdapter(requireActivity(), receipts)

        listView.setOnItemClickListener { parent, view, position, id ->
            val receipt = receipts[position];
            (context as MainActivity).makeCurrentFragment(receipt_details.newInstance(receipt), "receipt_details")
        }

        return view
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment AllReceipt.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(receipts: ArrayList<Receipt>) =
            AllReceipt().apply {
                arguments = Bundle().apply {
                    putSerializable("receipts", receipts)
                }
            }
    }
}