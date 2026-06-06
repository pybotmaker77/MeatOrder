package com.example.meatorder.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.meatorder.R
import com.example.meatorder.databinding.FragmentOrder4Binding
import com.example.meatorder.utils.getPrefs

class Order4Fragment : Fragment() {
    private var _binding: FragmentOrder4Binding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOrder4Binding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val finalText = arguments?.getString("finalItemsJson") ?: "Нет данных"
        binding.tvFinalText.text = finalText

        binding.btnCopy.setOnClickListener {
            val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("order", finalText))
            Toast.makeText(requireContext(), "Текст скопирован", Toast.LENGTH_SHORT).show()
            getPrefs().clearDraft()
        }

        binding.btnSend.setOnClickListener {
            val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, finalText)
                type = "text/plain"
            }
            startActivity(Intent.createChooser(sendIntent, "Отправить заказ"))
            getPrefs().clearDraft()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
