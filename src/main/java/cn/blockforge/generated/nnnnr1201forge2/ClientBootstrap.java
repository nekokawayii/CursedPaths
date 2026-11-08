package cn.blockforge.generated.nnnnr1201forge2;

import com.mojang.logging.LogUtils;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.bus.api.EventPriority;


/** 由模组构造器在客户端显式调用，不依赖自动扫描来接入关键渲染事件。 */
public final class ClientBootstrap {
    private static boolean installed;

    public static void install() {
        if(installed)return;
        installed=true;
        character.CharacterClient.install();
        var modBus=GeneratedMod.MOD_BUS;
        modBus.register(Client.Registration.class);
        modBus.register(TenShadowsClient.Registration.class);
        modBus.addListener(DismantleMotionClient.Registration::layers);
        modBus.addListener(FinalePoseClient.Registration::layers);
        modBus.addListener(MeleeComboClient.Registration::layers);
        modBus.register(GojoOpticsClient.Registration.class);
        modBus.register(BloodClient.Registration.class);
        modBus.addListener(BloodRealmRenderer::shaders);
        Network.motionReceiver=DismantleMotionClient::receive;
        MeleeCombo.receiver=MeleeComboClient::receive;
        modBus.addListener(EntityRendererRegistry::register);
        modBus.addListener(EventPriority.LOWEST,EntityRendererRegistry::afterReload);
        modBus.addListener(TenkaPendantClient::layers);
        modBus.addListener(RitualClient::register);
        modBus.addListener(CurseModelReload::register);
        ExpeditionProgress.receiver=RitualClient::progress;

        var gameBus=NeoForge.EVENT_BUS;
        EventBuses.registerSplit(Client.class);
        EventBuses.registerSplit(TenShadowsClient.class);
        EventBuses.registerSplit(ShadowClient.class);
        modBus.register(ShadowClient.Registration.class);
        ShadowSystem.receiver=ShadowClient::receive;
        ShadowSystem.fxReceiver=ShadowClient::effect;
        EventBuses.registerSplit(DismantleMotionClient.class);
        EventBuses.registerSplit(FinaleClient.class);
        EventBuses.registerSplit(FinalePoseClient.class);
        EventBuses.registerSplit(CastHands.class);
        EventBuses.registerSplit(ClientInput.class);
        EventBuses.registerSplit(MeleeComboClient.class);
        EventBuses.registerSplit(GojoOpticsClient.class);
        EventBuses.registerSplit(BloodClient.class);
        EventBuses.registerSplit(DeathStarClient.class);
        EventBuses.registerSplit(BloodRealmRenderer.class);
        EventBuses.registerSplit(TenkaPendantClient.class);
        EventBuses.registerSplit(ThousandMileChainClient.class);
        ThousandMileChain.receiver=ThousandMileChainClient::receive;
        gameBus.addListener(EntityRendererRegistry::login);
        gameBus.addListener(EntityRendererRegistry::logout);
        gameBus.addListener(EventPriority.LOWEST,EntityRendererRegistry::entityJoined);
        gameBus.addListener(EventPriority.LOWEST,EntityRendererRegistry::beforeFrame);
        gameBus.addListener(EventPriority.LOWEST,EntityRendererRegistry::beforeEntities);
        LogUtils.getLogger().info("[宿傩渲染 r127] 实体、解动作、终式、结印与八段体术已从模组入口接入。");
    }
    private ClientBootstrap() {}
}
