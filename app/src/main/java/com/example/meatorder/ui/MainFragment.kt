package com.example.meatorder.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.meatorder.R
import com.example.meatorder.databinding.FragmentMainBinding
import com.example.meatorder.utils.applyFontSize
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
        applyFontSize(binding.root, prefs.fontSize)

        binding.btnOrder.setOnClickListener {
            findNavController().navigate(R.id.action_mainFragment_to_order1Fragment)
        }
        binding.btnSettings.setOnClickListener {
            findNavController().navigate(R.id.action_mainFragment_to_settingsFragment)
        }

        if (!prefs.draftOrderJson.isNullOrEmpty()) {
            val dialog = AlertDialog.Builder(requireContext())
                .setTitle("Незавершённый заказ")
                .setMessage("У вас есть незавершённый заказ. Продолжить?")
                .setPositiveButton("Да") { _, _ ->
                    val draftJson = prefs.draftOrderJson
                    val bundle = Bundle().apply {
                        putBoolean("byBalance", false)
                        putIntArray("templateIds", intArrayOf())
                        putString("initialItemsJson", draftJson) // передаём как предзаполнение
                    }
                    findNavController().navigate(R.id.action_mainFragment_to_order2Fragment, bundle)
                }
                .setNegativeButton("Нет") { _, _ ->
                    prefs.clearDraft()
                }
                .create()
            dialog.setOnShowListener { dialogInterface ->
                (dialogInterface as? AlertDialog)?.let {
                    it.window?.decorView?.let { rootView ->
                        applyFontSize(rootView, prefs.fontSize)
                    }
                    it.getButton(AlertDialog.BUTTON_POSITIVE)?.let { btn ->
                        applyFontSize(btn, prefs.fontSize)
                    }
                    it.getButton(AlertDialog.BUTTON_NEGATIVE)?.let { btn ->
                        applyFontSize(btn, prefs.fontSize)
                    }
                }
            }
            dialog.show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
