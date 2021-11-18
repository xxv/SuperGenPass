package info.staticfree.supergenpass

import android.content.Intent
import android.os.Bundle
import android.text.method.PasswordTransformationMethod
import android.view.*
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.CursorAdapter
import android.widget.SimpleCursorAdapter
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import info.staticfree.supergenpass.databinding.SgpFragmentBinding

class SGPFragment : Fragment() {
    private val model: PasswordViewModel by viewModels()

    private lateinit var viewBinding: SgpFragmentBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        model.load(requireContext())
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewBinding = SgpFragmentBinding.inflate(inflater, container, false)

        return viewBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewBinding.domainEdit.requestFocus()

        registerOutputs()
        registerShowOutput()
        registerDomainEdit()
        registerPasswordEdit()
        registerHidePassword()
        registerPinDigits()
    }

    private fun registerHidePassword() {
        viewBinding.hideMasterPassword.setOnCheckedChangeListener { _, isChecked ->
            val edit = viewBinding.passwordEdit
            val selStart: Int = edit.selectionStart
            val selEnd: Int = edit.selectionEnd
            viewBinding.passwordEdit.transformationMethod =
                if (isChecked) null else PasswordTransformationMethod()
            edit.setSelection(selStart, selEnd)
        }
    }

    private fun registerPasswordEdit() {
        viewBinding.passwordEdit.apply {
            addTextChangedListener {
                model.setMainPassword(it.toString())
            }

            setOnEditorActionListener { _, _, _ -> go() }
        }
    }

    private fun registerDomainEdit() {
        viewBinding.domainEdit.apply {
            addTextChangedListener {
                model.setDomain(it.toString())
            }

            val adapter = SimpleCursorAdapter(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                null,
                arrayOf(Domain.DOMAIN),
                intArrayOf(
                    android.R.id.text1
                ),
                CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER
            )

            adapter.filterQueryProvider = DomainQueryProvider(requireContext().contentResolver)
            adapter.stringConversionColumn = DomainQueryProvider.DOMAIN_COLUMN

            setAdapter(adapter)

            setOnItemClickListener { _, _, _, _ -> viewBinding.passwordEdit.requestFocus() }
        }

        model.checkDomain.observe(viewLifecycleOwner, {
            viewBinding.domainEdit.setHint(
                when (it) {
                    true -> R.string.domain_hint
                    false -> R.string.domain_hint_no_checking
                }
            )
        })
    }

    private fun registerOutputs() {
        model.generatedPassword.observe(viewLifecycleOwner, {
            viewBinding.passwordOutput.text = it
        })

        model.generatedPin.observe(viewLifecycleOwner, {
            viewBinding.pinOutput.text = it
        })
    }

    private fun registerShowOutput() {
        viewBinding.showGenPassword.setOnCheckedChangeListener { _, isChecked ->
            viewBinding.passwordOutput.hidePassword = !isChecked
            viewBinding.pinOutput.hidePassword = !isChecked
            model.setShowOutput(isChecked)
        }

        model.showOutput.observe(viewLifecycleOwner, {
            viewBinding.showGenPassword.isChecked = it
        })
    }

    private fun registerPinDigits() {
        model.pinDigits.observe(viewLifecycleOwner, {
            viewBinding.pinLength.apply {
                val first = getItemAtPosition(0) as String
                setSelection(it - first.toInt())
            }
        })

        viewBinding.pinLength.onItemSelectedListener = object : OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                val selected = parent?.getItemAtPosition(position) as String
                model.setPinDigits(selected.toInt())
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // empty body
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.options, menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        menu.apply {
            findItem(R.id.verify).isEnabled = viewBinding.passwordEdit.text.isNotBlank()
            findItem(R.id.copy).isEnabled = viewBinding.passwordOutput.text.isNotBlank()
            // TODO menu.findItem(R.id.write_nfc).isEnabled = mMasterPwEdit.getText().length > 0
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.settings -> {
                startActivity(Intent(requireContext(), SgpPreferencesActivity::class.java))
                true
            }
            R.id.about -> {
                //TODO AboutFragment().show(supportFragmentManager, "about")
                true
            }
            R.id.verify -> {
                VerifyFragment.showVerifyFragment(
                    childFragmentManager,
                    viewBinding.passwordEdit.text.toString()
                )
                true
            }
            R.integer.ime_go -> {
                go()
                true
            }
            R.id.copy -> {
                postGenerate(true)
                true
            }
            R.id.write_nfc -> {
                // TODO writeNfc()
                true
            }
            else -> false
        }
    }

    private fun go(): Boolean {
        postGenerate(model.copyToClipboard.value == true)

        return true
    }

    private fun postGenerate(copyToClipboard: Boolean) {
        model.onConfirmed(requireActivity())

        if (copyToClipboard) {
            viewBinding.passwordOutput.copyToClipboard()
            /*TODO if (Intent.ACTION_SEND == getIntent().getAction() &&
                mGenPwView.getHidePassword()
            ) {
                finish()
            }*/
        }
    }

}