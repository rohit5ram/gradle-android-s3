package com.yantranet.gradle

import com.amazonaws.AmazonClientException
import com.amazonaws.AmazonServiceException
import com.amazonaws.auth.AWSCredentials
import com.amazonaws.event.ProgressEvent
import com.amazonaws.services.s3.model.CannedAccessControlList
import com.amazonaws.services.s3.model.PutObjectRequest
import com.amazonaws.services.s3.transfer.PersistableTransfer
import com.amazonaws.services.s3.transfer.TransferManager
import com.amazonaws.services.s3.transfer.Upload
import com.amazonaws.services.s3.transfer.internal.S3ProgressListener
import org.gradle.api.GradleException
import org.gradle.api.tasks.TaskAction

class UploadTask extends S3Task {
    String cannedAccessControlList
    String[] fileLocalPaths

    private void validate() {
        if (bucketName == null || bucketName.isEmpty()) throw new GradleException('bucketName can\'t be empty or NULL')
        if (fileLocalPaths.length <= 0) throw new GradleException('No files to upload')
        for (String filePath : fileLocalPaths) {
            validateFilePath(filePath)
        }
    }

    private static void validateFilePath(String filePath) {
        if (filePath == null || filePath.isEmpty()) throw new GradleException(" filePath can't be NULL or empty ")
        if (!new File(filePath).exists()) throw new GradleException('file doesn\'t exist at location' + filePath)
        if (new File(filePath).isDirectory()) throw new GradleException(filePath + ' file represents a folder')
    }

    private static void upload(AWSCredentials credentials, String bucketName, String key, String uploadPath) {
        upload(credentials, new PutObjectRequest(bucketName, key, new File(uploadPath)))
    }

    private static void upload(AWSCredentials credentials, String bucketName, String s3Object, String uploadPath, CannedAccessControlList cannedAccessControlList) {
        PutObjectRequest objectRequest = new PutObjectRequest(bucketName, s3Object, new File(uploadPath))
        objectRequest.setCannedAcl(cannedAccessControlList)
        upload(credentials, objectRequest)
    }

    private static void upload(AWSCredentials credentials, PutObjectRequest objectRequest) {
        TransferManager manager = new TransferManager(credentials)
        objectRequest.setKey(objectRequest.getKey() + '/' + objectRequest.getFile().name)
        try {
            println("Upload src : " + objectRequest.getFile().absolutePath)
            println(" Uploading to  :: " + 'https://s3.amazonaws.com/' + objectRequest.getBucketName() + '/' + objectRequest.getKey())
            Upload upload = manager.upload(objectRequest)
            upload.addProgressListener(new S3ProgressListener() {
                @Override
                void onPersistableTransfer(PersistableTransfer persistableTransfer) {

                }

                @Override
                void progressChanged(ProgressEvent progressEvent) {
                    print(' .')
                }
            })
            upload.waitForCompletion()
            println "Upload finished .."
        } catch (AmazonServiceException e) {
            throw new GradleException("AMAZON SERVICE ERROR: " + e.getMessage())
        } catch (AmazonClientException e) {
            throw new GradleException("AMAZON CLIENT ERROR: " + e.getMessage())
        } catch (InterruptedException e) {
            throw new GradleException("TRANSFER INTERRUPTED: " + e.getMessage())
        }
        manager.shutdownNow()
    }

    void reset() {
        accessKey = ''
        secretKey = ''
        bucketName = ''
        key = ''
        fileLocalPath = ''
        cannedAccessControlList = ''
        accessControlList = null
    }

    @TaskAction
    void run() {
        validate()
        AWSCredentials credentials = getAWSCredentials()
        if(credentials == null) throw new IllegalArgumentException("Invalid AWS Credentials")
        if (cannedAccessControlList != null && !cannedAccessControlList.isEmpty()) {
            println('cannedAccessControlList' + CannedAccessControlList.valueOf(cannedAccessControlList))
            for (String filePath : fileLocalPaths) {
                upload(credentials, bucketName, key, filePath, CannedAccessControlList.valueOf(cannedAccessControlList))
            }
        } else {
            for (String filePath : fileLocalPaths) {
                upload(credentials, bucketName, key, filePath)
            }
        }
    }
}