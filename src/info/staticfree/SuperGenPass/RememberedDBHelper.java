package info.staticfree.SuperGenPass;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class RememberedDBHelper extends SQLiteOpenHelper {
	private final static String DB_NAME = "autocomplete_domains";
	public final static String DB_DOMAINS_TABLE = "domains";
	private final static int DB_VERSION = 2;

	public RememberedDBHelper(Context context) {
		super(context, DB_NAME, null, DB_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL("CREATE TABLE '"+DB_DOMAINS_TABLE+
				"' ('"+Domain._ID+"' INTEGER PRIMARY KEY, '"+Domain.DOMAIN+"' VARCHAR(255))");

	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		db.execSQL("DROP TABLE IF EXISTS "+ DB_DOMAINS_TABLE);
		onCreate(db);
	}
}
