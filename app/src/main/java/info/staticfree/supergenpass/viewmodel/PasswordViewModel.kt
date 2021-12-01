package info.staticfree.supergenpass.viewmodel

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import info.staticfree.supergenpass.hashes.HashType
import info.staticfree.supergenpass.hashes.PasswordGenerationException
import info.staticfree.supergenpass.repository.HashRepository
import kotlinx.coroutines.launch

class PasswordViewModel(private val repository: HashRepository = HashRepository()) : ViewModel() {
    private var domain = ""
    private var mainPassword = ""

    val generatedPassword = MutableLiveData<String>()
    val generatedPin = MutableLiveData<String>()
    val showOutput = repository.getShowOutput()
    val pinDigits = repository.getPinDigits()
    val copyToClipboard = repository.getCopyToClipboard()
    val checkDomain = repository.getCheckDomain()
    val hashType = repository.getHashType()

    val showPin = repository.showPin
    val showVisualHash = repository.showVisualHash

    fun load(context: Context) {
        viewModelScope.launch {
            repository.loadDomains(context.resources)
            repository.loadFromPreferences(context)
        }
    }

    fun setDomain(domain: String) {
        this.domain = domain

        generatePasswordAndPin()
    }

    fun setMainPassword(password: String) {
        this.mainPassword = password

        generatePasswordAndPin()
    }

    fun setShowOutput(showOutput: Boolean) {
        repository.setShowOutput(showOutput)
    }

    fun setHashType(hashType: HashType) {
        repository.setHashType(hashType)
        generatePasswordAndPin()
    }

    fun onConfirmed(context: Context) {
        repository.saveDomainIfEnabled(context.contentResolver, domain)
    }

    fun setPinDigits(pinDigits: Int) {
        repository.setPinDigits(pinDigits)
        generatePasswordAndPin()
    }

    private fun generatePasswordAndPin() {
        if (domain.isBlank() || mainPassword.isBlank()) {
            generatedPassword.value = ""
            generatedPin.value = ""
        } else {
            try {
                generatedPassword.value = repository.generate(mainPassword, domain)
                generatedPin.value = repository.generatePin(mainPassword, domain)
            } catch (e: PasswordGenerationException) {
                generatedPassword.value = ""
                generatedPin.value = ""
            }
        }
    }
}