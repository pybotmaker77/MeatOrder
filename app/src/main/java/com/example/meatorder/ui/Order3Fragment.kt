package com.example.meatorder.ui

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.meatorder.R
import com.example.meatorder.data.entity.InputType
import com.example.meatorder.databinding.FragmentOrder3Binding
import com.example.meatorder.utils.getDao
import com.example.meatorder.utils.getPrefs
import com.example.meatorder.utils.showToast
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class Order3Fragment : Fragment() {
    private var _binding: FragmentOrder3Binding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: Order3Adapter
    private var items = mutableListOf<Order3Item>()
    private var inputTypes = listOf<InputType>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentOrder3Binding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val header = binding.root.findViewById<androidx.appcompat.widget.Toolbar>(R.id.header)
        header?.setNavigationOnClickListener { findNavController().popBackStack() }

        val selectedJson = arguments?.getString("selectedItemsJson") ?: return
        // ... (остальная логика, но с использованием selectedJson)
        // В конце навигация:
        binding.fabContinue.setOnClickListener {
            if (items.isEmpty()) {
                showToast("Список пуст")
            } else {
                val json = Gson().toJson(items.map {
                    mapOf(
                        "entity_id" to it.entityId,
                        "entity" to it.entity,
                        "group" to it.group,
                        "input_type" to it.inputType.type_name,
                        "quantity" to it.quantity
                    )
                })
                val bundle = Bundle().apply { putString("finalItemsJson", json) }
                findNavController().navigate(R.id.action_order3Fragment_to_order4Fragment, bundle)
            }
        }
    }
    // ... другие методы
}
