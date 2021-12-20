package com.mustafacan.clouddbdemo;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.huawei.agconnect.auth.AGConnectAuth;
import com.huawei.agconnect.auth.AGConnectAuthCredential;
import com.huawei.agconnect.auth.AGConnectUser;
import com.huawei.agconnect.cloud.database.AGConnectCloudDB;
import com.huawei.agconnect.cloud.database.CloudDBZone;
import com.huawei.agconnect.cloud.database.CloudDBZoneConfig;
import com.huawei.agconnect.cloud.database.CloudDBZoneObjectList;
import com.huawei.agconnect.cloud.database.CloudDBZoneQuery;
import com.huawei.agconnect.cloud.database.CloudDBZoneSnapshot;
import com.huawei.agconnect.cloud.database.exceptions.AGConnectCloudDBException;
import com.huawei.hmf.tasks.Task;
import com.mustafacan.clouddbdemo.model.*;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    public final String TAG = "HMSCLOUDDB";
    public CloudDBZone mCloudDBZone;
    public AGConnectCloudDB mCloudDB;
    public TextView log;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initAGConnectCloudDB(this);
        log = findViewById(R.id.log);

        AGConnectAuth.getInstance().signInAnonymously().addOnSuccessListener(signInResult -> {
            AGConnectUser user = signInResult.getUser();
            Log.i(TAG, "onCreate: User is Authenticated, Display Name: " + user.getDisplayName() );
            log.setText("User is Authenticated");
        }).addOnFailureListener(e -> {
            Log.e(TAG, "onCreate: Can't authorise the user, due to: " + e.getMessage() );
            log.setText("Error: " + e.getMessage());
        });
        startScreenFill();
        mCloudDB = AGConnectCloudDB.getInstance();
        startCloudDBInitialization();
    }

    public void startScreenFill() {
        TextView bookId = findViewById(R.id.bookid);
        TextView bookName = findViewById(R.id.book_name);
        TextView bookAuthor = findViewById(R.id.book_author);
        TextView bookPrice = findViewById(R.id.book_price);
        TextView bookPublisher = findViewById(R.id.book_publisher);

        bookId.setText("Book ID: 1");
        bookName.setText("Book Name: Crime and Punishment");
        bookAuthor.setText("Book Author: Fyodor Dostoyevski");
        bookPrice.setText("Book Price: 30.00");
        bookPublisher.setText("Book Publisher: Karbon Kitaplar");
    }

    public void startCloudDBInitialization() {
        try {
            mCloudDB.createObjectType(ObjectTypeInfoHelper.getObjectTypeInfo());
            openCloudDBZone();
        } catch (AGConnectCloudDBException e) {
            Log.e(TAG, "onCreate: Cannot Get Object Type");
            log.setText("Error: " + e.getMessage());
        }
    }

    public void openCloudDBZone() {
        CloudDBZoneConfig mConfig = new CloudDBZoneConfig("QuickStartDemo",
                CloudDBZoneConfig.CloudDBZoneSyncProperty.CLOUDDBZONE_CLOUD_CACHE,
                CloudDBZoneConfig.CloudDBZoneAccessProperty.CLOUDDBZONE_PUBLIC);
        mConfig.setPersistenceEnabled(true);
        Task<CloudDBZone> openDBZoneTask = mCloudDB.openCloudDBZone2(mConfig, true);
        openDBZoneTask.addOnSuccessListener(cloudDBZone -> {
            mCloudDBZone = cloudDBZone;
            log.setText("Cloud DB Zone Found, Name: " + mCloudDBZone.getCloudDBZoneConfig().getCloudDBZoneName());
        }).addOnFailureListener(e -> {
            Log.e(TAG, "onCreate: open CloudDBZone failed for: " + e.getMessage() );
            log.setText("Error: " + e.getMessage());
        });
    }

    public BookInfo addCrimeAndPunishment(BookInfo bookInfo) {
        bookInfo.setId(1);
        bookInfo.setBookName("Crime and Punishment");
        bookInfo.setAuthor("Fyodor Dostoyevski");
        bookInfo.setPrice(30.00);
        bookInfo.setPublisher("Karbon Kitaplar");
        try {
            bookInfo.setPublishTime(new SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH).parse("15/07/2016"));
        } catch (ParseException e) {
            Log.e(TAG, "addCrimeAndPunishment: Failed to parse date, e -> " + e.getMessage());
            log.setText("Error: " + e.getMessage());
            return null;
        }
        return bookInfo;
    }

    public void upsertBookInfo(BookInfo bookInfo) {
        if (mCloudDBZone == null) {
            Log.e(TAG, "upsertBookInfo: CloudDBZone is null, try re-opening");
            return;
        }

        Task<Integer> upsertTask = mCloudDBZone.executeUpsert(bookInfo);
        upsertTask.addOnSuccessListener(integer -> {
            Log.i(TAG, "upsertBookInfos: Upserted " + integer + " records");
            log.setText("upsertBookInfos: Upserted " + integer + " records");
        }).addOnFailureListener(e -> {
            Log.e(TAG, "upsertBookInfos: Insert book info failed with: " + e.getMessage() + "\n"
            + "AG Err Message: " + ((AGConnectCloudDBException)e).getErrMsg() + ", code: " + ((AGConnectCloudDBException)e).getCode());
            log.setText("Error: " + e.getMessage());

        });
    }

    public void deleteBookInfo(BookInfo bookInfo) {
        if (mCloudDBZone == null) {
            Log.e(TAG, "deleteBookInfo: CloudDBZone is null, try re-opening");
            return;
        }

        Task<Integer> deleteTask = mCloudDBZone.executeDelete(bookInfo);
        deleteTask.addOnSuccessListener(integer -> {
            Log.i(TAG, "deleteBookInfo: Deleted " + integer + " records");
            log.setText("deleteBookInfo: Deleted " + integer + " records");
        }).addOnFailureListener(e -> {
            Log.e(TAG, "deleteBookInfo: Delete book info failed with: " + e.getMessage() + "\n"
                    + "AG Err Message: " + ((AGConnectCloudDBException)e).getErrMsg() + ", code: " + ((AGConnectCloudDBException)e).getCode());
            log.setText("Error: " + e.getMessage());

        });
    }

    public void listQueriedObjects(CloudDBZoneObjectList<BookInfo> bookInfoCursor) {
        try {
            TextView queryText = findViewById(R.id.book_query);
            if (!bookInfoCursor.hasNext()) {
                queryText.setText("Query\n" +
                        "Book ID:\n" +
                        "Book Name:\n" +
                        "Book Author:\n" +
                        "Book Price:\n" +
                        "Book Publisher:");
            }
            while (bookInfoCursor.hasNext()) {
                BookInfo bookInfo = bookInfoCursor.next();
                queryText.setText("Query\n" +
                        "Book ID: " + bookInfo.getId() + "\n" +
                        "Book Name: " + bookInfo.getBookName() + "\n" +
                        "Book Author: " + bookInfo.getAuthor() + "\n" +
                        "Book Price: " + bookInfo.getPrice()+ "\n" +
                        "Book Publisher: " + bookInfo.getPublisher());
            }
            log.setText("listQueriedObjects: Queried " + bookInfoCursor.size() + " objects.");
        }catch (Exception e) {
            Log.e(TAG, "listQueriedObjects: Error while Listing Query results, e -> " + e.getMessage());
            log.setText("Error: " + e.getMessage());
        }
    }

    public void queryBookInfo() {
        if (mCloudDBZone == null) {
            Log.e(TAG, "deleteBookInfo: CloudDBZone is null, try re-opening");
            return;
        }

        Task<CloudDBZoneSnapshot<BookInfo>> queryTask = mCloudDBZone.executeQuery(
                CloudDBZoneQuery.where(BookInfo.class),
                CloudDBZoneQuery.CloudDBZoneQueryPolicy.POLICY_QUERY_FROM_CLOUD_ONLY);

        queryTask
        .addOnSuccessListener( bookInfoCloudDBZoneSnapshot -> {
            listQueriedObjects(bookInfoCloudDBZoneSnapshot.getSnapshotObjects());
        }).addOnFailureListener(e -> {
            Log.e(TAG, "queryBookInfo: e -> " + e.getMessage());
            log.setText("Error: " + e.getMessage());
        });
    }

    public static void initAGConnectCloudDB(Context context) {
        AGConnectCloudDB.initialize(context);
    }

    public void OnUpsertBookInfoClick(View view) {
        BookInfo book1 = new BookInfo();
        book1 = addCrimeAndPunishment(book1);
        upsertBookInfo(book1);
    }

    public void OnDeleteBookInfoClick(View view) {
        BookInfo book1 = new BookInfo();
        book1 = addCrimeAndPunishment(book1);
        deleteBookInfo(book1);
    }

    public void OnQueryBookInfoClick(View view) {
        queryBookInfo();
    }
}