package snownee.snow.block;

import net.minecraft.nbt.CompoundNBT;
import snownee.kiwi.tile.TextureTile;
import snownee.snow.MainModule;

public class SnowTextureTile extends TextureTile {

    public SnowTextureTile() {
        super(MainModule.TEXTURE_TILE, "0");
    }

    @Override
    public boolean isMark(String key) {
        return "0".equals(key);
    }

    @Override
    public void read(CompoundNBT compound) {
        readPacketData(compound);
        super.read(compound);
    }

    @Override
    public CompoundNBT write(CompoundNBT compound) {
        writePacketData(compound);
        return super.write(compound);
    }

}
