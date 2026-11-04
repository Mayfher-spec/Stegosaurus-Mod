package com.prodigytown.chibi;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.*;
import software.bernie.geckolib.GeckoLib;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.util.GeckoLibUtil;
import org.jetbrains.annotations.Nullable;
@Mod(ChibiAlpha.MODID)
public class ChibiAlpha {
 public static final String MODID="prodigychibi";
 public static final DeferredRegister<EntityType<?>> ENTITIES=DeferredRegister.create(ForgeRegistries.ENTITY_TYPES,MODID);
 public static final DeferredRegister<Item> ITEMS=DeferredRegister.create(ForgeRegistries.ITEMS,MODID);
 public static final RegistryObject<EntityType<ChibiStego>> STEGO=ENTITIES.register("stegosaurus_chibi",()->EntityType.Builder.of(ChibiStego::new,MobCategory.CREATURE).sized(1.8f,1.65f).clientTrackingRange(10).build(MODID+":stegosaurus_chibi"));
 public static final RegistryObject<Item> EGG=ITEMS.register("stegosaurus_chibi_spawn_egg",()->new ForgeSpawnEggItem(STEGO,0x5A7B25,0xEADCB8,new Item.Properties()));
 public ChibiAlpha(){
  GeckoLib.initialize();
  var bus=FMLJavaModLoadingContext.get().getModEventBus();
  ENTITIES.register(bus); ITEMS.register(bus);
  bus.addListener(this::creative);
 }
 private void creative(BuildCreativeModeTabContentsEvent event){if(event.getTabKey()==CreativeModeTabs.SPAWN_EGGS)event.accept(EGG.get());}
 @Mod.EventBusSubscriber(modid=MODID,bus=Mod.EventBusSubscriber.Bus.MOD)
 public static class Events {
  @SubscribeEvent public static void attributes(EntityAttributeCreationEvent event){event.put(STEGO.get(),ChibiStego.attributes().build());}
 }
}
class ChibiStego extends PathfinderMob implements GeoEntity {
 private static final EntityDataAccessor<Boolean> SADDLED=SynchedEntityData.defineId(ChibiStego.class,EntityDataSerializers.BOOLEAN);
 private static final RawAnimation IDLE=RawAnimation.begin().thenLoop("animation.chibi_stegosaurus.idle");
 private static final RawAnimation WALK=RawAnimation.begin().thenLoop("animation.chibi_stegosaurus.walk");
 private final AnimatableInstanceCache cache=GeckoLibUtil.createInstanceCache(this);
 public ChibiStego(EntityType<? extends ChibiStego> type,Level level){super(type,level);setMaxUpStep(1f);}
 public static AttributeSupplier.Builder attributes(){return Mob.createMobAttributes().add(Attributes.MAX_HEALTH,35d).add(Attributes.MOVEMENT_SPEED,0.22d).add(Attributes.KNOCKBACK_RESISTANCE,0.45d);}
 @Override protected void defineSynchedData(){super.defineSynchedData();entityData.define(SADDLED,false);}
 public boolean saddled(){return entityData.get(SADDLED);}
 public void saddle(boolean value){entityData.set(SADDLED,value);}
 @Override public void addAdditionalSaveData(CompoundTag tag){super.addAdditionalSaveData(tag);tag.putBoolean("ChibiSaddle",saddled());}
 @Override public void readAdditionalSaveData(CompoundTag tag){super.readAdditionalSaveData(tag);saddle(tag.getBoolean("ChibiSaddle"));}
 @Override protected void registerGoals(){goalSelector.addGoal(0,new FloatGoal(this));goalSelector.addGoal(3,new RandomStrollGoal(this,.8));goalSelector.addGoal(4,new LookAtPlayerGoal(this,Player.class,8f));goalSelector.addGoal(5,new RandomLookAroundGoal(this));}
 @Override public InteractionResult mobInteract(Player player,InteractionHand hand){
  ItemStack held=player.getItemInHand(hand);
  if(!saddled() && held.is(Items.SADDLE)){if(!level().isClientSide()){saddle(true);if(!player.getAbilities().instabuild)held.shrink(1);}return InteractionResult.sidedSuccess(level().isClientSide());}
  if(saddled() && held.isEmpty() && player.isShiftKeyDown()){if(!level().isClientSide()){saddle(false);spawnAtLocation(Items.SADDLE);}return InteractionResult.sidedSuccess(level().isClientSide());}
  if(held.is(Items.WHEAT) && getHealth()<getMaxHealth()){if(!level().isClientSide()){heal(5f);if(!player.getAbilities().instabuild)held.shrink(1);}return InteractionResult.sidedSuccess(level().isClientSide());}
  if(saddled() && !player.isShiftKeyDown() && !isVehicle()){if(!level().isClientSide())player.startRiding(this);return InteractionResult.sidedSuccess(level().isClientSide());}
  return super.mobInteract(player,hand);
 }
 @Override protected boolean canAddPassenger(Entity passenger){return saddled() && getPassengers().isEmpty();}
 @Override public double getPassengersRidingOffset(){return 1.22d;}
 @Nullable @Override public LivingEntity getControllingPassenger(){Entity e=getFirstPassenger();return saddled() && e instanceof Player p ? p:null;}
 @Override public void travel(Vec3 input){LivingEntity passenger=getControllingPassenger();if(isAlive() && isVehicle() && passenger instanceof Player rider){setYRot(rider.getYRot());yRotO=getYRot();setXRot(rider.getXRot()*0.5f);yBodyRot=getYRot();yHeadRot=yBodyRot;float s=rider.xxa*.55f,f=rider.zza;if(f<=0)f*=.3f;setSpeed((float)getAttributeValue(Attributes.MOVEMENT_SPEED)*1.4f);super.travel(new Vec3(s,input.y,f));}else super.travel(input);}
 @Override public void registerControllers(AnimatableManager.ControllerRegistrar reg){reg.add(new AnimationController<>(this,"movement",6,state->state.setAndContinue(state.isMoving()?WALK:IDLE)));}
 @Override public AnimatableInstanceCache getAnimatableInstanceCache(){return cache;}
}
