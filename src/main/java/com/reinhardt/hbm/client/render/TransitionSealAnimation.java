package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.reinhardt.hbm.ReinhardtsHBM;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * The transition seal is the one heavy door whose 1.7.10 renderer used a
 * Collada animated model instead of the normal per-part translation fallback.
 *
 * <p>The OBJ asset is the first (closed) Collada pose.  This class reads the
 * original animation tracks and applies the exact legacy transform delta
 * {@code M(t) * inverse(M(0))} to each OBJ object.  There is deliberately no
 * shared model scale or offset here; all transforms come from the old DAE.</p>
 */
final class TransitionSealAnimation {
    private static final ResourceLocation ANIMATION_LOCATION =
            ReinhardtsHBM.id("models/obj/doors/transition_seal.dae");

    /*
     * The OBJ exporter renamed three duplicate roots after the DAE was
     * exported.  Keep this mapping explicit so animation data is matched by
     * geometry, not by a guessed suffix.
     */
    private static final Map<String, String> DAE_CHANNEL_BY_OBJ = Map.ofEntries(
            Map.entry("door.006", "door.006"),
            Map.entry("ring.002", "ring.002"),
            Map.entry("door.004", "door.004"),
            Map.entry("door.003", "door.003"),
            Map.entry("ring.004", "ring.001"),
            Map.entry("door.008", "door.008"),
            Map.entry("door.005", "door.002"),
            Map.entry("door.007", "door.005"),
            Map.entry("Cylinder.011", "Cylinder.011"),
            Map.entry("Cylinder.010", "Cylinder.010"),
            Map.entry("Cylinder.009", "Cylinder.009"),
            Map.entry("Circle", "Circle"),
            Map.entry("Cylinder.008", "Cylinder.008"),
            Map.entry("Cylinder.007", "Cylinder.007"),
            Map.entry("Cube.006", "Cube.006"),
            Map.entry("Cylinder.005", "Cylinder.005"),
            Map.entry("Cylinder.003", "Cylinder.003"),
            Map.entry("Cylinder.001", "Cylinder.001"),
            Map.entry("door", "door")
    );

    private static final Object LOAD_LOCK = new Object();
    private static volatile AnimationData animationData;
    private static final ThreadLocal<Scratch> SCRATCH = ThreadLocal.withInitial(Scratch::new);

    private TransitionSealAnimation() {
    }

    /**
     * Applies one object's legacy Collada pose to the current PoseStack.
     *
     * @return true when a DAE track was found and applied, false when the
     *         resource could not be loaded (the caller may use a safe fallback)
     */
    static boolean apply(PoseStack poseStack, String objObjectName, float progress) {
        String channelName = DAE_CHANNEL_BY_OBJ.get(objObjectName);
        if (channelName == null) {
            return false;
        }

        AnimationData data = data();
        Track track = data.tracks.get(channelName);
        if (track == null) {
            return false;
        }

        track.apply(poseStack, progress, SCRATCH.get());
        return true;
    }

    private static AnimationData data() {
        AnimationData current = animationData;
        if (current != null) {
            return current;
        }
        synchronized (LOAD_LOCK) {
            current = animationData;
            if (current == null) {
                current = load();
                animationData = current;
            }
        }
        return current;
    }

    private static AnimationData load() {
        java.util.Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(ANIMATION_LOCATION);
        if (resource.isEmpty()) {
            ReinhardtsHBM.LOGGER.error("Transition seal animation resource is missing: {}", ANIMATION_LOCATION);
            return AnimationData.EMPTY;
        }

        try (InputStream stream = resource.get().open()) {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);

            Document document = factory.newDocumentBuilder().parse(stream);
            Element library = firstElement(document.getElementsByTagName("library_animations"));
            if (library == null) {
                throw new IllegalStateException("library_animations is missing");
            }

            Map<String, Track> tracks = new HashMap<>();
            for (Element outer : childElements(library, "animation")) {
                String objectName = outer.getAttribute("name");
                Element nested = firstElement(outer.getElementsByTagName("animation"));
                if (nested == null) {
                    continue;
                }
                Track track = parseTrack(nested);
                if (track != null) {
                    tracks.put(objectName, track);
                }
            }

            if (tracks.isEmpty()) {
                throw new IllegalStateException("no transition seal animation tracks were parsed");
            }
            ReinhardtsHBM.LOGGER.info("Loaded legacy transition seal animation: {} DAE object tracks", tracks.size());
            return new AnimationData(Map.copyOf(tracks));
        } catch (Exception exception) {
            ReinhardtsHBM.LOGGER.error("Failed to load legacy transition seal animation {}", ANIMATION_LOCATION, exception);
            return AnimationData.EMPTY;
        }
    }

    private static Track parseTrack(Element animation) {
        Element sampler = firstElement(animation.getElementsByTagName("sampler"));
        if (sampler == null) {
            return null;
        }

        String outputId = null;
        for (Element input : childElements(sampler, "input")) {
            if ("OUTPUT".equals(input.getAttribute("semantic"))) {
                outputId = stripHash(input.getAttribute("source"));
                break;
            }
        }
        if (outputId == null) {
            return null;
        }

        Element outputSource = null;
        for (Element source : childElements(animation, "source")) {
            if (outputId.equals(source.getAttribute("id"))) {
                outputSource = source;
                break;
            }
        }
        if (outputSource == null) {
            return null;
        }

        Element array = firstElement(outputSource.getElementsByTagName("float_array"));
        if (array == null || array.getTextContent() == null) {
            return null;
        }
        float[] values = parseFloats(array.getTextContent());
        if (values.length < 32 || values.length % 16 != 0) {
            return null;
        }
        return new Track(values);
    }

    private static String stripHash(String value) {
        return value != null && value.startsWith("#") ? value.substring(1) : value;
    }

    private static float[] parseFloats(String text) {
        String trimmed = text.trim();
        if (trimmed.isEmpty()) {
            return new float[0];
        }
        String[] tokens = trimmed.split("\\s+");
        float[] values = new float[tokens.length];
        for (int i = 0; i < tokens.length; i++) {
            values[i] = Float.parseFloat(tokens[i]);
        }
        return values;
    }

    private static Element firstElement(org.w3c.dom.NodeList nodes) {
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (node instanceof Element element) {
                return element;
            }
        }
        return null;
    }

    private static java.util.List<Element> childElements(Element parent, String tagName) {
        java.util.ArrayList<Element> elements = new java.util.ArrayList<>();
        for (Node node = parent.getFirstChild(); node != null; node = node.getNextSibling()) {
            if (node instanceof Element element && tagName.equals(element.getTagName())) {
                elements.add(element);
            }
        }
        return elements;
    }

    private static final class AnimationData {
        private static final AnimationData EMPTY = new AnimationData(Map.of());
        private final Map<String, Track> tracks;

        private AnimationData(Map<String, Track> tracks) {
            this.tracks = tracks;
        }
    }

    private static final class Track {
        private final float[] tx;
        private final float[] ty;
        private final float[] tz;
        private final float[] sx;
        private final float[] sy;
        private final float[] sz;
        private final float[] qx;
        private final float[] qy;
        private final float[] qz;
        private final float[] qw;
        private final Matrix4f initialInverse;

        private Track(float[] matrices) {
            int count = matrices.length / 16;
            this.tx = new float[count];
            this.ty = new float[count];
            this.tz = new float[count];
            this.sx = new float[count];
            this.sy = new float[count];
            this.sz = new float[count];
            this.qx = new float[count];
            this.qy = new float[count];
            this.qz = new float[count];
            this.qw = new float[count];

            Matrix4f matrix = new Matrix4f();
            Vector3f translation = new Vector3f();
            Vector3f scale = new Vector3f();
            Quaternionf rotation = new Quaternionf();
            for (int i = 0; i < count; i++) {
                matrix.setTransposed(matrices, i * 16);
                matrix.getTranslation(translation);
                matrix.getScale(scale);
                matrix.getNormalizedRotation(rotation);

                tx[i] = translation.x;
                ty[i] = translation.y;
                tz[i] = translation.z;
                sx[i] = scale.x;
                sy[i] = scale.y;
                sz[i] = scale.z;
                qx[i] = rotation.x;
                qy[i] = rotation.y;
                qz[i] = rotation.z;
                qw[i] = rotation.w;
            }

            Matrix4f initial = new Matrix4f().translationRotateScale(
                    new Vector3f(tx[0], ty[0], tz[0]),
                    new Quaternionf(qx[0], qy[0], qz[0], qw[0]),
                    new Vector3f(sx[0], sy[0], sz[0])
            );
            this.initialInverse = initial.invertAffine(new Matrix4f());
        }

        private void apply(PoseStack poseStack, float progress, Scratch scratch) {
            float frame = Math.max(0.0F, Math.min(1.0F, progress)) * (tx.length - 1);
            int first = (int) frame;
            int next = Math.min(first + 1, tx.length - 1);
            float inter = frame - first;

            scratch.translation.set(tx[first], ty[first], tz[first])
                    .lerp(new Vector3f(tx[next], ty[next], tz[next]), inter);
            scratch.scale.set(sx[first], sy[first], sz[first])
                    .lerp(new Vector3f(sx[next], sy[next], sz[next]), inter);
            scratch.rotation.set(qx[first], qy[first], qz[first], qw[first])
                    .slerp(scratch.nextRotation.set(qx[next], qy[next], qz[next], qw[next]), inter);

            scratch.current.translationRotateScale(scratch.translation, scratch.rotation, scratch.scale);
            scratch.current.mul(this.initialInverse, scratch.delta);
            poseStack.mulPose(scratch.delta);
        }
    }

    private static final class Scratch {
        private final Vector3f translation = new Vector3f();
        private final Vector3f scale = new Vector3f();
        private final Quaternionf rotation = new Quaternionf();
        private final Quaternionf nextRotation = new Quaternionf();
        private final Matrix4f current = new Matrix4f();
        private final Matrix4f delta = new Matrix4f();
    }
}
