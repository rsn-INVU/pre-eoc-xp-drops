package com.preeocxp;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.api.Skill;

@RequiredArgsConstructor
public enum ConfigSkill
{
	ATTACK("Attack"),
	DEFENCE("Defence"),
	STRENGTH("Strength"),
	HITPOINTS("Hitpoints"),
	RANGED("Ranged"),
	PRAYER("Prayer"),
	MAGIC("Magic"),
	COOKING("Cooking"),
	WOODCUTTING("Woodcutting"),
	FLETCHING("Fletching"),
	FISHING("Fishing"),
	FIREMAKING("Firemaking"),
	CRAFTING("Crafting"),
	SMITHING("Smithing"),
	MINING("Mining"),
	HERBLORE("Herblore"),
	AGILITY("Agility"),
	THIEVING("Thieving"),
	SLAYER("Slayer"),
	FARMING("Farming"),
	RUNECRAFT("Runecraft"),
	HUNTER("Hunter"),
	CONSTRUCTION("Construction"),
	OVERALL("Overall");

	@Getter
	private final String name;

	public Skill toSkill() {
		if (this == OVERALL)
			return null;


		return Skill.valueOf(this.name.toUpperCase());
	}
}
