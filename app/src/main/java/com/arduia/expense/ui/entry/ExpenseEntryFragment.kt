package com.arduia.expense.ui.entry

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.observe
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.arduia.core.extension.px
import com.arduia.expense.ui.MainHost
import com.arduia.expense.R
import com.arduia.expense.databinding.FragExpenseEntryBinding
import com.arduia.expense.ui.common.*
import com.arduia.expense.ui.vto.ExpenseDetailsVto
import com.arduia.mvvm.EventObserver
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class ExpenseEntryFragment : Fragment() {

    private lateinit var viewBinding: FragExpenseEntryBinding

    private val args: ExpenseEntryFragmentArgs by navArgs()

    private val viewModel by viewModels<ExpenseEntryViewModel>()

    @Inject
    lateinit var mainHost: MainHost

    private lateinit var categoryAdapter: CategoryListAdapter

    @Inject
    lateinit var categoryProvider: ExpenseCategoryProvider

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        initViewBinding(parent = container)
        return viewBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupView()
        setupViewModel()
        chooseEntryMode()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        updateCategoryListAfterAnimation()
    }

    private fun setupView() {
        setupCategoryListAdapter()
        setupCategoryListView()
        setupEntryCloseButton()
        setupEntryAmountEditText()
        setupLockButton()
    }

    private fun setupLockButton() {
        viewBinding.cvLock.setOnClickListener {
            viewModel.invertLockMode()
            mainHost.showSnackMessage("Repeat Mode Changed!")
        }
    }

    private fun setupViewModel() {
        addLifecycleObserver()
        observeDataInsertedEvent()
        observeDataUpdatedEvent()
        observeEntryModeEvent()
        observeEventExpenseDataState()
        observeSelectedCategoryState()
        observeOnLockMode()
        observeOnNext()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        hideInputKeyboard()
    }

    private fun setupCategoryListView() {
        viewBinding.rvCategory.adapter = categoryAdapter
        viewBinding.rvCategory.addItemDecoration(
            MarginItemDecoration(
                requireContext().px(4),
                requireContext().px(4),
                true
            )
        )
    }

    private fun setupEntryAmountEditText() {
        viewBinding.edtAmount.filters = arrayOf(FloatingInputFilter(decimalLength = 3))
    }

    private fun setupCategoryListAdapter() {
        categoryAdapter = CategoryListAdapter(layoutInflater)
        categoryAdapter.setOnItemClickListener {
            viewModel.selectCategory(it)
        }
    }

    private fun setupEntryCloseButton() {
        viewBinding.btnEntryClose.setOnClickListener {
            backToPreviousFragment()
        }
    }

    private fun addLifecycleObserver() {
        lifecycle.addObserver(viewModel)
    }

    private fun observeSelectedCategoryState() {
        viewModel.selectedCategory.observe(viewLifecycleOwner, Observer { item ->
            setSelectedItem(item)
        })
    }

    private fun observeDataInsertedEvent() {
        viewModel.insertedEvent.observe(viewLifecycleOwner, EventObserver {
            backToPreviousFragment()
        })
    }

    private fun observeOnNext(){
        viewModel.onNext.observe(viewLifecycleOwner, EventObserver{
            cleanUi()
            focusOnName()
            showItemSaved()
        })
    }

    private fun showItemSaved(){
        mainHost.showSnackMessage("Saved!")
    }

    private fun observeDataUpdatedEvent() {
        viewModel.updatedEvent.observe(viewLifecycleOwner, EventObserver {
            showDataUpdatedMessage()
            backToPreviousFragment()
        })
    }


    private fun observeOnLockMode() {
        viewModel.lockMode.observe(viewLifecycleOwner) {
            //Replace with Drawable State Lists
            when (it) {
                LockMode.LOCKED -> {
                    viewBinding.cvLock.backgroundTintList = getColorList(R.color.blue_light_500)
                    viewBinding.imvLock.setImageResource(R.drawable.ic_lock_closed)
                    viewBinding.imvLock.imageTintList = getColorList(R.color.white)
                    viewBinding.btnSave.text = getString(R.string.next)
                }
                LockMode.UNLOCK -> {
                    viewBinding.cvLock.backgroundTintList = getColorList(R.color.white)
                    viewBinding.imvLock.setImageResource(R.drawable.ic_lock_open)
                    viewBinding.imvLock.imageTintList = getColorList(R.color.blue_light_500)
                    viewBinding.btnSave.text = getString(R.string.save)
                }
            }
        }
    }

    private fun cleanUi(){
        with(viewBinding){
            edtAmount.setText("")
            edtName.setText("")
            edtNote.setText("")
        }
    }

    private fun focusOnName(){
        viewBinding.edtName.requestFocus()
    }

    private fun observeEntryModeEvent() {
        viewModel.currentModeEvent.observe(viewLifecycleOwner, EventObserver { mode ->
            changeViewToSelectedMode(mode)
        })
    }

    private fun observeEventExpenseDataState() {
        viewModel.data.observe(viewLifecycleOwner, Observer { data ->
            bindExpenseDetail(data)
        })
    }

    private fun chooseEntryMode() {

        Timber.d("chooseMode")
        val argId = args.expenseId
        val isInvalidId = argId < 0
        if (isInvalidId) {
            viewModel.chooseSaveMode()
        } else {
            viewModel.chooseUpdateMode()
        }
    }

    private fun changeViewToSelectedMode(mode: ExpenseEntryMode) {
        Timber.d("changeViewToSelectedMode -> $mode")
        when (mode) {
            ExpenseEntryMode.UPDATE -> changeToUpdateMode()
            ExpenseEntryMode.INSERT -> changeToSaveMode()
        }
    }

    private fun bindExpenseDetail(data: ExpenseUpdateDataVto) {
        viewBinding.edtName.setText(data.name)
        viewBinding.edtAmount.setText(data.amount)
        viewBinding.edtNote.setText(data.note)
        viewModel.selectCategory(data.category)
        categoryAdapter.submitList(listOf(data.category))
    }

    private fun updateCategoryListAfterAnimation() {
        MainScope().launch(Dispatchers.Main) {
            val duration = resources.getInteger(R.integer.duration_entry_pop_up).toLong()
            delay(duration)
            updateCategoryList()
        }
    }

    private fun updateCategoryList() {
        val list = getCategoryList()
        val item = categoryAdapter.selectedItem
        if (item != null) {
            moveItemToFirstIndex(list = list, item = item)
        }
        categoryAdapter.submitList(list)
    }

    private fun moveItemToFirstIndex(list: MutableList<ExpenseCategory>, item: ExpenseCategory) {
        list.remove(item)
        list.add(0, item)
    }

    private fun getCategoryList(): MutableList<ExpenseCategory> {
        return categoryProvider.getCategoryList().toMutableList()
    }

    private fun changeToUpdateMode() = with(viewBinding) {
        tvEntryTitle.text = getString(R.string.update_data)
        btnSave.text = getString(R.string.update)
        btnSave.setOnClickListener { updateData() }
        viewModel.setCurrentExpenseId(args.expenseId)
        viewBinding.cvLock.isEnabled = false
        viewBinding.imvLock.imageTintList = getColorList(R.color.darker_gray)
    }

    private fun changeToSaveMode() = with(viewBinding) {
        tvEntryTitle.text = getString(R.string.expense_entry)
        btnSave.text = getString(R.string.save)
        btnSave.setOnClickListener { saveData() }
        edtName.requestFocus()
        setInitialDefaultCategory()
    }

    private fun setInitialDefaultCategory() {
        val default = categoryProvider.getCategoryByID(ExpenseCategory.OUTCOME)
        categoryAdapter.submitList(listOf(default))
        viewModel.selectCategory(default)
    }

    private fun saveData() {
        val expense = getCurrentExpenseDetail()
        val isEmpty = expense.amount.isEmpty()

        if (isEmpty) {
            showAmountEmptyError()
            return
        }
        viewModel.saveExpenseData(expense)
    }


    private fun updateData() {
        val expense = getCurrentExpenseDetail()
        val isEmpty = expense.amount.isEmpty()

        if (isEmpty) {
            showAmountEmptyError()
            return
        }

        viewModel.updateExpenseData(expense)
    }

    private fun showAmountEmptyError() {
        viewBinding.edtAmount.error = getString(R.string.empty_cost)
    }

    private fun getCurrentExpenseDetail(): ExpenseDetailsVto {

        val name = getNameText()
        val amount = getAmountText()
        val note = getNoteText()
        val category = getSelectedCategory()
        val id = getExpenseId()

        return ExpenseDetailsVto(
            id = id,
            name = name,
            date = "",
            category = category.id,
            amount = amount,
            finance = "",
            note = note
        )
    }

    private fun getExpenseId(): Int {
        val argId = args.expenseId
        val isInvalid = (argId < 0)
        return if (isInvalid) 0
        else argId
    }

    private fun getNameText() = viewBinding.edtName.text.toString()

    private fun getAmountText() = viewBinding.edtAmount.text.toString()

    private fun getNoteText() = viewBinding.edtNote.text.toString()

    private fun getSelectedCategory() = categoryAdapter.selectedItem
        ?: throw Exception("Category Item is not selected!")

    private fun hideInputKeyboard() {
        val inputMethodManager =
            (requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager)
        inputMethodManager?.hideSoftInputFromWindow(viewBinding.edtName.windowToken, 0)
    }

    private fun initViewBinding(parent: ViewGroup?) {
        viewBinding = FragExpenseEntryBinding.inflate(layoutInflater, parent, false)
    }

    private fun showDataUpdatedMessage() {
        mainHost.showSnackMessage(getString(R.string.data_updated))
    }

    private fun backToPreviousFragment() {
        findNavController().popBackStack()
    }

    private fun setSelectedItem(item: ExpenseCategory) {
        categoryAdapter.selectedItem = item
    }

    companion object {
        private const val TAG = "ExpenseUpdate"
    }

}
