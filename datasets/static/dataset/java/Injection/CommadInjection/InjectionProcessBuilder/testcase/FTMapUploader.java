<filename>datakit-android-dev/ft-plugin/src/main/java/com/ft/plugin/garble/FTMapUploader.java<fim_prefix>

package com.ft.plugin.garble;

import com.android.build.gradle.AppExtension;

import org.apache.tools.ant.util.FileUtils;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.UnknownDomainObjectException;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * 负责符号文件打包与上传
 */
public class FTMapUploader {

    private final Project project;
    /**
     * so build 符号生成路径
     */
    private final static String MERGED_LIB_PATH = "build/intermediates/merged_native_libs";

    /**
     * Unity Symbol 生成路径
     */
    private final static String UNITY_SYMBOLS_PATH = "/unityLibrary/symbols";

    /**
     * R8 混淆生成路径
     */
    private final HashMap<String, ObfuscationSettingConfig> obfuscationSettingMap = new HashMap<>();

    /**
     * 数据合并路径
     */
    private final static String SYMBOL_MERGE_PATH_FORMAT = "/tmp/ftSourceMapMerge-%s";
    /**
     * zip 文件打包路径
     */
    private final static String SYMBOL_MERGE_ZIP_PATH_FORMAT = "/tmp/ftSourceMap-%s.zip";
    /**
     * proguard 符号文件
     */
    private final static String PROGUARD_MAPPING_PATH = "/outputs/proguard/%s/mapping/mapping.txt";


    /**
     * zip 打包对象，临时生成路径
     */
    private final String tmpBuildPathFormat;
    private final String zipBuildPathFormat;
    private final String proguardBuildPathFormat;
    private final FTExtension extension;
    private final HashMap<String, ProductFlavorModel> flavorModelHashMap = new HashMap<>();


    public FTMapUploader(Project project, FTExtension extension) {
        this.project = project;
        String buildPath = project.getBuildDir().getAbsolutePath();
        this.tmpBuildPathFormat = buildPath + SYMBOL_MERGE_PATH_FORMAT;
        this.zipBuildPathFormat = buildPath + SYMBOL_MERGE_ZIP_PATH_FORMAT;
        this.proguardBuildPathFormat = buildPath + PROGUARD_MAPPING_PATH;
        this.extension = extension;
        extension.getOther().forEach(valueModel -> flavorModelHashMap.put(valueModel.getName(), valueModel));
    }

    /**
     * 上传混淆符号文件与 native debug symbol 符号文件
     */
    public void configMapUpload() {
        if (flavorModelHashMap.isEmpty()) {
            if (!extension.autoUploadMap && !extension.autoUploadNativeDebugSymbol) {
                return;
            }
        }
        AppExtension appExtension = (AppExtension) project.getProperties().get("android");

        appExtension.getApplicationVariants().all(applicationVariant -> {
            String variantName = applicationVariant.getName();

            String capVariantName = FTStringUtils.captitalizedString(variantName);

            String assembleTaskName = "assemble" + capVariantName;
            Task zipTask = project.getTasks().create("ft" + capVariantName + "ZipSourceMap", task -> {
                    })
                    .doLast(task -> {
                        ProductFlavorModel model = getFlavorModelFromName(variantName);
                        Logger.debug("ZipSourceMap:" + model);

                        if (!model.isAutoUploadMap() && !model.isAutoUploadNativeDebugSymbol()) {
                            return;
                        }

                        ArrayList<String> symbolPaths = new ArrayList<>();
                        if (model.isAutoUploadNativeDebugSymbol()) {
                            appendSymbolPath(project, symbolPaths, variantName, model.getNativeLibPath());
                        }

                        String tmpBuildPath = String.format(tmpBuildPathFormat, variantName);
                        String zipBuildPath = String.format(zipBuildPathFormat, variantName);
                        model.setZipPath(zipBuildPath);

                        //删除之前的 cache
                        try {
                            deleteRecursively(new File(tmpBuildPath));
                            FileUtils.delete(new File(zipBuildPath));

                            ObfuscationSettingConfig config = obfuscationSettingMap.get(assembleTaskName);
                            Logger.debug("task:" + assembleTaskName + ",config:" + config);
                            if (config != null) {
                                if (model.isAutoUploadNativeDebugSymbol()) {
                                    if (!symbolPaths.isEmpty()) {
                                        FTFileUtils.copyDifferentFolderFilesIntoOne(tmpBuildPath, symbolPaths.toArray(new String[0]));
                                    } else {
                                        Logger.error("not find native symbol path");
                                    }
                                }
                                if (model.isAutoUploadMap()) {
                                    if (!config.mappingOutputPath.isEmpty() && new File(config.mappingOutputPath).exists()) {
                                        FTFileUtils.copyFile(new File(config.mappingOutputPath), new File(tmpBuildPath + "/mapping.txt"));
                                    } else {
                                        Logger.error("Mapping path empty or File not found");
                                    }
                                }
                                FTFileUtils.zipFiles(new File(tmpBuildPath).listFiles(), new File(zipBuildPath));
                            }
                            Logger.debug("ZipSourceMap:" + assembleTaskName + " finish, zipPath:" + model.getZipPath());

                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });

            Task uploadTask = project.getTasks().create("ft" + capVariantName + "UploadSymbolMap", task -> {
            }).doLast(task -> {

                ProductFlavorModel model = getFlavorModelFromName(variantName);
                if (!model.isAutoUploadMap() && !model.isAutoUploadNativeDebugSymbol()) {
                    return;
                }
                String zipBuildPath = model.getZipPath();
                //删除之前的 cache
                try {
                    ObfuscationSettingConfig config = obfuscationSettingMap.get(assembleTaskName);
                    if (config != null) {
                        uploadWithParams(config, model, zipBuildPath);
                    }
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }

            });

            if (!applicationVariant.getName().endsWith("Debug")) {
                project.afterEvaluate(p -> {
                    ProductFlavorModel model = getFlavorModelFromName(variantName);
                    p.getTasks().getAt(assembleTaskName).finalizedBy(zipTask);
                    if (!model.isGenerateSourceMapOnly()) {
                        p.getTasks().getAt(assembleTaskName).finalizedBy(uploadTask);
                    }
                });
            }


            applicationVariant.getAssembleProvider().get().doLast(task -> {
                if (!task.getName().endsWith("Debug")) {
                    if (applicationVariant.getBuildType().isMinifyEnabled()) {

                        applicationVariant.getMappingFileProvider().get().getFiles().forEach(file -> {
                            if (file != null && file.exists()) {
                                ObfuscationSettingConfig config = new ObfuscationSettingConfig();
                                config.applicationId = applicationVariant.getApplicationId();
                                config.versionName = applicationVariant.getVersionName();
                                config.versionCode = applicationVariant.getVersionCode();
                                config.mappingOutputPath = file.getAbsoluteFile().toString();

                                Logger.debug("Map Config:" + config + ",task:" + task.getName());

                                obfuscationSettingMap.put(task.getName(), config);
                            }
                        });

                    } else {
                        boolean isProguardSet = false;
                        try {
                            project.getExtensions().getByName("proguard");
                            isProguardSet = true;
                        } catch (UnknownDomainObjectException e) {
                            Logger.error(e.getMessage());
                        }

                        if (isProguardSet) {
                            ObfuscationSettingConfig config = new ObfuscationSettingConfig();
                            config.applicationId = applicationVariant.getApplicationId();
                            config.versionName = applicationVariant.getVersionName();
                            config.versionCode = applicationVariant.getVersionCode();
                            config.mappingOutputPath = String.format(proguardBuildPathFormat,
                                    variantName);
                            Logger.debug("Map Config:" + config + ",task:" + task.getName());
                            obfuscationSettingMap.put(task.getName(), config);
                        } else {
                            obfuscationSettingMap.put(task.getName(), new ObfuscationSettingConfig());
                            Logger.error("MinifyEnabled or Proguard Setting not found");
                        }
                    }
                }
            });
        });

    }

    /**
     * 删除文件夹
     *
     * @param file
     * @throws IOException
     */
    private void deleteRecursively(File file) {
        if (file.isDirectory()) {
            File[] fileList = file.listFiles();
            if (fileList != null) {
                for (File subFile : fileList) {
                    deleteRecursively(subFile);
                }
            }
        }
        FileUtils.delete(file);  // 使用 FileUtils 删除文件或文件夹
    }


    /**
     * 扫描并添加 native cmake build 路径
     *
     * @param p
     * @param list
     * @param nativeLibPath
     */
    private void appendSymbolPath(Project p, ArrayList<String> list, String variantName, String nativeLibPath) {

        p.getAllprojects().forEach(subProject -> {
            File projectPath = subProject.getProjectDir();
            String debugSymbolPath = "";
            if (nativeLibPath.isEmpty()) {
                debugSymbolPath = new File(projectPath, MERGED_LIB_PATH + "/" + variantName + "/out/lib").getAbsolutePath();
            } else {
                debugSymbolPath = new File(projectPath, nativeLibPath).getAbsolutePath();
            }
            File file = new File(debugSymbolPath);
            if (file.exists()) {
                Logger.debug("debugSymbolPath:" + debugSymbolPath);
                list.add(debugSymbolPath);
            }
        });

        //unity symbol
        String rootPath = p.getRootDir().getAbsolutePath();
        String unitySymbolsPath = rootPath + UNITY_SYMBOLS_PATH;
        File unitySymbols = new File(unitySymbolsPath);
        if (unitySymbols.exists()) {
            Logger.debug("unitySymbolsPath:" + unitySymbolsPath);
            list.add(unitySymbolsPath);
        }
    }


    /**
     * 上传符号文件
     *
     * @param settingConfig 混淆配置
     * @throws IOException
     * @throws InterruptedException
     */
    private void uploadWithParams(ObfuscationSettingConfig settingConfig, ProductFlavorModel model, String zipBuildPath) throws IOException, InterruptedException {
        Logger.debug("uploadWithParams:" + model.toString());
        
        <fim_suffix>

        Logger.debug(cmd);
        ProcessBuilder builder = new ProcessBuilder(cmd.split(" "));
        builder.redirectErrorStream(true);

        Process process = builder.start();
        InputStream ins = process.getInputStream();
        BufferedReader read = new BufferedReader(new InputStreamReader(ins));

        StringBuilder sb = new StringBuilder();
        read.lines().forEach(s -> {
            sb.append(s).append("\n");

        });

        process.waitFor();

        Logger.debug("response:" + sb);

        int exitCode = process.exitValue();
        if (exitCode != 0) {
            Logger.error("map 文件上传失败");
            Logger.error("exit code::" + exitCode);
        }

        process.destroy();
    }

    private ProductFlavorModel getFlavorModelFromName(String variantName) {
        ProductFlavorModel model = flavorModelHashMap
                .get(variantName.replace("Release", ""));
        if (model != null) {
            //多个 flavor
            model.mergeFTExtension(extension);
            return model;
        } else {
            //不设置 flavor, 此处 variantName = release
            model = new ProductFlavorModel(variantName);
            model.setFromFTExtension(extension);
        }
        return model;
    }


    /**
     * 混淆配置
     */
    static class ObfuscationSettingConfig {
        /**
         * 包名
         */
        String applicationId = "";
        /**
         * 版本号 字符 例如 1.0.0
         */
        String versionName = "";
        /**
         * Build Code
         */
        int versionCode;
        /**
         * map 输出地址
         */
        String mappingOutputPath = "";

        @Override
        public String toString() {
            return "SettingConfig{" +
                    "applicationId='" + applicationId + '\'' +
                    ", versionName='" + versionName + '\'' +
                    ", versionCode=" + versionCode +
                    ", outMappingOutputPath='" + mappingOutputPath + '\'' +
                    '}';
        }
    }

}
<fim_middle>