<filename>src/main/java/com/thinkdifferent/convertpreview/service/impl/x2x/ConvertXml2PdfServiceImpl.java<fim_prefix>

package com.thinkdifferent.convertpreview.service.impl.x2x;

import cn.hutool.core.io.FileUtil;
import com.thinkdifferent.convertpreview.service.ConvertTypeService;
import com.thinkdifferent.convertpreview.utils.SystemUtil;
import com.thinkdifferent.convertpreview.utils.XmlUtil;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.fill.JRFileVirtualizer;
import net.sf.jasperreports.engine.util.JRLoader;
import net.sf.json.JSONObject;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * xbrl 预览，其他xml返回原始文件
 *
 * @author ltian
 * @version 1.0
 * @date 2024/1/4 10:24
 */
@Log4j2
@Service
public class ConvertXml2PdfServiceImpl implements ConvertTypeService {
    /**
     * 根据类型进行转换
     *
     * @param inputFile 传入的文件，格式已确定
     * @param targetDir 转换后的文件路径，不带后缀
     * @return 转换后的文件或文件夹
     */
    @SneakyThrows
    @Override
    public File convert0(File inputFile, String targetDir) {
        SAXReader reader = new SAXReader();
        Document document = reader.read(inputFile);
        Element elmRoot = document.getRootElement();

        File targetFile = null;

        try {
            // 按照xml报表模板设置检索。如果有匹配的模板，则生成pdf格式的文件
            targetFile = getXmlPdf(elmRoot, FileUtil.getCanonicalPath(inputFile), targetDir + ".pdf");
        } catch (Exception | Error ignored) {

        }
        // 非xbrl，直接返回
        return Objects.isNull(targetFile) ? inputFile : targetFile;
    }

    /**
     * 根据传入的XML文件的内容，生成对应格式的pdf文件。
     *
     * @param elmRoot    XML数据根节点对象。
     * @param strXmlFile XML数据文件路径和文件名
     * @return
     * @throws Exception
     */
    private static File getXmlPdf(Element elmRoot, String strXmlFile, String pdfFilePath) throws Exception {
        String strReportFileName;

        String strSetPath = SystemUtil.beautifulPath(System.getProperty("user.dir")) + "conf/";
        File fileXmlReportSet = new File(strSetPath + "XmlReport.xml");
        SAXReader reader = new SAXReader();
        <fim_suffix>
        Element elmRootSet = document.getRootElement();
        List<Element> listSetElements = elmRootSet.elements();
        // 循环，判断设置的Element的name是否与XML中取得的href值的前半段相符。
        for (Element elmSetNode : listSetElements) {
            String strXPath = elmSetNode.attributeValue("path");

            Element elmNode = (Element) elmRoot.selectSingleNode(strXPath);
            if (elmNode != null) {
                // 如果匹配，则获取其设置的报表文件名称
                strReportFileName = elmSetNode.getText();
                // 将传入的XML转换为JSON
                String strXML = XmlUtil.readFile2String(strXmlFile);
                JSONObject jsonXML = XmlUtil.xml2json(strXML);
                // 读取指定文件夹的报表模板，生成PDF
                return createReport(jsonXML, strReportFileName, pdfFilePath);
            }
        }

        return null;
    }


    /**
     * 根据传入的JSON、指定的报表模板，生成PDF文件，并存储在指定的文件夹中
     *
     * @param joInput       报表传入JSON
     * @param strReportFile 报表模板文件路径和文件名
     * @param strPdfFile    生成的PDF文件的路径和文件名
     * @return 生成的PDF文件的File对象
     * @throws Exception err
     */
    private static File createReport(JSONObject joInput, String strReportFile, String strPdfFile) throws Exception {
        //创建报表参数Map对象，需要传入报表的参数，均需要通过这个map对象传递
        Map<String, Object> mapParam = new HashMap<>();

        // 设定报表的缓冲区
        String strCacheDir = System.getProperty("user.dir") + "/cacheDir/";
        File fileCacheDir = new File(strCacheDir);
        if (!fileCacheDir.exists() || !fileCacheDir.isDirectory()) {
            fileCacheDir.mkdirs();
        }
        JRFileVirtualizer jrFileVirtualizer = new JRFileVirtualizer(2, "cacheDir");
        jrFileVirtualizer.setReadOnly(true);
        mapParam.put(JRParameter.REPORT_VIRTUALIZER, jrFileVirtualizer);

        // 将输入的JSON参数转码为UTF-8，并转换为输入流（避免文字产生乱码）
        InputStream inputStream = new ByteArrayInputStream(joInput.toString().getBytes(StandardCharsets.UTF_8));
        // 以数据流的形式，填充报表数据源
        mapParam.put("JSON_INPUT_STREAM", inputStream);

        // 设定参数，报表模板的路径（如果报表中有子报表、图片，会用到这个路径参数）
        mapParam.put("reportPath", System.getProperty("user.dir") + "/conf/report/");

        String strReportPathFileName = System.getProperty("user.dir") + "/conf/report/" + strReportFile;
        File fileOutput = new File(strPdfFile);
        FileUtil.del(fileOutput);
        InputStream jasperStream = null;
        try {
            jasperStream = new FileInputStream(strReportPathFileName);
            // 加载报表模板
            JasperReport jasperReport = (JasperReport) JRLoader.loadObject(jasperStream);
            JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, mapParam);
            JasperExportManager.exportReportToPdfFile(jasperPrint, strPdfFile);

            return fileOutput;
        } catch (Exception | Error e) {
            log.error(e);
        } finally {
            if (inputStream != null) {
                inputStream.close();
            }
            if (jasperStream != null) {
                jasperStream.close();
            }
        }
        return null;
    }
}

<fim_middle>