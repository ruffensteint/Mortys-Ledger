package com.ruffensteint.mortimerslayertracker.model;

public class LootItemRecord
{
	private int itemId;
	private String name;
	private int quantity;

	public LootItemRecord()
	{
	}

	public LootItemRecord(int itemId, String name, int quantity)
	{
		this.itemId = itemId;
		this.name = name;
		this.quantity = quantity;
	}

	public LootItemRecord(LootItemRecord other)
	{
		this(other.itemId, other.name, other.quantity);
	}

	public int getItemId()
	{
		return itemId;
	}

	public String getName()
	{
		return name;
	}

	public int getQuantity()
	{
		return quantity;
	}

	public void addQuantity(int amount)
	{
		quantity += amount;
	}
}
