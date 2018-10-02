package ro7ti.com.br.read4u;


import android.util.Log;
import android.util.SparseArray;
import ro7ti.com.br.read4u.ui.camera.GraphicOverlay;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.text.TextBlock;
import com.google.cloud.vision.v1.AnnotateImageRequest;
import com.google.cloud.vision.v1.AnnotateImageResponse;
import com.google.cloud.vision.v1.BatchAnnotateImagesResponse;
import com.google.cloud.vision.v1.EntityAnnotation;
import com.google.cloud.vision.v1.Feature;
import com.google.cloud.vision.v1.Image;
import com.google.cloud.vision.v1.ImageAnnotatorClient;
import com.google.protobuf.ByteString;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


/**
 * A very simple Processor which receives detected TextBlocks and adds them to the overlay
 * as OcrGraphics.
 */
public class OcrDetectorProcessor implements Detector.Processor<TextBlock> {

    private GraphicOverlay<OcrGraphic> mGraphicOverlay;
    private OcrCaptureActivity ocrCapture;

    OcrDetectorProcessor(GraphicOverlay<OcrGraphic> ocrGraphicOverlay, OcrCaptureActivity ocrCaptureParm) {
        mGraphicOverlay = ocrGraphicOverlay;
        ocrCapture = ocrCaptureParm;
    }

    private Integer cont = 0;
    private Integer times = 0;
    private List<String> lista = new ArrayList<String>();

    /**
     * Called by the detector to deliver detection results.
     * If your application called for it, this could be a place to check for
     * equivalent detections by tracking TextBlocks that are similar in location and content from
     * previous frames, or reduce noise by eliminating TextBlocks that have not persisted through
     * multiple detections.
     */
    @Override
    public void receiveDetections(Detector.Detections<TextBlock> detections) {
        mGraphicOverlay.clear();
        SparseArray<TextBlock> items = detections.getDetectedItems();
        if (items.size() > 0) {
            if(items.size() > cont) {
                for (int i = 0; i < items.size(); ++i) {
                    TextBlock item = items.valueAt(i);

                    if(!lista.contains(item.getValue()))
                    {
                        lista.add(item.getValue());
                    }
                }
                cont = items.size();
            }

            if(this.times < 5){
                this.times++;
                this.ocrCapture.startCameraSource();
            }
            else if(this.times == 5)
            {
                String texto = "";
                for (int i = 0; i < lista.size(); ++i) {
                    texto += lista.get(i) + "\n";
                }
                cont = 0;
                this.times = 0;
                lista.clear();
                this.ocrCapture.print(texto);
            }
        }
        else
        {
            this.times = 0;
            this.ocrCapture.startCameraSource();
        }
    }

    public static void detectText(ByteArrayInputStream byteStram) throws Exception, IOException {
        List<AnnotateImageRequest> requests = new ArrayList<>();

        ByteString imgBytes = ByteString.readFrom(byteStram);

        Image img = Image.newBuilder().setContent(imgBytes).build();
        Feature feat = Feature.newBuilder().setType(Feature.Type.TEXT_DETECTION).build();
        AnnotateImageRequest request =
                AnnotateImageRequest.newBuilder().addFeatures(feat).setImage(img).build();
        requests.add(request);

        try (ImageAnnotatorClient client = ImageAnnotatorClient.create()) {
            BatchAnnotateImagesResponse response = client.batchAnnotateImages(requests);
            List<AnnotateImageResponse> responses = response.getResponsesList();

            for (AnnotateImageResponse res : responses) {
                if (res.hasError()) {
                    Log.d("Erro", res.getError().getMessage());
                    return;
                }

                // For full list of available annotations, see http://g.co/cloud/vision/docs
                for (EntityAnnotation annotation : res.getTextAnnotationsList()) {
                    Log.d("Texto: ", annotation.getDescription());
                    Log.d("Position : ", annotation.getBoundingPoly().toString());
                }
            }
        }
    }

    /**
     * Frees the resources associated with this detection processor.
     */
    @Override
    public void release() {
        mGraphicOverlay.clear();
    }
}
