package cn.blockforge.generated.nnnnr1201forge2;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
public final class SealPillarBlock extends Block {
    public static final BooleanProperty INLAID=BooleanProperty.create("inlaid");
    private final boolean special;
    public SealPillarBlock(boolean special){super(BlockBehaviour.Properties.ofFullCopy(Blocks.DEEPSLATE_BRICKS).strength(6,1200).lightLevel(s->s.getValue(INLAID)?12:0));this.special=special;registerDefaultState(stateDefinition.any().setValue(INLAID,false));}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(INLAID);}
    @Override public net.minecraft.world.ItemInteractionResult useItemOn(net.minecraft.world.item.ItemStack stack, BlockState s, Level l, BlockPos pos, Player p, InteractionHand h, BlockHitResult hit){
        if(!special||!p.getItemInHand(h).is(ExpeditionContent.item("star_iron_fragment"))||s.getValue(INLAID))return net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if(p instanceof ServerPlayer){l.setBlock(pos,s.setValue(INLAID,true),3);if(!p.isCreative())p.getItemInHand(h).shrink(1);}
        return net.minecraft.world.ItemInteractionResult.sidedSuccess(l.isClientSide);
    }
}
