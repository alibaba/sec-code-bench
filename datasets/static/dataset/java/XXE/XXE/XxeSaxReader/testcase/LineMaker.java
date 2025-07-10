<filename>src/main/java/com/thinkdifferent/convertpreview/utils/LineMaker.java<fim_prefix>

package com.thinkdifferent.convertpreview.utils;

import cn.hutool.core.io.FileUtil;
import com.thinkdifferent.convertpreview.entity.ConvertVideoEntity;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import java.io.File;
import java.util.Arrays;
import java.util.List;

public class LineMaker {

    /**
     * 获取：通过自定义命令行模板名称，获取转换的命令行
     *
     * @param listCommand        命令行对象
     * @param convertVideoEntity 转换对象
     * @param strInputFile       输入的视频文件路径和文件名
     * @param strOutputFile      输出的截图（JPG）路径和文件名
     * @return 命令行对象List<String>
     */
    public void lineByCustom(List<String> listCommand,
                             ConvertVideoEntity convertVideoEntity,
                             String strInputFile,
                             String strOutputFile) {
        if (!StringUtils.isEmpty(convertVideoEntity.getParams().getCustom())) {
            // 读取xml配置文件中，对应标签的内容
            String strLine = getLine(convertVideoEntity.getParams().getCustom());
            // 替换输入文件路径
            strLine = strLine.replaceAll("\\$\\{inputFile}", strInputFile);
            // 替换输出文件路径
            strLine = strLine.replaceAll("\\$\\{outputFile}", strOutputFile);

            String[] strLines = strLine.split(" ");

            for (int i = 0; i < strLines.length; i++) {
                listCommand.add(strLines[i]);
            }

        }

    }

    public List<String> lineByCustomM3u8(String strInputFile,
                                         String strOutputFile) {
        // 读取xml配置文件中，对应标签的内容
        String strLine = getLine("m3u8");

        // 替换输入文件路径
        strLine = strLine.replaceAll("\\$\\{inputFile}", SystemUtil.beautifulFilePath(strInputFile));
        // 替换输出文件路径
        strLine = strLine.replaceAll("\\$\\{outputFile}", SystemUtil.beautifulFilePath(strOutputFile));

        String[] strLines = strLine.split(" ");
        return Arrays.asList(strLines);
    }

    /**
     * 根据输入的“定制命令行名称”，从配置文件中获取命令行模板，替换文件名后，生成命令行
     *
     * @param strName 模板名称
     * @return 命令行对象List<String>
     */
    public String getLine(String strName) {
        try {
            String strRoot = System.getProperty("user.dir");
            strRoot = strRoot.replaceAll("\\\\", "/");
            String strPath = strRoot + "/conf/";

            SAXReader reader = new SAXReader();
            <fim_suffix>
            Document document = reader.read(new File(strPath + "CustomLine.xml"));
            Element elmRoot = document.getRootElement();
            Element elmField = elmRoot.element(strName);
            if (elmField == null) {
                elmField = elmRoot.element("default");
            }
            return elmField.getTextTrim();

        } catch (Exception | Error e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * 获取：通过参数设置转换MP3的命令行
     *
     * @param listCommand   命令行对象
     * @param strInputFile  输入的视频文件路径和文件名
     * @param strOutputFile 输出的截图（JPG）路径和文件名
     * @return 命令行对象List<String>
     */
    @SneakyThrows
    public void toMp3ByParam(List<String> listCommand,
                             String strInputFile,
                             String strOutputFile) {

//        1.amr, 3gp, 3gpp, aac, ape, aif, au, mid, wma, ra, rm, rmx, vqf, ogg,   转mp3：
//        ffmpeg -i XXX.amr XXX.mp3
//        2.wav转mp3：
//        ffmpeg -i XXX.wav -f mp3 -acodec libmp3lame -y XXX.mp3

        // 指定输入文件名
        listCommand.add("-i");
        listCommand.add(strInputFile);

        String strInputFileType = FileUtil.extName(new File(strInputFile));
        if ("wav".equalsIgnoreCase(strInputFileType)) {
            listCommand.add("-f");
            listCommand.add("mp3");
            listCommand.add("-acodec");
            listCommand.add("libmp3lame");
        }

        // 覆盖已有文件
        listCommand.add("-y");
        listCommand.add(strOutputFile);
    }

    /**
     * 获取：通过参数设置转换MP4的命令行
     *
     * @param listCommand        命令行对象
     * @param convertVideoEntity 转换对象
     * @param strInputFile       输入的视频文件路径和文件名
     * @param strOutputFile      输出的截图（JPG）路径和文件名
     * @return 命令行对象List<String>
     */
    public void toMp4ByParam(List<String> listCommand,
                             ConvertVideoEntity convertVideoEntity,
                             String strInputFile,
                             String strOutputFile) {
        // 限制使用CPU核心数量
        if (convertVideoEntity.getParams() != null &&
                convertVideoEntity.getParams().getThreads() != 0) {
            listCommand.add("-threads");
            listCommand.add(String.valueOf(convertVideoEntity.getParams().getThreads()));
        }

        // 指定输入文件名
        listCommand.add("-i");
        listCommand.add(strInputFile);


        // 设置视频格式为libx264
        listCommand.add("-c:v");
        // 如果此处设置为copy，则取原编码格式
        if (convertVideoEntity.getParams() != null &&
                !StringUtils.isEmpty(convertVideoEntity.getParams().getVideoCode())) {
            listCommand.add(convertVideoEntity.getParams().getVideoCode());
        } else {
            listCommand.add("libx264");
        }

        // 指定帧率(fps)
        if (convertVideoEntity.getParams() != null &&
                convertVideoEntity.getParams().getFps() != 0) {
            listCommand.add("-r");
            listCommand.add(String.valueOf(convertVideoEntity.getParams().getFps()));
        }

        // 指定分辨率
        if (convertVideoEntity.getParams() != null &&
                !StringUtils.isEmpty(convertVideoEntity.getParams().getResolution())) {
            listCommand.add("-s");
            listCommand.add(convertVideoEntity.getParams().getResolution());
        }

        // -mbd mode 宏块决策。0：FF_MB_DECISION_SIMPLE，使用mb_cmp；1：FF_MB_DECISION_BITS；2：FF_MB_DECISION_RD
        listCommand.add("-mbd");
        listCommand.add("0");

        // 设置音频格式为aac
        listCommand.add("-c:a");
        // 如果此处设置为copy，则取原编码格式
        if (convertVideoEntity.getParams() != null &&
                !StringUtils.isEmpty(convertVideoEntity.getParams().getAudioCode())) {
            listCommand.add(convertVideoEntity.getParams().getAudioCode());
        } else {
            listCommand.add("aac");
        }

        // -strict strictness 跟标准的严格性
        listCommand.add("-strict");
        listCommand.add("-2");
        // 设置像素格式为yuv420p
        listCommand.add("-pix_fmt");
        listCommand.add("yuv420p");

        // 加入水印
        if (convertVideoEntity.getParams() != null &&
                (convertVideoEntity.getParams().getPicMark() != null ||
                        convertVideoEntity.getParams().getTextMark() != null)) {

            String strCommand = "";
            // 判断是否设置图片水印，拼装图片水印的参数
            if (convertVideoEntity.getParams().getPicMark() != null) {
                // movie是指图片水印路径，搭配overlay一起使用，重要的是overlay=后面的部分，
                // 第一个参数表示水印距离视频左边的距离，第二个参数表示水印距离视频上边的距离
                String strScale = "";
                if (!StringUtils.isEmpty(convertVideoEntity.getParams().getPicMark().getScale())) {
                    strScale = ",scale=" + convertVideoEntity.getParams().getPicMark().getScale();
                }
                strCommand = "movie=" + convertVideoEntity.getParams().getPicMark().getPicFile() + strScale + "[wm];" +
                        "[in][wm]overlay=" + convertVideoEntity.getParams().getPicMark().getOverlay() + "[out]";
            } else if (convertVideoEntity.getParams().getTextMark() != null) {
                // 判断是否设置文字水印，拼装文字水印的参数
                // 加入文字水印。text是文字内容，x:y是显示位置，fontsize文字大小，fontcolor文字颜色
                strCommand = "drawtext=fontfile=" + convertVideoEntity.getParams().getTextMark().getFontFile() + ":" +
                        "text='" + convertVideoEntity.getParams().getTextMark().getText() + "':" +
                        "x=" + convertVideoEntity.getParams().getTextMark().getLocalX() + ":" +
                        "y=" + convertVideoEntity.getParams().getTextMark().getLocalY() + ":" +
                        "fontsize=" + convertVideoEntity.getParams().getTextMark().getFontSize() + ":" +
                        "fontcolor=" + convertVideoEntity.getParams().getTextMark().getFontColor() + ":" +
                        "shadowy=2";
            }

            if (!StringUtils.isEmpty(strCommand)) {
                listCommand.add("-vf");
                listCommand.add("\"" + strCommand + "\"");
            }

        }

        // 如果不指定faststart, 则ffmpeg默认会将moov box放在mdat box的后面，如果加上这个flag，moov box会放在前面。
        listCommand.add("-movflags");
        listCommand.add("faststart");
        // 覆盖已有文件
        listCommand.add("-y");

        listCommand.add(strOutputFile);
    }

    /**
     * 获取：截图的命令行
     *
     * @param listCommand        命令行对象
     * @param convertVideoEntity 转换对象
     * @param strInputFile       输入的视频文件路径和文件名
     * @param strOutputFile      输出的截图（JPG）路径和文件名
     * @return 命令行对象List<String>
     */
    public void toJpg(List<String> listCommand,
                      ConvertVideoEntity convertVideoEntity,
                      String strInputFile,
                      String strOutputFile) {
        // -ss 00:50:00  -i RevolutionOS.rmvb sample.jpg  -r 1 -vframes 1 -an -vcodec mjpeg
        listCommand.add("-ss");
        listCommand.add(convertVideoEntity.getParams().getTime());
        // 指定输入文件名
        listCommand.add("-i");
        listCommand.add(strInputFile);
        // 覆盖已有文件
        listCommand.add("-y");
        listCommand.add("-f");
        listCommand.add("image2");
        listCommand.add("-vframes");
        listCommand.add("1");
        listCommand.add(strOutputFile);
    }

}

<fim_middle>