package src.data.scripts.plugins;

import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import java.util.List;
import java.util.Map;
import org.lwjgl.util.vector.Vector2f;
import src.data.utils.SAD_effectsHook;

public class SAD_DisruptSystemEffect extends BaseEveryFrameCombatPlugin {

    private final float damageMalus = 1.5f;

    public Map<ShipAPI, Float> TELEMETRY = new java.util.HashMap();
    private final IntervalUtil interval = new IntervalUtil(2.0F, 2.0F);
    private CombatEngineAPI engine;

    public SAD_DisruptSystemEffect() {
    }

    @Override
    public void init(CombatEngineAPI engine) {
        this.engine = engine;
    }

    @Override
    public void advance(float amount, List<InputEventAPI> events) {
        if (engine == null) {
            return;
        }
        if (engine.isPaused()) {
            return;
        }

        if (!TELEMETRY.isEmpty()) {
            for (Map.Entry<ShipAPI, Float> entry : TELEMETRY.entrySet()) {
                ShipAPI ship = (ShipAPI) entry.getKey();
                String id = ship.getFleetMemberId()+"_disrupt";
                Vector2f loc = ship.getLocation();
                Vector2f vel = ship.getVelocity();

                float remaining = (entry.getValue()) - amount;

                if (remaining < 0.0F) {
                    TELEMETRY.clear();

                    ship.getMutableStats().getArmorDamageTakenMult().unmodify(id);
                    ship.getMutableStats().getHullDamageTakenMult().unmodify(id);
                    ship.getMutableStats().getShieldDamageTakenMult().unmodify(id);
                } else {
                    TELEMETRY.put(ship, remaining);

                    interval.advance(amount);
                    if (interval.intervalElapsed()) {
                        for (int i = 0; i < 1; i++) {
                            SAD_effectsHook.createPing(loc, vel);
                        }
                    }
/*
                    ship.getMutableStats().getArmorDamageTakenMult().modifyMult(id, damageMalus);
                    ship.getMutableStats().getHullDamageTakenMult().modifyMult(id, damageMalus);
                    ship.getMutableStats().getShieldDamageTakenMult().modifyMult(id, damageMalus);
*/
                }
            }
        }
    }

    public void putTELEMETRY(ShipAPI ship) {
        TELEMETRY.put(ship, 20.0f);
    }
}