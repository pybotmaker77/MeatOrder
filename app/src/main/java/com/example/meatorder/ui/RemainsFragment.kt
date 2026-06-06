package com.example.meatorder.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.meatorder.R
import com.example.meatorder.databinding.FragmentRemainsBinding
import com.example.meatorder.utils.getDao
import kotlinx.coroutines.launch

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
        binding.header.setNavigationOnClickListener { findNavController().popBackStack() }

        val dao = getDao()
        lifecycleScope.launch {
            dao.getAllEntities().collect { entities ->
                val adapter = RemainsAdapter(entities) { _, _ -> }
                binding.recyclerRemains.layoutManager = LinearLayoutManager(requireContext())
                binding.recyclerRemains.adapter = adapter

                binding.fabContinue.setOnClickListener {
                    val quantities = adapter.getQuantities()
                    val emptyEntities = entities.filter {
                        it.id !in quantities.keys || quantities[it.id] == 0
                    }.map { it.id }.toIntArray()
                    val action = RemainsFragmentDirections.actionRemainsFragmentToOrder2Fragment(
                        byBalance = true,
                        templateIds = intArrayOf(),
                        preSelectedIds = emptyEntities
                    )
                    findNavController().navigate(action)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
