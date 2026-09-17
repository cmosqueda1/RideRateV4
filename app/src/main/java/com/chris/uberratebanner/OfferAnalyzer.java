package com.chris.uberratebanner;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import java.util.*;

public class OfferAnalyzer {
    public interface Callback{void onSuccess(OfferParser.ParsedOffer offer);void onFailure(String message);}
    private final Context context; private final TextRecognizer recognizer=TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
    public OfferAnalyzer(Context c){context=c.getApplicationContext();}
    public void analyze(Bitmap bitmap,Callback cb){
        recognizer.process(InputImage.fromBitmap(bitmap,0)).addOnSuccessListener(text->{
            List<OfferParser.Line> lines=new ArrayList<>();
            for(Text.TextBlock b:text.getTextBlocks())for(Text.Line l:b.getLines()){Rect box=l.getBoundingBox();lines.add(new OfferParser.Line(l.getText(),box));}
            OfferParser.ParsedOffer p=OfferParser.parseOffer(context,lines,bitmap.getWidth(),bitmap.getHeight());
            if(p==null)cb.onFailure("Could not identify a valid Uber ride or delivery offer."); else cb.onSuccess(p);
        }).addOnFailureListener(e->cb.onFailure("OCR failed: "+e.getMessage()));
    }
    public void close(){try{recognizer.close();}catch(Exception ignored){}}
}
