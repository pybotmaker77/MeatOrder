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
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.meatorder.R
import com.example.meatorder.databinding.FragmentOrder4Binding
import com.example.meatorder.utils.applyFontSize
import com.example.meatorder.utils.getDao
import com.example.meatorder.utils.getPrefs
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

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

        val header = binding.root.findViewById<androidx.appcompat.widget.Toolbar>(R.id.header)
        header?.setNavigationOnClickListener { findNavController().popBackStack() }
        header?.setBackgroundColor(getPrefs().headerColor)
        header?.title = "Итоговый заказ"
        applyFontSize(binding.root, getPrefs().fontSize)

        val json = arguments?.getString("finalItemsJson") ?: return
        val type = object : TypeToken<List<Map<String, Any>>>() {}.type
        val items: List<Map<String, Any>> = Gson().fromJson(json, type)

        lifecycleScope.launch {
            val inputTypes = getDao().getAllInputTypes().first()
            val activePattern = getDao().getAllPatterns().first().find { it.is_active }
            val patternStr = activePattern?.template ?: "- {entity} - {input} {input_type_short}.\n{summary}"
            val sb = StringBuilder()

            for (item in items) {
                val entity = item["entity"] as String
                val quantity = (item["quantity"] as Double).toInt()
                val inputTypeName = item["input_type"] as String
                val inputType = inputTypes.find { it.type_name == inputTypeName }
                val shortName = inputType?.short_name ?: inputTypeName

                val line = patternStr
                    .replace("{entity}", entity)
                    .replace("{input}", quantity.toString())
                    .replace("{input_type}", inputTypeName)
                    .replace("{input_type_short}", shortName)
                    .replace("{summary}", "")
                sb.appendLine(line)
            }

            val blocks = items.filter { it["input_type"] == "Блок" }.sumOf { (it["quantity"] as Double).toInt() }
            val bags = items.filter { it["input_type"] == "Мешок" }.sumOf { (it["quantity"] as Double).toInt() }
            val kg = items.filter { it["input_type"] == "Кг" }.sumOf { (it["quantity"] as Double).toInt() }
            val totalWeight = items.sumOf {
                val type = inputTypes.find { t -> t.type_name == it["input_type"] }?.weight_kg ?: 1.0
                (it["quantity"] as Double).toInt() * type
            }
            val summary = "Итого: Блоков: $blocks | Мешков: $bags | Кг: $kg\nОбщий вес: $totalWeight кг"
            val finalText = if (patternStr.contains("{summary}")) {
                sb.toString().replace("{summary}", summary)
            } else {
                sb.appendLine().appendLine(summary).toString()
            }.trimEnd()

            binding.tvFinalText.text = finalText

            binding.btnCopy.setOnClickListener {
                val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("order", finalText))
                Toast.makeText(requireContext(), "Текст скопирован", Toast.LENGTH_SHORT).show()
                getPrefs().clearDraft()
            }

            binding.btnSend.setOnClickListener {
                val sendIntent = Intent(Intent.ACTION_SEND)
                sendIntent.setType("text/plain")
                sendIntent.putExtra(Intent.EXTRA_TEXT, finalText)
                startActivity(Intent.createChooser(sendIntent, "Отправить заказ"))
                getPrefs().clearDraft()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
