package com.example.meatorder.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.meatorder.databinding.FragmentDirectoriesBinding

class DirectoriesFragment : Fragment() {
    private var _binding: FragmentDirectoriesBinding? = null
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDirectoriesBinding.inflate(inflater, container, false)
        return binding.root
    }
}
