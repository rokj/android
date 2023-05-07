package com.owncloud.android.authentication;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.UploadObjectArgs;
import io.minio.errors.MinioException;

import com.nextcloud.client.account.User;
import com.nextcloud.client.account.UserAccountManager;
import com.owncloud.android.MainApp;
import com.owncloud.android.R;

import com.owncloud.android.databinding.S3LoginBinding;
import com.owncloud.android.lib.common.UserInfo;
import com.owncloud.android.lib.common.accounts.AccountUtils;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.providers.DocumentsStorageProvider;
import com.owncloud.android.ui.activity.FileDisplayActivity;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import javax.inject.Inject;

import androidx.appcompat.app.AppCompatActivity;
import io.minio.messages.Bucket;

public class S3LoginActivity extends AppCompatActivity {
    private S3LoginBinding binding;
    private Account mAccount;
    private AccountManager mAccountMgr;
    private Bundle mResultBundle;

    @Inject
    protected UserAccountManager accountManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAccountMgr = AccountManager.get(this);

        setContentView(R.layout.s3_login);

        Button button = findViewById(R.id.real_s3login);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String bucketName = "tmp";


                try {
                    // Create a minioClient with the MinIO server playground, its access key and secret key.
                    MinioClient minioClient =
                        MinioClient.builder()
                            .endpoint("https://moja.shramba.arnes.si")
                            .credentials("EAN71J9WLBWFUIMD5ZTO", "ST39OWIyTQnwWGCGlxCM7mfoLRTyx2woiAGT6OjM")
                            .build();

                    List<Bucket> bucketList = minioClient.listBuckets();
                    for (Bucket bucket : bucketList) {
                        System.out.println(bucket.creationDate() + ", " + bucket.name());
                        Log.i("FAFA", bucket.name());
                    }
                } catch (Exception e) {
                    System.out.println("Error occurred: " + e);
                    // System.out.println("HTTP trace: " + e.httpTrace());
                }

                // createAccount();

                Intent i = new Intent(v.getContext(), FileDisplayActivity.class);
                i.setAction(FileDisplayActivity.RESTART);
                i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(i);
            }
        });
    }

    /**
     * Creates a new account through the Account Authenticator that started this activity.
     * <p>
     * This makes the account permanent.
     * <p>
     * TODO Decide how to name the OAuth accounts
     */
    @SuppressFBWarnings("DMI")
    @SuppressLint("TrulyRandom")
    protected boolean createAccount() {
        String accountType = MainApp.getAccountType(this);

        // TODO: add host url from UI
        Uri uri = Uri.parse("https://moja.shramba.arnes.si");
        // used for authenticate on every login/network connection, determined by first login (weblogin/old login)
        // can be anything: email, name, name with whitespaces
        String loginName = "rokj";

        String accountName = com.owncloud.android.lib.common.accounts.AccountUtils.buildAccountName(uri, loginName);
        Account newAccount = new Account(accountName, accountType);
        if (MainApp.userAccountManager.exists(newAccount)) {
            // fail - not a new account, but an existing one; disallow
            RemoteOperationResult result = new RemoteOperationResult(RemoteOperationResult.ResultCode.ACCOUNT_NOT_NEW);

            // updateAuthStatusIconAndText(result);
            // showAuthStatus();

            Log_OC.d("FAFA", result.getLogMessage());
            return false;

        } else {
            mAccount = newAccount;
            mAccountMgr.addAccountExplicitly(mAccount, "somepassword", null);
            mAccountMgr.notifyAccountAuthenticated(mAccount);
            // mAccountMgr.setAccountVisibility()

            // add the new account as default in preferences, if there is none already
            User defaultAccount = MainApp.userAccountManager.getUser();
            if (defaultAccount.isAnonymous()) {
                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
                editor.putString("select_oc_account", accountName);
                editor.apply();
            }

            /// prepare result to return to the Authenticator
            //  TODO check again what the Authenticator makes with it; probably has the same
            //  effect as addAccountExplicitly, but it's not well done
            final Intent intent = new Intent();
            intent.putExtra(AccountManager.KEY_ACCOUNT_TYPE, accountType);
            intent.putExtra(AccountManager.KEY_ACCOUNT_NAME, mAccount.name);
            intent.putExtra(AccountManager.KEY_USERDATA, loginName);

            /// add user data to the new account; TODO probably can be done in the last parameter
            //      addAccountExplicitly, or in KEY_USERDATA
            // mAccountMgr.setUserData(mAccount, AccountUtils.Constants.KEY_OC_VERSION, mServerInfo.mVersion.getVersion());
            // mAccountMgr.setUserData(mAccount, AccountUtils.Constants.KEY_OC_BASE_URL, mServerInfo.mBaseUrl);

            // TODO:
            mAccountMgr.setUserData(mAccount, AccountUtils.Constants.KEY_DISPLAY_NAME, "Rok Jaklic - test");
            mAccountMgr.setUserData(mAccount, AccountUtils.Constants.KEY_USER_ID, "rokj");
            mAccountMgr.setUserData(mAccount,
                                    AccountUtils.Constants.KEY_OC_ACCOUNT_VERSION,
                                    Integer.toString(UserAccountManager.ACCOUNT_VERSION_WITH_PROPER_ID));


            setAccountAuthenticatorResult(intent.getExtras());
            setResult(RESULT_OK, intent);

            // notify Document Provider
            DocumentsStorageProvider.notifyRootsChanged(this);

            return true;
        }
    }

    /**
     * Set the result that is to be sent as the result of the request that caused this Activity to be launched.
     * If result is null or this method is never called then the request will be canceled.
     *
     * @param result this is returned as the result of the AbstractAccountAuthenticator request
     */
    public final void setAccountAuthenticatorResult(Bundle result) {
        mResultBundle = result;
    }

    private static long calKb(Long val) {
        return val/1024;
    }

    @Override
    public void onBackPressed() {
        Intent authenticatorActivityIntent = new Intent(this, AuthenticatorActivity.class);
        startActivity(authenticatorActivityIntent);
    }
}
