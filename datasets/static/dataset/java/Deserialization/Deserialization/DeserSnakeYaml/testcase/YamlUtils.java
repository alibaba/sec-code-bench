<filename>src/main/java/com/actualplayer/rememberme/util/YamlUtils.java<fim_prefix>

package com.actualplayer.rememberme.util;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.constructor.CustomClassLoaderConstructor;

import java.io.*;

public class YamlUtils {

    public static <T> T readFile(File file, Class<T> clazz) throws FileNotFoundException {
        <fim_suffix>
        InputStream stream = new FileInputStream(file);

        return yaml.loadAs(stream, clazz);
    }

    public static void writeFile(File file, Object object) throws IOException {
        DumperOptions options = new DumperOptions();
        options.setIndent(2);
        options.setPrettyFlow(true);
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);

        Yaml yaml = new Yaml(options);

        yaml.dump(object, new FileWriter(file));
    }
}
<fim_middle>