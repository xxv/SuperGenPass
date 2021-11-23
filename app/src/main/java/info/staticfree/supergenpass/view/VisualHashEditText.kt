package info.staticfree.supergenpass.view

import android.content.Context
import info.staticfree.supergenpass.drawable.VisualHash
import info.staticfree.supergenpass.R
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatEditText
import java.lang.RuntimeException
import java.security.NoSuchAlgorithmException

class VisualHashEditText : AppCompatEditText {
    private var showVisualHash = false
    private var visualHash: VisualHash? = null

    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(
        context,
        attrs,
        defStyle
    ) {
        visualHash = try {
            VisualHash()
        } catch (e: NoSuchAlgorithmException) {
            throw RuntimeException(e)
        }
        init(context, attrs)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        visualHash = try {
            VisualHash()
        } catch (e: NoSuchAlgorithmException) {
            throw RuntimeException(e)
        }
        init(context, attrs)
    }

    constructor(context: Context) : super(context) {
        visualHash = try {
            VisualHash()
        } catch (e: NoSuchAlgorithmException) {
            throw RuntimeException(e)
        }
        init(context, null)
    }

    private fun init(context: Context, attrs: AttributeSet?) {
        val h: Int
        val w: Int
        if (isInEditMode) {
            h = 45
            w = 45
        } else {
            val ta = context.obtainStyledAttributes(attrs, R.styleable.VisualHashEditText)
            showVisualHash = ta.getBoolean(R.styleable.VisualHashEditText_showVisualHash, true)
            h = ta.getDimensionPixelSize(R.styleable.VisualHashEditText_visualHashHeight, 45)
            w = ta.getDimensionPixelSize(R.styleable.VisualHashEditText_visualHashWidth, 45)
            ta.recycle()
        }

        visualHash?.setBounds(0, 0, h, w)

        refreshVisualHash()
    }

    fun setShowVisualHash(showVisualHash: Boolean) {
        this.showVisualHash = showVisualHash
        refreshVisualHash()
    }

    private fun refreshVisualHash() {
        if (showVisualHash) {
            setCompoundDrawables(null, null, visualHash, null)
        } else {
            setCompoundDrawables(null, null, null, null)
        }
    }

    override fun onTextChanged(
        text: CharSequence?, start: Int, lengthBefore: Int,
        lengthAfter: Int
    ) {
        super.onTextChanged(text, start, lengthBefore, lengthAfter)
        text?.let{
            visualHash?.setData(it.toString().toByteArray())
        }
    }

    override fun setError(error: CharSequence?, icon: Drawable) {
        super.setError(error, icon)

        if (error == null) {
            refreshVisualHash()
        }
    }
}