package it.unibo.agar.model;

import java.io.Serializable;

public class Food extends AbstractEntity implements Serializable {
    public static final double DEFAULT_MASS = 100.0;
    public Food(final String id, final double x, final double y, final double mass) {
        super(id, x, y, mass);
    }
}
