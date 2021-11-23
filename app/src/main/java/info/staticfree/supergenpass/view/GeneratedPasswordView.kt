package info.staticfree.supergenpass.view

import android.app.Application
import kotlin.jvm.JvmOverloads
import info.staticfree.supergenpass.R
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import android.text.method.PasswordTransformationMethod
import android.os.Parcelable
import android.os.Parcel
import android.text.TextUtils
import android.text.method.NumberKeyListener
import android.text.InputType
import android.util.AttributeSet
import android.util.Log
import android.view.ContextMenu
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.widget.AppCompatTextView

class GeneratedPasswordView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null,
    defStyle: Int = R.attr.generatedPasswordViewStyle
) : AppCompatTextView(context, attrs, defStyle), View.OnClickListener,
    MenuItem.OnMenuItemClickListener {
    private var mOnClickListener: OnClickListener? = null
    private var domain: CharSequence? = null
    override fun setOnClickListener(onClickListener: OnClickListener?) {
        mOnClickListener = onClickListener
    }

    override fun onClick(v: View) {
        Log.d("gpwv", "click!")

        // propagate the click
        if (mOnClickListener != null) {
            mOnClickListener!!.onClick(v)
        }
    }

    override fun onCreateContextMenu(menu: ContextMenu) {
        menu.add(Menu.NONE, MENU_ID_COPY, Menu.NONE, android.R.string.copy)
            .setOnMenuItemClickListener(this)
        menu.setHeaderTitle(R.string.generated_password)
    }

    override fun onTextContextMenuItem(id: Int): Boolean {
        return when (id) {
            MENU_ID_COPY -> {
                copyToClipboard()
                true
            }
            else -> super.onTextContextMenuItem(id)
        }
    }

    /**
     * Sets the domain name that will be displayed when copying to clipboard.
     *
     * @param domainName the domain to show in the Toast
     */
    fun setDomainName(domainName: CharSequence?) {
        domain = domainName
    }

    override fun setText(text: CharSequence?, type: BufferType) {
        super.setText(text, type)
        isEnabled = text?.isNotEmpty() ?: false
    }

    fun copyToClipboard() {
        val genPw = text ?: return
        val clipMan = context.getSystemService(Application.CLIPBOARD_SERVICE) as ClipboardManager
        clipMan.setPrimaryClip(
            ClipData.newPlainText(
                context.getText(
                    R.string.generated_password
                ), genPw
            )
        )
        if (genPw.isNotEmpty()) {
            if (domain != null) {
                Toast.makeText(
                    context,
                    resources.getString(R.string.toast_copied, domain), Toast.LENGTH_SHORT
                )
                    .show()
            } else {
                Toast.makeText(
                    context,
                    resources.getString(R.string.toast_copied_no_domain),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    var hidePassword: Boolean
        get() = transformationMethod != null
        set(hidePassword) {
            transformationMethod = if (hidePassword) {
                PasswordTransformationMethod.getInstance()
            } else {
                null
            }
        }

    override fun onMenuItemClick(item: MenuItem): Boolean {
        return onTextContextMenuItem(item.itemId)
    }

    /* (for all the state-related code below)
     *
     * Copyright (C) 2006 The Android Open Source Project
     *
     * Licensed under the Apache License, Version 2.0 (the "License");
     * you may not use this file except in compliance with the License.
     * You may obtain a copy of the License at
     *
     *      http://www.apache.org/licenses/LICENSE-2.0
     *
     * Unless required by applicable law or agreed to in writing, software
     * distributed under the License is distributed on an "AS IS" BASIS,
     * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
     * See the License for the specific language governing permissions and
     * limitations under the License.
     */
    override fun onRestoreInstanceState(state: Parcelable) {
        if (state !is SavedState) {
            super.onRestoreInstanceState(state)
            return
        }
        super.onRestoreInstanceState(state.superState)
        if (state.text != null) {
            text = state.text
        }
    }

    override fun onSaveInstanceState(): Parcelable {
        val superState = super.onSaveInstanceState()
        val ss = SavedState(superState)
        ss.text = text
        return ss
    }

    class SavedState : BaseSavedState {
        var text: CharSequence? = null

        constructor(superState: Parcelable?) : super(superState)

        override fun writeToParcel(dest: Parcel, flags: Int) {
            super.writeToParcel(dest, flags)
            TextUtils.writeToParcel(text, dest, flags)
        }

        private constructor(parcel: Parcel) : super(parcel) {
            text = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(parcel)
        }

        override fun describeContents(): Int {
            return 0
        }

        companion object CREATOR : Parcelable.Creator<SavedState> {
            override fun createFromParcel(parcel: Parcel): SavedState {
                return SavedState(parcel)
            }

            override fun newArray(size: Int): Array<SavedState?> {
                return arrayOfNulls(size)
            }
        }

    } /* end Copyright (C) 2006 The Android Open Source Project */

    companion object {
        const val MENU_ID_COPY = android.R.id.copy
    }

    init {
        super.setOnClickListener(this)
        keyListener = object : NumberKeyListener() {
            override fun getInputType(): Int {
                return InputType.TYPE_NULL
            }

            override fun getAcceptedChars(): CharArray {
                return charArrayOf()
            }
        }
    }
}