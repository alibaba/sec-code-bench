<filename>XCoLab-master/view/src/main/java/org/xcolab/view/pages/loginregister/ImageUploadUtils.java<fim_prefix>

        package org.xcolab.view.pages.loginregister;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.xcolab.client.admin.attributes.platform.PlatformAttributeKey;
import org.xcolab.client.content.StaticContentContext;
import org.xcolab.client.content.pojo.FileEntryWrapper;
import org.xcolab.client.content.pojo.IFileEntry;
import org.xcolab.client.content.pojo.tables.pojos.FileEntry;
import org.xcolab.client.user.StaticUserContext;
import org.xcolab.client.user.exceptions.MemberNotFoundException;
import org.xcolab.client.user.pojo.wrapper.UserWrapper;
import org.xcolab.view.util.entity.upload.FileUploadUtil;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Timestamp;
import java.util.Date;

import javax.imageio.ImageIO;

public class ImageUploadUtils {

    private static final Logger _log = LoggerFactory.getLogger(ImageUploadUtils.class);

    private static final int IMAGE_RESIZE_WIDTH = 300;
    private static final int IMAGE_RESIZE_HEIGHT = 300;

    private static final String UPLOAD_PATH = PlatformAttributeKey.FILES_UPLOAD_DIR.get();

    public static long uploadImage(URL url) {
        try {
            <fim_suffix>
            BufferedImage image = ImageIO.read(url);
            byte[] imgBArr = FileUploadUtil.resizeAndCropImage(image, IMAGE_RESIZE_WIDTH, IMAGE_RESIZE_HEIGHT);


            IFileEntry file = new FileEntry();
            file.setCreatedAt(new Timestamp(new Date().getTime()));
            file.setFileExtension("png");
            file.setFileSize(imgBArr.length);
            file.setFileName(url.toString());

            file = StaticContentContext.getFileClient()
                    .createFileEntry(new FileEntryWrapper(file, imgBArr, UPLOAD_PATH));

            return file.getId();
        } catch (IOException  e) {
            _log.error("Could not upload picture {}", url.toString(), e);
        }

        return 0L;
    }

    public static void updateProfilePicture(UserWrapper member, String picURL) {
        // Link picture if it is not yet set
        if (member.getPortraitId() == 0) {
            member.setPortraitFileEntryId(linkProfilePicture(picURL));
            try{
                StaticUserContext.getUserClient().updateUser(member);
            }catch (MemberNotFoundException e){

            }
        }
    }

    public static long linkProfilePicture(String picUrl) {
        try {
            URL url = new URL(picUrl);
            return ImageUploadUtils.uploadImage(url);
        } catch (MalformedURLException e) {
            _log.warn("Could not upload  image with url {}", picUrl, e);
        }

        return 0L;
    }
}
<fim_middle>