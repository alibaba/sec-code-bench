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

import com.caucho.hessian.io.Hessian2Output;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

public class User implements Serializable {
    private String name;
    private int age;
    private String email;

    public User(String name, int age, String email) {
        this.name = name;
        this.age = age;
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    @Override
    public boolean equals(Object user) {
        if (user instanceof User){
            User user2 = (User) user;
            if (this.name.equals(user2.getName()) && this.age == user2.getAge() && this.email.equals(user2.getEmail())) {
                return true;
            } else {
                return false;
            }
        }

        return false;

    }

    public byte[] toBytes() throws Exception{
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        Hessian2Output oos = new Hessian2Output(bos);
        oos.writeObject(this);
        oos.flush();
        oos.close();
        return bos.toByteArray();
    }
}
