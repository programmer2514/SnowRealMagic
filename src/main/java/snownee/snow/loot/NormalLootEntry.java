package snownee.snow.loot;

import java.util.function.Consumer;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;

import net.minecraft.block.BlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.LootContext;
import net.minecraft.loot.LootParameterSets;
import net.minecraft.loot.LootParameters;
import net.minecraft.loot.LootPoolEntryType;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.LootTables;
import net.minecraft.loot.StandaloneLootEntry;
import net.minecraft.loot.conditions.ILootCondition;
import net.minecraft.loot.functions.ILootFunction;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IItemProvider;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.server.ServerWorld;
import snownee.snow.CoreModule;
import snownee.snow.block.SnowTile;

public class NormalLootEntry extends StandaloneLootEntry {

	private NormalLootEntry(int weightIn, int qualityIn, ILootCondition[] conditionsIn, ILootFunction[] functionsIn) {
		super(weightIn, qualityIn, conditionsIn, functionsIn);
	}

	@SuppressWarnings("deprecation")
	@Override
	protected void func_216154_a(Consumer<ItemStack> consumer, LootContext context) {
		TileEntity tile = context.get(LootParameters.BLOCK_ENTITY);
		if (tile instanceof SnowTile) {
			BlockState state = ((SnowTile) tile).getState();
			if (!state.isAir()) {
				ResourceLocation resourcelocation = state.getBlock().getLootTable();
				if (resourcelocation != LootTables.EMPTY) {
					LootContext.Builder builder = new LootContext.Builder(context);
					LootContext lootcontext = builder.withParameter(LootParameters.BLOCK_STATE, state).build(LootParameterSets.BLOCK);
					ServerWorld serverworld = lootcontext.getWorld();
					LootTable loottable = serverworld.getServer().getLootTableManager().getLootTableFromLocation(resourcelocation);
					loottable.generate(lootcontext).forEach(consumer::accept);
				}
			}
		}
	}

	public static StandaloneLootEntry.Builder<?> builder(IItemProvider itemIn) {
		return builder(NormalLootEntry::new);
	}

	@Override
	public LootPoolEntryType func_230420_a_() {
		return CoreModule.NORMAL;
	}

	public static class Serializer extends StandaloneLootEntry.Serializer<NormalLootEntry> {
		@Override
		public void func_230422_a_(JsonObject json, NormalLootEntry lootEntry, JsonSerializationContext context) {
			super.func_230422_a_(json, lootEntry, context);
		}

		@Override
		protected NormalLootEntry func_212829_b_(JsonObject json, JsonDeserializationContext context, int weightIn, int qualityIn, ILootCondition[] conditionsIn, ILootFunction[] functionsIn) {
			return new NormalLootEntry(weightIn, qualityIn, conditionsIn, functionsIn);
		}
	}
}
