package com.dsk.myexpense.expense_module.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.dsk.myexpense.databinding.ItemSettingBinding
import com.dsk.myexpense.expense_module.data.model.Setting
import com.dsk.myexpense.expense_module.data.model.SettingType

class SettingsAdapter(
    private val onSettingChanged: (Setting) -> Unit
) : RecyclerView.Adapter<SettingsAdapter.SettingsViewHolder>() {

    private var settings: List<Setting> = emptyList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SettingsViewHolder {
        val binding = ItemSettingBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SettingsViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SettingsViewHolder, position: Int) {
        val setting = settings[position]
        holder.bind(setting, onSettingChanged)
    }

    override fun getItemCount(): Int = settings.size

    fun submitList(newSettings: List<Setting>) {
        settings = newSettings
        notifyDataSetChanged()
    }

    inner class SettingsViewHolder(private val binding: ItemSettingBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(setting: Setting, onSettingChanged: (Setting) -> Unit) {
            binding.settingName.text = setting.name
            binding.settingValue.text = setting.value

            // Example of a switch toggle for Dark Mode
            if (setting.type == SettingType.DARK_MODE) {
                binding.settingSwitch.isChecked = setting.value.toBoolean()
                binding.settingSwitch.setOnCheckedChangeListener { _, isChecked ->
                    onSettingChanged(setting.copy(value = isChecked.toString()))
                }
            }
        }
    }
}
