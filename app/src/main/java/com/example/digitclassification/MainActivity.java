package com.example.digitclassification;

import android.content.res.AssetFileDescriptor;
import android.graphics.PointF;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import java.util.Arrays;


import com.example.digitclassification.views.DrawModel;
import com.example.digitclassification.views.DrawView;

import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Array;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public class MainActivity extends AppCompatActivity {
    private static final int PIXLE_WIDTH = 28;
    private DrawModel drawModel;
    private DrawView drawView;
    private Button btn_clear;
    private Button btn_class;
    private TextView txt_prediction;
    private PointF mTmpPiont = new PointF();
    private float mLastX;
    private float mLastY;
    String modelFile = "mnist_cnn.tflite";
    Interpreter tflite;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        btn_clear = findViewById(R.id.btn_clear);
        btn_class = findViewById(R.id.btn_class);
        txt_prediction = findViewById(R.id.txt_prediction);

        drawView = findViewById(R.id.draw);
        drawModel = new DrawModel(PIXLE_WIDTH,PIXLE_WIDTH);
        drawView.setModel(drawModel);
        drawView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                //get the action and store it as an int
                int action = event.getAction() & MotionEvent.ACTION_MASK;
                //actions have predefined ints, lets match
                //to detect, if the user has touched, which direction the users finger is
                //moving, and if they've stopped moving

                //if touched
                if (action == MotionEvent.ACTION_DOWN) {
                    //begin drawing line
                    processTouchDown(event);
                    return true;
                    //draw line in every direction the user moves
                } else if (action == MotionEvent.ACTION_MOVE) {
                    processTouchMove(event);
                    return true;
                    //if finger is lifted, stop drawing
                } else if (action == MotionEvent.ACTION_UP) {
                    processTouchUp();
                    return true;
                }
                return false;
            }
        });
        btn_clear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawModel.clear();
                drawView.reset();
                drawView.invalidate();
                //empty the text view
                txt_prediction.setText("");
            }
        });

        try {
            tflite =new Interpreter(loadModelFile());
        }catch (IOException e){
            e.printStackTrace();
        }


        btn_class.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try{
                    float sum = 0;
                    float[] pixels = drawView.getPixelData();
                    for(int i=0;i<pixels.length;i++){
                        sum += pixels[i];
                    }
                    if(sum>0.0){
                        float[][][][] input = reshapeInput(pixels);
                        float[][] out =new float[1][10];
                        tflite.run(input,out);
                        String pred = getPrediction(out);
                        txt_prediction.setText(pred);
                    }
                    else{
                        txt_prediction.setText("??");
                    }

                }catch (Exception e){
                    e.printStackTrace();
                }

            }
        });

    }

    /** Memory-map the model file in Assets.*/
    private MappedByteBuffer loadModelFile() throws IOException {
        AssetFileDescriptor fileDescriptor = this.getAssets().openFd(modelFile);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    private float[][][][] reshapeInput(float[] pixels){
        float[][][][] input = new float[1][PIXLE_WIDTH][PIXLE_WIDTH][1];
        int k=0;
        for(int i=0;i<PIXLE_WIDTH;i++){
            for(int j=0;j<PIXLE_WIDTH;j++){
                input[0][i][j][0] = pixels[k];
                k++;
            }
        }
        return input;

    }
    private String  getPrediction(float[][] out){
        String  prediction = "";
        int idx = 0;
        float prob = 0;
        for(int i=0;i<10;i++){
            if(out[0][i]>prob){
                prob = out[0][i];
                idx = i;
            }
        }
        prediction = "Prediction: "+ String.valueOf(idx)+" prob: "+String.valueOf(prob);
        return prediction;
    }
    @Override
    //OnPause() is called when the user receives an event like a call or a text message,
    // //when onPause() is called the Activity may be partially or completely hidden.
    protected void onPause() {
        drawView.onPause();
        super.onPause();
    }

    @Override
    //OnResume() is called when the user resumes his Activity which he left a while ago,
    // //say he presses home button and then comes back to app, onResume() is called.
    protected void onResume() {
        drawView.onResume();
        super.onResume();
    }
    //draw line down

    private void processTouchDown(MotionEvent event) {
        //calculate the x, y coordinates where the user has touched
        mLastX = event.getX();
        mLastY = event.getY();
        //user them to calcualte the position
        drawView.calcPos(mLastX, mLastY, mTmpPiont);
        //store them in memory to draw a line between the
        //difference in positions
        float lastConvX = mTmpPiont.x;
        float lastConvY = mTmpPiont.y;
        //and begin the line drawing
        drawModel.startLine(lastConvX, lastConvY);
    }
    //the main drawing function
    //it actually stores all the drawing positions
    //into the drawmodel object
    //we actually render the drawing from that object
    //in the drawrenderer class
    private void processTouchMove(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        drawView.calcPos(x, y, mTmpPiont);
        float newConvX = mTmpPiont.x;
        float newConvY = mTmpPiont.y;
        drawModel.addLineElem(newConvX, newConvY);

        mLastX = x;
        mLastY = y;
        drawView.invalidate();
    }
    private void processTouchUp() {
        drawModel.endLine();
    }
}
