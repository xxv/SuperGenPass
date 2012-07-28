package info.staticfree.SuperGenPass;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.EditText;

public class VisualHashEditText extends EditText {

	private boolean mShowVisualHash;
	private final VisualHash mVh = new VisualHash();

	public VisualHashEditText(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);

		init(context, attrs);
	}

	public VisualHashEditText(Context context, AttributeSet attrs) {
		super(context, attrs);

		init(context, attrs);
	}

	public VisualHashEditText(Context context) {
		super(context);

		init(context, null);

	}

	private void init(Context context, AttributeSet attrs){
		final TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.VisualHashEditText);

		mShowVisualHash = ta.getBoolean(R.styleable.VisualHashEditText_showVisualHash, true);


		final int h = ta.getDimensionPixelSize(R.styleable.VisualHashEditText_visualHashHeight, 45);
		final int w = ta.getDimensionPixelSize(R.styleable.VisualHashEditText_visualHashWidth, 45);

		// this number is based on what looks good with standard edit boxes.
		mVh.setBounds(0, 0, h, w);

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
	protected void onTextChanged(CharSequence text, int start, int lengthBefore, int lengthAfter) {
		super.onTextChanged(text, start, lengthBefore, lengthAfter);

		if (text != null && mVh != null) {
			mVh.setData(text.toString().getBytes());
		}
	}

	@Override
	public void setError(CharSequence error, Drawable icon) {
		super.setError(error, icon);

		if (error == null) {
			refreshVisualHash();
		}
	}
}

