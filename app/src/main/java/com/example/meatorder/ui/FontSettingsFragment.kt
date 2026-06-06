package com.example.meatorder.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.meatorder.databinding.FragmentFontSettingsBinding
import com.example.meatorder.utils.getDao
import com.example.meatorder.utils.getPrefs
import kotlinx.coroutines.launch

class FontSettingsFragment : Fragment() {
    private var _binding: FragmentFontSettingsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFontSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.header.setNavigationOnClickListener { findNavController().popBackStack() }

        val prefs = getPrefs()
        var currentSize = prefs.fontSize
        binding.tvCurrentSize.text = "${currentSize}sp"
        binding.tvPreview.textSize = currentSize.toFloat()
        binding.btnPlus.setOnClickListener {
            currentSize++
            prefs.fontSize = currentSize
            binding.tvCurrentSize.text = "${currentSize}sp"
            binding.tvPreview.textSize = currentSize.toFloat()
        }
        binding.btnMinus.setOnClickListener {
            if (currentSize > 8) currentSize--
            prefs.fontSize = currentSize
            binding.tvCurrentSize.text = "${currentSize}sp"
            binding.tvPreview.textSize = currentSize.toFloat()
        }

        lifecycleScope.launch {
            getDao().getAllEntities().collect { list ->
                binding.tvPreview.text = list.joinToString("\n") { it.entity }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
