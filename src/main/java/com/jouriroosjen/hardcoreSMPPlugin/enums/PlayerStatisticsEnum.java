package com.jouriroosjen.hardcoreSMPPlugin.enums;

public enum PlayerStatisticsEnum {
    FISH_CAUGHT(1),
    LILLY_PAD_CAUGHT(2),
    JUMPED(3),
    IDLE_KICKS(4),
    TRADES(5),
    NETHER_PORTAL_USED(6),
    END_PORTAL_USED(7),
    ENDER_PEARL_USED(8),
    AIR_PUNCHES(9),
    BED_USED(10),
    EGGS_THROWN(11),
    CHICKENS_HATCHED(12),
    TOTAL_ITEM_DAMAGE(13),
    FOOD_CONSUMED(14),
    POTIONS_CONSUMED(15),
    MILK_CONSUMED(16),
    SNEAKED(17),
    FLIGHTS(18),
    SPRINTS(19),
    ADVANCEMENTS_DONE(20),
    ENDERMAN_ATTACKS(21),
    TOTAL_EXPERIENCE_GAINED(22),
    POTATOES_HARVESTED(23),
    FISH_HOOK_IN_GROUND(24),
    FISHING_FAILED(25),
    FISH_HOOK_IN_PLAYER(26),
    TOTAL_DAMAGE_RECEIVED(27),
    PUFFERFISH_CAUGHT(28),
    CAKE_CONSUMED(29),
    CHORUS_FRUIT_CONSUMED(30),
    END_GATEWAY_USED(31),
    TOTAL_DAMAGE_GIVEN(32),
    HOSTILE_MOBS_KILLED(33),
    NEUTRAL_MOBS_KILLED(34),
    PASSIVE_MOBS_KILLED(35),
    PLAYERS_KILLED(36),
    BLOCKS_TRAVELED(37),
    ENTITIES_BRED(38),
    CHEST_OPENED(39),
    TRAPPED_CHEST_OPENED(40),
    SHULKER_BOX_OPENED(41),
    BARREL_OPENED(42),
    BLOCKS_PLACED(43),
    BLOCKS_DESTROYED(44);

    private final int id;

    /**
     * Constructs a new {@code PlayerStatisticsEnum} instance.
     *
     * @param id
     */
    PlayerStatisticsEnum(int id) {
        this.id = id;
    }

    /**
     * Get the corresponding ID of a value.
     *
     * @return The ID of this value.
     */
    public int getId() {
        return id;
    }
}
