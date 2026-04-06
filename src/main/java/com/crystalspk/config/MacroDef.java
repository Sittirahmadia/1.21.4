package com.crystalspk.config;

import java.util.List;

public class MacroDef {
    public final String id;
    public final String name;
    public final String category;
    public final int defaultDelay;
    public final List<String> slotNames;

    public MacroDef(String id, String name, String category, int defaultDelay, List<String> slotNames) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.defaultDelay = defaultDelay;
        this.slotNames = slotNames;
    }

    public static final List<MacroDef> ALL = List.of(
        new MacroDef("sa",  "Single Anchor",       "crystal", 27, List.of("anchorSlot", "glowstoneSlot", "explodeSlot")),
        new MacroDef("da",  "Double Anchor",       "crystal", 48, List.of("anchorSlot", "glowstoneSlot", "explodeSlot")),
        new MacroDef("ap",  "Anchor Pearl",        "crystal", 25, List.of("anchorSlot", "glowstoneSlot", "explodeSlot", "pearlSlot")),
        new MacroDef("hc",  "Hit Crystal",         "crystal", 1,  List.of("obsidianSlot", "crystalSlot")),
        new MacroDef("ac",  "Auto Crystal",        "crystal", 25, List.of("crystalSlot")),
        new MacroDef("kp",  "Key Pearl",           "crystal", 30, List.of("pearlSlot", "returnSlot")),
        new MacroDef("idh", "Inventory D-Hand",    "crystal", 25, List.of("totemSlot", "swapSlot")),
        new MacroDef("oht", "Offhand Totem",       "crystal", 35, List.of("totemSlot", "swapSlot")),
        new MacroDef("fxp", "Fast XP",             "crystal", 35, List.of()),
        new MacroDef("sr",  "Sprint Reset",        "sword",   35, List.of()),
        new MacroDef("asb", "Auto Shield Breaker", "sword",   35, List.of("axeSlot", "swordSlot")),
        new MacroDef("ls",  "Lunge Swap",          "sword",   0,  List.of("swordSlot", "spearSlot")),
        new MacroDef("es",  "Elytra Swap",         "mace",    50, List.of("elytraSlot", "returnSlot")),
        new MacroDef("pc",  "Pearl Catch",         "mace",    50, List.of("pearlSlot", "windChargeSlot")),
        new MacroDef("ss",  "Stun Slam",           "mace",    10, List.of("axeSlot", "maceSlot")),
        new MacroDef("bs",  "Breach Swap",         "mace",    25, List.of("maceSlot", "swordSlot")),
        new MacroDef("ic",  "Insta Cart",          "cart",    50, List.of("railSlot", "bowSlot", "cartSlot")),
        new MacroDef("xb",  "Crossbow Cart",       "cart",    50, List.of("railSlot", "cartSlot", "fnsSlot", "crossbowSlot")),
        new MacroDef("dr",  "Drain",               "uhc",     30, List.of("bucketSlot")),
        new MacroDef("lw",  "Lava Web",            "uhc",     30, List.of("lavaSlot", "cobwebSlot")),
        new MacroDef("la",  "Lava",                "uhc",     30, List.of("lavaSlot"))
    );
}
