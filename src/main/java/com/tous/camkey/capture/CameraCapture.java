package com.tous.camkey.capture;

import net.minecraft.client.Camera;
import net.minecraft.world.phys.Vec3;

import com.tous.camkey.model.Keyframe;

public final class CameraCapture {

    private CameraCapture() {
    }

    public static Keyframe capture(Camera camera) {
        Vec3 position = camera.getPosition();
        return new Keyframe(position.x, position.y, position.z, camera.getYRot(), camera.getXRot());
    }
}
