package com.example.calculategame;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.microedition.khronos.opengles.GL10;

import android.net.wifi.WpsInfo;
import android.provider.UserDictionary.Words;
import android.util.Log;
import android.widget.Toast;

import com.badlogic.androidgames.framework.Game;
import com.badlogic.androidgames.framework.Input.TouchEvent;
import com.badlogic.androidgames.framework.gl.Animation;
import com.badlogic.androidgames.framework.gl.Camera2D;
import com.badlogic.androidgames.framework.gl.FPSCounter;
import com.badlogic.androidgames.framework.gl.SpriteBatcher;
import com.badlogic.androidgames.framework.gl.TextureRegion;
import com.badlogic.androidgames.framework.impl.GLScreen;
import com.badlogic.androidgames.framework.math.OverlapTester;
import com.badlogic.androidgames.framework.math.Rectangle;
import com.badlogic.androidgames.framework.math.Vector2;
import com.example.calculategame.World.WorldListener;

public class GameScreen extends GLScreen {

	static final int GAME_READY=0;
	static final int GAME_RUNNING=1;
	static final int GAME_PAUSED=2;
	static final int GAME_LEVEL_END=3;
	static final int GAME_OVER=4;
	
	int state;
	Camera2D guiCam;
	Vector2 touchPoint;
	SpriteBatcher batcher;
	World world;
	WorldRenderer renderer;
	Rectangle pauseBounds;
	Rectangle resumeBounds;
	Rectangle quitBounds;
	int lastScore;
	String scoreString;
	FPSCounter fpsCounter;
	
	WorldListener worldListener;
	
	String showProblem="Attack: ";
	String keepMyAnser="";
	public static String keeySpacialQuestion="";
	boolean isNumberClick[] = new boolean[10];
	boolean isOperandClick[] = new boolean[4];
	
	public static ArrayList<NumberButton> NumberButtonList;
	public static ArrayList<NumberButton> OperandButtonList;
	public static AttackButton attackButton,windButton,effect3Button; 
	
	int hpLength=100;
	public static int limitFire = 6,limitWind=3;
	
	public GameScreen(Game game, int whichGame) {
		super(game);
		state = GAME_RUNNING;
		guiCam = new Camera2D(glGraphics,320,480);
		touchPoint = new Vector2();
		batcher = new SpriteBatcher(glGraphics,1000);
		world.GAME_PLAY=whichGame;
		
        pauseBounds = new Rectangle(290, 8, 32, 32);
        resumeBounds = new Rectangle(160 - 96, 240, 192, 36);
        quitBounds = new Rectangle(160 - 96, 240 - 36, 192, 36);
		
        worldListener = new WorldListener() {
			@Override
			public void explode() {
				Assets.playSound(Assets.explode);
			}

			@Override
			public void windEffect() {
				Assets.playSound(Assets.windEffect);				
			}

			@Override
			public void fireEffect() {
				Assets.playSound(Assets.fireEffect);
			}

			@Override
			public void touchBounds() {
				Assets.playSound(Assets.clickSound);
			}
        };
		world = new World(worldListener);
		renderer = new WorldRenderer(glGraphics,batcher,world);
		
		lastScore=0;
		scoreString="score: 0";
		fpsCounter = new FPSCounter();
		
		attackButton = new AttackButton(15, 20);
		windButton = new AttackButton(40,20);
		effect3Button = new AttackButton(65,20);
		this.NumberButtonList = new ArrayList<NumberButton>();
		float x=21.0f,y=460;
		for (int i = 0; i < 10; i++) {
			NumberButton number = new NumberButton(x, y);
			x+=30;
			NumberButtonList.add(number);
		}
		
		OperandButtonList = new ArrayList<NumberButton>();
		x=21.0f;
		y=420;
		for (int i = 0; i < 4; i++) {
			NumberButton operand = new NumberButton(x,y);
			OperandButtonList.add(operand);
			x+=30;
		}
		Arrays.fill(isNumberClick, false);
		Arrays.fill(isOperandClick, false);
		
		
		Assets.music1.stop();
//		Assets.music2.stop();
//		Assets.music3.stop();
		if (Settings.soundEnabled) {
			if (world.GAME_PLAY==1) {
				Assets.music2.play();
				Assets.music2.setLooping(true);
			}
			else{
				Assets.music3.play();
				Assets.music3.setLooping(true);
			}
		}
	}

	@Override
	public void update(float deltaTime) {
		if (deltaTime>0.1f) {
			deltaTime=0.1f;
		}
		
		switch (state) {
		case GAME_READY:
			updateReady();
			break;
		case GAME_RUNNING:
			updateRunning(deltaTime);
			break;
		case GAME_PAUSED:
			updatePaused();
			break;
	    case GAME_LEVEL_END:
	        updateGameOver();
	        break;
		}
	}
	
	
	private void updatePaused() {
	    List<TouchEvent> touchEvents = game.getInput().getTouchEvents();
	    int len = touchEvents.size();
	    for(int i = 0; i < len; i++) {
	        TouchEvent event = touchEvents.get(i);
	        if(event.type != TouchEvent.TOUCH_UP)
	            continue;
	        
	        touchPoint.set(event.x, event.y);
	        guiCam.touchToWorld(touchPoint);
	        
	        if(OverlapTester.pointInRectangle(resumeBounds, touchPoint)) {
	            Assets.playSound(Assets.clickSound);
	            state = GAME_RUNNING;
	            return;
	        }
	        
	        if(OverlapTester.pointInRectangle(quitBounds, touchPoint)) {
	            Assets.playSound(Assets.clickSound);
	            	Settings.addScore(world.score);
	    			Settings.save(game.getFileIO());
	    	        state = GAME_OVER;
	    	        state=GAME_LEVEL_END;
	            
	            return;
	        }
	    }
	}
	
	
	private void updateGameOver() {
		try {
		    List<TouchEvent> touchEvents = game.getInput().getTouchEvents();
		    int len = touchEvents.size();
		    for(int i = 0; i < len; i++) {                   
		        TouchEvent event = touchEvents.get(i);
		        if(event.type == TouchEvent.TOUCH_UP)
		            continue;
		        game.setScreen(new MainMenuScreen(game));
		    }
		} catch (Exception e) {
			Log.d("error", "error = "+e);
		}
	}
	
	private void updateReady(){
		if (game.getInput().getTouchEvents().size()>0) {
			state = GAME_RUNNING;
		}
	}

	boolean windAttackBool=false;
	boolean effect3AttackBool=false;
//	float windAttack=1.0f;
	boolean isDragWindButton=false;
	boolean isAttackButtonClick=false;
	boolean isEffect3Button=false;
	private void updateRunning(float deltaTime) {
		List<TouchEvent> touchEvents = game.getInput().getTouchEvents();
		int len = touchEvents.size();
		for (int i = 0; i < len; i++) {
			TouchEvent event = touchEvents.get(i);
//			if (event.type != TouchEvent.TOUCH_UP) {
//				continue;
//			}
			
			touchPoint.set(event.x,event.y);
			guiCam.touchToWorld(touchPoint);
			
			if (event.type==TouchEvent.TOUCH_DOWN) {
				for (int j = 0; j < 10; j++) {
//					Log.d("show", NumberButtonList.get(0).position.x+":"+NumberButtonList.get(0).position.y+" = "+touchPoint.x+":"+touchPoint.y);
					if (OverlapTester.pointInRectangle(NumberButtonList.get(j).bounds,touchPoint.x+10,touchPoint.y)) {
//						Log.d("click", "click = "+j);
						isNumberClick[j]=true;
						keepMyAnser+=j;
						showProblem+=j;
						Assets.playSound(Assets.clickSound);
					}
				}
				
				for (int j = 0; j < 4; j++) {
					if (OverlapTester.pointInRectangle(OperandButtonList.get(j).bounds,touchPoint.x+10,touchPoint.y)) {
						isOperandClick[j]=true;
						if (j==0) {
							keepMyAnser+="+";
							showProblem+="+";							
						}
						if (j==1) {
							keepMyAnser+="-";
							showProblem+="-";
						}
						if (j==2) {
							keepMyAnser+="x";
							showProblem+="x";
						}
						if (j==3) {
							keepMyAnser+="/";
							showProblem+="/";
						}
						Assets.playSound(Assets.clickSound);
					}
				}
				//Touch Attack
//				Log.d("Food", "Food "+attackButton.position.x+":"+attackButton.position.y+" = "+(touchPoint.x)+" : "+(touchPoint.y-15) );
//				if (OverlapTester.pointInRectangle(attackButton.bounds, touchPoint.x, (touchPoint.y-15)))
				if (OverlapTester.pointInRectangle(attackButton.bounds, touchPoint))
				{						Assets.playSound(Assets.clickSound);
//					world.food.state=world.food.ATTACK;
//					world.food.stateTime=0.6f;
					showProblem="Attack: ";
					isAttackButtonClick=true;
					world.CreateLaserPlayer(keepMyAnser);
					keepMyAnser="";
//					Log.d("Food", "Attack");
				}
				
				
		        if(OverlapTester.pointInRectangle(pauseBounds, touchPoint)) {
		            Assets.playSound(Assets.clickSound);
		            state = GAME_PAUSED;
		            return;
		        }
				
			}
			
			
			if (event.type==TouchEvent.TOUCH_UP) {
				for (int j = 0; j < 10; j++) {
						isNumberClick[j]=false;
				}
				for (int j = 0; j < 4; j++) {
					isOperandClick[j]=false;
				}
				
				isAttackButtonClick=false;
				isDragWindButton=false;
				isEffect3Button=false;

				if (windAttackBool==true) {
					limitWind--;
					world.createWindStromEffect(touchPoint.x,touchPoint.y);
					windAttackBool=false;
				}
				if (effect3AttackBool) {
					limitFire--;
					world.createExplode3Effect(touchPoint.x,touchPoint.y);
					effect3AttackBool=false;
				}
//				hpLength-=5;
			}
			
			
			if (event.type==TouchEvent.TOUCH_DRAGGED) {
//				windAttack+=0.01f;
				if (OverlapTester.pointInRectangle(windButton.bounds, touchPoint.x,touchPoint.y-5) && limitWind>0) {
					isDragWindButton=true;
					windAttackBool=true;
				}
				if (OverlapTester.pointInRectangle(effect3Button.bounds, touchPoint.x,touchPoint.y-5) && limitFire>0) {
					isEffect3Button=true;
					effect3AttackBool=true;
				}
			}
		}
		
		world.update(deltaTime,game.getInput().getAccelX());
//	    if(world.score != lastScore) {
	        lastScore = world.score;
//	    }
		
		for (int i = 0; i < 10; i++) {
			NumberButton number = NumberButtonList.get(i);
			number.update(deltaTime);
		}
		for (int i = 0; i < 4; i++) {
			NumberButton operand = OperandButtonList.get(i);
			operand.update(deltaTime);
		}
	}

	@Override
	public void present(float deltaTime) {
		GL10 gl = glGraphics.getGL();
		gl.glClear(GL10.GL_COLOR_BUFFER_BIT);
		gl.glEnable(GL10.GL_TEXTURE_2D);
		
		renderer.render();
		
		guiCam.setViewportAndMatrices();
		gl.glEnable(GL10.GL_BLEND);
		gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);
		
		renderNumberButton();
		renferOperandButton();
		
		switch (state) {
		case GAME_RUNNING:
			presentRunning();
			break;
		case GAME_READY:
			presentReady();
			break;
		case GAME_PAUSED:
			presentPaused();
			break;
		case GAME_LEVEL_END:
			presentGameOver();
			break;
		}
		gl.glDisable(GL10.GL_BLEND);
//		fpsCounter.logFrame();
	}
	
	
	private void presentGameOver() {
		batcher.beginBatch(Assets.items);
	    batcher.drawSprite(160, 240, 160, 96, Assets.gameOver);        
	    float scoreWidth = Assets.font.glyphWidth * scoreString.length();
	    if (scoreString.contains("new highscore")) 
	    	Assets.font.drawText(batcher, scoreString, 320/2-40, 320,4,10);
	    else
	    	Assets.font.drawText(batcher, scoreString, 320/2-20, 320,4,10);
	    batcher.endBatch();
	}
	
	
	private void presentReady() {
		batcher.beginBatch(Assets.items);
	    batcher.drawSprite(160, 240, 192, 32, Assets.ready);
	    batcher.endBatch();
	}
	
	
	private void presentPaused() {        
        if(lastScore >= Settings.highscores[4]) 
            scoreString = "new highscore: " + lastScore;
        else
            scoreString = "score: " + world.score;
        
		
		batcher.beginBatch(Assets.items);
	    batcher.drawSprite(160, 240, 192, 96, Assets.pauseMenu);
	    if (scoreString.contains("new highscore")) 
	    	Assets.font.drawText(batcher, scoreString, 320/2-40, 320,4,10);
	    else
	    	Assets.font.drawText(batcher, scoreString, 320/2-20, 320,4,10);
	    batcher.endBatch();
	}
	
	
	private void presentRunning() {
		
		batcher.beginBatch(Assets.items);
		Assets.font.drawText(batcher, showProblem, 10, 250,4,10);
		Assets.font.drawText(batcher, "Level: "+world.LEVEL, 10, 290,4,10);
		Assets.font.drawText(batcher, "Destination: "+world.DestinationString, 10, 270,4,10);
		Assets.font.drawText(batcher, limitFire+"", effect3Button.position.x, 20+20,4,10);
		Assets.font.drawText(batcher, limitWind+"", windButton.position.x, 20+20,4,10);

		Assets.font.drawText(batcher, "Score: "+world.score, 250, 428,5,12);
		
//		Assets.font.drawText(batcher, ""+touchPoint.x+" : "+touchPoint.y, 250, 428,5,12);
		batcher.drawSprite(300, 20, 32, 32, Assets.pause);
		batcher.endBatch();
		
		if (!isAttackButtonClick) {
			batcher.beginBatch(Assets.AttackTexture);
			batcher.drawSprite(attackButton.position.x	, attackButton.position.y, 17f, 25f, Assets.AttackTexturereRegion);
			batcher.endBatch();
		}
		else {
			batcher.beginBatch(Assets.AttackTexture2);
			batcher.drawSprite(attackButton.position.x	, attackButton.position.y, 17f, 25f, Assets.AttackTexturereRegion2);
			batcher.endBatch();
		}
		
		renderWindButton();
		renderEffect3Button();
		renderSpacialQuestion();
//		renderHpPoint();
	}


	private void renferOperandButton() {
		float x=0.0f;
		for (int i = 0; i < 4; i++) {
			if (isOperandClick[i]==false) {
				batcher.beginBatch(Assets.operandButtonTexture1);
				NumberButton operand = OperandButtonList.get(i);
				TextureRegion keyFrame = Assets.numberOperandAnim1.getKeyFrame(x, Animation.ANIMATION_LOOPING);
				batcher.drawSprite(operand.position.x, operand.position.y, operand.WIDTH, operand.HEIGHT, keyFrame);
				x+=0.2f;
				batcher.endBatch();
			}
			else{
				batcher.beginBatch(Assets.operandButtonTexture2);
				NumberButton operand = OperandButtonList.get(i);
				TextureRegion keyFrame = Assets.numberOperandAnim2.getKeyFrame(x, Animation.ANIMATION_LOOPING);
				batcher.drawSprite(operand.position.x, operand.position.y, operand.WIDTH, operand.HEIGHT, keyFrame);
				x+=0.2f;
				batcher.endBatch();
			}
		}
	}

	private void renderNumberButton() {
		float x=0.0f;
		for (int i = 0; i < 10; i++) {
			if (isNumberClick[i]==false) {
				batcher.beginBatch(Assets.numberButtonTexture);
				NumberButton number = NumberButtonList.get(i);
				TextureRegion keyFrame = Assets.numberButtonAnimation.getKeyFrame(x, Animation.ANIMATION_LOOPING);
				batcher.drawSprite(number.position.x, number.position.y, number.WIDTH, number.HEIGHT, keyFrame);
				x+=0.2f;
				batcher.endBatch();
			}
			else {
				batcher.beginBatch(Assets.numberButtonTexture2);
				NumberButton number = NumberButtonList.get(i);
				TextureRegion keyFrame = Assets.numberButtonAnimation2.getKeyFrame(x, Animation.ANIMATION_LOOPING);
				batcher.drawSprite(number.position.x, number.position.y, number.WIDTH, number.HEIGHT, keyFrame);
				x+=0.2f;
				batcher.endBatch();
//				isNumberClick[i]=!isNumberClick[i];
			}
		}
	}

//	int minus=0;
//	float time=0.0f;
//	private void renderHpPoint() {
//		batcher.beginBatch(Assets.hpTexture);
//		TextureRegion keyFrame = Assets.hpRegion.getKeyFrame(time, Animation.ANIMATION_LOOPING);
//		batcher.drawSprite(120, 20, hpLength, 20, keyFrame);
//		batcher.endBatch();
////		time+=0.01f;
//	}

	private void renderSpacialQuestion() {
		if (world.GAME_PLAY==1) {
			keeySpacialQuestion ="     GAME1";
		}
		else{
			keeySpacialQuestion ="Game2: "+world.game2.base+"=?";
		}
//		keeySpacialQuestion="testGame2";
		batcher.beginBatch(Assets.items);
		Assets.font.drawText(batcher, keeySpacialQuestion, guiCam.frustumWidth/2-20, guiCam.frustumHeight-50,4,11);
		batcher.endBatch();
	}

	private void renderWindButton() {
		if (isDragWindButton) {
			batcher.beginBatch(Assets.windIconTexture);
			batcher.drawSprite(touchPoint.x	, touchPoint.y, 32*4f, 13*32f, Assets.windIconTextureRegion);
			batcher.endBatch();			
		}
		else{
			batcher.beginBatch(Assets.windIconTexture);
			batcher.drawSprite(windButton.position.x	, windButton.position.y, 17f, 25f, Assets.windIconTextureRegion);
			batcher.endBatch();
		}
	}
	
	float magicTime=0.0f;
	private void renderEffect3Button() {
		if (isEffect3Button) {
			magicTime+=0.05f;
			batcher.beginBatch(Assets.MagicCircleTexture);
			TextureRegion keyFrame = Assets.MagicCircleAnim.getKeyFrame(magicTime	, Animation.ANIMATION_LOOPING);
			batcher.drawSprite(touchPoint.x	, touchPoint.y, 32, 32, keyFrame);
			batcher.endBatch();
		}
		else{
			magicTime=0;
			batcher.beginBatch(Assets.MagicCircleTexture);
			TextureRegion keyFrame = Assets.MagicCircleAnim.getKeyFrame(magicTime	, Animation.ANIMATION_LOOPING);
			batcher.drawSprite(effect3Button.position.x	, effect3Button.position.y+2, 17f, 30f, keyFrame);
			batcher.endBatch();
		}
	}
	
	@Override
	public void pause() {
		if (state==GAME_RUNNING) {
			state = GAME_PAUSED;
		}
	}

	@Override
	public void resume() {
	}

	@Override
	public void dispose() {
	}

}
