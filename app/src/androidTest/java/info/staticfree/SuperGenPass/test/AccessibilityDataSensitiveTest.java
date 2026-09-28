package info.staticfree.SuperGenPass.test;

import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;

import org.junit.Test;
import org.junit.runner.RunWith;

import info.staticfree.SuperGenPass.R;
import info.staticfree.SuperGenPass.Super_Gen_Pass;

import static org.junit.Assert.assertTrue;

/**
 * Passwords must only be readable by accessibility tools (such as TalkBack), not by any other
 * accessibility service.
 */
@RunWith(AndroidJUnit4.class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
public class AccessibilityDataSensitiveTest {

    @Test
    public void mainScreenPasswordsAreSensitive() {
        try (ActivityScenario<Super_Gen_Pass> scenario =
                     ActivityScenario.launch(Super_Gen_Pass.class)) {
            scenario.onActivity(activity -> {
                assertSensitive(activity.findViewById(R.id.password_edit));
                assertSensitive(activity.findViewById(R.id.password_output));
                assertSensitive(activity.findViewById(R.id.pin_output));
            });
        }
    }

    @Test
    public void verifyDialogPasswordIsSensitive() {
        View layout = LayoutInflater.from(ApplicationProvider.getApplicationContext())
                .inflate(R.layout.master_pw_verify, null);
        assertSensitive(layout.findViewById(R.id.verify));
    }

    private static void assertSensitive(View view) {
        assertTrue(view.getClass().getSimpleName() + " should be accessibility data sensitive",
                view.isAccessibilityDataSensitive());
    }
}
