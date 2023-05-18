/*
 * Nextcloud Android client application
 *
 * @author Chris Narkiewicz
 * Copyright (C) 2019 Chris Narkiewicz
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU AFFERO GENERAL PUBLIC LICENSE
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU AFFERO GENERAL PUBLIC LICENSE for more details.
 *
 * You should have received a copy of the GNU Affero General Public
 * License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.nextcloud.client.account;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import com.nextcloud.common.NextcloudClient;
import com.nextcloud.java.util.Optional;
import com.owncloud.android.MainApp;
import com.owncloud.android.R;
import com.owncloud.android.authentication.AuthenticatorActivity;
import com.owncloud.android.datamodel.ArbitraryDataProvider;
import com.owncloud.android.datamodel.ArbitraryDataProviderImpl;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.lib.common.OwnCloudAccount;
import com.owncloud.android.lib.common.OwnCloudClientManagerFactory;
import com.owncloud.android.lib.common.UserInfo;
import com.owncloud.android.lib.common.accounts.AccountUtils;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.status.OwnCloudVersion;
import com.owncloud.android.lib.resources.users.GetUserInfoRemoteOperation;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

@SuppressLint("ParcelCreator")
public class UserImpl implements User {

    private static final String TAG = UserImpl.class.getSimpleName();
    private static final String PREF_SELECT_OC_ACCOUNT = "select_oc_account";

    private Context context;
    private String accountName;
    private Server server;

    public static final Parcelable.Creator<UserImpl> CREATOR = new Parcelable.Creator<UserImpl>() {

        @Override
        public UserImpl createFromParcel(Parcel source) {
            return new UserImpl(source);
        }

        @Override
        public UserImpl[] newArray(int size) {
            return new UserImpl[size];
        }
    };

    public UserImpl(Parcel source) {
    }

    public static UserImpl fromContext(Context context, String accountName, Server server) {
        return new UserImpl(context, accountName, server);
    }

    @Inject
    public UserImpl(Context context, String accountName, Server server) {
        this.context = context;
        this.accountName = accountName;
        this.server = server;
    }

    @NonNull
    @Override
    public String getAccountName() {
        return this.accountName;
    }

    @NonNull
    @Override
    public Server getServer() {
        return null;
    }

    @Override
    public boolean isAnonymous() {
        return false;
    }

    @NonNull
    @Override
    public Account toPlatformAccount() {
        return null;
    }

    @NonNull
    @Override
    public OwnCloudAccount toOwnCloudAccount() {
        return null;
    }

    @Override
    public boolean nameEquals(@Nullable User user) {
        return false;
    }

    @Override
    public boolean nameEquals(@Nullable CharSequence accountName) {
        return false;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {

    }
}
