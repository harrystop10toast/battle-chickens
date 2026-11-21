package com.cluck.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.model.animal.chicken.AdultChickenModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.monster.phantom.PhantomModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;

final class ChickenJemModel {
	private final String resource;
	private final JsonObject document;
	private final TextureSize textureSize;

	private ChickenJemModel(String resource) {
		this.resource = resource;
		document = readDocument(resource);
		textureSize = textureSize(document, null, resource);
	}

	static ChickenJemModel load(String resource) {
		return new ChickenJemModel(resource);
	}

	Identifier texture() {
		JsonElement texture = document.get("texture");
		if (texture == null) {
			for (JsonElement element : document.getAsJsonArray("models")) {
				JsonObject model = element.getAsJsonObject();
				if (model.has("texture")) {
					texture = model.get("texture");
					break;
				}
			}
		}
		if (texture == null) {
			throw new IllegalStateException("Missing texture reference in " + resource);
		}
		return Identifier.parse(texture.getAsString());
	}

	ModelPart createModel() {
		ModelPart vanillaRoot = AdultChickenModel.createBodyLayer().bakeRoot();
		Map<String, Map<String, ModelPart>> attachments = new LinkedHashMap<>();
		for (String bone : List.of("head", "body", "right_leg", "left_leg",
				"right_wing", "left_wing")) {
			attachments.put(bone, new LinkedHashMap<>());
		}
		ModelPart vanillaBody = vanillaRoot.getChild("body");
		attachments.put("body_overlay", new LinkedHashMap<>());

		JsonArray models = document.getAsJsonArray("models");
		for (int index = 0; index < models.size(); index++) {
			JsonObject model = models.get(index).getAsJsonObject();
			String partName = model.get("part").getAsString();
			String bone = attachmentBone(partName);
			String id = model.get("id").getAsString();
			Map<String, ModelPart> children = attachments.get(bone);
			if (children == null) {
				throw new IllegalStateException(
						"Unknown chicken model part '" + partName + "' in " + resource);
			}
			if (children.putIfAbsent(id,
					createPart(model, textureSize, !id.equals(partName))) != null) {
				throw new IllegalStateException(
						"Duplicate model part '" + id + "' in " + resource);
			}
		}

		Map<String, ModelPart> bones = new LinkedHashMap<>();
		for (Map.Entry<String, Map<String, ModelPart>> entry : attachments.entrySet()) {
			PartPose pose = entry.getKey().equals("body_overlay")
					? PartPose.offset(vanillaBody.x, vanillaBody.y, vanillaBody.z)
					: vanillaRoot.getChild(entry.getKey()).getInitialPose();
			bones.put(entry.getKey(), part(List.of(), entry.getValue(), pose));
		}
		return part(List.of(), bones, PartPose.ZERO);
	}

	ModelPart createStandaloneModel() {
		JsonArray models = document.getAsJsonArray("models");
		if (models.size() != 1) {
			throw new IllegalStateException("Expected one root model in " + resource);
		}
		JsonObject model = models.get(0).getAsJsonObject();
		String partName = model.get("part").getAsString();
		String id = model.get("id").getAsString();
		return createPart(model, textureSize, !id.equals(partName));
	}

	PhantomModel createPhantomModel() {
		ModelPart vanillaRoot = PhantomModel.createBodyLayer().bakeRoot();
		ModelPart vanillaBody = vanillaRoot.getChild("body");
		Map<String, Map<String, ModelPart>> attachments = new LinkedHashMap<>();
		for (String bone : List.of("body", "head", "left_wing_base", "left_wing_tip",
				"right_wing_base", "right_wing_tip", "tail_base", "tail_tip")) {
			attachments.put(bone, new LinkedHashMap<>());
		}

		for (JsonElement element : document.getAsJsonArray("models")) {
			JsonObject model = element.getAsJsonObject();
			String partName = model.get("part").getAsString();
			String bone = phantomBone(partName);
			String id = model.get("id").getAsString();
			Map<String, ModelPart> children = attachments.get(bone);
			if (children.putIfAbsent(id,
					createPart(model, textureSize, !id.equals(partName))) != null) {
				throw new IllegalStateException(
						"Duplicate model part '" + id + "' in " + resource);
			}
		}

		ModelPart vanillaTailBase = vanillaBody.getChild("tail_base");
		Map<String, ModelPart> tailBaseChildren = new LinkedHashMap<>(attachments.get("tail_base"));
		tailBaseChildren.put("tail_tip", part(List.of(), attachments.get("tail_tip"),
				vanillaTailBase.getChild("tail_tip").getInitialPose()));
		ModelPart tailBase = part(List.of(), tailBaseChildren, vanillaTailBase.getInitialPose());

		ModelPart vanillaLeftWing = vanillaBody.getChild("left_wing_base");
		Map<String, ModelPart> leftWingChildren = new LinkedHashMap<>(
				attachments.get("left_wing_base"));
		leftWingChildren.put("left_wing_tip", part(List.of(), attachments.get("left_wing_tip"),
				vanillaLeftWing.getChild("left_wing_tip").getInitialPose()));
		ModelPart leftWing = part(List.of(), leftWingChildren, vanillaLeftWing.getInitialPose());

		ModelPart vanillaRightWing = vanillaBody.getChild("right_wing_base");
		Map<String, ModelPart> rightWingChildren = new LinkedHashMap<>(
				attachments.get("right_wing_base"));
		rightWingChildren.put("right_wing_tip", part(List.of(), attachments.get("right_wing_tip"),
				vanillaRightWing.getChild("right_wing_tip").getInitialPose()));
		ModelPart rightWing = part(List.of(), rightWingChildren, vanillaRightWing.getInitialPose());

		Map<String, ModelPart> bodyChildren = new LinkedHashMap<>(attachments.get("body"));
		bodyChildren.put("head", part(List.of(), attachments.get("head"),
				vanillaBody.getChild("head").getInitialPose()));
		bodyChildren.put("tail_base", tailBase);
		bodyChildren.put("left_wing_base", leftWing);
		bodyChildren.put("right_wing_base", rightWing);
		ModelPart body = part(List.of(), bodyChildren, vanillaBody.getInitialPose());
		return new PhantomModel(part(List.of(), Map.of("body", body), PartPose.ZERO));
	}

	private String phantomBone(String partName) {
		return switch (partName) {
			case "body", "saddle" -> "body";
			case "head" -> "head";
			case "left_wing" -> "left_wing_base";
			case "left_wing_tip" -> "left_wing_tip";
			case "right_wing" -> "right_wing_base";
			case "right_wing_tip" -> "right_wing_tip";
			case "tail" -> "tail_base";
			case "tail2" -> "tail_tip";
			default -> throw new IllegalStateException(
					"Unknown phantom model part '" + partName + "' in " + resource);
		};
	}

	private static String attachmentBone(String partName) {
		return switch (partName) {
			case "bill", "chin" -> "head";
			default -> partName;
		};
	}

	private ModelPart createPart(JsonObject model, TextureSize inheritedTextureSize,
			boolean applyRotation) {
		TextureSize textureSize = textureSize(model, inheritedTextureSize, resource);
		List<ModelPart.Cube> cubes = new ArrayList<>();
		if (model.has("boxes")) {
			for (JsonElement element : model.getAsJsonArray("boxes")) {
				cubes.add(createCube(element.getAsJsonObject(), model, textureSize));
			}
		}

		Map<String, ModelPart> children = new LinkedHashMap<>();
		if (model.has("submodels")) {
			JsonArray submodels = model.getAsJsonArray("submodels");
			for (int index = 0; index < submodels.size(); index++) {
				JsonObject child = submodels.get(index).getAsJsonObject();
				String name = child.has("id") ? child.get("id").getAsString() : "part_" + index;
				if (children.putIfAbsent(name, createPart(child, textureSize, true)) != null) {
					throw new IllegalStateException(
							"Duplicate submodel '" + name + "' in " + resource);
				}
			}
		}

		return part(cubes, children, pose(model, applyRotation));
	}

	private static ModelPart part(List<ModelPart.Cube> cubes, Map<String, ModelPart> children,
			PartPose pose) {
		ModelPart part = new ModelPart(cubes, children);
		part.setInitialPose(pose);
		part.loadPose(pose);
		return part;
	}

	private static ModelPart.Cube createCube(JsonObject box, JsonObject model, TextureSize textureSize) {
		float[] coordinates = coordinates(box, model);
		float sizeAdd = box.has("sizeAdd") ? box.get("sizeAdd").getAsFloat() : 0.0F;
		int u = 0;
		int v = 0;
		if (box.has("textureOffset")) {
			JsonArray uv = box.getAsJsonArray("textureOffset");
			u = uv.get(0).getAsInt();
			v = uv.get(1).getAsInt();
		}

		ModelPart.Cube cube = new ModelPart.Cube(u, v,
				coordinates[0], coordinates[1], coordinates[2],
				coordinates[3], coordinates[4], coordinates[5],
				sizeAdd, sizeAdd, sizeAdd, false, textureSize.width(), textureSize.height(),
				EnumSet.allOf(Direction.class));
		if (!box.has("textureOffset")) {
			applyFaceUvs(cube, box, model, textureSize);
		}
		return cube;
	}

	private static void applyFaceUvs(ModelPart.Cube cube, JsonObject box, JsonObject model,
			TextureSize textureSize) {
		for (int index = 0; index < cube.polygons.length; index++) {
			ModelPart.Polygon polygon = cube.polygons[index];
			Direction face = Direction.getApproximateNearest(
					polygon.normal().x(), polygon.normal().y(), polygon.normal().z());
			Direction sourceFace = sourceFace(face, model);
			String key = "uv" + capitalize(sourceFace.getName());
			if (!box.has(key)) {
				continue;
			}
			JsonArray uv = box.getAsJsonArray(key);
			float u0 = uv.get(0).getAsFloat();
			float v0 = uv.get(1).getAsFloat();
			float u1 = uv.get(2).getAsFloat();
			float v1 = uv.get(3).getAsFloat();
			if (inverts(model, uvAxis(sourceFace, true))) {
				float swap = u0;
				u0 = u1;
				u1 = swap;
			}
			if (inverts(model, uvAxis(sourceFace, false))) {
				float swap = v0;
				v0 = v1;
				v1 = swap;
			}
			cube.polygons[index] = new ModelPart.Polygon(polygon.vertices().clone(),
					u0, v0, u1, v1, textureSize.width(), textureSize.height(), false, face);
		}
	}

	private static String uvAxis(Direction face, boolean u) {
		return switch (face) {
			case EAST, WEST -> u ? "z" : "y";
			case UP, DOWN -> u ? "x" : "z";
			case NORTH, SOUTH -> u ? "x" : "y";
		};
	}

	private static Direction sourceFace(Direction face, JsonObject model) {
		return switch (face) {
			case EAST -> inverts(model, "x") ? Direction.WEST : face;
			case WEST -> inverts(model, "x") ? Direction.EAST : face;
			case UP -> inverts(model, "y") ? Direction.DOWN : face;
			case DOWN -> inverts(model, "y") ? Direction.UP : face;
			case SOUTH -> inverts(model, "z") ? Direction.NORTH : face;
			case NORTH -> inverts(model, "z") ? Direction.SOUTH : face;
		};
	}

	private static TextureSize textureSize(JsonObject model, TextureSize fallback, String resource) {
		if (!model.has("textureSize")) {
			if (fallback == null) {
				throw new IllegalStateException("Missing textureSize in " + resource);
			}
			return fallback;
		}
		JsonArray values = model.getAsJsonArray("textureSize");
		if (values.size() != 2) {
			throw new IllegalStateException("Invalid textureSize in " + resource);
		}
		int width = values.get(0).getAsInt();
		int height = values.get(1).getAsInt();
		if (width <= 0 || height <= 0) {
			throw new IllegalStateException("Invalid textureSize in " + resource);
		}
		return new TextureSize(width, height);
	}

	private static float[] coordinates(JsonObject box, JsonObject model) {
		JsonArray values = box.getAsJsonArray("coordinates");
		float x = values.get(0).getAsFloat();
		float y = values.get(1).getAsFloat();
		float z = values.get(2).getAsFloat();
		float width = values.get(3).getAsFloat();
		float height = values.get(4).getAsFloat();
		float depth = values.get(5).getAsFloat();
		x = inverts(model, "x") ? -x - width : x;
		y = inverts(model, "y") ? -y - height : y;
		return new float[] {x, y, z, width, height, depth};
	}

	private static PartPose pose(JsonObject model, boolean applyRotation) {
		float[] position = vector(model, "translate");
		float[] rotation = applyRotation
				? vector(model, "rotate")
				: new float[] {0.0F, 0.0F, 0.0F};
		if (inverts(model, "x")) {
			position[0] = -position[0];
			rotation[0] = -rotation[0];
		}
		if (inverts(model, "y")) {
			position[1] = -position[1];
			rotation[1] = -rotation[1];
		}
		float radians = (float) (Math.PI / 180.0);
		return PartPose.offsetAndRotation(position[0], position[1], position[2],
				rotation[0] * radians, rotation[1] * radians, rotation[2] * radians);
	}

	private static float[] vector(JsonObject model, String key) {
		if (!model.has(key)) {
			return new float[] {0.0F, 0.0F, 0.0F};
		}
		JsonArray values = model.getAsJsonArray(key);
		return new float[] {
				values.get(0).getAsFloat(),
				values.get(1).getAsFloat(),
				values.get(2).getAsFloat()
		};
	}

	private static boolean inverts(JsonObject model, String axis) {
		return model.has("invertAxis")
				&& model.get("invertAxis").getAsString().toLowerCase(Locale.ROOT).contains(axis);
	}

	private static String capitalize(String value) {
		return Character.toUpperCase(value.charAt(0)) + value.substring(1);
	}

	private static JsonObject readDocument(String resource) {
		try (var stream = ChickenJemModel.class.getResourceAsStream(resource)) {
			if (stream == null) {
				throw new IllegalStateException("Missing chicken model resource: " + resource);
			}
			try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
				return JsonParser.parseReader(reader).getAsJsonObject();
			}
		} catch (IOException exception) {
			throw new IllegalStateException("Unable to read chicken model resource: " + resource,
					exception);
		}
	}

	private record TextureSize(int width, int height) {
	}
}
