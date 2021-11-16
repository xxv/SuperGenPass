package info.staticfree.supergenpass

import android.content.ContentResolver
import android.content.Intent
import android.database.Cursor
import android.os.Bundle
import android.provider.BaseColumns
import android.text.method.PasswordTransformationMethod
import android.view.*
import android.widget.CursorAdapter
import android.widget.FilterQueryProvider
import android.widget.SimpleCursorAdapter
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.preference.PreferenceManager
import info.staticfree.supergenpass.databinding.SgpFragmentBinding

class SGPFragment : Fragment() {
    private val model: PasswordViewModel by viewModels()

    private lateinit var viewBinding: SgpFragmentBinding
    private var copyToClipboard = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        model.load(requireContext())
        loadFromPreferences()
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

        registerShowOutput()
        registerDomainEdit()
        registerPasswordEdit()
        registerHidePassword()
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
    }

    private fun registerShowOutput() {
        model.generatedPassword.observe(viewLifecycleOwner, {
            viewBinding.passwordOutput.text = it
        })

        viewBinding.showGenPassword.setOnCheckedChangeListener { _, isChecked ->
            viewBinding.passwordOutput.hidePassword = !isChecked
            model.setShowOutput(isChecked)
        }

        model.showOutput.observe(viewLifecycleOwner, {
            viewBinding.showGenPassword.isChecked = it
        })
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.options, menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        val verify = menu.findItem(R.id.verify)
        verify.isEnabled = viewBinding.passwordEdit.length() > 0
        menu.findItem(R.id.copy).isEnabled = viewBinding.passwordOutput.text.isNotBlank()
        //menu.findItem(R.id.write_nfc).isEnabled = mMasterPwEdit.getText().length > 0
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.settings -> {
                val preferencesIntent =
                    Intent().setClass(requireContext(), SgpPreferencesActivity::class.java)
                startActivity(preferencesIntent)
                true
            }
            R.id.about -> {
                //TODO AboutFragment().show(supportFragmentManager, "about")
                true
            }
            R.id.verify -> {
                VerifyFragment.showVerifyFragment(childFragmentManager, viewBinding.passwordEdit.text.toString())
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

    private fun loadFromPreferences() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(requireContext())
        copyToClipboard = prefs.getBoolean(Preferences.PREF_CLIPBOARD, true)
    }

    private fun go(): Boolean {
        postGenerate(copyToClipboard)

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

    private class DomainQueryProvider(val contentResolver: ContentResolver) : FilterQueryProvider {
        override fun runQuery(constraint: CharSequence?): Cursor? {
            val c: Cursor? = if (constraint == null || constraint.isEmpty()) {
                contentResolver
                    .query(
                        Domain.CONTENT_URI,
                        PROJECTION,
                        null,
                        null,
                        Domain.SORT_ORDER
                    )
            } else {
                contentResolver
                    .query(
                        Domain.CONTENT_URI,
                        PROJECTION,
                        Domain.DOMAIN + " GLOB ?",
                        arrayOf(
                            "$constraint*"
                        ),
                        Domain.SORT_ORDER
                    )
            }
            return c
        }

        companion object {
            val PROJECTION = arrayOf(Domain.DOMAIN, BaseColumns._ID)
            const val DOMAIN_COLUMN = 0
        }
    }

}