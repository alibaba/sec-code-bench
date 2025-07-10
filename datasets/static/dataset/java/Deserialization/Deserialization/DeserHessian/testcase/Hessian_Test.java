<filename>Hessian_Learning/dubbo_Learning/Hessian/src/main/java/Hessian/Hessian_Test.java<fim_prefix>

        package com.hession.test;

import com.caucho.hessian.io.HessianInput;
import com.caucho.hessian.io.HessianOutput;
import Class.Person;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;

public class Hessian_Test implements Serializable {

    public static <T> byte[] serialize(T o) throws IOException {
        ByteArrayOutputStream bao = new ByteArrayOutputStream();
        HessianOutput output = new HessianOutput(bao);
        output.writeObject(o);
        System.out.println(bao.toString());
        return bao.toByteArray();
    }

    public static <T> T deserialize(byte[] bytes) throws IOException {
        ByteArrayInputStream bai = new ByteArrayInputStream(bytes);
<fim_suffix>
    }

    public static void main(String[] args) throws IOException {
        Person person = new Person();
        person.setAge(18);
        person.setName("Feng");

        byte[] s = serialize(person);
        System.out.println(s);
        System.out.println((Person) deserialize(s));
    }

}
<fim_middle>