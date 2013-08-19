package com.sk89q.craftbook.mech.ai;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.EntityTargetEvent;

import com.sk89q.craftbook.CraftBookMechanic;
import com.sk89q.craftbook.bukkit.CraftBookPlugin;
import com.sk89q.craftbook.util.EntityUtil;

public class AIMechanic implements CraftBookMechanic {

    private List<BaseAIMechanic> mechanics = new ArrayList<BaseAIMechanic>();

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityTarget(EntityTargetEvent event) {

        if (event.getEntity() == null) return;
        for (BaseAIMechanic mechanic : mechanics) {
            if (!(mechanic instanceof TargetAIMechanic))
                continue;

            boolean passes = false;

            for(EntityType t : mechanic.entityType) {
                if(t == event.getEntity().getType()) {
                    passes = true;
                    break;
                }
            }

            if(passes) {
                CraftBookPlugin.logDebugMessage("AI Mechanic Running: " + mechanic.getClass().getName(), "ai-mechanics.entity-target");
                ((TargetAIMechanic) mechanic).onEntityTarget(event);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityShootBow(EntityShootBowEvent event) {

        if (event.getEntity() == null) return;
        for (BaseAIMechanic mechanic : mechanics) {
            if (!(mechanic instanceof BowShotAIMechanic))
                continue;

            boolean passes = false;

            for(EntityType t : mechanic.entityType) {
                if(t == event.getEntity().getType()) {
                    passes = true;
                    break;
                }
            }

            if(passes) {
                CraftBookPlugin.logDebugMessage("AI Mechanic Running: " + mechanic.getClass().getName(), "ai-mechanics.shoot-bow");
                ((BowShotAIMechanic) mechanic).onBowShot(event);
            }
        }
    }

    public boolean registerAIMechanic(BaseAIMechanic mechanic) {

        if(mechanic == null)
            return false;
        return mechanics.add(mechanic);
    }

    @Override
    public boolean enable () {

        int regd = 0;
        if(CraftBookPlugin.inst().getConfiguration().aiVisionEnabled.size() > 0)
            if(!registerAIMechanic(new VisionAIMechanic(EntityUtil.parseEntityList(CraftBookPlugin.inst().getConfiguration().aiVisionEnabled))))
                CraftBookPlugin.logger().severe("Failed To Register Realistic Vision AI Mechanic!");
            else
                regd++;
        if (CraftBookPlugin.inst().getConfiguration().aiCritBowEnabled.size() > 0)
            if(!registerAIMechanic(new CriticalBotAIMechanic(EntityUtil.parseEntityList(CraftBookPlugin.inst().getConfiguration().aiCritBowEnabled))))
                CraftBookPlugin.logger().severe("Failed To Register Critical Shot AI Mechanic!");
            else
                regd++;
        if (CraftBookPlugin.inst().getConfiguration().aiAttackPassiveEnabled.size() > 0)
            if(!registerAIMechanic(new AttackPassiveAIMechanic(EntityUtil.parseEntityList(CraftBookPlugin.inst().getConfiguration().aiAttackPassiveEnabled))))
                CraftBookPlugin.logger().severe("Failed To Register Passive Attack AI Mechanic!");
            else
                regd++;
        return regd > 0;
    }

    @Override
    public void disable () {
        mechanics = null;
    }
}