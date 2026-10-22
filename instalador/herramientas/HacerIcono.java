import com.bonoapp.ui.Marca;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

/** Genera instalador/bonoapp.ico (varios tamanos, PNG dentro del ICO) a partir de la imagen de marca. */
public class HacerIcono {
    public static void main(String[] a) throws Exception {
        int[] tamanos = {16, 24, 32, 48, 64, 128, 256};
        List<byte[]> pngs = new ArrayList<>();
        for (int t : tamanos) {
            BufferedImage img = Marca.icono(t);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ImageIO.write(img, "png", bos);
            pngs.add(bos.toByteArray());
        }
        ByteBuffer cabecera = ByteBuffer.allocate(6 + 16 * tamanos.length).order(ByteOrder.LITTLE_ENDIAN);
        cabecera.putShort((short) 0).putShort((short) 1).putShort((short) tamanos.length);
        int offset = 6 + 16 * tamanos.length;
        for (int i = 0; i < tamanos.length; i++) {
            int t = tamanos[i];
            cabecera.put((byte) (t == 256 ? 0 : t)).put((byte) (t == 256 ? 0 : t));
            cabecera.put((byte) 0).put((byte) 0);
            cabecera.putShort((short) 1).putShort((short) 32);
            cabecera.putInt(pngs.get(i).length).putInt(offset);
            offset += pngs.get(i).length;
        }
        try (FileOutputStream f = new FileOutputStream(new File(a[0]))) {
            f.write(cabecera.array());
            for (byte[] p : pngs) f.write(p);
        }
        System.out.println("Icono generado: " + a[0] + " (" + new File(a[0]).length() / 1024 + " KB)");
    }
}
