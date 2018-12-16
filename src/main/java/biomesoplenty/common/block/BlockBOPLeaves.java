/*******************************************************************************
 * Copyright 2014-2016, the Biomes O' Plenty Team
 * 
 * This work is licensed under a Creative Commons Attribution-NonCommercial-NoDerivatives 4.0 International Public License.
 * 
 * To view a copy of this license, visit http://creativecommons.org/licenses/by-nc-nd/4.0/.
 ******************************************************************************/

package biomesoplenty.common.block;

import java.util.List;
import java.util.Random;

import javax.annotation.Nullable;

import com.google.common.collect.Lists;

import biomesoplenty.api.enums.BOPTrees;
import biomesoplenty.api.item.BOPItems;
import biomesoplenty.common.item.ItemBOPBlock;
import biomesoplenty.common.util.block.VariantPagingHelper;
import net.minecraft.block.BlockLeaves;
import net.minecraft.block.BlockPlanks;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.color.IBlockColor;
import net.minecraft.client.renderer.color.IItemColor;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.stats.StatList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.ColorizerFoliage;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeColorHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class BlockBOPLeaves extends BlockLeaves implements IBOPBlock
{
    
    // setup paged variant property
    
    // CHECK_DECAY and DECAYABLE require one bit each, so we have 2 bits left for the VARIANT which means we can have four per instance
    public static VariantPagingHelper<BlockBOPLeaves, BOPTrees> paging = new VariantPagingHelper<BlockBOPLeaves, BOPTrees>(4, BOPTrees.class);
    
    // Slightly naughty hackery here
    // The constructor of Block() calls createBlockState() which needs to know the particular instance's variant property
    // There is no way to set the individual block instance's variant property before this, because the super() has to be first
    // So, we use the static variable currentVariantProperty to provide each instance access to its variant property during creation
    private static IProperty currentVariantProperty;
    
    // Create an instance for each page
    public static void createAllPages()
    {        
        int numPages = paging.getNumPages();        
        for (int i = 0; i < numPages; ++i)
        {
            currentVariantProperty = paging.getVariantProperty(i);
            paging.addBlock(i, new BlockBOPLeaves());
        }
        
    }
    
    // Each instance has a reference to its own variant property
    public IProperty variantProperty;
    
    @Override
    protected BlockStateContainer createBlockState()
    {
        this.variantProperty = currentVariantProperty; // get from static variable
        return new BlockStateContainer(this, CHECK_DECAY, DECAYABLE, this.variantProperty);
    }
    
    
    // implement IBOPBlock
    @Override
    public Class<? extends ItemBlock> getItemClass() { return ItemBOPBlock.class; }
    @Override
    public IProperty[] getPresetProperties() { return new IProperty[] {this.variantProperty}; }
    @Override
    public IProperty[] getNonRenderingProperties() { return new IProperty[] {CHECK_DECAY, DECAYABLE}; }
    @Override
    public String getStateName(IBlockState state)
    {
        BOPTrees tree = ((BOPTrees) state.getValue(this.variantProperty));
        return tree.getName() + "_leaves";
    }

    public enum ColoringType {PLAIN, TINTED, OVERLAY}

    public static ColoringType getColoringType(BOPTrees tree)
    {
        switch (tree)
        {
            case UMBRAN: case DEAD: case ETHEREAL: case FIR: case HELLBARK: case JACARANDA: case MAGIC: case MAPLE: case ORANGE_AUTUMN: case ORIGIN: case PINK_CHERRY: case REDWOOD: case WHITE_CHERRY: case YELLOW_AUTUMN:
            return ColoringType.PLAIN;
            case FLOWERING:
                return ColoringType.OVERLAY;
            case MAHOGANY: case PALM: case WILLOW: case EBONY: default:
            return ColoringType.TINTED;
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IBlockColor getBlockColor()
    {
        return new IBlockColor()
        {
            @Override
            public int colorMultiplier(IBlockState state, IBlockAccess world, BlockPos pos, int tintIndex)
            {
            	boolean inWorld = world != null && pos != null;

            	switch (getColoringType((BOPTrees) state.getValue(BlockBOPLeaves.this.variantProperty)))
            	{
            		case TINTED:
            			return inWorld ? BiomeColorHelper.getFoliageColorAtPos(world, pos) : ColorizerFoliage.getFoliageColorBasic();
            		case OVERLAY:
            			if (tintIndex == 0) 
            				return inWorld ? BiomeColorHelper.getFoliageColorAtPos(world, pos) : ColorizerFoliage.getFoliageColorBasic();
            			//Otherwise fall-through
            		default:
            			return 0xFFFFFF;
            	}
            }
        };
    }
    @Override
    @SideOnly(Side.CLIENT)
    public IItemColor getItemColor() { return BlockColoring.BLOCK_ITEM_COLORING; }
    
    private BlockBOPLeaves()
    {
        super();
        this.setDefaultState(this.blockState.getBaseState().withProperty(CHECK_DECAY, Boolean.valueOf(true)).withProperty(DECAYABLE, Boolean.valueOf(true)));
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void getSubBlocks(CreativeTabs tab, NonNullList<ItemStack> list) {}
    
    // map from meta to state and vice verca.  Use the same scheme as for the vanilla leaf blocks
    // highest bit is for CHECK_DECAY  true=>1 false=>0
    // next bit is for DECAYABLE  true=>0  false=>1  (other way round this time!  cheers Mojang)
    // low 2 bits for VARIANT
    @Override
    public IBlockState getStateFromMeta(int meta)
    {
        return this.getDefaultState().withProperty(this.variantProperty, paging.getVariant(this, meta & 3)).withProperty(DECAYABLE, Boolean.valueOf((meta & 4) == 0)).withProperty(CHECK_DECAY, Boolean.valueOf((meta & 8) > 0));
    }
    @Override
    public int getMetaFromState(IBlockState state)
    {
        BOPTrees tree = (BOPTrees) state.getValue(this.variantProperty);
        int meta = paging.getIndex(tree);
        if (!state.getValue(DECAYABLE).booleanValue())
        {
            meta |= 4;
        }
        if (state.getValue(CHECK_DECAY).booleanValue())
        {
            meta |= 8;
        }
        return meta;
    }
    
    // blocks that are not placed during generation should not decay
    @Override
    public IBlockState getStateForPlacement(World worldIn, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer)
    {
        return this.getStateFromMeta(meta).withProperty(CHECK_DECAY, Boolean.valueOf(false)).withProperty(DECAYABLE, Boolean.valueOf(false));
    }
    
    // Inventory models are set only for the default states of leaves. Consequently, we modify the states for player placed leaves when they are
    // actually placed, not when they are in the inventory. We cannot change the default properties whilst reusing code from BlockLeaves.
    @Override
    public ItemStack getPickBlock(IBlockState state, RayTraceResult target, World world, BlockPos pos, EntityPlayer player)
    {
        return new ItemStack(this, 1, this.getMetaFromState(this.getDefaultState().withProperty(this.variantProperty, state.getValue(this.variantProperty))));
    }
    
    // leaves in the inventory should not be decayable
    @Override
    protected ItemStack getSilkTouchDrop(IBlockState state)
    {
    	IBlockState newState = state.withProperty(CHECK_DECAY, Boolean.valueOf(false)).withProperty(DECAYABLE, Boolean.valueOf(false));
    	
    	return super.getSilkTouchDrop(newState);
    }
    
    @Override
    protected int getSaplingDropChance(IBlockState state)
    {
        return 20;
    }
    
    @Override
    public Item getItemDropped(IBlockState state, Random rand, int fortune)
    {
        BOPTrees treeType = ((BOPTrees) state.getValue(this.variantProperty));
        return treeType.hasSapling() ? Item.getItemFromBlock( BlockBOPSapling.paging.getBlock(treeType) ) : null;
    }
    
    @Override
    public int damageDropped(IBlockState state)
    {
        BOPTrees treeType = ((BOPTrees) state.getValue(this.variantProperty));
        return treeType.hasSapling() ? BlockBOPSapling.paging.getVariantItem(treeType).getItemDamage() : 0;
    }
    
    @Override
    public MapColor getMapColor(IBlockState state, IBlockAccess worldIn, BlockPos pos)
    {
    	switch ((BOPTrees) state.getValue(this.variantProperty))
    	{
    		case YELLOW_AUTUMN:
    			return MapColor.YELLOW;
    			
    		case ORANGE_AUTUMN:
    			return MapColor.ADOBE;
    			
    		case MAGIC:
    			return MapColor.LIGHT_BLUE;
    			
    		case UMBRAN:
    			return MapColor.BLUE_STAINED_HARDENED_CLAY;
    			
    		case DEAD:
    			return MapColor.BROWN;
    			
    		case PINK_CHERRY:
    			return MapColor.PINK;
    			
    		case WHITE_CHERRY:
    			return MapColor.WHITE_STAINED_HARDENED_CLAY;
    			
    		case MAPLE:
    			return MapColor.PINK_STAINED_HARDENED_CLAY;
    	
    		case JACARANDA:
    			return MapColor.PURPLE;
    	
    		default:
    			return this.blockMapColor;
    	}
    }
    
    @Override
    public List<ItemStack> getDrops(IBlockAccess world, BlockPos pos, IBlockState state, int fortune)
    {
        BOPTrees treeType = ((BOPTrees) state.getValue(this.variantProperty));
        
        if (treeType.hasSapling())
        {
            return super.getDrops(world, pos, state, fortune);
        }
        
        //Tree types without saplings shouldn't drop apples either
        return Lists.newArrayList();
    }
    
    @Override
    protected void dropApple(World worldIn, BlockPos pos, IBlockState state, int chance)
    {
        // chance will initially be related to fortune as follows:  0 => 200, 1 => 180, 2 => 160, 3 => 120, 4+ => 40
        ItemStack fruit = null;
        
        BOPTrees tree = ((BOPTrees) state.getValue(this.variantProperty));
        switch (tree)
        {
	        case FLOWERING: case JACARANDA: case MAHOGANY:
	        	if (worldIn.rand.nextInt(chance) == 0)
	            {
	                fruit = new ItemStack(BOPItems.peach, 1, 0);
	            }
	        	break;
            case WILLOW:
            	if (worldIn.rand.nextInt(chance) == 0)
                {
                    fruit = new ItemStack(BOPItems.pear, 1, 0);
                }
            	break;
	        case YELLOW_AUTUMN: case ORANGE_AUTUMN: case MAPLE: case DEAD:
	        	if (worldIn.rand.nextInt(chance) == 0)
	            {
	                fruit = new ItemStack(BOPItems.persimmon, 1, 0);
	            }
	        	break;
            case ORIGIN:
            	if (worldIn.rand.nextInt(chance) == 0)
                {
                    fruit = new ItemStack(Items.APPLE, 1, 0);
                }
            	break;
            default:
                break;
        }
        
        if (fruit != null) {
            spawnAsEntity(worldIn, pos, fruit);
        }
    }

    @Override
    public void harvestBlock(World worldIn, EntityPlayer player, BlockPos pos, IBlockState state, @Nullable TileEntity te, @Nullable ItemStack stack)
    {
        if (!worldIn.isRemote && stack != null && stack.getItem() == Items.SHEARS)
        {
            player.addStat(StatList.getBlockStats(this));
        }
        else
        {
            super.harvestBlock(worldIn, player, pos, state, te, stack);
        }
    }

    @Override
    public List<ItemStack> onSheared(ItemStack item, IBlockAccess world, BlockPos pos, int fortune)
    {
        List<ItemStack> ret = new java.util.ArrayList<ItemStack>();
        IBlockState state = world.getBlockState(pos);
        if (state.getBlock() == this)
        {
            // get default state corresponding to this tree (IE discard CHECK_DECAY and DECAYABLE bits for item meta)
            BOPTrees tree = (BOPTrees)state.getValue(this.variantProperty);
            int meta = this.getMetaFromState(this.getDefaultState().withProperty(this.variantProperty, tree));
            ret.add(new ItemStack(this, 1, meta));
        }
        return ret;
    }
    
    @Override
    public int getFlammability(IBlockAccess world, BlockPos pos, EnumFacing face)
    {
        BOPTrees tree = ((BOPTrees) world.getBlockState(pos).getValue(this.variantProperty));
        switch (tree)
        {
            case HELLBARK:
                return 0;
            default:
                return Blocks.LEAVES.getFlammability(world, pos, face);
        }
    }
    
    @Override
    public int getFireSpreadSpeed(IBlockAccess world, BlockPos pos, EnumFacing face)
    {
        BOPTrees tree = ((BOPTrees) world.getBlockState(pos).getValue(this.variantProperty));
        switch (tree)
        {
            case HELLBARK:
                return 0;
            default:
                return Blocks.LEAVES.getFireSpreadSpeed(world, pos, face);
        }
    }
    
    
    //The fields used by getBlockLayer(), isOpaqueCube() and shouldSideBeRendered are set externally for Blocks.LEAVES *specifically*. As a result, we do not inherit
    //it simply be extending BlockLeaves.
    @Override
    @SideOnly(Side.CLIENT)
    public BlockRenderLayer getBlockLayer()
    {
        return Blocks.LEAVES.getBlockLayer();
    }
    
    @Override
    public boolean isOpaqueCube(IBlockState state)
    {
        return Blocks.LEAVES.isOpaqueCube(state);
    }

    @SideOnly(Side.CLIENT)
    @Override
    public boolean shouldSideBeRendered(IBlockState state, IBlockAccess world, BlockPos pos, EnumFacing side)
    {
        return Blocks.LEAVES.shouldSideBeRendered(state, world, pos, side);
    }

    // We are forced to implement the method below in order to extend the BlockLeaves abstract class
    // ...however, we don't actually use it anywhere so it's safe to just return null
    // it makes no sense in our context
    @Override
    public BlockPlanks.EnumType getWoodType(int meta) {return null;}
    
    
}
    
  