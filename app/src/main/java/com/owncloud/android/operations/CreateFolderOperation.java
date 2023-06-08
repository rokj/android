/*
 *   ownCloud Android client application
 *
 *   @author David A. Velasco
 *   @author masensio
 *   Copyright (C) 2015 ownCloud Inc.
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License version 2,
 *   as published by the Free Software Foundation.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.owncloud.android.operations;

import android.content.Context;
import android.util.Pair;

import com.nextcloud.client.account.User;
import com.owncloud.android.MainApp;
import com.owncloud.android.datamodel.ArbitraryDataProvider;
import com.owncloud.android.datamodel.ArbitraryDataProviderImpl;
import com.owncloud.android.datamodel.DecryptedFolderMetadata;
import com.owncloud.android.datamodel.EncryptedFolderMetadata;
import com.owncloud.android.datamodel.FileDataStorageManager;
import com.owncloud.android.datamodel.OCFile;
import com.owncloud.android.lib.common.OwnCloudClient;
import com.owncloud.android.lib.common.operations.OnRemoteOperationListener;
import com.owncloud.android.lib.common.operations.RemoteOperation;
import com.owncloud.android.lib.common.operations.RemoteOperationResult;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.e2ee.ToggleEncryptionRemoteOperation;
import com.owncloud.android.lib.resources.files.CreateFolderRemoteOperation;
import com.owncloud.android.lib.resources.files.ReadFolderRemoteOperation;
import com.owncloud.android.lib.resources.files.model.RemoteFile;
import com.owncloud.android.operations.common.SyncOperation;
import com.owncloud.android.utils.EncryptionUtils;
import com.owncloud.android.utils.FileStorageUtils;
import com.owncloud.android.utils.MimeType;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

import androidx.annotation.NonNull;
import io.minio.MakeBucketArgs;
import io.minio.PutObjectArgs;
import io.minio.errors.ErrorResponseException;
import io.minio.errors.InsufficientDataException;
import io.minio.errors.InternalException;
import io.minio.errors.InvalidResponseException;
import io.minio.errors.ServerException;
import io.minio.errors.XmlParserException;

import static com.owncloud.android.datamodel.OCFile.PATH_SEPARATOR;
import static com.owncloud.android.datamodel.OCFile.ROOT_PATH;

/**
 * Access to remote operation performing the creation of a new folder in the ownCloud server.
 * Save the new folder in Database.
 */
public class CreateFolderOperation {

    private static final String TAG = CreateFolderOperation.class.getSimpleName();

    protected String remotePath;
    private RemoteFile createdRemoteFolder;
    private User user;
    private Context context;

    /**
     * Constructor
     */
    public CreateFolderOperation(String remotePath, User user, Context context, FileDataStorageManager storageManager) {
        this.remotePath = remotePath;
        this.user = user;
        this.context = context;
    }

    public void run() {
        String remoteParentPath = new File(remotePath).getParent();
        remoteParentPath = remoteParentPath.endsWith(PATH_SEPARATOR) ?
            remoteParentPath : remoteParentPath + PATH_SEPARATOR;

        OCFile parent = MainApp.storageManager.getFileByDecryptedRemotePath(remoteParentPath);

        String tempRemoteParentPath = remoteParentPath;
        while (parent == null) {
            tempRemoteParentPath = new File(tempRemoteParentPath).getParent();

            if (!tempRemoteParentPath.endsWith(PATH_SEPARATOR)) {
                tempRemoteParentPath = tempRemoteParentPath + PATH_SEPARATOR;
            }

            parent = MainApp.storageManager.getFileByDecryptedRemotePath(tempRemoteParentPath);
        }

        if (remoteParentPath.equals(ROOT_PATH)) {
            try {
                String bucket = getBucket();
                String path = getPath();

                if (bucket == null) {
                    return;
                }

                if (path == null || path.equals("") || path.equals("/")) {
                    MainApp.minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
                    saveFolderInDB();

                    return;
                }

                MainApp.minioClient.putObject(
                    PutObjectArgs.builder().bucket(bucket).object(path).stream(
                            new ByteArrayInputStream(new byte[]{}), 0, -1)
                        .build());
            }  catch (Exception e) {
                Log_OC.d("minio", e.toString());
            }
        } else {
//            try {
//                String bucket = getBucket();
//
//                MainApp.minioClient.putObject(
//                    PutObjectArgs.builder().bucket("my-bucketname").object("path/to/").stream(
//                            new ByteArrayInputStream(new byte[]{}), 0, -1)
//                        .build());
//            }  catch (Exception e) {
//                Log_OC.d("minio", e.toString());
//            }
        }
    }

    private String getBucket() {
        if (remotePath == null || remotePath.equals("")) {
            return null;
        }

        String[] path = remotePath.split("/");
        if (path.length == 0) {
            return null;
        }

        if (path.length >= 1) {
            return path[1];
        }

        Log_OC.d("minio", "could not get bucket from " + remotePath);

        return null;
    }

    private String getPath() {
        if (remotePath == null || remotePath.equals("")) {
            return null;
        }

        String path = remotePath.replace(getBucket(), "");
        path = path.replaceAll("[/]+", "/");

        if (path.equals("/")) {
            return null;
        }

        return path;
    }

    private RemoteOperationResult encryptedCreate(OCFile parent, OwnCloudClient client) {
        ArbitraryDataProvider arbitraryDataProvider = new ArbitraryDataProviderImpl(context);
        String privateKey = arbitraryDataProvider.getValue(user.getAccountName(), EncryptionUtils.PRIVATE_KEY);
        String publicKey = arbitraryDataProvider.getValue(user.getAccountName(), EncryptionUtils.PUBLIC_KEY);

        String token = null;
        Boolean metadataExists;
        DecryptedFolderMetadata metadata;
        String encryptedRemotePath = null;

        String filename = new File(remotePath).getName();

        try {
            // lock folder
            token = EncryptionUtils.lockFolder(parent, client);

            // get metadata
            Pair<Boolean, DecryptedFolderMetadata> metadataPair = EncryptionUtils.retrieveMetadata(parent,
                                                                                                   client,
                                                                                                   privateKey,
                                                                                                   publicKey);

            metadataExists = metadataPair.first;
            metadata = metadataPair.second;

            // check if filename already exists
            if (isFileExisting(metadata, filename)) {
                return new RemoteOperationResult(RemoteOperationResult.ResultCode.FOLDER_ALREADY_EXISTS);
            }

            // generate new random file name, check if it exists in metadata
            String encryptedFileName = createRandomFileName(metadata);
            encryptedRemotePath = parent.getRemotePath() + encryptedFileName;

            RemoteOperationResult result = new CreateFolderRemoteOperation(encryptedRemotePath,
                                                                           true,
                                                                           token)
                .execute(client);

            if (result.isSuccess()) {
                // update metadata
                metadata.getFiles().put(encryptedFileName, createDecryptedFile(filename));

                EncryptedFolderMetadata encryptedFolderMetadata = EncryptionUtils.encryptFolderMetadata(metadata,
                                                                                                        privateKey);
                String serializedFolderMetadata = EncryptionUtils.serializeJSON(encryptedFolderMetadata);

                // upload metadata
                EncryptionUtils.uploadMetadata(parent,
                                               serializedFolderMetadata,
                                               token,
                                               client,
                                               metadataExists);

                // unlock folder
                if (token != null) {
                    RemoteOperationResult unlockFolderResult = EncryptionUtils.unlockFolder(parent, client, token);

                    if (unlockFolderResult.isSuccess()) {
                        token = null;
                    } else {
                        // TODO do better
                        throw new RuntimeException("Could not unlock folder!");
                    }
                }

                RemoteOperationResult remoteFolderOperationResult = new ReadFolderRemoteOperation(encryptedRemotePath)
                    .execute(client);

                createdRemoteFolder = (RemoteFile) remoteFolderOperationResult.getData().get(0);
                OCFile newDir = createRemoteFolderOcFile(parent, filename, createdRemoteFolder);
                MainApp.storageManager.saveFile(newDir);

                RemoteOperationResult encryptionOperationResult = new ToggleEncryptionRemoteOperation(
                    newDir.getLocalId(),
                    newDir.getRemotePath(),
                    true)
                    .execute(client);

                if (!encryptionOperationResult.isSuccess()) {
                    throw new RuntimeException("Error creating encrypted subfolder!");
                }
            } else {
                // revert to sane state in case of any error
                Log_OC.e(TAG, remotePath + " hasn't been created");
            }

            return result;
        } catch (Exception e) {
            if (!EncryptionUtils.unlockFolder(parent, client, token).isSuccess()) {
                throw new RuntimeException("Could not clean up after failing folder creation!", e);
            }

            // remove folder
            if (encryptedRemotePath != null) {
                RemoteOperationResult removeResult = new RemoveRemoteEncryptedFileOperation(encryptedRemotePath,
                                                                                            parent.getLocalId(),
                                                                                            user,
                                                                                            context,
                                                                                            filename).execute(client);

                if (!removeResult.isSuccess()) {
                    throw new RuntimeException("Could not clean up after failing folder creation!");
                }
            }

            // TODO do better
            return new RemoteOperationResult(e);
        } finally {
            // unlock folder
            if (token != null) {
                RemoteOperationResult unlockFolderResult = EncryptionUtils.unlockFolder(parent, client, token);

                if (!unlockFolderResult.isSuccess()) {
                    // TODO do better
                    throw new RuntimeException("Could not unlock folder!");
                }
            }
        }
    }

    private boolean isFileExisting(DecryptedFolderMetadata metadata, String filename) {
        for (String key : metadata.getFiles().keySet()) {
            DecryptedFolderMetadata.DecryptedFile file = metadata.getFiles().get(key);

            if (file != null && filename.equalsIgnoreCase(file.getEncrypted().getFilename())) {
                return true;
            }
        }
        return false;
    }

    @NonNull
    private OCFile createRemoteFolderOcFile(OCFile parent, String filename, RemoteFile remoteFolder) {
        OCFile newDir = new OCFile(remoteFolder.getRemotePath());

        newDir.setMimeType(MimeType.DIRECTORY);
        newDir.setParentId(parent.getFileId());
        newDir.setRemoteId(remoteFolder.getRemoteId());
        newDir.setModificationTimestamp(System.currentTimeMillis());
        newDir.setEncrypted(true);
        newDir.setPermissions(remoteFolder.getPermissions());
        newDir.setDecryptedRemotePath(parent.getDecryptedRemotePath() + filename + "/");

        return newDir;
    }

    @NonNull
    private DecryptedFolderMetadata.DecryptedFile createDecryptedFile(String filename) {
        // Key, always generate new one
        byte[] key = EncryptionUtils.generateKey();

        // IV, always generate new one
        byte[] iv = EncryptionUtils.randomBytes(EncryptionUtils.ivLength);

        DecryptedFolderMetadata.DecryptedFile decryptedFile = new DecryptedFolderMetadata.DecryptedFile();
        DecryptedFolderMetadata.Data data = new DecryptedFolderMetadata.Data();
        data.setFilename(filename);
        data.setMimetype(MimeType.WEBDAV_FOLDER);
        data.setKey(EncryptionUtils.encodeBytesToBase64String(key));

        decryptedFile.setEncrypted(data);
        decryptedFile.setInitializationVector(EncryptionUtils.encodeBytesToBase64String(iv));

        return decryptedFile;
    }

    @NonNull
    private String createRandomFileName(DecryptedFolderMetadata metadata) {
        String encryptedFileName = UUID.randomUUID().toString().replaceAll("-", "");

        while (metadata.getFiles().get(encryptedFileName) != null) {
            encryptedFileName = UUID.randomUUID().toString().replaceAll("-", "");
        }
        return encryptedFileName;
    }

    private RemoteOperationResult normalCreate(OwnCloudClient client) {
        RemoteOperationResult result = new CreateFolderRemoteOperation(remotePath, true).execute(client);

        if (result.isSuccess()) {
            RemoteOperationResult remoteFolderOperationResult = new ReadFolderRemoteOperation(remotePath)
                .execute(client);

            createdRemoteFolder = (RemoteFile) remoteFolderOperationResult.getData().get(0);
            saveFolderInDB();
        } else {
            Log_OC.e(TAG, remotePath + " hasn't been created");
        }

        return result;
    }

    public void onRemoteOperationFinish(RemoteOperation operation, RemoteOperationResult result) {
        if (operation instanceof CreateFolderRemoteOperation) {
            onCreateRemoteFolderOperationFinish(result);
        }
    }

    private void onCreateRemoteFolderOperationFinish(RemoteOperationResult result) {
        if (result.isSuccess()) {
            saveFolderInDB();
        } else {
            Log_OC.e(TAG, remotePath + " hasn't been created");
        }
    }

    /**
     * Save new directory in local database.
     */
    private void saveFolderInDB() {
        if (MainApp.storageManager.getFileByPath(FileStorageUtils.getParentPath(remotePath)) == null) {
            // When parent of remote path is not created
            String[] subFolders = remotePath.split(PATH_SEPARATOR);
            String composedRemotePath = ROOT_PATH;

            // For each ancestor folders create them recursively
            for (String subFolder : subFolders) {
                if (!subFolder.isEmpty()) {
                    composedRemotePath = composedRemotePath + subFolder + PATH_SEPARATOR;
                    remotePath = composedRemotePath;
                    saveFolderInDB();
                }
            }
        } else { // Create directory on DB
            OCFile newDir = new OCFile(remotePath);
            newDir.setMimeType(MimeType.DIRECTORY);
            long parentId = MainApp.storageManager.getFileByPath(FileStorageUtils.getParentPath(remotePath)).getFileId();
            newDir.setParentId(parentId);
            // newDir.setRemoteId(createdRemoteFolder.getRemoteId());
            newDir.setModificationTimestamp(System.currentTimeMillis());
            newDir.setEncrypted(FileStorageUtils.checkEncryptionStatus(newDir, MainApp.storageManager));
            // newDir.setPermissions(createdRemoteFolder.getPermissions());
            MainApp.storageManager.saveFile(newDir);

            Log_OC.d(TAG, "Create directory " + remotePath + " in Database");
        }
    }

    public String getRemotePath() {
        return remotePath;
    }
}
