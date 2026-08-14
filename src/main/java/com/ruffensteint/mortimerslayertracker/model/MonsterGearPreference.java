package com.ruffensteint.mortimerslayertracker.model;

public class MonsterGearPreference
{
	private int weaponItemId = -1;
	private int shieldItemId = -1;
	private int slayerItemId = -1;
	private boolean cannonEnabled;

	public MonsterGearPreference()
	{
	}

	public MonsterGearPreference(MonsterGearPreference other)
	{
		weaponItemId = other.weaponItemId;
		shieldItemId = other.shieldItemId;
		slayerItemId = other.slayerItemId;
		cannonEnabled = other.cannonEnabled;
	}

	public int getWeaponItemId()
	{
		return weaponItemId;
	}

	public void setWeaponItemId(int weaponItemId)
	{
		this.weaponItemId = weaponItemId;
	}

	public int getShieldItemId()
	{
		return shieldItemId;
	}

	public void setShieldItemId(int shieldItemId)
	{
		this.shieldItemId = shieldItemId;
	}

	public int getSlayerItemId()
	{
		return slayerItemId;
	}

	public void setSlayerItemId(int slayerItemId)
	{
		this.slayerItemId = slayerItemId;
	}

	public boolean isCannonEnabled()
	{
		return cannonEnabled;
	}

	public void setCannonEnabled(boolean cannonEnabled)
	{
		this.cannonEnabled = cannonEnabled;
	}
}
