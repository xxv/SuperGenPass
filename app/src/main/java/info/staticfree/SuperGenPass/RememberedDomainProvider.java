package info.staticfree.SuperGenPass;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import edu.mit.mobile.android.utils.ProviderUtils;

public class RememberedDomainProvider extends ContentProvider {

    public static final String
    AUTHORITY = "info.staticfree.android.sgp";

    public static final String
    TYPE_DOMAINS_DIR = "vnd.android.cursor.dir/vnd."+AUTHORITY+".domains",
    TYPE_DOMAINS_ITEM = "vnd.android.cursor.item/vnd."+AUTHORITY+".domains";


    private static final int
    MATCHER_DOMAIN_DIR = 0,
    MATCHER_DOMAIN_ITEM = 1;

    private RememberedDBHelper mDBHelper;




    @Override
    public String getType(final Uri uri) {
        switch (mUriMatcher.match(uri)){
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
    public static void addRememberedDomain(final ContentResolver cr, final String domain){
        final Cursor existingEntries = cr.query(Domain.CONTENT_URI, null,
                Domain.DOMAIN + "=?", new String[] {domain}, null);

        if (existingEntries != null) {
            try {
                if (!existingEntries.moveToFirst()) {
                    final ContentValues cv = new ContentValues();
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
        mDBHelper = new RememberedDBHelper(getContext());
        return true;
    }

    @Override
    public Uri insert(final Uri uri, final ContentValues values) {
        final SQLiteDatabase db = mDBHelper.getWritableDatabase();

        switch(mUriMatcher.match(uri)){
        case MATCHER_DOMAIN_DIR:
            final long id = db.insert(RememberedDBHelper.DB_DOMAINS_TABLE, null, values);
            return ContentUris.withAppendedId(Domain.CONTENT_URI, id);

        default:
            throw new IllegalArgumentException();
        }
    }

    @Override
    public Cursor query(final Uri uri, final String[] projection, final String selection,
            final String[] selectionArgs, final String sortOrder) {
        final SQLiteDatabase db = mDBHelper.getWritableDatabase();

        switch(mUriMatcher.match(uri)){
        case MATCHER_DOMAIN_DIR:
            return db.query(RememberedDBHelper.DB_DOMAINS_TABLE, projection, selection, selectionArgs, null, null, null);

        case MATCHER_DOMAIN_ITEM:
            return db.query(RememberedDBHelper.DB_DOMAINS_TABLE, projection,
                    ProviderUtils.addExtraWhere(selection, Domain._ID+"=?"),
                    ProviderUtils.addExtraWhereArgs(selectionArgs, uri.getLastPathSegment()),
                    null, null, null);
        default:
            throw new IllegalArgumentException();
        }
    }

    @Override
    public int update(final Uri uri, final ContentValues values, final String selection,
            final String[] selectionArgs) {
        final SQLiteDatabase db = mDBHelper.getWritableDatabase();

        switch(mUriMatcher.match(uri)){
        case MATCHER_DOMAIN_DIR:
            return db.update(RememberedDBHelper.DB_DOMAINS_TABLE, values, selection, selectionArgs);

        case MATCHER_DOMAIN_ITEM:
            return db.update(RememberedDBHelper.DB_DOMAINS_TABLE, values,
                    ProviderUtils.addExtraWhere(selection, Domain._ID+"=?"),
                    ProviderUtils.addExtraWhereArgs(selectionArgs, uri.getLastPathSegment()));
            default:
                throw new IllegalArgumentException();
        }
    }

    @Override
    public int delete(final Uri uri, final String selection, final String[] selectionArgs) {
        final SQLiteDatabase db = mDBHelper.getWritableDatabase();

        switch (mUriMatcher.match(uri)){
        case MATCHER_DOMAIN_DIR:
            return db.delete(RememberedDBHelper.DB_DOMAINS_TABLE, selection, selectionArgs);

        case MATCHER_DOMAIN_ITEM:
            return db.delete(RememberedDBHelper.DB_DOMAINS_TABLE,
                    ProviderUtils.addExtraWhere(selection, Domain._ID+"=?"),
                    ProviderUtils.addExtraWhereArgs(selectionArgs, uri.getLastPathSegment()));

        default:
            throw new IllegalArgumentException("delete not supported for the given uri");
        }
    }

    private static final UriMatcher mUriMatcher;
    static {
        mUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

        mUriMatcher.addURI(AUTHORITY, Domain.PATH, MATCHER_DOMAIN_DIR);
        mUriMatcher.addURI(AUTHORITY, Domain.PATH + "/#", MATCHER_DOMAIN_ITEM);

    }
}
