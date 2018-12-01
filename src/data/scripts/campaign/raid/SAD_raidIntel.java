package src.data.scripts.campaign.raid;

import java.awt.Color;
import java.util.Random;
import java.util.Set;

import org.lwjgl.input.Keyboard;
import org.lwjgl.util.vector.Vector2f;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.DebugFlags;
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3;
import com.fs.starfarer.api.impl.campaign.fleets.FleetParamsV3;
import com.fs.starfarer.api.impl.campaign.fleets.RouteLocationCalculator;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.impl.campaign.ids.Ranks;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.intel.raid.RaidIntel;
import com.fs.starfarer.api.impl.campaign.intel.raid.RaidIntel.RaidDelegate;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.ButtonAPI;
import com.fs.starfarer.api.ui.IntelUIAPI;
import com.fs.starfarer.api.ui.LabelAPI;
import com.fs.starfarer.api.ui.SectorMapAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import src.data.scripts.campaign.raid.SAD_RouteManager.RouteData;
import src.data.scripts.campaign.raid.SAD_RouteManager.SAD_OptionalFleetData;
import src.data.scripts.campaign.raid.SAD_raidManager.PunExGoal;
import src.data.scripts.campaign.raid.SAD_raidManager.PunExReason;
import src.data.scripts.campaign.raid.SAD_raidManager.PunExType;
import src.data.utils.SAD_Tags;

public class SAD_raidIntel extends RaidIntel implements RaidDelegate, SAD_RouteManager.SAD_RouteFleetSpawner{

	public static final String BUTTON_AVERT = "BUTTON_CHANGE_ORDERS";
	
	public static enum PunExOutcome {
		TASK_FORCE_DEFEATED,
		COLONY_NO_LONGER_EXISTS,
		SUCCESS,
		BOMBARD_FAIL,
		RAID_FAIL,
		AVERTED,
	}
	
	public static final Object ENTERED_SYSTEM_UPDATE = new Object();
	public static final Object OUTCOME_UPDATE = new Object();
	
	protected SAD_ActionStage action;
	protected PunExGoal goal;
	protected MarketAPI target;
	protected CampaignFleetAPI from;
	protected PunExOutcome outcome;
	
	protected Random random = new Random();

	protected PunExReason bestReason;
	protected Industry targetIndustry;
	protected FactionAPI targetFaction;
	
	public SAD_raidIntel(FactionAPI faction, CampaignFleetAPI from, MarketAPI target, 
								   float expeditionFP, float organizeDuration,
								   PunExGoal goal, Industry targetIndustry, PunExReason bestReason) {
		super(target.getStarSystem(), faction, null);
		this.goal = goal;
		this.targetIndustry = targetIndustry;
		this.bestReason = bestReason;
		this.delegate = this;
		this.from = from;
		this.target = target;
		targetFaction = target.getFaction();
		
		SectorEntityToken gather = from;//target.getPrimaryEntity();
		
		
		float orgDur = organizeDuration;
		if (DebugFlags.PUNITIVE_EXPEDITION_DEBUG) orgDur = 0.5f;
		
		addStage(new SAD_OrganizeStage(this, from, orgDur));
		
		float successMult = 0.5f;
		SAD_AssembleStage assemble = new SAD_AssembleStage(this, gather,from);
		assemble.setSpawnFP(expeditionFP);
		assemble.setAbortFP(expeditionFP * successMult);
		addStage(assemble);
		
		
		SectorEntityToken raidJump = RouteLocationCalculator.findJumpPointToUse(getFactionForUIColors(), target.getPrimaryEntity());
		SAD_TravelStage travel = new SAD_TravelStage(this, gather, raidJump, false);
		travel.setAbortFP(expeditionFP * successMult);
		addStage(travel);
		
		action = new SAD_ActionStage(this, target);
		action.setAbortFP(expeditionFP * successMult);
		addStage(action);
		
		addStage(new SAD_returnStage(this));
		
		Global.getSector().getIntelManager().addIntel(this);
	}
	
	public Random getRandom() {
		return random;
	}

	public MarketAPI getTarget() {
		return target;
	}
	
	public FactionAPI getTargetFaction() {
		return targetFaction;
	}

	public void reportAboutToBeDespawnedByRouteManager(RouteData route) {
	}
	
	public boolean shouldRepeat(RouteData route) {
		return false;
	}

	public boolean shouldCancelRouteAfterDelayCheck(RouteData route) {
		return false;
	}

	public SAD_RaidAssignmentAI createAssignmentAI(CampaignFleetAPI fleet, RouteData route) {
		SAD_RaidAssignmentAI raidAI = new SAD_RaidAssignmentAI(fleet, route, action);
		//raidAI.setDelegate(action);
		return raidAI;
	}
	
	@Override
	protected void advanceImpl(float amount) {
		super.advanceImpl(amount);
	}

	public void sendOutcomeUpdate() {
		sendUpdateIfPlayerHasIntel(OUTCOME_UPDATE, false);
	}
	
	public void sendEnteredSystemUpdate() {
		sendUpdateIfPlayerHasIntel(ENTERED_SYSTEM_UPDATE, false);
	}
	
	@Override
	public String getName() {
		String base = Misc.ucFirst(faction.getPersonNamePrefix()) + " Expedition";
		if (isEnding()) {
			if (outcome == PunExOutcome.AVERTED) {
				return base + " - Averted";
			}
			if (isSendingUpdate() && isFailed()) {
				return base + " - Failed";
			}
			if (isSucceeded() || outcome == PunExOutcome.SUCCESS) {
				return base + " - Successful";
			}
			if (outcome == PunExOutcome.RAID_FAIL || 
					outcome == PunExOutcome.BOMBARD_FAIL ||
					outcome == PunExOutcome.COLONY_NO_LONGER_EXISTS ||
					outcome == PunExOutcome.TASK_FORCE_DEFEATED) {
				return base + " - Failed";
			}
		}
		return base;
	}

	
	@Override
	protected void addBulletPoints(TooltipMakerAPI info, ListInfoMode mode) {
		//super.addBulletPoints(info, mode);
		
		Color h = Misc.getHighlightColor();
		Color g = Misc.getGrayColor();
		float pad = 3f;
		float opad = 10f;
		
		float initPad = pad;
		if (mode == ListInfoMode.IN_DESC) initPad = opad;
		
		Color tc = getBulletColorForMode(mode);
		
		bullet(info);
		boolean isUpdate = getListInfoParam() != null;
		
		if (getListInfoParam() == OUTCOME_UPDATE) {
		}
		
		if (getListInfoParam() == ENTERED_SYSTEM_UPDATE) {
			FactionAPI other = target.getFaction();
			info.addPara("Target: %s", initPad, tc,
					     other.getBaseUIColor(), target.getName());
			initPad = 0f;
			info.addPara("Arrived in-system", tc, initPad);
//			info.addPara("" + faction.getDisplayName() + " forces arrive in-system", initPad, tc,
//					faction.getBaseUIColor(), faction.getDisplayName());
			return;
		}
		

		FactionAPI other = targetFaction;
		if (outcome != null) {
			if (outcome == PunExOutcome.TASK_FORCE_DEFEATED) {
				info.addPara("Expeditionary force defeated", tc, initPad);
			} else if (outcome == PunExOutcome.COLONY_NO_LONGER_EXISTS) {
				info.addPara("Expedition aborted", tc, initPad);
			} else if (outcome == PunExOutcome.AVERTED) {
				info.addPara("Expedition planning disrupted", initPad, tc, other.getBaseUIColor(), target.getName());
			} else if (outcome == PunExOutcome.BOMBARD_FAIL) {
				info.addPara("Bombardment of %s failed", initPad, tc, other.getBaseUIColor(), target.getName());
			} else if (outcome == PunExOutcome.RAID_FAIL) {
				info.addPara("Raid of %s failed", initPad, tc, other.getBaseUIColor(), target.getName());
			} else if (outcome == PunExOutcome.SUCCESS) {
				if (goal == PunExGoal.BOMBARD) {
					if (!target.isInEconomy()) {
						info.addPara("%s destroyed by bombardment", initPad, tc, other.getBaseUIColor(), target.getName());
					} else {
						info.addPara("Bombardment of %s successful", initPad, tc, other.getBaseUIColor(), target.getName());
					}
				} else if (targetIndustry != null && targetIndustry.getDisruptedDays() >= 2) {
					info.addPara(targetIndustry.getCurrentName() + " disrupted for %s days",
							initPad, tc, h, "" + (int)Math.round(targetIndustry.getDisruptedDays()));
				}
			}
			return;
		}
		
		info.addPara("Target: %s", initPad, tc,
			     other.getBaseUIColor(), target.getName());
		initPad = 0f;
		
		if (goal == PunExGoal.BOMBARD) {
			String goalStr = "saturation bombardment";
			info.addPara("Goal: %s", initPad, tc, Misc.getNegativeHighlightColor(), goalStr);
		}
		
		float eta = getETA();
		if (eta > 1 && !isEnding()) {
			String days = getDaysString(eta);
			info.addPara("Estimated %s " + days + " until arrival", 
					initPad, tc, h, "" + (int)Math.round(eta));
			initPad = 0f;
		} else if (!isEnding() && action.getElapsed() > 0) {
			info.addPara("Currently in-system", tc, initPad);
			initPad = 0f;
		}
		
		unindent(info);
	}
	
	public SAD_ActionStage getActionStage() {
		for (RaidStage stage : stages) {
			if (stage instanceof SAD_ActionStage) {
				return (SAD_ActionStage) stage;
			}
		}
		return null;
		//return (PEActionStage) stages.get(2);
	}

	@Override
	public void createIntelInfo(TooltipMakerAPI info, ListInfoMode mode) {
		super.createIntelInfo(info, mode);
	}

	
	public void addInitialDescSection(TooltipMakerAPI info, float initPad) {
		Color h = Misc.getHighlightColor();
		float opad = 10f;
		
		FactionAPI faction = getFaction();
		String is = faction.getDisplayNameIsOrAre();
		
		String goalDesc = "";
		String goalHL = "";
		Color goalColor = Misc.getTextColor();
		switch (goal) {
		case RAID_PRODUCTION:
			goalDesc = "disrupting the colony's " + targetIndustry.getCurrentName();
			break;
		case RAID_SPACEPORT:
			goalDesc = "raiding the colony's " + targetIndustry.getCurrentName() + " to disrupt its operations";
			break;
		case BOMBARD:
			goalDesc = "a saturation bombardment of the colony";
			goalHL = "saturation bombardment of the colony";
			goalColor = Misc.getNegativeHighlightColor();
			break;
		}
		
		String strDesc = getRaidStrDesc();
		
		if (outcome == null) {
			LabelAPI label = info.addPara(Misc.ucFirst(faction.getDisplayNameWithArticle()) + " " + is + 
					" targeting %s with a " + strDesc + " expeditionary force. " +
					"Its likely goal is " + goalDesc + ".",
					initPad, faction.getBaseUIColor(), target.getName());
			label.setHighlight(faction.getDisplayNameWithArticleWithoutArticle(), target.getName(), strDesc, goalHL);
			label.setHighlightColors(faction.getBaseUIColor(), targetFaction.getBaseUIColor(), h, goalColor);	
		} else {
			LabelAPI label = info.addPara(Misc.ucFirst(faction.getDisplayNameWithArticle()) + " " + is + 
					" targeting %s with an expeditionary force. " +
					"Its likely goal is " + goalDesc + ".",
					initPad, faction.getBaseUIColor(), target.getName());
			label.setHighlight(faction.getDisplayNameWithArticleWithoutArticle(), target.getName(), goalHL);
			label.setHighlightColors(faction.getBaseUIColor(), targetFaction.getBaseUIColor(), goalColor);
		}
	}
	
	@Override
	public void createSmallDescription(TooltipMakerAPI info, float width, float height) {
		//super.createSmallDescription(info, width, height);
		
		Color h = Misc.getHighlightColor();
		Color g = Misc.getGrayColor();
		Color tc = Misc.getTextColor();
		float pad = 3f;
		float opad = 10f;
		
		info.addImage(getFactionForUIColors().getLogo(), width, 128, opad);
		
		FactionAPI faction = getFaction();
		String has = faction.getDisplayNameHasOrHave();
		String is = faction.getDisplayNameIsOrAre();
		
		addInitialDescSection(info, opad);
		

		info.addPara("The primary reason for the expedition is than they want see you died. " +
						 "Peace and love claimed by " + faction.getDisplayNameWithArticle() + ".", opad);
		
		
		if (outcome == null) {
			addStandardStrengthComparisons(info, target, targetFaction, goal != PunExGoal.BOMBARD, goal == PunExGoal.BOMBARD,
										   "expedition", "expedition's");
		}
		
		info.addSectionHeading("Status", 
				   faction.getBaseUIColor(), faction.getDarkUIColor(), Alignment.MID, opad);
		
		for (RaidStage stage : stages) {
			stage.showStageInfo(info);
			if (getStageIndex(stage) == failStage) break;
		}
		
		if (getCurrentStage() == 0 && !isFailed()) {
			FactionAPI pf = Global.getSector().getPlayerFaction();
			ButtonAPI button = info.addButton("Avert", BUTTON_AVERT, 
				  	pf.getBaseUIColor(), pf.getDarkUIColor(),
				  (int)(width), 20f, opad * 2f);
			button.setShortcut(Keyboard.KEY_T, true);
		}
		
	
		if (!from.getFaction().isHostileTo(targetFaction) && !isFailed()) {
//			LabelAPI label = info.addPara("This operation is being carried " +
//					"without an open declaration of war. Fighting the " +
//					"expeditionary force will not result in " + faction.getDisplayNameWithArticle() + 
//					" immediately becoming hostile, unless the relationship is already strained.", Misc.getGrayColor(), 
//					opad);
			LabelAPI label = info.addPara("This operation is being carried " +
					"without an open declaration of war. Fighting the " +
					"expeditionary force should not result in " + faction.getDisplayNameWithArticle() + 
					" immediately becoming hostile. But they are crazy, so nobody like them.", Misc.getGrayColor(), 
					opad);
			label.setHighlight(faction.getDisplayNameWithArticleWithoutArticle());
			label.setHighlightColors(faction.getBaseUIColor());
		}
	}
	
	

	@Override
	public void sendUpdateIfPlayerHasIntel(Object listInfoParam, boolean onlyIfImportant, boolean sendIfHidden) {
		
		if (listInfoParam == UPDATE_RETURNING) {
			// we're using sendOutcomeUpdate() to send an end-of-event update instead
			return;
		}
		
		super.sendUpdateIfPlayerHasIntel(listInfoParam, onlyIfImportant, sendIfHidden);
	}

	@Override
	public Set<String> getIntelTags(SectorMapAPI map) {
		//return super.getIntelTags(map);
		
		Set<String> tags = super.getIntelTags(map);
		tags.add(Tags.INTEL_MILITARY);
		tags.add(Tags.INTEL_COLONIES);
		tags.add(getFaction().getId());
		return tags;
	}

	
	public void notifyRaidEnded(RaidIntel raid, RaidStageStatus status) {
		if (outcome == null && failStage >= 0) {
			if (!target.isInEconomy() || !target.isPlayerOwned()) {
				outcome = PunExOutcome.COLONY_NO_LONGER_EXISTS;
			} else {
				outcome = PunExOutcome.TASK_FORCE_DEFEATED;
			}
		}
		
		SAD_raidManager.PunExData data = SAD_raidManager.getInstance().dataPun;
		if (data != null) {
			if (outcome == PunExOutcome.SUCCESS) {
				data.numSuccesses++;
			}
		}
	}
	
	
	@Override
	public String getIcon() {
		return faction.getCrest();
	}

	public PunExGoal getGoal() {
		return goal;
	}

	public Industry getTargetIndustry() {
		return targetIndustry;
	}

	public PunExOutcome getOutcome() {
		return outcome;
	}

	public void setOutcome(PunExOutcome outcome) {
		this.outcome = outcome;
	}
	
	public CampaignFleetAPI spawnFleet(RouteData route) {
		
		CampaignFleetAPI fleet = createFleet(SAD_Tags.SAD_FACTION, route, null, random);
		
		if (fleet == null || fleet.isEmpty()) return null;
		
		
		from.getContainingLocation().addEntity(fleet);
		fleet.setFacing((float) Math.random() * 360f);
		// this will get overridden by the patrol assignment AI, depending on route-time elapsed etc
		fleet.setLocation(from.getLocation().x, from.getLocation().x);
		
		fleet.addScript(createAssignmentAI(fleet, route));
		
		return fleet;
	}
	
	public CampaignFleetAPI createFleet(String factionId, RouteData route, Vector2f locInHyper, Random random) {
		if (random == null) random = this.random;
		
		SAD_OptionalFleetData extra = route.getExtra();

		float combat = extra.fp;
		float tanker = extra.fp * (0.1f + random.nextFloat() * 0.05f);
		float transport = extra.fp * (0.1f + random.nextFloat() * 0.05f);
		float freighter = 0f;
		
		if (goal == PunExGoal.BOMBARD) {
			tanker += transport;
		} else {
			transport += tanker / 2f;
			tanker *= 0.5f;
		}
		
		combat -= tanker;
		combat -= transport;
		
		
		FleetParamsV3 params = new FleetParamsV3(
				null, 
				locInHyper,
				factionId,
				route == null ? null : route.getQualityOverride(),
				extra.fleetType,
				combat, // combatPts
				freighter, // freighterPts 
				tanker, // tankerPts
				0f, // transportPts
				0f, // linerPts
				0f, // utilityPts
				0f // qualityMod, won't get used since routes mostly have quality override set
				);
		//params.ignoreMarketFleetSizeMult = true; // already accounted for in extra.fp
		
		if (route != null) {
			params.timestamp = route.getTimestamp();
		}
		params.random = random;
		CampaignFleetAPI fleet = FleetFactoryV3.createFleet(params);
		
		if (fleet == null || fleet.isEmpty()) return null;
		
		fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_WAR_FLEET, true);
		fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_RAIDER, true);
		
		if (fleet.getFaction().getCustomBoolean(Factions.CUSTOM_PIRATE_BEHAVIOR)) {
			fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_PIRATE, true);
		}
		
		String postId = Ranks.POST_PATROL_COMMANDER;
		String rankId = Ranks.SPACE_COMMANDER;
		
		fleet.getCommander().setPostId(postId);
		fleet.getCommander().setRankId(rankId);
		
		Misc.makeLowRepImpact(fleet, "punex");
		Misc.makeHostile(fleet);
		
		
		return fleet;
	}
	
	
        @Override
	public void buttonPressConfirmed(Object buttonId, IntelUIAPI ui) {
		if (buttonId == BUTTON_AVERT) {
			ui.showDialog(null, new SAD_AvertInteractionDialogPluginImpl(this, ui));
		}
	}

	public PunExReason getBestReason() {
		return bestReason;
	}

	public boolean isTerritorial() {
		return bestReason != null && bestReason.type == PunExType.TERRITORIAL;
	}
	
	@Override
	public SectorEntityToken getMapLocation(SectorMapAPI map) {
		if (target != null && target.isInEconomy() && target.getPrimaryEntity() != null) {
			return target.getPrimaryEntity();
		}
		return super.getMapLocation(map);
	}

}






