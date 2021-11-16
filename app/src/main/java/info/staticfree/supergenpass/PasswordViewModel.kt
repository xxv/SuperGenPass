package info.staticfree.supergenpass

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class PasswordViewModel : ViewModel() {
    private var domain = ""
    private var mainPassword = ""

    private val repository = HashRepository()

    val generatedPassword = MutableLiveData<String>()
    val showOutput = repository.getShowOutput()

    fun load(context: Context) {
        viewModelScope.launch {
            repository.loadDomains(context.resources)
            repository.loadFromPreferences(context)
        }
    }

    fun setDomain(domain: String) {
        this.domain = domain

        generatePassword()
    }

    fun setMainPassword(password: String) {
        this.mainPassword = password

        generatePassword()
    }

    fun setShowOutput(showOutput: Boolean) {
        repository.setShowOutput(showOutput)
    }

    fun onConfirmed(context: Context) {
        repository.saveDomainIfEnabled(context.contentResolver, domain)
    }

    private fun generatePassword() {
        if (domain.isBlank() || mainPassword.isBlank()) {
            generatedPassword.value = ""
        } else {
            try {
                generatedPassword.value = repository.generate(mainPassword, domain)
            } catch (e: PasswordGenerationException) {
                generatedPassword.value = ""
            }
        }
    }
}