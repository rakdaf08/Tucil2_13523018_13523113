import javax.imageio.*;
import javax.imageio.metadata.*;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.*;
import java.io.*;

public class GifWriter {
    private ImageWriter writer;
    private ImageWriteParam param;
    private IIOMetadata metadata;
    private ImageOutputStream stream;

    public GifWriter(ImageOutputStream stream, int imageType, int delayTimeCS, boolean loop) throws IOException {
        this.stream = stream;
        writer = ImageIO.getImageWritersByFormatName("gif").next();
        param = writer.getDefaultWriteParam();
        ImageTypeSpecifier typeSpecifier = ImageTypeSpecifier.createFromBufferedImageType(imageType);
        metadata = writer.getDefaultImageMetadata(typeSpecifier, param);

        configureRootMetadata(delayTimeCS, loop);
        writer.setOutput(stream);
        writer.prepareWriteSequence(null);
    }

    private void configureRootMetadata(int delayTimeCS, boolean loop) throws IIOInvalidTreeException {
        String metaFormat = metadata.getNativeMetadataFormatName();
        IIOMetadataNode root = new IIOMetadataNode(metaFormat);

        IIOMetadataNode gce = new IIOMetadataNode("GraphicControlExtension");
        gce.setAttribute("disposalMethod", "none");
        gce.setAttribute("userInputFlag", "FALSE");
        gce.setAttribute("transparentColorFlag", "FALSE");
        gce.setAttribute("delayTime", Integer.toString(delayTimeCS));  // <-- penting
        gce.setAttribute("transparentColorIndex", "0");
        root.appendChild(gce);

        IIOMetadataNode appExtensions = new IIOMetadataNode("ApplicationExtensions");
        IIOMetadataNode appNode = new IIOMetadataNode("ApplicationExtension");

        appNode.setAttribute("applicationID", "NETSCAPE");
        appNode.setAttribute("authenticationCode", "2.0");

        byte[] loopBytes = new byte[]{1, (byte) (loop ? 0 : 1), 0};
        appNode.setUserObject(loopBytes);
        appExtensions.appendChild(appNode);
        root.appendChild(appExtensions);

        metadata.setFromTree(metaFormat, root);
    }

    public void writeToSequence(RenderedImage img, int delayTimeCS) throws IOException {
        IIOMetadata frameMetadata = writer.getDefaultImageMetadata(
                ImageTypeSpecifier.createFromRenderedImage(img), param);

        String metaFormat = frameMetadata.getNativeMetadataFormatName();
        IIOMetadataNode root = new IIOMetadataNode(metaFormat);

        IIOMetadataNode gce = new IIOMetadataNode("GraphicControlExtension");
        gce.setAttribute("disposalMethod", "none");
        gce.setAttribute("userInputFlag", "FALSE");
        gce.setAttribute("transparentColorFlag", "FALSE");
        gce.setAttribute("delayTime", Integer.toString(delayTimeCS));
        gce.setAttribute("transparentColorIndex", "0");
        root.appendChild(gce);

        frameMetadata.mergeTree(metaFormat, root);
        writer.writeToSequence(new IIOImage(img, null, frameMetadata), param);
    }

    public void close() throws IOException {
        writer.endWriteSequence();
        stream.close();
    }
}
