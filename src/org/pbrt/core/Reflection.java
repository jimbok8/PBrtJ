
/*
 * PBrtJ -- Port of pbrt v3 to Java.
 * Copyright (c) 2017 Rick Weyrauch.
 *
 * pbrt source code is Copyright(c) 1998-2016
 * Matt Pharr, Greg Humphreys, and Wenzel Jakob.
 *
 */

package org.pbrt.core;

public class Reflection {

    // Reflection Declarations
    public static float FrDielectric(float cosThetaI, float etaI, float etaT) {
    }

    public static Spectrum FrConductor(float cosThetaI, Spectrum etaI, Spectrum etaT, Spectrum k) {
    }

    // BSDF Inline Functions
    public static float CosTheta(Vector3f w) {
        return w.z;
    }

    public static float Cos2Theta(Vector3f w) {
        return w.z * w.z;
    }

    public static float AbsCosTheta(Vector3f w) {
        return Math.abs(w.z);
    }

    public static float Sin2Theta(Vector3f w) {
        return Math.max(0, 1 - Cos2Theta(w));
    }

    public static float SinTheta(Vector3f w) { return (float)Math.sqrt(Sin2Theta(w)); }

    public static float TanTheta(Vector3f w) { return SinTheta(w) / CosTheta(w); }

    public static float Tan2Theta(Vector3f w) {
        return Sin2Theta(w) / Cos2Theta(w);
    }

    public static float CosPhi(Vector3f w) {
        float sinTheta = SinTheta(w);
        return (sinTheta == 0) ? 1 : Pbrt.Clamp(w.x / sinTheta, -1, 1);
    }

    public static float SinPhi(Vector3f w) {
        float sinTheta = SinTheta(w);
        return (sinTheta == 0) ? 0 : Pbrt.Clamp(w.y / sinTheta, -1, 1);
    }

    public static float Cos2Phi(Vector3f w) { return CosPhi(w) * CosPhi(w); }

    public static float Sin2Phi(Vector3f w) { return SinPhi(w) * SinPhi(w); }

    public static float CosDPhi(Vector3f wa, Vector3f wb) {
        return Pbrt.Clamp(
                (wa.x * wb.x + wa.y * wb.y) / (float)Math.sqrt((wa.x * wa.x + wa.y * wa.y) *
                (wb.x * wb.x + wb.y * wb.y)),
                -1, 1);
    }

    public static Vector3f Reflect(Vector3f wo, Vector3f n) {
        return wo.negate() + 2 * Vector3f.Dot(wo, n) * n;
    }

    public static Vector3f Refract(Vector3f wi, Normal3f n, float eta) {
        // Compute $\cos \theta_\roman{t}$ using Snell's law
        float cosThetaI = Normal3f.Dot(n, wi);
        float sin2ThetaI = Math.max(0, 1 - cosThetaI * cosThetaI);
        float sin2ThetaT = eta * eta * sin2ThetaI;

        // Handle total internal reflection for transmission
        if (sin2ThetaT >= 1) return null;
        float cosThetaT = (float)Math.sqrt(1 - sin2ThetaT);
        Vector3f wt = eta * wi.negate() + (eta * cosThetaI - cosThetaT) * (new Vector3f(n));
        return wt;
    }

    public static boolean SameHemisphere(Vector3f w, Vector3f wp) {
        return w.z * wp.z > 0;
    }

    public static boolean SameHemisphere(Vector3f w, Normal3f wp) {
        return w.z * wp.z > 0;
    }

}