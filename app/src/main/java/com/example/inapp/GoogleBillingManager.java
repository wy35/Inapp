package com.example.inapp;

import android.app.Activity;
import android.support.annotation.Nullable;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.ConsumeResponseListener;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GoogleBillingManager {

    private static final String BASE_64_ENCODED_PUBLIC_KEY = "平台的key";
    private BillingClient mBillingClient;
    private OnPurchasesUpdatedListener mOnPurchasesUpdatedListener;
    private OnConnectionStateChangedListener mOnConnectionListener;
    private boolean mIsServiceConnected;
    private boolean isAutoConsume = true;


    private static final GoogleBillingManager INSTANCE = new GoogleBillingManager();

    private GoogleBillingManager() {

    }

    public static GoogleBillingManager getInstance() {
        return INSTANCE;
    }

    public GoogleBillingManager create() {
        if (mBillingClient == null) {
            mBillingClient = BillingClient.newBuilder(App.getContext()).setListener(new MyPurchasesUpdatedListener()).build();
        }
        startServiceConnection();
        return INSTANCE;
    }

    public GoogleBillingManager setOnPurchasesUpdatedListener(OnPurchasesUpdatedListener listener) {
        mOnPurchasesUpdatedListener = listener;
        return INSTANCE;
    }

    public GoogleBillingManager setOnConnectionStateChangedListener(OnConnectionStateChangedListener listener) {
        mOnConnectionListener = listener;
        return INSTANCE;
    }

    /**
     * 连接服务
     */
    public void startServiceConnection() {
        mBillingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(@BillingClient.BillingResponse int billingResponseCode) {
                if (billingResponseCode == BillingClient.BillingResponse.OK) {
                    mIsServiceConnected = true;
                    if (mOnConnectionListener != null) {
                        mOnConnectionListener.onConnectionSuccess();
                    }
                    queryPurchases();
                } else {
                    mIsServiceConnected = false;
                    if (mOnConnectionListener != null) {
                        mOnConnectionListener.onConnectionFail(billingResponseCode);
                    }
                }
            }

            @Override
            public void onBillingServiceDisconnected() {
                mIsServiceConnected = false;
                if (mOnConnectionListener != null) {
                    mOnConnectionListener.onDisconnected();
                }
            }
        });
    }

    /**
     * 查询已购买内购商品，自动消耗
     */
    public void queryPurchases() {
        Runnable queryToExecute = new Runnable() {
            @Override
            public void run() {
                Purchase.PurchasesResult purchasesResult = mBillingClient.queryPurchases(BillingClient.SkuType.INAPP);
                if (purchasesResult != null) {
                    if (purchasesResult.getResponseCode() == BillingClient.BillingResponse.OK) {
                        List<Purchase> purchaseList = purchasesResult.getPurchasesList();
                        if (isAutoConsume) {
                            if (purchaseList != null) {
                                for (Purchase purchase : purchaseList) {
                                    consumeAsync(purchase.getPurchaseToken());
                                }
                            }
                        }
                    }
                }
            }
        };

        executeServiceRequest(queryToExecute);
    }

    /**
     * 消耗内购
     */
    private void consumeAsync(final String purchaseToken) {

        Runnable consumeRequest = new Runnable() {
            @Override
            public void run() {
                mBillingClient.consumeAsync(purchaseToken, new ConsumeResponseListener() {
                    @Override
                    public void onConsumeResponse(int responseCode, String purchaseToken) {

                    }
                });
            }
        };

        executeServiceRequest(consumeRequest);
    }

    /**
     * 发起内购
     *
     * @param activity
     * @param skuId
     */
    public void purchaseInApp(Activity activity, String skuId) {
        initiatePurchaseFlow(activity, skuId, BillingClient.SkuType.INAPP);
    }

    /**
     * Start a purchase flow
     */
    public void initiatePurchaseFlow(Activity activity, final String skuId, final String billingType) {
        initiatePurchaseFlow(activity, skuId, null, billingType);
    }

    /**
     * Start a purchase or subscription replace flow
     */
    public void initiatePurchaseFlow(final Activity activity, final String skuId, final ArrayList<String> oldSkus,
                                     final String billingType) {
        Runnable purchaseFlowRequest = new Runnable() {
            @Override
            public void run() {

                BillingFlowParams purchaseParams = BillingFlowParams.newBuilder()
                        .setSku(skuId).setType(billingType).setOldSkus(oldSkus).build();
                mBillingClient.launchBillingFlow(activity, purchaseParams);
            }
        };

        executeServiceRequest(purchaseFlowRequest);
    }

    private void executeServiceRequest(Runnable runnable) {
        if (mIsServiceConnected) {
            runnable.run();
        } else {
            startServiceConnection();
        }
    }

    /**
     * 连接服务回调
     */
    public interface OnConnectionStateChangedListener {
        void onConnectionSuccess();

        void onConnectionFail(int responseCode);

        void onDisconnected();
    }

    /**
     * 自定义购买结果回调
     */
    public interface OnPurchasesUpdatedListener {
        void onPurchaseSuccess(List<Purchase> purchases);

        void onPurchaseFail(int responseCode);
    }

    /**
     * 原始购买结果回调
     */
    private class MyPurchasesUpdatedListener implements PurchasesUpdatedListener {
        @Override
        public void onPurchasesUpdated(int responseCode, @Nullable List<Purchase> purchases) {
            if (responseCode == BillingClient.BillingResponse.OK && purchases != null) {
                if (isAutoConsume) {
                    for (Purchase purchase : purchases) {
                        if (isInapp(purchase)) {
                            consumeAsync(purchase.getPurchaseToken());
                        }
                    }
                }
                List<Purchase> filterPurchases = filterPurchases(purchases);
                if (mOnPurchasesUpdatedListener != null) {
                    mOnPurchasesUpdatedListener.onPurchaseSuccess(filterPurchases);
                }
            } else {
                if (mOnPurchasesUpdatedListener != null) {
                    mOnPurchasesUpdatedListener.onPurchaseFail(responseCode);
                }
            }
        }
    }

    private boolean isInapp(Purchase purchase) {
        //如果是订阅，修改
        return true;
    }

    /**
     * 验证BASE_64_ENCODED_PUBLIC_KEY，过滤商品，只是简单验证，为了安全建议放到后台
     *
     * @param purchases 购买的商品
     * @return 通过验证的商品
     */
    private List<Purchase> filterPurchases(List<Purchase> purchases) {
        List<Purchase> filterPurchases = new ArrayList<>();
        for (Purchase purchase : purchases) {
            if (verifyValidSignature(purchase.getOriginalJson(), purchase.getSignature())) {
                filterPurchases.add(purchase);
            }
        }
        return filterPurchases;
    }

    private boolean verifyValidSignature(String signedData, String signature) {
        try {
            return Security.verifyPurchase(BASE_64_ENCODED_PUBLIC_KEY, signedData, signature);
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * 资源回收
     */
    public void destroy() {

        if (mBillingClient != null && mBillingClient.isReady()) {
            mBillingClient.endConnection();
            mBillingClient = null;
        }
    }
}
