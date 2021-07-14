package info.staticfree.supergenpass;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.support.annotation.NonNull;

public class RememberedDBHelper extends SQLiteOpenHelper {
    private static final String DB_NAME = "autocomplete_domains";
    public static final String DB_DOMAINS_TABLE = "domains";
    private static final int DB_VERSION = 2;

    public RememberedDBHelper(@NonNull Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(@NonNull SQLiteDatabase db) {
        db.execSQL("CREATE TABLE '" + DB_DOMAINS_TABLE +
                "' ('" + Domain._ID + "' INTEGER PRIMARY KEY, '" + Domain.DOMAIN +
                "' VARCHAR(255))");
    }

    @Override
    public void onUpgrade(@NonNull SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + DB_DOMAINS_TABLE);
        onCreate(db);
    }
}
