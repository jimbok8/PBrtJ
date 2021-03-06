
/*
 * PBrtJ -- Port of pbrt v3 to Java.
 * Copyright (c) 2017 Rick Weyrauch.
 *
 * pbrt source code is Copyright(c) 1998-2016
 * Matt Pharr, Greg Humphreys, and Wenzel Jakob.
 *
 */

package org.pbrt.core;

import java.util.Objects;

public class Texture<T> {

    public enum ImageWrap { Repeat, Black, Clamp }
    public enum AAMethod { None, ClosedForm }

    public static class TexInfo implements Comparable<TexInfo> {
        public TexInfo(String filename, boolean doTrilinear, float maxAniso, Texture.ImageWrap wrapMode, float scale, boolean gamma) {
            this.filename = filename;
            this.doTrilinear = doTrilinear;
            this.maxAniso = maxAniso;
            this.wrapMode = wrapMode;
            this.scale = scale;
            this.gamma = gamma;
        }

        @Override
        public int compareTo(@org.jetbrains.annotations.NotNull TexInfo t2) {
            if (!Objects.equals(filename, t2.filename)) return filename.compareTo(t2.filename);
            if (doTrilinear != t2.doTrilinear) return -1;
            if (maxAniso != t2.maxAniso) return (maxAniso < t2.maxAniso) ? -1 : 1;
            if (scale != t2.scale) return (scale < t2.scale) ? -1 : 1;
            if (gamma != t2.gamma) return -1;
            return (wrapMode != t2.wrapMode) ? -1 : 0;
        }

        public String filename;
        public boolean doTrilinear;
        public float maxAniso;
        public Texture.ImageWrap wrapMode;
        public float scale;
        public boolean gamma;
    }

    public static float Lanczos(float x, float tau) {
        x = Math.abs(x);
        if (x < 1e-5f) return 1;
        if (x > 1.f) return 0;
        x *= (float)Math.PI;
        float s = (float)Math.sin(x * tau) / (x * tau);
        float lanczos = (float)Math.sin(x) / x;
        return s * lanczos;
    }

    public static float Noise(float x, float y, float z) {
        int ix = (int)Math.floor(x), iy = (int)Math.floor(y), iz = (int)Math.floor(z);
        float dx = x - ix, dy = y - iy, dz = z - iz;

        // Compute gradient weights
        ix &= NoisePermSize - 1;
        iy &= NoisePermSize - 1;
        iz &= NoisePermSize - 1;
        float w000 = Grad(ix, iy, iz, dx, dy, dz);
        float w100 = Grad(ix + 1, iy, iz, dx - 1, dy, dz);
        float w010 = Grad(ix, iy + 1, iz, dx, dy - 1, dz);
        float w110 = Grad(ix + 1, iy + 1, iz, dx - 1, dy - 1, dz);
        float w001 = Grad(ix, iy, iz + 1, dx, dy, dz - 1);
        float w101 = Grad(ix + 1, iy, iz + 1, dx - 1, dy, dz - 1);
        float w011 = Grad(ix, iy + 1, iz + 1, dx, dy - 1, dz - 1);
        float w111 = Grad(ix + 1, iy + 1, iz + 1, dx - 1, dy - 1, dz - 1);

        // Compute trilinear interpolation of weights
        float wx = NoiseWeight(dx), wy = NoiseWeight(dy), wz = NoiseWeight(dz);
        float x00 = Pbrt.Lerp(wx, w000, w100);
        float x10 = Pbrt.Lerp(wx, w010, w110);
        float x01 = Pbrt.Lerp(wx, w001, w101);
        float x11 = Pbrt.Lerp(wx, w011, w111);
        float y0 = Pbrt.Lerp(wy, x00, x10);
        float y1 = Pbrt.Lerp(wy, x01, x11);
        return Pbrt.Lerp(wz, y0, y1);
    }
    public static float Noise(float x, float y) {
        return Noise(x, y, 0.5f);
    }
    public static float Noise(float x) {
        return Noise(x, 0.5f, 0.5f);
    }
    public static float Noise(Point3f p) {
        return Noise(p.x, p.y, p.z);
    }

    public static float FBm(Point3f p, Vector3f dpdx, Vector3f dpdy, float omega, int octaves) {
        // Compute number of octaves for antialiased FBm
        float len2 = Math.max(dpdx.LengthSquared(), dpdy.LengthSquared());
        float n = Pbrt.Clamp(-1 - .5f * Pbrt.Log2(len2), 0, octaves);
        int nInt = (int)Math.floor(n);

        // Compute sum of octaves of noise for FBm
        float sum = 0, lambda = 1, o = 1;
        for (int i = 0; i < nInt; ++i) {
            sum += o * Noise(p.scale(lambda));
            lambda *= 1.99f;
            o *= omega;
        }
        Float nPartial = n - nInt;
        sum += o * SmoothStep(.3f, .7f, nPartial) * Noise(p.scale(lambda));
        return sum;
    }

    public static float Turbulence(Point3f p, Vector3f dpdx, Vector3f dpdy, float omega, int octaves) {
        // Compute number of octaves for antialiased FBm
        float len2 = Math.max(dpdx.LengthSquared(), dpdy.LengthSquared());
        float n = Pbrt.Clamp(-1 - .5f * Pbrt.Log2(len2), 0, octaves);
        int nInt = (int)Math.floor(n);

        // Compute sum of octaves of noise for turbulence
        float sum = 0, lambda = 1, o = 1;
        for (int i = 0; i < nInt; ++i) {
            sum += o * Math.abs(Noise(p.scale(lambda)));
            lambda *= 1.99f;
            o *= omega;
        }

        // Account for contributions of clamped octaves in turbulence
        Float nPartial = n - nInt;
        sum += o * Pbrt.Lerp(SmoothStep(.3f, .7f, nPartial), 0.2f, Math.abs(Noise(p.scale(lambda))));
        for (int i = nInt; i < octaves; ++i) {
            sum += o * 0.2f;
            o *= omega;
        }
        return sum;
    }

    private static float Grad(int x, int y, int z, float dx, float dy, float dz) {
        int h = NoisePerm[NoisePerm[NoisePerm[x] + y] + z];
        h &= 15;
        float u = h < 8 || h == 12 || h == 13 ? dx : dy;
        float v = h < 4 || h == 12 || h == 13 ? dy : dz;
        return ((h & 1) != 0 ? -u : u) + ((h & 2) != 0 ? -v : v);
    }

    private static float NoiseWeight(float t) {
        float t3 = t * t * t;
        float t4 = t3 * t;
        return 6 * t4 * t - 15 * t4 + 10 * t3;
    }

    // Perlin Noise Data
    private static final int NoisePermSize = 256;
    private static final int[] NoisePerm = {
        151, 160, 137, 91, 90, 15, 131, 13, 201, 95, 96, 53, 194, 233, 7, 225, 140,
        36, 103, 30, 69, 142,
        // Remainder of the noise permutation table
        8, 99, 37, 240, 21, 10, 23, 190, 6, 148, 247, 120, 234, 75, 0, 26, 197, 62,
        94, 252, 219, 203, 117, 35, 11, 32, 57, 177, 33, 88, 237, 149, 56, 87, 174,
        20, 125, 136, 171, 168, 68, 175, 74, 165, 71, 134, 139, 48, 27, 166, 77,
        146, 158, 231, 83, 111, 229, 122, 60, 211, 133, 230, 220, 105, 92, 41, 55,
        46, 245, 40, 244, 102, 143, 54, 65, 25, 63, 161, 1, 216, 80, 73, 209, 76,
        132, 187, 208, 89, 18, 169, 200, 196, 135, 130, 116, 188, 159, 86, 164, 100,
        109, 198, 173, 186, 3, 64, 52, 217, 226, 250, 124, 123, 5, 202, 38, 147,
        118, 126, 255, 82, 85, 212, 207, 206, 59, 227, 47, 16, 58, 17, 182, 189, 28,
        42, 223, 183, 170, 213, 119, 248, 152, 2, 44, 154, 163, 70, 221, 153, 101,
        155, 167, 43, 172, 9, 129, 22, 39, 253, 19, 98, 108, 110, 79, 113, 224, 232,
        178, 185, 112, 104, 218, 246, 97, 228, 251, 34, 242, 193, 238, 210, 144, 12,
        191, 179, 162, 241, 81, 51, 145, 235, 249, 14, 239, 107, 49, 192, 214, 31,
        181, 199, 106, 157, 184, 84, 204, 176, 115, 121, 50, 45, 127, 4, 150, 254,
        138, 236, 205, 93, 222, 114, 67, 29, 24, 72, 243, 141, 128, 195, 78, 66,
        215, 61, 156, 180, 151, 160, 137, 91, 90, 15, 131, 13, 201, 95, 96, 53, 194,
        233, 7, 225, 140, 36, 103, 30, 69, 142, 8, 99, 37, 240, 21, 10, 23, 190, 6,
        148, 247, 120, 234, 75, 0, 26, 197, 62, 94, 252, 219, 203, 117, 35, 11, 32,
        57, 177, 33, 88, 237, 149, 56, 87, 174, 20, 125, 136, 171, 168, 68, 175, 74,
        165, 71, 134, 139, 48, 27, 166, 77, 146, 158, 231, 83, 111, 229, 122, 60,
        211, 133, 230, 220, 105, 92, 41, 55, 46, 245, 40, 244, 102, 143, 54, 65, 25,
        63, 161, 1, 216, 80, 73, 209, 76, 132, 187, 208, 89, 18, 169, 200, 196, 135,
        130, 116, 188, 159, 86, 164, 100, 109, 198, 173, 186, 3, 64, 52, 217, 226,
        250, 124, 123, 5, 202, 38, 147, 118, 126, 255, 82, 85, 212, 207, 206, 59,
        227, 47, 16, 58, 17, 182, 189, 28, 42, 223, 183, 170, 213, 119, 248, 152, 2,
        44, 154, 163, 70, 221, 153, 101, 155, 167, 43, 172, 9, 129, 22, 39, 253, 19,
        98, 108, 110, 79, 113, 224, 232, 178, 185, 112, 104, 218, 246, 97, 228, 251,
        34, 242, 193, 238, 210, 144, 12, 191, 179, 162, 241, 81, 51, 145, 235, 249,
        14, 239, 107, 49, 192, 214, 31, 181, 199, 106, 157, 184, 84, 204, 176, 115,
        121, 50, 45, 127, 4, 150, 254, 138, 236, 205, 93, 222, 114, 67, 29, 24, 72,
        243, 141, 128, 195, 78, 66, 215, 61, 156, 180};

    private static float SmoothStep(float min, float max, float value) {
        float v = Pbrt.Clamp((value - min) / (max - min), 0, 1);
        return v * v * (-2 * v + 3);
    }

}