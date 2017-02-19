/*
 * PBrtJ -- Port of pbrt v3 to Java.
 * Copyright (c) 2017 Rick Weyrauch.
 *
 * pbrt source code is Copyright(c) 1998-2016
 * Matt Pharr, Greg Humphreys, and Wenzel Jakob.
 *
 */

package org.pbrt.core;

import java.util.function.Predicate;

public class Pbrt
{
    public static Options options;

    public static float MachineEpsilon() {
        return Math.ulp(1.0f) * 0.5f;
    }
    public static final float Infinity = Float.MAX_VALUE;
    public static final float ShadowEpsilon = 0.0001f;
    public static final float OneMinusEpsilon = 0x1.fffffep-1f;
    public static float gamma(int n) {
        return (n * MachineEpsilon()) / (1 - n * MachineEpsilon());
    }

    public static float GammaCorrect(float value) {
        if (value <= 0.0031308f) return 12.92f * value;
        return 1.055f * (float)Math.pow(value, (1.0 / 2.4)) - 0.055f;
    }

    public static float InverseGammaCorrect(float value) {
        if (value <= 0.04045f) return value * 1.f / 12.92f;
        return (float)Math.pow((value + 0.055) * 1.0 / 1.055, 2.4);
    }

    public static float Clamp(float v, float low, float high) {
        if (v < low) return low;
        else if (v > high) return high;
        else return v;
    }
    public static int Clamp(int v, int low, int high) {
        if (v < low) return low;
        else if (v > high) return high;
        else return v;
    }

    public static float Log2(float x) {
        float invLog2 = 1.442695040888963387004650940071f;
        return (float)Math.log(x) * invLog2;
    }


    public static int Log2Int(int v) {
        return (Integer.SIZE - 1) - Integer.numberOfLeadingZeros(v);
    }
    public static long Log2Int(long v) {
        return (Long.SIZE - 1) - Long.numberOfLeadingZeros(v);
    }

    public static int Mod(int a, int b) {
        int result = a - (a / b) * b;
        return (int)((result < 0) ? result + b : result);
    }

    public static float Mod(float a, float b) {
        return a % b;
    }

    public static boolean IsPowerOf2(int v) {
        return (v > 0) && ((v & (v - 1)) == 0);
    }

    public static int RoundUpPow2(int v) {
        v--;
        v |= v >> 1;
        v |= v >> 2;
        v |= v >> 4;
        v |= v >> 8;
        v |= v >> 16;
        return v + 1;
    }

    public static int FindInterval(int size, Predicate<Integer> pred) {
        int first = 0, len = size;
        while (len > 0) {
            int half = len >> 1, middle = first + half;
            // Bisect range based on value of _pred_ at _middle_
            if (pred.test(middle)) {
                first = middle + 1;
                len -= half + 1;
            } else
                len = half;
        }
        return Clamp(first - 1, 0, size - 2);
    }

    public static float Lerp(float t, float v1, float v2) { return (1 - t) * v1 + t * v2; }

    public static class QuadRes {
        public float t0, t1;
    }
    public static QuadRes Quadratic(float a, float b, float c) {
        // Find quadratic discriminant
        double discrim = (double)b * (double)b - 4 * (double)a * (double)c;
        if (discrim < 0) return null;
        double rootDiscrim = Math.sqrt(discrim);

        // Compute quadratic _t_ values
        double q;
        if (b < 0)
            q = -.5 * (b - rootDiscrim);
        else
            q = -.5 * (b + rootDiscrim);
        QuadRes res = new QuadRes();
        res.t0 = (float)q / a;
        res.t1 = c / (float)q;
        if (res.t0 > res.t1) {
            float temp = res.t0;
            res.t0 = res.t1;
            res.t1 = temp;
        }
        return res;
    }

    public static float NextFloatUp(float v) {
        // Handle infinity and negative zero for _NextFloatUp()_
        if (Float.isInfinite(v) && v > 0) return v;
        if (v == -0.f) v = 0.f;

        // Advance _v_ to next higher float
        int ui = Float.floatToRawIntBits(v);
        if (v >= 0) {
            ++ui;
        } else {
            --ui;
        }
        return Float.intBitsToFloat(ui);
    }

    public static float NextFloatDown(float v) {
    // Handle infinity and positive zero for _NextFloatDown()_
        if (Float.isInfinite(v) && v > 0) return v;
        if (v == 0.f) v = -0.f;
        int ui = Float.floatToRawIntBits(v);
        if (v > 0)
            --ui;
        else
            ++ui;
        return Float.intBitsToFloat(ui);
    }

}