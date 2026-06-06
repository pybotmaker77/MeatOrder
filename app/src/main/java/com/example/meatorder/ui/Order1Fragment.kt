package com.example.meatorder.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.meatorder.R
import com.example.meatorder.databinding.FragmentOrder1Binding

class Order1Fragment : Fragment() {
    private var _binding: FragmentOrder1Binding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOrder1Binding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBalance.setOnClickListener {
            findNavController().navigate(R.id.action_order1Fragment_to_remainsFragment)
        }

        binding.btnFromScratch.setOnClickListener {
            findNavController().navigate(R.id.action_order1Fragment_to_order2Fragment)
        }

        binding.btnFromTemplate.setOnClickListener {
            // показываем toast как заглушку
            android.widget.Toast.makeText(requireContext(), "Выбор шаблона (в разработке)", android.widget.Toast.LENGTH_SHORT).show()
        }

        binding.btnTemplates.setOnClickListener {
            android.widget.Toast.makeText(requireContext(), "Шаблоны (в разработке)", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
