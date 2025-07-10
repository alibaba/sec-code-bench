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

package com.rpc;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

public class EvilObject extends Product implements Serializable {
    private String tmpFile;

    public EvilObject(String tmpFile) {
        this.tmpFile = tmpFile;
    }

    public String getTmpFile() {
        return tmpFile;
    }

    public void setTmpFile(String tmpFile) {
        this.tmpFile = tmpFile;
    }

    private void readObject(java.io.ObjectInputStream in) throws java.io.IOException, ClassNotFoundException {
        in.defaultReadObject();
        Process p = Runtime.getRuntime().exec("touch " + tmpFile);
        try{
            p.waitFor();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public byte[] toBytes() throws Exception{
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(this);
        oos.flush();
        oos.close();
        return bos.toByteArray();
    }
}
