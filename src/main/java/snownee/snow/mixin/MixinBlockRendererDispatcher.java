package snownee.snow.mixin;

import java.util.Optional;
import java.util.Random;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;

import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.SnowBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockModelRenderer;
import net.minecraft.client.renderer.BlockRendererDispatcher;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.RenderTypeLookup;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.crash.CrashReport;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.crash.ReportedException;
import net.minecraft.resources.IResourceManager;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockDisplayReader;
import net.minecraftforge.client.MinecraftForgeClient;
import net.minecraftforge.client.model.data.IModelData;
import snownee.snow.CoreModule;
import snownee.snow.block.SnowTile;
import snownee.snow.block.SnowTile.Options;
import snownee.snow.block.SnowVariant;

@Mixin(BlockRendererDispatcher.class)
public abstract class MixinBlockRendererDispatcher {

	@Shadow
	@Final
	private BlockModelRenderer blockModelRenderer;

	private final Options defaultOptions = new Options();
	private IBakedModel cachedSnowModel;
	private IBakedModel cachedOverlayModel;

	@SuppressWarnings("deprecation")
	@Inject(
			method = "renderModel(Lnet/minecraft/block/BlockState;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/world/IBlockDisplayReader;Lcom/mojang/blaze3d/matrix/MatrixStack;Lcom/mojang/blaze3d/vertex/IVertexBuilder;ZLjava/util/Random;Lnet/minecraftforge/client/model/data/IModelData;)Z", at = @At(
				"HEAD"
			), remap = false, cancellable = true
	)
	private void srm_renderModel(BlockState blockStateIn, BlockPos posIn, IBlockDisplayReader lightReaderIn, MatrixStack matrixStackIn, IVertexBuilder vertexBuilderIn, boolean checkSides, Random rand, IModelData modelData, CallbackInfoReturnable<Boolean> ci) {
		if (!(blockStateIn.getBlock() instanceof SnowVariant)) {
			return;
		}
		if (blockStateIn.hasProperty(SnowBlock.LAYERS) && blockStateIn.get(SnowBlock.LAYERS) == 8) {
			return;
		}
		BlockState state = modelData.getData(SnowTile.BLOCKSTATE);
		if (state == null || (!state.isAir() && state.getRenderType() != BlockRenderType.MODEL)) {
			return;
		}
		try {
			RenderType cutoutMipped = RenderType.getCutoutMipped();
			RenderType solid = RenderType.getSolid();
			RenderType layer = MinecraftForgeClient.getRenderLayer();
			boolean canRender;
			if (layer == null) {
				canRender = layer == cutoutMipped;
			} else {
				canRender = RenderTypeLookup.canRenderInLayer(state, layer);
			}
			if (canRender) {
				matrixStackIn.push();
				if (blockStateIn.hasProperty(SnowBlock.LAYERS)) {
					String namespace = state.getBlock().getRegistryName().getNamespace();
					if ("projectvibrantjourneys".equals(namespace) || "foragecraft".equals(namespace)) {
						if (blockStateIn.get(SnowBlock.LAYERS) > 3) {
							matrixStackIn.pop();
							return;
						}
						matrixStackIn.translate(0.001, 0.101, 0.001);
						matrixStackIn.scale(0.998f, 1, 0.998f);
					}
				}
				blockModelRenderer.renderModel(lightReaderIn, getModelForState(state), state, posIn, matrixStackIn, vertexBuilderIn, false, rand, state.getPositionRandom(posIn), OverlayTexture.NO_OVERLAY, modelData);
				matrixStackIn.pop();
			}
			Options options = Optional.ofNullable(modelData.getData(SnowTile.OPTIONS)).orElse(defaultOptions);
			if (options.renderBottom && (layer == null || layer == solid)) {
				if (cachedSnowModel == null) {
					cachedSnowModel = getModelForState(CoreModule.BLOCK.getDefaultState());
				}
				blockModelRenderer.renderModel(lightReaderIn, cachedSnowModel, CoreModule.BLOCK.getDefaultState(), posIn, matrixStackIn, vertexBuilderIn, false, rand, state.getPositionRandom(posIn), OverlayTexture.NO_OVERLAY, modelData);
			}

			if (blockStateIn.getBlock() == CoreModule.SLAB || blockStateIn.getBlock() instanceof SnowBlock) {
				if (options.renderOverlay && layer == cutoutMipped) {
					if (cachedOverlayModel == null) {
						cachedOverlayModel = Minecraft.getInstance().getModelManager().getModel(CoreModule.OVERLAY_MODEL);
					}
					matrixStackIn.push();
					matrixStackIn.scale(1.002F, 1, 1.002F);
					BlockPos pos = posIn;
					if (blockStateIn.getBlock() == CoreModule.SLAB) {
						matrixStackIn.translate(-0.001, -0.375, -0.001);
					} else {
						matrixStackIn.translate(-0.001, -1, -0.001);
						pos = pos.down();
					}
					blockModelRenderer.renderModel(lightReaderIn, cachedOverlayModel, blockStateIn, pos, matrixStackIn, vertexBuilderIn, false, rand, blockStateIn.getPositionRandom(pos), OverlayTexture.NO_OVERLAY, modelData);
					matrixStackIn.pop();
				}
			} else {
				if (!options.renderOverlay) {
					ci.setReturnValue(true);
					return;
				}
			}

			// specify base model's render type
			if (blockStateIn.getBlock() == CoreModule.TILE_BLOCK) {
				if (layer != solid) {
					ci.setReturnValue(true);
					return;
				}
			} else {
				if (layer != cutoutMipped) {
					ci.setReturnValue(true);
					return;
				}
			}

			double yOffset = ((SnowVariant) blockStateIn.getBlock()).getYOffset();
			if (yOffset != 0) {
				matrixStackIn.translate(0, yOffset, 0);
			}
		} catch (Throwable throwable) {
			CrashReport crashreport = CrashReport.makeCrashReport(throwable, "Tesselating block in world");
			CrashReportCategory crashreportcategory = crashreport.makeCategory("Block being tesselated");
			CrashReportCategory.addBlockInfo(crashreportcategory, posIn, blockStateIn);
			throw new ReportedException(crashreport);
		}
	}

	@Shadow
	public abstract IBakedModel getModelForState(BlockState state);

	@Inject(at = @At("HEAD"), method = "onResourceManagerReload")
	private void srm_onResourceManagerReload(IResourceManager resourceManager, CallbackInfo ci) {
		cachedSnowModel = null;
		cachedOverlayModel = null;
	}
}
