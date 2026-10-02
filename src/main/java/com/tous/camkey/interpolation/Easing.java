package com.tous.camkey.interpolation;

@FunctionalInterface
public interface Easing {

    Easing LINEAR = t -> t;
    Easing SMOOTHSTEP = t -> t * t * (3.0 - 2.0 * t);

    double apply(double t);
}
