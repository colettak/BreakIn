package com.colettak.breakin;

import org.anddev.andengine.entity.sprite.AnimatedSprite;
import org.anddev.andengine.opengl.texture.region.TiledTextureRegion;

public class GameSprite extends AnimatedSprite {
  private static final String TAG = "GameSprite";
  private SpriteType spritetype;
  public GameSprite(float pX, float pY, TiledTextureRegion pTiledTextureRegion, SpriteType spriteType) {
    super(pX, pY, pTiledTextureRegion);
    spritetype = spriteType;
  }
  
  public SpriteType getSpriteType(){
    return spritetype;
  }
  
}
