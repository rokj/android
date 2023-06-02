package com.s3;

import com.owncloud.android.MainApp;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.lib.resources.files.model.RemoteFile;
import com.owncloud.android.lib.resources.shares.ShareeUser;
import com.owncloud.android.utils.MimeType;

import java.util.ArrayList;
import java.util.List;

import io.minio.ListObjectsArgs;
import io.minio.Result;
import io.minio.messages.Bucket;
import io.minio.messages.Item;

public class s3 {

    public static List<Object> readRemoteFolder(String remotePath) {
        Iterable<Result<Item>> objects;
        List<Object> data = new ArrayList<>();
        String[] path = remotePath.split("/");
        String bucket = "";
        String prefix = "";
        List<Bucket> buckets;

        if (remotePath.equals("/")) {
            try {
                buckets = MainApp.minioClient.listBuckets();
                for (int i = 0; i < buckets.size(); i++) {
                    RemoteFile remoteFile = new RemoteFile();
                    Bucket tmpBucket = buckets.get(i);
                    remoteFile.setMimeType("DIR");
                    remoteFile.setRemotePath("/" + tmpBucket.name());

                    ShareeUser[] sharees = new ShareeUser[0];
                    remoteFile.setSharees(sharees);

                    data.add(remoteFile);
                }
            } catch (Exception e) {
                Log_OC.d("minio", e.toString());
            }
        }

        if (path.length == 0) {
            return data;
        }

        if (path.length >= 1) {
            bucket = path[1];
        }

        if (path.length > 2) {
            for (int i = 0; i < path.length - 1; i++) {
                prefix = remotePath.replaceAll(bucket + "/", "");
            }
        }

        try {
            objects = MainApp.minioClient.listObjects(ListObjectsArgs.builder().bucket(bucket).prefix(prefix).recursive(false).build());

            for (Result<Item> item : objects) {
                RemoteFile remoteFile = new RemoteFile();
                if (item.get().isDir()) {
                    remoteFile.setMimeType(MimeType.DIRECTORY);
                } else {
                    remoteFile.setMimeType(MimeType.FILE);
                }

                String remotePathForSave = remotePath + item.get().objectName();
                remotePathForSave.replaceAll("[/]+", "/");
                remoteFile.setRemotePath(remotePathForSave);
                remoteFile.setEtag(item.get().etag());
                remoteFile.setModifiedTimestamp(item.get().lastModified().toEpochSecond());
                remoteFile.setCreationTimestamp(item.get().lastModified().toEpochSecond());
                remoteFile.setSize(item.get().size());

                ShareeUser[] sharees = new ShareeUser[0];
                remoteFile.setSharees(sharees);

                data.add(remoteFile);
            }
        } catch (Exception e) {
            Log_OC.d("minio", e.toString());
        }

        return data;
    }
}
