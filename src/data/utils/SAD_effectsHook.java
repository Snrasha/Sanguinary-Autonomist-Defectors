package src.data.utils;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import org.lwjgl.util.vector.Vector2f;

public class SAD_effectsHook extends BaseEveryFrameCombatPlugin
{
  private static final float CONCUSSION_SHOCKWAVE_DURATION = 0.25F;
  private static final float CONCUSSION_SHOCKWAVE_MAX_SCALE = 1.5F;
  private static final float CONCUSSION_SHOCKWAVE_MIN_SCALE = 0.25F;
  private static final String DATA_KEY = "SAD_effectsHook";
  private static final float FLAK_SHOCKWAVE_DURATION = 0.2F;
  private static final float FLAK_SHOCKWAVE_MAX_SCALE = 0.8F;
  private static final float FLAK_SHOCKWAVE_MIN_SCALE = 0.15F;
  private static final float GAP_DURATION = 0.2F;
  private static final float GAP_MAX_SCALE = 0.8F;
  private static final float GAP_MIN_SCALE = 0.15F;
  private static final float PING_DURATION = 3.0F;
  private static final float PING_MAX_SCALE = 3.5F;
  private static final float PING_MIN_SCALE = 0.75F;
  private static final float PULSE_DURATION = 1.25F;
  private static final float PULSE_MAX_SCALE = 2.5F;
  private static final float PULSE_MIN_SCALE = 0.5F;
  private static final float EMP_SHOCKWAVE_DURATION = 0.05F;
  private static final float EMP_SHOCKWAVE_MAX_SCALE = 1.0F;
  private static final float EMP_SHOCKWAVE_MIN_SCALE = 0.2F;
  private static final int SHOCKWAVE_SIZE = 256;
  private CombatEngineAPI engine;
  
  public SAD_effectsHook() {}
  
  public static void createFlakShockwave(Vector2f location)
  {
    LocalData localData = (LocalData)Global.getCombatEngine().getCustomData().get("SAD_effectsHook");
    if (localData == null)
    {
      return;
    }
    
    List<Shockwave> shockwaves = localData.shockwaves;
    
    shockwaves.add(new Shockwave(location, 0.2F, 0.8F, 0.15F));
  }
  
  public static void createShockwave(Vector2f location)
  {
    LocalData localData = (LocalData)Global.getCombatEngine().getCustomData().get("SAD_effectsHook");
    if (localData == null)
    {
      return;
    }
    
    List<Shockwave> shockwaves = localData.shockwaves;
    
    shockwaves.add(new Shockwave(location, 0.25F, 1.5F, 0.25F));
  }
  
  public static void createEMPShockwave(Vector2f location)
  {
    LocalData localData = (LocalData)Global.getCombatEngine().getCustomData().get("SAD_effectsHook");
    if (localData == null)
    {
      return;
    }
    
    List<Shockwave> shockwaves = localData.shockwaves;
    
    shockwaves.add(new Shockwave(location, 0.05F, 1.0F, 0.2F));
  }
  

  public static void createPing(Vector2f location, Vector2f velocity)
  {
    LocalData localData = (LocalData)Global.getCombatEngine().getCustomData().get("SAD_effectsHook");
    if (localData == null)
    {
      return;
    }
    
    List<tagPing> pings = localData.pings;
    
    pings.add(new tagPing(location, velocity, 3.0F, 3.5F, 0.75F));
  }
  
  public static void createPulse(Vector2f location)
  {
    LocalData localData = (LocalData)Global.getCombatEngine().getCustomData().get("SAD_effectsHook");
    if (localData == null)
    {
      return;
    }
    
    List<swacsPulse> pulses = localData.pulses;
    
    pulses.add(new swacsPulse(location, 1.25F, 2.5F, 0.5F));
  }
  



  public static void createRift(Vector2f location)
  {
    LocalData localData = (LocalData)Global.getCombatEngine().getCustomData().get("SAD_effectsHook");
    if (localData == null)
    {
      return;
    }
   
    List<Rift> rifts =  localData.rifts;
    
    rifts.add(new Rift(location, 0.2F, 0.8F, 0.15F));
  }
  

  public void advance(float amount, List<InputEventAPI> events)
  {
    if (engine == null)
    {
      return;
    }
    
    if (engine.isPaused())
    {
      return;
    }
    
    LocalData localData = (LocalData)engine.getCustomData().get("SAD_effectsHook");
    List<Shockwave> shockwaves = localData.shockwaves;
    
    Iterator<Shockwave> iter = shockwaves.iterator();
    while (iter.hasNext())
    {
      Shockwave wave = (Shockwave)iter.next();
      
      wave.lifespan -= amount;
      if (wave.lifespan <= 0.0F)
      {
        iter.remove();
      }
      else
      {
        wave.alpha = (wave.lifespan / wave.maxLifespan);
        wave.scale = (wave.minScale + (wave.maxLifespan - wave.lifespan) / wave.maxLifespan * (wave.maxScale - wave.minScale));
      }
    }
    List<tagPing> pings = localData.pings;
    
    Iterator<tagPing> iterB = pings.iterator();
    while (iterB.hasNext())
    {
      tagPing ping = (tagPing)iterB.next();
      
      ping.lifespan -= amount;
      if (ping.lifespan <= 0.0F)
      {
        iterB.remove();
      }
      else
      {
        ping.alpha = (ping.lifespan / ping.maxLifespan);
        ping.scale = (ping.minScale + (ping.maxLifespan - ping.lifespan) / ping.maxLifespan * (ping.maxScale - ping.minScale));
      }
    }
    List<swacsPulse> pulses = localData.pulses;
    
    Iterator<swacsPulse> iterC = pulses.iterator();
    while (iterC.hasNext())
    {
      swacsPulse pulse = (swacsPulse)iterC.next();
      
      pulse.lifespan -= amount;
      if (pulse.lifespan <= 0.0F)
      {
        iterC.remove();
      }
      else
      {
        pulse.alpha = (pulse.lifespan / pulse.maxLifespan);
        pulse.scale = (pulse.minScale + (pulse.maxLifespan - pulse.lifespan) / pulse.maxLifespan * (pulse.maxScale - pulse.minScale));
      }
    }
    List<Rift> rifts = localData.rifts;
    
    Iterator<Rift> iterD = rifts.iterator();
    while (iterD.hasNext())
    {
      Rift rift = (Rift)iterD.next();
      
      rift.lifespan -= amount;
      if (rift.lifespan <= 0.0F)
      {
        iterD.remove();
      }
      else
      {
        rift.alpha = (rift.lifespan / rift.maxLifespan);
        rift.scale = (rift.maxScale - (rift.maxLifespan - rift.lifespan) / rift.maxLifespan * (rift.maxScale - rift.minScale));
      }
    }
  }
  
  public void init(CombatEngineAPI engine)
  {
    this.engine = engine;
    engine.getCustomData().put("SAD_effectsHook", new LocalData());
  }
  

  public void renderInWorldCoords(com.fs.starfarer.api.combat.ViewportAPI viewport)
  {
    if (engine == null)
    {
      return;
    }
    


    LocalData localData = (LocalData)engine.getCustomData().get("SAD_effectsHook");
    List<Shockwave> shockwaves = localData.shockwaves;
    List<tagPing> pings = localData.pings;
    List<swacsPulse> pulses = localData.pulses;
    List<Rift> rifts = localData.rifts;
    

    for (Shockwave wave : shockwaves)
    {
      SpriteAPI waveSprite = Global.getSettings().getSprite("concussion", "flareFlakWave");
      if (waveSprite != null)
      {
        waveSprite.setAlphaMult(wave.alpha);
        waveSprite.setAdditiveBlend();
        waveSprite.setAngle(wave.facing);
        waveSprite.setSize(wave.scale * 256.0F, wave.scale * 256.0F);
        waveSprite.renderAtCenter(wave.location.x, wave.location.y);
      }
    }
    
    for (tagPing ping : pings) {
      SpriteAPI waveSprite = Global.getSettings().getSprite("ping", "tagPing");
      if (waveSprite != null)
      {
        waveSprite.setAlphaMult(ping.alpha);
        waveSprite.setAdditiveBlend();
        waveSprite.setAngle(ping.facing);
        waveSprite.setSize(ping.scale * 256.0F, ping.scale * 256.0F);
        waveSprite.renderAtCenter(ping.location.x, ping.location.y);
      }
    }
    
    for (swacsPulse pulse : pulses) {
      SpriteAPI waveSprite = Global.getSettings().getSprite("ping", "swacsPing");
      if (waveSprite != null)
      {
        waveSprite.setAlphaMult(pulse.alpha);
        waveSprite.setAdditiveBlend();
        waveSprite.setAngle(pulse.facing);
        waveSprite.setSize(pulse.scale * 256.0F, pulse.scale * 256.0F);
        waveSprite.renderAtCenter(pulse.location.x, pulse.location.y);
      }
    }
    
    for (Rift rift : rifts) {
      SpriteAPI eventHorizonSprite = Global.getSettings().getSprite("misc", "ms_eventHorizon");
      SpriteAPI gapSprite = Global.getSettings().getSprite("misc", "ms_phaseSpaceRift" + org.lazywizard.lazylib.MathUtils.getRandomNumberInRange(1, 5));
      
      SpriteAPI flareSprite = Global.getSettings().getSprite("flare", "nidhoggr_ALF");
      if (eventHorizonSprite != null)
      {
        eventHorizonSprite.setAlphaMult(rift.alpha);
        eventHorizonSprite.setAdditiveBlend();
        eventHorizonSprite.setAngle(rift.facing);
        eventHorizonSprite.setSize(150.0F, 150.0F);
        eventHorizonSprite.renderAtCenter(rift.location.x, rift.location.y);
      }
      
      if (gapSprite != null)
      {
        gapSprite.setAlphaMult(0.8F);
        gapSprite.setAdditiveBlend();
        gapSprite.setAngle(rift.facing);
        gapSprite.setSize(rift.scale * 256.0F, rift.scale * 256.0F);
        gapSprite.renderAtCenter(rift.location.x, rift.location.y);
      }
      

      if (flareSprite != null)
      {
        flareSprite.setAlphaMult(rift.alpha);
        flareSprite.setAdditiveBlend();
        flareSprite.setAngle(0.0F);
        flareSprite.renderAtCenter(rift.location.x, rift.location.y);
      }
    }
  }
  
  private static final class LocalData
  {
    final List<SAD_effectsHook.Shockwave> shockwaves = new LinkedList();
    final List<SAD_effectsHook.tagPing> pings = new LinkedList();
    final List<SAD_effectsHook.swacsPulse> pulses = new LinkedList();
    final List<SAD_effectsHook.Rift> rifts = new LinkedList();
    
    private LocalData() {}
  }
  
  static class Shockwave {
    float alpha;
    final float facing;
    float lifespan;
    final Vector2f location;
    float maxLifespan;
    float maxScale;
    float minScale;
    float scale;
    
    Shockwave(Vector2f location, float duration, float maxScale, float minScale) {
      this.location = new Vector2f(location);
      alpha = 1.0F;
      facing = ((float)Math.random() * 360.0F);
      maxLifespan = duration;
      lifespan = maxLifespan;
      this.maxScale = maxScale;
      this.minScale = minScale;
      scale = minScale;
    }
  }
  
  static class tagPing
  {
    float alpha;
    final float facing;
    float lifespan;
    final Vector2f location;
    final Vector2f velocity;
    float maxLifespan;
    float maxScale;
    float minScale;
    float scale;
    
    tagPing(Vector2f location, Vector2f velocity, float duration, float maxScale, float minScale)
    {
      this.location = new Vector2f(location);
      this.velocity = new Vector2f(velocity);
      alpha = 1.0F;
      facing = ((float)Math.random() * 360.0F);
      maxLifespan = duration;
      lifespan = maxLifespan;
      this.maxScale = maxScale;
      this.minScale = minScale;
      scale = minScale;
    }
  }
  
  static class swacsPulse
  {
    float alpha;
    final float facing;
    float lifespan;
    final Vector2f location;
    float maxLifespan;
    float maxScale;
    float minScale;
    float scale;
    
    swacsPulse(Vector2f location, float duration, float maxScale, float minScale)
    {
      this.location = new Vector2f(location);
      alpha = 1.0F;
      facing = ((float)Math.random() * 360.0F);
      maxLifespan = duration;
      lifespan = maxLifespan;
      this.maxScale = maxScale;
      this.minScale = minScale;
      scale = minScale;
    }
  }
  
  static class Rift
  {
    float alpha;
    final float facing;
    float lifespan;
    final Vector2f location;
    float maxLifespan;
    float maxScale;
    float minScale;
    float scale;
    
    Rift(Vector2f location, float duration, float maxScale, float minScale)
    {
      this.location = new Vector2f(location);
      alpha = 1.0F;
      facing = ((float)Math.random() * 360.0F);
      maxLifespan = duration;
      lifespan = maxLifespan;
      this.maxScale = maxScale;
      this.minScale = minScale;
      scale = minScale;
    }
  }
}
