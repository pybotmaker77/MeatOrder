package com.example.meatorder.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.meatorder.R
import com.example.meatorder.databinding.FragmentDirectoriesBinding

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

        binding.btnEntities.setOnClickListener {
            // Переход к редактированию номенклатуры (пока заглушка)
            findNavController().navigate(R.id.action_directoriesFragment_to_directoryEditFragment)
        }
        binding.btnTemplates.setOnClickListener {
            findNavController().navigate(R.id.action_directoriesFragment_to_directoryEditFragment)
        }
        binding.btnPatterns.setOnClickListener {
            findNavController().navigate(R.id.action_directoriesFragment_to_directoryEditFragment)
        }
        binding.btnInputTypes.setOnClickListener {
            findNavController().navigate(R.id.action_directoriesFragment_to_directoryEditFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
