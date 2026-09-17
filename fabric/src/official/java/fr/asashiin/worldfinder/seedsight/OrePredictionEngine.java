package fr.asashiin.worldfinder.seedsight;

import fr.asashiin.worldfinder.api.world.WorldDimension;
import fr.asashiin.worldfinder.client.worldgen.MinecraftBindings;
import fr.asashiin.worldfinder.client.worldgen.MinecraftGenerationRuntime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.FeatureSorter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.WorldDimensions;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.XoroshiroRandomSource;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

/** Predicts vanilla ore feature rolls without loading or changing any chunks. */
final class OrePredictionEngine {
    static final int RADIUS = 48;
    static final int MAX_RESULTS = 512;

    private static final List<FeatureSpec> DIAMONDS = List.of(
            new FeatureSpec("ore_diamond", 7, 0, Height.TRAPEZOID_OVERWORLD),
            new FeatureSpec("ore_diamond_large", 1, 9, Height.TRAPEZOID_OVERWORLD),
            new FeatureSpec("ore_diamond_buried", 4, 0, Height.TRAPEZOID_OVERWORLD),
            new FeatureSpec("ore_diamond_medium", 2, 0, Height.UNIFORM_DIAMOND));
    private static final List<FeatureSpec> DEBRIS = List.of(
            new FeatureSpec("ore_ancient_debris_large", 1, 0, Height.TRAPEZOID_DEBRIS),
            new FeatureSpec("ore_debris_small", 1, 0, Height.UNIFORM_NETHER));

    private final DimensionFeatures overworld;
    private final DimensionFeatures nether;

    OrePredictionEngine() {
        MinecraftBindings bindings = MinecraftGenerationRuntime.current().bindings();
        WorldDimensions dimensions = bindings.createNormalWorldDimensions();
        HolderLookup.RegistryLookup<PlacedFeature> registry = bindings.vanillaRegistries()
                .lookupOrThrow(Registries.PLACED_FEATURE);
        overworld = resolve(dimensions, bindings, registry, WorldDimension.OVERWORLD, DIAMONDS);
        nether = resolve(dimensions, bindings, registry, WorldDimension.NETHER, DEBRIS);
    }

    List<PredictedOre> predict(long seed, ClientLevel level, BlockPos player) {
        DimensionFeatures dimension;
        OreKind kind;
        if (level.dimension() == Level.OVERWORLD) {
            dimension = overworld;
            kind = OreKind.DIAMOND;
        } else if (level.dimension() == Level.NETHER) {
            dimension = nether;
            kind = OreKind.ANCIENT_DEBRIS;
        } else {
            return List.of();
        }

        int chunkRadius = (RADIUS + 15) / 16 + 1;
        ChunkPos center = new ChunkPos(player.getX() >> 4, player.getZ() >> 4);
        Set<BlockPos> candidates = new HashSet<>();
        for (int chunkX = center.x() - chunkRadius; chunkX <= center.x() + chunkRadius; chunkX++) {
            for (int chunkZ = center.z() - chunkRadius; chunkZ <= center.z() + chunkRadius; chunkZ++) {
                for (ResolvedFeature feature : dimension.features) {
                    predictFeature(seed, chunkX, chunkZ, level, dimension, feature, candidates);
                }
            }
        }

        long horizontalLimit = (long) RADIUS * RADIUS;
        return candidates.stream()
                .filter(pos -> Math.abs(pos.getY() - player.getY()) <= RADIUS)
                .filter(pos -> horizontalDistanceSquared(pos, player) <= horizontalLimit)
                .filter(pos -> possibleTarget(level, pos, kind))
                .map(pos -> new PredictedOre(pos, kind, confirmed(level, pos, kind)))
                .sorted(Comparator.comparingLong(ore -> distanceSquared(ore.pos(), player)))
                .limit(MAX_RESULTS)
                .toList();
    }

    private static DimensionFeatures resolve(WorldDimensions dimensions, MinecraftBindings bindings,
            HolderLookup.RegistryLookup<PlacedFeature> registry, WorldDimension dimension,
            List<FeatureSpec> specs) {
        LevelStem stem = dimensions.get(bindings.levelStemKey(dimension))
                .orElseThrow(() -> new IllegalStateException("Missing vanilla " + dimension + " dimension"));
        ChunkGenerator generator = stem.generator();
        List<FeatureSorter.StepFeatureData> steps = FeatureSorter.buildFeaturesPerStep(
                List.copyOf(generator.getBiomeSource().possibleBiomes()),
                biome -> generator.getBiomeGenerationSettings(biome).features(), true);
        List<ResolvedFeature> resolved = new ArrayList<>();
        for (FeatureSpec spec : specs) {
            ResourceKey<PlacedFeature> key = ResourceKey.create(
                    Registries.PLACED_FEATURE,
                    Identifier.fromNamespaceAndPath("minecraft", spec.id));
            PlacedFeature placed = registry.getOrThrow(key).value();
            int step = -1;
            int index = -1;
            for (int candidateStep = 0; candidateStep < steps.size(); candidateStep++) {
                int candidateIndex = steps.get(candidateStep).features().indexOf(placed);
                if (candidateIndex >= 0) {
                    step = candidateStep;
                    index = candidateIndex;
                    break;
                }
            }
            if (index < 0) {
                throw new IllegalStateException("Vanilla feature is not used by its dimension: " + spec.id);
            }
            ConfiguredFeature<?, ?> configured = placed.feature().value();
            if (!(configured.config() instanceof OreConfiguration ore)) {
                throw new IllegalStateException("Vanilla feature is not an ore: " + spec.id);
            }
            resolved.add(new ResolvedFeature(spec, step, index, ore.size, ore.discardChanceOnAirExposure,
                    configured.feature() == Feature.SCATTERED_ORE));
        }
        int minY = stem.type().value().minY();
        int maxY = minY + stem.type().value().height() - 1;
        OreKind kind = dimension == WorldDimension.OVERWORLD ? OreKind.DIAMOND : OreKind.ANCIENT_DEBRIS;
        return new DimensionFeatures(minY, maxY, kind, List.copyOf(resolved));
    }

    private static void predictFeature(long seed, int chunkX, int chunkZ, ClientLevel level,
            DimensionFeatures dimension, ResolvedFeature feature, Set<BlockPos> output) {
        WorldgenRandom random = new WorldgenRandom(new XoroshiroRandomSource(0L));
        int blockX = chunkX * 16;
        int blockZ = chunkZ * 16;
        long decorationSeed = random.setDecorationSeed(seed, blockX, blockZ);
        random.setFeatureSeed(decorationSeed, feature.index, feature.step);

        if (feature.spec.rarity > 0 && random.nextFloat() >= 1.0F / feature.spec.rarity) {
            return;
        }
        for (int attempt = 0; attempt < feature.spec.count; attempt++) {
            int x = blockX + random.nextInt(16);
            int z = blockZ + random.nextInt(16);
            int y = sampleHeight(random, dimension, feature.spec.height);
            BlockPos origin = new BlockPos(x, y, z);
            if (feature.scattered) {
                addScattered(random, origin, feature, level, dimension.kind, output);
            } else {
                addVein(random, origin, feature, level, dimension, output);
            }
        }
    }

    private static int sampleHeight(RandomSource random, DimensionFeatures dimension, Height height) {
        return switch (height) {
            case TRAPEZOID_OVERWORLD -> trapezoid(random, dimension.minY - 80, dimension.minY + 80);
            case UNIFORM_DIAMOND -> between(random, -64, -4);
            case TRAPEZOID_DEBRIS -> trapezoid(random, 8, 24);
            case UNIFORM_NETHER -> between(random, dimension.minY + 8, dimension.maxY - 8);
        };
    }

    private static int trapezoid(RandomSource random, int min, int max) {
        int range = max - min;
        int lowerHalf = range / 2;
        int upperHalf = range - lowerHalf;
        return min + between(random, 0, upperHalf) + between(random, 0, lowerHalf);
    }

    private static int between(RandomSource random, int min, int max) {
        return min + random.nextInt(max - min + 1);
    }

    private static void addScattered(RandomSource random, BlockPos origin, ResolvedFeature feature,
            ClientLevel level, OreKind kind, Set<BlockPos> output) {
        int count = random.nextInt(feature.size + 1);
        for (int i = 0; i < count; i++) {
            int spread = Math.min(i, 7);
            int x = Math.round((random.nextFloat() - random.nextFloat()) * spread);
            int y = Math.round((random.nextFloat() - random.nextFloat()) * spread);
            int z = Math.round((random.nextFloat() - random.nextFloat()) * spread);
            BlockPos pos = origin.offset(x, y, z);
            if (wouldPlace(random, level, pos, kind, feature.discardChance)) output.add(pos);
        }
    }

    private static void addVein(RandomSource random, BlockPos origin, ResolvedFeature feature,
            ClientLevel level, DimensionFeatures dimension, Set<BlockPos> output) {
        int size = feature.size;
        float angle = random.nextFloat() * (float) Math.PI;
        float reach = size / 8.0F;
        double x1 = origin.getX() + Math.sin(angle) * reach;
        double x2 = origin.getX() - Math.sin(angle) * reach;
        double z1 = origin.getZ() + Math.cos(angle) * reach;
        double z2 = origin.getZ() - Math.cos(angle) * reach;
        double y1 = origin.getY() + random.nextInt(3) - 2;
        double y2 = origin.getY() + random.nextInt(3) - 2;
        double[] spheres = new double[size * 4];
        for (int i = 0; i < size; i++) {
            float progress = (float) i / size;
            double radius = ((Math.sin(Math.PI * progress) + 1.0)
                    * random.nextDouble() * size / 16.0 + 1.0) / 2.0;
            spheres[i * 4] = lerp(progress, x1, x2);
            spheres[i * 4 + 1] = lerp(progress, y1, y2);
            spheres[i * 4 + 2] = lerp(progress, z1, z2);
            spheres[i * 4 + 3] = radius;
        }
        removeContainedSpheres(spheres, size);
        Set<BlockPos> visited = new HashSet<>();
        for (int i = 0; i < size; i++) {
            double radius = spheres[i * 4 + 3];
            if (radius < 0.0) continue;
            double cx = spheres[i * 4];
            double cy = spheres[i * 4 + 1];
            double cz = spheres[i * 4 + 2];
            for (int x = (int) Math.floor(cx - radius); x <= Math.floor(cx + radius); x++) {
                double dx = (x + 0.5 - cx) / radius;
                if (dx * dx >= 1.0) continue;
                for (int y = (int) Math.floor(cy - radius); y <= Math.floor(cy + radius); y++) {
                    double dy = (y + 0.5 - cy) / radius;
                    if (dx * dx + dy * dy >= 1.0) continue;
                    for (int z = (int) Math.floor(cz - radius); z <= Math.floor(cz + radius); z++) {
                        double dz = (z + 0.5 - cz) / radius;
                        if (dx * dx + dy * dy + dz * dz < 1.0) {
                            BlockPos pos = new BlockPos(x, y, z);
                            if (y >= dimension.minY && y <= dimension.maxY && visited.add(pos)
                                    && wouldPlace(random, level, pos, dimension.kind,
                                            feature.discardChance)) {
                                output.add(pos);
                            }
                        }
                    }
                }
            }
        }
    }

    private static boolean wouldPlace(RandomSource random, ClientLevel level, BlockPos pos, OreKind kind,
            float discardChance) {
        boolean loaded = level.isLoaded(pos);
        boolean target = !loaded || possibleTarget(level, pos, kind);
        if (!target) return false;

        boolean confirmed = loaded && confirmed(level, pos, kind);
        boolean accepted;
        if (discardChance <= 0.0F) {
            accepted = true;
        } else if (discardChance >= 1.0F) {
            accepted = !adjacentToAir(level, pos);
        } else {
            accepted = random.nextFloat() >= discardChance || !adjacentToAir(level, pos);
        }
        return accepted || confirmed;
    }

    private static boolean adjacentToAir(ClientLevel level, BlockPos pos) {
        for (Direction direction : Direction.values()) {
            BlockPos neighbor = pos.relative(direction);
            if (level.isLoaded(neighbor) && level.getBlockState(neighbor).isAir()) return true;
        }
        return false;
    }

    private static void removeContainedSpheres(double[] spheres, int size) {
        for (int first = 0; first < size - 1; first++) {
            if (spheres[first * 4 + 3] <= 0.0) continue;
            for (int second = first + 1; second < size; second++) {
                if (spheres[second * 4 + 3] <= 0.0) continue;
                double dx = spheres[first * 4] - spheres[second * 4];
                double dy = spheres[first * 4 + 1] - spheres[second * 4 + 1];
                double dz = spheres[first * 4 + 2] - spheres[second * 4 + 2];
                double radiusDifference = spheres[first * 4 + 3] - spheres[second * 4 + 3];
                if (radiusDifference * radiusDifference > dx * dx + dy * dy + dz * dz) {
                    spheres[(radiusDifference > 0.0 ? second : first) * 4 + 3] = -1.0;
                }
            }
        }
    }

    private static boolean possibleTarget(ClientLevel level, BlockPos pos, OreKind kind) {
        if (!level.isLoaded(pos)) return false;
        BlockState state = level.getBlockState(pos);
        return switch (kind) {
            case DIAMOND -> state.is(Blocks.DIAMOND_ORE) || state.is(Blocks.DEEPSLATE_DIAMOND_ORE)
                    || state.is(BlockTags.STONE_ORE_REPLACEABLES)
                    || state.is(BlockTags.DEEPSLATE_ORE_REPLACEABLES);
            case ANCIENT_DEBRIS -> state.is(Blocks.ANCIENT_DEBRIS) || state.is(BlockTags.BASE_STONE_NETHER);
        };
    }

    private static boolean confirmed(ClientLevel level, BlockPos pos, OreKind kind) {
        BlockState state = level.getBlockState(pos);
        return switch (kind) {
            case DIAMOND -> state.is(Blocks.DIAMOND_ORE) || state.is(Blocks.DEEPSLATE_DIAMOND_ORE);
            case ANCIENT_DEBRIS -> state.is(Blocks.ANCIENT_DEBRIS);
        };
    }

    private static long horizontalDistanceSquared(BlockPos first, BlockPos second) {
        long dx = first.getX() - second.getX();
        long dz = first.getZ() - second.getZ();
        return dx * dx + dz * dz;
    }

    private static long distanceSquared(BlockPos first, BlockPos second) {
        long dy = first.getY() - second.getY();
        return horizontalDistanceSquared(first, second) + dy * dy;
    }

    private static double lerp(double amount, double start, double end) {
        return start + amount * (end - start);
    }

    enum OreKind { DIAMOND, ANCIENT_DEBRIS }

    record PredictedOre(BlockPos pos, OreKind kind, boolean confirmed) { }

    private enum Height { TRAPEZOID_OVERWORLD, UNIFORM_DIAMOND, TRAPEZOID_DEBRIS, UNIFORM_NETHER }

    private record FeatureSpec(String id, int count, int rarity, Height height) { }

    private record ResolvedFeature(FeatureSpec spec, int step, int index, int size,
            float discardChance, boolean scattered) { }

    private record DimensionFeatures(int minY, int maxY, OreKind kind, List<ResolvedFeature> features) { }
}
