package com.dsk.myexpense.expense_module.util.headerbar

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import com.dsk.myexpense.databinding.HeaderBarViewBinding

class HeaderBarView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : ConstraintLayout(context, attrs) {

    private val binding: HeaderBarViewBinding = HeaderBarViewBinding.inflate(
        LayoutInflater.from(context), this, true
    )

    fun setLeftIcon(resourceId: Int?) {
        if (resourceId != null) {
            binding.leftIcon.setImageResource(resourceId)
            binding.leftIcon.visibility = VISIBLE
        } else {
            binding.leftIcon.visibility = GONE
        }
    }

    fun setRightIcon(resourceId: Int?) {
        if (resourceId != null) {
            binding.rightIcon.setImageResource(resourceId)
            binding.rightIcon.visibility = VISIBLE
        } else {
            binding.rightIcon.visibility = GONE
        }
    }

    fun setHeaderTitle(title: String) {
        binding.headerTitle.text = title
    }

    fun setLeftIconVisibility(isVisible: Boolean) {
        binding.leftIcon.visibility = if (isVisible) VISIBLE else GONE
    }

    fun setRightIconVisibility(isVisible: Boolean) {
        binding.rightIcon.visibility = if (isVisible) VISIBLE else GONE
    }

    fun setOnLeftIconClickListener(listener: OnClickListener) {
        binding.leftIcon.setOnClickListener(listener)
    }

    fun setOnRightIconClickListener(listener: OnClickListener) {
        binding.rightIcon.setOnClickListener(listener)
    }
}