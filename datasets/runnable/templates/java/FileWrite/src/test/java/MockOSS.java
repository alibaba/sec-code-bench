// Copyright (c) 2025 Alibaba Group and its affiliates

//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at

//      http://www.apache.org/licenses/LICENSE-2.0

//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.

import com.aliyun.oss.ClientException;
import com.aliyun.oss.HttpMethod;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.common.auth.Credentials;
import com.aliyun.oss.common.comm.ResponseMessage;
import com.aliyun.oss.common.comm.SignVersion;
import com.aliyun.oss.model.*;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

public class MockOSS implements OSS {
    @Override
    public void switchCredentials(Credentials credentials) {
    }

    @Override
    public void switchSignatureVersion(SignVersion signVersion) {

    }

    @Override
    public void shutdown() {

    }

    @Override
    public String getConnectionPoolStats() {
        return "";
    }

    @Override
    public Bucket createBucket(String s) throws OSSException, ClientException {
        return null;
    }

    @Override
    public Bucket createBucket(CreateBucketRequest createBucketRequest) throws OSSException, ClientException {
        return null;
    }

    @Override
    public VoidResult deleteBucket(String s) throws OSSException, ClientException {
        return null;
    }

    @Override
    public VoidResult deleteBucket(GenericRequest genericRequest) throws OSSException, ClientException {
        return null;
    }

    @Override
    public List<Bucket> listBuckets() throws OSSException, ClientException {
        return Collections.emptyList();
    }

    @Override
    public BucketList listBuckets(String s, String s1, Integer integer) throws OSSException, ClientException {
        return null;
    }

    @Override
    public BucketList listBuckets(ListBucketsRequest listBucketsRequest) throws OSSException, ClientException {
        return null;
    }

    @Override
    public VoidResult setBucketAcl(String s, CannedAccessControlList cannedAccessControlList) throws OSSException, ClientException {
        return null;
    }

    @Override
    public VoidResult setBucketAcl(SetBucketAclRequest setBucketAclRequest) throws OSSException, ClientException {
        return null;
    }

    @Override
    public AccessControlList getBucketAcl(String s) throws OSSException, ClientException {
        return null;
    }

    @Override
    public AccessControlList getBucketAcl(GenericRequest genericRequest) throws OSSException, ClientException {
        return null;
    }

    @Override
    public BucketMetadata getBucketMetadata(String s) throws OSSException, ClientException {
        return null;
    }

    @Override
    public BucketMetadata getBucketMetadata(GenericRequest genericRequest) throws OSSException, ClientException {
        return null;
    }

    @Override
    public VoidResult setBucketReferer(String s, BucketReferer bucketReferer) throws OSSException, ClientException {
        return null;
    }

    @Override
    public VoidResult setBucketReferer(SetBucketRefererRequest setBucketRefererRequest) throws OSSException, ClientException {
        return null;
    }

    @Override
    public BucketReferer getBucketReferer(String s) throws OSSException, ClientException {
        return null;
    }

    @Override
    public BucketReferer getBucketReferer(GenericRequest genericRequest) throws OSSException, ClientException {
        return null;
    }

    @Override
    public String getBucketLocation(String s) throws OSSException, ClientException {
        return "";
    }

    @Override
    public String getBucketLocation(GenericRequest genericRequest) throws OSSException, ClientException {
        return "";
    }

    @Override
    public VoidResult setBucketTagging(String s, Map<String, String> map) throws OSSException, ClientException {
        return null;
    }

    @Override
    public VoidResult setBucketTagging(String s, TagSet tagSet) throws OSSException, ClientException {
        return null;
    }

    @Override
    public VoidResult setBucketTagging(SetBucketTaggingRequest setBucketTaggingRequest) throws OSSException, ClientException {
        return null;
    }

    @Override
    public TagSet getBucketTagging(String s) throws OSSException, ClientException {
        return null;
    }

    @Override
    public TagSet getBucketTagging(GenericRequest genericRequest) throws OSSException, ClientException {
        return null;
    }

    @Override
    public VoidResult deleteBucketTagging(String s) throws OSSException, ClientException {
        return null;
    }

    @Override
    public VoidResult deleteBucketTagging(GenericRequest genericRequest) throws OSSException, ClientException {
        return null;
    }

    @Override
    public BucketVersioningConfiguration getBucketVersioning(String s) throws OSSException, ClientException {
        return null;
    }

    @Override
    public BucketVersioningConfiguration getBucketVersioning(GenericRequest genericRequest) throws OSSException, ClientException {
        return null;
    }

    @Override
    public VoidResult setBucketVersioning(SetBucketVersioningRequest setBucketVersioningRequest) throws OSSException, ClientException {
        return null;
    }

    @Override
    public boolean doesBucketExist(String s) throws OSSException, ClientException {
        return false;
    }

    @Override
    public boolean doesBucketExist(GenericRequest genericRequest) throws OSSException, ClientException {
        return false;
    }

    @Override
    public ObjectListing listObjects(String s) throws OSSException, ClientException {
        return null;
    }

    @Override
    public ObjectListing listObjects(String s, String s1) throws OSSException, ClientException {
        return null;
    }

    @Override
    public ObjectListing listObjects(ListObjectsRequest listObjectsRequest) throws OSSException, ClientException {
        return null;
    }

    @Override
    public ListObjectsV2Result listObjectsV2(ListObjectsV2Request listObjectsV2Request) throws OSSException, ClientException {
        return null;
    }

    @Override
    public ListObjectsV2Result listObjectsV2(String s) throws OSSException, ClientException {
        return null;
    }

    @Override
    public ListObjectsV2Result listObjectsV2(String s, String s1) throws OSSException, ClientException {
        return null;
    }

    @Override
    public ListObjectsV2Result listObjectsV2(String s, String s1, String s2, String s3, String s4, Integer integer, String s5, boolean b) throws OSSException, ClientException {
        return null;
    }

    @Override
    public VersionListing listVersions(String s, String s1) throws OSSException, ClientException {
        return null;
    }

    @Override
    public VersionListing listVersions(String s, String s1, String s2, String s3, String s4, Integer integer) throws OSSException, ClientException {
        return null;
    }

    @Override
    public VersionListing listVersions(ListVersionsRequest listVersionsRequest) throws OSSException, ClientException {
        return null;
    }

    @Override
    public PutObjectResult putObject(String s, String s1, InputStream inputStream) throws OSSException, ClientException {
        return null;
    }

    @Override
    public PutObjectResult putObject(String s, String s1, InputStream inputStream, ObjectMetadata objectMetadata) throws OSSException, ClientException {
        return null;
    }

    @Override
    public PutObjectResult putObject(String s, String s1, File file, ObjectMetadata objectMetadata) throws OSSException, ClientException {
        return null;
    }

    @Override
    public PutObjectResult putObject(String s, String s1, File file) throws OSSException, ClientException {
        return null;
    }

    @Override
    public PutObjectResult putObject(PutObjectRequest putObjectRequest) throws OSSException, ClientException {
        return null;
    }

    @Override
    public PutObjectResult putObject(URL url, String s, Map<String, String> map) throws OSSException, ClientException {
        return null;
    }

    @Override
    public PutObjectResult putObject(URL url, String s, Map<String, String> map, boolean b) throws OSSException, ClientException {
        return null;
    }

    @Override
    public PutObjectResult putObject(URL url, InputStream inputStream, long l, Map<String, String> map) throws OSSException, ClientException {
        return null;
    }

    @Override
    public PutObjectResult putObject(URL url, InputStream inputStream, long l, Map<String, String> map, boolean b) throws OSSException, ClientException {
        return null;
    }

    @Override
    public CopyObjectResult copyObject(String s, String s1, String s2, String s3) throws OSSException, ClientException {
        return null;
    }

    @Override
    public CopyObjectResult copyObject(CopyObjectRequest copyObjectRequest) throws OSSException, ClientException {
        return null;
    }

    @Override
    public OSSObject getObject(String s, String s1) throws OSSException, ClientException {
        OSSObject ossObject = new OSSObject();

        InputStream is = new ByteArrayInputStream("hello world".getBytes());
        ossObject.setObjectContent(is);
        return ossObject;
    }

    @Override
    public ObjectMetadata getObject(GetObjectRequest getObjectRequest, File file) throws OSSException, ClientException {
        try {
            Files.write(file.toPath(), "hello world".getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new OSSException("Error writing file", e);
        }
        return null;
    }

    @Override
    public OSSObject getObject(GetObjectRequest getObjectRequest) throws OSSException, ClientException {
        OSSObject ossObject = new OSSObject();

        InputStream is = new ByteArrayInputStream("hello world".getBytes());
        ossObject.setObjectContent(is);
        return ossObject;
    }

    @Override
    public OSSObject selectObject(SelectObjectRequest selectObjectRequest) throws OSSException, ClientException {
        return null;
    }

    @Override
    public OSSObject getObject(URL url, Map<String, String> map) throws OSSException, ClientException {
        OSSObject ossObject = new OSSObject();

        InputStream is = new ByteArrayInputStream("hello world".getBytes());
        ossObject.setObjectContent(is);
        return ossObject;
    }

    @Override
    public SimplifiedObjectMeta getSimplifiedObjectMeta(String s, String s1) throws OSSException, ClientException {
        return null;
    }

    @Override
    public SimplifiedObjectMeta getSimplifiedObjectMeta(GenericRequest genericRequest) throws OSSException, ClientException {
        return null;
    }

    @Override
    public ObjectMetadata getObjectMetadata(String s, String s1) throws OSSException, ClientException {
        return null;
    }

    @Override
    public ObjectMetadata getObjectMetadata(GenericRequest genericRequest) throws OSSException, ClientException {
        return null;
    }

    @Override
    public SelectObjectMetadata createSelectObjectMetadata(CreateSelectObjectMetadataRequest createSelectObjectMetadataRequest) throws OSSException, ClientException {
        return null;
    }

    @Override
    public ObjectMetadata headObject(String s, String s1) throws OSSException, ClientException {
        return null;
    }

    @Override
    public ObjectMetadata headObject(HeadObjectRequest headObjectRequest) throws OSSException, ClientException {
        return null;
    }

    @Override
    public AppendObjectResult appendObject(AppendObjectRequest appendObjectRequest) throws OSSException, ClientException {
        return null;
    }

    @Override
    public VoidResult deleteObject(String s, String s1) throws OSSException, ClientException {
        return null;
    }

    @Override
    public VoidResult deleteObject(GenericRequest genericRequest) throws OSSException, ClientException {
        return null;
    }

    @Override
    public DeleteObjectsResult deleteObjects(DeleteObjectsRequest deleteObjectsRequest) throws OSSException, ClientException {
        return null;
    }

    @Override
    public VoidResult deleteVersion(String s, String s1, String s2) throws OSSException, ClientException {
        return null;
    }

    @Override
    public VoidResult deleteVersion(DeleteVersionRequest deleteVersionRequest) throws OSSException, ClientException {
        return null;
    }

    @Override
    public DeleteVersionsResult deleteVersions(DeleteVersionsRequest deleteVersionsRequest) throws OSSException, ClientException {
        return null;
    }

    @Override
    public boolean doesObjectExist(String s, String s1) throws OSSException, ClientException {
        return false;
    }

    @Override
    public boolean doesObjectExist(GenericRequest genericRequest) throws OSSException, ClientException {
        return false;
    }

    @Override
    public boolean doesObjectExist(String s, String s1, boolean b) {
        return false;
    }

    @Override
    public boolean doesObjectExist(GenericRequest genericRequest, boolean b) throws OSSException, ClientException {
        return false;
    }

    @Override
    public boolean doesObjectExist(HeadObjectRequest headObjectRequest) throws OSSException, ClientException {
        return false;
    }

    @Override
    public VoidResult setObjectAcl(String s, String s1, CannedAccessControlList cannedAccessControlList) throws OSSException, ClientException {
        return null;
    }

    @Override
    public VoidResult setObjectAcl(SetObjectAclRequest setObjectAclRequest) throws OSSException, ClientException {
        return null;
    }

    @Override
    public ObjectAcl getObjectAcl(String s, String s1) throws OSSException, ClientException {
        return null;
    }

    @Override
    public ObjectAcl getObjectAcl(GenericRequest genericRequest) throws OSSException, ClientException {
        return null;
    }

    @Override
    public RestoreObjectResult restoreObject(String s, String s1) throws OSSException, ClientException {
        return null;
    }

    @Override
    public RestoreObjectResult restoreObject(GenericRequest genericRequest) throws OSSException, ClientException {
        return null;
    }

    @Override
    public RestoreObjectResult restoreObject(String s, String s1, RestoreConfiguration restoreConfiguration) throws OSSException, ClientException {
        return null;
    }

    @Override
    public RestoreObjectResult restoreObject(RestoreObjectRequest restoreObjectRequest) throws OSSException, ClientException {
        return null;
    }

    @Override
    public VoidResult setObjectTagging(String s, String s1, Map<String, String> map) throws OSSException, ClientException {
        return null;
    }

    @Override
    public VoidResult setObjectTagging(String s, String s1, TagSet tagSet) throws OSSException, ClientException {
        return null;
    }

    @Override
    public VoidResult setObjectTagging(SetObjectTaggingRequest setObjectTaggingRequest) throws OSSException, ClientException {
        return null;
    }

    @Override
    public TagSet getObjectTagging(String s, String s1) throws OSSException, ClientException {
        return null;
    }

    @Override
    public TagSet getObjectTagging(GenericRequest genericRequest) throws OSSException, ClientException {
        return null;
    }

    @Override
    public VoidResult deleteObjectTagging(String s, String s1) throws OSSException, ClientException {
        return null;
    }

    @Override
    public VoidResult deleteObjectTagging(GenericRequest genericRequest) throws OSSException, ClientException {
        return null;
    }

    @Override
    public URL generatePresignedUrl(String s, String s1, Date date) throws ClientException {
        return null;
    }

    @Override
    public URL generatePresignedUrl(String s, String s1, Date date, HttpMethod httpMethod) throws ClientException {
        return null;
    }

    @Override
    public URL generatePresignedUrl(GeneratePresignedUrlRequest generatePresignedUrlRequest) throws ClientException {
        return null;
    }

    @Override
    public VoidResult putBucketImage(PutBucketImageRequest putBucketImageRequest) throws OSSException, ClientException {
        return null;
    }

    @Override
    public GetBucketImageResult getBucketImage(String s) throws OSSException, ClientException {
        return null;
    }

    @Override
    public GetBucketImageResult getBucketImage(String s, GenericRequest genericRequest) throws OSSException, ClientException {
        return null;
    }

    @Override
    public VoidResult deleteBucketImage(String s) throws OSSException, ClientException {
        return null;
    }

    @Override
    public VoidResult deleteBucketImage(String s, GenericRequest genericRequest) throws OSSException, ClientException {
        return null;
    }

    @Override
    public VoidResult deleteImageStyle(String s, String s1) throws OSSException, ClientException {
        return null;
    }

    @Override
    public VoidResult deleteImageStyle(String s, String s1, GenericRequest genericRequest) throws OSSException, ClientException {
        return null;
    }

    @Override
    public VoidResult putImageStyle(PutImageStyleRequest putImageStyleRequest) throws OSSException, ClientException {
        return null;
    }

    @Override
    public GetImageStyleResult getImageStyle(String s, String s1) throws OSSException, ClientException {
        return null;
    }

    @Override
    public GetImageStyleResult getImageStyle(String s, String s1, GenericRequest genericRequest) throws OSSException, ClientException {
        return null;
    }

    @Override
    public List<Style> listImageStyle(String s) throws OSSException, ClientException {
        return Collections.emptyList();
    }

    @Override
    public List<Style> listImageStyle(String s, GenericRequest genericRequest) throws OSSException, ClientException {
        return Collections.emptyList();
    }

    @Override
    public VoidResult setBucketProcess(SetBucketProcessRequest setBucketProcessRequest) throws OSSException, ClientException {
        return null;
    }

    @Override
    public BucketProcess getBucketProcess(String s) throws OSSException, ClientException {
        return null;
    }

    @Override
    public BucketProcess getBucketProcess(GenericRequest genericRequest) throws OSSException, ClientException {
        return null;
    }

    @Override
    public InitiateMultipartUploadResult initiateMultipartUpload(InitiateMultipartUploadRequest initiateMultipartUploadRequest) throws OSSException, ClientException {
        return null;
    }

    @Override
    public MultipartUploadListing listMultipartUploads(ListMultipartUploadsRequest listMultipartUploadsRequest) throws OSSException, ClientException {
        return null;
    }

    @Override
    public PartListing listParts(ListPartsRequest listPartsRequest) throws OSSException, ClientException {
        return null;
    }

    @Override
    public UploadPartResult uploadPart(UploadPartRequest uploadPartRequest) throws OSSException, ClientException {
        return null;
    }

    @Override
    public UploadPartCopyResult uploadPartCopy(UploadPartCopyRequest uploadPartCopyRequest) throws OSSException, ClientException {
        return null;
    }

    @Override
    public VoidResult abortMultipartUpload(AbortMultipartUploadRequest abortMultipartUploadRequest) throws OSSException, ClientException {
        return null;
    }

    @Override
    public CompleteMultipartUploadResult completeMultipartUpload(CompleteMultipartUploadRequest completeMultipartUploadRequest) throws OSSException, ClientException {
        return null;
    }

    @Override
    public VoidResult setBucketCORS(SetBucketCORSRequest setBucketCORSRequest) throws OSSException, ClientException {
        return null;
    }

    @Override
    public List<SetBucketCORSRequest.CORSRule> getBucketCORSRules(String s) throws OSSException, ClientException {
        return Collections.emptyList();
    }

    @Override
    public List<SetBucketCORSRequest.CORSRule> getBucketCORSRules(GenericRequest genericRequest) throws OSSException, ClientException {
        return Collections.emptyList();
    }

    @Override
    public CORSConfiguration getBucketCORS(GenericRequest genericRequest) throws OSSException, ClientException {
        return null;
    }

    @Override
    public VoidResult deleteBucketCORSRules(String s) throws OSSException, ClientException {
        return null;
    }

    @Override
    public VoidResult deleteBucketCORSRules(GenericRequest genericRequest) throws OSSException, ClientException {
        return null;
    }

    @Override
    public ResponseMessage optionsObject(OptionsRequest optionsRequest) throws OSSException, ClientException {
        return null;
    }

    @Override
    public VoidResult setBucketLogging(SetBucketLoggingRequest setBucketLoggingRequest) throws OSSException, ClientException {
        return null;
    }

    @Override
    public BucketLoggingResult getBucketLogging(String s) throws OSSException, ClientException {
        return null;
    }

    @Override
    public BucketLoggingResult getBucketLogging(GenericRequest genericRequest) throws OSSException, ClientException {
        return null;
    }

    @Override
    public VoidResult deleteBucketLogging(String s) throws OSSException, ClientException {
        return null;
    }

    @Override
    public VoidResult deleteBucketLogging(GenericRequest genericRequest) throws OSSException, ClientException {
        return null;
    }

    @Override
    public VoidResult setBucketWebsite(SetBucketWebsiteRequest setBucketWebsiteRequest) throws OSSException, ClientException {
        return null;
    }

    @Override
    public BucketWebsiteResult getBucketWebsite(String s) throws OSSException, ClientException {
        return null;
    }

    @Override
    public BucketWebsiteResult getBucketWebsite(GenericRequest genericRequest) throws OSSException, ClientException {
        return null;
    }

    @Override
    public VoidResult deleteBucketWebsite(String s) throws OSSException, ClientException {
        return null;
    }

    @Override
    public VoidResult deleteBucketWebsite(GenericRequest genericRequest) throws OSSException, ClientException {
        return null;
    }

    @Override
    public String generatePostPolicy(Date date, PolicyConditions policyConditions) throws ClientException {
        return "";
    }

    @Override
    public String calculatePostSignature(String s) {
        return "";
    }

    @Override
    public VoidResult setBucketLifecycle(SetBucketLifecycleRequest setBucketLifecycleRequest) throws OSSException, ClientException {
        return null;
    }

    @Override
    public List<LifecycleRule> getBucketLifecycle(String s) throws OSSException, ClientException {
        return Collections.emptyList();
    }

    @Override
    public List<LifecycleRule> getBucketLifecycle(GenericRequest genericRequest) throws OSSException, ClientException {
        return Collections.emptyList();
    }

    @Override
    public VoidResult deleteBucketLifecycle(String s) throws OSSException, ClientException {
        return null;
    }

    @Override
    public VoidResult deleteBucketLifecycle(GenericRequest genericRequest) throws OSSException, ClientException {
        return null;
    }

    @Override
    public VoidResult addBucketReplication(AddBucketReplicationRequest addBucketReplicationRequest) throws OSSException, ClientException {
        return null;
    }

    @Override
    public List<ReplicationRule> getBucketReplication(String s) throws OSSException, ClientException {
        return Collections.emptyList();
    }

    @Override
    public List<ReplicationRule> getBucketReplication(GenericRequest genericRequest) throws OSSException, ClientException {
        return Collections.emptyList();
    }

    @Override
    public VoidResult deleteBucketReplication(String s, String s1) throws OSSException, ClientException {
        return null;
    }

    @Override
    public VoidResult deleteBucketReplication(DeleteBucketReplicationRequest deleteBucketReplicationRequest) throws OSSException, ClientException {
        return null;
    }

    @Override
    public BucketReplicationProgress getBucketReplicationProgress(String s, String s1) throws OSSException, ClientException {
        return null;
    }

    @Override
    public BucketReplicationProgress getBucketReplicationProgress(GetBucketReplicationProgressRequest getBucketReplicationProgressRequest) throws OSSException, ClientException {
        return null;
    }

    @Override
    public List<String> getBucketReplicationLocation(String s) throws OSSException, ClientException {
        return Collections.emptyList();
    }

    @Override
    public List<String> getBucketReplicationLocation(GenericRequest genericRequest) throws OSSException, ClientException {
        return Collections.emptyList();
    }

    @Override
    public AddBucketCnameResult addBucketCname(AddBucketCnameRequest addBucketCnameRequest) throws OSSException, ClientException {
        return null;
    }

    @Override
    public List<CnameConfiguration> getBucketCname(String s) throws OSSException, ClientException {
        return Collections.emptyList();
    }

    @Override
    public List<CnameConfiguration> getBucketCname(GenericRequest genericRequest) throws OSSException, ClientException {
        return Collections.emptyList();
    }

    @Override
    public VoidResult deleteBucketCname(String s, String s1) throws OSSException, ClientException {
        return null;
    }

    @Override
    public VoidResult deleteBucketCname(DeleteBucketCnameRequest deleteBucketCnameRequest) throws OSSException, ClientException {
        return null;
    }

    @Override
    public CreateBucketCnameTokenResult createBucketCnameToken(CreateBucketCnameTokenRequest createBucketCnameTokenRequest) throws OSSException, ClientException {
        return null;
    }

    @Override
    public GetBucketCnameTokenResult getBucketCnameToken(GetBucketCnameTokenRequest getBucketCnameTokenRequest) throws OSSException, ClientException {
        return null;
    }

    @Override
    public BucketInfo getBucketInfo(String s) throws OSSException, ClientException {
        return null;
    }

    @Override
    public BucketInfo getBucketInfo(GenericRequest genericRequest) throws OSSException, ClientException {
        return null;
    }

    @Override
    public BucketStat getBucketStat(String s) throws OSSException, ClientException {
        return null;
    }

    @Override
    public BucketStat getBucketStat(GenericRequest genericRequest) throws OSSException, ClientException {
        return null;
    }

    @Override
    public VoidResult setBucketStorageCapacity(String s, UserQos userQos) throws OSSException, ClientException {
        return null;
    }

    @Override
    public VoidResult setBucketStorageCapacity(SetBucketStorageCapacityRequest setBucketStorageCapacityRequest) throws OSSException, ClientException {
        return null;
    }

    @Override
    public UserQos getBucketStorageCapacity(String s) throws OSSException, ClientException {
        return null;
    }

    @Override
    public UserQos getBucketStorageCapacity(GenericRequest genericRequest) throws OSSException, ClientException {
        return null;
    }

    @Override
    public VoidResult setBucketEncryption(SetBucketEncryptionRequest setBucketEncryptionRequest) throws OSSException, ClientException {
        return null;
    }

    @Override
    public ServerSideEncryptionConfiguration getBucketEncryption(String s) throws OSSException, ClientException {
        return null;
    }

    @Override
    public ServerSideEncryptionConfiguration getBucketEncryption(GenericRequest genericRequest) throws OSSException, ClientException {
        return null;
    }

    @Override
    public VoidResult deleteBucketEncryption(String s) throws OSSException, ClientException {
        return null;
    }

    @Override
    public VoidResult deleteBucketEncryption(GenericRequest genericRequest) throws OSSException, ClientException {
        return null;
    }

    @Override
    public VoidResult setBucketPolicy(String s, String s1) throws OSSException, ClientException {
        return null;
    }

    @Override
    public VoidResult setBucketPolicy(SetBucketPolicyRequest setBucketPolicyRequest) throws OSSException, ClientException {
        return null;
    }

    @Override
    public GetBucketPolicyResult getBucketPolicy(GenericRequest genericRequest) throws OSSException, ClientException {
        return null;
    }

    @Override
    public GetBucketPolicyResult getBucketPolicy(String s) throws OSSException, ClientException {
        return null;
    }

    @Override
    public VoidResult deleteBucketPolicy(GenericRequest genericRequest) throws OSSException, ClientException {
        return null;
    }

    @Override
    public VoidResult deleteBucketPolicy(String s) throws OSSException, ClientException {
        return null;
    }

    @Override
    public UploadFileResult uploadFile(UploadFileRequest uploadFileRequest) throws Throwable {
        return null;
    }

    @Override
    public DownloadFileResult downloadFile(DownloadFileRequest downloadFileRequest) throws Throwable {
        return null;
    }

    @Override
    public CreateLiveChannelResult createLiveChannel(CreateLiveChannelRequest createLiveChannelRequest) throws OSSException, ClientException {
        return null;
    }

    @Override
    public VoidResult setLiveChannelStatus(String s, String s1, LiveChannelStatus liveChannelStatus) throws OSSException, ClientException {
        return null;
    }

    @Override
    public VoidResult setLiveChannelStatus(SetLiveChannelRequest setLiveChannelRequest) throws OSSException, ClientException {
        return null;
    }

    @Override
    public LiveChannelInfo getLiveChannelInfo(String s, String s1) throws OSSException, ClientException {
        return null;
    }

    @Override
    public LiveChannelInfo getLiveChannelInfo(LiveChannelGenericRequest liveChannelGenericRequest) throws OSSException, ClientException {
        return null;
    }

    @Override
    public LiveChannelStat getLiveChannelStat(String s, String s1) throws OSSException, ClientException {
        return null;
    }

    @Override
    public LiveChannelStat getLiveChannelStat(LiveChannelGenericRequest liveChannelGenericRequest) throws OSSException, ClientException {
        return null;
    }

    @Override
    public VoidResult deleteLiveChannel(String s, String s1) throws OSSException, ClientException {
        return null;
    }

    @Override
    public VoidResult deleteLiveChannel(LiveChannelGenericRequest liveChannelGenericRequest) throws OSSException, ClientException {
        return null;
    }

    @Override
    public List<LiveChannel> listLiveChannels(String s) throws OSSException, ClientException {
        return Collections.emptyList();
    }

    @Override
    public LiveChannelListing listLiveChannels(ListLiveChannelsRequest listLiveChannelsRequest) throws OSSException, ClientException {
        return null;
    }

    @Override
    public List<LiveRecord> getLiveChannelHistory(String s, String s1) throws OSSException, ClientException {
        return Collections.emptyList();
    }

    @Override
    public List<LiveRecord> getLiveChannelHistory(LiveChannelGenericRequest liveChannelGenericRequest) throws OSSException, ClientException {
        return Collections.emptyList();
    }

    @Override
    public VoidResult generateVodPlaylist(String s, String s1, String s2, long l, long l1) throws OSSException, ClientException {
        return null;
    }

    @Override
    public VoidResult generateVodPlaylist(GenerateVodPlaylistRequest generateVodPlaylistRequest) throws OSSException, ClientException {
        return null;
    }

    @Override
    public OSSObject getVodPlaylist(String s, String s1, long l, long l1) throws OSSException, ClientException {
        return null;
    }

    @Override
    public OSSObject getVodPlaylist(GetVodPlaylistRequest getVodPlaylistRequest) throws OSSException, ClientException {
        return null;
    }

    @Override
    public String generateRtmpUri(String s, String s1, String s2, long l) throws OSSException, ClientException {
        return "";
    }

    @Override
    public String generateRtmpUri(GenerateRtmpUriRequest generateRtmpUriRequest) throws OSSException, ClientException {
        return "";
    }

    @Override
    public VoidResult createSymlink(String s, String s1, String s2) throws OSSException, ClientException {
        return null;
    }

    @Override
    public VoidResult createSymlink(CreateSymlinkRequest createSymlinkRequest) throws OSSException, ClientException {
        return null;
    }

    @Override
    public OSSSymlink getSymlink(String s, String s1) throws OSSException, ClientException {
        return null;
    }

    @Override
    public OSSSymlink getSymlink(GenericRequest genericRequest) throws OSSException, ClientException {
        return null;
    }

    @Override
    public GenericResult processObject(ProcessObjectRequest processObjectRequest) throws OSSException, ClientException {
        return null;
    }

    @Override
    public VoidResult setBucketRequestPayment(String s, Payer payer) throws OSSException, ClientException {
        return null;
    }

    @Override
    public VoidResult setBucketRequestPayment(SetBucketRequestPaymentRequest setBucketRequestPaymentRequest) throws OSSException, ClientException {
        return null;
    }

    @Override
    public GetBucketRequestPaymentResult getBucketRequestPayment(String s) throws OSSException, ClientException {
        return null;
    }

    @Override
    public GetBucketRequestPaymentResult getBucketRequestPayment(GenericRequest genericRequest) throws OSSException, ClientException {
        return null;
    }

    @Override
    public VoidResult setBucketQosInfo(String s, BucketQosInfo bucketQosInfo) throws OSSException, ClientException {
        return null;
    }

    @Override
    public VoidResult setBucketQosInfo(SetBucketQosInfoRequest setBucketQosInfoRequest) throws OSSException, ClientException {
        return null;
    }

    @Override
    public BucketQosInfo getBucketQosInfo(String s) throws OSSException, ClientException {
        return null;
    }

    @Override
    public BucketQosInfo getBucketQosInfo(GenericRequest genericRequest) throws OSSException, ClientException {
        return null;
    }

    @Override
    public VoidResult deleteBucketQosInfo(String s) throws OSSException, ClientException {
        return null;
    }

    @Override
    public VoidResult deleteBucketQosInfo(GenericRequest genericRequest) throws OSSException, ClientException {
        return null;
    }

    @Override
    public UserQosInfo getUserQosInfo() throws OSSException, ClientException {
        return null;
    }

    @Override
    public SetAsyncFetchTaskResult setAsyncFetchTask(String s, AsyncFetchTaskConfiguration asyncFetchTaskConfiguration) throws OSSException, ClientException {
        return null;
    }

    @Override
    public SetAsyncFetchTaskResult setAsyncFetchTask(SetAsyncFetchTaskRequest setAsyncFetchTaskRequest) throws OSSException, ClientException {
        return null;
    }

    @Override
    public GetAsyncFetchTaskResult getAsyncFetchTask(String s, String s1) throws OSSException, ClientException {
        return null;
    }

    @Override
    public GetAsyncFetchTaskResult getAsyncFetchTask(GetAsyncFetchTaskRequest getAsyncFetchTaskRequest) throws OSSException, ClientException {
        return null;
    }

    @Override
    public CreateVpcipResult createVpcip(CreateVpcipRequest createVpcipRequest) throws OSSException, ClientException {
        return null;
    }

    @Override
    public List<Vpcip> listVpcip() throws OSSException, ClientException {
        return Collections.emptyList();
    }

    @Override
    public VoidResult deleteVpcip(DeleteVpcipRequest deleteVpcipRequest) throws OSSException, ClientException {
        return null;
    }

    @Override
    public VoidResult createBucketVpcip(CreateBucketVpcipRequest createBucketVpcipRequest) throws OSSException, ClientException {
        return null;
    }

    @Override
    public List<VpcPolicy> getBucketVpcip(GenericRequest genericRequest) throws OSSException, ClientException {
        return Collections.emptyList();
    }

    @Override
    public VoidResult deleteBucketVpcip(DeleteBucketVpcipRequest deleteBucketVpcipRequest) throws OSSException, ClientException {
        return null;
    }

    @Override
    public VoidResult setBucketInventoryConfiguration(String s, InventoryConfiguration inventoryConfiguration) throws OSSException, ClientException {
        return null;
    }

    @Override
    public VoidResult setBucketInventoryConfiguration(SetBucketInventoryConfigurationRequest setBucketInventoryConfigurationRequest) throws OSSException, ClientException {
        return null;
    }

    @Override
    public GetBucketInventoryConfigurationResult getBucketInventoryConfiguration(String s, String s1) throws OSSException, ClientException {
        return null;
    }

    @Override
    public GetBucketInventoryConfigurationResult getBucketInventoryConfiguration(GetBucketInventoryConfigurationRequest getBucketInventoryConfigurationRequest) throws OSSException, ClientException {
        return null;
    }

    @Override
    public ListBucketInventoryConfigurationsResult listBucketInventoryConfigurations(String s) throws OSSException, ClientException {
        return null;
    }

    @Override
    public ListBucketInventoryConfigurationsResult listBucketInventoryConfigurations(String s, String s1) throws OSSException, ClientException {
        return null;
    }

    @Override
    public ListBucketInventoryConfigurationsResult listBucketInventoryConfigurations(ListBucketInventoryConfigurationsRequest listBucketInventoryConfigurationsRequest) throws OSSException, ClientException {
        return null;
    }

    @Override
    public VoidResult deleteBucketInventoryConfiguration(String s, String s1) throws OSSException, ClientException {
        return null;
    }

    @Override
    public VoidResult deleteBucketInventoryConfiguration(DeleteBucketInventoryConfigurationRequest deleteBucketInventoryConfigurationRequest) throws OSSException, ClientException {
        return null;
    }

    @Override
    public InitiateBucketWormResult initiateBucketWorm(InitiateBucketWormRequest initiateBucketWormRequest) throws OSSException, ClientException {
        return null;
    }

    @Override
    public InitiateBucketWormResult initiateBucketWorm(String s, int i) throws OSSException, ClientException {
        return null;
    }

    @Override
    public VoidResult abortBucketWorm(String s) throws OSSException, ClientException {
        return null;
    }

    @Override
    public VoidResult abortBucketWorm(GenericRequest genericRequest) throws OSSException, ClientException {
        return null;
    }

    @Override
    public VoidResult completeBucketWorm(String s, String s1) throws OSSException, ClientException {
        return null;
    }

    @Override
    public VoidResult completeBucketWorm(CompleteBucketWormRequest completeBucketWormRequest) throws OSSException, ClientException {
        return null;
    }

    @Override
    public VoidResult extendBucketWorm(String s, String s1, int i) throws OSSException, ClientException {
        return null;
    }

    @Override
    public VoidResult extendBucketWorm(ExtendBucketWormRequest extendBucketWormRequest) throws OSSException, ClientException {
        return null;
    }

    @Override
    public GetBucketWormResult getBucketWorm(String s) throws OSSException, ClientException {
        return null;
    }

    @Override
    public GetBucketWormResult getBucketWorm(GenericRequest genericRequest) throws OSSException, ClientException {
        return null;
    }

    @Override
    public VoidResult createDirectory(String s, String s1) throws OSSException, ClientException {
        return null;
    }

    @Override
    public VoidResult createDirectory(CreateDirectoryRequest createDirectoryRequest) throws OSSException, ClientException {
        return null;
    }

    @Override
    public DeleteDirectoryResult deleteDirectory(String s, String s1) throws OSSException, ClientException {
        return null;
    }

    @Override
    public DeleteDirectoryResult deleteDirectory(String s, String s1, boolean b, String s2) throws OSSException, ClientException {
        return null;
    }

    @Override
    public DeleteDirectoryResult deleteDirectory(DeleteDirectoryRequest deleteDirectoryRequest) throws OSSException, ClientException {
        return null;
    }

    @Override
    public VoidResult renameObject(String s, String s1, String s2) throws OSSException, ClientException {
        return null;
    }

    @Override
    public VoidResult renameObject(RenameObjectRequest renameObjectRequest) throws OSSException, ClientException {
        return null;
    }

    @Override
    public VoidResult setBucketResourceGroup(SetBucketResourceGroupRequest setBucketResourceGroupRequest) throws OSSException, ClientException {
        return null;
    }

    @Override
    public GetBucketResourceGroupResult getBucketResourceGroup(String s) throws OSSException, ClientException {
        return null;
    }

    @Override
    public VoidResult createUdf(CreateUdfRequest createUdfRequest) throws OSSException, ClientException {
        return null;
    }

    @Override
    public UdfInfo getUdfInfo(UdfGenericRequest udfGenericRequest) throws OSSException, ClientException {
        return null;
    }

    @Override
    public List<UdfInfo> listUdfs() throws OSSException, ClientException {
        return Collections.emptyList();
    }

    @Override
    public VoidResult deleteUdf(UdfGenericRequest udfGenericRequest) throws OSSException, ClientException {
        return null;
    }

    @Override
    public VoidResult uploadUdfImage(UploadUdfImageRequest uploadUdfImageRequest) throws OSSException, ClientException {
        return null;
    }

    @Override
    public List<UdfImageInfo> getUdfImageInfo(UdfGenericRequest udfGenericRequest) throws OSSException, ClientException {
        return Collections.emptyList();
    }

    @Override
    public VoidResult deleteUdfImage(UdfGenericRequest udfGenericRequest) throws OSSException, ClientException {
        return null;
    }

    @Override
    public VoidResult createUdfApplication(CreateUdfApplicationRequest createUdfApplicationRequest) throws OSSException, ClientException {
        return null;
    }

    @Override
    public UdfApplicationInfo getUdfApplicationInfo(UdfGenericRequest udfGenericRequest) throws OSSException, ClientException {
        return null;
    }

    @Override
    public List<UdfApplicationInfo> listUdfApplications() throws OSSException, ClientException {
        return Collections.emptyList();
    }

    @Override
    public VoidResult deleteUdfApplication(UdfGenericRequest udfGenericRequest) throws OSSException, ClientException {
        return null;
    }

    @Override
    public VoidResult upgradeUdfApplication(UpgradeUdfApplicationRequest upgradeUdfApplicationRequest) throws OSSException, ClientException {
        return null;
    }

    @Override
    public VoidResult resizeUdfApplication(ResizeUdfApplicationRequest resizeUdfApplicationRequest) throws OSSException, ClientException {
        return null;
    }

    @Override
    public UdfApplicationLog getUdfApplicationLog(GetUdfApplicationLogRequest getUdfApplicationLogRequest) throws OSSException, ClientException {
        return null;
    }

    @Override
    public VoidResult setBucketTransferAcceleration(String s, boolean b) throws OSSException, ClientException {
        return null;
    }

    @Override
    public TransferAcceleration getBucketTransferAcceleration(String s) throws OSSException, ClientException {
        return null;
    }

    @Override
    public VoidResult deleteBucketTransferAcceleration(String s) throws OSSException, ClientException {
        return null;
    }

    @Override
    public VoidResult putBucketAccessMonitor(String s, String s1) throws OSSException, ClientException {
        return null;
    }

    @Override
    public AccessMonitor getBucketAccessMonitor(String s) throws OSSException, ClientException {
        return null;
    }

    @Override
    public VoidResult openMetaQuery(String s) throws OSSException, ClientException {
        return null;
    }

    @Override
    public GetMetaQueryStatusResult getMetaQueryStatus(String s) throws OSSException, ClientException {
        return null;
    }

    @Override
    public DoMetaQueryResult doMetaQuery(DoMetaQueryRequest doMetaQueryRequest) throws OSSException, ClientException {
        return null;
    }

    @Override
    public VoidResult closeMetaQuery(String s) throws OSSException, ClientException {
        return null;
    }

    @Override
    public DescribeRegionsResult describeRegions(DescribeRegionsRequest describeRegionsRequest) throws OSSException, ClientException {
        return null;
    }

    @Override
    public VoidResult setBucketCallbackPolicy(SetBucketCallbackPolicyRequest setBucketCallbackPolicyRequest) throws OSSException, ClientException {
        return null;
    }

    @Override
    public GetBucketCallbackPolicyResult getBucketCallbackPolicy(GenericRequest genericRequest) throws OSSException, ClientException {
        return null;
    }

    @Override
    public VoidResult deleteBucketCallbackPolicy(GenericRequest genericRequest) throws OSSException, ClientException {
        return null;
    }

    @Override
    public AsyncProcessObjectResult asyncProcessObject(AsyncProcessObjectRequest asyncProcessObjectRequest) throws OSSException, ClientException {
        return null;
    }

    @Override
    public VoidResult writeGetObjectResponse(WriteGetObjectResponseRequest writeGetObjectResponseRequest) throws OSSException, ClientException {
        return null;
    }
}
