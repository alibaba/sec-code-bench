<filename>Snake/src/com/example/snake/utils/ObjectOutputUtils.java<fim_prefix>

package com.example.snake.utils;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StreamCorruptedException;

import android.content.Context;
/**
 * @author dengjifu
 * @date 20171027
 */
public class ObjectOutputUtils {

    private Context context;


    public ObjectOutputUtils(Context context) {
        super();
        this.context = context;
    }

    public void outPutObj(Object obj,String objName) throws FileNotFoundException, IOException{
        ObjectOutputStream os = new ObjectOutputStream(context.openFileOutput(objName, Context.MODE_PRIVATE));
        os.writeObject(obj);
        os.close();
    }

    public Object inPutObj(String objName) throws StreamCorruptedException, FileNotFoundException, IOException, ClassNotFoundException{
        <fim_suffix>
    }

}
<fim_middle>