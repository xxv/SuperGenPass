package info.staticfree.supergenpass;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import edu.mit.mobile.android.utils.ProviderUtils;

public class RememberedDomainProvider extends ContentProvider {

    public static final String AUTHORITY = BuildConfig.APPLICATION_ID;

    public static final String TYPE_DOMAINS_DIR =
            "vnd.android.cursor.dir/vnd." + AUTHORITY + ".domains";
    public static final String TYPE_DOMAINS_ITEM =
            "vnd.android.cursor.item/vnd." + AUTHORITY + ".domains";

    private static final int MATCHER_DOMAIN_DIR = 0, MATCHER_DOMAIN_ITEM = 1;

    @Nullable
    private RememberedDBHelper mDBHelper;

    @Override
    public String getType(@NonNull Uri uri) {
        switch (mUriMatcher.match(uri)) {
            case MATCHER_DOMAIN_DIR:
                return TYPE_DOMAINS_DIR;
            case MATCHER_DOMAIN_ITEM:
                return TYPE_DOMAINS_ITEM;

            default:
                throw new IllegalArgumentException();
        }
    }

    /**
     * Adds the given domain to the list of remembered domains.
     *
     * @param cr the resolver
     * @param domain the filtered domain name
     */
    public static void addRememberedDomain(@NonNull ContentResolver cr, String domain) {
        Cursor existingEntries =
                cr.query(Domain.CONTENT_URI, null, Domain.DOMAIN + "=?", new String[] { domain },
                        null);

        if (existingEntries != null) {
            try {
                if (!existingEntries.moveToFirst()) {
                    ContentValues cv = new ContentValues();
                    cv.put(Domain.DOMAIN, domain);
                    cr.insert(Domain.CONTENT_URI, cv);
                }
            } finally {
                existingEntries.close();
            }
        }
    }

    @Override
    public boolean onCreate() {
        Context context = getContext();

        if (context != null) {
            mDBHelper = new RememberedDBHelper(context);
        }

        return true;
    }

    @Override
    public Uri insert(@NonNull Uri uri, ContentValues values) {
        if (mDBHelper == null) {
            throw new IllegalStateException("Cannot access database helper");
        }

        SQLiteDatabase db = mDBHelper.getWritableDatabase();

        Uri newUri;

        switch (mUriMatcher.match(uri)) {
            case MATCHER_DOMAIN_DIR:
                long id = db.insert(RememberedDBHelper.DB_DOMAINS_TABLE, null, values);
                newUri = ContentUris.withAppendedId(Domain.CONTENT_URI, id);
                break;

            default:
                throw new IllegalArgumentException();
        }

        Context context = getContext();
        if (context != null) {
            ContentResolver cr = context.getContentResolver();
            cr.notifyChange(uri, null);
        }

        return newUri;
    }

    @Override
    public Cursor query(@NonNull Uri uri, String[] projection, String selection,
            String[] selectionArgs, String sortOrder) {
        if (mDBHelper == null) {
            throw new IllegalStateException("Cannot access database helper");
        }

        SQLiteDatabase db = mDBHelper.getWritableDatabase();

        Cursor cursor;

        switch (mUriMatcher.match(uri)) {
            case MATCHER_DOMAIN_DIR:
                cursor = db.query(RememberedDBHelper.DB_DOMAINS_TABLE, projection, selection,
                        selectionArgs, null, null, null);
                break;

            case MATCHER_DOMAIN_ITEM:
                cursor = db.query(RememberedDBHelper.DB_DOMAINS_TABLE, projection,
                        ProviderUtils.addExtraWhere(selection, Domain._ID + "=?"),
                        ProviderUtils.addExtraWhereArgs(selectionArgs, uri.getLastPathSegment()),
                        null, null, null);
                break;
            default:
                throw new IllegalArgumentException();
        }
        Context context = getContext();
        if (context != null) {
            ContentResolver cr = context.getContentResolver();
            cursor.setNotificationUri(cr, uri);
        }

        return cursor;
    }

    @Override
    public int update(@NonNull Uri uri, ContentValues values, String selection,
            String[] selectionArgs) {
        if (mDBHelper == null) {
            throw new IllegalStateException("Cannot access database helper");
        }

        SQLiteDatabase db = mDBHelper.getWritableDatabase();

        int changeCount;
        switch (mUriMatcher.match(uri)) {
            case MATCHER_DOMAIN_DIR:
                changeCount = db.update(RememberedDBHelper.DB_DOMAINS_TABLE, values, selection,
                        selectionArgs);
                break;

            case MATCHER_DOMAIN_ITEM:
                changeCount = db.update(RememberedDBHelper.DB_DOMAINS_TABLE, values,
                        ProviderUtils.addExtraWhere(selection, Domain._ID + "=?"),
                        ProviderUtils.addExtraWhereArgs(selectionArgs, uri.getLastPathSegment()));
                break;
            default:
                throw new IllegalArgumentException();
        }

        if (changeCount != 0) {
            Context context = getContext();
            if (context != null) {
                ContentResolver cr = context.getContentResolver();
                cr.notifyChange(uri, null);
            }
        }

        return changeCount;
    }

    @Override
    public int delete(@NonNull Uri uri, String selection, String[] selectionArgs) {
        if (mDBHelper == null) {
            throw new IllegalStateException("Cannot access database helper");
        }

        SQLiteDatabase db = mDBHelper.getWritableDatabase();

        int changeCount;
        switch (mUriMatcher.match(uri)) {
            case MATCHER_DOMAIN_DIR:
                changeCount =
                        db.delete(RememberedDBHelper.DB_DOMAINS_TABLE, selection, selectionArgs);
                break;

            case MATCHER_DOMAIN_ITEM:
                changeCount = db.delete(RememberedDBHelper.DB_DOMAINS_TABLE,
                        ProviderUtils.addExtraWhere(selection, Domain._ID + "=?"),
                        ProviderUtils.addExtraWhereArgs(selectionArgs, uri.getLastPathSegment()));
                break;

            default:
                throw new IllegalArgumentException("delete not supported for the given uri");
        }

        if (changeCount != 0) {
            Context context = getContext();
            if (context != null) {
                ContentResolver cr = context.getContentResolver();
                cr.notifyChange(uri, null);
            }
        }

        return changeCount;
    }

    @NonNull
    private static final UriMatcher mUriMatcher;

    static {
        mUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

        mUriMatcher.addURI(AUTHORITY, Domain.PATH, MATCHER_DOMAIN_DIR);
        mUriMatcher.addURI(AUTHORITY, Domain.PATH + "/#", MATCHER_DOMAIN_ITEM);
    }
}
