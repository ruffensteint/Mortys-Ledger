package com.ruffensteint.mortimerslayertracker.model;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class SlayerHistoryTest
{
	@Test
	public void storesGearPreferencesByNormalizedMonsterName()
	{
		SlayerHistory history = new SlayerHistory();
		history.setWeaponPreference("Dark Beasts", 1_234);
		history.setShieldPreference("dark beasts", 5_678);
		history.setSlayerItemPreference("Dark Beasts", 9_101);
		history.setCannonPreference("Dark Beasts", true);

		MonsterGearPreference preference = history.getGearPreference("DARK BEASTS");
		assertNotNull(preference);
		assertEquals(1_234, preference.getWeaponItemId());
		assertEquals(5_678, preference.getShieldItemId());
		assertEquals(9_101, preference.getSlayerItemId());
		org.junit.Assert.assertTrue(preference.isCannonEnabled());
	}

	@Test
	public void copiesGearPreferencesIndependently()
	{
		SlayerHistory history = new SlayerHistory();
		history.setWeaponPreference("Basilisks", 100);

		SlayerHistory copy = new SlayerHistory(history);
		copy.setWeaponPreference("Basilisks", 200);

		assertEquals(100, history.getGearPreference("Basilisks").getWeaponItemId());
		assertEquals(200, copy.getGearPreference("Basilisks").getWeaponItemId());
	}
}
