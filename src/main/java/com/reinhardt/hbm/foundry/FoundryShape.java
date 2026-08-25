package com.reinhardt.hbm.foundry;

public enum FoundryShape {
    QUANTUM(1, "quantum"),
    NUGGET(8, "nugget"),
    WIRE(9, "wire"),
    BOLT(9, "bolt"),
    BILLET(48, "billet"),
    INGOT(72, "ingot"),
    DUST(72, "dust"),
    DENSE_WIRE(72, "wire_dense"),
    PLATE(72, "plate"),
    CAST_PLATE(216, "plate_cast"),
    MECHANISM(288, "mechanism"),
    WELDED_PLATE(432, "plate_welded"),
    PIPE(216, "pipe"),
    SHELL(288, "shell"),
    LIGHT_BARREL(216, "part_barrel_light"),
    HEAVY_BARREL(432, "part_barrel_heavy"),
    LIGHT_RECEIVER(288, "part_receiver_light"),
    HEAVY_RECEIVER(648, "part_receiver_heavy"),
    STOCK(288, "part_stock"),
    GRIP(144, "part_grip"),
    BLOCK(648, "block");

    private final int quanta;
    private final String key;

    FoundryShape(int quanta, String key) {
        this.quanta = quanta;
        this.key = key;
    }

    public int quanta() {
        return this.quanta;
    }

    public int q(int amount) {
        return this.quanta * amount;
    }

    public String key() {
        return this.key;
    }
}
