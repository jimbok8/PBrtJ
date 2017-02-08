/*
 * PBrtJ -- Port of pbrt v3 to Java.
 * Copyright (c) 2017 Rick Weyrauch.
 *
 * pbrt source code is Copyright(c) 1998-2016
 * Matt Pharr, Greg Humphreys, and Wenzel Jakob.
 *
 */

package org.pbrt.core;

public class TabulatedBSSRDF extends SeparableBSSRDF {

    private BSSRDFTable table;
    private Spectrum sigma_t, rho;

    public TabulatedBSSRDF(SurfaceInteraction po, Material material,
                    Material.TransportMode mode, float eta, Spectrum sigma_a,
                    Spectrum sigma_s, BSSRDFTable table) {
        super(po, eta, material, mode);
        this.table = table;
        this.sigma_t = (Spectrum)CoefficientSpectrum.add(sigma_a,sigma_s);
        for (int c = 0; c < sigma_s.numSamples(); ++c) {
            if (this.sigma_t.at(c) != 0) {
                this.rho.set(c, sigma_s.at(c) / this.sigma_t.at(c));
            }
            else {
                this.rho.set(c, 0);
            }
        }
    }

    @Override
    public Spectrum Sr(float r) {
        Spectrum Sr = new Spectrum(0);
        for (int ch = 0; ch < Sr.numSamples(); ++ch) {
            // Convert $r$ into unitless optical radius $r_{\roman{optical}}$
            float rOptical = r * sigma_t.at(ch);

            // Compute spline weights to interpolate BSSRDF on channel _ch_
            int rhoOffset, radiusOffset;
            float rhoWeights[] = { 0, 0, 0, 0};
            float radiusWeights[] = { 0, 0, 0, 0};
            if (!CatmullRomWeights(table.nRhoSamples, table.rhoSamples,
                    rho.at(ch), &rhoOffset, rhoWeights) ||
            !CatmullRomWeights(table.nRadiusSamples, table.radiusSamples,
                    rOptical, &radiusOffset, radiusWeights))
            continue;

            // Set BSSRDF value _Sr[ch]_ using tensor spline interpolation
            float sr = 0;
            for (int i = 0; i < 4; ++i) {
                for (int j = 0; j < 4; ++j) {
                    float weight = rhoWeights[i] * radiusWeights[j];
                    if (weight != 0)
                        sr += weight * table.EvalProfile(rhoOffset + i, radiusOffset + j);
                }
            }

            // Cancel marginal PDF factor from tabulated BSSRDF profile
            if (rOptical != 0) sr /= 2 * (float)Math.PI * rOptical;
            Sr.set(ch, sr);
        }
        // Transform BSSRDF value into world space units
        Sr.multiply(sigma_t); // * sigma_t ^ 2
        Sr.multiply(sigma_t);
        return Sr.Clamp();
    }

    @Override
    public float Sample_Sr(int ch, float u) {
        if (sigma_t.at(ch) == 0) return -1;
        return SampleCatmullRom2D(table.nRhoSamples, table.nRadiusSamples,
                table.rhoSamples, table.radiusSamples,
                table.profile, table.profileCDF,
                rho.at(ch), u) / sigma_t.at(ch);
    }

    @Override
    public float Pdf_Sr(int ch, float r) {
        // Convert $r$ into unitless optical radius $r_{\roman{optical}}$
        float rOptical = r * sigma_t.at(ch);

        // Compute spline weights to interpolate BSSRDF density on channel _ch_
        int rhoOffset, radiusOffset;
        float rhoWeights[] = { 0, 0, 0, 0};
        float radiusWeights[] = {0, 0, 0, 0};
        if (!CatmullRomWeights(table.nRhoSamples, table.rhoSamples, rho.at(ch),
                &rhoOffset, rhoWeights) ||
        !CatmullRomWeights(table.nRadiusSamples, table.radiusSamples,
                rOptical, &radiusOffset, radiusWeights))
        return 0;

        // Return BSSRDF profile density for channel _ch_
        float sr = 0, rhoEff = 0;
        for (int i = 0; i < 4; ++i) {
            if (rhoWeights[i] == 0) continue;
            rhoEff += table.rhoEff[rhoOffset + i] * rhoWeights[i];
            for (int j = 0; j < 4; ++j) {
                if (radiusWeights[j] == 0) continue;
                sr += table.EvalProfile(rhoOffset + i, radiusOffset + j) *
                        rhoWeights[i] * radiusWeights[j];
            }
        }

        // Cancel marginal PDF factor from tabulated BSSRDF profile
        if (rOptical != 0) sr /= 2 * (float)Math.PI * rOptical;
        return Math.max(0, sr * sigma_t.at(ch) * sigma_t.at(ch) / rhoEff);
    }
}