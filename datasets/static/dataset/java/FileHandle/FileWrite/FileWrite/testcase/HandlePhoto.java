<filename>src/com/upc/smartbus/HandlePhoto.java<fim_prefix>

        package com.upc.smartbus;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

import android.graphics.Bitmap;

public class HandlePhoto {
    /*
     * 这个类中的方法，用来处理照片
     */
    private ConnectService cs;//连接webservice,获取照片名称，下载照片
    private Thumbnail tb;//获取照片缩略图。
    private Bitmap bitmap;
    private String bigphotourl="http://192.168.191.1:8080/axis2/images/";
    private String SmallphotoPath="mnt/sdcard/Smartbus/small/";


    //判断sd卡上是否已经存在照片，若存在则不下载。
    public boolean fileExist(String file_absolutePath){
        try{
            File myfile=new File(file_absolutePath);
            if(!myfile.exists())//照片不存在返回false，存在返回true
                return false;
        }catch(Exception e){
            return false;
        }
        return true;
    }
    /*******************获取照片名称，并以字符串数组形式返回*********************/
    public String[] handle_photoname(String cityName){
        cs=new ConnectService();
        return namesplit(cs.getPhotoName(cityName));
    }
    public String[] namesplit(String name){//解析字符串为字符串数组
        int count=0;
        //统计照片名称个数
        for(int k=0;k<name.length();k++){
            if(name.charAt(k)==',')
                count++;
        }
        String []ss=new String[count];
        ss=name.split(",");
        return ss;
    }
    /********************下载照片到sd卡***************************************/
    //下载缩略图的方法.
    public boolean DownLoad2SDCard(String []photoName){
        cs=new ConnectService();
        FileOutputStream fos=null;
        String []image=new String[photoName.length];
        System.out.println(photoName.length);
        for (int i=0;i<photoName.length;i++){
            if(!fileExist(SmallphotoPath+photoName[i])){
                image[i]=cs.downloadphoto(photoName[i],"downloadSmallphoto");
                try{
                    byte [] buffer=android.util.Base64.decode(image[i], android.util.Base64.DEFAULT);
                    File destdir=new File(SmallphotoPath);
                    if(!destdir.exists())destdir.mkdir();
<fim_suffix>
                    fos.write(buffer);
                    image[i]=null;
                    System.gc();//处理内存溢出
                    fos.flush();
                    fos.close();
                }
                catch(Exception e){
                    e.printStackTrace();
                    return false;
                }
            }
            else{
                System.out.println("error___error");
            }
        }
        return true;
    }
    /****************获取sd卡中照片的缩略图*******************************************/
    //获取清晰图片。
    public Bitmap getBigPhoto(String photoName) throws FileNotFoundException{
        tb=new Thumbnail();
        bitmap=null;
        bitmap=tb.getImageThumbnail(bigphotourl+photoName);
        return bitmap;
    }
}
<fim_middle>