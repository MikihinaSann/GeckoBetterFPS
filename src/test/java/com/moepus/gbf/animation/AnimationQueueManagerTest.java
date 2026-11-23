package com.moepus.gbf.animation;

import org.junit.jupiter.api.Test;
import software.bernie.geckolib.animation.keyframe.BoneAnimationQueue;
import software.bernie.geckolib.cache.object.GeoBone;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnimationQueueManagerTest {
    @Test
    void acceptsSameBoneInstances() {
        GeoBone head = bone("head");
        GeoBone body = bone("body");
        Map<String, BoneAnimationQueue> queues = Map.of(
                "head", new BoneAnimationQueue(head),
                "body", new BoneAnimationQueue(body));

        assertTrue(AnimationQueueManager.hasSameBoneLayout(queues, List.of(head, body)));
    }

    @Test
    void rejectsDifferentBoneWithSameName() {
        GeoBone oldBone = bone("head");
        GeoBone newBone = bone("head");
        Map<String, BoneAnimationQueue> queues = Map.of("head", new BoneAnimationQueue(oldBone));

        assertFalse(AnimationQueueManager.hasSameBoneLayout(queues, List.of(newBone)));
    }

    @Test
    void rejectsDifferentMapSize() {
        GeoBone head = bone("head");
        Map<String, BoneAnimationQueue> queues = Map.of("head", new BoneAnimationQueue(head));

        assertFalse(AnimationQueueManager.hasSameBoneLayout(queues, List.of(head, bone("body"))));
    }

    private static GeoBone bone(String name) {
        return new GeoBone(null, name, false, null, false, false);
    }
}
