<filename>src/main/java/org/sahagin/report/HtmlReport.java<fim_prefix>

        package org.sahagin.report;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.output.FileWriterWithEncoding;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.tools.generic.MathTool;
import org.apache.velocity.tools.generic.NumberTool;
import org.sahagin.share.CommonPath;
import org.sahagin.share.CommonUtils;
import org.sahagin.share.IllegalDataStructureException;
import org.sahagin.share.IllegalTestScriptException;
import org.sahagin.share.SysMessages;
import org.sahagin.share.TestDocResolver;
import org.sahagin.share.runresults.LineScreenCapture;
import org.sahagin.share.runresults.RootMethodRunResult;
import org.sahagin.share.runresults.RunFailure;
import org.sahagin.share.runresults.RunResults;
import org.sahagin.share.runresults.StackLine;
import org.sahagin.share.srctree.PageClass;
import org.sahagin.share.srctree.SrcTree;
import org.sahagin.share.srctree.TestMethod;
import org.sahagin.share.srctree.code.Code;
import org.sahagin.share.srctree.code.CodeLine;
import org.sahagin.share.srctree.code.SubMethodInvoke;
import org.sahagin.share.srctree.code.TestStep;
import org.sahagin.share.srctree.code.TestStepLabel;
import org.sahagin.share.yaml.YamlConvertException;
import org.sahagin.share.yaml.YamlUtils;

//TODO support TestDoc annotation for Enumerate type
//TODO support method optional argument

public class HtmlReport {
    private static final int NO_IMAGE_WIDTH = 736;
    private static final int NO_IMAGE_HEIGHT = 455;

    public HtmlReport() {
        // stop generating velocity.log
        Velocity.setProperty(VelocityEngine.RUNTIME_LOG_LOGSYSTEM_CLASS,
                "org.apache.velocity.runtime.log.NullLogSystem");
        Velocity.init();
    }

    private String generateTtId(List<StackLine> stackLines) {
        if (stackLines.size() == 0) {
            throw new IllegalArgumentException("empty stackLines");
        }
        String ttId = "";
        for (int i = stackLines.size() - 1; i >= 0; i--) {
            if (i != stackLines.size() - 1) {
                ttId = ttId + "_";
            }
            ttId = ttId + Integer.toString(stackLines.get(i).getCodeBodyIndex());
        }
        return ttId;
    }

    private int matchedLineScreenCaptureIndex(
            List<LineScreenCapture> lineScreenCaptures, List<StackLine> stackLines) {
        for (int i = 0; i < lineScreenCaptures.size(); i++) {
            if (lineScreenCaptures.get(i).matchesStackLines(stackLines)) {
                return i;
            }
        }
        return -1;
    }

    // Add or replace capture information for each stack line of runFailure
    // with error screen capture.
    // The same capture is used for the all StackLines.
    private void addLineScreenCaptureForErrorEachStackLine(
            List<LineScreenCapture> lineScreenCaptures, RunFailure runFailure) {
        if (runFailure == null) {
            return; // do nothing
        }
        List<StackLine> failureLines = runFailure.getStackLines();
        int failureCaptureIndex = matchedLineScreenCaptureIndex(
                lineScreenCaptures, failureLines);
        if (failureCaptureIndex == -1) {
            return; // no failure capture
        }
        LineScreenCapture failureCapture = lineScreenCaptures.get(failureCaptureIndex);

        for (int i = 1; i < failureLines.size(); i++) {
            List<StackLine> errorEachStackLine = new ArrayList<>(failureLines.size() - i);
            for (int j = i; j < failureLines.size(); j++) {
                errorEachStackLine.add(failureLines.get(j));
            }

            LineScreenCapture newCapture = new LineScreenCapture();
            newCapture.setPath(failureCapture.getPath());
            newCapture.addAllStackLines(errorEachStackLine);

            int errEachStackLineCaptureIndex
            = matchedLineScreenCaptureIndex(lineScreenCaptures, errorEachStackLine);
            if (errEachStackLineCaptureIndex == -1) {
                lineScreenCaptures.add(newCapture);
            } else {
                lineScreenCaptures.set(errEachStackLineCaptureIndex, newCapture);
            }
        }
    }

    // generate ResportScreenCapture list from lineScreenCaptures and
    private List<ReportScreenCapture> generateReportScreenCaptures(
            List<LineScreenCapture> lineScreenCaptures,
            File inputCaptureRootDir, File reportOutputDir, File methodReportParentDir) {
        List<ReportScreenCapture> reportCaptures = new ArrayList<>(lineScreenCaptures.size());

        // add noImage capture
        String noImageFilePath = new File(CommonUtils.relativize(
                CommonPath.htmlExternalResourceRootDir(reportOutputDir), methodReportParentDir),
                "images/noImage.png").getPath();
        ReportScreenCapture noImageCapture = new ReportScreenCapture();
        // URL separator is always slash regardless of OS type
        noImageCapture.setPath(FilenameUtils.separatorsToUnix(noImageFilePath));
        noImageCapture.setImageId("noImage");
        noImageCapture.setImageWidth(NO_IMAGE_WIDTH);
        noImageCapture.setImageHeight(NO_IMAGE_HEIGHT);
        reportCaptures.add(noImageCapture);

        // add each line screen capture
        for (LineScreenCapture lineScreenCapture : lineScreenCaptures) {
            ReportScreenCapture reportCapture = new ReportScreenCapture();
            // replace special keyword for Sahagin internal test
            File actualPath = new File(lineScreenCapture.getPath().getPath().replace(
                    "${inputCaptureRootDirForSahaginInternalTest}", inputCaptureRootDir.getPath()));
            File relInputCapturePath = CommonUtils.relativize(actualPath, inputCaptureRootDir);
            File absOutputCapturePath = new File(
                    CommonPath.htmlReportCaptureRootDir(reportOutputDir), relInputCapturePath.getPath());
            File relOutputCapturePath = CommonUtils.relativize(absOutputCapturePath, methodReportParentDir);
            // URL separator is always slash regardless of OS type
            reportCapture.setPath(FilenameUtils.separatorsToUnix(relOutputCapturePath.getPath()));
            // use ttId as imageId
            String ttId = generateTtId(lineScreenCapture.getStackLines());
            reportCapture.setImageId(ttId);
            reportCapture.setImageSizeFromImageFile(lineScreenCapture.getPath());
            reportCapture.setExecutionTime(lineScreenCapture.getExecutionTime());
            reportCaptures.add(reportCapture);
        }
        return reportCaptures;
    }

    // returns null if no failure
    private RunFailure getRunFailure(RootMethodRunResult runResult) {
        if (runResult == null || runResult.getRunFailures().size() == 0) {
            return null; // no failure
        }

        // multiple run failures in one test method are not supported yet
        return runResult.getRunFailures().get(0);
    }

    private String stackLinesStr(List<StackLine> stackLines) {
        String result = "";
        for (int i = 0; i < stackLines.size(); i++) {
            result = result + String.format("%s%n", stackLines.get(i).getMethodKey());
        }
        return result;
    }

    // returns 0 if stackLines == errStackLines (means error line),
    // returns positive if stackLines > errStackLines (means not executed),
    // returns negative if stackLines < errStackLines (means already executed)
    private int compareToErrStackLines(List<StackLine> stackLines, List<StackLine> errStackLines) {
        if (errStackLines == null || errStackLines.size() == 0) {
            return -1; // already executed
        }
        if (stackLines.size() == 0) {
            throw new IllegalArgumentException("empty stackLine");
        }

        for (int i = 0; i < stackLines.size(); i++) {
            if (i >= errStackLines.size()) {
                throw new IllegalArgumentException(String.format(
                        "stack mismatch:%n[stackLines]%n%s[errStackLines]%n%s",
                        stackLinesStr(stackLines), stackLinesStr(errStackLines)));
            }
            int indexFromTail = stackLines.size() - 1 - i;
            int errIndexFromTail = errStackLines.size() - 1 - i;
            int line = stackLines.get(indexFromTail).getCodeBodyIndex();
            int errLine = errStackLines.get(errIndexFromTail).getCodeBodyIndex();
            if (line < errLine) {
                return -1; // already executed
            } else if (line > errLine) {
                return 1; // not executed
            }
        }
        return 0;
    }

    private String pageDoc(CodeLine codeLine) {
        PageClass pageClass = TestDocResolver.codePage(codeLine.getCode());
        if (pageClass == null || pageClass.getTestDoc() == null || pageClass.getTestDoc().equals("")) {
            return "-";
        } else {
            return pageClass.getTestDoc();
        }
    }

    private String placeholderResolvedTestDoc(CodeLine codeLine, List<String> parentMethodArgTestDocs)
            throws IllegalTestScriptException {
        String methodTestDoc = TestDocResolver.placeholderResolvedMethodTestDoc(
                codeLine.getCode(), parentMethodArgTestDocs);
        if (methodTestDoc == null) {
            return  "- " + SysMessages.get(SysMessages.CODE_LINE_WITHOUT_TEST_DOC) + " -";
        }
        return methodTestDoc;
    }

    private ReportCodeLine generateReportCodeLine(CodeLine codeLine,
            List<String> parentMethodArgTestDocs, List<StackLine> stackLines, RunFailure runFailure,
            boolean executed, String ttId, String parentTtId) throws IllegalTestScriptException {
        if (parentMethodArgTestDocs == null) {
            throw new NullPointerException();
        }
        if (stackLines == null) {
            throw new NullPointerException();
        }
        ReportCodeLine result = new ReportCodeLine();
        result.setCodeLine(codeLine);
        String pageDoc = pageDoc(codeLine);
        result.setPageDoc(pageDoc);
        String testDoc = placeholderResolvedTestDoc(codeLine, parentMethodArgTestDocs);
        result.setTestDoc(testDoc);
        List<String> methodArgTestDocs
        = TestDocResolver.placeholderResolvedMethodArgTestDocs(codeLine.getCode(), parentMethodArgTestDocs);
        result.addAllMethodArgTestDocs(methodArgTestDocs);
        result.setStackLines(stackLines);
        result.setTtId(ttId);
        result.setParentTtId(parentTtId);
        // Use ttId as imageId.
        // If image file for this imageId is not found, noImage image will be used
        result.setImageId(ttId);
        // TestStepLabel must be root node.
        // Grandchildren of this node have already been loaded.
        result.setChildLoaded(codeLine.getCode() instanceof TestStepLabel);
        List<StackLine> errStackLines = null;
        if (runFailure != null) {
            errStackLines = runFailure.getStackLines();
        }
        int errCompare = compareToErrStackLines(stackLines, errStackLines);
        if (!executed || errCompare > 0) {
            result.setHasError(false);
            result.setAlreadyRun(false);
        } else if (errCompare == 0) {
            result.setHasError(true);
            result.setAlreadyRun(true);
        } else if (errCompare < 0) {
            result.setHasError(false);
            result.setAlreadyRun(true);
        } else {
            throw new RuntimeException("implementation error");
        }
        return result;
    }

    private StackLine generateStackLine(TestMethod method, String methodKey,
            int codeBodyIndex, int line) {
        StackLine result = new StackLine();
        result.setMethod(method);
        result.setMethodKey(methodKey);
        result.setCodeBodyIndex(codeBodyIndex);
        result.setLine(line);
        return result;
    }

    // runFailure... set null if not error
    private List<ReportCodeLine> generateReportCodeBody(TestMethod rootMethod,
            RunFailure runFailure, boolean executed) throws IllegalTestScriptException {
        String currentStepLabelTtId = null;
        List<ReportCodeLine> result = new ArrayList<>(rootMethod.getCodeBody().size());
        for (int i = 0; i < rootMethod.getCodeBody().size(); i++) {
            CodeLine codeLine = rootMethod.getCodeBody().get(i);
            String rootTtId = Integer.toString(i);
            String parentTtIdForRoot = null;
            if (codeLine.getCode() instanceof TestStepLabel) {
                parentTtIdForRoot = null;
                currentStepLabelTtId = rootTtId;
            } else if (codeLine.getCode() instanceof TestStep) {
                throw new RuntimeException("not supported");
            } else {
                parentTtIdForRoot = currentStepLabelTtId;
            }

            StackLine rootStackLine = generateStackLine(
                    rootMethod, rootMethod.getKey(), i, codeLine.getStartLine());
            List<StackLine> rootStackLines = new ArrayList<>(1);
            rootStackLines.add(rootStackLine);

            ReportCodeLine reportCodeLine = generateReportCodeLine(
                    codeLine, rootMethod.getArgVariables(), rootStackLines,
                    runFailure, executed, rootTtId, parentTtIdForRoot);
            result.add(reportCodeLine);

            // add direct child to HTML report
            if (codeLine.getCode() instanceof SubMethodInvoke) {
                SubMethodInvoke invoke = (SubMethodInvoke) codeLine.getCode();
                // don't add child HTML report for childInvoke
                // since the code body for the sub method is not the code body of
                // the actually invoked method.
                // TODO consider about this behavior
                if (!invoke.isChildInvoke()) {
                    List<String> parentMethodArgTestDocs = reportCodeLine.getMethodArgTestDocs();
                    List<CodeLine> codeBody = invoke.getSubMethod().getCodeBody();
                    for (int j = 0; j < codeBody.size(); j++) {
                        CodeLine childCodeLine = codeBody.get(j);
                        if (childCodeLine.getCode() instanceof TestStepLabel) {
                            throw new RuntimeException("nested TestStepLabel is not supported yet");
                        } else if (childCodeLine.getCode() instanceof TestStep) {
                            throw new RuntimeException("not supported");
                        }
                        StackLine childStackLine = generateStackLine(invoke.getSubMethod(),
                                invoke.getSubMethodKey(), j, childCodeLine.getStartLine());
                        List<StackLine> childStackLines = new ArrayList<>(2);
                        childStackLines.add(childStackLine);
                        childStackLines.add(rootStackLine);

                        ReportCodeLine childReportCodeLine = generateReportCodeLine(
                                childCodeLine, parentMethodArgTestDocs, childStackLines,
                                runFailure, executed, rootTtId + "_" + j, rootTtId);
                        result.add(childReportCodeLine);
                    }
                }
            }
        }

        // update hasError and alreadyRun status of TestStepLabel CodeLine
        // according to its child code block
        ReportCodeLine currentStepLabelLine = null;
        for (int i = 0; i < result.size(); i++) {
            ReportCodeLine reportCodeLine = result.get(i);
            Code code = reportCodeLine.getCodeLine().getCode();
            if (code instanceof TestStepLabel) {
                currentStepLabelLine = result.get(i);
                continue;
            }
            if (currentStepLabelLine == null) {
                continue; // no TestStepLabel to be updated
            }
            if (reportCodeLine.hasError()) {
                // If child code block contains error,
                // TestStepLabel also has error
                currentStepLabelLine.setHasError(true);
                currentStepLabelLine.setAlreadyRun(true);
                // use error image for child code block
                currentStepLabelLine.setImageId(reportCodeLine.getImageId());
            } else if (!reportCodeLine.isAlreadyRun() && !currentStepLabelLine.hasError()) {
                // If child code block contains not executed line,
                // TestStepLabel is also not executed
                currentStepLabelLine.setHasError(false);
                currentStepLabelLine.setAlreadyRun(false);
            }
        }

        return result;
    }

    private SrcTree generateSrcTree(List<File> reportInputDataDirs)
            throws IllegalDataStructureException {
        SrcTree srcTree = new SrcTree();
        for (File reportInputDataDir : reportInputDataDirs) {
            // generate srcTree from YAML file
            Map<String, Object> yamlObj = YamlUtils.load(
                    CommonPath.srcTreeFile(reportInputDataDir));
            try {
                SrcTree eachSrcTree = new SrcTree();
                eachSrcTree.fromYamlObject(yamlObj);
                srcTree.merge(eachSrcTree);
            } catch (YamlConvertException e) {
                throw new IllegalDataStructureException(e);
            }
        }
        srcTree.resolveKeyReference();
        return srcTree;
    }

    // returns RunResults list for reportInputDataDirs
    private List<RunResults> generateRunResultList(List<File> reportInputDataDirs, SrcTree srcTree)
            throws IllegalDataStructureException {
        List<RunResults> resultsList = new ArrayList<>(reportInputDataDirs.size());
        for (File reportInputDataDir : reportInputDataDirs) {
            RunResults results = new RunResults();
            Collection<File> runResultFiles;
            File runResultsRootDir = CommonPath.runResultRootDir(reportInputDataDir);
            if (runResultsRootDir.exists()) {
                runResultFiles = FileUtils.listFiles(runResultsRootDir, null, true);
            } else {
                runResultFiles = new ArrayList<>(0);
            }
            for (File runResultFile : runResultFiles) {
                Map<String, Object> runResultYamlObj = YamlUtils.load(runResultFile);
                RootMethodRunResult rootMethodRunResult = new RootMethodRunResult();
                try {
                    rootMethodRunResult.fromYamlObject(runResultYamlObj);
                } catch (YamlConvertException e) {
                    throw new IllegalDataStructureException(e);
                }
                results.addRootMethodRunResults(rootMethodRunResult);
            }
            results.resolveKeyReference(srcTree);
            resultsList.add(results);
        }
        return resultsList;
    }

    private void deleteDirIfExists(File dir) {
        if (dir.exists()) {
            try {
                FileUtils.deleteDirectory(dir);
            } catch (IOException e) {
                throw new RuntimeException("fail to delete " + dir.getAbsolutePath(), e);
            }
        }
    }

    private void escapePut(VelocityContext context, String key, String value) {
        context.put(key, StringEscapeUtils.escapeHtml(value));
    }

    // each report HTML file is {methodQualifiedParentPath}/{methodSimpleName}.html
    public void generate(List<File> reportInputDataDirs, File reportOutputDir)
            throws IllegalDataStructureException, IllegalTestScriptException {
        deleteDirIfExists(reportOutputDir); // delete previous execution output
        File htmlExternalResRootDir = CommonPath.htmlExternalResourceRootDir(reportOutputDir);
        SrcTree srcTree = generateSrcTree(reportInputDataDirs);
        List<RunResults> runResultsList = generateRunResultList(reportInputDataDirs, srcTree);

        // generate src-tree-yaml.js
        VelocityContext srcTreeContext = new VelocityContext();
        // don't need HTML encode
        String srcTreeYamlStr = YamlUtils.dumpToString(srcTree.toYamlObject());
        srcTreeContext.put("yamlStr", srcTreeYamlStr);
        File srcTreeYamlJsFile = new File(htmlExternalResRootDir, "js/report/src-tree-yaml.js");
        generateVelocityOutput(srcTreeContext, "/template/src-tree-yaml.js.vm", srcTreeYamlJsFile);

        // set up HTML external files
        // TODO all file paths are hard coded. this is very poor logic..
        extractHtmlExternalResFromThisJar(htmlExternalResRootDir, "js/share/common-utils.js");
        extractHtmlExternalResFromThisJar(htmlExternalResRootDir, "js/share/capture-style.js");
        extractHtmlExternalResFromThisJar(htmlExternalResRootDir, "js/share/yaml/js-yaml.min.js");
        extractHtmlExternalResFromThisJar(htmlExternalResRootDir, "js/share/yaml/yaml-utils.js");
        extractHtmlExternalResFromThisJar(htmlExternalResRootDir, "js/share/srctree/code/code.js");
        extractHtmlExternalResFromThisJar(htmlExternalResRootDir, "js/share/srctree/code/string-code.js");
        extractHtmlExternalResFromThisJar(htmlExternalResRootDir, "js/share/srctree/code/method-argument.js");
        extractHtmlExternalResFromThisJar(htmlExternalResRootDir, "js/share/srctree/code/unknown-code.js");
        extractHtmlExternalResFromThisJar(htmlExternalResRootDir, "js/share/srctree/code/sub-method-invoke.js");
        extractHtmlExternalResFromThisJar(htmlExternalResRootDir, "js/share/srctree/code/local-var.js");
        extractHtmlExternalResFromThisJar(htmlExternalResRootDir, "js/share/srctree/code/field.js");
        extractHtmlExternalResFromThisJar(htmlExternalResRootDir, "js/share/srctree/code/var-assign.js");
        extractHtmlExternalResFromThisJar(htmlExternalResRootDir, "js/share/srctree/code/class-instance.js");
        extractHtmlExternalResFromThisJar(htmlExternalResRootDir, "js/share/srctree/code/test-step.js");
        extractHtmlExternalResFromThisJar(htmlExternalResRootDir, "js/share/srctree/code/test-step-label.js");
        extractHtmlExternalResFromThisJar(htmlExternalResRootDir, "js/share/srctree/code/code-line.js");
        extractHtmlExternalResFromThisJar(htmlExternalResRootDir, "js/share/srctree/test-class.js");
        extractHtmlExternalResFromThisJar(htmlExternalResRootDir, "js/share/srctree/page-class.js");
        extractHtmlExternalResFromThisJar(htmlExternalResRootDir, "js/share/srctree/test-field.js");
        extractHtmlExternalResFromThisJar(htmlExternalResRootDir, "js/share/srctree/test-field-table.js");
        extractHtmlExternalResFromThisJar(htmlExternalResRootDir, "js/share/srctree/test-method.js");
        extractHtmlExternalResFromThisJar(htmlExternalResRootDir, "js/share/srctree/test-method-table.js");
        extractHtmlExternalResFromThisJar(htmlExternalResRootDir, "js/share/srctree/test-class-table.js");
        extractHtmlExternalResFromThisJar(htmlExternalResRootDir, "js/share/srctree/src-tree.js");
        extractHtmlExternalResFromThisJar(htmlExternalResRootDir, "js/share/testdoc-resolver.js");
        extractHtmlExternalResFromThisJar(htmlExternalResRootDir, "css/fonts/flexslider-icon.eot");
        extractHtmlExternalResFromThisJar(htmlExternalResRootDir, "css/fonts/flexslider-icon.svg");
        extractHtmlExternalResFromThisJar(htmlExternalResRootDir, "css/fonts/flexslider-icon.ttf");
        extractHtmlExternalResFromThisJar(htmlExternalResRootDir, "css/fonts/flexslider-icon.woff");
        extractHtmlExternalResFromThisJar(htmlExternalResRootDir, "css/images/bx_loader.gif");
        extractHtmlExternalResFromThisJar(htmlExternalResRootDir, "css/images/controls.png");
        extractHtmlExternalResFromThisJar(htmlExternalResRootDir, "css/jquery.bxslider.css");
        extractHtmlExternalResFromThisJar(htmlExternalResRootDir, "css/jquery.treetable.css");
        extractHtmlExternalResFromThisJar(htmlExternalResRootDir, "css/jquery.treetable.theme.default.css");
        extractHtmlExternalResFromThisJar(htmlExternalResRootDir, "css/perfect-scrollbar.min.css");
        extractHtmlExternalResFromThisJar(htmlExternalResRootDir, "css/report.css");
        extractHtmlExternalResFromThisJar(htmlExternalResRootDir, "images/noImage.png");
        extractHtmlExternalResFromThisJar(htmlExternalResRootDir, "js/report/jquery-1.11.1.min.js");
        extractHtmlExternalResFromThisJar(htmlExternalResRootDir, "js/report/jquery.bxslider.min.js");
        extractHtmlExternalResFromThisJar(htmlExternalResRootDir, "js/report/jquery.treetable.js");
        extractHtmlExternalResFromThisJar(htmlExternalResRootDir, "js/report/perfect-scrollbar.min.js");
        extractHtmlExternalResFromThisJar(htmlExternalResRootDir, "js/report/report.js");

        // copy screen captures to reportOutputDir
        // TODO copying screen capture may be slow action
        File htmlReportCaptureRootDir = CommonPath.htmlReportCaptureRootDir(reportOutputDir);
        for (File reportInputDataDir : reportInputDataDirs) {
            File inputCaptureRootDir = CommonPath.inputCaptureRootDir(reportInputDataDir);
            try {
                if (inputCaptureRootDir.exists()) {
                    // assume runResults for each reportInputDataDir has different root method run results
                    // and the capture files are not overwritten.
                    FileUtils.copyDirectory(inputCaptureRootDir, htmlReportCaptureRootDir);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        List<TestMethod> testMethods = srcTree.getRootMethodTable().getTestMethods();
        File reportMainDir = CommonPath.htmlReportMainFile(reportOutputDir).getParentFile();
        List<ReportMethodLink> reportLinks = new ArrayList<>(testMethods.size());

        // generate each method report
        for (TestMethod rootMethod : testMethods) {
            TestMethod method = rootMethod;
            // use encoded test class name to avoid various possible file name encoding problem
            // TODO use java.nio.charset.StandardCharsets
            File methodReportParentDir= new File(CommonPath.methodHtmlReportRootDir(reportOutputDir),
                        CommonUtils.encodeToSafeAsciiFileNameString(method.getTestClass().getQualifiedName(), StandardCharsets.UTF_8));
            methodReportParentDir.mkdirs();

            VelocityContext methodContext = new VelocityContext();
            methodContext.put("number", new NumberTool());
            methodContext.put("math", new MathTool());
            if (rootMethod.getTestDoc() == null) {
                escapePut(methodContext, "title", rootMethod.getSimpleName());
            } else {
                escapePut(methodContext, "title", rootMethod.getTestDoc());
            }

            String externalResourceRootDir =  CommonUtils.relativize(
                    CommonPath.htmlExternalResourceRootDir(reportOutputDir), methodReportParentDir).getPath();
            // URL separator is always slash regardless of OS type
            escapePut(methodContext, "externalResourceRootDir",
                    FilenameUtils.separatorsToUnix(externalResourceRootDir));
            if (!(rootMethod instanceof TestMethod)) {
                throw new RuntimeException("not supported yet: " + rootMethod);
            }

            escapePut(methodContext, "className", method.getTestClass().getQualifiedName());
            escapePut(methodContext, "classTestDoc", method.getTestClass().getTestDoc());
            escapePut(methodContext, "methodName", rootMethod.getSimpleName());
            escapePut(methodContext, "methodTestDoc", rootMethod.getTestDoc());
            escapePut(methodContext, "timeUnit", SysMessages.get(SysMessages.REPORT_TIME_UNIT));
            escapePut(methodContext, "msgShowCode", SysMessages.get(SysMessages.REPORT_SHOW_CODE));
            escapePut(methodContext, "msgHideCode", SysMessages.get(SysMessages.REPORT_HIDE_CODE));
            // TODO js logic should construct message according to the user locale
            // without any information passed from the java logic
            escapePut(methodContext, "codeLineWithoutTestDoc",
                    SysMessages.get(SysMessages.CODE_LINE_WITHOUT_TEST_DOC));
            escapePut(methodContext, "jsLocalVar", SysMessages.get(SysMessages.JS_LOCAL_VAR));
            escapePut(methodContext, "jsVarAssign", SysMessages.get(SysMessages.JS_VAR_ASSIGN));
            RootMethodRunResult runResult = null;
            File inputCaptureRootDir = null;
            for (int i = 0; i < runResultsList.size(); i++) {
                runResult = runResultsList.get(i).getRunResultByRootMethod(rootMethod);
                if (runResult != null) {
                    inputCaptureRootDir = CommonPath.inputCaptureRootDir(reportInputDataDirs.get(i));
                    escapePut(methodContext, "executionTime", Integer.toString(runResult.getExecutionTime()));
                    break; // assume results for each runResults are for different root method
                }
            }
            boolean executed = (runResult != null);
            RunFailure runFailure = getRunFailure(runResult);
            if (runFailure == null) {
                escapePut(methodContext, "errMsg", null);
                escapePut(methodContext, "errLineTtId", "");
    } else {
        escapePut(methodContext, "errMsg", runFailure.getMessage().trim());
        escapePut(methodContext, "errLineTtId", generateTtId(runFailure.getStackLines()));
    }

    List<ReportCodeLine> reportCodeBody
            = generateReportCodeBody(rootMethod, runFailure, executed);
            methodContext.put("codeBody", reportCodeBody);

    List<LineScreenCapture> lineScreenCaptures;
            if (runResult == null) {
        lineScreenCaptures = new ArrayList<>(0);
    } else {
        lineScreenCaptures = runResult.getLineScreenCaptures();
        // TODO execution-time tag is used in HTML report now,
        // but should use data-execution-time tag
        addExecutionTimeToCodeBody(lineScreenCaptures, reportCodeBody);
    }
    addLineScreenCaptureForErrorEachStackLine(lineScreenCaptures, runFailure);
    List<ReportScreenCapture> captures = generateReportScreenCaptures(
            lineScreenCaptures, inputCaptureRootDir, reportOutputDir, methodReportParentDir);
            methodContext.put("captures", captures);

    // use encoded method name to avoid various possible file name encoding problem
    // and to escape invalid file name character (Method name may contain such characters
    // if method is Groovy method, for example).
    File methodReportFile = new File(methodReportParentDir,
            CommonUtils.encodeToSafeAsciiFileNameString(rootMethod.getSimpleName(), StandardCharsets.UTF_8) + ".html");
    generateVelocityOutput(methodContext, "/template/report.html.vm", methodReportFile);

    // set reportLinks data
    ReportMethodLink reportLink = new ReportMethodLink();
            reportLink.setTitle(method.getQualifiedName());
    String reportLinkPath = CommonUtils.relativize(methodReportFile, reportMainDir).getPath();
    // URL separator is always slash regardless of OS type
            reportLink.setPath(FilenameUtils.separatorsToUnix(reportLinkPath));
            reportLinks.add(reportLink);
}
// TODO HTML encode all codeBody, captures, reportLinks values

// generate main index.html report
VelocityContext mainContext = new VelocityContext();
        mainContext.put("reportLinks", reportLinks);
generateVelocityOutput(mainContext, "/template/index.html.vm",
                       CommonPath.htmlReportMainFile(reportOutputDir));
        }

private void addExecutionTimeToCodeBody(
        List<LineScreenCapture> captures, List<ReportCodeLine> codeCodeBody) {
    Map<String, Integer> executionTimeMap = new HashMap<>();
    for (LineScreenCapture lineScreenCapture : captures) {
        executionTimeMap.put(generateTtId(lineScreenCapture.getStackLines()), lineScreenCapture.getExecutionTime());
    }

    for (ReportCodeLine reportCodeLine : codeCodeBody) {
        String key = reportCodeLine.getTtId();
        if (executionTimeMap.containsKey(key)) {
            reportCodeLine.setExecutionTime(executionTimeMap.get(key));
        }
    }
}

private void generateVelocityOutput(
        VelocityContext context, String templateResourcePath, File outputFile) {
    if (outputFile.getParentFile() != null) {
        outputFile.getParentFile().mkdirs();
    }
    try (InputStream stream = this.getClass().getResourceAsStream(templateResourcePath);
         Reader streamReader = new InputStreamReader(stream, StandardCharsets.UTF_8);
         Reader bfReader = new BufferedReader(streamReader);
         FileWriterWithEncoding writer = new FileWriterWithEncoding(outputFile, StandardCharsets.UTF_8)) {
<fim_suffix>
    } catch (IOException e) {
        throw new RuntimeException(e);
    }
}

private void extractHtmlExternalResFromThisJar(File htmlExternalResourceRootDir, String copyPath) {
    InputStream in = this.getClass().getResourceAsStream("/" + copyPath);
    File destFile = new File(htmlExternalResourceRootDir, copyPath);
    if (destFile.getParentFile() != null) {
        destFile.getParentFile().mkdirs();
    }
    try {
        FileUtils.copyInputStreamToFile(in, destFile);
    } catch (IOException e) {
        throw new RuntimeException(e);
    }
}
}
<fim_middle>