package com.pablo67340.guishop.listenable;

import java.util.*;
import java.util.logging.Level;

import org.apache.commons.lang.StringUtils;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitScheduler;

import com.github.stefvanschie.inventoryframework.Gui;
import com.github.stefvanschie.inventoryframework.GuiItem;
import com.github.stefvanschie.inventoryframework.pane.PaginatedPane;
import com.github.stefvanschie.inventoryframework.shade.mininbt.ItemNBTUtil;
import com.github.stefvanschie.inventoryframework.shade.mininbt.NBTWrappers.NBTTagCompound;

import com.pablo67340.guishop.definition.Item;
import com.pablo67340.guishop.definition.ItemType;
import com.pablo67340.guishop.definition.Price;
import com.pablo67340.guishop.definition.ShopPane;
import com.pablo67340.guishop.Main;
import com.pablo67340.guishop.util.Config;
import com.pablo67340.guishop.util.MatLib;
import com.pablo67340.guishop.util.XEnchantment;
import com.pablo67340.guishop.util.XMaterial;

import lombok.Getter;

public class Shop {

	/**
	 * The name of this {@link Shop}.
	 */
	@Getter
	private final String name;

	/**
	 * The shop name of this {@link Shop}.
	 */
	@Getter
	private final String shop;

	/**
	 * The description of this {@link Shop}.
	 */
	@Getter
	private final String description;

	/**
	 * The lore of this {@link Shop}.
	 */
	@Getter
	private final List<String> lore;

	/**
	 * The list of {@link Item}s in this {@link Shop}.
	 */
	@Getter
	private List<Item> items;

	/**
	 * The list of {@link Page}'s in this {@link Shop}.
	 */
	private Gui GUI;

	private Menu menuInstance;

	private Boolean hasClicked = false;

	private int pageC = 0;

	private PaginatedPane currentPane;

	int oldPage = 0;

	private Integer lastIndex = 0;

	/**
	 * The constructor for a {@link Shop}.
	 *
	 * @param name        The name of the shop.
	 * @param description The description of the shop.
	 * @param lore        The lore of the shop.
	 */
	Shop(String shop, String name, String description, List<String> lore, Menu menuInstance) {
		this.name = name;
		this.shop = shop;
		this.description = description;
		this.lore = lore;
		this.menuInstance = menuInstance;
	}

	/**
	 * The constructor for a {@link Shop}.
	 *
	 * @param name        The name of the shop.
	 * @param description The description of the shop.
	 * @param lore        The lore of the shop.
	 */
	public Shop(String shop, String name, String description, List<String> lore) {
		this.name = name;
		this.shop = shop;
		this.description = description;
		this.lore = lore;
		this.menuInstance = null;
	}

	/**
	 * The constructor for a {@link Shop}.
	 *
	 * @param name        The name of the shop.
	 * @param description The description of the shop.
	 * @param lore        The lore of the shop.
	 */
	Shop(String shop, String name, String description, List<String> lore, Menu menuInstance, List<Item> items) {
		this.name = name;
		this.shop = shop;
		this.description = description;
		this.lore = lore;
		this.menuInstance = menuInstance;
		this.items = items;
	}

	/**
	 * Load the specified shop
	 */
	public void loadItems() {

		if (items == null) {

			items = new ArrayList<>();

			ConfigurationSection config = Main.getINSTANCE().getCustomConfig().getConfigurationSection(shop);

			assert config != null;
			for (String str : config.getKeys(false)) {

				Item item = new Item();

				ConfigurationSection section = config.getConfigurationSection(str);

				item.setSlot((Integer.parseInt(str)));

				item.setMaterial((section.contains("id") ? (String) section.get("id") : "AIR"));
				item.setMobType((section.contains("mobType") ? (String) section.get("mobType") : null));
				item.setShopName((section.contains("shop-name") ? (String) section.get("shop-name") : null));
				item.setBuyName((section.contains("buy-name") ? (String) section.get("buy-name") : null));
				if (section.contains("enchantments")) {
					String enchantments = section.getString("enchantments");
					if (!enchantments.equalsIgnoreCase(" ")) {
						item.setEnchantments(enchantments.split(" "));
					}
				}

				item.setBuyPrice((section.contains("buy-price") ? section.get("buy-price") : false));

				item.setSellPrice((section.contains("sell-price") ? section.get("sell-price") : false));

				item.setItemType(
						section.contains("type") ? ItemType.valueOf((String) section.get("type")) : ItemType.SHOP);

				item.setShopLore(
						(section.contains("shop-lore") ? section.getStringList("shop-lore") : new ArrayList<>()));
				item.setBuyLore((section.contains("buy-lore") ? section.getStringList("buy-lore") : new ArrayList<>()));
				item.setCommands(
						(section.contains("commands") ? section.getStringList("commands") : new ArrayList<>()));

				if (!Main.getINSTANCE().getPRICETABLE().containsKey(item.getMaterial())
						&& (!(item.getSellPrice() instanceof Boolean))) {
					Double sellPrice = item.getSellPrice() instanceof Integer
							? ((Integer) item.getSellPrice()).doubleValue()
							: ((Double) item.getSellPrice());

					if (item.isMobSpawner()) {
						Main.getINSTANCE().getPRICETABLE()
								.put(item.getMaterial() + ":" + item.getMobType().toLowerCase(), new Price(sellPrice));
					} else {
						Main.getINSTANCE().getPRICETABLE().put(item.getMaterial(), new Price(sellPrice));
					}
				}
				items.add(item);

			}
			loadShop();

		} else {
			loadShop();
		}

	}

	private void loadShop() {
		Integer index = 0;
		ShopPane page = new ShopPane(9, 6);

		this.GUI = new Gui(Main.getINSTANCE(), 6,
				ChatColor.translateAlternateColorCodes('&', "Menu &f> &r") + getName());
		PaginatedPane pane = new PaginatedPane(0, 0, 9, 6);

		for (Item item : items) {

			ItemStack itemStack;
			GuiItem gItem = null;
			
			
			itemStack = XMaterial.matchXMaterial(item.getMaterial()).get().parseItem();

			try {
				gItem = new GuiItem(itemStack);
			} catch (Exception ex2) {
				if (Config.isDebugMode()) {
					Main.log("Failed to find item by Material: " + item.getMaterial() + ". Attempting OFF Fix...");
				}
				try {
					itemStack = new ItemStack(Material.valueOf(item.getMaterial() + "_OFF"));
					gItem = new GuiItem(itemStack);
				} catch (Exception ex3) {
					if (Config.isDebugMode()) {
						Main.log("OFF Fix for: " + item.getMaterial() + " Failed. Attempting ItemID Lookup...");
					}

					// Final Stand, lets try to find this user's item
					String itemID = MatLib.getMAP().get(item.getMaterial());
					String[] idParts = itemID.split(":");
					Integer id = Integer.parseInt(idParts[0]);
					short data = Short.parseShort(idParts[1]);
					// Use for comment injection method for forward & backward compat
					itemStack = new ItemStack(id, 1, (short) data);

					try {
						gItem = new GuiItem(itemStack);
					} catch (Exception ex4) {
						if (Config.isDebugMode()) {
							Main.log("ItemID Fix for: " + item.getMaterial() + " Failed. Falling back to air.");
						}
						
						item.setItemType(ItemType.BLANK);
						item.setEnchantments(null);
					}

				}
			}
			
			
			// Checks if an item is either a shop item or command item. This also handles
			// Null items as there is a item type switch in the lines above.
			if (item.getItemType() == ItemType.SHOP || item.getItemType() == ItemType.COMMAND) {

				ItemMeta itemMeta = gItem.getItem().getItemMeta();

				List<String> lore = new ArrayList<>();

				if (item.hasBuyPrice()) {
					if (item.getBuyPrice() instanceof Double) {
						if ((Double) item.getBuyPrice() != 0.0) {
							lore.add(Config.getBuyLore().replace("{amount}",
									Config.getCurrency() + item.getBuyPrice() + Config.getCurrencySuffix()));
						} else {
							lore.add(Config.getFreeLore());
						}
					} else {
						if ((Integer) item.getBuyPrice() != 0) {
							lore.add(Config.getBuyLore().replace("{amount}", Config.getCurrency()
									+ ((Integer) item.getBuyPrice()).doubleValue() + Config.getCurrencySuffix()));
						} else {
							lore.add(Config.getFreeLore());
						}

					}
				} else {
					lore.add(Config.getCannotBuy());
				}

				if (item.hasSellPrice()) {
					lore.add(Config.getSellLore().replace("{amount}",
							Config.getCurrency() + item.getSellPrice() + Config.getCurrencySuffix()));
				} else {
					lore.add(Config.getCannotSell());
				}

				if (item.hasShopLore()) {
					item.getShopLore().forEach(str -> {
						if (!lore.contains(str) && !lore
								.contains(Config.getBuyLore().replace("{AMOUNT}", item.getBuyPrice().toString()))) {
							lore.add(ChatColor.translateAlternateColorCodes('&', str));
						}
					});
				}

				if (!lore.isEmpty()) {
					assert itemMeta != null;
					itemMeta.setLore(lore);
				}

				if (item.hasShopName()) {
					assert itemMeta != null;
					itemMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', item.getShopName()));
				} else if (item.isMobSpawner()) {
					String mobName = item.getMobType();
					mobName = mobName.toLowerCase();
					mobName = mobName.substring(0, 1).toUpperCase() + mobName.substring(1).replace("_", " ");
					assert itemMeta != null;
					itemMeta.setDisplayName(mobName + " Spawner");
				}

				gItem.getItem().setItemMeta(itemMeta);

				if (item.getEnchantments() != null) {
					if (item.getEnchantments().length > 1) {
						for (String enc : item.getEnchantments()) {
							String enchantment = StringUtils.substringBefore(enc, ":");
							String level = StringUtils.substringAfter(enc, ":");
							gItem.getItem().addUnsafeEnchantment(
									XEnchantment.matchXEnchantment(enchantment).get().parseEnchantment(),
									Integer.parseInt(level));
						}
					}
				}

			}

			// Create Page
			if (index == items.size() || ((index) - lastIndex) == 44) {
				if (item.getItemType() == ItemType.SHOP || item.getItemType() == ItemType.COMMAND) {
					page.setItem(gItem, item.getSlot());
				}

				if (items.size() > 45) {
					applyButtons(page);
				}
				lastIndex = index;
				pane.addPane(pageC, page);
				pageC += 1;
				page = new ShopPane(9, 6);
			} else {
				if (pageC == 0) {
					if (item.getItemType() == ItemType.SHOP || item.getItemType() == ItemType.COMMAND) {
						page.setItem(gItem, item.getSlot());
					} else {
						page.setDummy(item.getSlot(), new ItemStack(Material.AIR));
					}
				} else {

					if (item.getItemType() == ItemType.SHOP || item.getItemType() == ItemType.COMMAND) {
						page.setItem(gItem, item.getSlot() - lastIndex - 1);
					} else {
						page.setDummy(item.getSlot() - lastIndex - 1, new ItemStack(Material.AIR));
					}
				}
			}

			if (index + 1 == items.size()) {
				pane.addPane(pageC, page);
				applyButtons(page);
				GUI.addPane(pane);
				Main.getINSTANCE().getLoadedShops().put(name, items);
			}
			index += 1;

		}

		this.currentPane = pane;

	}

	private void applyButtons(ShopPane page) {
		if (page.getINSTANCE().getItemsMap().containsKey(44)) {
			page.setItem(new GuiItem(new ItemStack(Material.ARROW)), 51);
		}
		if (pageC > 0) {
			page.setItem(new GuiItem(new ItemStack(Material.ARROW)), 47);
		}
		if (!Config.isEscapeOnly()) {

			ItemStack backButtonItem = new ItemStack(
					Objects.requireNonNull(XMaterial.matchXMaterial(Config.getBackButtonItem()).get().parseMaterial()));

			ItemMeta backButtonMeta = backButtonItem.getItemMeta();

			assert backButtonMeta != null;
			backButtonMeta.setDisplayName(Config.getBackButtonText());

			backButtonItem.setItemMeta(backButtonMeta);

			GuiItem item = new GuiItem(backButtonItem);

			page.setItem(item, 53);
		}
	}

	/**
	 * Open the player's shop
	 */
	public void open(Player input) {
		// currentPane.setPage(0);
		GUI.show(input);
		if (!Main.getCREATOR().contains(input.getName())) {
			GUI.setOnBottomClick(event -> {
				event.setCancelled(true);
			});
		}
		GUI.setOnClose(this::onClose);
		GUI.setOnTopClick(this::onShopClick);

	}

	private void onShopClick(InventoryClickEvent e) {
		Player player = (Player) e.getWhoClicked();
		if (!Main.getCREATOR().contains(player.getName())) {
			e.setCancelled(true);
			hasClicked = true;
		}

		if (e.getCurrentItem() == null || e.getCurrentItem().getType() == Material.AIR) {
			return;
		}

		// Forward Button
		if (e.getSlot() == 51) {
			hasClicked = true;
			if (Main.getCREATOR().contains(player.getName())) {
				ItemStack[] items = GUI.getInventory().getContents();

				int slot = 0;
				for (ItemStack item : items) {
					((ShopPane) currentPane.getPanes().toArray()[currentPane.getPage()]).setItem(new GuiItem(item),
							slot);
					slot += 1;
				}

				saveShop(player);
			}
			((ShopPane) currentPane.getPanes().toArray()[currentPane.getPage()]).setVisible(false);
			currentPane.setPage(currentPane.getPage() + 1);

			((ShopPane) currentPane.getPanes().toArray()[currentPane.getPage()]).setVisible(true);
			GUI.update();
			return;
			// Backward Button
		} else if (e.getSlot() == 47) {
			hasClicked = true;

			if (Main.getCREATOR().contains(player.getName())) {
				ItemStack[] items = GUI.getInventory().getContents();

				int slot = 0;
				for (ItemStack item : items) {
					((ShopPane) currentPane.getPanes().toArray()[currentPane.getPage()]).setItem(new GuiItem(item),
							slot);
					slot += 1;
				}
				saveShop(player);
			}
			((ShopPane) currentPane.getPanes().toArray()[currentPane.getPage()]).setVisible(false);
			currentPane.setPage(currentPane.getPage() - 1);

			((ShopPane) currentPane.getPanes().toArray()[currentPane.getPage()]).setVisible(true);
			GUI.update();
			return;
			// Back Button
		} else if (e.getSlot() == 53 && !Config.isEscapeOnly()) {
			if (menuInstance != null && !Main.getCREATOR().contains(player.getName())) {
				menuInstance.open(player);
			}
			return;
		}

		/*
		 * If the player's inventory is full
		 */
		if (player.getInventory().firstEmpty() == -1) {
			player.sendMessage(Config.getFull());
			return;
		}

		/*
		 * If the player has enough money to purchase the item, then allow them to.
		 */
		if (!Main.getCREATOR().contains(player.getName())) {
			Item item = getItems().get((currentPane.getPage() * 45) + e.getSlot());

			if (item.getItemType() == ItemType.SHOP && item.hasBuyPrice()) {
				new Quantity(item, this, player).loadInventory().open();
			} else if (item.getItemType() == ItemType.COMMAND) {
				if (Main.getECONOMY().withdrawPlayer(player, (Double) item.getBuyPrice()).transactionSuccess()) {
					item.getCommands().forEach(str -> {
						Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(),
								Main.placeholderIfy(str, player, item));
					});
				} else {
					player.sendMessage(Config.getPrefix() + Config.getNotEnoughPre() + item.getBuyPrice()
							+ Config.getNotEnoughPost());
				}
			} else {
				player.sendMessage(Config.getPrefix() + " " + Config.getCannotBuy());
			}
		}

	}

	/**
	 * The inventory closeEvent handling for the Menu.
	 */
	private void onClose(InventoryCloseEvent e) {
		Player player = (Player) e.getPlayer();
		if (!Main.CREATOR.contains(player.getName())) {
			if (Config.isEscapeOnly() && !hasClicked) {
				BukkitScheduler scheduler = Bukkit.getServer().getScheduler();
				scheduler.scheduleSyncDelayedTask(Main.getINSTANCE(), () -> menuInstance.open(player), 1L);

			} else {
				hasClicked = false;
			}
		} else {
			if (!hasClicked) {
				saveShop(player);
			} else {
				hasClicked = false;
			}
		}

	}

	private void saveShop(Player input) {
		Main.getINSTANCE().getCustomConfig().set(shop, null);
		Main.getINSTANCE().getCustomConfig().createSection(shop);
		int slots = items.size() - 1;
		int mult;

		if (currentPane.getPage() == 0) {
			mult = (currentPane.getPage() + 1) * 44;
		} else {
			mult = (currentPane.getPage()) * 44;
		}
		int pageSlots = 0;
		int pageItemCounter = 0;

		pageSlots = slots - mult - 1;

		for (Integer slot = 0; slot <= pageSlots; slot++) {

			if (currentPane.getPage() == 0) {
				pageItemCounter = slot;
			} else {
				pageItemCounter = slot + mult + 1;
			}

			ItemStack itemStack = GUI.getInventory().getItem(slot);

			Item item = items.get(pageItemCounter) != null ? items.get(pageItemCounter) : new Item();
			if (itemStack != null) {
				if (!item.getMaterial().equalsIgnoreCase(itemStack.getType().toString())
						|| item.getItemType() == null) {
					item = new Item();
				}
				NBTTagCompound comp = ItemNBTUtil.getTag(itemStack);
				ItemMeta im = itemStack.getItemMeta();

				item.setItemType(ItemType.SHOP);
				item.setMaterial(itemStack.getType().toString());
				item.setSlot(pageItemCounter);
				if (comp.hasKey("buyPrice")) {
					Object buyPrice = getBuyPrice(itemStack);

					if (buyPrice instanceof Boolean) {
						item.setBuyPrice((Boolean) buyPrice);
					} else {
						item.setBuyPrice((Double) buyPrice);
					}
				}
				if (comp.hasKey("sellPrice")) {
					Object sellPrice = getSellPrice(itemStack);
					if (sellPrice instanceof Boolean) {
						item.setSellPrice((Boolean) sellPrice);
					} else {
						item.setSellPrice((Double) sellPrice);
					}
				}

				if (im.hasDisplayName()) {
					item.setShopName(im.getDisplayName());
				}

				if (comp.hasKey("buyName")) {
					item.setBuyName(comp.getString("buyName"));
				}

				if (comp.hasKey("enchantments")) {
					item.setEnchantments(comp.getString("enchantments").split(" "));
				}

				if (comp.hasKey("itemType")) {
					item.setItemType(ItemType.valueOf(comp.getString("itemType")));
				}

				if (comp.hasKey("commands")) {
					item.setItemType(ItemType.COMMAND);
					item.setCommands(Arrays.asList(comp.getString("commands").split("::")));
				}

				if (comp.hasKey("mobType")) {
					item.setMobType(comp.getString("mobType"));
				}

				if (im.hasLore()) {
					List<String> lore = im.getLore();
					List<String> cleaned = new ArrayList<>();
					for (String str : lore) {
						if (!(ChatColor.stripColor(str)
								.contains(ChatColor.stripColor(Config.getBuyLore().replace("{amount}", ""))))
								&& !(ChatColor.stripColor(str)
										.contains(ChatColor.stripColor(Config.getSellLore().replace("{amount}", ""))))
								&& !(ChatColor.stripColor(str).contains(ChatColor.stripColor(Config.getCannotBuy())))
								&& !(ChatColor.stripColor(str)
										.contains(ChatColor.stripColor(Config.getCannotSell())))) {
							cleaned.add(str);
						}
					}
					item.setShopLore(cleaned);
				}

				if (comp.hasKey("loreLines")) {
					String line = comp.getString("loreLines");
					String[] lore = line.split("::");
					item.setBuyLore(Arrays.asList(lore));
				}

				items.set(item.getSlot(), item);
			} else {
				Item blank = new Item();
				blank.setItemType(ItemType.BLANK);
				items.set(pageItemCounter, blank);
			}
		}
		saveItems(input);
	}

	/**
	 * Load the specified shop
	 */
	public void saveItems(Player player) {

		Main.getINSTANCE().getCustomConfig().set(shop, null);

		ConfigurationSection config = Main.getINSTANCE().getCustomConfig().createSection(shop);

		assert config != null;
		for (Item item : items) {
			config.set(item.getSlot() + "", null);
			ConfigurationSection section = config.createSection(item.getSlot() + "");
			section.set("type", item.getItemType().toString());
			section.set("id", item.getMaterial());
			if (item.hasBuyPrice()) {
				section.set("buy-price", item.getBuyPrice());
			}
			if (item.hasSellPrice()) {
				section.set("sell-price", item.getSellPrice());
			}
			if (item.hasShopLore()) {
				section.set("shop-lore", item.getShopLore());
			}
			if (item.hasShopName()) {
				section.set("shop-name", item.getShopName());
			}
			if (item.hasBuyName()) {
				section.set("buy-name", item.getBuyName());
			}
			if (item.hasEnchantments()) {
				String parsed = "";
				for (String str : item.getEnchantments()) {
					parsed += str + " ";
				}
				section.set("enchantments", parsed.trim());
			}
			if (item.hasShopLore()) {
				section.set("shop-lore", item.getShopLore());
			}
			if (item.hasBuyLore()) {
				section.set("buy-lore", item.getBuyLore());
			}
			if (item.hasCommands()) {
				section.set("commands", item.getCommands());
			}
			if (item.hasMobType()) {
				section.set("mobType", item.getMobType());
			}
		}
		try {
			Main.getINSTANCE().getCustomConfig().save(Main.getINSTANCE().getSpecialf());

		} catch (Exception ex) {
			Main.getINSTANCE().getLogger().log(Level.WARNING, ex.getMessage());
		}

		if (!hasClicked) {
			Main.getCREATOR().remove(player.getName());
		}
		player.sendMessage("�aShop Saved!");

	}

	private Object getBuyPrice(ItemStack item) {
		NBTTagCompound comp = ItemNBTUtil.getTag(item);

		if (comp.hasKey("buyPrice")) {
			Double vl = comp.getDouble("buyPrice");
			return vl;
		} else {
			return false;
		}

	}

	private Object getSellPrice(ItemStack item) {
		NBTTagCompound comp = ItemNBTUtil.getTag(item);

		if (comp.hasKey("sellPrice")) {
			Double vl = comp.getDouble("sellPrice");
			return vl;
		} else {
			return false;
		}
	}

}
