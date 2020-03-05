package com.pablo67340.guishop.definition;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;

import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import com.github.stefvanschie.inventoryframework.Gui;
import com.github.stefvanschie.inventoryframework.GuiItem;
import com.github.stefvanschie.inventoryframework.pane.Pane;

import lombok.Getter;

import org.jetbrains.annotations.NotNull;

public class ShopPane extends Pane {

	private Map<Integer, GuiItem> items;
	@Getter
	private Map<Integer, ItemStack> dummies;
	public static ShopPane INSTANCE;

	public ShopPane(int length, int height) {
		super(length, height);
		items = new HashMap<>();
		dummies = new HashMap<>();
		INSTANCE = this;
	}

	public void addItem(GuiItem item) {
		items.put(items.size(), item);
	}

	public void setItem(GuiItem item, Integer slot) {
		items.put(slot, item);
	}

	@Override
	public void clear() {
		items.clear();
	}

	@Override
	public boolean click(@NotNull Gui arg0, @NotNull InventoryClickEvent event, int arg2, int arg3, int arg4,
			int arg5) {
		return false;
	}
	
	public void setDummy(Integer slot, ItemStack item) {
		dummies.put(slot, item);
	}

	@Override
	public void display(@NotNull Gui gui, @NotNull Inventory inventory, @NotNull PlayerInventory playerInventory,
			int paneOffsetX, int paneOffsetY, int maxLength, int maxHeight) {
		for (Entry<Integer, GuiItem> entry : items.entrySet()) {
			inventory.setItem(entry.getKey(), entry.getValue().getItem());
		}
		for (Entry<Integer, ItemStack> entry : dummies.entrySet()) {
			System.out.println("Entry: "+entry.getKey()+":"+entry.getValue());
			inventory.setItem(entry.getKey(), entry.getValue());
		}
	}

	@NotNull
	@Override
	public Collection<GuiItem> getItems() {
		return new ArrayList<>(items.values());
	}

	@NotNull
	@Override
	public Collection<Pane> getPanes() {
		return new HashSet<>();
	}

	public Map<Integer, GuiItem> getItemsMap() {
		return this.items;
	}

	public ShopPane getINSTANCE() {
		return INSTANCE;
	}

}
