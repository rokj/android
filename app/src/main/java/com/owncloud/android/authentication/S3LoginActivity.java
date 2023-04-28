package com.owncloud.android.authentication;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.UploadObjectArgs;
import io.minio.errors.MinioException;
import com.owncloud.android.R;

import com.owncloud.android.databinding.S3LoginBinding;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import androidx.appcompat.app.AppCompatActivity;
import io.minio.messages.Bucket;

public class S3LoginActivity extends AppCompatActivity {
    private S3LoginBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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
            }
        });
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
