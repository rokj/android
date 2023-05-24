package com.owncloud.android.operations.common;

public class NewRemoteOperationResult {
    public ResultCode resultCode;

    public NewRemoteOperationResult(ResultCode resultCode) {
        this.resultCode = resultCode;
    }

    public static enum ResultCode {
        OK,
        OK_SSL,
        OK_NO_SSL,
        UNHANDLED_HTTP_CODE,
        UNAUTHORIZED,
        FILE_NOT_FOUND,
        INSTANCE_NOT_CONFIGURED,
        UNKNOWN_ERROR,
        WRONG_CONNECTION,
        TIMEOUT,
        INCORRECT_ADDRESS,
        HOST_NOT_AVAILABLE,
        NO_NETWORK_CONNECTION,
        SSL_ERROR,
        SSL_RECOVERABLE_PEER_UNVERIFIED,
        BAD_OC_VERSION,
        CANCELLED,
        INVALID_LOCAL_FILE_NAME,
        INVALID_OVERWRITE,
        CONFLICT,
        OAUTH2_ERROR,
        SYNC_CONFLICT,
        LOCAL_STORAGE_FULL,
        LOCAL_STORAGE_NOT_MOVED,
        LOCAL_STORAGE_NOT_COPIED,
        OAUTH2_ERROR_ACCESS_DENIED,
        QUOTA_EXCEEDED,
        ACCOUNT_NOT_FOUND,
        ACCOUNT_EXCEPTION,
        ACCOUNT_NOT_NEW,
        ACCOUNT_NOT_THE_SAME,
        INVALID_CHARACTER_IN_NAME,
        SHARE_NOT_FOUND,
        LOCAL_STORAGE_NOT_REMOVED,
        FORBIDDEN,
        SHARE_FORBIDDEN,
        OK_REDIRECT_TO_NON_SECURE_CONNECTION,
        INVALID_MOVE_INTO_DESCENDANT,
        INVALID_COPY_INTO_DESCENDANT,
        PARTIAL_MOVE_DONE,
        PARTIAL_COPY_DONE,
        SHARE_WRONG_PARAMETER,
        WRONG_SERVER_RESPONSE,
        INVALID_CHARACTER_DETECT_IN_SERVER,
        DELAYED_FOR_WIFI,
        DELAYED_FOR_CHARGING,
        LOCAL_FILE_NOT_FOUND,
        NOT_AVAILABLE,
        MAINTENANCE_MODE,
        LOCK_FAILED,
        DELAYED_IN_POWER_SAVE_MODE,
        ACCOUNT_USES_STANDARD_PASSWORD,
        METADATA_NOT_FOUND,
        OLD_ANDROID_API,
        UNTRUSTED_DOMAIN,
        ETAG_CHANGED,
        ETAG_UNCHANGED,
        VIRUS_DETECTED,
        FOLDER_ALREADY_EXISTS,
        CANNOT_CREATE_FILE;

        private ResultCode() {
        }
    }
}