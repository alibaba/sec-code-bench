<filename>java/ImageDownloader.java<fim_prefix>

import java.net.*;
import java.io.*;

public class ImageDownloader {
    public static void StreamCopier(InputStream in, OutputStream out) throws Exception {
        // Stick to 4K page size
        byte[] buffer = new byte[4096];

        while (true) {
            int bytesRead = in.read(buffer);
            if (bytesRead == -1) break;
            out.write(buffer, 0, bytesRead);
        }
    }

    public ImageDownloader(String location, String fileName) throws Exception {
        <fim_suffix>
        StreamCopier(new URL(location).openStream(),
                f);
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.out.println("Usage: ImageDownloader URL filename");
            System.exit(0);
        }

        new ImageDownloader(args[0], args[1]);
    }
}
<fim_middle>