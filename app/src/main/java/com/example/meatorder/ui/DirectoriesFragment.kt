package com.example.meatorder.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.meatorder.R
import com.example.meatorder.databinding.FragmentDirectoriesBinding
import com.example.meatorder.utils.getPrefs

class DirectoriesFragment : Fragment() {
    private var _binding: FragmentDirectoriesBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDirectoriesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val header = binding.root.findViewById<androidx.appcompat.widget.Toolbar>(R.id.header)
        header?.setNavigationOnClickListener { findNavController().popBackStack() }
        header?.setBackgroundColor(getPrefs().headerColor)

        binding.btnEntities.setOnClickListener { navigateToEditor("entities") }
        binding.btnMinOrder.setOnClickListener { navigateToEditor("min_order") }
        binding.btnTemplates.setOnClickListener { navigateToEditor("templates") }
        binding.btnPatterns.setOnClickListener { navigateToEditor("patterns") }
        binding.btnInputTypes.setOnClickListener { navigateToEditor("input_types") }
    }

    private fun navigateToEditor(dict: String) {
        val bundle = Bundle().apply {
            putString("dict", dict)
        }
        findNavController().navigate(R.id.action_directoriesFragment_to_directoryEditFragment, bundle)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
