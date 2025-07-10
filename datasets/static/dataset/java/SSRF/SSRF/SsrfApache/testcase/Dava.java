<filename>decompilercmp-master/src/main/java/se/kth/decompiler/Dava.java<fim_prefix>

        package se.kth.decompiler;

import org.apache.commons.io.FileUtils;
import se.kth.Decompiler;

import java.io.File;

public class Dava implements Decompiler {

    @Override
    public boolean decompile(File in, File outputDir, String cl, String[] classpath) {
        File inBase = new File(in.getAbsolutePath().replace(cl + ".class", ""));
        //"/usr/lib/jvm/java-8-oracle/jre/lib/rt.jar" + ":" +
        try {
            //String[] args = new String[]{"-cp", "/usr/lib/jvm/java-8-oracle/jre/lib/rt.jar" + ":" + "/usr/lib/jvm/java-8-oracle/jre/lib/jce.jar" + ":" + inBase.getAbsolutePath(), "-ice", "-allow-phantom-refs", "-f", "dava", cl.replace("/", "."), "-d", outputDir.getAbsolutePath()};
            //System.out.println("args: " + Arrays.stream(args).collect(Collectors.joining(", ")));

            //Main.main(args);
            // Main.v().run(args);

            ProcessBuilder pb = new ProcessBuilder("java",
                    "-jar", "lib/soot-3.3.0-jar-with-dependencies.jar",
                    "-cp", "/usr/lib/jvm/java-8-oracle/jre/lib/rt.jar" +
                    ":" + "/usr/lib/jvm/java-8-oracle/jre/lib/jce.jar" +
                    ":" + inBase.getAbsolutePath(),
                    "-ice", "-allow-phantom-refs",
                    "-f", "dava", cl.replace("/", "."),
                    "-d", "tmp"
            );

            Process p = pb.start();
            int r =  p.waitFor();
<fim_suffix>
            FileUtils.moveFile(new File("tmp/dava/src/" + cl + ".java"), new File(outputDir, cl + ".java"));
            if(new File("tmp/dava").exists()) FileUtils.deleteQuietly(new File("tmp/dava"));

            return r==0;
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println(e.getMessage());
        }
        return false;
    }

    @Override
    public String getName() {
        return "Dava-3.3.0";
    }
}
<fim_middle>