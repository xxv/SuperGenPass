package info.staticfree.SuperGenPass;

import android.app.Activity;
import android.view.View;
import android.widget.Toolbar;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

/**
 * Sets up an activity's own toolbar and keeps it and the content clear of the system bars.
 *
 * <p>From Android 15, apps targeting API 35 or later are always drawn edge-to-edge, so the window
 * no longer keeps content out from under the status bar, navigation bar or display cutout. The
 * toolbar is padded down past the status bar (so its background shows behind it) and the content
 * is padded clear of the navigation bar. On earlier versions the window still does this itself,
 * the insets seen here are zero, and nothing changes.
 */
final class SystemBarInsets {
    private SystemBarInsets() {
        // This class cannot be instantiated.
    }

    /**
     * Call right after {@code setContentView()}. The layout must include {@code @layout/toolbar}.
     *
     * @param activity the activity whose content view has just been set
     * @param contentId the view below the toolbar that holds the rest of the screen
     */
    static void setUp(@NonNull Activity activity, @IdRes int contentId) {
        Toolbar toolbar = activity.findViewById(R.id.toolbar);
        activity.setActionBar(toolbar);

        View content = activity.findViewById(contentId);
        View root = (View) toolbar.getParent();

        int toolbarLeft = toolbar.getPaddingLeft();
        int toolbarTop = toolbar.getPaddingTop();
        int toolbarRight = toolbar.getPaddingRight();
        int contentLeft = content.getPaddingLeft();
        int contentRight = content.getPaddingRight();
        int contentBottom = content.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(root, (v, windowInsets) -> {
            Insets bars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.displayCutout());

            toolbar.setPadding(toolbarLeft + bars.left, toolbarTop + bars.top,
                    toolbarRight + bars.right, toolbar.getPaddingBottom());
            content.setPadding(contentLeft + bars.left, content.getPaddingTop(),
                    contentRight + bars.right, contentBottom + bars.bottom);

            return WindowInsetsCompat.CONSUMED;
        });
    }
}
