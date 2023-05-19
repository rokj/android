package com.owncloud.android.authentication;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.annotation.SuppressLint;
import android.content.Context;
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

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.nextcloud.client.account.Server;
import com.nextcloud.client.account.User;
import com.nextcloud.client.account.UserAccountManager;
import com.nextcloud.client.account.UserImpl;
import com.nextcloud.client.di.Injectable;
import com.nextcloud.client.mixins.SessionMixin;
import com.owncloud.android.MainApp;
import com.owncloud.android.R;

import com.owncloud.android.databinding.S3LoginBinding;
import com.owncloud.android.lib.common.UserInfo;
import com.owncloud.android.lib.common.accounts.AccountUtils;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.status.OwnCloudVersion;
import com.owncloud.android.providers.DocumentsStorageProvider;
import com.owncloud.android.ui.activity.BaseActivity;
import com.owncloud.android.ui.activity.FileDisplayActivity;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import javax.inject.Inject;

import androidx.appcompat.app.AppCompatActivity;
import io.minio.messages.Bucket;

import static com.owncloud.android.MainApp.PREFS_NAME;
import static com.owncloud.android.MainApp.PREF_ACCESS_KEY;
import static com.owncloud.android.MainApp.PREF_HOSTNAME;
import static com.owncloud.android.MainApp.PREF_SECRET_KEY;

public class S3LoginActivity extends BaseActivity implements Injectable {
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
                String hostName = "";
                String accessKey = "";
                String secretKey = "";

                TextInputLayout hostNameInput = findViewById(R.id.s3_hostname_container);
                hostName = hostNameInput.getEditText().getText().toString();
                TextInputLayout accessKeyInput = findViewById(R.id.s3_access_key_container);
                accessKey = accessKeyInput.getEditText().getText().toString();
                TextInputLayout secretKeyInput = findViewById(R.id.s3_secret_key_container);
                secretKey = secretKeyInput.getEditText().getText().toString();

                try {
                    MinioClient minioClient =
                        MinioClient.builder()
                            .endpoint(hostName)
                            .credentials(accessKey, secretKey)
                            .build();

                    List<Bucket> bucketList = minioClient.listBuckets();
                    for (Bucket bucket : bucketList) {
                        Log.d("S3", bucket.name());
                    }

                    MainApp.minioClient = minioClient;
                    MainApp.s3HostName = hostName;
                    MainApp.s3AccessKey = accessKey;
                    // TODO save preferences for "certain" time or revoke access key and secret key after some time
                    savePreferences(hostName, accessKey, secretKey);

                    sessionMixin = new SessionMixin(S3LoginActivity.this,
                                                    getContentResolver(),
                                                    accountManager);
                    Server server = new Server(URI.create(hostName),
                                               OwnCloudVersion.nextcloud_20);
                    User user = new UserImpl(S3LoginActivity.this, accessKey, server);
                    sessionMixin.setUser(user);

                    mixinRegistry.add(sessionMixin);
                } catch (Exception e) {
                    Log.d("S3", e.toString());
                    // TODO show message invalid credentials or something
                    return;
                }

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
//    @SuppressFBWarnings("DMI")
//    @SuppressLint("TrulyRandom")
//    protected boolean createAccount() {
//        String accountType = MainApp.getAccountType(this);
//
//        // TODO: add host url from UI
//        Uri uri = Uri.parse("https://moja.shramba.arnes.si");
//        // used for authenticate on every login/network connection, determined by first login (weblogin/old login)
//        // can be anything: email, name, name with whitespaces
//        String loginName = "rokj";
//
//        String accountName = com.owncloud.android.lib.common.accounts.AccountUtils.buildAccountName(uri, loginName);
//        Account newAccount = new Account(accountName, accountType);
//        if (MainApp.userAccountManager.exists(newAccount)) {
//            // fail - not a new account, but an existing one; disallow
//            RemoteOperationResult result = new RemoteOperationResult(RemoteOperationResult.ResultCode.ACCOUNT_NOT_NEW);
//
//            // updateAuthStatusIconAndText(result);
//            // showAuthStatus();
//
//            Log_OC.d("FAFA", result.getLogMessage());
//            return false;
//
//        } else {
//            mAccount = newAccount;
//            mAccountMgr.addAccountExplicitly(mAccount, "somepassword", null);
//            mAccountMgr.notifyAccountAuthenticated(mAccount);
//            // mAccountMgr.setAccountVisibility()
//
//            // add the new account as default in preferences, if there is none already
//            User defaultAccount = MainApp.userAccountManager.getUser();
//            if (defaultAccount.isAnonymous()) {
//                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
//                editor.putString("select_oc_account", accountName);
//                editor.apply();
//            }
//
//            /// prepare result to return to the Authenticator
//            //  TODO check again what the Authenticator makes with it; probably has the same
//            //  effect as addAccountExplicitly, but it's not well done
//            final Intent intent = new Intent();
//            intent.putExtra(AccountManager.KEY_ACCOUNT_TYPE, accountType);
//            intent.putExtra(AccountManager.KEY_ACCOUNT_NAME, mAccount.name);
//            intent.putExtra(AccountManager.KEY_USERDATA, loginName);
//
//            /// add user data to the new account; TODO probably can be done in the last parameter
//            //      addAccountExplicitly, or in KEY_USERDATA
//            // mAccountMgr.setUserData(mAccount, AccountUtils.Constants.KEY_OC_VERSION, mServerInfo.mVersion.getVersion());
//            // mAccountMgr.setUserData(mAccount, AccountUtils.Constants.KEY_OC_BASE_URL, mServerInfo.mBaseUrl);
//
//            // TODO:
//            mAccountMgr.setUserData(mAccount, AccountUtils.Constants.KEY_DISPLAY_NAME, "Rok Jaklic - test");
//            mAccountMgr.setUserData(mAccount, AccountUtils.Constants.KEY_USER_ID, "rokj");
//            mAccountMgr.setUserData(mAccount,
//                                    AccountUtils.Constants.KEY_OC_ACCOUNT_VERSION,
//                                    Integer.toString(UserAccountManager.ACCOUNT_VERSION_WITH_PROPER_ID));
//
//
//            setAccountAuthenticatorResult(intent.getExtras());
//            setResult(RESULT_OK, intent);
//
//            // notify Document Provider
//            DocumentsStorageProvider.notifyRootsChanged(this);
//
//            return true;
//        }
//    }

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

    private void savePreferences(String hostname, String accessKey, String secretKey) {
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();

        editor.putString(PREF_HOSTNAME, hostname);
        editor.putString(PREF_ACCESS_KEY, accessKey);
        editor.putString(PREF_SECRET_KEY, secretKey);
        editor.commit();
    }
}
