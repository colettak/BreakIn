package com.colettak.breakin;

import java.util.Collections;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import org.anddev.andengine.engine.Engine;
import org.anddev.andengine.engine.camera.ZoomCamera;
import org.anddev.andengine.engine.handler.IUpdateHandler;
import org.anddev.andengine.engine.options.EngineOptions;
import org.anddev.andengine.engine.options.EngineOptions.ScreenOrientation;
import org.anddev.andengine.engine.options.resolutionpolicy.RatioResolutionPolicy;
import org.anddev.andengine.entity.primitive.Rectangle;
import org.anddev.andengine.entity.scene.Scene;
import org.anddev.andengine.entity.scene.Scene.IOnAreaTouchListener;
import org.anddev.andengine.entity.scene.Scene.ITouchArea;
import org.anddev.andengine.entity.scene.background.ColorBackground;
import org.anddev.andengine.entity.shape.Shape;
import org.anddev.andengine.entity.util.FPSLogger;
import org.anddev.andengine.extension.physics.box2d.PhysicsConnector;
import org.anddev.andengine.extension.physics.box2d.PhysicsFactory;
import org.anddev.andengine.extension.physics.box2d.PhysicsWorld;
import org.anddev.andengine.extension.physics.box2d.util.Vector2Pool;
import org.anddev.andengine.input.touch.TouchEvent;
import org.anddev.andengine.opengl.texture.Texture;
import org.anddev.andengine.opengl.texture.TextureOptions;
import org.anddev.andengine.opengl.texture.region.TextureRegionFactory;
import org.anddev.andengine.opengl.texture.region.TiledTextureRegion;
import org.anddev.andengine.ui.activity.BaseGameActivity;

import android.hardware.SensorManager;
import android.util.Log;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.badlogic.gdx.physics.box2d.Contact;
import com.badlogic.gdx.physics.box2d.ContactListener;
import com.badlogic.gdx.physics.box2d.FixtureDef;

public class Main extends BaseGameActivity implements IOnAreaTouchListener{

//===========================================================
//Constants
//===========================================================
  static final int CAMERA_WIDTH = 1280;
  static final int CAMERA_HEIGHT = 750;
  private static final String TAG = "AndEngineTest";
  private static final float ANGLE_CONSTANT = 90;
  private static final FixtureDef BALL_DEF = PhysicsFactory.createFixtureDef(1f, .5f, .1f);
  private static final FixtureDef FIXTURE_DEF = PhysicsFactory.createFixtureDef(0, .5f, .5f);
  private static final float MAX_VELOCITY = 30.0f;
  
  
//===========================================================
//Fields
//===========================================================
  private ZoomCamera mCamera;
  private Texture mTexture;
  private TiledTextureRegion mPegTextureRegion;
  private TiledTextureRegion mPlayerTextureRegion;
  private TiledTextureRegion mPlayerTouchRegion;
  //private SurfaceScrollDetector mScrollDetector;
  //private CatapultDetector mCatapultDetector;
  private GameSprite mActivePlayer;
  private GameSprite mTouchArea;
  private PhysicsWorld mPhysicsWorld;
  private Rectangle goalBox;
  public float mFirstX;
  public float mFirstY;
  public int numBalls = 10;
  public int totalPegs;
  final Set<GameSprite> mtoRemoved = Collections.synchronizedSet(new HashSet<GameSprite>());
  public boolean shotOver = false;
  
//================
//===========================================
//Constructors
//===========================================================

//===========================================================
//Getter &amp; Setter
//===========================================================

//==========================================================
//Methods for/from SuperClass/Interfaces
//===========================================================

  
  
  @Override
  public Engine onLoadEngine() {
    this.mCamera = new ZoomCamera(0, 0, CAMERA_WIDTH, CAMERA_HEIGHT);
    final int alturaTotal = CAMERA_HEIGHT*3;
    this.mCamera.setBounds(0, CAMERA_WIDTH, 0, alturaTotal);
    this.mCamera.setBoundsEnabled(true);
    final EngineOptions engineOptions = new EngineOptions(true, ScreenOrientation.LANDSCAPE, new RatioResolutionPolicy(CAMERA_WIDTH, CAMERA_HEIGHT), this.mCamera);
    engineOptions.getTouchOptions().setRunOnUpdateThread(true);
    return new Engine(engineOptions);
  
  }

  @Override
  public void onLoadResources() {
    this.mTexture = new Texture(256,256, TextureOptions.BILINEAR_PREMULTIPLYALPHA);
    this.mPlayerTextureRegion = TextureRegionFactory.createTiledFromAsset(this.mTexture, this, "gfx/ball.png", 0, 0, 1, 1);
    this.mPegTextureRegion = TextureRegionFactory.createTiledFromAsset(this.mTexture, this, "gfx/rectanglepegs.png", 128, 0, 2, 3);
    this.mPlayerTouchRegion = TextureRegionFactory.createTiledFromAsset(this.mTexture, this, "gfx/touchbox.png", 0, 33, 1, 1);

    this.mEngine.getTextureManager().loadTexture(this.mTexture);
  }

  @Override
  public Scene onLoadScene() {
    this.mEngine.registerUpdateHandler(new FPSLogger());
    
    final Scene scene = new Scene(1);
    scene.setBackground(new ColorBackground(0f,0f,0f));
    scene.setOnAreaTouchTraversalFrontToBack();
 
    //this.mPhysicsWorld = new PhysicsWorld(new Vector2(0, SensorManager.GRAVITY_EARTH), true);
    this.mPhysicsWorld = new MaxStepPhysicsWorld(30, new Vector2(0, SensorManager.GRAVITY_EARTH), true);

    final Shape ground = new Rectangle(0, CAMERA_HEIGHT - 2, CAMERA_WIDTH, 2);
    final Shape roof = new Rectangle(0, 0, CAMERA_WIDTH, 2);
    final Shape left = new Rectangle(0, 0, 2, CAMERA_HEIGHT);
    final Shape right = new Rectangle(CAMERA_WIDTH - 2, 0, 2, CAMERA_HEIGHT);
    final FixtureDef wallFixtureDef = PhysicsFactory.createFixtureDef(0f, 0.5f, 0.5f);
    //PhysicsFactory.createBoxBody(this.mPhysicsWorld, ground, BodyType.StaticBody, wallFixtureDef);
    PhysicsFactory.createBoxBody(this.mPhysicsWorld, roof, BodyType.StaticBody, wallFixtureDef);
    PhysicsFactory.createBoxBody(this.mPhysicsWorld, left, BodyType.StaticBody, wallFixtureDef);
    PhysicsFactory.createBoxBody(this.mPhysicsWorld, right, BodyType.StaticBody, wallFixtureDef);

    //scene.getFirstChild().attachChild(ground);
    scene.getFirstChild().attachChild(roof);
    scene.getFirstChild().attachChild(left);
    scene.getFirstChild().attachChild(right);

    this.mPhysicsWorld.setContactListener(new ContactListener(){
      
      @Override
      public void beginContact(final Contact pContact) {
        Main.this.BallCollisions(pContact.getFixtureA().getBody(), pContact.getFixtureB().getBody());

      }

      @Override
      public void endContact(Contact pContact) {

      }

});
    
    scene.registerUpdateHandler(this.mPhysicsWorld);
    scene.registerUpdateHandler(this.getCollisionUpdateHandler());
    scene.setTouchAreaBindingEnabled(true);
    scene.setOnAreaTouchListener(this);

    return scene;
  }

  @Override
  public void onLoadComplete() {
    createPlayer();
    createBox();
    createPegs();

  }
    /** Called when the activity is first created. */

//===========================================================
//Methods
//===========================================================

  private void createPlayer() {
    final Scene scene = this.mEngine.getScene();
    final GameSprite sprite = new GameSprite(((CAMERA_WIDTH - this.mPlayerTextureRegion.getWidth())/2),100,this.mPlayerTextureRegion, SpriteType.GAMEBALL);
    final GameSprite touchbox = new GameSprite(((CAMERA_WIDTH - this.mPlayerTouchRegion.getWidth())/2),50,this.mPlayerTouchRegion, SpriteType.GAMEBALL);
    scene.registerTouchArea(touchbox);
    scene.getLastChild().attachChild(touchbox);
    scene.getLastChild().attachChild(sprite);
    this.mActivePlayer = sprite;
    this.mTouchArea = touchbox;
    
  }
  
  private void startPhysics() {
        final GameSprite sprite = this.mActivePlayer;
        
        final Body body = PhysicsFactory.createCircleBody(this.mPhysicsWorld, sprite, BodyType.DynamicBody, BALL_DEF);
        //body.setFixedRotation(true);
        body.setUserData(sprite);
        this.mPhysicsWorld.registerPhysicsConnector(new PhysicsConnector(sprite, body, true, true));
   
  }
  
  private void createBox() {
    final Scene scene = this.mEngine.getScene();
    this.goalBox = new Rectangle(0, 750, 1280, 32);
    final Rectangle winRectangle = this.goalBox;
    scene.getLastChild().attachChild(this.goalBox);
    final Body body = PhysicsFactory.createBoxBody(this.mPhysicsWorld, winRectangle, BodyType.KinematicBody, FIXTURE_DEF);
    body.setUserData(winRectangle);
    this.mPhysicsWorld.registerPhysicsConnector(new PhysicsConnector(winRectangle, body, true, true));
  }
  

  private void createPegs(){
    int starty = 250;
    int startx = 100;
    Main.this.totalPegs = 20;

    Random generator = new Random();
    final Scene scene = this.mEngine.getScene();
    for (int i = 0; i < Main.this.totalPegs; i++){
       GameSprite peg = new GameSprite((startx + generator.nextInt(30)*40), starty + generator.nextInt(15)*20, this.mPegTextureRegion.clone(), SpriteType.ROUNDPEG );
       scene.getLastChild().attachChild(peg);
       final Body body = PhysicsFactory.createBoxBody(this.mPhysicsWorld, peg, BodyType.StaticBody, FIXTURE_DEF);
       body.setUserData(peg);
       this.mPhysicsWorld.registerPhysicsConnector(new PhysicsConnector(peg, body, true,true));
    }
    
  }
  
//===========================================================
//Inner and Anonymous Classes
//=========================================================== 

    @Override
    public boolean onAreaTouched(TouchEvent pSceneTouchEvent, ITouchArea pTouchArea, 
        float pTouchAreaLocalX, float pTouchAreaLocalY) {
         
        final float touchX = pSceneTouchEvent.getX();
        final float touchY = pSceneTouchEvent.getY();
        final GameSprite player = this.mActivePlayer;

        //normalize values first!
        float currentx = pTouchAreaLocalX - 64 - player.getWidth() / 2;
        float currenty =  64 - pTouchAreaLocalY - player.getHeight() / 2;
        
        if (Math.sqrt(currentx*currentx + currenty*currenty) > (this.mTouchArea.getWidth() / 2)){
        //if touch outside circle,
          //intersection of line segment from origin to point touching and the touch circle
        }
        else{
        //touch inside circle
          player.setPosition((pSceneTouchEvent.getX() - player.getWidth() / 2), (pSceneTouchEvent.getY() - player.getHeight() / 2));
        }

        if(pSceneTouchEvent.isActionDown()) {
          this.mFirstX = touchX;
          this.mFirstY = touchY;
          return true;
        }
        else if(pSceneTouchEvent.isActionUp()){
          
        //turn on physics    
        //CHANGE THIS PART TO USE THE LOCAL X AND Y BASED OFF CENTER POSITION OF BALL
        startPhysics();
        this.mEngine.getScene().unregisterTouchArea(this.mTouchArea);

        float startx = player.getInitialX();
        float starty = player.getInitialY();
        float xspeed = (float) ((startx - currentx)*.5);
        float yspeed = (float) ((starty - currenty)*.5);
        final Body playerBody = this.mPhysicsWorld.getPhysicsConnectorManager().findBodyByShape(player);

        final Vector2 velocity = Vector2Pool.obtain(xspeed, yspeed);
        playerBody.setLinearVelocity(velocity);
        Vector2Pool.recycle(velocity);
        this.mEngine.getScene().unregisterTouchArea(this.mActivePlayer);
        return true;
        }
    
        return true;
    }
  
    public void BallCollisions(Body a, Body b){
      
      if (a.getUserData() instanceof GameSprite && b.getUserData() instanceof GameSprite){
        //if ball a touches a peg
        if(((GameSprite) a.getUserData()).getSpriteType() == SpriteType.GAMEBALL && ((GameSprite) b.getUserData()).getSpriteType() == SpriteType.ROUNDPEG){
          Log.d(TAG, "Collision of peg and ball detected!");
          this.mtoRemoved.add((GameSprite)b.getUserData());
          ((GameSprite) b.getUserData()).animate(60, false);

        }
        //if ball b touches a peg
        else if(((GameSprite) b.getUserData()).getSpriteType() == SpriteType.GAMEBALL && ((GameSprite) a.getUserData()).getSpriteType() == SpriteType.ROUNDPEG){
          Log.d(TAG, "Collision of peg and ball detected!");
          this.mtoRemoved.add((GameSprite)a.getUserData());
          long[] aniArray = {0,120,60,40,40,40};
          ((GameSprite) a.getUserData()).animate(aniArray, false);
        }
      }
      //EVERYTHING INSTANCE OF SHAPE
      else {
        Log.d(TAG, "Collision of box and ball detected!");

        //if ball b touches the goalbox
        if (a.getUserData() instanceof Rectangle && b.getUserData() instanceof GameSprite){
          this.mtoRemoved.add((GameSprite)b.getUserData() );
          this.shotOver = true;
        }
        //if ball a touches the goalbox
        else if (b.getUserData() instanceof Rectangle && a.getUserData() instanceof GameSprite ){
          this.mtoRemoved.add((GameSprite)a.getUserData() );
          this.shotOver = true;
        }
      }
    }
    public void removePhysicsObject(){
      
      
    }
    public void endBall(){
      //if ball is set for deletion 
      System.out.println(Main.this.mtoRemoved.toString());

      for (GameSprite face : Main.this.mtoRemoved){
        
        Log.d(TAG, "GOT inside update handler, TBR: " + face.toString());
        
        final Scene scene = Main.this.mEngine.getScene();
        final Body body = Main.this.mPhysicsWorld.getPhysicsConnectorManager().findBodyByShape(face);
        final PhysicsConnector facePhysicsConnector = Main.this.mPhysicsWorld.getPhysicsConnectorManager().findPhysicsConnectorByShape(face);
        if (((GameSprite) body.getUserData()).getSpriteType() == SpriteType.ROUNDPEG){
          this.totalPegs--;
        }
        
        Main.this.mPhysicsWorld.unregisterPhysicsConnector(facePhysicsConnector);
        Main.this.mPhysicsWorld.destroyBody(body);

        scene.unregisterTouchArea(face);
        scene.getLastChild().detachChild(face);

      }
      Main.this.mtoRemoved.clear();
      


      
    
    }
    public IUpdateHandler getCollisionUpdateHandler(){
      return new IUpdateHandler(){

              @Override
              public void onUpdate(float pSecondsElapsed) {
                   if (Main.this.shotOver == true){
                     Main.this.endBall();
                     //if you still have balls left
                     if (Main.this.numBalls > 0){
                       //if no more pegs left to hit
                       if (Main.this.totalPegs == 0){
                         Log.d(TAG, "YOU WIN!!");
                         //gameover / quit
                       }
                       Main.this.createPlayer();
                       Main.this.shotOver = false;
                       Main.this.numBalls -= 1;
                     }
                     else {
                       //if no more balls left
                       if (Main.this.totalPegs == 0){
                       //win scenario. plus 5 for num balls.
                       Log.d(TAG, "You won! Good Job!");

                       }
                       else{
                       //lose scenario
                       Log.d(TAG, "GAME OVER");
                       }
                     }
                     
                   }
                        
              }

              @Override
              public void reset() {
                // TODO Auto-generated method stub
                
              }};
              }
    
    
    
        
} 