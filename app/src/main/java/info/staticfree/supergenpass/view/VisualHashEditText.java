package info.staticfree.supergenpass.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.security.NoSuchAlgorithmException;

import info.staticfree.supergenpass.R;
import info.staticfree.supergenpass.drawable.VisualHash;

public class VisualHashEditText extends EditText {
    private boolean mShowVisualHash;
    @Nullable
    private final VisualHash mVh;

    public VisualHashEditText(@NonNull Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        try {
            mVh = new VisualHash();
        } catch (@NonNull NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }

        init(context, attrs);
    }

    public VisualHashEditText(@NonNull Context context, AttributeSet attrs) {
        super(context, attrs);

        try {
            mVh = new VisualHash();
        } catch (@NonNull NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }

        init(context, attrs);
    }

    public VisualHashEditText(@NonNull Context context) {
        super(context);

        try {
            mVh = new VisualHash();
        } catch (@NonNull NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }

        init(context, null);
    }

    private void init(@NonNull Context context, AttributeSet attrs) {
        int h, w;

        if (isInEditMode()) {
            h = 45;
            w = 45;
        } else {
            TypedArray ta =
                    context.obtainStyledAttributes(attrs, R.styleable.VisualHashEditText);

            mShowVisualHash = ta.getBoolean(R.styleable.VisualHashEditText_showVisualHash, true);

            h = ta.getDimensionPixelSize(R.styleable.VisualHashEditText_visualHashHeight, 45);
            w = ta.getDimensionPixelSize(R.styleable.VisualHashEditText_visualHashWidth, 45);

            ta.recycle();
        }

        // this number is based on what looks good with standard edit boxes.
        if (mVh != null) {
            mVh.setBounds(0, 0, h, w);
        }

        refreshVisualHash();
    }

    public void setShowVisualHash(boolean showVisualHash) {
        mShowVisualHash = showVisualHash;
        refreshVisualHash();
    }

    private void refreshVisualHash() {
        if (mShowVisualHash) {
            setCompoundDrawables(null, null, mVh, null);
        } else {
            setCompoundDrawables(null, null, null, null);
        }
    }

    @Override
    protected void onTextChanged(@Nullable CharSequence text, int start, int lengthBefore,
            int lengthAfter) {
        super.onTextChanged(text, start, lengthBefore, lengthAfter);

        if (text != null && mVh != null) {
            mVh.setData(text.toString().getBytes());
        }
    }

    @Override
    public void setError(@Nullable CharSequence error, Drawable icon) {
        super.setError(error, icon);

        if (error == null) {
            refreshVisualHash();
        }
    }
}

