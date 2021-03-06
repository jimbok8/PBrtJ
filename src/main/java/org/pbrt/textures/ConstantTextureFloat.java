
/*
 * PBrtJ -- Port of pbrt v3 to Java.
 * Copyright (c) 2017 Rick Weyrauch.
 *
 * pbrt source code is Copyright(c) 1998-2016
 * Matt Pharr, Greg Humphreys, and Wenzel Jakob.
 *
 */

package org.pbrt.textures;

import org.pbrt.core.*;

public class ConstantTextureFloat extends TextureFloat {

    public static ConstantTextureFloat CreateFloat(Transform tex2world, TextureParams tp) {
        return new ConstantTextureFloat(tp.FindFloat("value", 1.0f));
    }

    public ConstantTextureFloat(float value) {
        this.value = value;
    }

    public float Evaluate(SurfaceInteraction si) { return value; }

    private final float value;
}