package info.staticfree.SuperGenPass;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.widget.FilterQueryProvider;
import android.widget.SimpleCursorAdapter;

public class RememberedDBHelper extends SQLiteOpenHelper {
	private final static String DB_NAME = "autocomplete_domains";
	private final static String DB_DOMAINS_TABLE = "domains";
	private final static int DB_VERSION = 2;
	private final Context context;

	public RememberedDBHelper(Context context) {
		super(context, DB_NAME, null, DB_VERSION);
		this.context = context;
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL("CREATE TABLE '"+DB_DOMAINS_TABLE+
				"' ('_id' INTEGER PRIMARY KEY, 'domain' VARCHAR(255))");

	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		db.execSQL("DROP TABLE IF EXISTS "+ DB_DOMAINS_TABLE);
		onCreate(db);
	}

	   /**
     * Adds the given domain to the list of remembered domains.
     *
     * @param domain the filtered domain name
     */
    static void addRememberedDomain(SQLiteDatabase db, String domain){
    	final Cursor existingEntries = db.query(DB_DOMAINS_TABLE, null,
    			"domain=?", new String[] {domain},
    			null, null, null);

    	if (existingEntries.getCount() == 0) {
	    	final ContentValues cv = new ContentValues();
	    	cv.put("domain", domain);
	    	db.insert(DB_DOMAINS_TABLE, null, cv);
    	}
    	existingEntries.close();
    }

    static void clearRememberedDomains(SQLiteDatabase db){
    	db.delete(DB_DOMAINS_TABLE, null, null);
    }


    /**
     * Creates an Adapter that looks for the start of a domain string from the database.
     * For use with the AutoCompleteTextView
     *
     * @param db the domain database
     * @return an Adapter that uses the Simple Dropdown Item view
     */
    public SimpleCursorAdapter getDomainPrefixAdapter(final Activity activity, SQLiteDatabase db){
		final Cursor dbCursor = db.query(DB_DOMAINS_TABLE, null, null, null, null, null, "domain ASC");
		activity.startManagingCursor(dbCursor);

		// Simple it says - ha!
		final SimpleCursorAdapter adapter = new SimpleCursorAdapter(context,
				android.R.layout.simple_dropdown_item_1line,
				dbCursor,
				new String[] {"domain"},
				// Not sure if the below line is correct, but it seems to work.
				new int[] {android.R.id.text1} );

		adapter.setStringConversionColumn(dbCursor.getColumnIndex("domain"));

		final SQLiteDatabase dbInner = db;
		// a filter that searches for domains starting with the given constraint
		adapter.setFilterQueryProvider(new FilterQueryProvider(){
			public Cursor runQuery(CharSequence constraint) {
				Cursor c;
				if (constraint == null || constraint.length() == 0){
					c = dbInner.query(DB_DOMAINS_TABLE, null, null, null, null, null, "domain ASC");
				}else{
					c = dbInner.query(DB_DOMAINS_TABLE,
							null,
							"domain GLOB ?",
							new String[] {constraint.toString()+"*"}, null, null, "domain ASC");
				}
				activity.startManagingCursor(c);
				return c;
			}
		});
		return adapter;
    }

}
