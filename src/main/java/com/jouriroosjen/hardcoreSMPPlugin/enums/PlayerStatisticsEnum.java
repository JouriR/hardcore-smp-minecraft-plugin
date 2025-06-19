package com.jouriroosjen.hardcoreSMPPlugin.enums;

public enum PlayerStatisticsEnum {
    FISH_CAUGHT(1),
    LILLY_PAD_CAUGHT(2),
    JUMPED(3),
    IDLE_KICKS(4),
    TRADES(5),
    BUCKET_USED(6),
    NETHER_PORTAL_USED(7),
    END_PORTAL_USED(8),
    ENDER_PEARL_USED(9),
    AIR_PUNCHES(10),
    BED_USED(11),
    EGGS_THROWN(12),
    CHICKENS_HATCHED(13),
    TOTAL_ITEM_DAMAGE(14),
    FOOD_CONSUMED(15),
    POTIONS_CONSUMED(16),
    MILK_CONSUMED(17),
    SNEAKED(18),
    FLIGHTS(19),
    SPRINTS(20),
    ADVANCEMENTS_DONE(21),
    ENDERMAN_ATTACKS(22),
    TOTAL_EXPERIENCE_GAINED(23),
    POTATOES_HARVESTED(24),
    FISH_HOOK_IN_GROUND(25),
    FISHING_FAILED(26),
    FISH_HOOK_IN_PLAYER(27);

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
