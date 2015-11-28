package info.staticfree.workarounds;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TabHost;

/**
 * <p>
 * This is a workaround for a bug in Android in which using tabs such that all the content of the
 * layout isn't in the tabs causes the TabHost to steal focus from any views outside the tab
 * content. This is most commonly found with EditTexts.
 * </p>
 * <p>
 * To use, simply place this view in your @android:id/tablayout frameview:
 * </p>
 * 
 * <pre>
 * <code>
 *             &lt;FrameLayout
 *                 android:id="@android:id/tabcontent"
 *                 android:layout_width="match_parent"
 *                 android:layout_height="match_parent" >
 * 
 *                 &lt;info.staticfree.workarounds.TabPatchView
 *                     android:layout_width="0dip"
 *                     android:layout_height="0dip" />
 *                     
 *                     [your actual content goes here]
 *                     &lt;/FrameLayout>
 * 
 * </pre>
 * 
 * </code>
 * 
 * @author <a href="mailto:steve@staticfree.info">Steve Pomeroy</a>
 * @see <a href="http://code.google.com/p/android/issues/detail?id=2516">issue 2516</a>
 */
public class TabPatchView extends View {

    public TabPatchView(Context context) {
        super(context);
    }

    public TabPatchView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public TabPatchView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        final TabHost tabhost = (TabHost) getRootView().findViewById(android.R.id.tabhost);
        tabhost.getViewTreeObserver().removeOnTouchModeChangeListener(tabhost);
    }
}
