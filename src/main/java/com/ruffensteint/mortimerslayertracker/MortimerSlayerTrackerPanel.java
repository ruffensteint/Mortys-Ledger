package com.ruffensteint.mortimerslayertracker;

import com.ruffensteint.mortimerslayertracker.model.SlayerHistory;
import com.ruffensteint.mortimerslayertracker.model.SlayerTaskRecord;
import com.ruffensteint.mortimerslayertracker.model.MonsterGearPreference;
import com.ruffensteint.mortimerslayertracker.service.DiscordWebhookClient;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Cursor;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.ListSelectionModel;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.Skill;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.SkillIconManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.AsyncBufferedImage;

public class MortimerSlayerTrackerPanel extends PluginPanel
{
	private static final int CARD_HEIGHT = 164;
	private static final int COMPACT_CARD_HEIGHT = 28;
	private static final int CARD_GAP = 8;
	private static final int FULL_HISTORY_TASKS = 5;

	private final JPanel currentTaskPanel = new JPanel();
	private final JPanel taskList = new JPanel();
	private final DiscordWebhookClient imageService;
	private final ItemManager itemManager;
	private final GearBadgeHandler gearBadgeHandler;
	private final Map<String, BufferedImage> monsterImages = new ConcurrentHashMap<>();
	private final Set<Integer> expandedOlderTasks = new HashSet<>();
	private final AsyncBufferedImage clueIcon;
	private final BufferedImage slayerIcon;
	private final AsyncBufferedImage heartIcon;
	private final AsyncBufferedImage cannonIcon;

	public MortimerSlayerTrackerPanel(ItemManager itemManager, SkillIconManager skillIconManager,
		DiscordWebhookClient imageService, GearBadgeHandler gearBadgeHandler)
	{
		super(false);
		this.itemManager = itemManager;
		this.imageService = imageService;
		this.gearBadgeHandler = gearBadgeHandler;
		clueIcon = itemManager.getImage(ItemID.TRAIL_CLUE_EASY_SIMPLE001);
		slayerIcon = skillIconManager.getSkillImage(Skill.SLAYER);
		heartIcon = itemManager.getImage(ItemID.IMBUED_HEART);
		cannonIcon = itemManager.getImage(ItemID.MCANNONBALL);
		clueIcon.onLoaded(this::repaint);
		heartIcon.onLoaded(this::repaint);
		cannonIcon.onLoaded(this::repaint);

		setLayout(new BorderLayout());
		setBackground(ColorScheme.DARK_GRAY_COLOR);

		JLabel title = new JLabel("Morty's Ledger", SwingConstants.CENTER);
		title.setForeground(ColorScheme.BRAND_ORANGE);
		title.setBorder(BorderFactory.createEmptyBorder(10, 4, 10, 4));
		title.setFont(title.getFont().deriveFont(Font.BOLD, 16f));
		add(title, BorderLayout.NORTH);

		JPanel content = new JPanel(new BorderLayout(0, 8));
		content.setBackground(ColorScheme.DARK_GRAY_COLOR);

		JPanel currentSection = new JPanel(new BorderLayout());
		currentSection.setBackground(ColorScheme.DARK_GRAY_COLOR);
		JLabel currentTitle = sectionTitle("Current Task");
		currentSection.add(currentTitle, BorderLayout.NORTH);
		currentTaskPanel.setLayout(new BoxLayout(currentTaskPanel, BoxLayout.Y_AXIS));
		currentTaskPanel.setBackground(ColorScheme.DARK_GRAY_COLOR);
		currentTaskPanel.setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 6));
		currentSection.add(currentTaskPanel, BorderLayout.CENTER);
		content.add(currentSection, BorderLayout.NORTH);

		taskList.setLayout(new BoxLayout(taskList, BoxLayout.Y_AXIS));
		taskList.setBackground(ColorScheme.DARK_GRAY_COLOR);
		taskList.setBorder(BorderFactory.createEmptyBorder(0, 6, 8, 6));

		JScrollPane scrollPane = new JScrollPane(taskList);
		scrollPane.setBorder(null);
		scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scrollPane.getVerticalScrollBar().setUnitIncrement(16);
		JPanel historySection = new JPanel(new BorderLayout());
		historySection.setBackground(ColorScheme.DARK_GRAY_COLOR);
		historySection.add(sectionTitle("Past Tasks"), BorderLayout.NORTH);
		historySection.add(scrollPane, BorderLayout.CENTER);
		content.add(historySection, BorderLayout.CENTER);
		add(content, BorderLayout.CENTER);
	}

	private static JLabel sectionTitle(String text)
	{
		JLabel label = new JLabel(text);
		label.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		label.setFont(label.getFont().deriveFont(Font.BOLD, 12f));
		label.setBorder(BorderFactory.createEmptyBorder(3, 7, 6, 7));
		return label;
	}

	public void updateHistory(SlayerHistory history, boolean thumbnailsEnabled)
	{
		SlayerHistory snapshot = new SlayerHistory(history);
		SwingUtilities.invokeLater(() -> rebuild(snapshot, thumbnailsEnabled));
	}

	public void updateCurrentTask(SlayerHistory history, boolean thumbnailsEnabled)
	{
		SlayerHistory snapshot = new SlayerHistory(history);
		SwingUtilities.invokeLater(() -> rebuildCurrentTask(snapshot, thumbnailsEnabled));
	}

	private void rebuild(SlayerHistory history, boolean thumbnailsEnabled)
	{
		rebuildCurrentTask(history, thumbnailsEnabled);
		rebuildTaskList(history, thumbnailsEnabled);
	}

	private void rebuildCurrentTask(SlayerHistory history, boolean thumbnailsEnabled)
	{
		currentTaskPanel.removeAll();
		SlayerTaskRecord activeTask = history.getActiveTask();
		if (activeTask == null)
		{
			JLabel empty = new JLabel("No active Mortimer task.", SwingConstants.CENTER);
			empty.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
			empty.setAlignmentX(CENTER_ALIGNMENT);
			empty.setBorder(BorderFactory.createEmptyBorder(12, 4, 12, 4));
			currentTaskPanel.add(empty);
		}
		else
		{
			currentTaskPanel.add(createTaskCard(activeTask, history, thumbnailsEnabled, false, true));
		}
		currentTaskPanel.revalidate();
		currentTaskPanel.repaint();
	}

	private void rebuildTaskList(SlayerHistory history, boolean thumbnailsEnabled)
	{
		taskList.removeAll();
		List<SlayerTaskRecord> completed = new ArrayList<>();
		for (SlayerTaskRecord task : history.getTasks())
		{
			if (task.isCompleted())
			{
				completed.add(task);
			}
		}
		Collections.reverse(completed);

		if (completed.isEmpty())
		{
			JLabel empty = new JLabel("No completed Mortimer tasks yet.", SwingConstants.CENTER);
			empty.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
			empty.setAlignmentX(CENTER_ALIGNMENT);
			empty.setBorder(BorderFactory.createEmptyBorder(24, 4, 4, 4));
			taskList.add(empty);
		}
		else
		{
			for (int index = 0; index < completed.size(); index++)
			{
				SlayerTaskRecord task = completed.get(index);
				boolean collapsible = index >= FULL_HISTORY_TASKS;
				boolean expanded = !collapsible || expandedOlderTasks.contains(task.getTaskNumber());
				taskList.add(createTaskCard(task, history, thumbnailsEnabled, collapsible, expanded));
				taskList.add(Box.createRigidArea(new Dimension(0, CARD_GAP)));
			}
		}
		taskList.revalidate();
		taskList.repaint();
	}

	private TaskCard createTaskCard(SlayerTaskRecord task, SlayerHistory history, boolean thumbnailsEnabled,
		boolean collapsible, boolean expanded)
	{
		MonsterGearPreference preference = history.getGearPreference(task.getMonster());
		AsyncBufferedImage weaponIcon = preference != null && preference.getWeaponItemId() >= 0
			? itemManager.getImage(preference.getWeaponItemId()) : null;
		AsyncBufferedImage shieldIcon = preference != null && preference.getShieldItemId() >= 0
			? itemManager.getImage(preference.getShieldItemId()) : null;
		AsyncBufferedImage slayerItemIcon = preference != null && preference.getSlayerItemId() >= 0
			? itemManager.getImage(preference.getSlayerItemId()) : null;
		TaskCard card = new TaskCard(task, modifierIcon(task), modifierMarker(task),
			weaponIcon, shieldIcon, slayerItemIcon, cannonIcon,
			preference != null && preference.isCannonEnabled(), thumbnailsEnabled, gearBadgeHandler,
			collapsible, expanded, isExpanded ->
			{
				if (isExpanded)
				{
					expandedOlderTasks.add(task.getTaskNumber());
				}
				else
				{
					expandedOlderTasks.remove(task.getTaskNumber());
				}
				taskList.revalidate();
				taskList.repaint();
			});
		if (weaponIcon != null)
		{
			weaponIcon.onLoaded(card::repaint);
		}
		if (shieldIcon != null)
		{
			shieldIcon.onLoaded(card::repaint);
		}
		if (slayerItemIcon != null)
		{
			slayerItemIcon.onLoaded(card::repaint);
		}
		if (thumbnailsEnabled)
		{
			loadMonsterImage(task.getMonster(), card);
		}
		return card;
	}

	public void chooseSlayerItem(String monster, List<ItemChoice> choices)
	{
		SwingUtilities.invokeLater(() ->
		{
			if (choices.isEmpty())
			{
				JOptionPane.showMessageDialog(this, "Carry or equip an item first.",
					"Morty's Ledger", JOptionPane.INFORMATION_MESSAGE);
				return;
			}
			JList<ItemChoice> itemList = new JList<>(choices.toArray(new ItemChoice[0]));
			itemList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			itemList.setFixedCellHeight(24);
			itemList.setSelectedIndex(0);
			itemList.setVisibleRowCount(Math.min(12, choices.size()));
			JScrollPane itemScrollPane = new JScrollPane(itemList);
			itemScrollPane.setPreferredSize(new Dimension(285, Math.min(260,
				Math.max(80, choices.size() * itemList.getFixedCellHeight()))));
			int result = JOptionPane.showConfirmDialog(this, itemScrollPane,
				"Choose Slayer item for " + monster, JOptionPane.OK_CANCEL_OPTION,
				JOptionPane.PLAIN_MESSAGE);
			if (result == JOptionPane.OK_OPTION && itemList.getSelectedValue() != null)
			{
				gearBadgeHandler.setSlayerItem(monster, itemList.getSelectedValue().getItemId());
			}
		});
	}

	private void loadMonsterImage(String monster, TaskCard card)
	{
		if (monster == null)
		{
			return;
		}
		String key = monster.toLowerCase(Locale.ENGLISH);
		BufferedImage cached = monsterImages.get(key);
		if (cached != null)
		{
			card.setMonsterImage(cached);
			return;
		}
		imageService.loadMonsterThumbnail(monster, image ->
		{
			if (image != null)
			{
				monsterImages.put(key, image);
				SwingUtilities.invokeLater(() -> card.setMonsterImage(image));
			}
		});
	}

	private BufferedImage modifierIcon(SlayerTaskRecord task)
	{
		if (task.isClueModifier())
		{
			return clueIcon;
		}
		if (task.isSlayerXpModifier())
		{
			return slayerIcon;
		}
		String modifier = task.getModifier() == null
			? "" : task.getModifier().toLowerCase(Locale.ENGLISH);
		if (modifier.contains("drop") || modifier.contains("superior"))
		{
			return heartIcon;
		}
		return null;
	}

	private String modifierMarker(SlayerTaskRecord task)
	{
		if ((task.isSlayerXpModifier() || task.isSlayerPointsModifier())
			&& task.getModifierValue() > 0)
		{
			return (task.isModifierNegative() ? "-" : "+") + task.getModifierValue();
		}
		return null;
	}

	private static class TaskCard extends JPanel
	{
		private static final int IMAGE_X = 7;
		private static final int IMAGE_Y = 28;
		private static final int IMAGE_HEIGHT = 128;
		private static final Color BADGE_COLOR = new Color(0, 0, 0, 190);

		private final SlayerTaskRecord task;
		private final BufferedImage modifierIcon;
		private final String modifierMarker;
		private final BufferedImage weaponIcon;
		private final BufferedImage shieldIcon;
		private final BufferedImage slayerItemIcon;
		private final BufferedImage cannonIcon;
		private final boolean cannonEnabled;
		private final GearBadgeHandler gearBadgeHandler;
		private final boolean thumbnailsEnabled;
		private final boolean collapsible;
		private final java.util.function.Consumer<Boolean> expansionHandler;
		private boolean expanded;
		private BufferedImage monsterImage;

		private TaskCard(SlayerTaskRecord task, BufferedImage modifierIcon, String modifierMarker,
			BufferedImage weaponIcon, BufferedImage shieldIcon, BufferedImage slayerItemIcon,
			BufferedImage cannonIcon, boolean cannonEnabled, boolean thumbnailsEnabled,
			GearBadgeHandler gearBadgeHandler, boolean collapsible, boolean expanded,
			java.util.function.Consumer<Boolean> expansionHandler)
		{
			this.task = new SlayerTaskRecord(task);
			this.modifierIcon = modifierIcon;
			this.modifierMarker = modifierMarker;
			this.weaponIcon = weaponIcon;
			this.shieldIcon = shieldIcon;
			this.slayerItemIcon = slayerItemIcon;
			this.cannonIcon = cannonIcon;
			this.cannonEnabled = cannonEnabled;
			this.gearBadgeHandler = gearBadgeHandler;
			this.thumbnailsEnabled = thumbnailsEnabled;
			this.collapsible = collapsible;
			this.expansionHandler = expansionHandler;
			this.expanded = expanded;
			setOpaque(false);
			setAlignmentX(LEFT_ALIGNMENT);
			updateCardSize();
			setToolTipText(task.getModifier());
			setComponentPopupMenu(createGearMenu(task.getMonster(), gearBadgeHandler));
			addMouseListener(new MouseAdapter()
			{
				@Override
				public void mouseClicked(MouseEvent event)
				{
					if (event.getButton() == MouseEvent.BUTTON1 && collapsible && event.getY() < IMAGE_Y)
					{
						TaskCard.this.expanded = !TaskCard.this.expanded;
						updateCardSize();
						expansionHandler.accept(TaskCard.this.expanded);
					}
					else if (event.getButton() == MouseEvent.BUTTON1 && expanded
						&& cannonBounds().contains(event.getPoint()))
					{
						gearBadgeHandler.toggleCannon(task.getMonster());
					}
				}
			});
			addMouseMotionListener(new MouseAdapter()
			{
				@Override
				public void mouseMoved(MouseEvent event)
				{
					setCursor((collapsible && event.getY() < IMAGE_Y)
						|| (expanded && cannonBounds().contains(event.getPoint()))
						? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR) : Cursor.getDefaultCursor());
				}
			});
		}

		private void updateCardSize()
		{
			int height = expanded ? CARD_HEIGHT : COMPACT_CARD_HEIGHT;
			setPreferredSize(new Dimension(PluginPanel.PANEL_WIDTH - 22, height));
			setMinimumSize(new Dimension(0, height));
			setMaximumSize(new Dimension(Integer.MAX_VALUE, height));
			revalidate();
			repaint();
		}

		private void setMonsterImage(BufferedImage monsterImage)
		{
			this.monsterImage = monsterImage;
			repaint();
		}

		@Override
		protected void paintComponent(Graphics graphics)
		{
			super.paintComponent(graphics);
			Graphics2D g = (Graphics2D) graphics.create();
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

			int imageWidth = getWidth() - IMAGE_X * 2;
			g.setColor(ColorScheme.DARKER_GRAY_COLOR);
			int cardHeight = expanded ? CARD_HEIGHT : COMPACT_CARD_HEIGHT;
			g.fillRoundRect(0, 0, getWidth(), cardHeight, 8, 8);
			g.setColor(ColorScheme.BORDER_COLOR);
			g.setStroke(new BasicStroke(1f));
			g.drawRoundRect(0, 0, getWidth() - 1, cardHeight - 1, 8, 8);

			g.setColor(ColorScheme.TEXT_COLOR);
			g.setFont(getFont().deriveFont(Font.BOLD, 13f));
			g.drawString("#" + task.getTaskNumber() + "  " + safe(task.getMonster()), 8, 19);
			if (collapsible)
			{
				g.setColor(ColorScheme.LIGHT_GRAY_COLOR);
				g.drawString(expanded ? "−" : "+", getWidth() - 18, 19);
			}
			if (!expanded)
			{
				g.dispose();
				return;
			}

			g.setClip(IMAGE_X, IMAGE_Y, imageWidth, IMAGE_HEIGHT);
			g.setColor(ColorScheme.MEDIUM_GRAY_COLOR);
			g.fillRect(IMAGE_X, IMAGE_Y, imageWidth, IMAGE_HEIGHT);
			if (monsterImage != null)
			{
				drawContainedImage(g, monsterImage, IMAGE_X, IMAGE_Y, imageWidth, IMAGE_HEIGHT);
			}
			else
			{
				g.setColor(ColorScheme.LIGHT_GRAY_COLOR);
				g.setFont(getFont().deriveFont(Font.PLAIN, 12f));
				drawCentered(g, thumbnailsEnabled ? "Loading image..." : "Wiki thumbnails disabled",
					IMAGE_X, IMAGE_Y, imageWidth, IMAGE_HEIGHT);
			}
			g.setClip(null);

			drawBadge(g, "x" + task.getAssignedAmount(), IMAGE_X + 5, IMAGE_Y + IMAGE_HEIGHT - 7, false);
			int totalXp = task.getBaseSlayerXp() + task.getBonusSlayerXp();
			drawBadge(g, formatXp(totalXp), IMAGE_X + imageWidth - 5, IMAGE_Y + 18, true);

			if (modifierIcon != null)
			{
				int iconSize = 28;
				int iconX = IMAGE_X + imageWidth - iconSize - 5;
				int iconY = IMAGE_Y + IMAGE_HEIGHT - iconSize - 5;
				g.setColor(BADGE_COLOR);
				g.fillRoundRect(iconX - 3, iconY - 3, iconSize + 6, iconSize + 6, 7, 7);
				g.drawImage(modifierIcon, iconX, iconY, iconSize, iconSize, null);
				drawModifierValue(g, modifierMarker, iconX + iconSize / 2, iconY + 2);
			}
			else if (modifierMarker != null)
			{
				drawBadge(g, modifierMarker, IMAGE_X + imageWidth - 5,
					IMAGE_Y + IMAGE_HEIGHT - 8, true);
			}
			drawGearBadge(g, weaponIcon, "W", IMAGE_X + 48, IMAGE_Y + IMAGE_HEIGHT - 43);
			drawGearBadge(g, shieldIcon, "S", IMAGE_X + 90, IMAGE_Y + IMAGE_HEIGHT - 43);
			drawGearBadge(g, slayerItemIcon, "I", IMAGE_X + 132, IMAGE_Y + IMAGE_HEIGHT - 43);
			drawCannonBadge(g);
			g.dispose();
		}

		private static void drawGearBadge(Graphics2D g, BufferedImage icon, String label, int x, int y)
		{
			if (icon == null)
			{
				return;
			}
			g.setColor(BADGE_COLOR);
			g.fillRoundRect(x, y, 40, 40, 8, 8);
			g.setColor(ColorScheme.LIGHT_GRAY_COLOR);
			g.drawRoundRect(x, y, 39, 39, 8, 8);
			g.drawImage(icon, x + 4, y + 4, 32, 32, null);
			g.setColor(ColorScheme.BRAND_ORANGE);
			g.fillOval(x - 4, y - 4, 15, 15);
			g.setColor(Color.BLACK);
			g.setFont(g.getFont().deriveFont(Font.BOLD, 10f));
			g.drawString(label, x, y + 7);
		}

		private static void drawModifierValue(Graphics2D g, String value, int centerX, int baseline)
		{
			if (value == null)
			{
				return;
			}
			g.setFont(g.getFont().deriveFont(Font.BOLD, 11f));
			FontMetrics metrics = g.getFontMetrics();
			int width = metrics.stringWidth(value) + 8;
			int x = centerX - width / 2;
			g.setColor(BADGE_COLOR);
			g.fillRoundRect(x, baseline - metrics.getAscent() - 3, width, metrics.getHeight() + 2, 6, 6);
			g.setColor(ColorScheme.BRAND_ORANGE);
			g.drawString(value, x + 4, baseline);
		}

		private void drawCannonBadge(Graphics2D g)
		{
			Rectangle bounds = cannonBounds();
			g.setColor(BADGE_COLOR);
			g.fillRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, 8, 8);
			g.setColor(ColorScheme.LIGHT_GRAY_COLOR);
			g.drawRoundRect(bounds.x, bounds.y, bounds.width - 1, bounds.height - 1, 8, 8);
			g.drawImage(cannonIcon, bounds.x + 3, bounds.y + 3, 28, 28, null);
			g.setColor(cannonEnabled ? new Color(46, 204, 113) : new Color(231, 76, 60));
			g.fillOval(bounds.x + 23, bounds.y - 4, 16, 16);
			g.setColor(Color.WHITE);
			g.setFont(g.getFont().deriveFont(Font.BOLD, 12f));
			g.drawString(cannonEnabled ? "✓" : "X", bounds.x + 27, bounds.y + 8);
		}

		private Rectangle cannonBounds()
		{
			return new Rectangle(IMAGE_X + 5, IMAGE_Y + IMAGE_HEIGHT - 78, 34, 34);
		}

		private static JPopupMenu createGearMenu(String monster, GearBadgeHandler handler)
		{
			JPopupMenu menu = new JPopupMenu();
			JMenuItem weapon = new JMenuItem("Use equipped weapon");
			weapon.addActionListener(event -> handler.captureWeapon(monster));
			menu.add(weapon);
			JMenuItem shield = new JMenuItem("Use equipped shield");
			shield.addActionListener(event -> handler.captureShield(monster));
			menu.add(shield);
			menu.addSeparator();
			JMenuItem clearWeapon = new JMenuItem("Clear weapon badge");
			clearWeapon.addActionListener(event -> handler.clearWeapon(monster));
			menu.add(clearWeapon);
			JMenuItem clearShield = new JMenuItem("Clear shield badge");
			clearShield.addActionListener(event -> handler.clearShield(monster));
			menu.add(clearShield);
			menu.addSeparator();
			JMenuItem slayerItem = new JMenuItem("Choose carried/equipped Slayer item");
			slayerItem.addActionListener(event -> handler.chooseSlayerItem(monster));
			menu.add(slayerItem);
			JMenuItem clearSlayerItem = new JMenuItem("Clear Slayer item badge");
			clearSlayerItem.addActionListener(event -> handler.clearSlayerItem(monster));
			menu.add(clearSlayerItem);
			return menu;
		}

		private static void drawContainedImage(Graphics2D g, BufferedImage image,
			int x, int y, int width, int height)
		{
			double scale = Math.min((double) width / image.getWidth(), (double) height / image.getHeight());
			int drawWidth = Math.max(1, (int) Math.round(image.getWidth() * scale));
			int drawHeight = Math.max(1, (int) Math.round(image.getHeight() * scale));
			int drawX = x + (width - drawWidth) / 2;
			int drawY = y + (height - drawHeight) / 2;
			g.drawImage(image, drawX, drawY, drawWidth, drawHeight, null);
		}

		private static void drawCentered(Graphics2D g, String text, int x, int y, int width, int height)
		{
			FontMetrics metrics = g.getFontMetrics();
			g.drawString(text, x + (width - metrics.stringWidth(text)) / 2,
				y + (height + metrics.getAscent()) / 2);
		}

		private static void drawBadge(Graphics2D g, String text, int anchorX, int baseline, boolean rightAligned)
		{
			g.setFont(g.getFont().deriveFont(Font.BOLD, 12f));
			FontMetrics metrics = g.getFontMetrics();
			int width = metrics.stringWidth(text) + 10;
			int x = rightAligned ? anchorX - width : anchorX;
			g.setColor(BADGE_COLOR);
			g.fillRoundRect(x, baseline - metrics.getAscent() - 4, width, metrics.getHeight() + 4, 7, 7);
			g.setColor(Color.WHITE);
			g.drawString(text, x + 5, baseline);
		}

		private static String formatXp(int xp)
		{
			return String.format(Locale.ENGLISH, "%,d XP", xp);
		}

		private static String safe(String value)
		{
			return value == null || value.trim().isEmpty() ? "Unknown" : value;
		}
	}

	public interface GearBadgeHandler
	{
		void captureWeapon(String monster);

		void captureShield(String monster);

		void clearWeapon(String monster);

		void clearShield(String monster);

		void chooseSlayerItem(String monster);

		void setSlayerItem(String monster, int itemId);

		void clearSlayerItem(String monster);

		void toggleCannon(String monster);
	}

	public static class ItemChoice
	{
		private final int itemId;
		private final String name;

		public ItemChoice(int itemId, String name)
		{
			this.itemId = itemId;
			this.name = name;
		}

		public int getItemId()
		{
			return itemId;
		}

		@Override
		public String toString()
		{
			return name;
		}
	}
}
