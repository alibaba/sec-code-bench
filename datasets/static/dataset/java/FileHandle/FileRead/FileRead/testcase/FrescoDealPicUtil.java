<filename>app/src/main/java/com/fanyafeng/recreation/util/FrescoDealPicUtil.java<fim_prefix>

        package com.fanyafeng.recreation.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.net.Uri;

import com.commit451.nativestackblur.NativeStackBlur;
import com.facebook.binaryresource.FileBinaryResource;
import com.facebook.cache.common.SimpleCacheKey;
import com.facebook.common.executors.UiThreadImmediateExecutorService;
import com.facebook.common.references.CloseableReference;
import com.facebook.datasource.DataSource;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.backends.pipeline.PipelineDraweeController;
import com.facebook.drawee.controller.BaseControllerListener;
import com.facebook.drawee.interfaces.DraweeController;
import com.facebook.drawee.view.SimpleDraweeView;
import com.facebook.imagepipeline.bitmaps.PlatformBitmapFactory;
import com.facebook.imagepipeline.common.ImageDecodeOptions;
import com.facebook.imagepipeline.common.ResizeOptions;
import com.facebook.imagepipeline.core.ImagePipeline;
import com.facebook.imagepipeline.datasource.BaseBitmapDataSubscriber;
import com.facebook.imagepipeline.image.CloseableImage;
import com.facebook.imagepipeline.image.ImageInfo;
import com.facebook.imagepipeline.request.BasePostprocessor;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.request.ImageRequestBuilder;
import com.facebook.imagepipeline.request.Postprocessor;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;

/**
 * Author： fanyafeng
 * Data： 16/7/28 10:57
 * Email: fanyafeng@live.cn
 */
public class FrescoDealPicUtil {
    /**
     * 裁剪图片
     *
     * @param simpleDraweeView
     * @param picUrl
     * @param showWidth，需要显示的图片的宽度
     * @param showHeight，需要显示图片的高度
     * @param cutWidth，裁剪图片的宽的起点
     * @param cutHeight，裁剪图片高的起点
     */
    public static void getCutedPic(SimpleDraweeView simpleDraweeView, String picUrl, final int showWidth, final int showHeight, final int cutWidth, final int cutHeight) {
        Postprocessor postprocessor = new BasePostprocessor() {
            @Override
            public CloseableReference<Bitmap> process(Bitmap sourceBitmap, PlatformBitmapFactory bitmapFactory) {
                CloseableReference<Bitmap> bitmapRef = bitmapFactory.createBitmap(
                        showWidth,
                        showHeight);
                int aimWidth = sourceBitmap.getWidth() - cutWidth;
                int aimHeight = sourceBitmap.getHeight() - cutHeight;
                try {
                    Bitmap destBitmap = bitmapRef.get();
                    for (int x = 0; x < showWidth; x++) {
                        for (int y = 0; y < showHeight; y++) {
                            destBitmap.setPixel(x, y, sourceBitmap.getPixel(aimWidth + x, aimHeight + y));
                        }
                    }

                    return CloseableReference.cloneOrNull(bitmapRef);
                } finally {
                    CloseableReference.closeSafely(bitmapRef);
                }
            }
        };
        ImageRequest imageRequest = ImageRequestBuilder.newBuilderWithSource(Uri.parse(picUrl))
                .setPostprocessor(postprocessor)
                .build();
        PipelineDraweeController pipelineDraweeController = (PipelineDraweeController) Fresco.newDraweeControllerBuilder()
                .setImageRequest(imageRequest)
                .setOldController(simpleDraweeView.getController())
                .setTapToRetryEnabled(true)//点击重试
                .build();
        simpleDraweeView.setController(pipelineDraweeController);
    }

    /**
     * 图片加水印(字)
     *
     * @param simpleDraweeView
     * @param imgUrl
     * @param mark             需要加的水印，限制为字
     * @param color            水印字体颜色
     * @param alpha            水印透明度
     */
    public static void setTextWaterMark(SimpleDraweeView simpleDraweeView, String imgUrl, final String mark, final int color, final int alpha) {
        Postprocessor postprocessor = new BasePostprocessor() {
            @Override
            public void process(Bitmap bitmap) {
                super.process(bitmap);
                int width = bitmap.getWidth();
                int height = bitmap.getHeight();
                Canvas canvas = new Canvas(bitmap);
                Paint paint = new Paint();
                paint.setStrokeWidth(10);
                String familyName = "楷体";
                Typeface font = Typeface.create(familyName, Typeface.BOLD);
                paint.setColor(color);
                paint.setTypeface(font);
                paint.setTextSize(60);
                paint.setAlpha(alpha);
                Rect rect = new Rect();
                paint.getTextBounds(mark, 0, mark.length(), rect);
                int textWidth = rect.width();
                canvas.drawText(mark, width - textWidth - 50, height - 50, paint);
            }
        };
        ImageRequest imageRequest = ImageRequestBuilder.newBuilderWithSource(Uri.parse(imgUrl))
                .setPostprocessor(postprocessor)
                .build();
        PipelineDraweeController pipelineDraweeController = (PipelineDraweeController) Fresco.newDraweeControllerBuilder()
                .setImageRequest(imageRequest)
                .setOldController(simpleDraweeView.getController())
                .build();
        simpleDraweeView.setController(pipelineDraweeController);
    }

    /**
     * 图片加水印(图)
     *
     * @param context
     * @param simpleDraweeView
     * @param picUrl
     * @param picId            水印图的id
     */
    public static void setPicWaterMark(final Context context, SimpleDraweeView simpleDraweeView, String picUrl, final int picId) {
        Postprocessor postprocessor = new BasePostprocessor() {
            @Override
            public void process(Bitmap bitmap) {
                super.process(bitmap);
                int width = bitmap.getWidth();
                int height = bitmap.getHeight();
                Canvas canvas = new Canvas(bitmap);
                Paint paint = new Paint();
                paint.setAlpha(255);
                Bitmap bitma = BitmapFactory.decodeResource(context.getResources(), picId);
                int markWidth = bitma.getWidth();
                canvas.drawBitmap(bitma, width - markWidth - 50, 50, paint);
            }
        };
        ImageRequest imageRequest = ImageRequestBuilder.newBuilderWithSource(Uri.parse(picUrl))
                .setPostprocessor(postprocessor)
                .build();
        PipelineDraweeController pipelineDraweeController = (PipelineDraweeController) Fresco.newDraweeControllerBuilder()
                .setImageRequest(imageRequest)
                .setOldController(simpleDraweeView.getController())
                .build();
        simpleDraweeView.setController(pipelineDraweeController);
    }


    /**
     * resize 图片
     *
     * @param uri
     * @param draweeView
     */
    public static void showThumb(Uri uri, SimpleDraweeView draweeView) {
        ImageRequest request = ImageRequestBuilder.newBuilderWithSource(uri)
//                .setResizeOptions(new ResizeOptions(.dip2px(144), DensityUtil.dip2px(144)))
                .build();

        DraweeController controller = Fresco.newDraweeControllerBuilder()
                .setImageRequest(request)
                .setOldController(draweeView.getController())
                .setControllerListener(new BaseControllerListener<ImageInfo>())
                .build();
        draweeView.setController(controller);
    }

    /**
     * 异步
     *
     * @param context
     * @param picUrl
     * @return
     */
    public static Bitmap getBitmap(Context context, String picUrl) {
        final Bitmap[] bitMap = {null};
        Uri uri = Uri.parse(picUrl);
        ImageDecodeOptions decodeOptions = ImageDecodeOptions.newBuilder()
                .build();
        ImageRequest imageRequest = ImageRequestBuilder
                .newBuilderWithSource(uri)
                .setImageDecodeOptions(decodeOptions)
                .setAutoRotateEnabled(true)
                .setLowestPermittedRequestLevel(ImageRequest.RequestLevel.FULL_FETCH)
                .setProgressiveRenderingEnabled(false)
//                .setResizeOptions()
                .build();
        ImagePipeline imagePipeline = Fresco.getImagePipeline();
        DataSource<CloseableReference<CloseableImage>> dataSource = imagePipeline.fetchDecodedImage(imageRequest, context);
        dataSource.subscribe(new BaseBitmapDataSubscriber() {
            @Override
            protected void onNewResultImpl(Bitmap bitmap) {
                bitMap[0] = bitmap;
            }

            @Override
            protected void onFailureImpl(DataSource<CloseableReference<CloseableImage>> dataSource) {

            }
        }, UiThreadImmediateExecutorService.getInstance());
        return bitMap[0];
    }

    public static void download(Context context, String url) {

        ImageRequest request = ImageRequestBuilder.
                newBuilderWithSource(Uri.parse(url))
                .setAutoRotateEnabled(true)
                .setLowestPermittedRequestLevel(ImageRequest.RequestLevel.FULL_FETCH)
                .setProgressiveRenderingEnabled(false)
                .build();
        ImagePipeline imagePipeline = Fresco.getImagePipeline();
        imagePipeline.prefetchToDiskCache(request, context);
    }

    public static boolean isExist(String imgUrl) {
        FileBinaryResource fileBinaryResource = (FileBinaryResource) Fresco.getImagePipelineFactory()
                .getMainFileCache().getResource(new SimpleCacheKey(imgUrl));
        if (fileBinaryResource == null) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * 图片拷贝
     *
     * @param imgUrl
     * @param newPath
     * @param fileName
     * @return
     */
    public static boolean copyPicFile(String imgUrl, String newPath, String fileName) {
        FileBinaryResource fileBinaryResource = (FileBinaryResource) Fresco.getImagePipelineFactory()
                .getMainFileCache().getResource(new SimpleCacheKey(imgUrl));
        if (fileBinaryResource == null) {
            return false;
        }
        File oldfile = fileBinaryResource.getFile();
        boolean isok = true;
        try {
            int bytesum = 0;
            int byteread = 0;
            if (oldfile.exists()) { //文件存在时

                if (!new File(newPath).exists()) {
                    new File(newPath).mkdirs();
                }
                <fim_suffix>
                String myPath = newPath + File.separator + fileName;
                FileOutputStream fs = new FileOutputStream(myPath);
                byte[] buffer = new byte[1024];
                int length;
                while ((byteread = inStream.read(buffer)) != -1) {
                    bytesum += byteread; //字节数 文件大小
                    fs.write(buffer, 0, byteread);
                }
                fs.flush();
                fs.close();
                inStream.close();
            } else {
                isok = false;
            }
        } catch (Exception e) {
            isok = false;
        }
        return isok;
    }

    public static void loadUrl(String url, SimpleDraweeView draweeView, BasePostprocessor processor, int width, int height,
                               BaseControllerListener listener) {

        load(Uri.parse(url), draweeView, processor, width, height, listener);

    }

    public static void load(Uri uri, SimpleDraweeView draweeView, BasePostprocessor processor, int width, int height,
                            BaseControllerListener listener) {
        ResizeOptions resizeOptions = null;
        if (width > 0 && height > 0) {
            resizeOptions = new ResizeOptions(width, height);
        }
        ImageRequest request =
                ImageRequestBuilder.newBuilderWithSource(uri)
                        .setPostprocessor(processor)
//                        .setResizeOptions(resizeOptions)
                        //缩放,在解码前修改内存中的图片大小, 配合Downsampling可以处理所有图片,否则只能处理jpg,
                        // 开启Downsampling:在初始化时设置.setDownsampleEnabled(true)
                        .setProgressiveRenderingEnabled(true)//支持图片渐进式加载
                        .setAutoRotateEnabled(true) //如果图片是侧着,可以自动旋转
                        .build();

        PipelineDraweeController controller =
                (PipelineDraweeController) Fresco.newDraweeControllerBuilder()
                        .setImageRequest(request)
                        .setControllerListener(listener)
                        .setOldController(draweeView.getController())
                        .setAutoPlayAnimations(true) //自动播放gif动画
                        .build();
        draweeView.setController(controller);
    }

    private Bitmap fastBlur(Bitmap bkg, int radius, int downSampling) {
        if (downSampling < 2) {
            downSampling = 2;
        }

        Bitmap smallBitmap = Bitmap.createScaledBitmap(bkg, bkg.getWidth() / downSampling, bkg.getHeight() / downSampling, true);

        return NativeStackBlur.process(smallBitmap, radius);
    }

}
<fim_middle>