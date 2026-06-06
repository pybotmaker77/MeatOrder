package com.example.meatorder.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.meatorder.R
import com.example.meatorder.databinding.FragmentRemainsBinding

class RemainsFragment : Fragment() {
    private var _binding: FragmentRemainsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRemainsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.fabContinue.setOnClickListener {
            // Заглушка – переход к Order2 с пустыми остатками
            val bundle = Bundle().apply {
                putBoolean("byBalance", true)
                putIntArray("templateIds", intArrayOf())
                putIntArray("preSelectedIds", intArrayOf())
            }
            findNavController().navigate(R.id.action_remainsFragment_to_order2Fragment, bundle)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
