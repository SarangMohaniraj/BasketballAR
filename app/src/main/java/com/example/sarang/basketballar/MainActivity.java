package com.example.sarang.basketballar;

import android.content.Context;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.widget.Toast;

import com.google.ar.core.Anchor;
import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;
import com.google.ar.core.Pose;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.HitTestResult;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.collision.Box;
import com.google.ar.sceneform.collision.Sphere;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.ar.sceneform.ux.TransformableNode;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;


public class MainActivity extends AppCompatActivity{

    private ArFragment arFragment;
    private ModelRenderable basketballRenderable;
    private TransformableNode basketball;
    private ModelRenderable hoopRenderable;
    private TransformableNode hoop;
    private Anchor hoopAnchor;
    int objects = 0;
    private AnchorNode anchorNode;
    private Anchor anchor;
    GameThread gameThread;
    BouncingThread bouncingThread;
    ShootingThread shootingThread;
    private volatile boolean bouncing = false;
    private volatile boolean game = true;
    private volatile boolean shooting = false;
    float ballDy = .08f;
    float velocity = .0815f;
    Vector3 hoopHeight;
    long timeStart;
    double timeElapsed;
    float floor;
    double angle;
    float xDist;
    float yDist;
    float zDist;
    float dist;
    double xAngle;
    double yAngle;
    double zAngle;
    int ballsShot = 0;
    ArrayList<Double> angles = new ArrayList<>();
    ArrayList<String> distances = new ArrayList<>();



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        arFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.ux_fragment);


        ModelRenderable.builder()
                .setSource(this, R.raw.hoop)
                .build()
                .thenAccept(renderable -> hoopRenderable = renderable)
                .exceptionally(
                        throwable -> {
                            Toast toast =
                                    Toast.makeText(this, "Unable to load hoop renderable", Toast.LENGTH_LONG);
                            toast.setGravity(Gravity.CENTER, 0, 0);
                            toast.show();
                            return null;
                        });

        ModelRenderable.builder()
                .setSource(this, R.raw.basketball)
                .build()
                .thenAccept(renderable -> basketballRenderable = renderable)
                .exceptionally(
                        throwable -> {
                            Toast toast =
                                    Toast.makeText(this, "Unable to load basketball renderable", Toast.LENGTH_LONG);
                            toast.setGravity(Gravity.CENTER, 0, 0);
                            toast.show();
                            return null;
                        });

        arFragment.setOnTapArPlaneListener(
                (HitResult hitResult, Plane plane, MotionEvent motionEvent) -> {
                    if (basketballRenderable == null)
                        return;

                    if(objects == 0){
                        hoopAnchor = hitResult.createAnchor();

                        AnchorNode hoopAnchorNode = new AnchorNode(hoopAnchor);
                        hoopAnchorNode.setParent(arFragment.getArSceneView().getScene());

                        // Create the transformable hoop and add it to the anchor.
                        hoop = new TransformableNode(arFragment.getTransformationSystem());
                        hoop.getScaleController().setMinScale(1.5f);
                        hoop.getScaleController().setMaxScale(2f);
                        hoop.setParent(hoopAnchorNode);
                        hoop.setRenderable(hoopRenderable);

                        Box box = (Box) hoop.getRenderable().getCollisionShape();
                        Vector3 renderableSize = box.getSize();
                        hoopHeight = new Vector3(renderableSize.x * hoop.getWorldScale().x,renderableSize.y * hoop.getWorldScale().y,renderableSize.z * hoop.getWorldScale().z);
                        hoop.setCollisionShape(new Box(hoopHeight));
                    }
                    else if(objects == 1) {
                        // Create the Anchor.
                        anchor = hitResult.createAnchor();

                        anchorNode = new AnchorNode(anchor);
                        anchorNode.setParent(arFragment.getArSceneView().getScene());

                        // Create the transformable basketball and add it to the anchor.
                        basketball = new TransformableNode(arFragment.getTransformationSystem());
                        basketball.getScaleController().setMinScale(8f);
                        basketball.getScaleController().setMaxScale(10f);
                        basketball.setParent(anchorNode);
                        basketball.setRenderable(basketballRenderable);
                        basketball.setCollisionShape(new Sphere());
                        floor = basketball.getWorldPosition().y;

                        basketball.setOnTapListener((HitTestResult hitTestResult, MotionEvent touchEvent) -> {
                            if(bouncing) {
                                bouncing = false;
                                shooting = false;
                                return;
                            }
                            shootingThread = new ShootingThread();
                            shooting = true;
                            angle = (Vector3.angleBetweenVectors(basketball.getWorldPosition(),hoopHeight))*Math.PI/180;

                            xDist = (float) Math.sqrt(Math.pow(basketball.getWorldPosition().x - hoopHeight.x,2));
                            yDist = (float) Math.sqrt(Math.pow(basketball.getWorldPosition().y - hoopHeight.y,2));
                            zDist = (float) Math.sqrt(Math.pow(basketball.getWorldPosition().z - hoopHeight.z,2));
                            zDist = (hoopHeight.z - basketball.getWorldPosition().z);
                            dist  = (float) Math.sqrt(Math.pow(basketball.getWorldPosition().x - hoopHeight.x,2) + Math.pow(basketball.getWorldPosition().y - hoopHeight.y,2) + Math.pow(basketball.getWorldPosition().z - hoopHeight.z,2));
                            //Direction cosines
                            xAngle = Math.acos((double)(xDist/dist));
                            yAngle = Math.acos((double)(yDist/dist));
                            zAngle = Math.acos((double)(zDist/dist));

                            ballsShot++;
                            angles.add(angle*180/Math.PI);
                            distances.add(dist+"m");

                            timeStart = System.nanoTime();
                            shootingThread.start();
                        });

                        gameThread = new GameThread();
                        gameThread.start();

                        bouncingThread = new BouncingThread();
//                    bouncingThread.start();

                    bouncing = true;
                    }
                    else{
                        anchor = hitResult.createAnchor();
                        anchorNode = new AnchorNode(anchor);
                        anchorNode.setParent(arFragment.getArSceneView().getScene());
                        basketball.setParent(anchorNode);
                        shooting = false;
                        bouncing = false;
                    }
                    objects++;

                });
    }



    public class BouncingThread extends Thread{
        @Override
        public void run() {
            while (bouncing) {
                super.run();
                try {
                    Thread.sleep(1000/12);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                runOnUiThread(() -> {
                    if(anchor.getPose().ty() > -.2 && ballDy > 0)  //[-1.2,-.2] interval
                        ballDy = -.08f;
                    else if(anchor.getPose().ty() < floor && ballDy < 0)
                        ballDy = .08f;


                    Vector3 position = new Vector3(arFragment.getArSceneView().getArFrame().getCamera().getPose().tx(),anchor.getPose().ty()+ballDy, arFragment.getArSceneView().getArFrame().getCamera().getPose().tz());
                    Pose pose = Pose.makeTranslation(position.x, position.y, position.z);
                    try {
                        anchor = arFragment.getArSceneView().getSession().createAnchor(pose);
                        anchorNode = new AnchorNode(anchor);
                        anchorNode.setParent(arFragment.getArSceneView().getScene());
                        basketball.setParent(anchorNode);
                    }catch(Exception e){}
                });

            }
            ballDy = -.08f;
            while(anchor.getPose().ty() > floor){
                Vector3 position = new Vector3(arFragment.getArSceneView().getArFrame().getCamera().getPose().tx(),anchor.getPose().ty()+ballDy, arFragment.getArSceneView().getArFrame().getCamera().getPose().tz());
                Pose pose = Pose.makeTranslation(position.x, position.y, position.z);
                try {
                    anchor = arFragment.getArSceneView().getSession().createAnchor(pose);
                    anchorNode = new AnchorNode(anchor);
                    anchorNode.setParent(arFragment.getArSceneView().getScene());
                    basketball.setParent(anchorNode);
                }catch(Exception e){}
            }

        }
    }

    public class GameThread extends Thread{
        @Override
        public void run() {
            while (game) {
                super.run();
                try {
                    Thread.sleep(1000/12);
                } catch (Exception e) {
                    e.printStackTrace();
                }


                float distance = (float) Math.sqrt(Math.pow(anchor.getPose().tx() - arFragment.getArSceneView().getArFrame().getCamera().getPose().tx(),2));

                if(!bouncingThread.isAlive() && !shooting && distance <= .15f) {
                    bouncing = true;
                    try {
                        bouncingThread.start();
                    }catch (Exception e){}
                }

            }
        }
    }

    public class ShootingThread extends Thread{
        @Override
        public void run() {
            while (shooting) {
                super.run();
                try {
                    Thread.sleep(1000/12);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                timeElapsed = (double)(System.nanoTime() - timeStart)/1_000_000_000.0;

                runOnUiThread(() -> {

//                    float vx = (float)(velocity*Math.cos(angle));
//                    float vy = (float)(velocity*Math.sin(angle)-.0098*timeElapsed);
                    float vx = (float)(velocity*Math.cos(xAngle));
                    float vy = (float)(velocity*Math.sin(yAngle)-.0098*timeElapsed);
//                    float vz = (float)(velocity*Math.cos(zAngle));
                    float vz = (float)(velocity*Math.cos(angle));

                    Vector3 position = new Vector3(anchor.getPose().tx()+vx,anchor.getPose().ty()+vy, anchor.getPose().tz()+vz);
                    Pose pose = Pose.makeTranslation(position.x, position.y, position.z);
                    try {
                        anchor = arFragment.getArSceneView().getSession().createAnchor(pose);
                        anchorNode = new AnchorNode(anchor);
                        anchorNode.setParent(arFragment.getArSceneView().getScene());
                        basketball.setParent(anchorNode);
                    }catch(Exception e){}

                });
//                try {
//                    Log.d("TAG", (arFragment.getArSceneView().getScene().overlapTest(basketball) == hoop) + "");
//                }catch (Exception e){}
                if(anchor.getPose().ty() < floor)
                    shooting = false;
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        game = false;
        bouncing = false;
        shooting = false;

        try {
            OutputStreamWriter writer = new OutputStreamWriter(openFileOutput("info.json", Context.MODE_PRIVATE));
            JSONArray array = new JSONArray();
            array.put(new JSONObject().put("balls shot",ballsShot));
            array.put(new JSONObject().put("angles",new JSONArray(Arrays.asList(angles))));
            array.put(new JSONObject().put("distances",new JSONArray(Arrays.asList(distances))));
            writer.write(array.toString());
            writer.close();
        } catch (Exception e) {e.printStackTrace();}
    }

    @Override
    protected void onResume() {
        super.onResume();
        game = true;
        if(basketball != null)
            gameThread.start();
    }

}
