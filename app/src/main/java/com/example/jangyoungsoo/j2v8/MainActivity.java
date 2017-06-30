package com.example.jangyoungsoo.j2v8;

import android.Manifest;
import android.content.ContentResolver;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.provider.ContactsContract;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.eclipsesource.v8.JavaCallback;
import com.eclipsesource.v8.JavaVoidCallback;
import com.eclipsesource.v8.Releasable;
import com.eclipsesource.v8.V8;
import com.eclipsesource.v8.V8Array;
import com.eclipsesource.v8.V8Object;

public class MainActivity extends AppCompatActivity {

    private final int MY_PERMISSIONS_REQUEST_READ_CONTACTS = 123;

    TextView tv_contacts;
    Button btn_contacts;

    V8 runtime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tv_contacts = (TextView) findViewById(R.id.tv_contacts);
        btn_contacts = (Button) findViewById(R.id.btn_contacts);

        runtime = V8.createV8Runtime();

        JavaVoidCallback callbackPrint = new JavaVoidCallback() {
            public void invoke(final V8Object receiver, final V8Array parameters) {
                if (parameters.length() > 0) {
                    Object arg1 = parameters.get(0);
                    Log.d("V8Result", "#################### " + arg1.toString());
                    if (arg1 instanceof Releasable) {
                        ((Releasable) arg1).release();
                    }
                }
            }
        };
        JavaCallback callbackGetContacts = new JavaCallback() {
            public String invoke(final V8Object receiver, final V8Array parameters) {
                return getContacts();
            }
        };
        JavaVoidCallback callbackSetText = new JavaVoidCallback() {
            public void invoke(final V8Object receiver, final V8Array parameters) {
                if (parameters.length() > 0) {
                    Object arg1 = parameters.get(0);
                    Log.e("TEXT", "type: " + arg1.getClass().getName());
                    tv_contacts.setText(arg1.toString());
                    if (arg1 instanceof Releasable) {
                        ((Releasable) arg1).release();
                    }
                }
            }
        };
        runtime.registerJavaMethod(callbackPrint, "print");
        runtime.registerJavaMethod(callbackGetContacts, "getContacts");
        runtime.registerJavaMethod(callbackSetText, "setText");
//        runtime.registerJavaMethod(callbackPrint, "setText");

        btn_contacts.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                Log.d("TEST", "#################### Button clicked!");
                if (ContextCompat.checkSelfPermission(getApplicationContext(),
                        Manifest.permission.READ_CONTACTS)
                        != PackageManager.PERMISSION_GRANTED) {
                    // Should we show an explanation?
                    if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,
                            Manifest.permission.READ_CONTACTS)) {
                        ActivityCompat.requestPermissions(MainActivity.this,
                                new String[]{Manifest.permission.READ_CONTACTS},
                                MY_PERMISSIONS_REQUEST_READ_CONTACTS);
                    } else {
                        ActivityCompat.requestPermissions(MainActivity.this,
                                new String[]{Manifest.permission.READ_CONTACTS},
                                MY_PERMISSIONS_REQUEST_READ_CONTACTS);
                    }
                } else {
                    runtime.executeScript("setText(getContacts());");
//                    runtime.executeScript("setText(Math.random());");
//                    runtime.executeScript("setText([1,2,3,4,'hi']);");
                }
            }
        });
    }

    public String getContacts() {
        StringBuilder stringBuilder = new StringBuilder();
        ContentResolver cr = getContentResolver();
        Cursor cur = cr.query(ContactsContract.Contacts.CONTENT_URI, null, null, null, null);

        if (cur.getCount() > 0) {
            while (cur.moveToNext()) {
                String id = cur.getString(
                        cur.getColumnIndex(ContactsContract.Contacts._ID));
                String name = cur.getString(cur.getColumnIndex(
                        ContactsContract.Contacts.DISPLAY_NAME));

                if (cur.getInt(cur.getColumnIndex(
                        ContactsContract.Contacts.HAS_PHONE_NUMBER)) > 0) {
                    Cursor pCur = cr.query(
                            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                            null,
                            ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                            new String[]{id}, null);
                    while (pCur.moveToNext()) {
                        String phoneNo = pCur.getString(pCur.getColumnIndex(
                                ContactsContract.CommonDataKinds.Phone.NUMBER));
                        stringBuilder.append(name + ", " + phoneNo + "\n");
                    }
                    pCur.close();
                }
            }
        }

        return stringBuilder.toString();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_READ_CONTACTS: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted
                    runtime.executeScript("setText(getContacts());");
                } else {
                    // permission denied
                }
                return;
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        runtime.release();
        Log.d("TEST", "#################### Application finished!");
    }

}
