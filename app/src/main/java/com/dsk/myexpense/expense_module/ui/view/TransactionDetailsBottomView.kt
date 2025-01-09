package com.dsk.myexpense.expense_module.ui.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.core.graphics.drawable.toBitmap
import androidx.fragment.app.viewModels
import com.dsk.myexpense.R
import com.dsk.myexpense.databinding.TransactionDetailsBinding
import com.dsk.myexpense.databinding.TransactionDetailsItemViewBinding
import com.dsk.myexpense.expense_module.core.ExpenseApplication
import com.dsk.myexpense.expense_module.data.model.ExpenseDetails
import com.dsk.myexpense.expense_module.ui.viewmodel.AppLoadingViewModel
import com.dsk.myexpense.expense_module.ui.viewmodel.GenericViewModelFactory
import com.dsk.myexpense.expense_module.util.NotificationUtils
import com.dsk.myexpense.expense_module.util.Utility
import com.dsk.myexpense.expense_module.util.Utility.dp
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class TransactionDetailsBottomView(
    private val context: Context,
    private val expenseDetails: ExpenseDetails
) : BottomSheetDialogFragment() {

    private var binding: TransactionDetailsBinding? = null
    private val bindingView get() = binding!!
    private val appLoadingViewModel: AppLoadingViewModel by viewModels {
        GenericViewModelFactory {
            AppLoadingViewModel((requireActivity().application as ExpenseApplication).expenseRepository)
        }
    }

    private lateinit var expenseDate: String
    private lateinit var expenseTime: String

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = TransactionDetailsBinding.inflate(inflater, container, false)
        return bindingView.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bindExpenseDetails()
        loadCategoryIcon()

        bindingView.btnDownloadReceipt.setOnClickListener {
            generateAndDownloadPdf("${expenseDetails.expenseSenderName}_expense")
            dismiss()
        }

        bindingView.editExpense.setOnClickListener {
            navigateToAddNewExpenseActivity()
        }

        bindingView.iconBack.setOnClickListener {
            dismiss()
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)?.apply {
            layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    private fun bindExpenseDetails() {
        // Set basic data dynamically
        bindingView.tvTransactionStatus.text =
            if (expenseDetails.isIncome) getString(R.string.text_income) else getString(R.string.text_expense)

        bindingView.tvTransactionAmount.text = expenseDetails.amount.toString()
        bindingView.totalLayout.transactionDetailLabel.text = getString(R.string.text_total)
        bindingView.totalLayout.transactionDetailValue.text = expenseDetails.amount.toString()

        bindingView.statusLayout.transactionDetailLabel.text = getString(R.string.text_status)
        // Set basic data dynamically
        bindingView.statusLayout.transactionDetailValue.text =
            if (expenseDetails.isIncome) {
//                bindingView.tvTransactionStatus.setTextColor(resources.getColor(R.color.colorNormal, null))
                getString(R.string.text_income)
            } else {
                bindingView.statusLayout.transactionDetailValue.setTextColor(resources.getColor(R.color.red, null))
                getString(R.string.text_expense)
            }

        bindingView.fromLayout.transactionDetailLabel.text = getString(R.string.text_from)
        bindingView.fromLayout.transactionDetailValue.text = expenseDetails.expenseSenderName

        bindingView.toLayout.transactionDetailLabel.text = getString(R.string.text_to)
        bindingView.toLayout.transactionDetailValue.text = expenseDetails.expenseReceiverName

        bindingView.categoryLayout.transactionDetailLabel.text = getString(R.string.text_category)

        val dateTimePair = Utility.getDateTime(expenseDetails.expenseAddedDate)
        expenseTime = dateTimePair.second.toString()
        expenseDate = dateTimePair.first.toString()

        bindingView.timeLayout.transactionDetailLabel.text = getString(R.string.text_time)
        bindingView.timeLayout.transactionDetailValue.text = expenseTime

        bindingView.dateLayout.transactionDetailLabel.text = getString(R.string.text_date)
        bindingView.dateLayout.transactionDetailValue.text = expenseDate

        // Hide optional sections
        bindingView.spendingDetails.visibility = View.GONE
        bindingView.divider1.visibility = View.GONE
    }

    private fun loadCategoryIcon() {
        CoroutineScope(Dispatchers.IO).launch {
            val category = expenseDetails.categoryId?.let { appLoadingViewModel.getCategoryNameByID(it) }
            withContext(Dispatchers.Main) {
                category?.let {
                    bindingView.ivTransactionIcon.setImageResource(it.iconResId)
                    val layoutParams = bindingView.ivTransactionIcon.layoutParams
                    layoutParams.width = 60.dp
                    layoutParams.height = 60.dp
                    bindingView.ivTransactionIcon.layoutParams = layoutParams
                } ?: run {
                    bindingView.ivTransactionIcon.setImageResource(R.drawable.ic_other_expenses)
                    val layoutParams = bindingView.ivTransactionIcon.layoutParams
                    layoutParams.width = 60.dp
                    layoutParams.height = 60.dp
                    bindingView.ivTransactionIcon.layoutParams = layoutParams
                }
                bindingView.categoryLayout.transactionDetailValue.text = category?.name
            }
        }
    }

    private fun navigateToAddNewExpenseActivity() {
        val bundle = Bundle().apply {
            putParcelable("expenseDetails", expenseDetails)
        }
        val addNewExpenseFragment = AddNewExpenseActivity().apply {
            arguments = bundle
        }
        addNewExpenseFragment.show(parentFragmentManager, "AddNewExpenseActivity")
        dismiss()
    }

    private fun generateAndDownloadPdf(fileName: String = "expense_details") {
        CoroutineScope(Dispatchers.IO).launch {
            if (isAdded) {  // Ensure the fragment is attached
                try {
                    val pdfDocument = PdfDocument()

                    // Use your custom layout dimensions
                    val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
                    val page = pdfDocument.startPage(pageInfo)

                    val canvas = page.canvas
                    val paint = Paint().apply {
                        color = Color.BLACK
                        textSize = 14f  // Adjust text size to match your layout
                        typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                    }

                    bindingView.run {
                        // Header Section with Title
                        canvas.drawText("Transaction Details", 50f, 50f, paint)

                        // Back Button
                        drawImage(canvas, iconBack, paint, 10f, 20f)

                        // Edit Button
                        drawImage(canvas, editExpense, paint, getImageXPosition(editExpense), 20f)

                        // Transaction Icon
                        drawImage(canvas, ivTransactionIcon, paint, getImageXPosition(ivTransactionIcon), 120f, 80.dp.toInt(), 80.dp.toInt())

                        // Transaction Status
                        canvas.drawText(tvTransactionStatus.text.toString(), getImageXPosition(tvTransactionStatus), 220f, paint)

                        // Transaction Amount
                        canvas.drawText(tvTransactionAmount.text.toString(), getImageXPosition(tvTransactionAmount), 260f, paint)

                        // Transaction Details Label
                        canvas.drawText(tvTransactionDetailsLabel.text.toString(), 20f, 320f, paint)

                        // Transaction Details Section
                        binding?.statusLayout?.let {
                            drawTransactionDetailsSection(canvas,
                                it, 350f, paint)
                        }
                        binding?.fromLayout?.let {
                            drawTransactionDetailsSection(canvas,
                                it, 430f, paint)
                        }
                        binding?.toLayout?.let {
                            drawTransactionDetailsSection(canvas,
                                it, 510f, paint)
                        }
                        binding?.timeLayout?.let {
                            drawTransactionDetailsSection(canvas,
                                it, 590f, paint)
                        }
                        binding?.dateLayout?.let {
                            drawTransactionDetailsSection(canvas,
                                it, 670f, paint)
                        }

                        // Divider
                        drawDivider(canvas, 700f)

                        // Spending Details
                        binding?.earningsLayout?.let {
                            drawTransactionDetailsSection(canvas,
                                it, 730f, paint)
                        }
                        binding?.feesLayout?.let {
                            drawTransactionDetailsSection(canvas,
                                it, 810f, paint)
                        }

                        // Divider
                        drawDivider(canvas, 850f)

                        // Total Details
                        binding?.totalLayout?.let {
                            drawTransactionDetailsSection(canvas,
                                it, 880f, paint)
                        }

                        // Download Receipt Button
                        drawButton(canvas, btnDownloadReceipt, paint, 20f, 980f)

                        pdfDocument.finishPage(page)

                        // Save to Downloads directory
                        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                        val file = File(downloadsDir, "${fileName}.pdf")
                        pdfDocument.writeTo(FileOutputStream(file))
                        pdfDocument.close()

                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "PDF saved to ${file.path}", Toast.LENGTH_SHORT).show()
                        }

                        // Show notification
                        NotificationUtils.showPdfSavedNotification(context,file)
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Log.d("DsK","Failed to generate PDF ${e.localizedMessage}")
                        Toast.makeText(context, "Failed to generate PDF ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun drawImage(canvas: Canvas, imageView: ImageView, paint: Paint, x: Float, y: Float, width: Int = 24, height: Int = 24) {
        val bitmap = imageView.drawable.toBitmap(width, height)
        canvas.drawBitmap(bitmap, x, y, paint)
    }

    private fun getImageXPosition(view: View): Float {
        return when (view.id) {
            R.id.iconBack -> 10f
            R.id.editExpense -> 575f // Right side position
            else -> 0f
        }
    }

    private fun drawTransactionDetailsSection(canvas: Canvas, binding: TransactionDetailsItemViewBinding, yPosition: Float, paint: Paint) {
        canvas.drawText(binding.transactionDetailValue.text.toString(), 20f, yPosition, paint)
    }

    private fun drawDivider(canvas: Canvas, yPosition: Float) {
        val paint = Paint().apply {
            color = Color.DKGRAY
        }
        canvas.drawRect(20f, yPosition, 575f, yPosition + 1f, paint)
    }

    private fun drawButton(canvas: Canvas, button: MaterialButton, paint: Paint, x: Float, y: Float) {
        val textBounds = Rect()
        paint.getTextBounds(button.text.toString(), 0, button.text.length, textBounds)
        val xOffset = (button.width - textBounds.width()) / 2
        val yOffset = (button.height + textBounds.height()) / 2
        canvas.drawText(button.text.toString(), x + xOffset, y + yOffset, paint)
    }

}
