<filename>BootstrapTranslationServiceImpl.java<fim_prefix>

/*
*************************************************************************
ADOBE SYSTEMS INCORPORATED
Copyright [first year code created] Adobe Systems Incorporated
All Rights Reserved.
 
NOTICE:  Adobe permits you to use, modify, and distribute this file in accordance with the
terms of the Adobe license agreement accompanying it.  If you have received this file from a
source other than Adobe, then your use, modification, or distribution of it requires the prior
written permission of Adobe.
*************************************************************************
 */

 package com.adobe.granite.translation.connector.bootstrap.core.impl;

 import java.io.File;
 import java.io.FileNotFoundException;
 import java.io.FileOutputStream;
 import java.io.IOException;
 import java.io.InputStream;
 import java.util.Arrays;
 import java.util.Collections;
 import java.util.Date;
 import java.util.LinkedHashMap;
 import java.util.Map;
 import java.util.Random;
 import java.util.zip.ZipEntry;
 import java.util.zip.ZipInputStream;
 
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 
 import com.adobe.granite.comments.Comment;
 import com.adobe.granite.comments.CommentCollection;
 import com.adobe.granite.translation.api.TranslationConfig;
 import com.adobe.granite.translation.api.TranslationConstants;
 import com.adobe.granite.translation.api.TranslationConstants.TranslationMethod;
 import com.adobe.granite.translation.api.TranslationConstants.TranslationStatus;
 import com.adobe.granite.translation.api.TranslationException;
 import com.adobe.granite.translation.api.TranslationMetadata;
 import com.adobe.granite.translation.api.TranslationObject;
 import com.adobe.granite.translation.api.TranslationResult;
 import com.adobe.granite.translation.api.TranslationScope;
 import com.adobe.granite.translation.api.TranslationService;
 import com.adobe.granite.translation.api.TranslationState;
 import com.adobe.granite.translation.bootstrap.tms.core.BootstrapTmsConstants;
 import com.adobe.granite.translation.bootstrap.tms.core.BootstrapTmsService;
 import com.adobe.granite.translation.connector.bootstrap.core.impl.config.BootstrapTranslationCloudConfigImpl;
 import com.adobe.granite.translation.core.common.AbstractTranslationService;
 import com.adobe.granite.translation.core.common.TranslationResultImpl;
 
 
 public class BootstrapTranslationServiceImpl extends AbstractTranslationService implements TranslationService {
     private static final Logger log = LoggerFactory.getLogger(BootstrapTranslationServiceImpl.class);
     
     private static final String TAG_METADATA = "/tag-metadata";
     
     private static final String ASSET_METADATA = "/asset-metadata";
 
     private static final String I18NCOMPONENTSTRINGDICT = "/i18n-dictionary";
     private static final String SERVICE_LABEL = "bootstrap";
     private static final String SERVICE_ATTRIBUTION = "Translation By Bootstrap";
     private String dummyConfigId = "";
     private String dummyServerUrl = "";
     private String previewPath = "";
     private Boolean isPreviewEnabled = false;
     private Boolean isPseudoLocalizationDisabled = false;
     private String exportFormat = BootstrapConstants.EXPORT_FORMAT_XML;
     private BootstrapTmsService bootstrapTmsService;
     private final static String BOOTSTRAP_SERVICE = "bootstrap-service";
 
 
     class TranslationJobDetails {
         String strName;
         String strDescprition;
         String strSourceLang;
         String strDestinationLang;
     };
 
     class TranslationScopeImpl implements TranslationScope {
         @Override
         public int getWordCount() {
             log.trace("TranslationScopeImpl.getWordCount");
 
             Random rand = new Random();
             return rand.nextInt(100);
         }
 
         @Override
         public int getImageCount() {
             log.trace("TranslationScopeImpl.getImageCount");
 
             Random rand = new Random();
             return rand.nextInt(100);
         }
 
         @Override
         public int getVideoCount() {
             log.trace("TranslationScopeImpl.getVideoCount");
 
             Random rand = new Random();
             return rand.nextInt(100);
         }
 
         @Override
         public Map<String, String> getFinalScope() {
             log.trace("TranslationScopeImpl.getFinalScope");
 
             Map<String, String> newScope = new LinkedHashMap<String, String>();
             newScope.put("ICE Words", "0");
             newScope.put("100% Words", "0");
             newScope.put("New Words", Integer.toString(getWordCount()));
             newScope.put("Repeated Words", "0");
             newScope.put("TranslationScope:CostEstimate", "USD 73.00");
             newScope.put("TranslationScope:DetailsLink","https://github.com/Adobe-Marketing-Cloud/aem-translation-framework-bootstrap-connector");
 
             return newScope;
         }
     }
 
     // Constructor
     public BootstrapTranslationServiceImpl(Map<String, String> availableLanguageMap,
         Map<String, String> availableCategoryMap, String name, Boolean isPreviewEnabled, Boolean isPseudoLocalizationDisabled, String exportFormat, String dummyConfigId, String dummyServerUrl, String previewPath,
         TranslationConfig translationConfig, BootstrapTmsService bootstrapTmsService) {
         super(availableLanguageMap, availableCategoryMap, name, SERVICE_LABEL, SERVICE_ATTRIBUTION,
             BootstrapTranslationCloudConfigImpl.ROOT_PATH, TranslationMethod.MACHINE_TRANSLATION, translationConfig);
 
         log.trace("BootstrapTranslationServiceImpl.");
         log.trace("dummyConfigId: {}",dummyConfigId);
         log.trace("dummyServerUrl: {}",dummyServerUrl);
         log.trace("previewPath: {}",previewPath);
         log.trace("isPreviewEnabled: {}",isPreviewEnabled);
         log.trace("isPseudoLocalizationDisabled: {}", isPseudoLocalizationDisabled);
         log.trace("exportFormat: {}",exportFormat);
         this.dummyConfigId = dummyConfigId;
         this.dummyServerUrl = dummyServerUrl;
         this.previewPath = previewPath;
         this.bootstrapTmsService = bootstrapTmsService;
         this.isPseudoLocalizationDisabled = isPseudoLocalizationDisabled;
         this.isPreviewEnabled = isPreviewEnabled;
         this.exportFormat=exportFormat;
     }
 
     @Override
     public Map<String, String> supportedLanguages() {
         log.trace("BootstrapTranslationServiceImpl.supportedLanguages");
 
         if (availableLanguageMap.size() <= 0) {
             availableLanguageMap.put("en", "en");
             availableLanguageMap.put("de", "de");
             availableLanguageMap.put("fr", "fr");
             availableLanguageMap.put("ja", "ja");
             availableLanguageMap.put("ko", "ko");
         }
 
         return Collections.unmodifiableMap(availableLanguageMap);
     }
 
     @Override
     public boolean isDirectionSupported(String sourceLanguage, String targetLanguage) throws TranslationException {
         log.trace("BootstrapTranslationServiceImpl.isDirectionSupported");
         // It should return true, if translation provider provides translation from sourceLanguage to targetLanguage
         // otherwise false
         return true;
     }
 
     @Override
     public String detectLanguage(String toDetectSource, TranslationConstants.ContentType contentType)
         throws TranslationException {
         log.trace("BootstrapTranslationServiceImpl.detectLanguage");
 
         // French language code
         return "fr";
     }
 
     @Override
     public TranslationResult translateString(String sourceString, String sourceLanguage, String targetLanguage,
         TranslationConstants.ContentType contentType, String contentCategory) throws TranslationException {
         log.trace("BootstrapTranslationServiceImpl.translateString");
         String translatedString = "";
         if(isPseudoLocalizationDisabled == true){
             translatedString = String.format("%s_%s_%s", sourceLanguage, sourceString, targetLanguage);
         }else {
             // Using Pseudo translation here using accented characters
             translatedString = bootstrapTmsService.getAccentedText(sourceString);	
         }
         return new TranslationResultImpl(translatedString, sourceLanguage, targetLanguage, contentType,
             contentCategory, sourceString, TranslationResultImpl.UNKNOWN_RATING, null);
     }
 
     @Override
     public TranslationResult[] translateArray(String[] sourceStringArr, String sourceLanguage, String targetLanguage,
         TranslationConstants.ContentType contentType, String contentCategory) throws TranslationException {
         log.trace("BootstrapTranslationServiceImpl.translateArray");
 
         TranslationResult arrResults[] = new TranslationResultImpl[sourceStringArr.length];
         for (int i = 0; i < sourceStringArr.length; i++) {
             arrResults[i] =
                 translateString(sourceStringArr[i], sourceLanguage, targetLanguage, contentType, contentCategory);
         }
         return arrResults;
     }
 
     @Override
     public TranslationResult[] getAllStoredTranslations(String sourceString, String sourceLanguage,
         String targetLanguage, TranslationConstants.ContentType contentType, String contentCategory, String userId,
         int maxTranslations) throws TranslationException {
         log.trace("BootstrapTranslationServiceImpl.getAllStoredTranslations");
 
         throw new TranslationException("This function is not implemented",
             TranslationException.ErrorCode.SERVICE_NOT_IMPLEMENTED);
     }
 
     @Override
     public void storeTranslation(String[] originalText, String sourceLanguage, String targetLanguage,
         String[] updatedTranslation, TranslationConstants.ContentType contentType, String contentCategory,
         String userId, int rating, String path) throws TranslationException {
         log.trace("BootstrapTranslationServiceImpl.storeTranslation[]");
         log.debug("OriginalText is {}. Updated Transation is {}",Arrays.toString(originalText),updatedTranslation);
     }
 
     @Override
     public void storeTranslation(String originalText, String sourceLanguage, String targetLanguage,
         String updatedTranslation, TranslationConstants.ContentType contentType, String contentCategory,
         String userId, int rating, String path) throws TranslationException {
         log.trace("BootstrapTranslationServiceImpl.storeTranslation");
     }
 
     @Override
     public String createTranslationJob(String name, String description, String strSourceLanguage,
         String strTargetLanguage, Date dueDate, TranslationState state, TranslationMetadata jobMetadata)
         throws TranslationException {
         log.trace("BootstrapTranslationServiceImpl.createTranslationJob");
         return bootstrapTmsService.createBootstrapTmsJob(name, strSourceLanguage, strTargetLanguage, dueDate);
     }
 
     @Override
     public TranslationScope getFinalScope(String strTranslationJobID) throws TranslationException {
         log.trace("BootstrapTranslationServiceImpl.getFinalScope");
 
         return new TranslationScopeImpl();
     }
 
     @Override
     public TranslationStatus updateTranslationJobState(String strTranslationJobID, TranslationState state)
         throws TranslationException {
         if(strTranslationJobID=="dummy"){
             log.debug("Dummy Translation job detected");
         } else if(strTranslationJobID == null) {
             log.debug("Job was never sent to TMS. Updated using Export/Import");
         } else {
             bootstrapTmsService.setTmsProperty(strTranslationJobID, BootstrapTmsConstants.BOOTSTRAP_TMS_STATUS, state.getStatus().toString());
             log.warn("JOB ID is {}",strTranslationJobID);
         }
         if(state.getStatus() == TranslationStatus.COMMITTED_FOR_TRANSLATION) {
             log.trace("Uploaded all Translation Objects in job {}",strTranslationJobID);
         }
         return state.getStatus();
     }
 
     @Override
     public TranslationStatus getTranslationJobStatus(String strTranslationJobID) throws TranslationException {
         log.trace("BootstrapTranslationServiceImpl.getTranslationJobStatus");
         String status = bootstrapTmsService.getTmsJobStatus(strTranslationJobID);
         log.debug("Status for Job {} is {}", strTranslationJobID, status);
         return TranslationStatus.fromString(status);
     }
 
     @Override
     public CommentCollection<Comment> getTranslationJobCommentCollection(String strTranslationJobID) {
         log.trace("BootstrapTranslationServiceImpl.getTranslationJobCommentCollection");
         return null;
     }
 
     @Override
     public void addTranslationJobComment(String strTranslationJobID, Comment comment) throws TranslationException {
         log.trace("BootstrapTranslationServiceImpl.addTranslationJobComment");
 
         throw new TranslationException("This function is not implemented",
             TranslationException.ErrorCode.SERVICE_NOT_IMPLEMENTED);
     }
 
     @Override
     public InputStream getTranslatedObject(String strTranslationJobID, TranslationObject translationObj)
         throws TranslationException {
         log.trace("BootstrapTranslationServiceImpl.getTranslatedObject");
         return bootstrapTmsService.getTmsObjectTranslatedInputStream(strTranslationJobID, getObjectPath(translationObj));
     }
 
     @Override
     public String uploadTranslationObject(String strTranslationJobID, TranslationObject translationObject)
         throws TranslationException {
 
         InputStream inputStream;
         log.trace("Using {} InputStream", exportFormat);
         if (exportFormat.equalsIgnoreCase(BootstrapConstants.EXPORT_FORMAT_XLIFF_1_2)) {
             inputStream = translationObject.getTranslationObjectXLIFFInputStream("1.2");
         } else if (exportFormat.equalsIgnoreCase(BootstrapConstants.EXPORT_FORMAT_XLIFF_2_0)) {
             inputStream = translationObject.getTranslationObjectXLIFFInputStream("2.0");
         }  else if (exportFormat.equalsIgnoreCase(BootstrapConstants.EXPORT_FORMAT_JSON)) {
                 inputStream = translationObject.getTranslationObjectJSONInputStream();
         } else {
             inputStream = translationObject.getTranslationObjectXMLInputStream();
         }
 
     String objectPath = bootstrapTmsService.uploadBootstrapTmsObject(strTranslationJobID, getObjectPath(translationObject), inputStream, translationObject.getMimeType(), exportFormat);
 
         // Generate Preview
         if(isPreviewEnabled) {
             try {
                 ZipInputStream zipInputStream = translationObject.getTranslationObjectPreview();
                 if (zipInputStream != null) {
                     unzipFileFromStream(zipInputStream, previewPath);
                 } else {
                     log.error("Got null for zipInputStream for " + getObjectPath(translationObject));
                 }
             } catch (FileNotFoundException e) {
                 log.error(e.getLocalizedMessage(), e);
             } catch (IOException e) {
                 log.error(e.getLocalizedMessage(), e);
             }			
         }
         log.trace("Preview Directory is: {}", previewPath);
 
         return objectPath;
     }
 
     @Override
     public TranslationStatus updateTranslationObjectState(String strTranslationJobID,
         TranslationObject translationObject, TranslationState state) throws TranslationException {
         log.trace("BootstrapTranslationServiceImpl.updateTranslationObjectState");
         bootstrapTmsService.setTmsProperty(strTranslationJobID+getObjectPath(translationObject), BootstrapTmsConstants.BOOTSTRAP_TMS_STATUS, state.getStatus().toString());
         return state.getStatus();
     }
 
     @Override
     public TranslationStatus getTranslationObjectStatus(String strTranslationJobID,
         TranslationObject translationObject) throws TranslationException {
         String status = bootstrapTmsService.getTmsObjectStatus(strTranslationJobID, getObjectPath(translationObject));
         return TranslationConstants.TranslationStatus.fromString(status);
     }
 
     @Override
     public TranslationStatus[] updateTranslationObjectsState(String strTranslationJobID,
         TranslationObject[] translationObjects, TranslationState[] states) throws TranslationException {
         log.trace("BootstrapTranslationServiceImpl.updateTranslationObjectsState");
 
         TranslationStatus[] retStatus = new TranslationStatus[states.length];
         for (int index = 0; index < states.length; index++) {
             retStatus[index] =
                 updateTranslationObjectState(strTranslationJobID, translationObjects[index], states[index]);
         }
         return retStatus;
     }
 
     @Override
     public TranslationStatus[] getTranslationObjectsStatus(String strTranslationJobID,
         TranslationObject[] translationObjects) throws TranslationException {
         log.trace("BootstrapTranslationServiceImpl.getTranslationObjectsStatus");
 
         TranslationStatus[] retStatus = new TranslationStatus[translationObjects.length];
         for (int index = 0; index < translationObjects.length; index++) {
             retStatus[index] = getTranslationObjectStatus(strTranslationJobID, translationObjects[index]);
         }
         return retStatus;
     }
 
     @Override
     public CommentCollection<Comment> getTranslationObjectCommentCollection(String strTranslationJobID,
         TranslationObject translationObject) throws TranslationException {
         log.trace("BootstrapTranslationServiceImpl.getTranslationObjectCommentCollection");
 
         throw new TranslationException("This function is not implemented",
             TranslationException.ErrorCode.SERVICE_NOT_IMPLEMENTED);
     }
 
     @Override
     public void addTranslationObjectComment(String strTranslationJobID, TranslationObject translationObject,
         Comment comment) throws TranslationException {
         log.trace("BootstrapTranslationServiceImpl.addTranslationObjectComment");
 
         throw new TranslationException("This function is not implemented",
             TranslationException.ErrorCode.SERVICE_NOT_IMPLEMENTED);
     }
 
     @Override
     public void updateTranslationJobMetadata(String strTranslationJobID, TranslationMetadata jobMetadata,
         TranslationMethod translationMethod) throws TranslationException {
         log.trace("BootstrapTranslationServiceImpl.updateTranslationJobMetadata");
 
         throw new TranslationException("This function is not implemented",
             TranslationException.ErrorCode.SERVICE_NOT_IMPLEMENTED);
     }
 
     
     private String getObjectPath (TranslationObject translationObject){
         
         if(translationObject.getTranslationObjectSourcePath()!= null && !translationObject.getTranslationObjectSourcePath().isEmpty()){
             return  translationObject.getTranslationObjectSourcePath();
         }
         else if(translationObject.getTitle().equals("TAGMETADATA")){
             return TAG_METADATA;
         }
         else if(translationObject.getTitle().equals("ASSETMETADATA")){
             return ASSET_METADATA;
         } 
         else if(translationObject.getTitle().equals("I18NCOMPONENTSTRINGDICT")){
             return I18NCOMPONENTSTRINGDICT;
         }
         return null;
     }    
 
     public void updateDueDate(String strTranslationJobID, Date date)
             throws TranslationException {
             log.debug("NEW DUE DATE:{}",date);
             bootstrapTmsService.setTmsJobDuedate(strTranslationJobID, date);
     }
     
     private static void unzipFileFromStream(ZipInputStream zipInputStream, String targetPath) throws IOException {
         File dirFile = new File(targetPath + File.separator);
         if (!dirFile.exists()) {
             dirFile.mkdirs();
             log.trace("Created directory: {}",dirFile);
         }
 
         ZipEntry zipEntry = null;
         while (null != (zipEntry = zipInputStream.getNextEntry())) {
             String zipFileName = zipEntry.getName();
             if (zipEntry.isDirectory()) {
                 File zipFolder = new File(targetPath + File.separator + zipFileName);
                 if (!zipFolder.exists()) {
                     zipFolder.mkdirs();
                     log.trace("Created directory: {}",zipFolder);
                 }
             } else {
                 File file = new File(targetPath + File.separator + zipFileName);
 
                 File parent = file.getParentFile();
                 <fim_suffix>
 
                 FileOutputStream fos = null;
                 try {
                     fos = new FileOutputStream(file);
                 } catch (FileNotFoundException e) {
                     log.error(e.getLocalizedMessage(),e);
                 }
                 int readLen = 0;
                 byte buffer[] = new byte[1024];
                 while (-1 != (readLen = zipInputStream.read(buffer))) {
                     fos.write(buffer, 0, readLen);
                 }
                 fos.close();
             }
         }
         zipInputStream.close();
     }
     
 }
 
<fim_middle>