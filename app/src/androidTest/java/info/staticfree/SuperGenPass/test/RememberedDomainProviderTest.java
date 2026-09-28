package info.staticfree.SuperGenPass.test;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import info.staticfree.SuperGenPass.Domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Exercises the single-item URIs of the remembered domains provider, which combine the row id
 * from the URI with any selection the caller passes in.
 */
@RunWith(AndroidJUnit4.class)
public class RememberedDomainProviderTest {
    private static final String TEST_SUFFIX = ".provider-test.example";
    private static final String DOMAIN_A = "a" + TEST_SUFFIX;
    private static final String DOMAIN_B = "b" + TEST_SUFFIX;

    private ContentResolver mResolver;
    private Uri mItemA;
    private Uri mItemB;

    @Before
    public void setUp() {
        mResolver = ApplicationProvider.getApplicationContext().getContentResolver();
        deleteTestRows();
        mItemA = insert(DOMAIN_A);
        mItemB = insert(DOMAIN_B);
    }

    @After
    public void tearDown() {
        deleteTestRows();
    }

    @Test
    public void queryItemReturnsOnlyThatRow() {
        assertEquals(DOMAIN_A, querySingleDomain(mItemA, null, null));
        assertEquals(DOMAIN_B, querySingleDomain(mItemB, null, null));
    }

    @Test
    public void queryItemHonoursCallerSelection() {
        // Matching selection: the caller's argument and the URI's id must both line up.
        assertEquals(DOMAIN_A, querySingleDomain(mItemA, Domain.DOMAIN + "=?",
                new String[] { DOMAIN_A }));

        // Selection that excludes the row named by the URI.
        assertEquals(0, count(mItemA, Domain.DOMAIN + "=?", new String[] { DOMAIN_B }));
    }

    @Test
    public void updateItemChangesOnlyThatRow() {
        ContentValues values = new ContentValues();
        values.put(Domain.DOMAIN, "renamed" + TEST_SUFFIX);

        assertEquals(1, mResolver.update(mItemA, values, null, null));
        assertEquals("renamed" + TEST_SUFFIX, querySingleDomain(mItemA, null, null));
        assertEquals(DOMAIN_B, querySingleDomain(mItemB, null, null));
    }

    @Test
    public void deleteItemRemovesOnlyThatRow() {
        // A selection that doesn't match the row must leave it alone.
        assertEquals(0, mResolver.delete(mItemA, Domain.DOMAIN + "=?", new String[] { DOMAIN_B }));

        assertEquals(1, mResolver.delete(mItemA, null, null));
        assertEquals(0, count(mItemA, null, null));
        assertEquals(1, count(mItemB, null, null));
    }

    @NonNull
    private Uri insert(@NonNull String domain) {
        ContentValues values = new ContentValues();
        values.put(Domain.DOMAIN, domain);
        Uri uri = mResolver.insert(Domain.CONTENT_URI, values);
        assertNotNull(uri);
        // Sanity check that the provider hands back an item URI we can address by id.
        assertEquals(ContentUris.parseId(uri), Long.parseLong(uri.getLastPathSegment()));
        return uri;
    }

    @Nullable
    private String querySingleDomain(@NonNull Uri uri, @Nullable String selection,
            @Nullable String[] selectionArgs) {
        try (Cursor c = mResolver.query(uri, new String[] { Domain.DOMAIN }, selection,
                selectionArgs, null)) {
            assertNotNull(c);
            assertEquals(1, c.getCount());
            c.moveToFirst();
            return c.getString(0);
        }
    }

    private int count(@NonNull Uri uri, @Nullable String selection,
            @Nullable String[] selectionArgs) {
        try (Cursor c = mResolver.query(uri, null, selection, selectionArgs, null)) {
            assertNotNull(c);
            return c.getCount();
        }
    }

    private void deleteTestRows() {
        mResolver.delete(Domain.CONTENT_URI, Domain.DOMAIN + " LIKE ?",
                new String[] { "%" + TEST_SUFFIX });
    }
}
