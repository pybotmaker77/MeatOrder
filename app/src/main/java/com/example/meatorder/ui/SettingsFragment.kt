package com.example.meatorder.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.meatorder.R
import com.example.meatorder.databinding.FragmentSettingsBinding

class SettingsFragment : Fragment() {
    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnFont.setOnClickListener {
            findNavController().navigate(R.id.action_settingsFragment_to_fontSettingsFragment)
        }
        binding.btnColors.setOnClickListener {
            findNavController().navigate(R.id.action_settingsFragment_to_colorSettingsFragment)
        }
        binding.btnDirectories.setOnClickListener {
            findNavController().navigate(R.id.action_settingsFragment_to_directoriesFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
