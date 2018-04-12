package com.example.inapp;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

import com.android.billingclient.api.Purchase;

import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private GoogleBillingManager mBillingManager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mBillingManager = GoogleBillingManager.getInstance()
                .setOnPurchasesUpdatedListener(new MyOnPurchasesUpdatedListener())
                .setOnConnectionStateChangedListener(new MyOnConnectionStateChangedListener())
                .create();
    }

    private class MyOnPurchasesUpdatedListener implements GoogleBillingManager.OnPurchasesUpdatedListener {

        @Override
        public void onPurchaseSuccess(List<Purchase> purchases) {
            Log.e(TAG, "购买成功" + purchases.toString());
        }

        @Override
        public void onPurchaseFail(int responseCode) {
            Log.e(TAG, "购买失败" + responseCode);
        }
    }

    private class MyOnConnectionStateChangedListener implements GoogleBillingManager.OnConnectionStateChangedListener {

        @Override
        public void onConnectionSuccess() {
            Log.e(TAG, "连接成功");
        }

        @Override
        public void onConnectionFail(int responseCode) {
            Log.e(TAG, "连接失败");
        }

        @Override
        public void onDisconnected() {
            Log.e(TAG, "断开连接");
        }
    }

    public void click(View view) {
        mBillingManager.purchaseInApp(this, "商品id");
    }

    @Override
    protected void onDestroy() {
        mBillingManager.destroy();
        super.onDestroy();
    }
}
