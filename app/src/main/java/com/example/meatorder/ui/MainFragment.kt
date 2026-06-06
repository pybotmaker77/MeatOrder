package com.example.meatorder.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.Fragment
import androidx.navigation.fragment.findNavController
import com.example.meatorder.R
import com.example.meatorder.databinding.FragmentMainBinding
import com.example.meatorder.utils.getPrefs

class MainFragment : Fragment() {
    private var _binding: FragmentMainBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMainBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val prefs = getPrefs()
        binding.btnOrder.setOnClickListener {
            findNavController().navigate(R.id.action_mainFragment_to_order1Fragment)
        }
        binding.btnSettings.setOnClickListener {
            findNavController().navigate(R.id.action_mainFragment_to_settingsFragment)
        }

        if (!prefs.draftOrderJson.isNullOrEmpty()) {
            AlertDialog.Builder(requireContext())
                .setTitle("Незавершенный заказ")
                .setMessage("У вас есть незавершенный заказ. Продолжить?")
                .setPositiveButton("Да") { _, _ ->
                    findNavController().navigate(R.id.action_mainFragment_to_order2Fragment)
                }
                .setNegativeButton("Нет") { _, _ ->
                    prefs.clearDraft()
                }
                .show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}